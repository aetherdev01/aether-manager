package dev.aether.manager.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * RootManager — Root state machine terinspirasi Magisk.
 *
 * Filosofi Magisk:
 * - Root TIDAK pernah di-request secara otomatis saat startup.
 * - Grant hanya dipicu oleh aksi user yang eksplisit (klik tombol).
 * - State root disimpan dan di-query tanpa side effect.
 * - isRooted = read-only query; requestRoot() = one-shot trigger berharap grant.
 *
 * Lifecycle:
 *   SplashActivity  → cek setup_done, tidak menyentuh su sama sekali
 *   SetupActivity   → user klik "Grant Root" → requestRoot() dipanggil
 *   MainActivity    → hanya memakai cachedRoot (hasil dari Setup)
 */
object RootManager {

    private const val TAG = "RootManager"

    // ── Root state (mirip Magisk Info.isRooted) ────────────────────────────
    // null  = belum dicheck (sebelum Setup)
    // false = denied / tidak tersedia
    // true  = granted dan terverifikasi
    @Volatile private var _cachedRoot: Boolean? = null

    /** Apakah root sudah dikonfirmasi granted (tanpa trigger su baru) */
    val isRootGranted: Boolean get() = _cachedRoot == true

    /** Belum pernah dicek sama sekali */
    val isRootUnknown: Boolean get() = _cachedRoot == null

    // ── Su binary resolver (sama persis dengan Magisk pattern) ────────────
    val suBinary: String by lazy {
        listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/magisk/.core/bin/su",
            "su"
        ).firstOrNull { path ->
            if (path == "su") true
            else File(path).let { it.exists() && it.canExecute() }
        } ?: "su"
    }

    // ── Detect root manager type (Magisk / KSU / APatch) ──────────────────
    fun detectRootType(): String = when {
        File("/data/adb/ksu").exists()  -> "KernelSU"
        File("/data/adb/ap").exists()   -> "APatch"
        File("/data/adb/magisk").exists()-> "Magisk"
        else                             -> "Unknown"
    }

    // ── READ-ONLY: check apakah su sudah granted TANPA memunculkan dialog ─
    // Ini mirip Magisk Shell.isAppGranted() — non-blocking read dari cache.
    // Hanya dipakai oleh MainActivity setelah Setup selesai.
    suspend fun isRooted(): Boolean = withContext(Dispatchers.IO) {
        _cachedRoot ?: run {
            // Kalau cache kosong, coba silent check (tidak memunculkan dialog
            // di Magisk karena package sudah pernah di-grant via Setup)
            val result = silentCheck()
            _cachedRoot = result
            result
        }
    }

    /**
     * Silent root check — hanya baca hasil grant yang sudah ada.
     * TIDAK memunculkan dialog baru di SU manager.
     * Dipakai setelah Setup selesai untuk re-verify.
     */
    private suspend fun silentCheck(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Stdin-pipe method: kompatibel Magisk, KSU, APatch
            val proc = ProcessBuilder(suBinary)
                .redirectErrorStream(false)
                .start()

            val writeThread = Thread {
                try {
                    proc.outputStream.bufferedWriter().use {
                        it.write("echo aether_root_ok\nexit\n")
                    }
                } catch (_: Exception) {}
            }
            writeThread.start()

            var out = ""
            val readThread = Thread {
                out = proc.inputStream.bufferedReader().readText()
            }
            readThread.start()
            writeThread.join(3_000)
            readThread.join(5_000)
            val code = try { proc.waitFor() } catch (_: Exception) { -1 }
            proc.destroy()

            val granted = out.contains("aether_root_ok") && code == 0
            Log.d(TAG, "silentCheck: granted=$granted su=$suBinary")
            granted
        } catch (e: Exception) {
            Log.w(TAG, "silentCheck failed: ${e.message}")
            false
        }
    }

    /**
     * REQUEST root secara eksplisit — HANYA dipanggil dari SetupActivity
     * saat user klik tombol "Grant Root Access".
     *
     * Mirip Magisk SuRequestHandler: menunggu user grant di dialog,
     * lalu verify hasilnya.
     *
     * Return: true kalau user ALLOW, false kalau DENY / timeout.
     */
    suspend fun requestRoot(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "requestRoot() dipanggil — menunggu user grant di SU manager...")
        _cachedRoot = null // reset cache sebelum request baru

        // Layer 1: stdin-pipe (disukai KSU & APatch)
        val r1 = tryGrantStdin()
        if (r1) {
            Log.d(TAG, "requestRoot: granted via stdin")
            _cachedRoot = true
            return@withContext true
        }

        // Layer 2: -c flag (beberapa su klasik & Magisk old)
        val r2 = tryGrantArgC()
        if (r2) {
            Log.d(TAG, "requestRoot: granted via -c")
            _cachedRoot = true
            return@withContext true
        }

        // Layer 3: NativeExec fallback
        val r3 = try { NativeExec.nativeAvailable && NativeExec.nHasRoot() } catch (_: Exception) { false }
        if (r3) {
            Log.d(TAG, "requestRoot: granted via native")
            _cachedRoot = true
            return@withContext true
        }

        Log.w(TAG, "requestRoot: semua layer failed — DENIED")
        _cachedRoot = false
        false
    }

    private fun tryGrantStdin(): Boolean {
        return try {
            val proc = ProcessBuilder(suBinary)
                .redirectErrorStream(false)
                .start()
            val wt = Thread {
                try { proc.outputStream.bufferedWriter().use { it.write("echo aether_root_ok\nexit\n") } }
                catch (_: Exception) {}
            }
            wt.start()
            var out = ""
            val rt = Thread { out = proc.inputStream.bufferedReader().readText() }
            rt.start()
            wt.join(5_000)
            rt.join(15_000) // lebih lama — user perlu waktu tap dialog
            val code = try { proc.waitFor() } catch (_: Exception) { -1 }
            proc.destroy()
            out.contains("aether_root_ok") && code == 0
        } catch (e: Exception) { false }
    }

    private fun tryGrantArgC(): Boolean {
        return try {
            val proc = ProcessBuilder(suBinary, "-c", "echo aether_root_ok")
                .redirectErrorStream(true)
                .start()
            var out = ""
            val rt = Thread { out = proc.inputStream.bufferedReader().readText() }
            rt.start()
            rt.join(10_000)
            val code = try { proc.waitFor() } catch (_: Exception) { -1 }
            proc.destroy()
            out.contains("aether_root_ok") && code == 0
        } catch (e: Exception) { false }
    }

    /** Reset cache (misal: setelah unroot atau debug) */
    fun clearCache() { _cachedRoot = null }

    /** Paksa set state (dari hasil Setup yang sudah terbukti granted) */
    fun markGranted() { _cachedRoot = true }
    fun markDenied()  { _cachedRoot = false }
}
