/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@Serializable
data class ExternalIntegrationsConfig(
    val discordEnabled: Boolean = false,
    val discordToken: String = "",
    val lastFmEnabled: Boolean = false,
    val lastFmApiKey: String = "",
    val lastFmApiSecret: String = "",
    val lastFmSessionKey: String = "",
    val lastFmUsername: String = "",
    val listenBrainzEnabled: Boolean = false,
    val listenBrainzToken: String = "",
) {
    val isDiscordReady: Boolean get() = discordEnabled && discordToken.isNotBlank()
    val isLastFmReady: Boolean get() = lastFmEnabled && lastFmApiKey.isNotBlank() && lastFmApiSecret.isNotBlank() && lastFmSessionKey.isNotBlank()
    val isListenBrainzReady: Boolean get() = listenBrainzEnabled && listenBrainzToken.isNotBlank()
}

/**
 * Discord Gateway v10 WebSocket Rich Presence client ("Escuchando Croix Player Prime").
 */
class DiscordRpcClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    @Volatile
    private var webSocket: WebSocket? = null
    @Volatile
    private var activeToken: String = ""
    @Volatile
    private var lastSequence: Long? = null
    private var heartbeatJob: Job? = null
    @Volatile
    private var pendingPresencePayload: String? = null

    fun updatePresence(
        token: String,
        track: Track?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
    ) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            disconnect()
            return
        }
        val presenceJson = buildPresencePayload(track, isPlaying, positionMs, durationMs)
        pendingPresencePayload = presenceJson

        if (webSocket == null || activeToken != cleanToken) {
            disconnect()
            activeToken = cleanToken
            connectGateway(cleanToken)
        } else {
            runCatching { webSocket?.send(presenceJson) }
        }
    }

    fun clearPresence() {
        val emptyPayload = buildPresencePayload(null, false, 0L, 0L)
        pendingPresencePayload = emptyPayload
        runCatching { webSocket?.send(emptyPayload) }
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        runCatching { webSocket?.close(1000, "Croix Prime") }
        webSocket = null
        activeToken = ""
        lastSequence = null
    }

    private fun connectGateway(token: String) {
        val req = Request.Builder()
            .url("wss://gateway.discord.gg/?v=10&encoding=json")
            .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile) AppleWebKit/537.36")
            .build()

        webSocket = okHttp.newWebSocket(
            req,
            object : WebSocketListener() {
                override fun onMessage(ws: WebSocket, text: String) {
                    runCatching {
                        val obj = json.parseToJsonElement(text).jsonObject
                        obj["s"]?.jsonPrimitive?.longOrNull?.let { lastSequence = it }
                        val op = obj["op"]?.jsonPrimitive?.intOrNull ?: return
                        if (op == 10) {
                            val intervalMs = obj["d"]?.jsonObject?.get("heartbeat_interval")?.jsonPrimitive?.longOrNull ?: 41_250L
                            startHeartbeat(ws, intervalMs)
                            ws.send(buildIdentifyPayload(token))
                            pendingPresencePayload?.let { p ->
                                scope.launch {
                                    delay(1_200L)
                                    runCatching { ws.send(p) }
                                }
                            }
                        } else if (op == 1) {
                            sendHeartbeatNow(ws)
                        }
                    }
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    if (webSocket === ws) {
                        heartbeatJob?.cancel()
                        webSocket = null
                    }
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    if (webSocket === ws) {
                        heartbeatJob?.cancel()
                        webSocket = null
                    }
                }
            },
        )
    }

    private fun startHeartbeat(ws: WebSocket, intervalMs: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            delay((intervalMs * 0.4).toLong().coerceAtLeast(3_000L))
            while (webSocket === ws) {
                sendHeartbeatNow(ws)
                delay(intervalMs.coerceAtLeast(10_000L))
            }
        }
    }

    private fun sendHeartbeatNow(ws: WebSocket) {
        val seq = lastSequence?.toString() ?: "null"
        runCatching { ws.send("""{"op":1,"d":$seq}""") }
    }

    private fun buildIdentifyPayload(token: String): String = buildJsonObject {
        put("op", 2)
        putJsonObject("d") {
            put("token", token)
            put("intents", 0)
            putJsonObject("properties") {
                put("os", "Android")
                put("browser", "Discord Android")
                put("device", "Croix Player Prime")
            }
        }
    }.toString()

    private fun buildPresencePayload(
        track: Track?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
    ): String = buildJsonObject {
        put("op", 3)
        putJsonObject("d") {
            put("since", System.currentTimeMillis())
            put("status", "online")
            put("afk", false)
            putJsonArray("activities") {
                if (track != null && isPlaying) {
                    val now = System.currentTimeMillis()
                    val startMs = (now - positionMs.coerceAtLeast(0L)).coerceAtLeast(0L)
                    val endMs = if (durationMs > 0L) startMs + durationMs else null
                    add(
                        buildJsonObject {
                            put("name", "Croix Player Prime")
                            put("type", 2) // Listening
                            put("details", track.title.take(120))
                            put("state", "por ${track.artist}".take(120))
                            putJsonObject("timestamps") {
                                put("start", startMs)
                                if (endMs != null) put("end", endMs)
                            }
                        },
                    )
                }
            }
        }
    }.toString()
}

/**
 * Last.fm Scrobbler & Now Playing API (MD5-signed) + ListenBrainz Scrobbler.
 */
class ScrobbleClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val http = HttpClient(OkHttp) { expectSuccess = false }

    suspend fun authenticateLastFm(
        apiKey: String,
        apiSecret: String,
        username: String,
        password: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val params = mapOf(
                "api_key" to apiKey.trim(),
                "method" to "auth.getMobileSession",
                "password" to password,
                "username" to username.trim(),
            )
            val sig = buildLastFmApiSignature(params, apiSecret.trim())
            val resp = http.submitForm(
                url = "https://ws.audioscrobbler.com/2.0/",
                formParameters = parameters {
                    params.forEach { (k, v) -> append(k, v) }
                    append("api_sig", sig)
                    append("format", "json")
                },
            )
            val root = json.parseToJsonElement(resp.bodyAsText()).jsonObject
            val errMsg = root["message"]?.jsonPrimitive?.contentOrNull
            val sessionKey = root["session"]?.jsonObject?.get("key")?.jsonPrimitive?.contentOrNull
            if (sessionKey.isNullOrBlank()) {
                error(errMsg ?: "Credenciales inválidas de Last.fm")
            }
            sessionKey
        }
    }

    suspend fun updateNowPlayingLastFm(config: ExternalIntegrationsConfig, track: Track): Boolean = withContext(Dispatchers.IO) {
        if (!config.isLastFmReady) return@withContext false
        runCatching {
            val params = mutableMapOf(
                "api_key" to config.lastFmApiKey.trim(),
                "artist" to track.artist,
                "method" to "track.updateNowPlaying",
                "sk" to config.lastFmSessionKey.trim(),
                "track" to track.title,
            )
            if (track.album.isNotBlank()) params["album"] = track.album
            if (track.durationSeconds > 0) params["duration"] = track.durationSeconds.toString()
            val sig = buildLastFmApiSignature(params, config.lastFmApiSecret.trim())
            val resp = http.submitForm(
                url = "https://ws.audioscrobbler.com/2.0/",
                formParameters = parameters {
                    params.forEach { (k, v) -> append(k, v) }
                    append("api_sig", sig)
                    append("format", "json")
                },
            )
            resp.status.isSuccess()
        }.getOrDefault(false)
    }

    suspend fun scrobbleLastFm(
        config: ExternalIntegrationsConfig,
        track: Track,
        startedAtEpochSec: Long,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!config.isLastFmReady) return@withContext false
        runCatching {
            val params = mutableMapOf(
                "api_key" to config.lastFmApiKey.trim(),
                "artist" to track.artist,
                "method" to "track.scrobble",
                "sk" to config.lastFmSessionKey.trim(),
                "timestamp" to startedAtEpochSec.toString(),
                "track" to track.title,
            )
            if (track.album.isNotBlank()) params["album"] = track.album
            val sig = buildLastFmApiSignature(params, config.lastFmApiSecret.trim())
            val resp = http.submitForm(
                url = "https://ws.audioscrobbler.com/2.0/",
                formParameters = parameters {
                    params.forEach { (k, v) -> append(k, v) }
                    append("api_sig", sig)
                    append("format", "json")
                },
            )
            resp.status.isSuccess()
        }.getOrDefault(false)
    }

    suspend fun submitListenBrainz(
        token: String,
        track: Track,
        isPlayingNow: Boolean,
        listenedAtEpochSec: Long = System.currentTimeMillis() / 1000L,
    ): Boolean = withContext(Dispatchers.IO) {
        val clean = token.trim()
        if (clean.isBlank()) return@withContext false
        runCatching {
            val payload = buildJsonObject {
                put("listen_type", if (isPlayingNow) "playing_now" else "single")
                put(
                    "payload",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                if (!isPlayingNow) put("listened_at", listenedAtEpochSec)
                                putJsonObject("track_metadata") {
                                    put("artist_name", track.artist)
                                    put("track_name", track.title)
                                    if (track.album.isNotBlank()) put("release_name", track.album)
                                    putJsonObject("additional_info") {
                                        put("media_player", "Croix Player Prime")
                                        put("submission_client", "Croix Player Prime")
                                        if (track.durationSeconds > 0) put("duration_ms", track.durationSeconds * 1000L)
                                    }
                                }
                            },
                        )
                    },
                )
            }
            val resp = http.post("https://api.listenbrainz.org/1/submit-listens") {
                header("Authorization", "Token $clean")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            resp.status.isSuccess()
        }.getOrDefault(false)
    }

    companion object {
        fun buildLastFmApiSignature(params: Map<String, String>, secret: String): String {
            val concat = params.toSortedMap().entries.joinToString("") { (k, v) -> "$k$v" } + secret
            val digest = MessageDigest.getInstance("MD5").digest(concat.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
