/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import java.net.URLEncoder
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor

@Serializable
data class SpotifySeedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val thumbnailUrl: String = "",
)

@Serializable
data class SpotifySeedArtist(
    val id: String,
    val name: String,
    val genres: List<String> = emptyList(),
    val thumbnailUrl: String = "",
)

@Serializable
data class SpotifyPlaylistSummary(
    val id: String,
    val name: String,
    val trackCount: Int = 0,
    val thumbnailUrl: String = "",
)

@Serializable
data class SpotifyHomeMixSummary(
    val id: String,
    val name: String,
    val description: String = "",
    val thumbnailUrl: String = "",
    val sectionTitle: String = "",
    val accentHex: Long = 0xFF1DB954L,
)

@Serializable
data class SpotifyTasteSnapshot(
    val connected: Boolean = false,
    val accountName: String = "",
    val avatarUrl: String = "",
    val topArtists: List<SpotifySeedArtist> = emptyList(),
    val shortTermArtists: List<SpotifySeedArtist> = emptyList(),
    val relatedArtists: List<SpotifySeedArtist> = emptyList(),
    val topTracks: List<SpotifySeedTrack> = emptyList(),
    val shortTermTracks: List<SpotifySeedTrack> = emptyList(),
    val recentTracks: List<SpotifySeedTrack> = emptyList(),
    val likedTracks: List<SpotifySeedTrack> = emptyList(),
    val recommendedTracks: List<SpotifySeedTrack> = emptyList(),
    val playlists: List<SpotifyPlaylistSummary> = emptyList(),
    val homeMixes: List<SpotifyHomeMixSummary> = emptyList(),
    val lastSyncedEpochMs: Long = 0L,
) {
    val allSeedTracks: List<SpotifySeedTrack>
        get() = (shortTermTracks + recentTracks + topTracks + likedTracks + recommendedTracks)
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
            .distinctBy { "${it.title.lowercase()}|${it.artist.lowercase()}" }

    val allSeedArtistNames: List<String>
        get() = (shortTermArtists.map { it.name } + topArtists.map { it.name } + relatedArtists.map { it.name } + allSeedTracks.map { it.artist })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
}

/**
 * Full-power Spotify Pathfinder GraphQL + Web API client:
 * - TOTP + server-time authenticated Web Player tokens (plus direct HTML session fallback)
 * - Automatic JS bundle persisted-query hash self-healing
 * - Syncs Short-Term & Medium-Term Top Artists/Tracks, Recently Played, Paginated Liked Songs (100+),
 *   Related Artists Graph (`queryArtistOverview`), Personalized Spotify Home Mixes (`home`: Daily Mix,
 *   Discover Weekly, Release Radar, Daylist), and live Artist/Track Recommendations!
 */
class SpotifyTasteClient {
    companion object {
        const val LOGIN_URL =
            "https://accounts.spotify.com/login?continue=https%3A%2F%2Fopen.spotify.com%2F"

        private const val TOKEN_URL = "https://open.spotify.com/api/token"
        private const val SERVER_TIME_URL = "https://open.spotify.com/api/server-time"
        private const val NUANCE_URL =
            "https://gist.githubusercontent.com/sonic-liberation/22ed9c6ba463899e933427f7de1f0eef/raw/nuances.json"
        private const val GQL_URL = "https://api-partner.spotify.com/pathfinder/v2/query"
        private const val REST_BASE = "https://api.spotify.com/v1"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"

        private val HASHES = ConcurrentHashMap(
            mapOf(
                "profileAttributes" to "53bcb064f6cd18c23f752bc324a791194d20df612d8e1239c735144ab0399ced",
                "libraryV3" to "973e511ca44261fda7eebac8b653155e7caee3675abb4fb110cc1b8c78b091c3",
                "fetchPlaylist" to "346811f856fb0b7e4f6c59f8ebea78dd081c6e2fb01b77c954b26259d5fc6763",
                "fetchLibraryTracks" to "087278b20b743578a6262c2b0b4bcd20d879c503cc359a2285baf083ef944240",
                "home" to "23e37f2e58d82d567f27080101d36609009d8c3676457b1086cb0acc55b72a5d",
                "searchDesktop" to "4801118d4a100f756e833d33984436a3899cff359c532f8fd3aaf174b60b3b49",
                "queryArtistOverview" to "5b9e64f43843fa3a9b6a98543600299b0a2cbbbccfdcdcef2402eb9c1017ca4c",
                "queryWhatsNewFeed" to "3b53dede3c6054e8b7c962dd280eb6761c5d1c82b06b039f4110d76a62b4966b",
            ),
        )
        private val BUNDLE_URL_REGEX = Regex("""https://open\.spotifycdn\.com/cdn/build/web-player/[^"']+?\.js""")
        private val OPERATION_HASH_REGEX = Regex("""new [\w$]+(?:\.[\w$]+)?\("(\w+)","(?:query|mutation|subscription)","([0-9a-f]{64})"""")
    }

    @Serializable
    private data class Nuance(val s: String, val v: Int)

    @Serializable
    private data class ServerTimeResponse(val serverTime: Long)

    @Serializable
    data class TokenResult(
        val accessToken: String = "",
        val accessTokenExpirationTimestampMs: Long = 0L,
        val isAnonymous: Boolean = true,
    )

    private val defaultNuance = Nuance(
        s = "GM3TMMJTGYZTQNZVGM4DINJZHA4TGOBYGMZTCMRTGEYDSMJRHE4TEOBUG4YTCMRUGQ4DQOJUGQYTAMRRGA2TCMJSHE3TCMBY",
        v = 61,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val http = HttpClient(OkHttp) {
        engine { preconfigured = okHttp }
        expectSuccess = false
    }

    private val tokenMutex = Mutex()
    private val artistOverviewCache = ConcurrentHashMap<String, Pair<List<SpotifySeedArtist>, List<SpotifySeedTrack>>>()
    private var cachedNuance: Nuance = defaultNuance
    private var cachedToken: String? = null
    private var cachedTokenExpiryMs: Long = 0L

    suspend fun fetchAccessToken(
        spDc: String,
        spKey: String = "",
        forceRefresh: Boolean = false,
    ): Result<TokenResult> = withContext(Dispatchers.IO) {
        tokenMutex.withLock {
            if (!forceRefresh && !cachedToken.isNullOrBlank() && cachedTokenExpiryMs > System.currentTimeMillis() + 60_000L) {
                return@withLock Result.success(TokenResult(cachedToken!!, cachedTokenExpiryMs, isAnonymous = false))
            }
            val cookie = buildString {
                append("sp_dc=${spDc.trim()}")
                if (spKey.isNotBlank()) append("; sp_key=${spKey.trim()}")
            }
            // Primary path: TOTP + server-time internal web-player token
            val primary = runCatching {
                val nuance = fetchLatestNuance()
                val serverTime = fetchServerTime()
                val totp = generateTotp(nuance.s, serverTime)
                val url = "$TOKEN_URL?reason=transport&productType=web-player&totp=$totp&totpServer=$totp&totpVer=${nuance.v}"
                val resp = http.get(url) {
                    header("User-Agent", USER_AGENT)
                    header("Cookie", cookie)
                    header("Accept", "application/json")
                }
                val body = resp.bodyAsText()
                if (resp.status.value !in 200..299) error("Spotify token HTTP ${resp.status.value}")
                val parsed = json.decodeFromString<TokenResult>(body)
                if (parsed.isAnonymous || parsed.accessToken.isBlank()) {
                    error("Sesión de Spotify expirada o cookie sp_dc inválida")
                }
                parsed
            }
            if (primary.isSuccess) {
                val parsed = primary.getOrThrow()
                cachedToken = parsed.accessToken
                cachedTokenExpiryMs = parsed.accessTokenExpirationTimestampMs
                return@withLock primary
            }
            // Fallback path: direct open.spotify.com HTML embedded session token (in case TOTP gist is blocked/down)
            val fallback = runCatching {
                val html = http.get("https://open.spotify.com/") {
                    header("User-Agent", USER_AGENT)
                    header("Cookie", cookie)
                }.bodyAsText()
                val match = Regex("""<script id="session" data-testid="session" type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
                    .find(html)?.groupValues?.get(1)
                    ?: error(primary.exceptionOrNull()?.message ?: "No se pudo obtener token de Spotify")
                val parsed = json.decodeFromString<TokenResult>(match)
                if (parsed.isAnonymous || parsed.accessToken.isBlank()) {
                    error("Sesión de Spotify expirada (inicia sesión de nuevo)")
                }
                parsed
            }
            fallback.onSuccess { parsed ->
                cachedToken = parsed.accessToken
                cachedTokenExpiryMs = parsed.accessTokenExpirationTimestampMs
            }
            fallback
        }
    }

    fun clearCachedToken() {
        cachedToken = null
        cachedTokenExpiryMs = 0L
        artistOverviewCache.clear()
    }

    /**
     * Deep Yuma/ArchiveTune-grade Spotify Taste & Library Sync:
     * Pulls short_term + medium_term Top Artists & Tracks, Recently Played, up to 100 Liked Songs,
     * Related Artists via Pathfinder GQL `queryArtistOverview`, Personalized Spotify Home Mixes
     * (Daily Mix, Discover Weekly, Release Radar, Daylist), and up to 50 Playlists!
     */
    suspend fun syncTasteProfile(spDc: String, spKey: String = ""): Result<SpotifyTasteSnapshot> =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = fetchAccessToken(spDc, spKey, forceRefresh = false).getOrThrow().accessToken

                coroutineScope {
                    val profileDef = async { fetchProfile(token) }
                    val shortArtistsDef = async { fetchTopArtistsRest(token, "short_term", 20) }
                    val medArtistsDef = async { fetchTopArtistsRest(token, "medium_term", 25) }
                    val shortTracksDef = async { fetchTopTracksRest(token, "short_term", 25) }
                    val medTracksDef = async { fetchTopTracksRest(token, "medium_term", 30) }
                    val recentDef = async { fetchRecentlyPlayedRest(token, 40) }
                    val likedDef = async { fetchLikedTracksPaginatedGql(token, maxTracks = 100) }
                    val playlistsDef = async { fetchMyPlaylistsGql(token, limit = 50) }
                    val followedArtistsDef = async { fetchFollowedArtistsGql(token, limit = 30) }
                    val homeMixesDef = async { fetchSpotifyHomeMixesGql(token) }

                    val (name, avatar) = profileDef.await()
                    val shortArtists = shortArtistsDef.await()
                    val topArtists = (shortArtists + medArtistsDef.await() + followedArtistsDef.await())
                        .distinctBy { it.name.lowercase() }
                        .take(35)
                    val shortTracks = shortTracksDef.await()
                    val topTracks = (shortTracks + medTracksDef.await())
                        .distinctBy { "${it.title.lowercase()}|${it.artist.lowercase()}" }
                        .take(45)
                    val recentTracks = recentDef.await()
                    val likedTracks = likedDef.await()
                    val playlists = playlistsDef.await()
                    val homeMixes = homeMixesDef.await()

                    // Expand Related Artists + Popular Tracks for user's top 6 artists via GQL (zero rate-limits!)
                    val seedArtistIds = topArtists.mapNotNull { it.id.takeIf(String::isNotBlank) }.distinct().take(6)
                    val overviews = seedArtistIds.map { aid ->
                        async { fetchArtistOverviewByIdGql(token, aid) }
                    }.awaitAll()

                    val relatedArtists = overviews.flatMap { it.first }
                        .distinctBy { it.name.lowercase() }
                        .filter { rel -> topArtists.none { it.name.equals(rel.name, ignoreCase = true) } }
                        .take(30)

                    val restRecs = runCatching {
                        fetchRecommendationsRest(
                            token = token,
                            seedArtistIds = seedArtistIds.take(3),
                            seedTrackIds = (shortTracks + recentTracks).mapNotNull { it.id.takeIf(String::isNotBlank) }.distinct().take(2),
                            limit = 35,
                        )
                    }.getOrElse { emptyList() }

                    val recommendedTracks = (restRecs + overviews.flatMap { it.second })
                        .distinctBy { "${it.title.lowercase()}|${it.artist.lowercase()}" }
                        .take(50)

                    SpotifyTasteSnapshot(
                        connected = true,
                        accountName = name.ifBlank { "Usuario Spotify" },
                        avatarUrl = avatar,
                        topArtists = topArtists,
                        shortTermArtists = shortArtists,
                        relatedArtists = relatedArtists,
                        topTracks = topTracks,
                        shortTermTracks = shortTracks,
                        recentTracks = recentTracks,
                        likedTracks = likedTracks,
                        recommendedTracks = recommendedTracks,
                        playlists = playlists,
                        homeMixes = homeMixes,
                        lastSyncedEpochMs = System.currentTimeMillis(),
                    )
                }
            }
        }

    /**
     * Live Spotify Artist Graph Lookup for ANY currently playing artist:
     * Uses Pathfinder GQL `searchDesktop` -> `queryArtistOverview` to return real Spotify
     * Related Artists + their Top Tracks for razor-sharp Spotify-style queues!
     */
    suspend fun fetchRelatedAndTopForArtist(
        spDc: String,
        spKey: String,
        artistName: String,
    ): Pair<List<SpotifySeedArtist>, List<SpotifySeedTrack>> = withContext(Dispatchers.IO) {
        val clean = artistName.split(",", "&", "feat.", "ft.", ignoreCase = true).firstOrNull()?.trim().orEmpty()
        if (spDc.isBlank() || clean.isBlank()) return@withContext emptyList<SpotifySeedArtist>() to emptyList()
        artistOverviewCache[clean.lowercase()]?.let { return@withContext it }

        runCatching {
            val token = fetchAccessToken(spDc, spKey).getOrThrow().accessToken
            val artistId = searchArtistIdGql(token, clean) ?: return@runCatching emptyList<SpotifySeedArtist>() to emptyList()
            val (related, topOfMain) = fetchArtistOverviewByIdGql(token, artistId)
            // Pull top tracks of the top 3 related artists in parallel for instant coherent pool!
            val relTopTracks = coroutineScope {
                related.take(3).mapNotNull { it.id.takeIf(String::isNotBlank) }.map { rid ->
                    async { fetchArtistOverviewByIdGql(token, rid).second }
                }.awaitAll().flatten()
            }
            val result = related to (topOfMain + relTopTracks).distinctBy { "${it.title.lowercase()}|${it.artist.lowercase()}" }
            artistOverviewCache[clean.lowercase()] = result
            result
        }.getOrElse { emptyList<SpotifySeedArtist>() to emptyList() }
    }

    suspend fun fetchPlaylistTracks(
        spDc: String,
        spKey: String,
        playlistId: String,
        limit: Int = 120,
    ): Result<List<SpotifySeedTrack>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = fetchAccessToken(spDc, spKey).getOrThrow().accessToken
            val collected = mutableListOf<SpotifySeedTrack>()
            var offset = 0
            while (collected.size < limit) {
                val pageSize = (limit - collected.size).coerceAtMost(100)
                val vars = buildJsonObject {
                    put("uri", "spotify:playlist:$playlistId")
                    put("offset", offset)
                    put("limit", pageSize)
                    put("enableWatchFeedEntrypoint", false)
                }
                val root = graphqlPost(token, "fetchPlaylist", vars)
                val content = root.obj("data")?.obj("playlistV2")?.obj("content")
                val items = content?.arr("items").orEmpty()
                if (items.isEmpty()) break
                items.forEach { elem ->
                    val wrapper = elem.jsonObject.obj("itemV2") ?: return@forEach
                    val data = wrapper.obj("data") ?: return@forEach
                    parseGqlTrack(data)?.let(collected::add)
                }
                val total = content?.int("totalCount") ?: collected.size
                offset += items.size
                if (offset >= total) break
            }
            collected.distinctBy { "${it.title.lowercase()}|${it.artist.lowercase()}" }.take(limit)
        }
    }

    suspend fun fetchPublicSpotifyPlaylist(rawUrl: String): Result<Pair<String, List<SpotifySeedTrack>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val id = extractSpotifyPlaylistId(rawUrl) ?: error("URL de playlist de Spotify inválida")
                val embedResp = http.get("https://open.spotify.com/embed/playlist/$id") {
                    header("User-Agent", USER_AGENT)
                }.bodyAsText()
                val nextData = Regex("""<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
                    .find(embedResp)?.groupValues?.get(1)
                    ?: error("No se pudieron leer los metadatos públicos de Spotify")
                val root = json.parseToJsonElement(nextData).jsonObject
                val entity = root.obj("props")?.obj("pageProps")?.obj("state")?.obj("data")?.obj("entity")
                    ?: error("Playlist de Spotify no disponible")
                val title = entity.str("name").orEmpty().ifBlank { "Playlist de Spotify" }
                val tracks = entity.arr("trackList")?.mapNotNull { t ->
                    val o = t.jsonObject
                    val tTitle = o.str("title").orEmpty()
                    val tArtist = o.str("subtitle").orEmpty()
                    val tUri = o.str("uri").orEmpty()
                    if (tTitle.isBlank()) null
                    else SpotifySeedTrack(
                        id = tUri.substringAfterLast(":"),
                        title = tTitle,
                        artist = tArtist,
                    )
                }.orEmpty()
                if (tracks.isEmpty()) error("La playlist de Spotify no tiene pistas legibles")
                title to tracks
            }
        }

    fun extractSpotifyPlaylistId(rawUrl: String): String? =
        Regex("""open\.spotify\.com/(?:intl-[a-z]+/)?playlist/([A-Za-z0-9]+)""")
            .find(rawUrl.trim())
            ?.groupValues
            ?.get(1)

    suspend fun fetchColorLyrics(
        spDc: String,
        spKey: String,
        title: String,
        artist: String,
    ): LyricsResult? = withContext(Dispatchers.IO) {
        if (spDc.isBlank() || title.isBlank()) return@withContext null
        runCatching {
            val token = fetchAccessToken(spDc, spKey).getOrThrow().accessToken
            val cleanArtist = artist.split(",", "&", "feat.", "ft.", ignoreCase = true).firstOrNull()?.trim().orEmpty()
            val q = URLEncoder.encode("$title $cleanArtist".trim(), "UTF-8")
            val searchRoot = restGet(token, "search?q=$q&type=track&limit=1")
            val trackId = searchRoot.obj("tracks")?.arr("items")?.firstOrNull()?.jsonObject?.str("id")
                ?: return@runCatching null

            val resp = http.get("https://spclient.wg.spotify.com/color-lyrics/v2/track/$trackId?format=json&vocalRemoval=false&market=from_token") {
                header("Authorization", "Bearer $token")
                header("app-platform", "WebPlayer")
                header("User-Agent", USER_AGENT)
                header("Accept", "application/json")
            }
            if (resp.status.value !in 200..299) return@runCatching null
            val root = json.parseToJsonElement(resp.bodyAsText()).jsonObject
            val lyricsObj = root.obj("lyrics") ?: return@runCatching null
            val syncType = lyricsObj.str("syncType").orEmpty()
            val linesArr = lyricsObj.arr("lines").orEmpty()
            if (linesArr.isEmpty()) return@runCatching null

            val synced = mutableListOf<SyncedLyricLine>()
            val plainBuilder = StringBuilder()
            linesArr.forEach { elem ->
                val o = elem.jsonObject
                val words = o.str("words").orEmpty().trim()
                if (words.isNotBlank() && words != "♪") {
                    val startMs = o.str("startTimeMs")?.toLongOrNull() ?: 0L
                    if (syncType == "LINE_SYNCED" && startMs >= 0L) {
                        synced += SyncedLyricLine(timeMs = startMs, text = words)
                    }
                    if (plainBuilder.isNotEmpty()) plainBuilder.append('\n')
                    plainBuilder.append(words)
                }
            }
            if (synced.isEmpty() && plainBuilder.isEmpty()) null
            else LyricsResult(synced = synced, plain = plainBuilder.toString(), source = "Spotify Lyrics")
        }.getOrNull()
    }

    private suspend fun fetchProfile(token: String): Pair<String, String> {
        runCatching {
            val root = graphqlPost(token, "profileAttributes")
            val profile = root.obj("data")?.obj("me")?.obj("profile")
            val name = profile?.str("name").orEmpty()
            val avatar = profile?.obj("avatar")?.arr("sources")?.firstOrNull()?.jsonObject?.str("url").orEmpty()
            if (name.isNotBlank()) return name to avatar
        }
        return runCatching {
            val root = restGet(token, "me")
            val name = root.str("display_name").orEmpty()
            val avatar = root.arr("images")?.firstOrNull()?.jsonObject?.str("url").orEmpty()
            name to avatar
        }.getOrElse { "Usuario Spotify" to "" }
    }

    /**
     * Fetches the user's REAL Spotify personalized Home Mixes (Daily Mixes, Discover Weekly,
     * Release Radar, Daylist, Made For You, Radio Stations) via Pathfinder GQL `home`!
     */
    private suspend fun fetchSpotifyHomeMixesGql(token: String): List<SpotifyHomeMixSummary> = runCatching {
        val vars = buildJsonObject {
            put("homeEndUserIntegration", "INTEGRATION_WEB_PLAYER")
            put("timeZone", TimeZone.getDefault().id)
            put("sp_t", "")
            put("facet", "")
            put("sectionItemsLimit", 10)
            put("includeEpisodeContentRatingsV2", false)
        }
        val root = graphqlPost(token, "home", vars)
        val sections = root.obj("data")?.obj("home")?.obj("sectionContainer")?.obj("sections")?.arr("items").orEmpty()
        val mixes = mutableListOf<SpotifyHomeMixSummary>()
        sections.forEach { secElem ->
            val secObj = secElem.jsonObject
            val titleObj = secObj.obj("data")?.obj("title")
            val secTitle = titleObj?.str("transformedLabel")
                ?: titleObj?.str("translatedBaseText")
                ?: titleObj?.str("text").orEmpty()
            val items = secObj.obj("sectionItems")?.arr("items").orEmpty()
            items.forEach { itemElem ->
                val content = itemElem.jsonObject.obj("content") ?: return@forEach
                if (content.str("__typename") != "PlaylistResponseWrapper") return@forEach
                val data = content.obj("data") ?: return@forEach
                val uri = data.str("uri") ?: return@forEach
                val id = uri.substringAfterLast(":")
                val name = data.str("name").orEmpty().trim()
                if (id.isBlank() || name.isBlank()) return@forEach
                val desc = data.str("description").orEmpty().replace(Regex("<[^>]+>"), "").trim()
                val imgItem = data.obj("images")?.arr("items")?.firstOrNull()?.jsonObject
                val imgUrl = imgItem?.arr("sources")?.firstOrNull()?.jsonObject?.str("url").orEmpty()
                val hexStr = imgItem?.obj("extractedColors")?.obj("colorDark")?.str("hex").orEmpty()
                val accent = parseHexColor(hexStr) ?: 0xFF1DB954L
                mixes += SpotifyHomeMixSummary(
                    id = id,
                    name = name,
                    description = desc,
                    thumbnailUrl = imgUrl,
                    sectionTitle = secTitle,
                    accentHex = accent,
                )
            }
        }
        // Prioritize personalized mixes (Daily Mix, Discover Weekly, Daylist, Mix de..., Radio...)
        val prioritized = mixes.distinctBy { it.id }.sortedByDescending { m ->
            val l = "${m.name} ${m.sectionTitle}".lowercase()
            when {
                "daily mix" in l || "mix diario" in l -> 10
                "discover" in l || "descubrimiento" in l || "daylist" in l -> 9
                "release radar" in l || "radar" in l || "para ti" in l || "made for" in l -> 8
                "mix" in l || "radio" in l -> 6
                else -> 3
            }
        }
        prioritized.take(14)
    }.getOrElse { emptyList() }

    private suspend fun searchArtistIdGql(token: String, artistName: String): String? = runCatching {
        val vars = buildJsonObject {
            put("searchTerm", artistName)
            put("offset", 0)
            put("limit", 4)
            put("numberOfTopResults", 3)
            put("includeAudiobooks", false)
            put("includeArtistHasConcertsField", false)
            put("includePreReleases", false)
            put("includeLocalConcertsField", false)
            put("includeAuthors", false)
        }
        val root = graphqlPost(token, "searchDesktop", vars)
        val items = root.obj("data")?.obj("searchV2")?.obj("artists")?.arr("items").orEmpty()
        items.firstNotNullOfOrNull { el ->
            val data = el.jsonObject.obj("data") ?: return@firstNotNullOfOrNull null
            val uri = data.str("uri").orEmpty()
            uri.substringAfterLast(":").takeIf(String::isNotBlank)
        }
    }.getOrNull()

    private suspend fun fetchArtistOverviewByIdGql(
        token: String,
        artistId: String,
    ): Pair<List<SpotifySeedArtist>, List<SpotifySeedTrack>> = runCatching {
        val vars = buildJsonObject {
            put("uri", "spotify:artist:$artistId")
            put("locale", "")
        }
        val root = graphqlPost(token, "queryArtistOverview", vars)
        val union = root.obj("data")?.obj("artistUnion") ?: return@runCatching emptyList<SpotifySeedArtist>() to emptyList()
        val related = union.obj("relatedContent")?.obj("relatedArtists")?.arr("items").orEmpty().mapNotNull { el ->
            val obj = el.jsonObject
            val uri = obj.str("uri") ?: return@mapNotNull null
            val name = obj.obj("profile")?.str("name") ?: return@mapNotNull null
            val img = obj.obj("visuals")?.obj("avatarImage")?.arr("sources")?.firstOrNull()?.jsonObject?.str("url").orEmpty()
            SpotifySeedArtist(id = uri.substringAfterLast(":"), name = name, thumbnailUrl = img)
        }
        val topTracks = union.obj("discography")?.obj("topTracks")?.arr("items").orEmpty().mapNotNull { el ->
            el.jsonObject.obj("track")?.let(::parseGqlTrack)
        }
        related to topTracks
    }.getOrElse { emptyList<SpotifySeedArtist>() to emptyList() }

    private suspend fun fetchRecommendationsRest(
        token: String,
        seedArtistIds: List<String>,
        seedTrackIds: List<String>,
        limit: Int,
    ): List<SpotifySeedTrack> {
        if (seedArtistIds.isEmpty() && seedTrackIds.isEmpty()) return emptyList()
        val params = buildList {
            if (seedArtistIds.isNotEmpty()) add("seed_artists=${seedArtistIds.take(3).joinToString(",")}")
            if (seedTrackIds.isNotEmpty()) add("seed_tracks=${seedTrackIds.take(2).joinToString(",")}")
            add("limit=$limit")
        }.joinToString("&")
        val root = restGet(token, "recommendations?$params")
        return root.arr("tracks").orEmpty().mapNotNull { parseRestTrack(it.jsonObject) }
    }

    private suspend fun fetchTopArtistsRest(token: String, timeRange: String, limit: Int): List<SpotifySeedArtist> = runCatching {
        val root = restGet(token, "me/top/artists?time_range=$timeRange&limit=$limit")
        root.arr("items").orEmpty().mapNotNull { el ->
            val o = el.jsonObject
            val id = o.str("id").orEmpty()
            val name = o.str("name").orEmpty()
            if (name.isBlank()) return@mapNotNull null
            val genres = o.arr("genres").orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }
            val thumb = o.arr("images")?.firstOrNull()?.jsonObject?.str("url").orEmpty()
            SpotifySeedArtist(id = id, name = name, genres = genres, thumbnailUrl = thumb)
        }
    }.getOrElse { emptyList() }

    private suspend fun fetchTopTracksRest(token: String, timeRange: String, limit: Int): List<SpotifySeedTrack> = runCatching {
        val root = restGet(token, "me/top/tracks?time_range=$timeRange&limit=$limit")
        root.arr("items").orEmpty().mapNotNull { parseRestTrack(it.jsonObject) }
    }.getOrElse { emptyList() }

    private suspend fun fetchRecentlyPlayedRest(token: String, limit: Int): List<SpotifySeedTrack> = runCatching {
        val root = restGet(token, "me/player/recently-played?limit=$limit")
        root.arr("items").orEmpty().mapNotNull { el ->
            el.jsonObject.obj("track")?.let(::parseRestTrack)
        }
    }.getOrElse { emptyList() }

    private suspend fun fetchLikedTracksPaginatedGql(token: String, maxTracks: Int): List<SpotifySeedTrack> = runCatching {
        val out = mutableListOf<SpotifySeedTrack>()
        var offset = 0
        while (out.size < maxTracks) {
            val vars = buildJsonObject {
                put("offset", offset)
                put("limit", 50)
            }
            val root = graphqlPost(token, "fetchLibraryTracks", vars)
            val items = root.obj("data")?.obj("me")?.obj("library")?.obj("tracks")?.arr("items").orEmpty()
            if (items.isEmpty()) break
            items.forEach { el ->
                val data = el.jsonObject.obj("track")?.obj("data") ?: return@forEach
                parseGqlTrack(data)?.let(out::add)
            }
            offset += items.size
            if (items.size < 50) break
        }
        out.distinctBy { "${it.title.lowercase()}|${it.artist.lowercase()}" }.take(maxTracks)
    }.getOrElse { emptyList() }

    private suspend fun fetchFollowedArtistsGql(token: String, limit: Int): List<SpotifySeedArtist> = runCatching {
        val vars = buildJsonObject {
            putJsonArray("filters") { add("Artists") }
            put("order", null as String?)
            put("textFilter", "")
            putJsonArray("features") { add("LIKED_SONGS") }
            put("limit", limit)
            put("offset", 0)
            put("flatten", false)
            putJsonArray("expandedFolders") {}
            put("folderUri", null as String?)
            put("includeFoldersWhenFlattening", true)
        }
        val root = graphqlPost(token, "libraryV3", vars)
        val items = root.obj("data")?.obj("me")?.obj("libraryV3")?.arr("items").orEmpty()
        items.mapNotNull { el ->
            val wrapper = el.jsonObject.obj("item") ?: return@mapNotNull null
            val data = wrapper.obj("data") ?: return@mapNotNull null
            val name = data.obj("profile")?.str("name") ?: data.str("name") ?: return@mapNotNull null
            val uri = wrapper.str("_uri") ?: data.str("uri").orEmpty()
            val thumb = data.obj("visuals")?.obj("avatarImage")?.arr("sources")?.firstOrNull()?.jsonObject?.str("url").orEmpty()
            SpotifySeedArtist(id = uri.substringAfterLast(":"), name = name, thumbnailUrl = thumb)
        }
    }.getOrElse { emptyList() }

    private suspend fun fetchMyPlaylistsGql(token: String, limit: Int): List<SpotifyPlaylistSummary> = runCatching {
        val vars = buildJsonObject {
            putJsonArray("filters") { add("Playlists") }
            put("order", null as String?)
            put("textFilter", "")
            putJsonArray("features") { add("LIKED_SONGS") }
            put("limit", limit)
            put("offset", 0)
            put("flatten", true)
            putJsonArray("expandedFolders") {}
            put("folderUri", null as String?)
            put("includeFoldersWhenFlattening", false)
        }
        val root = graphqlPost(token, "libraryV3", vars)
        val items = root.obj("data")?.obj("me")?.obj("libraryV3")?.arr("items").orEmpty()
        items.mapNotNull { el ->
            val wrapper = el.jsonObject.obj("item") ?: return@mapNotNull null
            if (wrapper.str("__typename") != "PlaylistResponseWrapper") return@mapNotNull null
            val data = wrapper.obj("data") ?: return@mapNotNull null
            val uri = wrapper.str("_uri") ?: return@mapNotNull null
            val name = data.str("name").orEmpty()
            if (name.isBlank()) return@mapNotNull null
            val count = data.obj("content")?.int("totalCount")
                ?: data.obj("tracks")?.int("totalCount")
                ?: data.int("totalCount")
                ?: 0
            val img = data.obj("images")?.arr("items")?.firstOrNull()?.jsonObject
                ?.arr("sources")?.firstOrNull()?.jsonObject?.str("url").orEmpty()
            SpotifyPlaylistSummary(
                id = uri.substringAfterLast(":"),
                name = name,
                trackCount = count,
                thumbnailUrl = img,
            )
        }
    }.getOrElse { emptyList() }

    private suspend fun restGet(token: String, path: String): JsonObject {
        val resp = http.get("$REST_BASE/$path") {
            header("Authorization", "Bearer $token")
            header("User-Agent", USER_AGENT)
            header("app-platform", "WebPlayer")
            header("Origin", "https://open.spotify.com")
            header("Referer", "https://open.spotify.com/")
            header("Accept", "application/json")
        }
        if (resp.status.value !in 200..299) error("Spotify REST HTTP ${resp.status.value}")
        return json.parseToJsonElement(resp.bodyAsText()).jsonObject
    }

    private suspend fun graphqlPost(
        token: String,
        operationName: String,
        variables: JsonObject = buildJsonObject {},
        allowHashDiscovery: Boolean = true,
    ): JsonObject {
        val hash = HASHES[operationName] ?: error("Unknown Spotify GQL op $operationName")
        val payload = buildJsonObject {
            put("variables", variables)
            put("operationName", operationName)
            putJsonObject("extensions") {
                putJsonObject("persistedQuery") {
                    put("version", 1)
                    put("sha256Hash", hash)
                }
            }
        }
        val resp = http.post(GQL_URL) {
            header("Authorization", "Bearer $token")
            header("User-Agent", USER_AGENT)
            header("app-platform", "WebPlayer")
            header("Origin", "https://open.spotify.com")
            header("Referer", "https://open.spotify.com/")
            setBody(TextContent(payload.toString(), ContentType.Application.Json.withParameter("charset", "UTF-8")))
        }
        val body = resp.bodyAsText()
        if (resp.status.value == 412 && allowHashDiscovery) {
            if (discoverLiveHashes()) {
                return graphqlPost(token, operationName, variables, allowHashDiscovery = false)
            }
        }
        if (resp.status.value !in 200..299) error("Spotify GQL HTTP ${resp.status.value}")
        val root = json.parseToJsonElement(body).jsonObject
        val errMsg = root.arr("errors")?.firstOrNull()?.jsonObject?.str("message").orEmpty()
        if (allowHashDiscovery && errMsg.contains("PersistedQueryNotFound", ignoreCase = true)) {
            if (discoverLiveHashes()) {
                return graphqlPost(token, operationName, variables, allowHashDiscovery = false)
            }
        }
        return root
    }

    private suspend fun discoverLiveHashes(): Boolean = runCatching {
        val html = http.get("https://open.spotify.com/") {
            header("User-Agent", USER_AGENT)
        }.bodyAsText()
        val bundles = BUNDLE_URL_REGEX.findAll(html).map { it.value }.distinct().take(12).toList()
        var updated = 0
        for (url in bundles) {
            val js = runCatching {
                http.get(url) { header("User-Agent", USER_AGENT) }.bodyAsText()
            }.getOrNull() ?: continue
            OPERATION_HASH_REGEX.findAll(js).forEach { m ->
                val op = m.groupValues[1]
                val h = m.groupValues[2]
                if (HASHES.containsKey(op) && HASHES[op] != h) {
                    HASHES[op] = h
                    updated++
                }
            }
            if (updated >= 4) break
        }
        updated > 0
    }.getOrDefault(false)

    private fun parseHexColor(hex: String): Long? {
        val clean = hex.trim().removePrefix("#")
        if (clean.length != 6) return null
        return runCatching { 0xFF000000L or clean.toLong(16) }.getOrNull()
    }

    private fun parseRestTrack(obj: JsonObject): SpotifySeedTrack? {
        val id = obj.str("id").orEmpty()
        val title = obj.str("name").orEmpty()
        if (title.isBlank()) return null
        val artist = obj.arr("artists").orEmpty()
            .mapNotNull { it.jsonObject.str("name") }
            .joinToString(", ")
            .ifBlank { "Unknown Artist" }
        val albumObj = obj.obj("album")
        val album = albumObj?.str("name").orEmpty()
        val thumb = albumObj?.arr("images")?.firstOrNull()?.jsonObject?.str("url").orEmpty()
        return SpotifySeedTrack(id = id, title = title, artist = artist, album = album, thumbnailUrl = thumb)
    }

    private fun parseGqlTrack(data: JsonObject): SpotifySeedTrack? {
        val title = data.str("name").orEmpty()
        if (title.isBlank()) return null
        val uri = data.str("uri") ?: data.str("_uri").orEmpty()
        val artist = data.obj("artists")?.arr("items").orEmpty()
            .mapNotNull { it.jsonObject.obj("profile")?.str("name") }
            .joinToString(", ")
            .ifBlank { "Unknown Artist" }
        val albumObj = data.obj("albumOfTrack")
        val album = albumObj?.str("name").orEmpty()
        val thumb = albumObj?.obj("coverArt")?.arr("sources")?.firstOrNull()?.jsonObject?.str("url").orEmpty()
        return SpotifySeedTrack(id = uri.substringAfterLast(":"), title = title, artist = artist, album = album, thumbnailUrl = thumb)
    }

    private suspend fun fetchLatestNuance(): Nuance = runCatching {
        val resp = http.get(NUANCE_URL) { header("User-Agent", USER_AGENT) }
        if (resp.status.value in 200..299) {
            val list = json.decodeFromString<List<Nuance>>(resp.bodyAsText())
            val best = list.filter { it.v > 0 && it.s.matches(Regex("^[A-Z2-7]+=*$")) }.maxByOrNull { it.v }
            if (best != null) {
                cachedNuance = best
                return@runCatching best
            }
        }
        cachedNuance
    }.getOrElse { cachedNuance }

    private suspend fun fetchServerTime(): Long = runCatching {
        val resp = http.get(SERVER_TIME_URL) { header("User-Agent", USER_AGENT) }
        if (resp.status.value in 200..299) {
            json.decodeFromString<ServerTimeResponse>(resp.bodyAsText()).serverTime
        } else {
            System.currentTimeMillis() / 1000L
        }
    }.getOrElse { System.currentTimeMillis() / 1000L }

    internal fun generateTotp(secret: String, serverTimeSec: Long): String {
        val key = base32Decode(secret)
        val timeStep = floor(serverTimeSec.toDouble() / 30.0).toLong()
        val timeBytes = ByteArray(8)
        var v = timeStep
        for (i in 7 downTo 0) {
            timeBytes[i] = (v and 0xFF).toByte()
            v = v shr 8
        }
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(timeBytes)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val code = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            ((hash[offset + 3].toInt() and 0xFF))
        return (code % 1_000_000).toString().padStart(6, '0')
    }

    private fun base32Decode(input: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val cleaned = input.uppercase().replace("=", "")
        val out = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0
        for (c in cleaned) {
            val value = alphabet.indexOf(c)
            if (value < 0) continue
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }

    private fun JsonObject.obj(k: String): JsonObject? =
        runCatching { this[k]?.takeIf { it !is JsonNull }?.jsonObject }.getOrNull()

    private fun JsonObject.arr(k: String): JsonArray? =
        runCatching { this[k]?.takeIf { it !is JsonNull }?.jsonArray }.getOrNull()

    private fun JsonObject.str(k: String): String? =
        runCatching { this[k]?.takeIf { it !is JsonNull }?.jsonPrimitive?.contentOrNull }.getOrNull()

    private fun JsonObject.int(k: String): Int? =
        runCatching { this[k]?.takeIf { it !is JsonNull }?.jsonPrimitive?.intOrNull }.getOrNull()
}
