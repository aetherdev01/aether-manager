package dev.aether.manager.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * RootUtils — utilitas root shell untuk AetherManager.
 *
 * Semua eksekusi shell melalui NativeExec (pure Java ProcessBuilder).
 * FIX: getMonitorState() & writeTweakConf() sekarang pakai single
 *      heredoc/inline-script agar variable shell persist dalam satu sesi.
 */
object RootUtils {

    const val CONF_DIR        = "/data/local/tmp/aether"
    const val TWEAKS_CONF     = "$CONF_DIR/tweaks.conf"
    const val PROFILE_FILE    = "$CONF_DIR/profile"
    const val SAFE_MODE_FILE  = "$CONF_DIR/safe_mode"
    const val BOOT_COUNT_FILE = "$CONF_DIR/boot_count"

    data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String)

    // ── Root check ────────────────────────────────────────────────────────
    suspend fun hasRoot(): Boolean = RootManager.isRooted()

    // ── Core shell exec ───────────────────────────────────────────────────
    // Semua command digabung menjadi SATU sesi shell agar variable persist.

    suspend fun sh(script: String): ShellResult = withContext(Dispatchers.IO) {
        val fullScript = "mkdir -p $CONF_DIR\n$script"
        val r = NativeExec.exec(fullScript)
        ShellResult(r.exitCode, r.stdout, r.stderr)
    }

    /** Legacy array overload — gabung jadi satu script */
    suspend fun sh(vararg cmds: String): ShellResult =
        sh(cmds.joinToString("\n"))

    // ── File utilities ────────────────────────────────────────────────────

    suspend fun readFile(path: String): String =
        sh("cat $path 2>/dev/null").stdout

    suspend fun writeFile(path: String, content: String): Boolean {
        val escaped = content.replace("'", "'\\''")
        return sh("printf '%s' '$escaped' > $path").exitCode == 0
    }

    suspend fun fileExists(path: String): Boolean =
        sh("[ -f $path ] && echo yes").stdout.contains("yes")

    suspend fun getProp(key: String): String =
        NativeExec.output("getprop $key 2>/dev/null")

    // ── Device info ───────────────────────────────────────────────────────

    suspend fun getDeviceInfo(): DeviceInfo {
        // Semua dalam satu script — variable persist
        val script = """
            model=$(getprop ro.product.model 2>/dev/null | head -c 40)
            android=$(getprop ro.build.version.release 2>/dev/null)
            platform=$(getprop ro.board.platform 2>/dev/null)
            hardware=$(getprop ro.hardware 2>/dev/null)
            soc_model=$(getprop ro.soc.model 2>/dev/null)
            kernel=$(uname -r 2>/dev/null | head -c 50)
            selinux=$(getenforce 2>/dev/null)
            if [ -d /data/adb/ksu ]; then root=KernelSU
            elif [ -d /data/adb/ap ]; then root=APatch
            else root=Magisk; fi
            profile=$(cat $PROFILE_FILE 2>/dev/null || echo balance)
            safe=$([ -f $SAFE_MODE_FILE ] && echo 1 || echo 0)
            boot=$(cat $BOOT_COUNT_FILE 2>/dev/null || echo 0)
            echo model=${'$'}model
            echo android=${'$'}android
            echo platform=${'$'}platform
            echo hardware=${'$'}hardware
            echo soc_model=${'$'}soc_model
            echo kernel=${'$'}kernel
            echo selinux=${'$'}selinux
            echo root=${'$'}root
            echo profile=${'$'}profile
            echo safe=${'$'}safe
            echo boot=${'$'}boot
        """.trimIndent()

        val result = sh(script)
        val map = parseKv(result.stdout)
        val platform = "${map["platform"]} ${map["hardware"]} ${map["soc_model"]}".lowercase()
        return DeviceInfo(
            model     = map["model"]    ?: "Unknown Device",
            android   = map["android"]  ?: "?",
            kernel    = map["kernel"]   ?: "?",
            selinux   = map["selinux"]  ?: "?",
            rootType  = map["root"]     ?: RootManager.detectRootType(),
            soc       = detectSoc(platform),
            socRaw    = map["platform"] ?: "",
            pid       = "",
            profile   = map["profile"]  ?: "balance",
            safeMode  = map["safe"] == "1",
            bootCount = map["boot"]?.toIntOrNull() ?: 0
        )
    }

    private fun detectSoc(platform: String): SocType = when {
        Regex("sm\\d|msm|qcom|snapdragon|sdm|lahaina|shima|taro|kalama|crow|parrot|neo|bengal|khaje|qualcomm|kryo|krait")
            .containsMatchIn(platform) -> SocType.SNAPDRAGON
        Regex("mt\\d|mediatek|helio|dimensity")
            .containsMatchIn(platform) -> SocType.MEDIATEK
        Regex("exynos|universal|s5e")
            .containsMatchIn(platform) -> SocType.EXYNOS
        Regex("kirin|hi36|hi37|emily|monica")
            .containsMatchIn(platform) -> SocType.KIRIN
        else -> SocType.OTHER
    }

    // ── Tweaks config R/W ─────────────────────────────────────────────────

    suspend fun readTweaksConf(): Map<String, String> =
        parseKv(readFile(TWEAKS_CONF))

    /**
     * FIX: if/else harus dalam satu script, bukan array command terpisah.
     */
    suspend fun writeTweakConf(key: String, value: String): Boolean {
        val script = """
            if grep -q '^$key=' $TWEAKS_CONF 2>/dev/null; then
              sed -i 's|^$key=.*|$key=$value|' $TWEAKS_CONF
            else
              echo '$key=$value' >> $TWEAKS_CONF
            fi
        """.trimIndent()
        return sh(script).exitCode == 0
    }

    suspend fun setProfile(profile: String): Boolean = writeFile(PROFILE_FILE, profile)

    // ── Apply tweaks — real-time, tanpa reboot ────────────────────────────

    suspend fun applyTweaksDirect(tweaks: Map<String, String>): Boolean {
        val sb = StringBuilder()

        val profile = tweaks["profile"] ?: "balance"
        val governor = when (profile) {
            "performance" -> "performance"
            "battery"     -> "powersave"
            else          -> "schedutil"
        }
        sb.appendLine("for p in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do [ -f \$p ] && echo $governor > \$p 2>/dev/null; done")

        if (profile == "gaming") {
            sb.appendLine("for p in /sys/devices/system/cpu/cpu*/cpufreq/scaling_min_freq; do [ -f \$p ] && cat \${p%min_freq}cpuinfo_max_freq > \$p 2>/dev/null; done")
        }
        if (profile == "battery") {
            sb.appendLine("for p in /sys/devices/system/cpu/cpu*/cpufreq/scaling_max_freq; do [ -f \$p ] && echo 1516800 > \$p 2>/dev/null; done")
        }

        val schedVal = if (tweaks["schedboost"] == "1") "1" else "0"
        sb.appendLine("[ -f /proc/sys/kernel/sched_boost ] && echo $schedVal > /proc/sys/kernel/sched_boost 2>/dev/null")

        if (tweaks["cpu_boost"] == "1") {
            sb.appendLine("[ -f /sys/module/cpu_boost/parameters/input_boost_enabled ] && echo 1 > /sys/module/cpu_boost/parameters/input_boost_enabled 2>/dev/null")
            sb.appendLine("[ -f /sys/module/cpu_boost/parameters/input_boost_freq ] && echo '0:1324800 1:1324800 2:1324800 3:1324800' > /sys/module/cpu_boost/parameters/input_boost_freq 2>/dev/null")
        } else {
            sb.appendLine("[ -f /sys/module/cpu_boost/parameters/input_boost_enabled ] && echo 0 > /sys/module/cpu_boost/parameters/input_boost_enabled 2>/dev/null")
        }

        if (tweaks["gpu_throttle_off"] == "1") {
            sb.appendLine("[ -f /sys/class/kgsl/kgsl-3d0/throttling ] && echo 0 > /sys/class/kgsl/kgsl-3d0/throttling 2>/dev/null")
            sb.appendLine("[ -f /sys/class/kgsl/kgsl-3d0/force_clk_on ] && echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null")
        } else {
            sb.appendLine("[ -f /sys/class/kgsl/kgsl-3d0/throttling ] && echo 1 > /sys/class/kgsl/kgsl-3d0/throttling 2>/dev/null")
        }

        if (tweaks["cpuset_opt"] == "1") {
            sb.appendLine("[ -d /dev/cpuset/top-app ] && echo '4-7' > /dev/cpuset/top-app/cpus 2>/dev/null")
            sb.appendLine("[ -d /dev/cpuset/foreground ] && echo '0-7' > /dev/cpuset/foreground/cpus 2>/dev/null")
            sb.appendLine("[ -d /dev/cpuset/background ] && echo '0-1' > /dev/cpuset/background/cpus 2>/dev/null")
        }

        if (tweaks["obb_noop"] == "1") {
            sb.appendLine("[ -f /sys/block/dm-0/queue/scheduler ] && echo 'none' > /sys/block/dm-0/queue/scheduler 2>/dev/null")
            sb.appendLine("[ -f /sys/block/dm-0/queue/read_ahead_kb ] && echo 2048 > /sys/block/dm-0/queue/read_ahead_kb 2>/dev/null")
        }

        if (tweaks["lmk_aggressive"] == "1") {
            sb.appendLine("[ -f /sys/module/lowmemorykiller/parameters/minfree ] && echo '18432,23040,27648,32256,55296,80640' > /sys/module/lowmemorykiller/parameters/minfree 2>/dev/null")
            sb.appendLine("[ -f /sys/module/lowmemorykiller/parameters/adj ] && echo '0,100,200,300,900,906' > /sys/module/lowmemorykiller/parameters/adj 2>/dev/null")
        }

        if (tweaks["zram"] == "1") {
            val size = tweaks["zram_size"] ?: "1073741824"
            val algo = tweaks["zram_algo"] ?: "lz4"
            sb.appendLine("swapoff /dev/zram0 2>/dev/null; echo 1 > /sys/block/zram0/reset 2>/dev/null")
            sb.appendLine("[ -f /sys/block/zram0/comp_algorithm ] && echo $algo > /sys/block/zram0/comp_algorithm 2>/dev/null")
            sb.appendLine("echo $size > /sys/block/zram0/disksize 2>/dev/null && mkswap /dev/zram0 2>/dev/null && swapon /dev/zram0 2>/dev/null")
        }

        if (tweaks["vm_dirty_opt"] == "1") {
            sb.appendLine("echo 10 > /proc/sys/vm/dirty_ratio 2>/dev/null")
            sb.appendLine("echo 5 > /proc/sys/vm/dirty_background_ratio 2>/dev/null")
            sb.appendLine("echo 50 > /proc/sys/vm/dirty_expire_centisecs 2>/dev/null")
            sb.appendLine("echo 25 > /proc/sys/vm/dirty_writeback_centisecs 2>/dev/null")
        }

        val ioScheduler = tweaks["io_scheduler"] ?: ""
        if (ioScheduler.isNotBlank()) {
            sb.appendLine("for dev in /sys/block/*/queue/scheduler; do [ -f \$dev ] && echo $ioScheduler > \$dev 2>/dev/null; done")
        }

        if (tweaks["io_latency_opt"] == "1") {
            sb.appendLine("for dev in /sys/block/*/queue/read_ahead_kb; do [ -f \$dev ] && echo 512 > \$dev 2>/dev/null; done")
            sb.appendLine("for dev in /sys/block/*/queue/add_random; do [ -f \$dev ] && echo 0 > \$dev 2>/dev/null; done")
            sb.appendLine("for dev in /sys/block/*/queue/rq_affinity; do [ -f \$dev ] && echo 2 > \$dev 2>/dev/null; done")
        }

        if (tweaks["tcp_bbr"] == "1") {
            sb.appendLine("[ -f /proc/sys/net/ipv4/tcp_congestion_control ] && echo bbr > /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null")
            sb.appendLine("[ -f /proc/sys/net/core/default_qdisc ] && echo fq > /proc/sys/net/core/default_qdisc 2>/dev/null")
        }

        if (tweaks["net_buffer"] == "1") {
            sb.appendLine("echo 4096 87380 16777216 > /proc/sys/net/ipv4/tcp_rmem 2>/dev/null")
            sb.appendLine("echo 4096 65536 16777216 > /proc/sys/net/ipv4/tcp_wmem 2>/dev/null")
            sb.appendLine("echo 16777216 > /proc/sys/net/core/rmem_max 2>/dev/null")
            sb.appendLine("echo 16777216 > /proc/sys/net/core/wmem_max 2>/dev/null")
        }

        if (tweaks["doh"] == "1") {
            sb.appendLine("settings put global private_dns_mode hostname 2>/dev/null")
            sb.appendLine("settings put global private_dns_specifier dns.google 2>/dev/null")
        } else {
            sb.appendLine("settings put global private_dns_mode off 2>/dev/null")
            sb.appendLine("settings delete global private_dns_specifier 2>/dev/null")
        }

        if (tweaks["doze"] == "1") {
            sb.appendLine("dumpsys deviceidle enable deep 2>/dev/null")
            sb.appendLine("dumpsys deviceidle force-idle deep 2>/dev/null")
        }

        if (tweaks["fast_anim"] == "1") {
            sb.appendLine("settings put global window_animation_scale 0.5 2>/dev/null")
            sb.appendLine("settings put global transition_animation_scale 0.5 2>/dev/null")
            sb.appendLine("settings put global animator_duration_scale 0.5 2>/dev/null")
        } else {
            sb.appendLine("settings put global window_animation_scale 1.0 2>/dev/null")
            sb.appendLine("settings put global transition_animation_scale 1.0 2>/dev/null")
            sb.appendLine("settings put global animator_duration_scale 1.0 2>/dev/null")
        }

        if (tweaks["entropy_boost"] == "1") {
            sb.appendLine("[ -f /proc/sys/kernel/random/write_wakeup_threshold ] && echo 256 > /proc/sys/kernel/random/write_wakeup_threshold 2>/dev/null")
        }

        if (tweaks["clear_cache"] == "1") {
            sb.appendLine("sync && echo 3 > /proc/sys/vm/drop_caches 2>/dev/null")
            sb.appendLine("pm trim-caches 0 2>/dev/null")
        }

        return sh(sb.toString()).exitCode == 0
    }

    suspend fun setProfileDirect(profile: String): Boolean {
        writeFile(PROFILE_FILE, profile)
        val tweaks = readTweaksConf().toMutableMap()
        tweaks["profile"] = profile
        return applyTweaksDirect(tweaks)
    }

    // ── Safe mode ─────────────────────────────────────────────────────────

    suspend fun toggleSafeMode(enable: Boolean): Boolean =
        if (enable) sh("touch $SAFE_MODE_FILE").exitCode == 0
        else        sh("rm -f $SAFE_MODE_FILE").exitCode == 0

    // ── Reboot ────────────────────────────────────────────────────────────

    suspend fun reboot(mode: RebootMode = RebootMode.NORMAL): Boolean =
        sh(when (mode) {
            RebootMode.NORMAL     -> "reboot"
            RebootMode.RECOVERY   -> "reboot recovery"
            RebootMode.BOOTLOADER -> "reboot bootloader"
        }).exitCode == 0

    enum class RebootMode { NORMAL, RECOVERY, BOOTLOADER }

    // ── Monitor state — FIX: semua dalam satu script, variable persist ────

    suspend fun getMonitorState(): dev.aether.manager.data.MonitorState =
        withContext(Dispatchers.IO) {
            // FIX: Script satu blok. Variable cpu1/cpu2/idle/total semuanya
            //      persist dalam satu sh session. Tidak ada escaped-awk-quotes.
            val script = """
                # CPU usage — delta /proc/stat
                read -r cpu1 < /proc/stat
                sleep 0.3
                read -r cpu2 < /proc/stat
                set -- ${'$'}cpu1
                shift
                t1=0; i1=${'$'}5
                for v in ${'$'}@; do t1=${'$'}((t1+v)); done
                set -- ${'$'}cpu2
                shift
                t2=0; i2=${'$'}5
                for v in ${'$'}@; do t2=${'$'}((t2+v)); done
                dt=${'$'}((t2-t1)); di=${'$'}((i2-i1))
                echo cpu_usage=${'$'}(( dt > 0 ? (dt-di)*100/dt : 0 ))

                # CPU freq (kHz → MHz, tanpa awk float)
                cf=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq 2>/dev/null || echo 0)
                echo cpu_freq=${'$'}((cf/1000))

                # GPU
                echo gpu_usage=$(cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage 2>/dev/null | tr -d '% ' || echo 0)
                gf=$(cat /sys/class/kgsl/kgsl-3d0/gpuclk 2>/dev/null || echo 0)
                echo gpu_freq=${'$'}((gf/1000000))

                # RAM (kB)
                mem_total=$(grep MemTotal /proc/meminfo | awk '{print $2}')
                mem_avail=$(grep MemAvailable /proc/meminfo | awk '{print $2}')
                echo ram_total_mb=${'$'}((mem_total/1024))
                echo ram_used_mb=${'$'}(((mem_total-mem_avail)/1024))

                # Temps
                echo cpu_temp=$(cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null || echo 0)
                echo bat_temp=$(cat /sys/class/power_supply/battery/temp 2>/dev/null || echo 0)
                echo bat_level=$(cat /sys/class/power_supply/battery/capacity 2>/dev/null || echo 0)

                # Governor
                echo cpu_gov=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || echo unknown)

                # Swap
                sw_total=$(grep SwapTotal /proc/meminfo | awk '{print $2}')
                sw_free=$(grep SwapFree /proc/meminfo | awk '{print $2}')
                echo swap_total_mb=${'$'}((sw_total/1024))
                echo swap_used_mb=${'$'}(((sw_total-sw_free)/1024))

                # Storage (KB)
                df_line=$(df /data 2>/dev/null | tail -1)
                echo storage_used_kb=$(echo ${'$'}df_line | awk '{print $3}')
                echo storage_total_kb=$(echo ${'$'}df_line | awk '{print $2}')

                # Uptime
                up=$(awk '{printf "%d", $1}' /proc/uptime)
                echo uptime=${'$'}((up/3600))h_${'$'}((up%3600/60))m
            """.trimIndent()

            val result = sh(script)
            val map = parseKv(result.stdout)

            val cpuTempRaw = map["cpu_temp"]?.toLongOrNull() ?: 0L
            val batTempRaw = map["bat_temp"]?.toLongOrNull() ?: 0L

            // cpu_freq & gpu_freq sudah dalam MHz (integer)
            val cpuFreqMhz = map["cpu_freq"]?.toLongOrNull() ?: 0L
            val gpuFreqMhz = map["gpu_freq"]?.toLongOrNull() ?: 0L

            dev.aether.manager.data.MonitorState(
                cpuUsage       = map["cpu_usage"]?.toIntOrNull()     ?: 0,
                cpuFreq        = if (cpuFreqMhz > 0) "$cpuFreqMhz MHz" else "",
                gpuUsage       = map["gpu_usage"]?.toIntOrNull()     ?: 0,
                gpuFreq        = if (gpuFreqMhz > 0) "$gpuFreqMhz MHz" else "",
                ramUsedMb      = map["ram_used_mb"]?.toLongOrNull()  ?: 0L,
                ramTotalMb     = map["ram_total_mb"]?.toLongOrNull() ?: 0L,
                cpuTemp        = if (cpuTempRaw > 200) cpuTempRaw / 1000f else cpuTempRaw.toFloat(),
                batTemp        = if (batTempRaw > 200) batTempRaw / 10f  else batTempRaw.toFloat(),
                storageUsedGb  = (map["storage_used_kb"]?.toLongOrNull()  ?: 0L) / 1_048_576f,
                storageTotalGb = (map["storage_total_kb"]?.toLongOrNull() ?: 0L) / 1_048_576f,
                uptime         = (map["uptime"] ?: "").replace("_", " "),
                batLevel       = map["bat_level"]?.toIntOrNull() ?: 0,
                cpuGovernor    = map["cpu_gov"] ?: "",
                swapUsedMb     = map["swap_used_mb"]?.toLongOrNull()  ?: 0L,
                swapTotalMb    = map["swap_total_mb"]?.toLongOrNull() ?: 0L,
            )
        }

    // ── Private helpers ───────────────────────────────────────────────────

    private fun parseKv(raw: String): Map<String, String> =
        raw.lines().associate { line ->
            val i = line.indexOf('=')
            if (i > 0) line.substring(0, i).trim() to line.substring(i + 1).trim()
            else line.trim() to ""
        }
}

// ── Data classes ──────────────────────────────────────────────────────────────

data class DeviceInfo(
    val model     : String,
    val android   : String,
    val kernel    : String,
    val selinux   : String,
    val rootType  : String,
    val soc       : SocType,
    val socRaw    : String,
    val pid       : String,
    val profile   : String,
    val safeMode  : Boolean,
    val bootCount : Int
)

enum class SocType(val label: String) {
    SNAPDRAGON("Snapdragon"),
    MEDIATEK  ("MediaTek"),
    EXYNOS    ("Exynos"),
    KIRIN     ("Kirin"),
    OTHER     ("Universal")
}
