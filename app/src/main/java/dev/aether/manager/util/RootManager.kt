package dev.aether.manager.util

import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * RootManager — Root state machine menggunakan libsu.
 *
 * libsu menangani Magisk dialog grant secara proper — tidak ada deadlock,
 * tidak perlu spawn/block manual. Shell.getShell() akan trigger dialog
 * Magisk/KernelSU/APatch otomatis saat pertama kali dipanggil.
 */
object RootManager {

    private const val TAG = "RootManager"

    @Volatile private var _cachedRoot: Boolean? = null

    val isRootGranted: Boolean get() = _cachedRoot == true
    val isRootUnknown: Boolean get() = _cachedRoot == null

    fun detectRootType(): String {
        return when {
            File("/data/adb/magisk").canRead()   -> "Magisk"
            File("/sbin/.magisk").exists()        -> "Magisk"
            File("/dev/.magisk.unblock").exists() -> "Magisk"
            File("/data/adb/ksu").canRead()       -> "KernelSU"
            File("/data/adb/ap").canRead()        -> "APatch"
            _cachedRoot == true                   -> detectRootTypeViaShell()
            else                                  -> "Unknown"
        }
    }

    private fun detectRootTypeViaShell(): String {
        return try {
            val result = Shell.cmd(
                "if [ -d /data/adb/ksu ]; then echo KernelSU",
                "elif [ -d /data/adb/ap ]; then echo APatch",
                "elif [ -d /data/adb/magisk ]; then echo Magisk",
                "else echo Unknown; fi"
            ).exec()
            val out = result.out.joinToString("").trim()
            Log.d(TAG, "detectRootTypeViaShell: $out")
            out.ifBlank { "Unknown" }
        } catch (e: Exception) {
            Log.w(TAG, "detectRootTypeViaShell failed: ${e.message}")
            "Unknown"
        }
    }

    suspend fun isRooted(): Boolean = withContext(Dispatchers.IO) {
        _cachedRoot ?: run {
            val result = silentCheck()
            _cachedRoot = result
            result
        }
    }

    private fun silentCheck(): Boolean {
        return try {
            // Cek dulu lewat isAppGrantedRoot() (non-blocking, tidak trigger dialog)
            val quickCheck = Shell.isAppGrantedRoot()
            if (quickCheck == true) {
                Log.d(TAG, "silentCheck: quickCheck granted")
                return true
            }

            // quickCheck null = shell belum pernah di-init di session ini
            // (terjadi setelah app restart). Coba init shell secara blocking
            // tapi TANPA memunculkan dialog baru (su -c true saja, bukan getShell()).
            // Kalau Magisk sudah grant sebelumnya, ini akan langsung return sukses.
            Log.d(TAG, "silentCheck: quickCheck=$quickCheck, trying shell init")
            val result = Shell.cmd("true").exec()
            val granted = result.isSuccess
            Log.d(TAG, "silentCheck: shell init result=$granted")
            granted
        } catch (e: Exception) {
            Log.w(TAG, "silentCheck failed: ${e.message}")
            false
        }
    }

    /**
     * REQUEST root — HANYA dipanggil dari SetupActivity saat user klik tombol.
     *
     * Shell.getShell() dari libsu akan:
     * 1. Spawn su dan trigger dialog Magisk/KernelSU/APatch secara proper
     * 2. Tunggu user grant (non-deadlock, libsu mengelola thread sendiri)
     * 3. Return shell yang sudah granted, atau throw kalau denied
     */
    suspend fun requestRoot(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "requestRoot() via libsu")
        _cachedRoot = null

        return@withContext try {
            // getShell() blocking — trigger dialog dan tunggu user grant
            val shell = Shell.getShell()
            val granted = shell.isRoot
            Log.d(TAG, "requestRoot: shell.isRoot=$granted")
            _cachedRoot = granted
            granted
        } catch (e: Exception) {
            Log.w(TAG, "requestRoot: exception — ${e.message}")
            _cachedRoot = false
            false
        }
    }

    fun clearCache() { _cachedRoot = null }
    fun markGranted() { _cachedRoot = true }
    fun markDenied()  { _cachedRoot = false }
}