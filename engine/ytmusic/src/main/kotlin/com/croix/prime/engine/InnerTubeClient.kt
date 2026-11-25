/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NpRequest
import org.schabi.newpipe.extractor.downloader.Response as NpResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class InnerTubeClient(
    private val localeHl: String = "en",
    private val localeGl: String = "US",
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val http = HttpClient(OkHttp) {
        engine { preconfigured = okHttp }
        install(ContentNegotiation) { json(json) }
    }

    private val streamCache = ConcurrentHashMap<String, ResolvedStream>()

    init {
        ensureNewPipeInit(okHttp)
    }

    val defaultMoods: List<MoodChip> = listOf(
        MoodChip("Focus", "focus instrumental music", 0xFF00E5FF),
        MoodChip("Workout", "high energy workout music", 0xFFFF3D71),
        MoodChip("Chill", "chill lofi night vibes", 0xFFA238FF),
        MoodChip("Party", "electronic dance hits", 0xFFFFAA00),
        MoodChip("Romance", "romantic acoustic pop", 0xFFFF4081),
        MoodChip("Sleep", "ambient deep sleep music", 0xFF3D5AFE),
    )

    suspend fun fetchHomeSections(): List<HomeSection> = withContext(Dispatchers.IO) {
        val sections = mutableListOf<HomeSection>()
        runCatching {
            val payload = buildWebRemixContext {
                put("browseId", "FEmusic_home")
            }
            val raw = postInnerTube("browse", payload)
            val root = json.parseToJsonElement(raw).jsonObject
            parseHomeShelves(root, sections)
        }

        if (sections.isEmpty()) {
            // Reliable fallback via curated YT Music searches so Home is never empty
            val quick = searchSongs("top hits music 2026", limit = 16)
            val chill = searchSongs("chill electronic pop hits", limit = 12)
            if (quick.isNotEmpty()) sections += HomeSection("Quick Picks", quick)
            if (chill.isNotEmpty()) sections += HomeSection("Trending Mix", chill)
        }
        sections
    }

    suspend fun searchSongs(query: String, limit: Int = 25): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching {
            val payload = buildWebRemixContext {
                put("query", query.trim())
                // Filter: Songs on YouTube Music
                put("params", "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
            }
            val raw = postInnerTube("search", payload)
            val root = json.parseToJsonElement(raw).jsonObject
            extractTracksFromTree(root).distinctBy { it.id }.take(limit)
        }.getOrElse { emptyList() }
    }

    suspend fun fetchRadioQueue(videoId: String, limit: Int = 20): List<Track> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildWebRemixContext {
                put("videoId", videoId)
                put("playlistId", "RDAMVM$videoId")
                put("isAudioOnly", true)
            }
            val raw = postInnerTube("next", payload)
            val root = json.parseToJsonElement(raw).jsonObject
            extractPlaylistPanelTracks(root).distinctBy { it.id }.take(limit)
        }.getOrElse { emptyList() }
    }

    suspend fun resolveAudioStream(videoId: String): Result<ResolvedStream> = withContext(Dispatchers.IO) {
        streamCache[videoId]?.takeIf { it.expiresAtEpochMs > System.currentTimeMillis() + 60_000L }?.let {
            return@withContext Result.success(it)
        }

        // Strategy 1: Direct InnerTube ANDROID_VR client (returns direct un-throttled audio URLs without signature cipher)
        val vrStream = runCatching { fetchDirectPlayerStream(videoId, clientName = "ANDROID_VR", clientVersion = "1.60.19") }.getOrNull()
        if (vrStream != null) {
            streamCache[videoId] = vrStream
            return@withContext Result.success(vrStream)
        }

        // Strategy 2: Direct InnerTube IOS client
        val iosStream = runCatching { fetchDirectPlayerStream(videoId, clientName = "IOS", clientVersion = "20.10.4") }.getOrNull()
        if (iosStream != null) {
            streamCache[videoId] = iosStream
            return@withContext Result.success(iosStream)
        }

        // Strategy 3: NewPipeExtractor fallback (handles JS signature deciphering automatically)
        runCatching {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val extractor = ServiceList.YouTube.getStreamExtractor(url)
            extractor.fetchPage()
            val best = extractor.audioStreams
                .filter { !it.content.isNullOrBlank() }
                .maxByOrNull { it.averageBitrate }
                ?: error("No playable audio stream found for $videoId")
            ResolvedStream(
                url = best.content,
                mimeType = best.format?.mimeType ?: "audio/mp4",
                bitrate = best.averageBitrate,
            ).also { streamCache[videoId] = it }
        }
    }

    private suspend fun fetchDirectPlayerStream(
        videoId: String,
        clientName: String,
        clientVersion: String,
    ): ResolvedStream? {
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", clientName)
                    put("clientVersion", clientVersion)
                    put("hl", localeHl)
                    put("gl", localeGl)
                    if (clientName == "ANDROID_VR") {
                        put("androidSdkVersion", 32)
                        put("deviceMake", "Oculus")
                        put("deviceModel", "Quest 3")
                    } else if (clientName == "IOS") {
                        put("deviceMake", "Apple")
                        put("deviceModel", "iPhone16,2")
                    }
                }
            }
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }

        val responseText = http.post("https://youtubei.googleapis.com/youtubei/v1/player?prettyPrint=false") {
            contentType(ContentType.Application.Json)
            header("User-Agent", "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip")
            setBody(payload.toString())
        }.bodyAsText()

        val root = json.parseToJsonElement(responseText).jsonObject
        val streamingData = root["streamingData"]?.jsonObject ?: return null
        val adaptive = streamingData["adaptiveFormats"]?.jsonArray ?: return null

        val bestAudio = adaptive
            .mapNotNull { it as? JsonObject }
            .filter { fmt ->
                val mime = fmt["mimeType"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val directUrl = fmt["url"]?.jsonPrimitive?.contentOrNull
                mime.startsWith("audio/") && !directUrl.isNullOrBlank()
            }
            .maxByOrNull { it["bitrate"]?.jsonPrimitive?.intOrNull ?: 0 }
            ?: return null

        return ResolvedStream(
            url = bestAudio["url"]!!.jsonPrimitive.content,
            mimeType = bestAudio["mimeType"]?.jsonPrimitive?.contentOrNull ?: "audio/mp4",
            bitrate = bestAudio["bitrate"]?.jsonPrimitive?.intOrNull ?: 128_000,
        )
    }

    private suspend fun postInnerTube(endpoint: String, body: JsonObject): String {
        return http.post("https://music.youtube.com/youtubei/v1/$endpoint?prettyPrint=false") {
            contentType(ContentType.Application.Json)
            header("Origin", "https://music.youtube.com")
            header("Referer", "https://music.youtube.com/")
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
            setBody(body.toString())
        }.bodyAsText()
    }

    private fun buildWebRemixContext(extra: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): JsonObject =
        buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20250401.01.00")
                    put("hl", localeHl)
                    put("gl", localeGl)
                }
            }
            extra()
        }

    private fun parseHomeShelves(root: JsonObject, out: MutableList<HomeSection>) {
        fun walk(el: JsonElement) {
            when (el) {
                is JsonObject -> {
                    val shelf = el["musicCarouselShelfRenderer"]?.jsonObject
                    if (shelf != null) {
                        val title = shelf["header"]?.jsonObject
                            ?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObject
                            ?.get("title")?.jsonObject
                            ?.get("runs")?.jsonArray
                            ?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.contentOrNull
                            ?: "For You"
                        val tracks = extractTracksFromTree(shelf).distinctBy { it.id }
                        if (tracks.isNotEmpty()) {
                            out += HomeSection(title = title, tracks = tracks)
                        }
                    }
                    el.values.forEach { walk(it) }
                }
                is JsonArray -> el.forEach { walk(it) }
                else -> Unit
            }
        }
        walk(root)
    }

    private fun extractTracksFromTree(element: JsonElement): List<Track> {
        val result = mutableListOf<Track>()
        fun walk(el: JsonElement) {
            when (el) {
                is JsonObject -> {
                    el["musicResponsiveListItemRenderer"]?.jsonObject?.let { item ->
                        parseListItem(item)?.let(result::add)
                    }
                    el["musicTwoRowItemRenderer"]?.jsonObject?.let { item ->
                        parseTwoRowItem(item)?.let(result::add)
                    }
                    el.values.forEach { walk(it) }
                }
                is JsonArray -> el.forEach { walk(it) }
                else -> Unit
            }
        }
        walk(element)
        return result
    }

    private fun parseListItem(item: JsonObject): Track? {
        val videoId = item["playlistItemData"]?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull
            ?: findFirstVideoId(item)
            ?: return null
        val columns = item["flexColumns"]?.jsonArray ?: return null
        val title = columns.getOrNull(0)?.let(::extractColumnText).orEmpty().ifBlank { return null }
        val subtitleRuns = columns.getOrNull(1)?.let(::extractColumnRuns).orEmpty()
        val artist = subtitleRuns.firstOrNull { it != " • " && !it.contains(":") } ?: "Unknown Artist"
        val duration = subtitleRuns.lastOrNull { it.contains(":") }?.let(::parseDurationToSeconds) ?: 0
        val thumb = extractBestThumbnail(item["thumbnail"])
        return Track(
            id = videoId,
            title = title,
            artist = artist,
            durationSeconds = duration,
            thumbnailUrl = upgradeThumbResolution(thumb),
        )
    }

    private fun parseTwoRowItem(item: JsonObject): Track? {
        val videoId = item["navigationEndpoint"]?.jsonObject
            ?.get("watchEndpoint")?.jsonObject
            ?.get("videoId")?.jsonPrimitive?.contentOrNull
            ?: return null
        val title = item["title"]?.jsonObject?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
            ?: return null
        val artist = item["subtitle"]?.jsonObject?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
            ?: "YouTube Music"
        val thumb = extractBestThumbnail(item["thumbnailRenderer"])
        return Track(
            id = videoId,
            title = title,
            artist = artist,
            thumbnailUrl = upgradeThumbResolution(thumb),
        )
    }

    private fun extractPlaylistPanelTracks(root: JsonObject): List<Track> {
        val tracks = mutableListOf<Track>()
        fun walk(el: JsonElement) {
            when (el) {
                is JsonObject -> {
                    el["playlistPanelVideoRenderer"]?.jsonObject?.let { r ->
                        val vid = r["videoId"]?.jsonPrimitive?.contentOrNull
                        val title = r["title"]?.jsonObject?.get("runs")?.jsonArray
                            ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                        val artist = r["longBylineText"]?.jsonObject?.get("runs")?.jsonArray
                            ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                            ?: "Unknown Artist"
                        val durText = r["lengthText"]?.jsonObject?.get("runs")?.jsonArray
                            ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                        val thumb = extractBestThumbnail(r["thumbnail"])
                        if (!vid.isNullOrBlank() && !title.isNullOrBlank()) {
                            tracks += Track(
                                id = vid,
                                title = title,
                                artist = artist,
                                durationSeconds = durText?.let(::parseDurationToSeconds) ?: 0,
                                thumbnailUrl = upgradeThumbResolution(thumb),
                            )
                        }
                    }
                    el.values.forEach { walk(it) }
                }
                is JsonArray -> el.forEach { walk(it) }
                else -> Unit
            }
        }
        walk(root)
        return tracks
    }

    private fun findFirstVideoId(el: JsonElement): String? = when (el) {
        is JsonObject -> el["videoId"]?.jsonPrimitive?.contentOrNull
            ?: el.values.firstNotNullOfOrNull(::findFirstVideoId)
        is JsonArray -> el.firstNotNullOfOrNull(::findFirstVideoId)
        else -> null
    }

    private fun extractColumnText(el: JsonElement): String =
        extractColumnRuns(el).joinToString("")

    private fun extractColumnRuns(el: JsonElement): List<String> {
        val runs = mutableListOf<String>()
        fun walk(node: JsonElement) {
            when (node) {
                is JsonObject -> {
                    node["runs"]?.jsonArray?.forEach { r ->
                        r.jsonObject["text"]?.jsonPrimitive?.contentOrNull?.let(runs::add)
                    }
                    if (runs.isEmpty()) node.values.forEach(::walk)
                }
                is JsonArray -> node.forEach(::walk)
                else -> Unit
            }
        }
        walk(el)
        return runs
    }

    private fun extractBestThumbnail(el: JsonElement?): String {
        if (el == null) return ""
        val urls = mutableListOf<String>()
        fun walk(node: JsonElement) {
            when (node) {
                is JsonObject -> {
                    node["thumbnails"]?.jsonArray?.forEach { t ->
                        t.jsonObject["url"]?.jsonPrimitive?.contentOrNull?.let(urls::add)
                    }
                    node.values.forEach(::walk)
                }
                is JsonArray -> node.forEach(::walk)
                else -> Unit
            }
        }
        walk(el)
        return urls.lastOrNull().orEmpty()
    }

    private fun upgradeThumbResolution(url: String): String {
        if (url.isBlank()) return ""
        return url.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
            .replace(Regex("=s\\d+"), "=s544")
    }

    private fun parseDurationToSeconds(text: String): Int {
        val parts = text.trim().split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0
        }
    }

    companion object {
        @Volatile
        private var newPipeInitialized = false

        private fun ensureNewPipeInit(client: OkHttpClient) {
            if (newPipeInitialized) return
            synchronized(this) {
                if (newPipeInitialized) return
                NewPipe.init(object : Downloader() {
                    override fun execute(request: NpRequest): NpResponse {
                        val builder = Request.Builder().url(request.url())
                        request.headers().forEach { (k, values) ->
                            values.forEach { v -> builder.addHeader(k, v) }
                        }
                        val bodyBytes = request.dataToSend()
                        when (request.httpMethod()) {
                            "POST" -> builder.post((bodyBytes ?: ByteArray(0)).toRequestBody("application/json".toMediaType()))
                            "HEAD" -> builder.head()
                            else -> builder.get()
                        }
                        client.newCall(builder.build()).execute().use { resp ->
                            return NpResponse(
                                resp.code,
                                resp.message,
                                resp.headers.toMultimap(),
                                resp.body?.string().orEmpty(),
                                resp.request.url.toString(),
                            )
                        }
                    }
                })
                newPipeInitialized = true
            }
        }
    }
}
