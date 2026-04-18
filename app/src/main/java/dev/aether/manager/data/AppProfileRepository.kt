package dev.aether.manager.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dev.aether.manager.util.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object AppProfileRepository {

    private const val PROFILE_DIR    = "${RootUtils.CONF_DIR}/app_profiles"
    private const val MONITOR_SCRIPT = "${RootUtils.CONF_DIR}/app_monitor.sh"
    private const val SERVICE_SCRIPT = "${RootUtils.CONF_DIR}/app_monitor_service.sh"

    // ── Public API ────────────────────────────────────────────────────────

    suspend fun loadUserApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        installed
            .filter { app ->
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystem = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                !isSystem || isUpdatedSystem
            }
            .filter { app -> pm.getLaunchIntentForPackage(app.packageName) != null }
            .map { app ->
                val label = try { pm.getApplicationLabel(app).toString() } catch (_: Exception) { app.packageName }
                val version = try { pm.getPackageInfo(app.packageName, 0).versionName ?: "" } catch (_: Exception) { "" }
                val icon = try { pm.getApplicationIcon(app.packageName) } catch (_: Exception) { null }
                AppInfo(
                    packageName = app.packageName,
                    label       = label,
                    versionName = version,
                    targetSdk   = app.targetSdkVersion,
                    isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
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
        } catch (_: Exception) { AppProfile(packageName) }
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
            put("package_name", profile.packageName)
            put("enabled",      profile.enabled)
            put("cpu_governor", profile.cpuGovernor)
            put("refresh_rate", profile.refreshRate)
            put("tweaks",       tweaks)
        }
        val content = json.toString()
        val path = "$PROFILE_DIR/${profile.packageName}.json"
        RootUtils.sh("mkdir -p $PROFILE_DIR && printf '%s' '${content.replace("'", "'\\''")}' > $path")
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

    suspend fun startMonitor() = withContext(Dispatchers.IO) {
        RootUtils.sh(
            "pkill -f app_monitor.sh 2>/dev/null || true",
            "nohup $MONITOR_SCRIPT >/dev/null 2>&1 &"
        )
    }

    suspend fun stopMonitor() = withContext(Dispatchers.IO) {
        RootUtils.sh("pkill -f app_monitor.sh 2>/dev/null || true")
    }

    suspend fun resetMonitor() = withContext(Dispatchers.IO) {
        RootUtils.sh("pkill -f app_monitor.sh 2>/dev/null || true")
    }

    suspend fun resetAllProfiles() = withContext(Dispatchers.IO) {
        // Stop monitor dulu sebelum hapus profile
        RootUtils.sh("pkill -f app_monitor.sh 2>/dev/null || true")
        // Hapus semua profile JSON
        RootUtils.sh("rm -f $PROFILE_DIR/*.json 2>/dev/null || true")
        // Hapus script monitor agar di-regenerate saat profile baru dibuat
        RootUtils.sh("rm -f $MONITOR_SCRIPT $SERVICE_SCRIPT 2>/dev/null || true")
    }

    suspend fun isMonitorRunning(): Boolean {
        val out = RootUtils.sh("pgrep -f app_monitor.sh").stdout.trim()
        return out.isNotEmpty()
    }

    // ── Script generation ─────────────────────────────────────────────────

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
                val gov    = j.optString("cpu_governor", "default")
                val rr     = j.optString("refresh_rate", "default")
                val tweaks = j.optJSONObject("tweaks") ?: JSONObject()

                val block = buildString {
                    appendLine("    \"$pkg\")")

                    // ── CPU Governor ──────────────────────────────────────
                    if (gov != "default") {
                        appendLine("      _apply_governor $gov")
                    }

                    // ── Refresh Rate ──────────────────────────────────────
                    if (rr != "default") {
                        appendLine("      _RR=$rr")
                        appendLine("      settings put system peak_refresh_rate \$_RR 2>/dev/null || true")
                        appendLine("      settings put system min_refresh_rate \$_RR 2>/dev/null || true")
                        appendLine("      service call SurfaceFlinger 1035 i32 \$_RR 2>/dev/null || true")
                        appendLine("      for _DSI in /sys/devices/platform/soc/soc:qcom,dsi-display* /sys/devices/platform/soc/*.dsi*; do")
                        appendLine("        [ -f \"\$_DSI/refresh_rate\" ] && echo \$_RR > \"\$_DSI/refresh_rate\" 2>/dev/null || true")
                        appendLine("        [ -f \"\$_DSI/dynamic_fps\" ]   && echo \$_RR > \"\$_DSI/dynamic_fps\" 2>/dev/null || true")
                        appendLine("      done")
                        appendLine("      [ -f /sys/class/display/panel0/max_refresh_rate ] && echo \$_RR > /sys/class/display/panel0/max_refresh_rate 2>/dev/null || true")
                        appendLine("      [ -f /sys/class/display/panel0/min_refresh_rate ] && echo \$_RR > /sys/class/display/panel0/min_refresh_rate 2>/dev/null || true")
                        appendLine("      for _DRM in /sys/class/drm/card*-DSI-*/; do")
                        appendLine("        [ -f \"\${_DRM}modes\" ]               && echo \$_RR > \"\${_DRM}modes\" 2>/dev/null || true")
                        appendLine("        [ -f \"\${_DRM}panel_refresh_rate\" ]   && echo \$_RR > \"\${_DRM}panel_refresh_rate\" 2>/dev/null || true")
                        appendLine("      done")
                    }

                    // ── Extra Tweaks ──────────────────────────────────────
                    if (tweaks.optBoolean("disable_doze", false)) {
                        appendLine("      dumpsys deviceidle disable 2>/dev/null || true")
                        appendLine("      dumpsys deviceidle step 2>/dev/null || true")
                    }
                    if (tweaks.optBoolean("lock_cpu_min", false)) {
                        appendLine("      for _f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_min_freq /sys/devices/system/cpu/cpufreq/policy*/scaling_min_freq; do")
                        appendLine("        [ -f \"\$_f\" ] || continue")
                        appendLine("        _MAX=\$(cat \"\${_f%min*}max_freq\" 2>/dev/null || cat \"\${_f/min/max}\" 2>/dev/null)")
                        appendLine("        [ -n \"\$_MAX\" ] && echo \$((_MAX/2)) > \"\$_f\" 2>/dev/null || true")
                        appendLine("      done")
                    }
                    if (tweaks.optBoolean("kill_background", false)) {
                        appendLine("      am kill-all 2>/dev/null || true")
                    }
                    if (tweaks.optBoolean("gpu_boost", false)) {
                        appendLine("      echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null || true")
                        appendLine("      echo 0 > /proc/gpufreq/gpufreq_opp_freq 2>/dev/null || true")
                        appendLine("      echo performance > /sys/kernel/gpu/gpu_policy 2>/dev/null || true")
                        appendLine("      for _gf in /sys/class/devfreq/*.gpu /sys/class/devfreq/gpufreq /sys/class/devfreq/ff9a0000.gpu /sys/class/devfreq/mali; do")
                        appendLine("        [ -d \"\$_gf\" ] && echo performance > \"\$_gf/governor\" 2>/dev/null || true")
                        appendLine("      done")
                        appendLine("      echo performance > /sys/devices/platform/mali/power_policy 2>/dev/null || true")
                    }
                    if (tweaks.optBoolean("io_latency", false)) {
                        appendLine("      for _blk in /sys/block/sda /sys/block/sdb /sys/block/mmcblk0 /sys/block/nvme0n1 /sys/block/dm-*; do")
                        appendLine("        [ -f \"\$_blk/queue/read_ahead_kb\" ] && echo 0 > \"\$_blk/queue/read_ahead_kb\" 2>/dev/null || true")
                        appendLine("        [ -f \"\$_blk/queue/scheduler\" ] && (echo deadline > \"\$_blk/queue/scheduler\" 2>/dev/null || echo noop > \"\$_blk/queue/scheduler\" 2>/dev/null || true)")
                        appendLine("      done")
                    }

                    appendLine("      ;;")
                }
                cases.append(block)
            } catch (_: Exception) {}
        }

        val script = """#!/system/bin/sh
# Aether App Profile Monitor — auto-generated, do not edit manually
# @AetherDev22
LAST=""
PROFILE_DIR=$PROFILE_DIR

# ── Snapshot default refresh rate sebelum monitor mulai ──────────────────────
# Dibaca sekali dari settings system; kalau kosong fallback ke sysfs atau 60.
_DEF_PEAK=${'$'}(settings get system peak_refresh_rate 2>/dev/null | grep -v null || echo "")
_DEF_MIN=${'$'}(settings get system min_refresh_rate 2>/dev/null | grep -v null || echo "")
# Fallback: baca dari sysfs kalau settings kosong
if [ -z "${'$'}_DEF_PEAK" ]; then
  _DEF_PEAK=${'$'}(cat /sys/class/display/panel0/default_refresh_rate \
                      /sys/class/display/panel0/max_refresh_rate \
                      2>/dev/null | head -1 | tr -d '[:space:]')
fi
[ -z "${'$'}_DEF_PEAK" ] && _DEF_PEAK="60"
[ -z "${'$'}_DEF_MIN"  ] && _DEF_MIN="${'$'}_DEF_PEAK"

# ── Universal governor helper ─────────────────────────────────────────────────
# Supports: per-cpu paths (legacy), per-policy paths (GKI/modern), MTK, Exynos
# Falls back gracefully when requested governor is unavailable on this chipset.
_apply_governor() {
  local _REQ="${'$'}1"

  # Collect available governors from policy0 or cpu0 (identical on all clusters)
  local _AVAIL
  _AVAIL=$(cat /sys/devices/system/cpu/cpufreq/policy0/scaling_available_governors \
               /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors \
               2>/dev/null | head -1)

  # Resolve the actual governor to write
  local _GOV
  if echo " ${'$'}_AVAIL " | grep -qiw "${'$'}_REQ"; then
    # Exact match (case-insensitive)
    _GOV=$(echo " ${'$'}_AVAIL " | tr ' ' '\n' | grep -im1 "^${'$'}_REQ${'$'}" | head -1)
    _GOV=${'$'}{_GOV:-${'$'}_REQ}
  else
    # Not available — resolve best equivalent
    case "${'$'}_REQ" in
      ondemand)
        # ondemand equivalent priority: schedutil > interactive > ondemand > conservative > first
        _GOV=$(echo "${'$'}_AVAIL" | tr ' ' '\n' | grep -im1 -xE 'schedutil|interactive|ondemand|conservative' \
               || echo "${'$'}_AVAIL" | awk '{print ${'$'}1}')
        ;;
      conservative)
        # conservative equivalent: schedutil > conservative > ondemand > interactive > first
        _GOV=$(echo "${'$'}_AVAIL" | tr ' ' '\n' | grep -im1 -xE 'schedutil|conservative|ondemand|interactive' \
               || echo "${'$'}_AVAIL" | awk '{print ${'$'}1}')
        ;;
      performance)
        # Try performance, fall back to schedutil
        _GOV=$(echo "${'$'}_AVAIL" | tr ' ' '\n' | grep -im1 -xE 'performance|schedutil' \
               || echo "${'$'}_AVAIL" | awk '{print ${'$'}1}')
        ;;
      powersave)
        # Try powersave, fall back to schedutil
        _GOV=$(echo "${'$'}_AVAIL" | tr ' ' '\n' | grep -im1 -xE 'powersave|schedutil' \
               || echo "${'$'}_AVAIL" | awk '{print ${'$'}1}')
        ;;
      *)
        _GOV=$(echo "${'$'}_AVAIL" | tr ' ' '\n' | grep -im1 -xE 'schedutil|ondemand|interactive' \
               || echo "${'$'}_AVAIL" | awk '{print ${'$'}1}')
        ;;
    esac
  fi

  [ -z "${'$'}_GOV" ] && return 0

  # 1. Per-policy paths — GKI kernels, Qualcomm SM8x50+, Dimensity 9xxx, Exynos 2xxx
  for _pol in /sys/devices/system/cpu/cpufreq/policy*/scaling_governor; do
    [ -f "${'$'}_pol" ] && echo "${'$'}_GOV" > "${'$'}_pol" 2>/dev/null || true
  done

  # 2. Per-cpu paths — legacy kernels, older Snapdragon, MediaTek Helio
  for _cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    [ -f "${'$'}_cpu" ] && echo "${'$'}_GOV" > "${'$'}_cpu" 2>/dev/null || true
  done

  # 3. MediaTek cpufreq sysfs (Dimensity 700/900/1000 series)
  for _mtk in /sys/devices/system/cpu/cpufreq/*/scaling_governor; do
    [ -f "${'$'}_mtk" ] && echo "${'$'}_GOV" > "${'$'}_mtk" 2>/dev/null || true
  done
}

# ── Default restore helper ────────────────────────────────────────────────────
_restore_default() {
  local _DEF
  _DEF=$(cat /sys/devices/system/cpu/cpufreq/policy0/scaling_available_governors \
             /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors \
             2>/dev/null | head -1 \
         | tr ' ' '\n' | grep -im1 -xE 'schedutil|ondemand|interactive' \
         || echo "schedutil")

  for _pol in /sys/devices/system/cpu/cpufreq/policy*/scaling_governor; do
    [ -f "${'$'}_pol" ] && echo "${'$'}_DEF" > "${'$'}_pol" 2>/dev/null || true
  done
  for _cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    [ -f "${'$'}_cpu" ] && echo "${'$'}_DEF" > "${'$'}_cpu" 2>/dev/null || true
  done
  for _mtk in /sys/devices/system/cpu/cpufreq/*/scaling_governor; do
    [ -f "${'$'}_mtk" ] && echo "${'$'}_DEF" > "${'$'}_mtk" 2>/dev/null || true
  done

  # ── Restore refresh rate ─────────────────────────────────────────────
  # Hapus override settings, lalu re-apply nilai default yang disimpan di awal
  settings delete system peak_refresh_rate 2>/dev/null || true
  settings delete system min_refresh_rate  2>/dev/null || true
  [ -n "${'$'}_DEF_PEAK" ] && settings put system peak_refresh_rate "${'$'}_DEF_PEAK" 2>/dev/null || true
  [ -n "${'$'}_DEF_MIN"  ] && settings put system min_refresh_rate  "${'$'}_DEF_MIN"  2>/dev/null || true

  # SurfaceFlinger — wajib di-reset, settings delete saja tidak cukup
  service call SurfaceFlinger 1035 i32 "${'$'}_DEF_PEAK" 2>/dev/null || true

  # sysfs panel nodes (Qualcomm/Samsung)
  [ -f /sys/class/display/panel0/max_refresh_rate ] && \
    echo "${'$'}_DEF_PEAK" > /sys/class/display/panel0/max_refresh_rate 2>/dev/null || true
  [ -f /sys/class/display/panel0/min_refresh_rate ] && \
    echo "${'$'}_DEF_MIN"  > /sys/class/display/panel0/min_refresh_rate 2>/dev/null || true

  # DRM connector nodes
  for _DRM in /sys/class/drm/card*-DSI-*/; do
    [ -f "${'$'}{_DRM}panel_refresh_rate" ] && \
      echo "${'$'}_DEF_PEAK" > "${'$'}{_DRM}panel_refresh_rate" 2>/dev/null || true
    [ -f "${'$'}{_DRM}modes" ] && \
      echo "${'$'}_DEF_PEAK" > "${'$'}{_DRM}modes" 2>/dev/null || true
  done

  # DSI display nodes (Qualcomm inline display)
  for _DSI in /sys/devices/platform/soc/soc:qcom,dsi-display* \
              /sys/devices/platform/soc/*.dsi*; do
    [ -f "${'$'}_DSI/refresh_rate" ] && \
      echo "${'$'}_DEF_PEAK" > "${'$'}_DSI/refresh_rate" 2>/dev/null || true
    [ -f "${'$'}_DSI/dynamic_fps" ] && \
      echo "${'$'}_DEF_PEAK" > "${'$'}_DSI/dynamic_fps" 2>/dev/null || true
  done

  for _gf in /sys/class/kgsl/kgsl-3d0/devfreq \
             /sys/class/devfreq/*.gpu \
             /sys/class/devfreq/gpufreq \
             /sys/class/devfreq/mali \
             /sys/devices/platform/mali; do
    [ -d "${'$'}_gf" ] && (echo msm-adreno-tz > "${'$'}_gf/governor" 2>/dev/null || \
                            echo simple_ondemand > "${'$'}_gf/governor" 2>/dev/null || true)
  done
}

apply_profile() {
  local pkg="${'$'}1"
  case "${'$'}pkg" in
${cases}
    *)
      _restore_default
      ;;
  esac
}

get_foreground_pkg() {
  local _line _pkg

  _line=${'$'}(dumpsys window windows 2>/dev/null | grep 'mCurrentFocus' | head -1)
  _pkg=${'$'}(echo "${'$'}_line" | sed 's/.*{ *[^ ]* [^ ]* //;s/[/ ].*//' | grep -E '^[a-zA-Z][a-zA-Z0-9_.]+\.[a-zA-Z]')
  [ -n "${'$'}_pkg" ] && { echo "${'$'}_pkg"; return 0; }

  _line=${'$'}(dumpsys activity activities 2>/dev/null | grep 'ResumedActivity' | head -1)
  _pkg=${'$'}(echo "${'$'}_line" | sed 's/.*{ *[^ ]* [^ ]* //;s/[/ ].*//' | grep -E '^[a-zA-Z][a-zA-Z0-9_.]+\.[a-zA-Z]')
  [ -n "${'$'}_pkg" ] && { echo "${'$'}_pkg"; return 0; }

  _line=${'$'}(dumpsys window windows 2>/dev/null | grep 'mFocusedApp' | head -1)
  _pkg=${'$'}(echo "${'$'}_line" | sed 's/.*{ *[^ ]* [^ ]* //;s/[/ ].*//' | grep -E '^[a-zA-Z][a-zA-Z0-9_.]+\.[a-zA-Z]')
  [ -n "${'$'}_pkg" ] && { echo "${'$'}_pkg"; return 0; }

  _pkg=${'$'}(dumpsys activity top 2>/dev/null | grep -E '^ {2}ACTIVITY ' | head -1 | awk '{print ${'$'}2}' | cut -d'/' -f1)
  [ -n "${'$'}_pkg" ] && { echo "${'$'}_pkg"; return 0; }

  _pkg=${'$'}(cmd window get-focused-app 2>/dev/null | grep -oE '[a-zA-Z][a-zA-Z0-9_.]+\.[a-zA-Z][a-zA-Z0-9_]+' | head -1)
  [ -n "${'$'}_pkg" ] && { echo "${'$'}_pkg"; return 0; }

  return 1
}

while true; do
  CURRENT=${'$'}(get_foreground_pkg)
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
nohup $MONITOR_SCRIPT >/dev/null 2>&1 &
"""
        val escaped = svc.replace("'", "'\\''")
        RootUtils.sh(
            "printf '%s' '$escaped' > $SERVICE_SCRIPT",
            "chmod +x $SERVICE_SCRIPT"
        )
    }
}