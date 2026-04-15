package dev.aether.manager.util

import android.util.Log
import java.io.File

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

    // ── Su binary resolver ────────────────────────────────────────────────

    /**
     * Cari binary 'su' yang valid.
     * PATH di app context seringkali tidak include /system/xbin atau /system/bin,
     * jadi kita probe manual.
     */
    val suBinary: String by lazy {
        val candidates = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/magisk/.core/bin/su",
            "su"  // fallback terakhir — andalkan PATH
        )
        candidates.firstOrNull { path ->
            if (path == "su") true
            else File(path).let { it.exists() && it.canExecute() }
        } ?: "su"
    }

    // ── Java fallback shell ───────────────────────────────────────────────

    /**
     * Eksekusi commands via su stdin-pipe.
     * WAJIB stdin-pipe (bukan argumen -c) supaya kompatibel dengan
     * Magisk, KernelSU, APatch — semua butuh stdin session untuk multi-command.
     */
    fun javaExec(vararg cmds: String): ShellResult {
        return try {
            val script = buildString {
                cmds.forEach { appendLine(it) }
                appendLine("exit")
            }

            val process = ProcessBuilder(suBinary)
                .redirectErrorStream(false)
                .start()

            val stdinThread = Thread {
                try {
                    process.outputStream.bufferedWriter().use { it.write(script) }
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

    /**
     * FIX: hasRoot via stdin-pipe dulu, lalu fallback ke -c flag.
     * KSU/APatch menolak -c tapi accept stdin; beberapa su klasik sebaliknya.
     */
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
