package dev.aether.manager.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * RootManager — Root state machine.
 *
 * Filosofi Magisk:
 * - Root TIDAK pernah di-request secara otomatis saat startup.
 * - Grant hanya dipicu oleh aksi user yang eksplisit (klik tombol).
 * - State root disimpan dan di-query tanpa side effect.
 *
 * PERUBAHAN: Hapus semua dependency ke NativeExec JNI.
 * requestRoot() sekarang murni Java ProcessBuilder (stdin-pipe + -c fallback).
 */
object RootManager {

    private const val TAG = "RootManager"

    @Volatile private var _cachedRoot: Boolean? = null

    val isRootGranted: Boolean get() = _cachedRoot == true
    val isRootUnknown: Boolean get() = _cachedRoot == null

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

    fun detectRootType(): String = when {
        File("/data/adb/ksu").exists()   -> "KernelSU"
        File("/data/adb/ap").exists()    -> "APatch"
        File("/data/adb/magisk").exists()-> "Magisk"
        else                             -> "Unknown"
    }

    suspend fun isRooted(): Boolean = withContext(Dispatchers.IO) {
        _cachedRoot ?: run {
            val result = silentCheck()
            _cachedRoot = result
            result
        }
    }

    private suspend fun silentCheck(): Boolean = withContext(Dispatchers.IO) {
        try {
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
     * Magisk: dialog muncul via `su -c` dengan timeout panjang (30s).
     * KernelSU/APatch: stdin-pipe langsung granted tanpa dialog.
     *
     * Urutan:
     * 1) su -c (trigger Magisk dialog + compatible dgn KSU/APatch)
     * 2) stdin fallback
     */
    suspend fun requestRoot(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "requestRoot() — rootType=${detectRootType()} su=$suBinary")
        _cachedRoot = null

        // Layer 1: su -c — paling reliable untuk trigger Magisk dialog
        val r1 = tryGrantArgC(timeoutMs = 30_000L)
        if (r1) {
            Log.d(TAG, "requestRoot: granted via -c")
            _cachedRoot = true
            return@withContext true
        }

        // Layer 2: stdin fallback
        val r2 = tryGrantStdin()
        if (r2) {
            Log.d(TAG, "requestRoot: granted via stdin")
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
            rt.join(15_000)
            val code = try { proc.waitFor() } catch (_: Exception) { -1 }
            proc.destroy()
            out.contains("aether_root_ok") && code == 0
        } catch (e: Exception) { false }
    }

    /**
     * @param timeoutMs waktu tunggu output — perlu panjang (>=30s) untuk Magisk
     *                  karena user harus sempat tap "Grant" di dialog.
     */
    private fun tryGrantArgC(timeoutMs: Long = 10_000L): Boolean {
        return try {
            val proc = ProcessBuilder(suBinary, "-c", "echo aether_root_ok")
                .redirectErrorStream(true)
                .start()
            var out = ""
            val rt = Thread { out = proc.inputStream.bufferedReader().readText() }
            rt.start()
            rt.join(timeoutMs)
            val code = try { proc.waitFor() } catch (_: Exception) { -1 }
            proc.destroy()
            out.contains("aether_root_ok") && code == 0
        } catch (e: Exception) { false }
    }

    fun clearCache() { _cachedRoot = null }
    fun markGranted() { _cachedRoot = true }
    fun markDenied()  { _cachedRoot = false }
}
