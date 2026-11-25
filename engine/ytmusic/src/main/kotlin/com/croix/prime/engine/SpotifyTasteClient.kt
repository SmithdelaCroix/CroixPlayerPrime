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
data class SpotifyTasteSnapshot(
    val connected: Boolean = false,
    val accountName: String = "",
    val avatarUrl: String = "",
    val topArtists: List<SpotifySeedArtist> = emptyList(),
    val topTracks: List<SpotifySeedTrack> = emptyList(),
    val recentTracks: List<SpotifySeedTrack> = emptyList(),
    val likedTracks: List<SpotifySeedTrack> = emptyList(),
    val playlists: List<SpotifyPlaylistSummary> = emptyList(),
    val lastSyncedEpochMs: Long = 0L,
) {
    val allSeedTracks: List<SpotifySeedTrack>
        get() = (topTracks + recentTracks + likedTracks)
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
            .distinctBy { "${it.title.lowercase()}|${it.artist.lowercase()}" }

    val allSeedArtistNames: List<String>
        get() = (topArtists.map { it.name } + allSeedTracks.map { it.artist })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
}

/**
 * Lightweight Spotify Web Player client for syncing your real taste profile
 * (Top Artists, Top Tracks, Recently Played, Liked Songs, Playlists) to drive
 * Croix Player Prime's Hybrid Recommendation Algorithm & playlist importer.
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
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"

        private val HASHES = mapOf(
            "profileAttributes" to "53bcb064f6cd18c23f752bc324a791194d20df612d8e1239c735144ab0399ced",
            "libraryV3" to "973e511ca44261fda7eebac8b653155e7caee3675abb4fb110cc1b8c78b091c3",
            "fetchPlaylist" to "346811f856fb0b7e4f6c59f8ebea78dd081c6e2fb01b77c954b26259d5fc6763",
            "fetchLibraryTracks" to "087278b20b743578a6262c2b0b4bcd20d879c503cc359a2285baf083ef944240",
        )
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
            runCatching {
                val nuance = fetchLatestNuance()
                val serverTime = fetchServerTime()
                val totp = generateTotp(nuance.s, serverTime)
                val cookie = buildString {
                    append("sp_dc=${spDc.trim()}")
                    if (spKey.isNotBlank()) append("; sp_key=${spKey.trim()}")
                }
                val url = "$TOKEN_URL?reason=transport&productType=web-player&totp=$totp&totpServer=$totp&totpVer=${nuance.v}"
                val resp = http.get(url) {
                    header("User-Agent", USER_AGENT)
                    header("Cookie", cookie)
                    header("Accept", "application/json")
                }
                val body = resp.bodyAsText()
                if (resp.status.value !in 200..299) {
                    error("Spotify token HTTP ${resp.status.value}")
                }
                val parsed = json.decodeFromString<TokenResult>(body)
                if (parsed.isAnonymous || parsed.accessToken.isBlank()) {
                    error("Invalid or expired Spotify session (sp_dc)")
                }
                cachedToken = parsed.accessToken
                cachedTokenExpiryMs = parsed.accessTokenExpirationTimestampMs
                parsed
            }
        }
    }

    fun clearCachedToken() {
        cachedToken = null
        cachedTokenExpiryMs = 0L
    }

    suspend fun syncTasteProfile(spDc: String, spKey: String = ""): Result<SpotifyTasteSnapshot> =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = fetchAccessToken(spDc, spKey, forceRefresh = false).getOrThrow().accessToken

                coroutineScope {
                    val profileDef = async { fetchProfile(token) }
                    val topArtistsDef = async { fetchTopArtistsRest(token) }
                    val topTracksDef = async { fetchTopTracksRest(token) }
                    val recentDef = async { fetchRecentlyPlayedRest(token) }
                    val likedDef = async { fetchLikedTracksGql(token) }
                    val playlistsDef = async { fetchMyPlaylistsGql(token) }
                    val followedArtistsDef = async { fetchFollowedArtistsGql(token) }

                    val (name, avatar) = profileDef.await()
                    val topArtists = topArtistsDef.await().ifEmpty { followedArtistsDef.await() }
                    val topTracks = topTracksDef.await()
                    val recentTracks = recentDef.await()
                    val likedTracks = likedDef.await()
                    val playlists = playlistsDef.await()

                    SpotifyTasteSnapshot(
                        connected = true,
                        accountName = name.ifBlank { "Spotify User" },
                        avatarUrl = avatar,
                        topArtists = topArtists,
                        topTracks = topTracks,
                        recentTracks = recentTracks,
                        likedTracks = likedTracks,
                        playlists = playlists,
                        lastSyncedEpochMs = System.currentTimeMillis(),
                    )
                }
            }
        }

    suspend fun fetchPlaylistTracks(
        spDc: String,
        spKey: String,
        playlistId: String,
        limit: Int = 80,
    ): Result<List<SpotifySeedTrack>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = fetchAccessToken(spDc, spKey).getOrThrow().accessToken
            val vars = buildJsonObject {
                put("uri", "spotify:playlist:$playlistId")
                put("offset", 0)
                put("limit", limit)
                put("enableWatchFeedEntrypoint", false)
            }
            val root = graphqlPost(token, "fetchPlaylist", vars)
            val items = root.obj("data")?.obj("playlistV2")?.obj("content")?.arr("items").orEmpty()
            items.mapNotNull { elem ->
                val wrapper = elem.jsonObject.obj("itemV2") ?: return@mapNotNull null
                val data = wrapper.obj("data") ?: return@mapNotNull null
                parseGqlTrack(data)
            }
        }
    }

    /**
     * Also parses a public Spotify Playlist URL (e.g. https://open.spotify.com/playlist/...)
     * directly from Spotify's public embed page if the user pastes a Spotify link in the URL importer!
     */
    suspend fun fetchPublicSpotifyPlaylist(rawUrl: String): Result<Pair<String, List<SpotifySeedTrack>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val id = extractSpotifyPlaylistId(rawUrl) ?: error("Invalid Spotify playlist URL")
                val embedResp = http.get("https://open.spotify.com/embed/playlist/$id") {
                    header("User-Agent", USER_AGENT)
                }.bodyAsText()
                val nextData = Regex("""<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
                    .find(embedResp)?.groupValues?.get(1)
                    ?: error("Could not read public Spotify playlist metadata")
                val root = json.parseToJsonElement(nextData).jsonObject
                val entity = root.obj("props")?.obj("pageProps")?.obj("state")?.obj("data")?.obj("entity")
                    ?: error("Spotify playlist entity unavailable")
                val title = entity.str("name").orEmpty().ifBlank { "Spotify Playlist" }
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
                if (tracks.isEmpty()) error("Spotify playlist has no readable tracks")
                title to tracks
            }
        }

    fun extractSpotifyPlaylistId(rawUrl: String): String? =
        Regex("""open\.spotify\.com/(?:intl-[a-z]+/)?playlist/([A-Za-z0-9]+)""")
            .find(rawUrl.trim())
            ?.groupValues
            ?.get(1)

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
        }.getOrElse { "Spotify User" to "" }
    }

    private suspend fun fetchTopArtistsRest(token: String): List<SpotifySeedArtist> = runCatching {
        val root = restGet(token, "me/top/artists?time_range=medium_term&limit=15")
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

    private suspend fun fetchTopTracksRest(token: String): List<SpotifySeedTrack> = runCatching {
        val root = restGet(token, "me/top/tracks?time_range=medium_term&limit=20")
        root.arr("items").orEmpty().mapNotNull { parseRestTrack(it.jsonObject) }
    }.getOrElse { emptyList() }

    private suspend fun fetchRecentlyPlayedRest(token: String): List<SpotifySeedTrack> = runCatching {
        val root = restGet(token, "me/player/recently-played?limit=20")
        root.arr("items").orEmpty().mapNotNull { el ->
            el.jsonObject.obj("track")?.let(::parseRestTrack)
        }
    }.getOrElse { emptyList() }

    private suspend fun fetchLikedTracksGql(token: String): List<SpotifySeedTrack> = runCatching {
        val vars = buildJsonObject {
            put("offset", 0)
            put("limit", 25)
        }
        val root = graphqlPost(token, "fetchLibraryTracks", vars)
        val items = root.obj("data")?.obj("me")?.obj("library")?.obj("tracks")?.arr("items").orEmpty()
        items.mapNotNull { el ->
            val data = el.jsonObject.obj("track")?.obj("data") ?: return@mapNotNull null
            parseGqlTrack(data)
        }
    }.getOrElse { emptyList() }

    private suspend fun fetchFollowedArtistsGql(token: String): List<SpotifySeedArtist> = runCatching {
        val vars = buildJsonObject {
            putJsonArray("filters") { add("Artists") }
            put("order", null as String?)
            put("textFilter", "")
            putJsonArray("features") { add("LIKED_SONGS") }
            put("limit", 20)
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

    private suspend fun fetchMyPlaylistsGql(token: String): List<SpotifyPlaylistSummary> = runCatching {
        val vars = buildJsonObject {
            putJsonArray("filters") { add("Playlists") }
            put("order", null as String?)
            put("textFilter", "")
            putJsonArray("features") { add("LIKED_SONGS") }
            put("limit", 30)
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
            header("Accept", "application/json")
        }
        if (resp.status.value !in 200..299) error("Spotify REST HTTP ${resp.status.value}")
        return json.parseToJsonElement(resp.bodyAsText()).jsonObject
    }

    private suspend fun graphqlPost(
        token: String,
        operationName: String,
        variables: JsonObject = buildJsonObject {},
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
        if (resp.status.value !in 200..299) error("Spotify GQL HTTP ${resp.status.value}")
        return json.parseToJsonElement(resp.bodyAsText()).jsonObject
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
            (hash[offset + 3].toInt() and 0xFF)
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
