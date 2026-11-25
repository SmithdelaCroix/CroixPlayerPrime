/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.playback

import android.content.Context
import com.croix.prime.engine.InnerTubeClient
import com.croix.prime.engine.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class CroixDownloadManager(
    context: Context,
    private val innerTube: InnerTubeClient,
) {
    private val downloadsDir = File(context.applicationContext.filesDir, "downloads").apply { mkdirs() }
    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _activeProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val activeProgress: StateFlow<Map<String, Float>> = _activeProgress.asStateFlow()

    fun getDownloadedFile(trackId: String): File? {
        val f = File(downloadsDir, "$trackId.m4a")
        return if (f.exists() && f.length() > 1024L) f else null
    }

    fun deleteDownload(trackId: String): Boolean {
        val f = File(downloadsDir, "$trackId.m4a")
        return if (f.exists()) f.delete() else false
    }

    suspend fun downloadTrack(track: Track): Result<File> = withContext(Dispatchers.IO) {
        getDownloadedFile(track.id)?.let { return@withContext Result.success(it) }

        _activeProgress.update { it + (track.id to 0.02f) }
        val result = runCatching {
            val stream = innerTube.resolveAudioStream(track.id).getOrThrow()
            val req = Request.Builder()
                .url(stream.url)
                .header("User-Agent", "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L) gzip")
                .build()

            val targetFile = File(downloadsDir, "${track.id}.m4a")
            val tempFile = File(downloadsDir, "${track.id}.part")

            okHttp.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body ?: error("Empty body")
                val total = body.contentLength().coerceAtLeast(1L)
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { out ->
                        val buf = ByteArray(16 * 1024)
                        var copied = 0L
                        while (true) {
                            val read = input.read(buf)
                            if (read <= 0) break
                            out.write(buf, 0, read)
                            copied += read
                            val frac = (copied.toFloat() / total.toFloat()).coerceIn(0.05f, 0.99f)
                            _activeProgress.update { it + (track.id to frac) }
                        }
                    }
                }
            }
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            targetFile
        }
        _activeProgress.update { it - track.id }
        result
    }
}
