/**
 * libaether-x.so  —  AetherManager Native Exec Engine
 *
 * Menyediakan root shell execution real-time via su dengan:
 *   - pipe()+fork()+execv() untuk full process control
 *   - select() multiplexed read (stdout+stderr) untuk hindari deadlock
 *   - Proper FD cleanup dan SIGPIPE handling
 *   - Timeout built-in (30 detik per batch command)
 *
 * JNI entry points:
 *   nHasRoot()                     → Boolean
 *   nExecSu(cmds: Array<String>)   → Array<String> [exitCode, stdout, stderr]
 */

#include <jni.h>
#include <unistd.h>
#include "aether_strings.h"
#include <sys/wait.h>
#include <sys/select.h>
#include <fcntl.h>
#include <signal.h>
#include <errno.h>
#include <string.h>
#include <string>
#include <vector>
#include <android/log.h>

#define TAG     "libaether-x"
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// ────────────────────────────────────────────────────────────────────────────
// Internal helpers
// ────────────────────────────────────────────────────────────────────────────

static void closeFd(int& fd) {
    if (fd >= 0) { close(fd); fd = -1; }
}

/**
 * Set FD non-blocking. Returns false on error.
 */
static bool setNonBlocking(int fd) {
    int flags = fcntl(fd, F_GETFL, 0);
    if (flags < 0) return false;
    return fcntl(fd, F_SETFL, flags | O_NONBLOCK) == 0;
}

/**
 * Fully write `data` to fd. Returns false if write fails.
 */
static bool writeAll(int fd, const std::string& data) {
    const char* ptr = data.c_str();
    size_t remaining = data.size();
    while (remaining > 0) {
        ssize_t n = write(fd, ptr, remaining);
        if (n < 0) {
            if (errno == EINTR) continue;
            LOGE("writeAll error: %s", strerror(errno));
            return false;
        }
        ptr += n;
        remaining -= n;
    }
    return true;
}

struct ExecResult {
    int    exitCode = -1;
    std::string stdoutStr;
    std::string stderrStr;
};

/**
 * Jalankan `su` dengan commands dikirim via stdin pipe.
 * stdout dan stderr dibaca secara multiplexed dengan select().
 * Timeout: 30 detik total.
 */
static ExecResult execSuInternal(const std::vector<std::string>& cmds) {
    ExecResult res;

    // Pipes: [0]=read, [1]=write
    int stdinPipe[2]  = {-1, -1};
    int stdoutPipe[2] = {-1, -1};
    int stderrPipe[2] = {-1, -1};

    if (pipe(stdinPipe)  < 0 ||
        pipe(stdoutPipe) < 0 ||
        pipe(stderrPipe) < 0) {
        LOGE("pipe() failed: %s", strerror(errno));
        res.stderrStr = "pipe() failed";
        closeFd(stdinPipe[0]);  closeFd(stdinPipe[1]);
        closeFd(stdoutPipe[0]); closeFd(stdoutPipe[1]);
        closeFd(stderrPipe[0]); closeFd(stderrPipe[1]);
        return res;
    }

    // Ignore SIGPIPE — jika su exits lebih awal
    struct sigaction sa_old{};
    struct sigaction sa_ign{};
    sa_ign.sa_handler = SIG_IGN;
    sigemptyset(&sa_ign.sa_mask);
    sigaction(SIGPIPE, &sa_ign, &sa_old);

    pid_t pid = fork();
    if (pid < 0) {
        LOGE("fork() failed: %s", strerror(errno));
        res.stderrStr = "fork() failed";
        sigaction(SIGPIPE, &sa_old, nullptr);
        closeFd(stdinPipe[0]);  closeFd(stdinPipe[1]);
        closeFd(stdoutPipe[0]); closeFd(stdoutPipe[1]);
        closeFd(stderrPipe[0]); closeFd(stderrPipe[1]);
        return res;
    }

    if (pid == 0) {
        // ── CHILD PROCESS ──────────────────────────────────────
        // Redirect stdin/stdout/stderr ke pipes
        dup2(stdinPipe[0],  STDIN_FILENO);
        dup2(stdoutPipe[1], STDOUT_FILENO);
        dup2(stderrPipe[1], STDERR_FILENO);

        // Tutup semua ujung pipe yang tidak dipakai di child
        closeFd(stdinPipe[0]);  closeFd(stdinPipe[1]);
        closeFd(stdoutPipe[0]); closeFd(stdoutPipe[1]);
        closeFd(stderrPipe[0]); closeFd(stderrPipe[1]);

        // Coba exec su dari path yang umum di Android
        const char* suPaths[] = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "su",
            nullptr
        };
        for (int i = 0; suPaths[i] != nullptr; i++) {
            execl(suPaths[i], "su", (char*)nullptr);
        }

        // Jika semua gagal
        const char* msg = "exec su failed\n";
        write(STDERR_FILENO, msg, strlen(msg));
        _exit(127);
    }

    // ── PARENT PROCESS ───────────────────────────────────────

    // Tutup ujung pipe yang dipakai child
    closeFd(stdinPipe[0]);
    closeFd(stdoutPipe[1]);
    closeFd(stderrPipe[1]);

    // Build stdin payload
    std::string payload;
    for (const auto& cmd : cmds) {
        payload += cmd;
        payload += '\n';
    }
    payload += "exit $?\n";

    // Tulis commands ke su's stdin
    writeAll(stdinPipe[1], payload);
    closeFd(stdinPipe[1]);   // signal EOF ke su

    // Set stdout/stderr non-blocking untuk select()
    setNonBlocking(stdoutPipe[0]);
    setNonBlocking(stderrPipe[0]);

    // Baca output dengan select() — mencegah deadlock buffer penuh
    constexpr int TIMEOUT_SEC = 30;
    constexpr size_t BUF_SIZE = 8192;
    char buf[BUF_SIZE];

    bool stdoutDone = false;
    bool stderrDone = false;

    struct timeval deadline{};
    deadline.tv_sec  = TIMEOUT_SEC;
    deadline.tv_usec = 0;

    while (!stdoutDone || !stderrDone) {
        fd_set readFds;
        FD_ZERO(&readFds);
        int maxFd = -1;

        if (!stdoutDone) { FD_SET(stdoutPipe[0], &readFds); maxFd = std::max(maxFd, stdoutPipe[0]); }
        if (!stderrDone) { FD_SET(stderrPipe[0], &readFds); maxFd = std::max(maxFd, stderrPipe[0]); }

        if (maxFd < 0) break;

        struct timeval tv = deadline;
        int sel = select(maxFd + 1, &readFds, nullptr, nullptr, &tv);

        if (sel < 0) {
            if (errno == EINTR) continue;
            break;
        }
        if (sel == 0) {
            // Timeout — kill su
            LOGE("execSu timeout, killing pid %d", pid);
            kill(pid, SIGKILL);
            res.stderrStr += "\n[aether-x] TIMEOUT after 30s";
            break;
        }

        if (!stdoutDone && FD_ISSET(stdoutPipe[0], &readFds)) {
            ssize_t n = read(stdoutPipe[0], buf, BUF_SIZE);
            if (n > 0)       res.stdoutStr.append(buf, n);
            else if (n == 0) stdoutDone = true;
            else if (errno != EAGAIN && errno != EWOULDBLOCK) stdoutDone = true;
        }

        if (!stderrDone && FD_ISSET(stderrPipe[0], &readFds)) {
            ssize_t n = read(stderrPipe[0], buf, BUF_SIZE);
            if (n > 0)       res.stderrStr.append(buf, n);
            else if (n == 0) stderrDone = true;
            else if (errno != EAGAIN && errno != EWOULDBLOCK) stderrDone = true;
        }
    }

    // Drain sisa data jika ada
    while (true) {
        ssize_t n = read(stdoutPipe[0], buf, BUF_SIZE);
        if (n <= 0) break;
        res.stdoutStr.append(buf, n);
    }
    while (true) {
        ssize_t n = read(stderrPipe[0], buf, BUF_SIZE);
        if (n <= 0) break;
        res.stderrStr.append(buf, n);
    }

    closeFd(stdoutPipe[0]);
    closeFd(stderrPipe[0]);

    // Tunggu child selesai
    int status = 0;
    waitpid(pid, &status, 0);
    res.exitCode = WIFEXITED(status) ? WEXITSTATUS(status) : -1;

    // Restore SIGPIPE handler
    sigaction(SIGPIPE, &sa_old, nullptr);

    // Trim trailing newlines dari output
    while (!res.stdoutStr.empty() && res.stdoutStr.back() == '\n')
        res.stdoutStr.pop_back();
    while (!res.stderrStr.empty() && res.stderrStr.back() == '\n')
        res.stderrStr.pop_back();

    LOGD("execSu exit=%d stdout_len=%zu stderr_len=%zu",
         res.exitCode, res.stdoutStr.size(), res.stderrStr.size());

    return res;
}

// ────────────────────────────────────────────────────────────────────────────
// JNI Helper: convert jstring ↔ std::string
// ────────────────────────────────────────────────────────────────────────────

static std::string jstringToStr(JNIEnv* env, jstring js) {
    if (!js) return "";
    const char* chars = env->GetStringUTFChars(js, nullptr);
    std::string s(chars);
    env->ReleaseStringUTFChars(js, chars);
    return s;
}

static jstring strToJstring(JNIEnv* env, const std::string& s) {
    return env->NewStringUTF(s.c_str());
}

// ────────────────────────────────────────────────────────────────────────────
// JNI Exports
// ────────────────────────────────────────────────────────────────────────────

extern "C" {

/**
 * nHasRoot() → Boolean
 * Cek apakah root tersedia dan dapat dieksekusi.
 */
JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_util_NativeExec_nHasRoot(JNIEnv* /*env*/, jclass /*cls*/) {
    std::vector<std::string> cmds = {"echo aether_root_ok"};
    ExecResult r = execSuInternal(cmds);
    bool ok = (r.exitCode == 0) &&
              (r.stdoutStr.find("aether_root_ok") != std::string::npos);
    LOGD("nHasRoot → %s (exit=%d)", ok ? "true" : "false", r.exitCode);
    return ok ? JNI_TRUE : JNI_FALSE;
}

/**
 * nExecSu(cmds: Array<String>) → Array<String>
 * Menjalankan array perintah shell via su stdin pipe.
 * Return: [exitCode_as_string, stdout, stderr]
 */
JNIEXPORT jobjectArray JNICALL
Java_dev_aether_manager_util_NativeExec_nExecSu(
        JNIEnv* env, jclass /*cls*/, jobjectArray jCmds)
{
    // Convert Java String[] → std::vector<std::string>
    std::vector<std::string> cmds;
    if (jCmds) {
        jsize len = env->GetArrayLength(jCmds);
        cmds.reserve(len);
        for (jsize i = 0; i < len; i++) {
            auto js = (jstring) env->GetObjectArrayElement(jCmds, i);
            cmds.push_back(jstringToStr(env, js));
            env->DeleteLocalRef(js);
        }
    }

    ExecResult r = execSuInternal(cmds);

    // Build result array: [exitCode, stdout, stderr]
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(3, stringClass, nullptr);
    env->SetObjectArrayElement(result, 0, strToJstring(env, std::to_string(r.exitCode)));
    env->SetObjectArrayElement(result, 1, strToJstring(env, r.stdoutStr));
    env->SetObjectArrayElement(result, 2, strToJstring(env, r.stderrStr));
    return result;
}

/**
 * nExecSuCmd(cmd: String) → Array<String>
 * Single real-time command via `su -c "cmd"` — lebih cepat untuk one-liner.
 * Return: [exitCode_as_string, stdout, stderr]
 */
JNIEXPORT jobjectArray JNICALL
Java_dev_aether_manager_util_NativeExec_nExecSuCmd(
        JNIEnv* env, jclass /*cls*/, jstring jCmd)
{
    std::string cmd = jstringToStr(env, jCmd);
    // Wrap single command supaya tetap pakai pipe path yang sama
    ExecResult r = execSuInternal({cmd});

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(3, stringClass, nullptr);
    env->SetObjectArrayElement(result, 0, strToJstring(env, std::to_string(r.exitCode)));
    env->SetObjectArrayElement(result, 1, strToJstring(env, r.stdoutStr));
    env->SetObjectArrayElement(result, 2, strToJstring(env, r.stderrStr));
    return result;
}

/**
 * nGetAdId(key: Int) → String
 * Return decrypted AdMob ID untuk key yang diberikan.
 *   key = 0 → Banner production ID
 * Return empty string jika key tidak dikenali.
 */
JNIEXPORT jstring JNICALL
Java_dev_aether_manager_ads_AdManager_nGetAdId(
        JNIEnv* env, jclass /*cls*/, jint key)
{
    std::string result;
    switch (key) {
        case 0:
            result = aether_decrypt(kAdMobBannerId, sizeof(kAdMobBannerId), AETHER_KEY);
            break;
        default:
            result = "";
            break;
    }
    return env->NewStringUTF(result.c_str());
}

} // extern "C"
