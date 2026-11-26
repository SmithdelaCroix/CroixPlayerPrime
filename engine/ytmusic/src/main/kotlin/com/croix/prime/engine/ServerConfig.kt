/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

@Serializable
enum class YtMusicServerMode(val labelEn: String, val labelEs: String) {
    DIRECT_ADFREE("Direct Ad-Free", "Directo Sin Anuncios"),
    PIPED_RELAY("Piped Relay Server", "Servidor Relay Piped"),
    CUSTOM_INSTANCE("Custom Server URL", "Servidor Personalizado"),
}

@Serializable
data class PipedRelayPreset(
    val name: String,
    val apiBaseUrl: String,
    val region: String,
)

object ServerPresets {
    val PipedNodes: List<PipedRelayPreset> = listOf(
        PipedRelayPreset("Kavin Global", "https://pipedapi.kavin.rocks", "Global"),
        PipedRelayPreset("AdminForge EU", "https://pipedapi.adminforge.de", "Europe"),
        PipedRelayPreset("Private Coffee", "https://api.piped.private.coffee", "Austria"),
        PipedRelayPreset("R4FO Privacy", "https://pipedapi.r4fo.com", "Germany"),
    )
}

@Serializable
data class ServerConnectionConfig(
    val mode: YtMusicServerMode = YtMusicServerMode.DIRECT_ADFREE,
    val pipedBaseUrl: String = ServerPresets.PipedNodes.first().apiBaseUrl,
    val customServerUrl: String = "",
    val strictAdBlockEnabled: Boolean = true,
    val ytMusicCookie: String = "",
    val ytMusicAccountName: String = "",
    val ytMusicAccountEmail: String = "",
    val ytMusicAccountHandle: String = "",
    val ytMusicAccountAvatarUrl: String = "",
    val ytMusicVisitorData: String = "",
    val ytMusicDataSyncId: String = "",
    val ytMusicPageId: String = "",
    val ytMusicAuthUser: String = "0",
    val ytMusicAutoSync: Boolean = true,
) {
    val activeRelayUrl: String
        get() = when (mode) {
            YtMusicServerMode.PIPED_RELAY -> pipedBaseUrl.trim().trimEnd('/')
            YtMusicServerMode.CUSTOM_INSTANCE -> customServerUrl.trim().trimEnd('/')
            YtMusicServerMode.DIRECT_ADFREE -> "https://music.youtube.com"
        }

    val hasYtMusicAccount: Boolean
        get() = ytMusicCookie.contains("SAPISID=") || ytMusicCookie.contains("__Secure-3PAPISID=")

    val effectiveOnBehalfOfUser: String
        get() = ytMusicPageId.trim().ifBlank {
            ytMusicDataSyncId.substringBefore("||").trim()
        }
}

@Serializable
data class ServerHealthStatus(
    val reachable: Boolean = false,
    val latencyMs: Long = -1L,
    val endpointLabel: String = "music.youtube.com",
    val message: String = "",
)

/**
 * Engine-level ad & promoted-content blocker for YouTube Music InnerTube payloads.
 * Strips ad slots, sponsored shelves, promoted items, and pre/mid-roll player ad pods.
 */
object AdBlockEngine {
    val blockedCount = AtomicInteger(0)

    private val adObjectKeys = setOf(
        "adSlotRenderer",
        "promotedSparklesWebRenderer",
        "promotedSparklesTextSearchRenderer",
        "promotedVideoRenderer",
        "inFeedAdLayoutRenderer",
        "displayAdRenderer",
        "actionCompanionAdRenderer",
        "statementBannerRenderer",
        "brandVideoSingletonRenderer",
        "brandVideoShelfRenderer",
        "mealbarPromoRenderer",
    )

    fun isAdOrPromotedNode(obj: JsonObject): Boolean {
        for (key in obj.keys) {
            if (key in adObjectKeys) {
                blockedCount.incrementAndGet()
                return true
            }
        }
        // Detect promoted badge markers inside list items or shelves
        val badges = obj["badges"]?.jsonArray
        if (badges != null) {
            for (b in badges) {
                val txt = b.toString().lowercase()
                if ("sponsored" in txt || "ad_badge" in txt || "promoted" in txt) {
                    blockedCount.incrementAndGet()
                    return true
                }
            }
        }
        return false
    }

    fun isSponsoredShelfTitle(title: String): Boolean {
        val lower = title.lowercase()
        val blocked = lower == "sponsored" || lower == "promoted" || lower == "patrocinado" || lower == "anuncio"
        if (blocked) blockedCount.incrementAndGet()
        return blocked
    }

    fun countAndStripPlayerAds(playerRoot: JsonObject): Int {
        var stripped = 0
        playerRoot["adPlacements"]?.jsonArray?.size?.let { if (it > 0) stripped += it }
        playerRoot["playerAds"]?.jsonArray?.size?.let { if (it > 0) stripped += it }
        playerRoot["adSlots"]?.jsonArray?.size?.let { if (it > 0) stripped += it }
        if (stripped > 0) blockedCount.addAndGet(stripped)
        return stripped
    }

    fun isAdTrackingStreamUrl(url: String): Boolean {
        val lower = url.lowercase()
        val isAd = "doubleclick.net" in lower ||
            "googleads." in lower ||
            "&adformat=" in lower ||
            "pagead/interaction" in lower
        if (isAd) blockedCount.incrementAndGet()
        return isAd
    }

    fun isPlayableNonAdObject(el: JsonElement): Boolean =
        el !is JsonObject || !isAdOrPromotedNode(el)
}

/**
 * Generates standard SAPISIDHASH Authorization headers for authenticated YouTube Music sessions.
 */
object SapisidAuthSigner {
    fun extractSapisid(cookieHeader: String): String? {
        val pairs = cookieHeader.split(";").map { it.trim() }
        for (target in listOf("__Secure-3PAPISID", "SAPISID", "__Secure-1PAPISID")) {
            val found = pairs.firstOrNull { it.startsWith("$target=") }
                ?.substringAfter("=")
                ?.trim()
            if (!found.isNullOrBlank()) return found
        }
        return null
    }

    fun buildAuthHeader(
        cookieHeader: String,
        origin: String = "https://music.youtube.com",
        epochSeconds: Long = System.currentTimeMillis() / 1000L,
    ): String? {
        val sapisid = extractSapisid(cookieHeader) ?: return null
        val digest = MessageDigest.getInstance("SHA-1")
            .digest("$epochSeconds $sapisid $origin".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "SAPISIDHASH ${epochSeconds}_$digest"
    }
}
