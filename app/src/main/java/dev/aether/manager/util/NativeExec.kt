package dev.aether.manager.util

import android.util.Log

/**
 * NativeExec — JNI bridge ke libaether-x.so
 *
 * Fallback ke Java Runtime.exec() kalau .so belum di-build atau gagal load.
 */
object NativeExec {

    private const val TAG = "NativeExec"

    /** true kalau libaether-x.so berhasil di-load */
    val nativeAvailable: Boolean

    init {
        nativeAvailable = try {
            System.loadLibrary("aether-x")
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "libaether-x.so tidak tersedia, pakai Java fallback: ${e.message}")
            false
        }
    }

    // ── Native declarations ───────────────────────────────────────────────

    @JvmStatic
    external fun nHasRoot(): Boolean

    @JvmStatic
    external fun nExecSu(cmds: Array<String>): Array<String>

    @JvmStatic
    external fun nExecSuCmd(cmd: String): Array<String>

    // ── Kotlin convenience wrappers (dengan Java fallback) ────────────────

    data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String)

    fun exec(vararg cmds: String): ShellResult {
        if (cmds.isEmpty()) return ShellResult(0, "", "")
        return if (nativeAvailable) {
            val raw = nExecSu(arrayOf(*cmds))
            ShellResult(raw[0].toIntOrNull() ?: -1, raw[1], raw[2])
        } else {
            javaExec(*cmds)
        }
    }

    fun execCmd(cmd: String): ShellResult {
        return if (nativeAvailable) {
            val raw = nExecSuCmd(cmd)
            ShellResult(raw[0].toIntOrNull() ?: -1, raw[1], raw[2])
        } else {
            javaExec(cmd)
        }
    }

    fun output(cmd: String): String = execCmd(cmd).stdout.trim()

    fun ok(vararg cmds: String): Boolean = exec(*cmds).exitCode == 0

    // ── Java fallback shell ───────────────────────────────────────────────

    private fun javaExec(vararg cmds: String): ShellResult {
        return try {
            val script = cmds.joinToString("\n") + "\n"
            val process = ProcessBuilder("su")
                .redirectErrorStream(false)
                .start()

            // FIX: Tulis stdin di thread terpisah supaya tidak deadlock
            // kalau stdout/stderr buffer penuh sebelum stdin selesai ditulis.
            val stdinThread = Thread {
                try {
                    process.outputStream.bufferedWriter().use { it.write(script) }
                } catch (_: Exception) {}
            }
            stdinThread.start()

            // Baca stdout dan stderr secara paralel
            var stdout = ""
            var stderr = ""
            val stdoutThread = Thread { stdout = process.inputStream.bufferedReader().readText() }
            val stderrThread = Thread { stderr = process.errorStream.bufferedReader().readText() }
            stdoutThread.start()
            stderrThread.start()

            stdinThread.join(5000)
            stdoutThread.join(30_000)
            stderrThread.join(5000)

            val exit = process.waitFor()
            ShellResult(exit, stdout, stderr)
        } catch (e: Exception) {
            Log.e(TAG, "javaExec error: ${e.message}")
            ShellResult(-1, "", e.message ?: "exec failed")
        }
    }

    fun javaHasRoot(): Boolean {
        return try {
            val p = ProcessBuilder("su", "-c", "echo aether_root_ok")
                .redirectErrorStream(true)
                .start()
            // FIX: baca output di thread terpisah dengan timeout 8 detik
            var out = ""
            val t = Thread { out = p.inputStream.bufferedReader().readText() }
            t.start()
            t.join(8000)
            val exited = p.waitFor()
            out.contains("aether_root_ok") && exited == 0
        } catch (e: Exception) {
            false
        }
    }
}
