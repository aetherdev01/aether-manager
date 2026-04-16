package dev.aether.manager.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dev.aether.manager.util.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object AppProfileRepository {

    private const val PROFILE_DIR = "${RootUtils.CONF_DIR}/app_profiles"
    private const val MONITOR_SCRIPT = "${RootUtils.CONF_DIR}/app_monitor.sh"
    private const val SERVICE_SCRIPT = "${RootUtils.CONF_DIR}/app_monitor_service.sh"

    suspend fun loadUserApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        installed
            .filter { app ->
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystem = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                !isSystem || isUpdatedSystem
            }
            .filter { app ->
                val intent = pm.getLaunchIntentForPackage(app.packageName)
                intent != null
            }
            .map { app ->
                val label = try {
                    pm.getApplicationLabel(app).toString()
                } catch (_: Exception) { app.packageName }
                val version = try {
                    pm.getPackageInfo(app.packageName, 0).versionName ?: ""
                } catch (_: Exception) { "" }
                val target = app.targetSdkVersion
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                AppInfo(
                    packageName = app.packageName,
                    label       = label,
                    versionName = version,
                    targetSdk   = target,
                    isSystemApp = isSystem,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun loadProfile(packageName: String): AppProfile = withContext(Dispatchers.IO) {
        val path = "$PROFILE_DIR/${packageName}.json"
        val raw = RootUtils.sh("cat $path 2>/dev/null").stdout.trim()
        if (raw.isEmpty()) return@withContext AppProfile(packageName)
        try {
            val j = JSONObject(raw)
            val tweaks = j.optJSONObject("tweaks") ?: JSONObject()
            AppProfile(
                packageName  = packageName,
                enabled      = j.optBoolean("enabled", false),
                cpuGovernor  = j.optString("cpu_governor", "default"),
                refreshRate  = j.optString("refresh_rate", "default"),
                extraTweaks  = AppExtraTweaks(
                    disableDoze    = tweaks.optBoolean("disable_doze", false),
                    lockCpuMin     = tweaks.optBoolean("lock_cpu_min", false),
                    killBackground = tweaks.optBoolean("kill_background", false),
                    gpuBoost       = tweaks.optBoolean("gpu_boost", false),
                    ioLatency      = tweaks.optBoolean("io_latency", false),
                ),
            )
        } catch (_: Exception) {
            AppProfile(packageName)
        }
    }

    suspend fun saveProfile(profile: AppProfile) = withContext(Dispatchers.IO) {
        val tweaks = JSONObject().apply {
            put("disable_doze",    profile.extraTweaks.disableDoze)
            put("lock_cpu_min",    profile.extraTweaks.lockCpuMin)
            put("kill_background", profile.extraTweaks.killBackground)
            put("gpu_boost",       profile.extraTweaks.gpuBoost)
            put("io_latency",      profile.extraTweaks.ioLatency)
        }
        val json = JSONObject().apply {
            put("package_name",  profile.packageName)
            put("enabled",       profile.enabled)
            put("cpu_governor",  profile.cpuGovernor)
            put("refresh_rate",  profile.refreshRate)
            put("tweaks",        tweaks)
        }
        val content = json.toString()
        val path = "$PROFILE_DIR/${profile.packageName}.json"
        RootUtils.sh("mkdir -p $PROFILE_DIR && printf '%s' '${content.replace("'", "'\\''")}' > $path")
        // Regenerate monitor script after each save
        writeMonitorScript()
    }

    suspend fun deleteProfile(packageName: String) = withContext(Dispatchers.IO) {
        RootUtils.sh("rm -f $PROFILE_DIR/${packageName}.json")
        writeMonitorScript()
    }

    suspend fun loadAllProfiles(): Map<String, AppProfile> = withContext(Dispatchers.IO) {
        val list = RootUtils.sh("ls $PROFILE_DIR/*.json 2>/dev/null").stdout
            .lines().filter { it.endsWith(".json") }
        list.associate { path ->
            val pkg = path.substringAfterLast("/").removeSuffix(".json")
            pkg to loadProfile(pkg)
        }
    }

    private suspend fun writeMonitorScript() = withContext(Dispatchers.IO) {
        val profilesRaw = RootUtils.sh("ls $PROFILE_DIR/*.json 2>/dev/null").stdout
            .lines().filter { it.endsWith(".json") }

        val cases = StringBuilder()
        for (path in profilesRaw) {
            val pkg = path.substringAfterLast("/").removeSuffix(".json")
            val raw = RootUtils.sh("cat $path 2>/dev/null").stdout.trim()
            if (raw.isEmpty()) continue
            try {
                val j = JSONObject(raw)
                if (!j.optBoolean("enabled", false)) continue
                val gov = j.optString("cpu_governor", "default")
                val rr  = j.optString("refresh_rate", "default")
                val tweaks = j.optJSONObject("tweaks") ?: JSONObject()

                val block = buildString {
                    appendLine("    \"$pkg\")")
                    if (gov != "default") {
                        appendLine("      for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do")
                        appendLine("        echo $gov > \"\$cpu\" 2>/dev/null")
                        appendLine("      done")
                    }
                    if (rr != "default") {
                        appendLine("      service call SurfaceFlinger 1035 i32 $rr 2>/dev/null || true")
                        appendLine("      settings put system peak_refresh_rate $rr 2>/dev/null || true")
                        appendLine("      settings put system min_refresh_rate $rr 2>/dev/null || true")
                    }
                    if (tweaks.optBoolean("disable_doze", false)) {
                        appendLine("      dumpsys deviceidle disable 2>/dev/null || true")
                    }
                    if (tweaks.optBoolean("lock_cpu_min", false)) {
                        appendLine("      for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_min_freq; do")
                        appendLine("        max=\$(cat \"\${f/min/max}\" 2>/dev/null); [ -n \"\$max\" ] && echo \$((max/2)) > \"\$f\" 2>/dev/null; done")
                    }
                    if (tweaks.optBoolean("kill_background", false)) {
                        appendLine("      am kill-all 2>/dev/null || true")
                    }
                    if (tweaks.optBoolean("gpu_boost", false)) {
                        appendLine("      echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null || true")
                        appendLine("      echo performance > /sys/class/devfreq/*.gpu/governor 2>/dev/null || true")
                    }
                    if (tweaks.optBoolean("io_latency", false)) {
                        appendLine("      echo 0 > /sys/block/sda/queue/read_ahead_kb 2>/dev/null || true")
                    }
                    appendLine("      ;;")
                }
                cases.append(block)
            } catch (_: Exception) {}
        }

        val script = """#!/system/bin/sh
# Aether App Profile Monitor — auto-generated, do not edit manually
# Apply per-app profiles when foreground app changes

LAST=""
PROFILE_DIR=$PROFILE_DIR

apply_profile() {
  local pkg="${'$'}1"
  case "${'$'}pkg" in
${cases}
    *)
      # Restore defaults when unlisted app is in foreground
      for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
        echo schedutil > "${'$'}cpu" 2>/dev/null || echo ondemand > "${'$'}cpu" 2>/dev/null
      done
      ;;
  esac
}

while true; do
  CURRENT=$(dumpsys window windows 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp' | grep -oE '[a-z][a-z0-9_]*\.[a-z][a-z0-9_.]+' | head -1)
  if [ -n "${'$'}CURRENT" ] && [ "${'$'}CURRENT" != "${'$'}LAST" ]; then
    LAST="${'$'}CURRENT"
    apply_profile "${'$'}CURRENT"
  fi
  sleep 2
done
"""
        val escaped = script.replace("'", "'\\''")
        RootUtils.sh(
            "mkdir -p $PROFILE_DIR",
            "printf '%s' '$escaped' > $MONITOR_SCRIPT",
            "chmod +x $MONITOR_SCRIPT"
        )
        writeServiceScript()
    }

    private suspend fun writeServiceScript() {
        val svc = """#!/system/bin/sh
# Aether App Profile Service starter
pkill -f app_monitor.sh 2>/dev/null
nohup $MONITOR_SCRIPT &>/dev/null &
"""
        val escaped = svc.replace("'", "'\\''")
        RootUtils.sh(
            "printf '%s' '$escaped' > $SERVICE_SCRIPT",
            "chmod +x $SERVICE_SCRIPT"
        )
    }

    suspend fun startMonitor() = withContext(Dispatchers.IO) {
        RootUtils.sh(
            "pkill -f app_monitor.sh 2>/dev/null || true",
            "nohup $MONITOR_SCRIPT >/dev/null 2>&1 &"
        )
    }

    suspend fun stopMonitor() = withContext(Dispatchers.IO) {
        RootUtils.sh("pkill -f app_monitor.sh 2>/dev/null || true")
    }

    suspend fun isMonitorRunning(): Boolean {
        val out = RootUtils.sh("pgrep -f app_monitor.sh").stdout.trim()
        return out.isNotEmpty()
    }
}
