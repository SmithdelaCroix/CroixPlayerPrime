/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap

object LrcParser {
    private val lineRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?](.*)""")

    fun parse(lrcContent: String): List<SyncedLyricLine> {
        if (lrcContent.isBlank()) return emptyList()
        val lines = mutableListOf<SyncedLyricLine>()
        lrcContent.lineSequence().forEach { rawLine ->
            val match = lineRegex.find(rawLine.trim()) ?: return@forEach
            val min = match.groupValues[1].toLongOrNull() ?: return@forEach
            val sec = match.groupValues[2].toLongOrNull() ?: return@forEach
            val fracRaw = match.groupValues[3]
            val millis = when (fracRaw.length) {
                0 -> 0L
                1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
                2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
                else -> fracRaw.take(3).toLongOrNull() ?: 0L
            }
            val text = match.groupValues[4].trim()
            if (text.isNotEmpty()) {
                lines += SyncedLyricLine(timeMs = min * 60_000L + sec * 1_000L + millis, text = text)
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    fun activeLineIndex(lines: List<SyncedLyricLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var active = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) active = i else break
        }
        return active
    }
}

class LrclibClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient(OkHttp)
    private val cache = ConcurrentHashMap<String, LyricsResult>()

    suspend fun fetchLyrics(track: Track): LyricsResult? = withContext(Dispatchers.IO) {
        cache[track.id]?.let { return@withContext it }

        val cleanTitle = track.title.replace(Regex("""\s*[\(\[](Official|Video|Audio|Lyrics|HD|4K).*?[\)\]]""", RegexOption.IGNORE_CASE), "").trim()
        val cleanArtist = track.artist.split(",", "&", "feat.", "ft.", ignoreCase = true).firstOrNull()?.trim().orEmpty()

        val exact = runCatching {
            val resp = http.get("https://lrclib.net/api/get") {
                header("User-Agent", "CroixPlayerPrime/1.0 (Android)")
                parameter("track_name", cleanTitle)
                parameter("artist_name", cleanArtist)
                if (track.durationSeconds > 0) parameter("duration", track.durationSeconds)
            }
            if (resp.status.isSuccess()) {
                parseLyricsObj(json.parseToJsonElement(resp.bodyAsText()).jsonObject)
            } else null
        }.getOrNull()

        val result = exact ?: runCatching {
            val resp = http.get("https://lrclib.net/api/search") {
                header("User-Agent", "CroixPlayerPrime/1.0 (Android)")
                parameter("q", "$cleanTitle $cleanArtist")
            }
            if (!resp.status.isSuccess()) return@runCatching null
            val list = json.parseToJsonElement(resp.bodyAsText()).jsonArray
            val best = list.mapNotNull { it as? JsonObject }
                .firstOrNull { !it["syncedLyrics"]?.jsonPrimitive?.contentOrNull.isNullOrBlank() }
                ?: list.firstOrNull()?.jsonObject
            best?.let(::parseLyricsObj)
        }.getOrNull()

        if (result != null) cache[track.id] = result
        result
    }

    private fun parseLyricsObj(obj: JsonObject): LyricsResult? {
        val syncedRaw = obj["syncedLyrics"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val plainRaw = obj["plainLyrics"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (syncedRaw.isBlank() && plainRaw.isBlank()) return null
        return LyricsResult(
            synced = LrcParser.parse(syncedRaw),
            plain = plainRaw,
            source = "LRCLib",
        )
    }
}
