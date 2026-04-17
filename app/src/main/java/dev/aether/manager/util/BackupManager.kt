package dev.aether.manager.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * BackupManager — backup & restore semua config AetherManager ke/dari file JSON.
 *
 * Backup disimpan di: /data/local/tmp/aether/backups/<timestamp>.json
 * Format: { "tweaks": {...}, "profile": "balance", "safe_mode": false }
 *
 * Reset default: hapus tweaks.conf, profile → balance, hapus safe_mode.
 * Juga revert semua system tweak ke nilai stock (animasi 1.0, dns off, dll).
 */
object BackupManager {

    private const val BACKUP_DIR = "${RootUtils.CONF_DIR}/backups"

    data class BackupEntry(
        val filename : String,   // e.g. "20260417_143022.json"
        val timestamp: String,   // human-readable "17 Apr 2026, 14:30"
        val profile  : String,
    )

    // ── Backup ────────────────────────────────────────────────────────────

    /** Tulis snapshot config sekarang ke file JSON. Return nama file jika berhasil. */
    suspend fun createBackup(): String? = withContext(Dispatchers.IO) {
        val ts = RootUtils.sh("date +%Y%m%d_%H%M%S").stdout.trim()
        if (ts.isBlank()) return@withContext null
        val filename = "$ts.json"
        val dest = "$BACKUP_DIR/$filename"

        val script = """
            mkdir -p $BACKUP_DIR
            tweaks=$(cat ${RootUtils.TWEAKS_CONF} 2>/dev/null || echo "")
            profile=$(cat ${RootUtils.PROFILE_FILE} 2>/dev/null || echo "balance")
            safe=$([ -f ${RootUtils.SAFE_MODE_FILE} ] && echo true || echo false)
            # Encode tweaks.conf sebagai JSON object sederhana key=value → "key":"value"
            pairs=""
            while IFS='=' read -r k v; do
              [ -z "${'$'}k" ] && continue
              pairs="${'$'}pairs\"${'$'}k\":\"${'$'}v\","
            done <<< "${'$'}tweaks"
            pairs="${'$'}{pairs%,}"
            printf '{"tweaks":{%s},"profile":"%s","safe_mode":%s}\n' \
              "${'$'}pairs" "${'$'}profile" "${'$'}safe" > $dest
            echo ok
        """.trimIndent()

        val r = RootUtils.sh(script)
        if (r.stdout.contains("ok")) filename else null
    }

    /** Daftar backup tersedia, terbaru dulu. */
    suspend fun listBackups(): List<BackupEntry> = withContext(Dispatchers.IO) {
        val out = RootUtils.sh("ls -t $BACKUP_DIR/*.json 2>/dev/null").stdout
        out.lines()
            .filter { it.endsWith(".json") }
            .mapNotNull { path ->
                val name = path.substringAfterLast('/')
                val ts = name.removeSuffix(".json")   // "20260417_143022"
                val human = parseTimestamp(ts) ?: ts
                // Baca profile dari isi file
                val profileLine = RootUtils.sh(
                    "grep -o '\"profile\":\"[^\"]*\"' $path 2>/dev/null | head -1"
                ).stdout.trim()
                val profile = Regex("\"profile\":\"([^\"]+)\"")
                    .find(profileLine)?.groupValues?.get(1) ?: "balance"
                BackupEntry(filename = name, timestamp = human, profile = profile)
            }
    }

    /** Restore backup dari filename. */
    suspend fun restoreBackup(filename: String): Boolean = withContext(Dispatchers.IO) {
        val path = "$BACKUP_DIR/$filename"
        val script = """
            [ -f $path ] || exit 1
            content=$(cat $path)
            # Ekstrak tweaks block
            tweaks_block=$(echo "${'$'}content" | sed 's/.*"tweaks":{\(.*\)},.*/\1/')
            # Tulis ulang tweaks.conf
            rm -f ${RootUtils.TWEAKS_CONF}
            echo "${'$'}tweaks_block" | tr ',' '\n' | sed 's/["{} ]//g' | grep '.' >> ${RootUtils.TWEAKS_CONF}
            # Profile
            profile=$(echo "${'$'}content" | grep -o '"profile":"[^"]*"' | sed 's/"profile":"//;s/"//')
            [ -n "${'$'}profile" ] && echo "${'$'}profile" > ${RootUtils.PROFILE_FILE}
            # Safe mode
            safe=$(echo "${'$'}content" | grep -o '"safe_mode":[a-z]*' | sed 's/"safe_mode"://')
            if [ "${'$'}safe" = "true" ]; then touch ${RootUtils.SAFE_MODE_FILE}
            else rm -f ${RootUtils.SAFE_MODE_FILE}; fi
            echo ok
        """.trimIndent()

        val r = RootUtils.sh(script)
        r.stdout.contains("ok")
    }

    /** Hapus satu file backup. */
    suspend fun deleteBackup(filename: String): Boolean = withContext(Dispatchers.IO) {
        RootUtils.sh("rm -f $BACKUP_DIR/$filename").exitCode == 0
    }

    // ── Reset to default ──────────────────────────────────────────────────

    /**
     * Reset SEMUA tweak ke default + revert system values.
     * Dipanggil dari UI dan juga bisa dipanggil saat uninstall via service/broadcast.
     */
    suspend fun resetToDefaults(): Boolean = withContext(Dispatchers.IO) {
        val script = """
            # 1. Hapus semua config file
            rm -f ${RootUtils.TWEAKS_CONF} ${RootUtils.SAFE_MODE_FILE}
            echo balance > ${RootUtils.PROFILE_FILE}

            # 2. Revert CPU governor ke schedutil (default Android modern)
            for p in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
              [ -f "${'$'}p" ] && echo schedutil > "${'$'}p" 2>/dev/null || true
            done
            # Restore max freq
            for core in /sys/devices/system/cpu/cpu*/cpufreq; do
              max="${'$'}core/cpuinfo_max_freq"
              gov="${'$'}core/scaling_max_freq"
              [ -f "${'$'}max" ] && [ -f "${'$'}gov" ] && cat "${'$'}max" > "${'$'}gov" 2>/dev/null || true
            done

            # 3. CPU boost — off
            [ -f /sys/module/cpu_boost/parameters/input_boost_enabled ] && \
              echo 0 > /sys/module/cpu_boost/parameters/input_boost_enabled 2>/dev/null

            # 4. GPU throttle — restore on
            [ -f /sys/class/kgsl/kgsl-3d0/throttling ] && \
              echo 1 > /sys/class/kgsl/kgsl-3d0/throttling 2>/dev/null
            [ -f /sys/class/kgsl/kgsl-3d0/force_clk_on ] && \
              echo 0 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null

            # 5. LMK — biarkan kernel handle

            # 6. VM dirty — restore default kernel
            echo 20  > /proc/sys/vm/dirty_ratio 2>/dev/null
            echo 10  > /proc/sys/vm/dirty_background_ratio 2>/dev/null
            echo 3000 > /proc/sys/vm/dirty_expire_centisecs 2>/dev/null
            echo 500  > /proc/sys/vm/dirty_writeback_centisecs 2>/dev/null

            # 7. TCP — restore default
            [ -f /proc/sys/net/ipv4/tcp_congestion_control ] && \
              echo cubic > /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null
            [ -f /proc/sys/net/core/default_qdisc ] && \
              echo pfifo_fast > /proc/sys/net/core/default_qdisc 2>/dev/null

            # 8. DNS — off
            settings put global private_dns_mode off 2>/dev/null
            settings delete global private_dns_specifier 2>/dev/null

            # 9. Animation — restore 1.0
            settings put global window_animation_scale 1.0 2>/dev/null
            settings put global transition_animation_scale 1.0 2>/dev/null
            settings put global animator_duration_scale 1.0 2>/dev/null

            # 10. Doze — disable forced idle
            dumpsys deviceidle disable 2>/dev/null

            # 11. Sched boost — off
            [ -f /proc/sys/kernel/sched_boost ] && \
              echo 0 > /proc/sys/kernel/sched_boost 2>/dev/null

            echo ok
        """.trimIndent()

        val r = RootUtils.sh(script)
        r.stdout.contains("ok")
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun parseTimestamp(ts: String): String? {
        // "20260417_143022" → "17 Apr 2026, 14:30"
        if (ts.length < 13) return null
        val months = listOf("","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        return try {
            val day   = ts.substring(6, 8).trimStart('0')
            val month = months[ts.substring(4, 6).toInt()]
            val year  = ts.substring(0, 4)
            val hour  = ts.substring(9, 11)
            val min   = ts.substring(11, 13)
            "$day $month $year, $hour:$min"
        } catch (_: Exception) { null }
    }
}
