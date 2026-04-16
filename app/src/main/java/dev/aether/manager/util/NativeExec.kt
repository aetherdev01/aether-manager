package dev.aether.manager.util

import android.util.Log
import java.io.File

/**
 * NativeExec — Pure Java/Kotlin shell executor (NO JNI, NO .so required)
 *
 * Semua eksekusi shell menggunakan ProcessBuilder langsung via stdin-pipe.
 * Variable shell PERSIST dalam satu sesi karena script dikirim sebagai
 * satu stdin session ke process su.
 *
 * JNI dihapus sepenuhnya — tidak ada System.loadLibrary(), tidak ada
 * external fun, tidak ada libaether-x.so dependency.
 */
object NativeExec {

    private const val TAG = "NativeExec"

    // Selalu true — tidak ada native dependency
    val nativeAvailable: Boolean = false

    // ── Su binary resolver ────────────────────────────────────────────────

    val suBinary: String by lazy {
        val candidates = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/magisk/.core/bin/su",
            "su"
        )
        candidates.firstOrNull { path ->
            if (path == "su") true
            else File(path).let { it.exists() && it.canExecute() }
        } ?: "su"
    }

    // ── Result type ───────────────────────────────────────────────────────

    data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String)

    // ── Core exec — stdin-pipe, satu sesi, variable persist ──────────────

    /**
     * Eksekusi script (boleh multi-line) sebagai satu stdin session.
     * Variable shell ($x, $y, dll) persist sepanjang script.
     */
    fun exec(script: String): ShellResult {
        if (script.isBlank()) return ShellResult(0, "", "")
        return javaExec(script)
    }

    /** Legacy overload: gabung array jadi satu script */
    fun exec(vararg cmds: String): ShellResult =
        exec(cmds.joinToString("\n"))

    fun execCmd(cmd: String): ShellResult = exec(cmd)

    fun output(cmd: String): String = exec(cmd).stdout.trim()

    fun ok(vararg cmds: String): Boolean = exec(*cmds).exitCode == 0

    // ── Java shell executor ───────────────────────────────────────────────

    fun javaExec(script: String): ShellResult {
        return try {
            val payload = if (script.trimEnd().endsWith("exit")) script
                          else "$script\nexit\n"

            val process = ProcessBuilder(suBinary)
                .redirectErrorStream(false)
                .start()

            val stdinThread = Thread {
                try {
                    process.outputStream.bufferedWriter().use { it.write(payload) }
                } catch (_: Exception) {}
            }
            stdinThread.start()

            var stdout = ""
            var stderr = ""
            val stdoutThread = Thread { stdout = process.inputStream.bufferedReader().readText() }
            val stderrThread = Thread { stderr = process.errorStream.bufferedReader().readText() }
            stdoutThread.start()
            stderrThread.start()

            stdinThread.join(5_000)
            stdoutThread.join(30_000)
            stderrThread.join(5_000)

            val exit = try { process.waitFor() } catch (_: Exception) { -1 }
            ShellResult(exit, stdout, stderr)
        } catch (e: Exception) {
            Log.e(TAG, "javaExec error: ${e.message}")
            ShellResult(-1, "", e.message ?: "exec failed")
        }
    }

    // ── Root check ────────────────────────────────────────────────────────

    fun javaHasRoot(): Boolean {
        if (javaHasRootStdin()) return true
        return javaHasRootArgC()
    }

    private fun javaHasRootStdin(): Boolean {
        return try {
            val process = ProcessBuilder(suBinary)
                .redirectErrorStream(true)
                .start()
            val writeThread = Thread {
                try {
                    process.outputStream.bufferedWriter().use {
                        it.write("echo aether_root_ok\nexit\n")
                    }
                } catch (_: Exception) {}
            }
            writeThread.start()
            var out = ""
            val readThread = Thread { out = process.inputStream.bufferedReader().readText() }
            readThread.start()
            writeThread.join(5_000)
            readThread.join(8_000)
            val code = try { process.waitFor() } catch (_: Exception) { -1 }
            process.destroy()
            Log.d(TAG, "javaHasRootStdin: out='${out.trim()}' exit=$code su=$suBinary")
            out.contains("aether_root_ok") && code == 0
        } catch (e: Exception) {
            Log.w(TAG, "javaHasRootStdin failed: ${e.message}")
            false
        }
    }

    private fun javaHasRootArgC(): Boolean {
        return try {
            val p = ProcessBuilder(suBinary, "-c", "echo aether_root_ok")
                .redirectErrorStream(true)
                .start()
            var out = ""
            val t = Thread { out = p.inputStream.bufferedReader().readText() }
            t.start()
            t.join(8_000)
            val code = try { p.waitFor() } catch (_: Exception) { -1 }
            p.destroy()
            Log.d(TAG, "javaHasRootArgC: out='${out.trim()}' exit=$code")
            out.contains("aether_root_ok") && code == 0
        } catch (e: Exception) {
            Log.w(TAG, "javaHasRootArgC failed: ${e.message}")
            false
        }
    }
}
