package dev.aether.manager.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val latestVersion: String,      // e.g. "2.1"
    val latestVersionCode: Int,     // e.g. 3
    val releaseNotes: String,       // body dari GitHub Release
    val downloadUrl: String,        // direct APK download URL
    val releasePageUrl: String,     // html_url dari release
    val isForceUpdate: Boolean,     // dari tag name prefix "force-" atau field di body
)

sealed class UpdateResult {
    data class UpdateAvailable(val info: ReleaseInfo) : UpdateResult()
    object UpToDate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

object UpdateChecker {

    private const val GITHUB_API =
        "https://api.github.com/repos/aetherdev01/aether-manager/releases/latest"

    /**
     * Cek update dari GitHub Releases API.
     * Harus dipanggil dari coroutine (IO dispatcher).
     *
     * Force update dikontrol dengan menambahkan baris di body release:
     *   `force_update: true`
     */
    suspend fun check(currentVersionCode: Int): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(GITHUB_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "AetherManager-Android")
                connectTimeout = 10_000
                readTimeout    = 10_000
            }

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                return@withContext UpdateResult.Error("HTTP $responseCode")
            }

            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json        = JSONObject(body)
            val tagName     = json.getString("tag_name")          // e.g. "v2.1" atau "v2.1+3"
            val releaseBody = json.optString("body", "")
            val htmlUrl     = json.getString("html_url")

            // Parse versi dari tag: support format "v2.1" dan "v2.1+3"
            val (versionName, versionCode) = parseTag(tagName)
                ?: return@withContext UpdateResult.Error("Tag tidak valid: $tagName")

            // Cari APK asset pertama
            val assets      = json.getJSONArray("assets")
            val downloadUrl = findApkDownloadUrl(assets)
                ?: return@withContext UpdateResult.Error("Tidak ada APK di release ini")

            // Cek force update dari body release
            val isForce = releaseBody.contains("force_update: true", ignoreCase = true)

            if (versionCode <= currentVersionCode) {
                return@withContext UpdateResult.UpToDate
            }

            UpdateResult.UpdateAvailable(
                ReleaseInfo(
                    latestVersion     = versionName,
                    latestVersionCode = versionCode,
                    releaseNotes      = cleanReleaseNotes(releaseBody),
                    downloadUrl       = downloadUrl,
                    releasePageUrl    = htmlUrl,
                    isForceUpdate     = isForce,
                )
            )
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Parse tag name ke (versionName, versionCode).
     * Support format:
     *   - "v2.1"      → ("2.1", 0) — pakai 0 jika tidak ada versionCode, fallback string compare
     *   - "v2.1+3"    → ("2.1", 3) — format recommended
     *   - "2.1+3"     → ("2.1", 3)
     */
    private fun parseTag(tag: String): Pair<String, Int>? {
        val clean = tag.trimStart('v', 'V')
        return if (clean.contains('+')) {
            val parts = clean.split('+')
            val name  = parts[0]
            val code  = parts[1].toIntOrNull() ?: return null
            name to code
        } else {
            // Tidak ada versionCode di tag — konversi dari versionName
            // "2.1" → 21, "2.10" → 210, dst
            val code = clean.replace(".", "").toIntOrNull() ?: return null
            clean to code
        }
    }

    private fun findApkDownloadUrl(assets: org.json.JSONArray): String? {
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name  = asset.getString("name")
            if (name.endsWith(".apk", ignoreCase = true)) {
                return asset.getString("browser_download_url")
            }
        }
        return null
    }

    private fun cleanReleaseNotes(body: String): String {
        // Hapus baris kontrol internal (force_update: true, dsb)
        return body.lines()
            .filterNot { it.trim().startsWith("force_update:", ignoreCase = true) }
            .joinToString("\n")
            .trim()
    }
}
