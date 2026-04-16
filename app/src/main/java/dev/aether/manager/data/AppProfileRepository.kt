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
                val icon = try { pm.getApplicationIcon(app.packageName) } catch (_: Exception) { null }
                AppInfo(
                    packageName = app.packageName,
                    label       = label,
                    versionName = version,
                    targetSdk   = target,
                    isSystemApp = isSystem,
                    icon        = icon,
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
                        // Validate governor exists on this device before applying
                        appendLine("      _GOV=$gov")
                        appendLine("      _AVAIL=\$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors 2>/dev/null)")
                        appendLine("      if echo \"\$_AVAIL\" | grep -qw \"\$_GOV\"; then")
                        appendLine("        for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do")
                        appendLine("          echo \"\$_GOV\" > \"\$cpu\" 2>/dev/null")
                        appendLine("        done")
                        appendLine("      else")
                        // fallback chain: schedutil → ondemand → interactive → first available
                        appendLine("        _FB=\$(echo \"\$_AVAIL\" | tr ' ' '\n' | grep -m1 -E '^(schedutil|ondemand|interactive)' || echo \"\$_AVAIL\" | awk '{print \$1}')")
                        appendLine("        for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do")
                        appendLine("          echo \"\$_FB\" > \"\$cpu\" 2>/dev/null")
                        appendLine("        done")
                        appendLine("      fi")
                    }
                    if (rr != "default") {
                        // Try multiple methods for refresh rate — ROM-agnostic
                        appendLine("      _RR=$rr")
                        // Method A: SurfaceFlinger binder call (varies by Android version)
                        appendLine("      service call SurfaceFlinger 1035 i32 \$_RR 2>/dev/null || true")
                        // Method B: Settings provider (works on most OEMs)
                        appendLine("      settings put system peak_refresh_rate \$_RR 2>/dev/null || true")
                        appendLine("      settings put system min_refresh_rate \$_RR 2>/dev/null || true")
                        // Method C: OEM-specific sysfs paths
                        appendLine("      for f in /sys/class/drm/*/modes /sys/kernel/gpu/gpu_max_clock; do [ -f \"\$f\" ] && true; done")
                        appendLine("      echo \$_RR > /sys/devices/platform/soc/soc:qcom,dsi-display*/refresh_rate 2>/dev/null || true")
                        appendLine("      echo \$_RR > /sys/class/display/panel0/max_refresh_rate 2>/dev/null || true")
                        appendLine("      echo \$_RR > /proc/drm/card0/max_refresh_rate 2>/dev/null || true")
                    }
                    if (tweaks.optBoolean("disable_doze", false)) {
                        appendLine("      dumpsys deviceidle disable 2>/dev/null || true")
                        appendLine("      dumpsys deviceidle step 2>/dev/null || true")
                    }
                    if (tweaks.optBoolean("lock_cpu_min", false)) {
                        // Lock min freq to 50% of max — universal path
                        appendLine("      for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_min_freq; do")
                        appendLine("        _MAX=\$(cat \"\${f%min*}max_freq\" 2>/dev/null || cat \"\${f/min/max}\" 2>/dev/null)")
                        appendLine("        [ -n \"\$_MAX\" ] && echo \$((_MAX/2)) > \"\$f\" 2>/dev/null")
                        appendLine("      done")
                    }
                    if (tweaks.optBoolean("kill_background", false)) {
                        appendLine("      am kill-all 2>/dev/null || true")
                    }
                    if (tweaks.optBoolean("gpu_boost", false)) {
                        // Qualcomm (kgsl)
                        appendLine("      echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null || true")
                        // MediaTek (gpufreq)
                        appendLine("      echo 0 > /proc/gpufreq/gpufreq_opp_freq 2>/dev/null || true")
                        appendLine("      echo performance > /sys/kernel/gpu/gpu_policy 2>/dev/null || true")
                        // Generic devfreq (Exynos / Unisoc / others)
                        appendLine("      for gf in /sys/class/devfreq/*.gpu /sys/class/devfreq/gpufreq /sys/class/devfreq/ff9a0000.gpu /sys/class/devfreq/mali; do")
                        appendLine("        [ -d \"\$gf\" ] && echo performance > \"\$gf/governor\" 2>/dev/null || true")
                        appendLine("      done")
                        // Mali GPU governor path (Exynos / Dimensity)
                        appendLine("      echo performance > /sys/devices/platform/mali/power_policy 2>/dev/null || true")
                    }
                    if (tweaks.optBoolean("io_latency", false)) {
                        // Apply to all block devices, not just sda
                        appendLine("      for blk in /sys/block/sda /sys/block/sdb /sys/block/mmcblk0 /sys/block/nvme0n1 /sys/block/dm-*; do")
                        appendLine("        [ -f \"\$blk/queue/read_ahead_kb\" ] && echo 0 > \"\$blk/queue/read_ahead_kb\" 2>/dev/null || true")
                        appendLine("        [ -f \"\$blk/queue/scheduler\" ] && (echo deadline > \"\$blk/queue/scheduler\" 2>/dev/null || echo noop > \"\$blk/queue/scheduler\" 2>/dev/null || true)")
                        appendLine("      done")
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
      # Restore defaults — pick best available governor
      _DEF=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors 2>/dev/null | tr ' ' '
' | grep -m1 -E '^(schedutil|ondemand|interactive)' || echo "ondemand")
      for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
        echo "${'$'}_DEF" > "${'$'}cpu" 2>/dev/null || true
      done
      # Restore refresh rate to device default
      settings put system peak_refresh_rate 0 2>/dev/null || true
      settings delete system min_refresh_rate 2>/dev/null || true
      # Restore GPU governor to default
      for gf in /sys/class/kgsl/kgsl-3d0/devfreq /sys/class/devfreq/*.gpu /sys/class/devfreq/gpufreq /sys/class/devfreq/mali /sys/devices/platform/mali; do
        [ -d "${'$'}gf" ] && echo msm-adreno-tz > "${'$'}gf/governor" 2>/dev/null ||                              echo simple_ondemand > "${'$'}gf/governor" 2>/dev/null || true
      done
      ;;
  esac
}

get_foreground_pkg() {
  # Method 1: mCurrentFocus (most reliable on most ROMs)
  local pkg
  pkg=$(dumpsys window windows 2>/dev/null | grep -E 'mCurrentFocus' | head -1 | grep -oE '[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]+)+' | head -1)
  [ -n "${'$'}pkg" ] && { echo "${'$'}pkg"; return; }

  # Method 2: ResumedActivity from activity manager
  pkg=$(dumpsys activity activities 2>/dev/null | grep -E 'ResumedActivity' | head -1 | grep -oE '[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]+)+' | head -1)
  [ -n "${'$'}pkg" ] && { echo "${'$'}pkg"; return; }

  # Method 3: mFocusedApp fallback
  pkg=$(dumpsys window windows 2>/dev/null | grep -E 'mFocusedApp' | head -1 | grep -oE '[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]+)+' | head -1)
  [ -n "${'$'}pkg" ] && { echo "${'$'}pkg"; return; }

  # Method 4: top process visible
  pkg=$(dumpsys activity top 2>/dev/null | grep 'TASK\|ACTIVITY' | grep -oE '[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]+)+' | head -1)
  echo "${'$'}pkg"
}

while true; do
  CURRENT=$(get_foreground_pkg)
  if [ -n "${'$'}CURRENT" ] && [ "${'$'}CURRENT" != "${'$'}LAST" ]; then
    LAST="${'$'}CURRENT"
    apply_profile "${'$'}CURRENT"
  fi
  sleep 1
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
