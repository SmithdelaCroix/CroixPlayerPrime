/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.data

import com.croix.player.prime.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val checking: Boolean = false,
    val checked: Boolean = false,
    val hasUpdate: Boolean = false,
    val latestLabel: String = BuildConfig.VERSION_NAME,
    val releaseNotes: String = "",
    val downloadUrl: String = DIRECT_APK_URL,
    val error: String? = null,
) {
    companion object {
        const val DIRECT_APK_URL =
            "https://github.com/SmithdelaCroix/CroixPlayerPrime/releases/download/latest-prime/CroixPlayerPrime-arm64-v8a-signed.apk"
        const val RELEASES_PAGE_URL =
            "https://github.com/SmithdelaCroix/CroixPlayerPrime/releases/latest"
    }
}

object AppUpdateChecker {
    private const val API_URL = "https://api.github.com/repos/SmithdelaCroix/CroixPlayerPrime/releases/latest"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun checkForUpdate(): AppUpdateInfo = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 12_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "CroixPlayerPrime/${BuildConfig.VERSION_NAME}")
            }
            val body = try {
                val code = conn.responseCode
                if (code !in 200..299) error("HTTP $code")
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
            val root = json.parseToJsonElement(body).jsonObject
            val tag = root["tag_name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val name = root["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val notes = root["body"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val apkAsset = root["assets"]?.jsonArray
                ?.mapNotNull { it.jsonObject }
                ?.firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk") == true }
            val dlUrl = apkAsset?.get("browser_download_url")?.jsonPrimitive?.contentOrNull
                ?: AppUpdateInfo.DIRECT_APK_URL
            val remoteVersion = Regex("""v?(\d+\.\d+\.\d+)""").find("$tag $name $notes")?.groupValues?.get(1)
            val currentVersion = BuildConfig.VERSION_NAME.substringBefore("-")
            val newer = remoteVersion != null && compareSemver(remoteVersion, currentVersion) > 0
            AppUpdateInfo(
                checking = false,
                checked = true,
                hasUpdate = newer,
                latestLabel = remoteVersion?.let { "v$it" } ?: (name.ifBlank { tag }),
                releaseNotes = notes.take(280),
                downloadUrl = dlUrl,
            )
        }.getOrElse { err ->
            AppUpdateInfo(
                checking = false,
                checked = true,
                hasUpdate = false,
                downloadUrl = AppUpdateInfo.DIRECT_APK_URL,
                error = err.message ?: "Sin conexión",
            )
        }
    }

    private fun compareSemver(a: String, b: String): Int {
        val pa = a.split(".").map { it.toIntOrNull() ?: 0 }
        val pb = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val diff = (pa.getOrNull(i) ?: 0) - (pb.getOrNull(i) ?: 0)
            if (diff != 0) return diff
        }
        return 0
    }
}
