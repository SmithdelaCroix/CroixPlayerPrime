/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.croix.prime.engine.AiApiConfig
import com.croix.prime.engine.ExternalIntegrationsConfig
import com.croix.prime.engine.ServerConnectionConfig
import com.croix.prime.engine.SpotifyTasteSnapshot
import com.croix.prime.engine.YtMusicPlaylistSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.serverSessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "croix_server_session")

@Immutable
@Serializable
data class ServerSessionState(
    val serverConfig: ServerConnectionConfig = ServerConnectionConfig(),
    val spotifySpDc: String = "",
    val spotifySpKey: String = "",
    val spotifyTaste: SpotifyTasteSnapshot = SpotifyTasteSnapshot(),
    val spotifyBlendWeight: Float = 0.55f,
    val integrations: ExternalIntegrationsConfig = ExternalIntegrationsConfig(),
    val aiConfig: AiApiConfig = AiApiConfig(),
    val ytMusicPlaylists: List<YtMusicPlaylistSummary> = emptyList(),
    val ytMusicSyncSummary: String = "",
) {
    val isSpotifyConnected: Boolean get() = spotifySpDc.isNotBlank() && spotifyTaste.connected
    val isYtMusicConnected: Boolean get() = serverConfig.hasYtMusicAccount
}

class ServerSessionRepository(private val context: Context) {
    private val key = stringPreferencesKey("server_session_v1")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    val sessionFlow: Flow<ServerSessionState> = context.serverSessionDataStore.data.map { prefs ->
        val raw = prefs[key] ?: return@map ServerSessionState()
        runCatching { json.decodeFromString<ServerSessionState>(raw) }.getOrElse { ServerSessionState() }
    }

    suspend fun update(transform: (ServerSessionState) -> ServerSessionState) {
        context.serverSessionDataStore.edit { prefs ->
            val current = prefs[key]?.let {
                runCatching { json.decodeFromString<ServerSessionState>(it) }.getOrNull()
            } ?: ServerSessionState()
            val updated = transform(current).let { s ->
                s.copy(spotifyBlendWeight = s.spotifyBlendWeight.coerceIn(0f, 1f))
            }
            prefs[key] = json.encodeToString(ServerSessionState.serializer(), updated)
        }
    }
}
