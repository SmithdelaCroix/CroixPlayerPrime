/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import java.util.Locale

/**
 * Metrolist / InnerTube-aligned 2026 YouTube client definitions for reliable ad-free audio playback.
 * Each client pairs its exact clientId, clientVersion, device fields, and matching User-Agent
 * so both `/youtubei/v1/player` and `googlevideo.com` CDN requests pass validation without HTTP 403.
 */
data class YtPlaybackClient(
    val key: String,
    val clientName: String,
    val clientVersion: String,
    val clientId: String,
    val userAgent: String,
    val osName: String? = null,
    val osVersion: String? = null,
    val deviceMake: String? = null,
    val deviceModel: String? = null,
    val androidSdkVersion: Int? = null,
    val loginSupported: Boolean = false,
    val loginRequired: Boolean = false,
    val useSignatureTimestamp: Boolean = false,
    val isEmbedded: Boolean = false,
) {
    val origin: String
        get() = when (clientName.uppercase(Locale.US)) {
            "WEB", "TVHTML5", "TVHTML5_SIMPLY_EMBEDDED_PLAYER", "TVHTML5_SIMPLY" -> ORIGIN_YOUTUBE
            else -> ORIGIN_YOUTUBE_MUSIC
        }

    val referer: String
        get() = when (clientName.uppercase(Locale.US)) {
            "WEB" -> "$ORIGIN_YOUTUBE/"
            "TVHTML5", "TVHTML5_SIMPLY_EMBEDDED_PLAYER", "TVHTML5_SIMPLY" -> "$ORIGIN_YOUTUBE/tv"
            else -> "$ORIGIN_YOUTUBE_MUSIC/"
        }

    val isWebLike: Boolean
        get() = clientName.uppercase(Locale.US) in setOf(
            "WEB",
            "WEB_REMIX",
            "WEB_CREATOR",
            "MWEB",
            "WEB_EMBEDDED_PLAYER",
            "TVHTML5",
            "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            "TVHTML5_SIMPLY",
        )

    fun playerEndpointUrl(): String = "$origin/youtubei/v1/player?prettyPrint=false"

    companion object {
        const val ORIGIN_YOUTUBE_MUSIC = "https://music.youtube.com"
        const val ORIGIN_YOUTUBE = "https://www.youtube.com"
        const val USER_AGENT_WEB =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
        const val USER_AGENT_WEB_REMIX =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

        // #1 Fast Path (Metrolist / ArchiveTune 2026): tokenless direct audio streams
        val VISIONOS = YtPlaybackClient(
            key = "VISIONOS@0.1",
            clientName = "VISIONOS",
            clientVersion = "0.1",
            clientId = "101",
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15",
            osName = "visionOS",
            osVersion = "1.3.21O771",
            deviceMake = "Apple",
            deviceModel = "RealityDevice14,1",
            loginSupported = false,
            useSignatureTimestamp = false,
        )

        val ANDROID_VR_NO_AUTH = YtPlaybackClient(
            key = "ANDROID_VR@1.37",
            clientName = "ANDROID_VR",
            clientVersion = "1.37",
            clientId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.37 (Linux; U; Android 12; en_US; Quest 3; Build/SQ3A.220605.009.A1; Cronet/107.0.5284.2)",
            osName = "Android",
            osVersion = "12",
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            androidSdkVersion = 32,
            loginSupported = false,
            useSignatureTimestamp = false,
        )

        val ANDROID_VR_1_61_48 = YtPlaybackClient(
            key = "ANDROID_VR@1.61.48",
            clientName = "ANDROID_VR",
            clientVersion = "1.61.48",
            clientId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.61.48 (Linux; U; Android 12; en_US; Quest 3; Build/SQ3A.220605.009.A1; Cronet/132.0.6808.3)",
            osName = "Android",
            osVersion = "12",
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            androidSdkVersion = 32,
            loginSupported = false,
            useSignatureTimestamp = false,
        )

        val ANDROID_VR_1_43_32 = YtPlaybackClient(
            key = "ANDROID_VR@1.43.32",
            clientName = "ANDROID_VR",
            clientVersion = "1.43.32",
            clientId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.43.32 (Linux; U; Android 12; en_US; Quest 3; Build/SQ3A.220605.009.A1; Cronet/107.0.5284.2)",
            osName = "Android",
            osVersion = "12",
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            androidSdkVersion = 32,
            loginSupported = false,
            useSignatureTimestamp = false,
        )

        val IOS = YtPlaybackClient(
            key = "IOS@19.29.1",
            clientName = "IOS",
            clientVersion = "19.29.1",
            clientId = "5",
            userAgent = "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X;)",
            osName = "iOS",
            osVersion = "17.5.1.21F90",
            deviceMake = "Apple",
            deviceModel = "iPhone16,2",
            loginSupported = false,
            useSignatureTimestamp = false,
        )

        val IOS_MUSIC = YtPlaybackClient(
            key = "IOS_MUSIC@7.27.0",
            clientName = "IOS_MUSIC",
            clientVersion = "7.27.0",
            clientId = "26",
            userAgent = "com.google.ios.youtubemusic/7.27.0 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X;)",
            osName = "iOS",
            osVersion = "17.5.1.21F90",
            deviceMake = "Apple",
            deviceModel = "iPhone16,2",
            loginSupported = false,
            useSignatureTimestamp = false,
        )

        val IPADOS = YtPlaybackClient(
            key = "IOS@19.22.3",
            clientName = "IOS",
            clientVersion = "19.22.3",
            clientId = "5",
            userAgent = "com.google.ios.youtube/19.22.3 (iPad7,6; U; CPU iPadOS 17_7_10 like Mac OS X; en-US)",
            osName = "iPadOS",
            osVersion = "17.7.10.21H450",
            deviceMake = "Apple",
            deviceModel = "iPad7,6",
            loginSupported = false,
            useSignatureTimestamp = false,
        )

        val ANDROID_MUSIC = YtPlaybackClient(
            key = "ANDROID_MUSIC@7.27.52",
            clientName = "ANDROID_MUSIC",
            clientVersion = "7.27.52",
            clientId = "21",
            userAgent = "com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 15; en_US; Pixel 9 Pro; Build/AP4A.250205.002; Cronet/132.0.6834.79) gzip",
            osName = "Android",
            osVersion = "15",
            deviceMake = "Google",
            deviceModel = "Pixel 9 Pro",
            androidSdkVersion = 35,
            loginSupported = true,
            useSignatureTimestamp = true,
        )

        val ANDROID_TESTSUITE = YtPlaybackClient(
            key = "ANDROID_TESTSUITE@1.9",
            clientName = "ANDROID_TESTSUITE",
            clientVersion = "1.9",
            clientId = "30",
            userAgent = "com.google.android.youtube/1.9 (Linux; U; Android 15; en_US; Pixel 9 Pro; Build/AP4A.250205.002) gzip",
            osName = "Android",
            osVersion = "15",
            deviceMake = "Google",
            deviceModel = "Pixel 9 Pro",
            androidSdkVersion = 35,
            loginSupported = false,
            useSignatureTimestamp = false,
        )

        val MOBILE = YtPlaybackClient(
            key = "ANDROID@21.10.38",
            clientName = "ANDROID",
            clientVersion = "21.10.38",
            clientId = "3",
            userAgent = "com.google.android.youtube/21.10.38 (Linux; U; Android 15; en_US; Pixel 9 Pro; Build/AP4A.250205.002; Cronet/132.0.6834.79) gzip",
            osName = "Android",
            osVersion = "15",
            deviceMake = "Google",
            deviceModel = "Pixel 9 Pro",
            androidSdkVersion = 35,
            loginSupported = true,
            useSignatureTimestamp = true,
        )

        val ANDROID_CREATOR = YtPlaybackClient(
            key = "ANDROID_CREATOR@23.47.101",
            clientName = "ANDROID_CREATOR",
            clientVersion = "23.47.101",
            clientId = "14",
            userAgent = "com.google.android.apps.youtube.creator/23.47.101 (Linux; U; Android 15; en_US; Pixel 9 Pro Fold; Build/AP3A.241005.015.A2; Cronet/132.0.6779.0)",
            osName = "Android",
            osVersion = "15",
            deviceMake = "Google",
            deviceModel = "Pixel 9 Pro Fold",
            androidSdkVersion = 35,
            loginSupported = true,
            useSignatureTimestamp = true,
        )

        val TVHTML5_SIMPLY_EMBEDDED_PLAYER = YtPlaybackClient(
            key = "TVHTML5_SIMPLY_EMBEDDED_PLAYER@2.0",
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            clientId = "85",
            userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.02) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15",
            loginSupported = true,
            useSignatureTimestamp = true,
            isEmbedded = true,
        )

        val WEB_REMIX = YtPlaybackClient(
            key = "WEB_REMIX@1.20260213.01.00",
            clientName = "WEB_REMIX",
            clientVersion = "1.20260213.01.00",
            clientId = "67",
            userAgent = USER_AGENT_WEB_REMIX,
            loginSupported = true,
            useSignatureTimestamp = true,
        )

        /**
         * Ordered Metrolist-style playback fallback ladder.
         */
        val PLAYBACK_LADDER: List<YtPlaybackClient> = listOf(
            VISIONOS,
            ANDROID_VR_NO_AUTH,
            ANDROID_VR_1_61_48,
            ANDROID_VR_1_43_32,
            IOS,
            IOS_MUSIC,
            IPADOS,
            ANDROID_TESTSUITE,
            ANDROID_MUSIC,
            MOBILE,
            ANDROID_CREATOR,
            TVHTML5_SIMPLY_EMBEDDED_PLAYER,
            WEB_REMIX,
        )

        /**
         * Resolves the exact User-Agent, Origin, and Referer required for a `googlevideo.com` URL
         * by reading its `?c=...&cver=...` query parameters. Used by both stream validation probes
         * and ExoPlayer's OkHttp interceptor so CDN requests NEVER fail with User-Agent mismatch 403!
         */
        fun resolveHeadersForStreamUrl(url: String): StreamRequestHeaders {
            val c = extractQueryParam(url, "c")?.uppercase(Locale.US).orEmpty()
            val cver = extractQueryParam(url, "cver").orEmpty()
            val client = when {
                c == "VISIONOS" -> VISIONOS
                c == "ANDROID_VR" -> when (cver) {
                    ANDROID_VR_1_61_48.clientVersion -> ANDROID_VR_1_61_48
                    ANDROID_VR_1_43_32.clientVersion -> ANDROID_VR_1_43_32
                    else -> ANDROID_VR_NO_AUTH
                }
                c == "IOS_MUSIC" -> IOS_MUSIC
                c.startsWith("IOS") -> if (cver == IPADOS.clientVersion) IPADOS else IOS
                c == "ANDROID_MUSIC" -> ANDROID_MUSIC
                c == "ANDROID_TESTSUITE" -> ANDROID_TESTSUITE
                c == "ANDROID_CREATOR" -> ANDROID_CREATOR
                c.startsWith("ANDROID") -> MOBILE
                c.startsWith("TVHTML5") -> TVHTML5_SIMPLY_EMBEDDED_PLAYER
                c == "WEB_REMIX" || c == "WEB" -> WEB_REMIX
                else -> ANDROID_VR_NO_AUTH
            }
            return StreamRequestHeaders(
                userAgent = client.userAgent,
                origin = if (client.isWebLike) client.origin else null,
                referer = if (client.isWebLike) client.referer else null,
                clientKey = client.key,
            )
        }

        fun patchClientVersion(url: String, clientVersion: String): String {
            if (!url.contains("cver=")) return url
            return url.replace(Regex("cver=[^&]+"), "cver=$clientVersion")
        }

        private fun extractQueryParam(url: String, name: String): String? {
            val query = url.substringAfter('?', "")
            if (query.isEmpty()) return null
            return query.split('&').firstOrNull { it.startsWith("$name=") }?.substringAfter('=')
        }
    }
}

data class StreamRequestHeaders(
    val userAgent: String,
    val origin: String? = null,
    val referer: String? = null,
    val clientKey: String = "",
)
