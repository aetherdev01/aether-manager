package dev.aether.manager.util

import android.util.Log
import com.topjohnwu.superuser.Shell

/**
 * NativeExec — Shell executor menggunakan libsu.
 *
 * Semua eksekusi shell melalui libsu Shell instance yang sudah granted.
 * Tidak ada ProcessBuilder manual, tidak ada deadlock Magisk.
 */
object NativeExec {

    private const val TAG = "NativeExec"

    val nativeAvailable: Boolean = false

    data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String)

    // ── Core exec ─────────────────────────────────────────────────────────

    fun exec(script: String): ShellResult {
        if (script.isBlank()) return ShellResult(0, "", "")
        return try {
            val lines = script.trim().lines().filter { it.isNotBlank() }
            val result = Shell.cmd(*lines.toTypedArray()).exec()
            val stdout = result.out.joinToString("\n")
            val stderr = result.err.joinToString("\n")
            val exitCode = if (result.isSuccess) 0 else 1
            Log.d(TAG, "exec: exit=$exitCode stdout=${stdout.take(100)}")
            ShellResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            Log.e(TAG, "exec error: ${e.message}")
            ShellResult(-1, "", e.message ?: "exec failed")
        }
    }

    fun exec(vararg cmds: String): ShellResult =
        exec(cmds.joinToString("\n"))

    fun execCmd(cmd: String): ShellResult = exec(cmd)

    fun output(cmd: String): String = exec(cmd).stdout.trim()

    fun ok(vararg cmds: String): Boolean = exec(*cmds).exitCode == 0

    // ── Root check ────────────────────────────────────────────────────────

    fun javaHasRoot(): Boolean {
        return try {
            Shell.isAppGrantedRoot() == true
        } catch (e: Exception) {
            Log.w(TAG, "javaHasRoot failed: ${e.message}")
            false
        }
    }

    // Kept for compatibility
    fun javaExec(script: String): ShellResult = exec(script)
}
