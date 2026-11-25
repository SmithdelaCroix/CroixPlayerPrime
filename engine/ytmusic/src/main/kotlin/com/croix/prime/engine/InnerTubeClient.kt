/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
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
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
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
    private val failedClientsByVideo = ConcurrentHashMap<String, MutableSet<String>>()

    @Volatile
    private var cachedVisitorData: String? = null

    @Volatile
    private var lastSuccessfulClientKey: String? = null

    @Volatile
    var serverConfig: ServerConnectionConfig = ServerConnectionConfig()

    fun updateServerConfig(config: ServerConnectionConfig) {
        val modeChanged = serverConfig.mode != config.mode || serverConfig.activeRelayUrl != config.activeRelayUrl
        serverConfig = config
        if (modeChanged) streamCache.clear()
    }

    fun markStreamClientFailed(videoId: String, clientKey: String) {
        if (clientKey.isBlank()) return
        failedClientsByVideo.computeIfAbsent(videoId) { ConcurrentHashMap.newKeySet() }.add(clientKey)
        streamCache.remove(videoId)
        if (lastSuccessfulClientKey == clientKey) {
            lastSuccessfulClientKey = null
        }
    }

    fun invalidateStreamCache(videoId: String) {
        streamCache.remove(videoId)
    }

    private suspend fun ensureVisitorData(): String? {
        cachedVisitorData?.takeIf { it.isNotBlank() }?.let { return it }
        return runCatching {
            val raw = http.get("https://music.youtube.com/sw.js_data") {
                header("User-Agent", YtPlaybackClient.USER_AGENT_WEB_REMIX)
                header("Origin", YtPlaybackClient.ORIGIN_YOUTUBE_MUSIC)
                header("Referer", "${YtPlaybackClient.ORIGIN_YOUTUBE_MUSIC}/")
            }.bodyAsText()
            val cleaned = raw.removePrefix(")]}'")
            val match = Regex("""["'](Cg[A-Za-z0-9_\-%]{12,})["']""").find(cleaned)?.groupValues?.get(1)
            if (!match.isNullOrBlank()) {
                cachedVisitorData = match
            }
            cachedVisitorData
        }.getOrNull()
    }

    suspend fun pingActiveServer(): ServerHealthStatus = withContext(Dispatchers.IO) {
        val cfg = serverConfig
        val targetUrl = when (cfg.mode) {
            YtMusicServerMode.DIRECT_ADFREE -> "https://music.youtube.com/generate_204"
            YtMusicServerMode.PIPED_RELAY, YtMusicServerMode.CUSTOM_INSTANCE -> {
                val base = cfg.activeRelayUrl.ifBlank { "https://music.youtube.com" }
                if (base.contains("music.youtube.com")) "$base/generate_204" else "$base/trending?region=US"
            }
        }
        val label = cfg.activeRelayUrl.removePrefix("https://").removePrefix("http://").ifBlank { "music.youtube.com" }
        val start = System.currentTimeMillis()
        runCatching {
            val resp = http.get(targetUrl) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }
            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            val ok = resp.status.value in 200..399
            ServerHealthStatus(
                reachable = ok,
                latencyMs = elapsed,
                endpointLabel = label,
                message = if (ok) "HTTP ${resp.status.value}" else "HTTP ${resp.status.value}",
            )
        }.getOrElse { err ->
            ServerHealthStatus(
                reachable = false,
                latencyMs = -1L,
                endpointLabel = label,
                message = err.message ?: "Unreachable",
            )
        }
    }

    suspend fun verifyYtMusicAccount(cookie: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val auth = SapisidAuthSigner.buildAuthHeader(cookie)
                ?: error("Missing SAPISID session cookie")
            val payload = buildWebRemixContext {}
            val respText = http.post("https://music.youtube.com/youtubei/v1/account/account_menu?prettyPrint=false") {
                contentType(ContentType.Application.Json)
                header("Origin", "https://music.youtube.com")
                header("Referer", "https://music.youtube.com/")
                header("Authorization", auth)
                header("Cookie", cookie)
                header("X-Origin", "https://music.youtube.com")
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                setBody(payload.toString())
            }.bodyAsText()
            val root = json.parseToJsonElement(respText).jsonObject
            val header = findFirstObjectByKey(root, "activeAccountHeaderRenderer")
            val accountName = header?.get("accountName")?.let(::extractColumnText).orEmpty().ifBlank {
                header?.get("channelHandle")?.let(::extractColumnText).orEmpty()
            }.ifBlank { "YouTube Music Account" }
            accountName
        }
    }

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

    suspend fun searchArtists(query: String, limit: Int = 15): List<RelatedArtist> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching {
            val payload = buildWebRemixContext {
                put("query", query.trim())
                put("params", "EgWKAQIgAWoKEAkQBRAKEAMQBA%3D%3D")
            }
            val raw = postInnerTube("search", payload)
            val root = json.parseToJsonElement(raw).jsonObject
            val out = mutableListOf<RelatedArtist>()
            findAllObjectsByKey(root, "musicResponsiveListItemRenderer").forEach { item ->
                val bid = item["navigationEndpoint"]?.jsonObject
                    ?.get("browseEndpoint")?.jsonObject
                    ?.get("browseId")?.jsonPrimitive?.contentOrNull
                val cols = item["flexColumns"]?.jsonArray
                val name = cols?.getOrNull(0)?.let(::extractColumnText).orEmpty()
                val subs = cols?.getOrNull(1)?.let(::extractColumnText).orEmpty()
                val thumb = upgradeThumbResolution(extractBestThumbnail(item["thumbnail"]))
                if (!bid.isNullOrBlank() && bid.startsWith("UC") && name.isNotBlank()) {
                    out += RelatedArtist(bid, name, subs, thumb)
                }
            }
            out.distinctBy { it.browseId }.take(limit)
        }.getOrElse { emptyList() }
    }

    suspend fun searchAlbums(query: String, limit: Int = 15): List<AlbumSummary> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching {
            val payload = buildWebRemixContext {
                put("query", query.trim())
                put("params", "EgWKAQIYAWoKEAkQBRAKEAMQBA%3D%3D")
            }
            val raw = postInnerTube("search", payload)
            val root = json.parseToJsonElement(raw).jsonObject
            val out = mutableListOf<AlbumSummary>()
            findAllObjectsByKey(root, "musicResponsiveListItemRenderer").forEach { item ->
                val bid = item["navigationEndpoint"]?.jsonObject
                    ?.get("browseEndpoint")?.jsonObject
                    ?.get("browseId")?.jsonPrimitive?.contentOrNull
                val cols = item["flexColumns"]?.jsonArray
                val title = cols?.getOrNull(0)?.let(::extractColumnText).orEmpty()
                val subRuns = cols?.getOrNull(1)?.let(::extractColumnRuns).orEmpty()
                val year = subRuns.firstOrNull { it.length == 4 && it.all(Char::isDigit) }
                    ?: subRuns.firstOrNull { it != " • " && !it.equals("Album", true) }.orEmpty()
                val thumb = upgradeThumbResolution(extractBestThumbnail(item["thumbnail"]))
                if (!bid.isNullOrBlank() && title.isNotBlank()) {
                    out += AlbumSummary(bid, title, year, thumb, isSingle = false)
                }
            }
            out.distinctBy { it.browseId }.take(limit)
        }.getOrElse { emptyList() }
    }

    suspend fun importPlaylistFromUrl(rawUrl: String): Result<Pair<String, List<Track>>> = withContext(Dispatchers.IO) {
        runCatching {
            val listId = Regex("""[?&]list=([A-Za-z0-9_-]+)""").find(rawUrl.trim())?.groupValues?.get(1)
                ?: rawUrl.trim().takeIf { it.startsWith("PL") || it.startsWith("OLAK") || it.startsWith("RDCLAK") }
                ?: error("Invalid YouTube / YouTube Music playlist URL")

            val browseId = if (listId.startsWith("VL")) listId else "VL$listId"
            val payload = buildWebRemixContext {
                put("browseId", browseId)
            }
            val raw = postInnerTube("browse", payload)
            val root = json.parseToJsonElement(raw).jsonObject
            val header = findFirstObjectByKey(root, "musicResponsiveHeaderRenderer")
                ?: findFirstObjectByKey(root, "musicDetailHeaderRenderer")
            val title = header?.get("title")?.let(::extractColumnText).orEmpty().ifBlank { "Imported Playlist" }
            val tracks = extractTracksFromTree(root).distinctBy { it.id }
            if (tracks.isEmpty()) error("No playable tracks found in playlist")
            title to tracks
        }
    }

    suspend fun fetchArtistProfile(
        artistName: String,
        browseId: String? = null,
    ): ArtistProfile = withContext(Dispatchers.IO) {
        val cleanName = artistName.split(",", "&", "feat.", "ft.", ignoreCase = true)
            .firstOrNull()?.trim()?.ifBlank { artistName } ?: artistName

        // Step 1: Resolve exact YouTube Music Artist browseId (UC...) if not supplied
        val resolvedBrowseId = browseId?.takeIf { it.isNotBlank() } ?: findArtistBrowseId(cleanName)

        if (!resolvedBrowseId.isNullOrBlank()) {
            val browsed = runCatching { fetchFullArtistPage(resolvedBrowseId, cleanName) }.getOrNull()
            if (browsed != null && (browsed.topTracks.isNotEmpty() || browsed.albums.isNotEmpty() || browsed.singles.isNotEmpty())) {
                return@withContext browsed
            }
        }

        // Graceful fallback if artist has no standard YT Music channel page
        val tracks = searchSongs(cleanName, limit = 20)
        val exact = tracks.filter { it.artist.contains(cleanName, ignoreCase = true) }
        val top = if (exact.size >= 4) exact else tracks
        ArtistProfile(
            browseId = resolvedBrowseId.orEmpty(),
            name = cleanName,
            bannerUrl = top.firstOrNull()?.thumbnailUrl.orEmpty(),
            topTracks = top.take(10),
        )
    }

    suspend fun fetchAlbumDetail(summary: AlbumSummary): AlbumDetail = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildWebRemixContext {
                put("browseId", summary.browseId)
            }
            val raw = postInnerTube("browse", payload)
            val root = json.parseToJsonElement(raw).jsonObject

            val header = findFirstObjectByKey(root, "musicResponsiveHeaderRenderer")
                ?: findFirstObjectByKey(root, "musicDetailHeaderRenderer")

            val title = header?.get("title")?.let(::extractColumnText).orEmpty().ifBlank { summary.title }
            val subtitleRuns = header?.get("subtitle")?.let(::extractColumnRuns).orEmpty()
            val strapRuns = header?.get("straplineTextOne")?.let(::extractColumnRuns).orEmpty()
            val artist = strapRuns.firstOrNull { it.isNotBlank() }
                ?: subtitleRuns.firstOrNull { it != " • " && !it.all(Char::isDigit) && !it.equals("Album", true) && !it.equals("Single", true) && !it.equals("EP", true) }
                ?: "Artist"
            val year = subtitleRuns.firstOrNull { it.length == 4 && it.all(Char::isDigit) } ?: summary.year
            val thumb = upgradeThumbResolution(
                extractBestThumbnail(header?.get("thumbnail")).ifBlank { summary.thumbnailUrl },
            )

            val rawTracks = extractTracksFromTree(root).distinctBy { it.id }.map { t ->
                t.copy(
                    artist = if (t.artist == "Unknown Artist" || t.artist.isBlank()) artist else t.artist,
                    album = title,
                    thumbnailUrl = t.thumbnailUrl.ifBlank { thumb },
                )
            }
            AlbumDetail(
                browseId = summary.browseId,
                title = title,
                artist = artist,
                year = year,
                thumbnailUrl = thumb,
                tracks = rawTracks,
            )
        }.getOrElse {
            AlbumDetail(
                browseId = summary.browseId,
                title = summary.title,
                artist = "Artist",
                year = summary.year,
                thumbnailUrl = summary.thumbnailUrl,
                tracks = searchSongs(summary.title, limit = 12),
            )
        }
    }

    private suspend fun findArtistBrowseId(artistName: String): String? {
        return runCatching {
            val payload = buildWebRemixContext {
                put("query", artistName)
                // Official YouTube Music Artists filter
                put("params", "EgWKAQIgAWoKEAkQBRAKEAMQBA%3D%3D")
            }
            val raw = postInnerTube("search", payload)
            val root = json.parseToJsonElement(raw).jsonObject
            findFirstBrowseIdWithPrefix(root, "UC")
        }.getOrNull()
    }

    private suspend fun fetchFullArtistPage(browseId: String, fallbackName: String): ArtistProfile {
        val payload = buildWebRemixContext {
            put("browseId", browseId)
        }
        val raw = postInnerTube("browse", payload)
        val root = json.parseToJsonElement(raw).jsonObject

        val header = findFirstObjectByKey(root, "musicImmersiveHeaderRenderer")
            ?: findFirstObjectByKey(root, "musicVisualHeaderRenderer")

        val name = header?.get("title")?.let(::extractColumnText).orEmpty().ifBlank { fallbackName }
        val bannerRaw = extractBestThumbnail(header?.get("thumbnail"))
        val bannerUrl = if (bannerRaw.isNotBlank()) upgradeBannerResolution(bannerRaw) else ""
        val description = header?.get("description")?.let(::extractColumnText).orEmpty().trim()

        val monthlyAudience = header?.get("monthlyListenerCount")?.let(::extractColumnText).orEmpty()
        val subsText = header?.get("subscriptionButton")?.jsonObject
            ?.get("subscribeButtonRenderer")?.jsonObject
            ?.get("longSubscriberCountText")?.let(::extractColumnText).orEmpty()
            .ifBlank {
                header?.get("subscriptionButton")?.jsonObject
                    ?.get("subscribeButtonRenderer")?.jsonObject
                    ?.get("subscriberCountText")?.let(::extractColumnText).orEmpty()
            }
        val listenersLabel = monthlyAudience.ifBlank { subsText }

        // Top Songs from musicShelfRenderer
        val songsShelf = findFirstObjectByKey(root, "musicShelfRenderer")
        val topTracks = songsShelf?.let { shelf ->
            extractTracksFromTree(shelf).distinctBy { it.id }.map { t ->
                t.copy(artist = if (t.artist == "Unknown Artist" || t.artist.isBlank()) name else t.artist)
            }
        }.orEmpty()

        // Carousels: Albums, Singles & EPs, Fans might also like
        val albums = mutableListOf<AlbumSummary>()
        val singles = mutableListOf<AlbumSummary>()
        val related = mutableListOf<RelatedArtist>()

        val carousels = findAllObjectsByKey(root, "musicCarouselShelfRenderer")
        carousels.forEach { shelf ->
            val shelfTitle = shelf["header"]?.jsonObject
                ?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObject
                ?.get("title")?.let(::extractColumnText).orEmpty()
            val lower = shelfTitle.lowercase()
            val contents = shelf["contents"]?.jsonArray ?: return@forEach

            when {
                lower.contains("album") -> {
                    contents.forEach { node ->
                        node.jsonObject["musicTwoRowItemRenderer"]?.jsonObject?.let { item ->
                            parseAlbumTwoRow(item, isSingle = false)?.let(albums::add)
                        }
                    }
                }
                lower.contains("single") || lower.contains("ep") -> {
                    contents.forEach { node ->
                        node.jsonObject["musicTwoRowItemRenderer"]?.jsonObject?.let { item ->
                            parseAlbumTwoRow(item, isSingle = true)?.let(singles::add)
                        }
                    }
                }
                lower.contains("fan") || lower.contains("similar") || lower.contains("artist") -> {
                    contents.forEach { node ->
                        node.jsonObject["musicTwoRowItemRenderer"]?.jsonObject?.let { item ->
                            parseRelatedArtistTwoRow(item)?.let(related::add)
                        }
                    }
                }
            }
        }

        // If artist page had songs hidden behind a shelf, fallback search for top tracks
        val finalTracks = if (topTracks.isNotEmpty()) topTracks else searchSongs(name, limit = 10)

        return ArtistProfile(
            browseId = browseId,
            name = name,
            bannerUrl = bannerUrl.ifBlank { finalTracks.firstOrNull()?.thumbnailUrl.orEmpty() },
            monthlyListeners = listenersLabel,
            description = description,
            topTracks = finalTracks.take(10),
            albums = albums.distinctBy { it.browseId }.take(15),
            singles = singles.distinctBy { it.browseId }.take(15),
            relatedArtists = related.distinctBy { it.browseId }.take(12),
        )
    }

    private fun parseAlbumTwoRow(item: JsonObject, isSingle: Boolean): AlbumSummary? {
        val bid = item["navigationEndpoint"]?.jsonObject
            ?.get("browseEndpoint")?.jsonObject
            ?.get("browseId")?.jsonPrimitive?.contentOrNull
            ?: return null
        val title = item["title"]?.let(::extractColumnText).orEmpty().ifBlank { return null }
        val subRuns = item["subtitle"]?.let(::extractColumnRuns).orEmpty()
        val year = subRuns.firstOrNull { it.length == 4 && it.all(Char::isDigit) }
            ?: subRuns.lastOrNull { it != " • " }.orEmpty()
        val thumb = upgradeThumbResolution(extractBestThumbnail(item["thumbnailRenderer"]))
        return AlbumSummary(
            browseId = bid,
            title = title,
            year = year,
            thumbnailUrl = thumb,
            isSingle = isSingle,
        )
    }

    private fun parseRelatedArtistTwoRow(item: JsonObject): RelatedArtist? {
        val bid = item["navigationEndpoint"]?.jsonObject
            ?.get("browseEndpoint")?.jsonObject
            ?.get("browseId")?.jsonPrimitive?.contentOrNull
            ?: return null
        val name = item["title"]?.let(::extractColumnText).orEmpty().ifBlank { return null }
        val subs = item["subtitle"]?.let(::extractColumnText).orEmpty()
        val thumb = upgradeThumbResolution(extractBestThumbnail(item["thumbnailRenderer"]))
        return RelatedArtist(
            browseId = bid,
            name = name,
            subscribers = subs,
            thumbnailUrl = thumb,
        )
    }

    private fun findFirstBrowseIdWithPrefix(el: JsonElement, prefix: String): String? = when (el) {
        is JsonObject -> {
            val bid = el["browseId"]?.jsonPrimitive?.contentOrNull
            if (bid != null && bid.startsWith(prefix)) bid
            else el.values.firstNotNullOfOrNull { findFirstBrowseIdWithPrefix(it, prefix) }
        }
        is JsonArray -> el.firstNotNullOfOrNull { findFirstBrowseIdWithPrefix(it, prefix) }
        else -> null
    }

    private fun findFirstObjectByKey(el: JsonElement, targetKey: String): JsonObject? = when (el) {
        is JsonObject -> {
            el[targetKey]?.jsonObject ?: el.values.firstNotNullOfOrNull { findFirstObjectByKey(it, targetKey) }
        }
        is JsonArray -> el.firstNotNullOfOrNull { findFirstObjectByKey(it, targetKey) }
        else -> null
    }

    private fun findAllObjectsByKey(el: JsonElement, targetKey: String): List<JsonObject> {
        val out = mutableListOf<JsonObject>()
        fun walk(node: JsonElement) {
            when (node) {
                is JsonObject -> {
                    node[targetKey]?.jsonObject?.let(out::add)
                    node.values.forEach(::walk)
                }
                is JsonArray -> node.forEach(::walk)
                else -> Unit
            }
        }
        walk(el)
        return out
    }

    private fun upgradeBannerResolution(url: String): String {
        if (url.isBlank()) return ""
        return url.replace(Regex("=w\\d+-h\\d+"), "=w1080-h600")
            .replace(Regex("=s\\d+"), "=s1080")
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

    /**
     * Metrolist-grade multi-client audio stream resolver:
     * 1. Bootstraps visitorData + remembers last healthy client.
     * 2. Walks the verified 2026 fallback ladder (VISIONOS -> ANDROID_VR -> IOS -> IOS_MUSIC -> IPADOS -> ANDROID_TESTSUITE -> ANDROID_MUSIC -> MOBILE -> ANDROID_CREATOR -> TVHTML5 -> WEB_REMIX).
     * 3. Handles direct URLs, ?n= throttling deobfuscation, signatureCipher deciphering, and cver patching.
     * 4. Probes candidate stream URLs with `Range: bytes=0-1` and exact client headers so dead/403 streams are skipped before ExoPlayer ever sees them!
     * 5. Falls back to NewPipeExtractor & Piped Relay if all direct clients are blocked on the current IP.
     */
    suspend fun resolveAudioStream(
        videoId: String,
        bypassCache: Boolean = false,
        excludedClientKeys: Set<String> = emptySet(),
    ): Result<ResolvedStream> = withContext(Dispatchers.IO) {
        if (!bypassCache) {
            streamCache[videoId]?.takeIf {
                it.expiresAtEpochMs > System.currentTimeMillis() + 60_000L && it.clientKey !in excludedClientKeys
            }?.let {
                return@withContext Result.success(it)
            }
        }

        val cfg = serverConfig
        val blocked = buildSet {
            addAll(excludedClientKeys)
            failedClientsByVideo[videoId]?.let(::addAll)
        }
        val diagnostics = mutableListOf<String>()

        // Strategy 0: Optional Piped Relay / Custom Server when explicitly selected by user
        if (cfg.mode != YtMusicServerMode.DIRECT_ADFREE && cfg.activeRelayUrl.startsWith("http")) {
            val relayStream = runCatching { fetchPipedAudioStream(cfg.activeRelayUrl, videoId) }.getOrNull()
            if (relayStream != null && probeStreamUrl(relayStream)) {
                streamCache[videoId] = relayStream
                return@withContext Result.success(relayStream)
            }
            diagnostics += "PIPED_RELAY:no_stream"
        }

        val visitorData = ensureVisitorData()
        val hasLogin = cfg.hasYtMusicAccount

        // Build ordered Metrolist client ladder:
        // VISIONOS fast-path -> lastSuccessful -> rest of ladder (login-capable prioritized if signed in)
        val orderedClients = buildList {
            add(YtPlaybackClient.VISIONOS)
            lastSuccessfulClientKey?.let { key ->
                YtPlaybackClient.PLAYBACK_LADDER.firstOrNull { it.key == key }?.let(::add)
            }
            if (hasLogin) {
                addAll(YtPlaybackClient.PLAYBACK_LADDER.filter { it.loginSupported })
                addAll(YtPlaybackClient.PLAYBACK_LADDER.filterNot { it.loginSupported })
            } else {
                addAll(YtPlaybackClient.PLAYBACK_LADDER)
            }
        }.distinctBy { it.key }.filterNot { it.key in blocked }

        var lazySigTimestamp: Int? = null
        var fetchedSigTimestamp = false

        for (client in orderedClients) {
            if (client.loginRequired && !hasLogin) continue
            val sigTs = if (client.useSignatureTimestamp) {
                if (!fetchedSigTimestamp) {
                    fetchedSigTimestamp = true
                    lazySigTimestamp = fetchSignatureTimestampOrNull(videoId)
                }
                lazySigTimestamp
            } else {
                null
            }

            val candidate = runCatching {
                fetchPlayerStreamWithClient(
                    videoId = videoId,
                    client = client,
                    visitorData = visitorData,
                    signatureTimestamp = sigTs,
                    useLogin = hasLogin && client.loginSupported,
                )
            }.onFailure { err ->
                diagnostics += "${client.key}:${err.message?.take(40) ?: err.javaClass.simpleName}"
            }.getOrNull()

            if (candidate != null) {
                val ok = probeStreamUrl(candidate)
                if (ok) {
                    lastSuccessfulClientKey = client.key
                    streamCache[videoId] = candidate
                    return@withContext Result.success(candidate)
                } else {
                    diagnostics += "${client.key}:probe_403"
                }
            }
        }

        // Strategy Fallback A: NewPipeExtractor full page stream extraction (independent extractor)
        val newPipeStream = runCatching {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val extractor = ServiceList.YouTube.getStreamExtractor(url)
            extractor.fetchPage()
            val candidates = extractor.audioStreams
                .filter { !it.content.isNullOrBlank() && (!cfg.strictAdBlockEnabled || !AdBlockEngine.isAdTrackingStreamUrl(it.content)) }
                .sortedByDescending { it.averageBitrate }
            candidates.firstNotNullOfOrNull { s ->
                val headers = YtPlaybackClient.resolveHeadersForStreamUrl(s.content)
                val resolved = ResolvedStream(
                    url = s.content,
                    mimeType = s.format?.mimeType ?: "audio/mp4",
                    bitrate = s.averageBitrate,
                    clientKey = "NEWPIPE",
                    userAgent = headers.userAgent,
                    origin = headers.origin,
                    referer = headers.referer,
                )
                if (probeStreamUrl(resolved)) resolved else null
            }
        }.onFailure { err ->
            diagnostics += "NEWPIPE:${err.message?.take(40) ?: err.javaClass.simpleName}"
        }.getOrNull()

        if (newPipeStream != null) {
            streamCache[videoId] = newPipeStream
            return@withContext Result.success(newPipeStream)
        }

        // Strategy Fallback B: Public Piped relay nodes as last resort
        for (node in ServerPresets.PipedNodes.take(2)) {
            val relay = runCatching { fetchPipedAudioStream(node.apiBaseUrl, videoId) }.getOrNull()
            if (relay != null && probeStreamUrl(relay)) {
                streamCache[videoId] = relay
                return@withContext Result.success(relay)
            }
        }

        Result.failure(IllegalStateException("No playable stream for $videoId (${diagnostics.take(4).joinToString(", ")})"))
    }

    private suspend fun fetchPipedAudioStream(baseUrl: String, videoId: String): ResolvedStream? {
        val respText = http.get("$baseUrl/streams/$videoId") {
            header("User-Agent", "CroixPlayerPrime/2.6 (Android 12+)")
            header("Accept", "application/json")
        }.bodyAsText()
        val root = json.parseToJsonElement(respText).jsonObject
        val audioStreams = root["audioStreams"]?.jsonArray ?: return null
        val best = audioStreams
            .mapNotNull { it as? JsonObject }
            .filter { obj ->
                val u = obj["url"]?.jsonPrimitive?.contentOrNull.orEmpty()
                u.startsWith("http") && (!serverConfig.strictAdBlockEnabled || !AdBlockEngine.isAdTrackingStreamUrl(u))
            }
            .maxByOrNull { it["bitrate"]?.jsonPrimitive?.intOrNull ?: 0 }
            ?: return null
        val streamUrl = best["url"]!!.jsonPrimitive.content
        val hdrs = YtPlaybackClient.resolveHeadersForStreamUrl(streamUrl)
        return ResolvedStream(
            url = streamUrl,
            mimeType = best["mimeType"]?.jsonPrimitive?.contentOrNull ?: "audio/mp4",
            bitrate = best["bitrate"]?.jsonPrimitive?.intOrNull ?: 128_000,
            clientKey = "PIPED_RELAY",
            userAgent = hdrs.userAgent,
            origin = hdrs.origin,
            referer = hdrs.referer,
        )
    }

    private suspend fun fetchPlayerStreamWithClient(
        videoId: String,
        client: YtPlaybackClient,
        visitorData: String?,
        signatureTimestamp: Int?,
        useLogin: Boolean,
    ): ResolvedStream? {
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", client.clientName)
                    put("clientVersion", client.clientVersion)
                    put("hl", localeHl)
                    put("gl", localeGl)
                    client.osName?.let { put("osName", it) }
                    client.osVersion?.let { put("osVersion", it) }
                    client.deviceMake?.let { put("deviceMake", it) }
                    client.deviceModel?.let { put("deviceModel", it) }
                    client.androidSdkVersion?.let { put("androidSdkVersion", it) }
                    visitorData?.takeIf { it.isNotBlank() }?.let { put("visitorData", it) }
                }
                if (client.isEmbedded) {
                    putJsonObject("thirdParty") {
                        put("embedUrl", "https://www.youtube.com/watch?v=$videoId")
                    }
                }
            }
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
            if (signatureTimestamp != null && client.useSignatureTimestamp) {
                putJsonObject("playbackContext") {
                    putJsonObject("contentPlaybackContext") {
                        put("signatureTimestamp", signatureTimestamp)
                    }
                }
            }
        }

        val cfg = serverConfig
        val authHeader = if (useLogin && cfg.hasYtMusicAccount) {
            SapisidAuthSigner.buildAuthHeader(cfg.ytMusicCookie, origin = client.origin)
        } else {
            null
        }

        val responseText = http.post(client.playerEndpointUrl()) {
            contentType(ContentType.Application.Json)
            header("X-Goog-Api-Format-Version", "1")
            header("X-YouTube-Client-Name", client.clientId)
            header("X-YouTube-Client-Version", client.clientVersion)
            header("X-Origin", client.origin)
            header("Origin", client.origin)
            header("Referer", client.referer)
            header("User-Agent", client.userAgent)
            visitorData?.takeIf { it.isNotBlank() }?.let { header("X-Goog-Visitor-Id", it) }
            if (authHeader != null) {
                header("Authorization", authHeader)
                header("Cookie", cfg.ytMusicCookie)
            }
            setBody(payload.toString())
        }.bodyAsText()

        val root = json.parseToJsonElement(responseText).jsonObject
        if (cfg.strictAdBlockEnabled) {
            AdBlockEngine.countAndStripPlayerAds(root)
        }

        // Capture visitorData from responseContext if we didn't have one yet
        if (cachedVisitorData.isNullOrBlank()) {
            root["responseContext"]?.jsonObject?.get("visitorData")?.jsonPrimitive?.contentOrNull?.let {
                if (it.isNotBlank()) cachedVisitorData = it
            }
        }

        val playabilityStatus = root["playabilityStatus"]?.jsonObject
            ?.get("status")?.jsonPrimitive?.contentOrNull
        if (playabilityStatus != "OK") return null

        val streamingData = root["streamingData"]?.jsonObject ?: return null
        val expiresSec = streamingData["expiresInSeconds"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 14_400L
        val adaptive = streamingData["adaptiveFormats"]?.jsonArray.orEmpty()
        val formats = streamingData["formats"]?.jsonArray.orEmpty()

        // Prefer adaptive audio-only formats (highest bitrate audio/mp4 or audio/webm),
        // fall back to muxed progressive formats if adaptive audio is unavailable.
        val audioCandidates = adaptive
            .mapNotNull { it as? JsonObject }
            .filter { fmt ->
                val mime = fmt["mimeType"]?.jsonPrimitive?.contentOrNull.orEmpty()
                mime.startsWith("audio/")
            }
            .sortedWith(
                compareByDescending<JsonObject> { fmt ->
                    // Prefer standard mp4a/opus non-DRC streams
                    val mime = fmt["mimeType"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    if ("mp4a" in mime || "opus" in mime) 1 else 0
                }.thenByDescending { it["bitrate"]?.jsonPrimitive?.intOrNull ?: 0 },
            ) + formats.mapNotNull { it as? JsonObject }.sortedByDescending { it["bitrate"]?.jsonPrimitive?.intOrNull ?: 0 }

        for (fmt in audioCandidates.take(6)) {
            val rawUrl = resolveFormatUrl(fmt, videoId, client) ?: continue
            if (cfg.strictAdBlockEnabled && AdBlockEngine.isAdTrackingStreamUrl(rawUrl)) continue
            val patchedUrl = YtPlaybackClient.patchClientVersion(rawUrl, client.clientVersion)
            val mime = fmt["mimeType"]?.jsonPrimitive?.contentOrNull ?: "audio/mp4"
            val bitrate = fmt["bitrate"]?.jsonPrimitive?.intOrNull ?: 128_000
            return ResolvedStream(
                url = patchedUrl,
                mimeType = mime,
                bitrate = bitrate,
                clientKey = client.key,
                userAgent = client.userAgent,
                origin = if (client.isWebLike) client.origin else null,
                referer = if (client.isWebLike) client.referer else null,
                expiresAtEpochMs = System.currentTimeMillis() + (expiresSec * 1000L).coerceAtLeast(600_000L),
            )
        }
        return null
    }

    private fun resolveFormatUrl(fmt: JsonObject, videoId: String, client: YtPlaybackClient): String? {
        val directUrl = fmt["url"]?.jsonPrimitive?.contentOrNull
        if (!directUrl.isNullOrBlank()) {
            return deobfuscateThrottlingParam(videoId, directUrl)
        }
        val cipher = fmt["signatureCipher"]?.jsonPrimitive?.contentOrNull
            ?: fmt["cipher"]?.jsonPrimitive?.contentOrNull
            ?: return null
        val params = cipher.split("&").mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq <= 0) null
            else URLDecoder.decode(part.substring(0, eq), "UTF-8") to URLDecoder.decode(part.substring(eq + 1), "UTF-8")
        }.toMap()
        val baseUrl = params["url"] ?: return null
        val obfuscatedSig = params["s"] ?: return null
        val sp = params["sp"]?.takeIf { it.isNotBlank() } ?: "signature"

        val decipheredSig = runCatching {
            YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, obfuscatedSig)
        }.recoverCatching {
            YoutubeJavaScriptPlayerManager.clearAllCaches()
            YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, obfuscatedSig)
        }.getOrNull() ?: return null

        val sep = if (baseUrl.contains("?")) "&" else "?"
        val signedUrl = "$baseUrl$sep$sp=${URLEncoder.encode(decipheredSig, "UTF-8")}"
        return deobfuscateThrottlingParam(videoId, signedUrl)
    }

    private fun deobfuscateThrottlingParam(videoId: String, url: String): String {
        if (!url.contains("n=")) return url
        return runCatching {
            YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
        }.recoverCatching {
            YoutubeJavaScriptPlayerManager.clearAllCaches()
            YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
        }.getOrElse { url }
    }

    private fun fetchSignatureTimestampOrNull(videoId: String): Int? =
        runCatching {
            YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
        }.recoverCatching {
            YoutubeJavaScriptPlayerManager.clearAllCaches()
            YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
        }.getOrNull()

    /**
     * Probing stream URLs with `Range: bytes=0-1` + exact client headers (Metrolist / ArchiveTune pattern)
     * catches HTTP 403 / HTML error pages immediately so we cleanly fall back to the next working client.
     */
    private fun probeStreamUrl(stream: ResolvedStream): Boolean = runCatching {
        val reqBuilder = Request.Builder()
            .url(stream.url)
            .get()
            .header("Range", "bytes=0-1")
            .header("User-Agent", stream.userAgent)
        stream.origin?.let { reqBuilder.header("Origin", it) }
        stream.referer?.let { reqBuilder.header("Referer", it) }

        val probeClient = okHttp.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(7, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        probeClient.newCall(reqBuilder.build()).execute().use { resp ->
            val code = resp.code
            if (code == 403 || code !in 200..399) return@use false
            val ct = resp.header("Content-Type").orEmpty().lowercase(Locale.US)
            if (ct.startsWith("text/html") || ct.startsWith("text/plain") || ct.startsWith("application/json")) {
                return@use false
            }
            resp.body?.source()?.request(1) == true
        }
    }.getOrDefault(false)

    private suspend fun postInnerTube(endpoint: String, body: JsonObject): String {
        val cfg = serverConfig
        val authHeader = if (cfg.hasYtMusicAccount) {
            SapisidAuthSigner.buildAuthHeader(cfg.ytMusicCookie)
        } else {
            null
        }
        return http.post("https://music.youtube.com/youtubei/v1/$endpoint?prettyPrint=false") {
            contentType(ContentType.Application.Json)
            header("Origin", "https://music.youtube.com")
            header("Referer", "https://music.youtube.com/")
            header("X-Origin", "https://music.youtube.com")
            if (authHeader != null) {
                header("Authorization", authHeader)
                header("Cookie", cfg.ytMusicCookie)
            }
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
        val blockAds = serverConfig.strictAdBlockEnabled
        fun walk(el: JsonElement) {
            when (el) {
                is JsonObject -> {
                    if (blockAds && AdBlockEngine.isAdOrPromotedNode(el)) return
                    val shelf = el["musicCarouselShelfRenderer"]?.jsonObject
                    if (shelf != null) {
                        val title = shelf["header"]?.jsonObject
                            ?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObject
                            ?.get("title")?.jsonObject
                            ?.get("runs")?.jsonArray
                            ?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.contentOrNull
                            ?: "For You"
                        if (!blockAds || !AdBlockEngine.isSponsoredShelfTitle(title)) {
                            val tracks = extractTracksFromTree(shelf).distinctBy { it.id }
                            if (tracks.isNotEmpty()) {
                                out += HomeSection(title = title, tracks = tracks)
                            }
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
        val blockAds = serverConfig.strictAdBlockEnabled
        val result = mutableListOf<Track>()
        fun walk(el: JsonElement) {
            when (el) {
                is JsonObject -> {
                    if (blockAds && AdBlockEngine.isAdOrPromotedNode(el)) return
                    el["musicResponsiveListItemRenderer"]?.jsonObject?.let { item ->
                        if (!blockAds || !AdBlockEngine.isAdOrPromotedNode(item)) {
                            parseListItem(item)?.let(result::add)
                        }
                    }
                    el["musicTwoRowItemRenderer"]?.jsonObject?.let { item ->
                        if (!blockAds || !AdBlockEngine.isAdOrPromotedNode(item)) {
                            parseTwoRowItem(item)?.let(result::add)
                        }
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
