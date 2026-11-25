/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.playback

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.croix.prime.engine.StreamRequestHeaders
import com.croix.prime.engine.YtPlaybackClient
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class CroixPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        // Dynamic Metrolist-style OkHttp interceptor: matches exact User-Agent, Origin & Referer
        // for EVERY YouTube CDN stream URL (?c=...&cver=...) so googlevideo.com never rejects with 403!
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val orig = chain.request()
                val urlStr = orig.url.toString()
                val isYouTubeMedia = "googlevideo.com" in urlStr || "youtube.com" in urlStr
                if (!isYouTubeMedia) {
                    return@addInterceptor chain.proceed(orig)
                }
                val headers = CroixAudioSessionBridge.resolveHeadersForUrl(urlStr)
                val updated = orig.newBuilder()
                    .header("User-Agent", headers.userAgent)
                if (headers.origin != null) {
                    updated.header("Origin", headers.origin!!)
                } else {
                    updated.removeHeader("Origin")
                }
                if (headers.referer != null) {
                    updated.header("Referer", headers.referer!!)
                } else {
                    updated.removeHeader("Referer")
                }
                chain.proceed(updated.build())
            }
            .build()
        val dataSourceFactory = OkHttpDataSource.Factory(okHttp)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val exo = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player = exo
        mediaSession = MediaSession.Builder(this, exo).build()
        CroixAudioSessionBridge.currentAudioSessionId = exo.audioSessionId
        CroixAudioSessionBridge.exoPlayerRef = exo
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        CroixAudioSessionBridge.exoPlayerRef = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }
}

object CroixAudioSessionBridge {
    @Volatile
    var currentAudioSessionId: Int = 0

    @Volatile
    var exoPlayerRef: ExoPlayer? = null

    private val streamHeadersByUrl = ConcurrentHashMap<String, StreamRequestHeaders>()

    fun registerStreamHeaders(url: String, headers: StreamRequestHeaders) {
        if (streamHeadersByUrl.size > 48) streamHeadersByUrl.clear()
        streamHeadersByUrl[url] = headers
    }

    fun resolveHeadersForUrl(url: String): StreamRequestHeaders =
        streamHeadersByUrl[url] ?: YtPlaybackClient.resolveHeadersForStreamUrl(url)
}
