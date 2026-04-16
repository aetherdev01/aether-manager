package dev.aether.manager.util

import android.util.Log
import java.io.File

/**
 * NativeExec — JNI bridge ke libaether-x.so
 *
 * FIX: exec() sekarang mengirim seluruh script sebagai SATU string
 *      via nExecSuCmd() agar variable shell persist dalam satu sesi.
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

    /**
     * Kirim array of commands — setiap item satu baris.
     * Return: [exitCode_string, stdout, stderr]
     */
    @JvmStatic
    external fun nExecSu(cmds: Array<String>): Array<String>

    /**
     * Kirim satu string cmd (boleh multi-line script).
     * Return: [exitCode_string, stdout, stderr]
     * FIX: ini yang dipakai exec(script) agar variable shell persist.
     */
    @JvmStatic
    external fun nExecSuCmd(cmd: String): Array<String>

    // ── Kotlin convenience wrappers ───────────────────────────────────────

    data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String)

    /**
     * Eksekusi satu script (boleh multi-line).
     * FIX: pakai nExecSuCmd() — script dikirim sebagai stdin satu sesi.
     * Variable shell ($x, $y, dll) persist sepanjang script.
     */
    fun exec(script: String): ShellResult {
        if (script.isBlank()) return ShellResult(0, "", "")
        return if (nativeAvailable) {
            val raw = nExecSuCmd(script)
            ShellResult(raw[0].toIntOrNull() ?: -1, raw[1], raw[2])
        } else {
            javaExec(script)
        }
    }

    /** Legacy overload: gabung array jadi satu script */
    fun exec(vararg cmds: String): ShellResult =
        exec(cmds.joinToString("\n"))

    fun execCmd(cmd: String): ShellResult = exec(cmd)

    fun output(cmd: String): String = exec(cmd).stdout.trim()

    fun ok(vararg cmds: String): Boolean = exec(*cmds).exitCode == 0

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

    // ── Java fallback shell ───────────────────────────────────────────────
    // Kirim script sebagai satu stdin-session agar variable persist.

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

    // ── javaHasRoot ───────────────────────────────────────────────────────

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
            Log.d(TAG, "javaHasRootArgC: out='${out.trim()}' exit=$code")
            out.contains("aether_root_ok") && code == 0
        } catch (e: Exception) {
            Log.w(TAG, "javaHasRootArgC failed: ${e.message}")
            false
        }
    }
}
