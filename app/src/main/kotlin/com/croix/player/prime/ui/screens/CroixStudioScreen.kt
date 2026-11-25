/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.croix.player.prime.BuildConfig
import com.croix.player.prime.data.AppIconManager
import com.croix.player.prime.data.AppUpdateInfo
import com.croix.player.prime.data.BlockedArtistEntity
import com.croix.player.prime.data.ServerSessionState
import com.croix.prime.design.CroixAnimatedBackdrop
import com.croix.prime.design.CroixDesignState
import com.croix.prime.design.CroixGlassCard
import com.croix.prime.design.CroixSeekBar
import com.croix.prime.design.CroixThemeJson
import com.croix.prime.design.LocalCroixDesign
import com.croix.prime.design.LocalCroixPalette
import com.croix.prime.design.LocalCroixStrings
import com.croix.prime.design.PrimeAudioQuality
import com.croix.prime.design.PrimeCardDensity
import com.croix.prime.design.PrimeColorMode
import com.croix.prime.design.PrimeLanguageMode
import com.croix.prime.design.PrimeLyricsAlign
import com.croix.prime.design.PrimeNavBarStyle
import com.croix.prime.design.PrimePlayerStyle
import com.croix.prime.design.PrimePresets
import com.croix.prime.design.PrimeProgressStyle
import com.croix.prime.design.PrimeTransitionStyle
import com.croix.prime.design.PrimeVisualizerStyle
import com.croix.prime.engine.ExternalIntegrationsConfig
import com.croix.prime.engine.ServerConnectionConfig
import com.croix.prime.engine.ServerHealthStatus
import com.croix.prime.engine.ServerPresets
import com.croix.prime.engine.SpotifyPlaylistSummary
import com.croix.prime.engine.YtMusicServerMode
import com.croix.prime.playback.AutoEqHeadphoneProfile
import com.croix.prime.playback.EqPreset
import com.croix.prime.playback.PlaybackUiState
import com.croix.prime.playback.ReverbPresetMode
import kotlin.math.roundToInt

private enum class SettingsPage {
    HOME,
    SERVERS,
    SPOTIFY,
    INTEGRATIONS,
    PRIVACY_BLOCKED,
    STUDIO,
    LYRICS_UI,
    AUDIO_ENGINE,
    EQUALIZER,
    CACHE_STORAGE,
    GESTURES,
    SPONSORBLOCK,
    UPDATES,
    ABOUT,
}

private data class SegmentedEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accent: Color,
    val badge: String? = null,
    val highlightDot: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun SettingsAndStudioOverlay(
    playback: PlaybackUiState,
    serverState: ServerSessionState,
    serverHealth: ServerHealthStatus?,
    adsBlockedCount: Int,
    spotifySyncing: Boolean,
    serverNotice: String?,
    updateInfo: AppUpdateInfo,
    audioCacheBytes: Long,
    blockedArtists: List<BlockedArtistEntity>,
    onClose: () -> Unit,
    onUpdateDesign: ((CroixDesignState) -> CroixDesignState) -> Unit,
    onApplyPreset: (CroixDesignState) -> Unit,
    onSelectAppIcon: (String) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetPitch: (Float) -> Unit,
    onSetSkipSilence: (Boolean) -> Unit,
    onSetAudio8dEnabled: (Boolean) -> Unit,
    onSetAudio8dSpeed: (Float) -> Unit,
    onSetReverbMode: (ReverbPresetMode) -> Unit,
    onApplyAutoEq: (AutoEqHeadphoneProfile) -> Unit,
    onSetEqEnabled: (Boolean) -> Unit,
    onApplyEqPreset: (EqPreset) -> Unit,
    onSetEqBand: (Int, Float) -> Unit,
    onSetBassBoost: (Float) -> Unit,
    onSetLoudness: (Int) -> Unit,
    onClearAudioCache: () -> Unit,
    onRefreshCacheSize: () -> Unit,
    onUnblockArtist: (String) -> Unit,
    onUpdateIntegrations: ((ExternalIntegrationsConfig) -> ExternalIntegrationsConfig) -> Unit,
    onLoginLastFm: (String, String, String, String) -> Unit,
    onUpdateServerConfig: ((ServerConnectionConfig) -> ServerConnectionConfig) -> Unit,
    onPingServer: () -> Unit,
    onOpenYtMusicLogin: () -> Unit,
    onDisconnectYtMusic: () -> Unit,
    onOpenSpotifyLogin: () -> Unit,
    onConnectSpotifyManual: (String) -> Unit,
    onSyncSpotifyNow: () -> Unit,
    onSetSpotifyBlendWeight: (Float) -> Unit,
    onApplyAlgorithmBlend: () -> Unit,
    onDisconnectSpotify: () -> Unit,
    onImportSpotifyPlaylist: (SpotifyPlaylistSummary) -> Unit,
    onCheckUpdates: () -> Unit,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val strings = LocalCroixStrings.current
    val es = strings.isSpanish
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val touchBlocker = remember { MutableInteractionSource() }

    var page by remember { mutableStateOf(SettingsPage.HOME) }
    var studioTab by remember { mutableIntStateOf(0) }
    var showImport by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }
    var importErr by remember { mutableStateOf<String?>(null) }
    var showManualSpDc by remember { mutableStateOf(false) }
    var manualSpDc by remember { mutableStateOf("") }
    var customServerDraft by remember(serverState.serverConfig.customServerUrl) {
        mutableStateOf(serverState.serverConfig.customServerUrl)
    }

    BackHandler(enabled = page != SettingsPage.HOME) {
        page = SettingsPage.HOME
    }

    fun openExternalUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = touchBlocker, indication = null) {},
    ) {
        CroixAnimatedBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CroixGlassCard(
                    cornerRadius = 50.dp,
                    onClick = { if (page == SettingsPage.HOME) onClose() else page = SettingsPage.HOME },
                ) {
                    IconButton(
                        onClick = { if (page == SettingsPage.HOME) onClose() else page = SettingsPage.HOME },
                        modifier = Modifier.size(42.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                }
                Column {
                    val pageTitle = when (page) {
                        SettingsPage.HOME -> if (es) "Configuración" else "Settings"
                        SettingsPage.SERVERS -> if (es) "Servidores y Ad-Block" else "Servers & Ad-Block"
                        SettingsPage.SPOTIFY -> if (es) "Algoritmo Spotify" else "Spotify Algorithm"
                        SettingsPage.INTEGRATIONS -> if (es) "Discord y Scrobbling" else "Discord & Scrobbling"
                        SettingsPage.PRIVACY_BLOCKED -> if (es) "Privacidad y Bloqueos" else "Privacy & Blocked"
                        SettingsPage.STUDIO -> if (es) "Estudio de Apariencia" else "Appearance Studio"
                        SettingsPage.LYRICS_UI -> if (es) "Letras y Visualizador" else "Lyrics & Visualizer"
                        SettingsPage.AUDIO_ENGINE -> if (es) "Reproductor y Audio" else "Player & Audio"
                        SettingsPage.EQUALIZER -> if (es) "Audio 8D, Reverb y AutoEq" else "8D Audio, Reverb & AutoEq"
                        SettingsPage.CACHE_STORAGE -> if (es) "Caché Inteligente de Audio" else "Smart Audio Cache"
                        SettingsPage.GESTURES -> if (es) "Gestos y Hardware" else "Gestures & Hardware"
                        SettingsPage.SPONSORBLOCK -> "SponsorBlock"
                        SettingsPage.UPDATES -> if (es) "Actualizaciones" else "Updates"
                        SettingsPage.ABOUT -> if (es) "Acerca de" else "About"
                    }
                    Text(pageTitle, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    if (page == SettingsPage.HOME) {
                        Text(
                            "Croix Player Prime • v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val enteringSubmenu = targetState != SettingsPage.HOME
                    val dir = if (enteringSubmenu) 1 else -1
                    val dur = (260f / design.transitionSpeed).toInt().coerceIn(90, 500)
                    when (design.transitionStyle) {
                        PrimeTransitionStyle.NONE -> fadeIn(tween(1)).togetherWith(fadeOut(tween(1)))
                        PrimeTransitionStyle.GENTLE_FADE -> fadeIn(tween(dur)).togetherWith(fadeOut(tween(dur / 2)))
                        PrimeTransitionStyle.SNAPPY_ZOOM, PrimeTransitionStyle.SMOOTH_SLIDE ->
                            (fadeIn(tween(dur, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(tween(dur, easing = FastOutSlowInEasing)) { dir * (it / 5) })
                                .togetherWith(
                                    fadeOut(tween((dur * 0.7f).toInt(), easing = FastOutSlowInEasing)) +
                                        slideOutHorizontally(tween((dur * 0.8f).toInt(), easing = FastOutSlowInEasing)) { -dir * (it / 6) },
                                )
                    }
                },
                label = "settings_submenu_transition",
                modifier = Modifier.weight(1f),
            ) { activePage ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 38.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (activePage) {
                        // =========================================================
                        // HOME: Grouped Material 3 Expressive Segmented Glass Menu
                        // =========================================================
                        SettingsPage.HOME -> {
                            // Update available banner right at top
                            if (updateInfo.hasUpdate) {
                                item {
                                    CroixGlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        accentTint = Color(0xFF00E676),
                                        onClick = { page = SettingsPage.UPDATES },
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF00E676).copy(alpha = 0.25f)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(Icons.Rounded.SystemUpdate, null, tint = Color(0xFF00E676))
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    if (es) "Nueva actualización disponible" else "New update available",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                Text(
                                                    "${updateInfo.latestLabel} • " + if (es) "Toca para descargar APK directo" else "Tap to download direct APK",
                                                    color = palette.textSecondary,
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White)
                                        }
                                    }
                                }
                            }

                            // Group 1: Cuenta e Integraciones
                            item {
                                SegmentedSettingsGroup(
                                    header = if (es) "CUENTA E INTEGRACIONES" else "ACCOUNT & INTEGRATIONS",
                                    items = listOf(
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Dns,
                                            title = if (es) "Servidores YouTube Music y Ad-Block" else "YouTube Music Servers & Ad-Block",
                                            subtitle = buildString {
                                                append(if (es) serverState.serverConfig.mode.labelEs else serverState.serverConfig.mode.labelEn)
                                                if (serverHealth?.reachable == true) append(" • ${serverHealth.latencyMs} ms")
                                                if (serverState.isYtMusicConnected) append(" • ${serverState.serverConfig.ytMusicAccountName}")
                                            },
                                            accent = palette.primary,
                                            badge = "$adsBlockedCount+ ads",
                                            onClick = { page = SettingsPage.SERVERS },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.AutoAwesome,
                                            title = if (es) "Spotify y Algoritmo Híbrido" else "Spotify & Hybrid Algorithm",
                                            subtitle = if (serverState.isSpotifyConnected) {
                                                "${serverState.spotifyTaste.accountName} • ${(serverState.spotifyBlendWeight * 100).roundToInt()}% Spotify"
                                            } else {
                                                if (es) "Conecta tu Spotify para potenciar recomendaciones" else "Connect Spotify to supercharge recommendations"
                                            },
                                            accent = Color(0xFF1DB954),
                                            badge = if (serverState.isSpotifyConnected) "Activo" else null,
                                            onClick = { page = SettingsPage.SPOTIFY },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Hub,
                                            title = if (es) "Discord RPC, Last.fm y ListenBrainz" else "Discord RPC, Last.fm & ListenBrainz",
                                            subtitle = buildString {
                                                val active = buildList {
                                                    if (serverState.integrations.isDiscordReady) add("Discord")
                                                    if (serverState.integrations.isLastFmReady) add("Last.fm")
                                                    if (serverState.integrations.isListenBrainzReady) add("ListenBrainz")
                                                }
                                                append(if (active.isEmpty()) (if (es) "Muestra lo que escuchas y sincroniza scrobbles" else "Show Rich Presence & sync scrobbles") else active.joinToString(" • "))
                                            },
                                            accent = Color(0xFF5865F2),
                                            badge = if (serverState.integrations.isDiscordReady || serverState.integrations.isLastFmReady || serverState.integrations.isListenBrainzReady) "On" else null,
                                            onClick = { page = SettingsPage.INTEGRATIONS },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.VisibilityOff,
                                            title = if (es) "Sesión Privada y Artistas Bloqueados" else "Private Session & Blocked Artists",
                                            subtitle = if (design.privateSessionEnabled) {
                                                if (es) "Incógnito ACTIVO • ${blockedArtists.size} artistas bloqueados" else "Incognito ON • ${blockedArtists.size} blocked artists"
                                            } else {
                                                if (es) "Pausa historial/scrobbling • ${blockedArtists.size} bloqueados" else "Pause history/scrobbling • ${blockedArtists.size} blocked"
                                            },
                                            accent = Color(0xFFCE93D8),
                                            badge = if (design.privateSessionEnabled) "Incógnito" else null,
                                            onClick = { page = SettingsPage.PRIVACY_BLOCKED },
                                        ),
                                    ),
                                )
                            }

                            // Group 2: Interfaz y Personalización
                            item {
                                SegmentedSettingsGroup(
                                    header = if (es) "INTERFAZ Y PERSONALIZACIÓN" else "APPEARANCE & PERSONALIZATION",
                                    items = listOf(
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Palette,
                                            title = if (es) "Estudio de Apariencia" else "Appearance Studio",
                                            subtitle = if (es) "Cristal, colores, inicio, transiciones y vista previa en vivo" else "Glass, colors, Home layout, transitions & live preview",
                                            accent = palette.primary,
                                            badge = design.presetName,
                                            onClick = { page = SettingsPage.STUDIO },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Lyrics,
                                            title = if (es) "Letras y Visualizador" else "Lyrics & Visualizer",
                                            subtitle = "${design.lyricsFontSizeSp.roundToInt()} sp • ${design.lyricsAlign.label} • ${design.visualizerStyle.label}",
                                            accent = palette.secondary,
                                            onClick = { page = SettingsPage.LYRICS_UI },
                                        ),
                                    ),
                                )
                            }

                            // Group 3: Reproducción y Audio
                            item {
                                SegmentedSettingsGroup(
                                    header = if (es) "REPRODUCCIÓN Y AUDIO" else "PLAYBACK & AUDIO",
                                    items = listOf(
                                        SegmentedEntry(
                                            icon = Icons.Rounded.PlayArrow,
                                            title = if (es) "Reproductor y Motor de Audio" else "Player & Audio Engine",
                                            subtitle = "${if (es) design.audioQuality.labelEs else design.audioQuality.labelEn} • ${"%.2f".format(playback.playbackSpeed)}x • Crossfade ${if (design.crossfadeSeconds == 0) "Off" else "${design.crossfadeSeconds}s"}",
                                            accent = palette.primary,
                                            onClick = { page = SettingsPage.AUDIO_ENGINE },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Equalizer,
                                            title = if (es) "Audio 8D, Reverb, AutoEq y Salida" else "8D Audio, Reverb, AutoEq & Output",
                                            subtitle = buildString {
                                                append(if (playback.audio8dEnabled) "8D Activo" else "8D Off")
                                                append(" • ${playback.reverbMode.label}")
                                                append(" • ${playback.activeOutput.route.labelEs}")
                                            },
                                            accent = palette.secondary,
                                            badge = if (playback.audio8dEnabled) "8D" else null,
                                            onClick = { page = SettingsPage.EQUALIZER },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Storage,
                                            title = if (es) "Caché Inteligente de Audio" else "Smart Audio Cache",
                                            subtitle = if (design.audioCacheLimitMb == 0) {
                                                if (es) "Desactivada • Streaming puro" else "Off • Pure streaming"
                                            } else {
                                                "${"%.1f".format(audioCacheBytes / (1024f * 1024f))} MB usados de ${design.audioCacheLimitMb} MB"
                                            },
                                            accent = Color(0xFFFFB300),
                                            badge = if (design.audioCacheLimitMb > 0) "${design.audioCacheLimitMb}MB" else "Off",
                                            onClick = {
                                                onRefreshCacheSize()
                                                page = SettingsPage.CACHE_STORAGE
                                            },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Shield,
                                            title = "SponsorBlock",
                                            subtitle = if (design.sponsorBlockEnabled) {
                                                if (es) "Saltando intros, patrocinios y partes no musicales" else "Auto-skipping sponsors & non-music"
                                            } else {
                                                if (es) "Desactivado" else "Disabled"
                                            },
                                            accent = Color(0xFF00E5FF),
                                            onClick = { page = SettingsPage.SPONSORBLOCK },
                                        ),
                                    ),
                                )
                            }

                            // Group 4: Comportamiento y Sistema
                            item {
                                SegmentedSettingsGroup(
                                    header = if (es) "COMPORTAMIENTO Y SISTEMA" else "BEHAVIOR & SYSTEM",
                                    items = listOf(
                                        SegmentedEntry(
                                            icon = Icons.Rounded.TouchApp,
                                            title = if (es) "Gestos y Hardware" else "Gestures & Hardware",
                                            subtitle = if (es) "Deslizar carátula, doble toque ±${design.doubleTapSeekSeconds}s y agitar" else "Swipe artwork, double-tap ±${design.doubleTapSeekSeconds}s & shake",
                                            accent = palette.primary,
                                            onClick = { page = SettingsPage.GESTURES },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.SystemUpdate,
                                            title = if (es) "Actualizaciones" else "Updates",
                                            subtitle = if (updateInfo.hasUpdate) {
                                                if (es) "Disponible: ${updateInfo.latestLabel}" else "Available: ${updateInfo.latestLabel}"
                                            } else {
                                                if (es) "Actualización directa sin conflicto de paquetes" else "Direct updates with permanent signature"
                                            },
                                            accent = Color(0xFF00E676),
                                            badge = "v${BuildConfig.VERSION_NAME.substringBefore("-")}",
                                            highlightDot = updateInfo.hasUpdate,
                                            onClick = { page = SettingsPage.UPDATES },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Info,
                                            title = if (es) "Acerca de Croix Player Prime" else "About Croix Player Prime",
                                            subtitle = "v${BuildConfig.VERSION_NAME} • arm64-v8a • Android 12+",
                                            accent = palette.secondary,
                                            onClick = { page = SettingsPage.ABOUT },
                                        ),
                                    ),
                                )
                            }

                            // Quick Language Selector Card
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Rounded.Language, null, tint = palette.primary, modifier = Modifier.size(18.dp))
                                            Text(if (es) "Idioma de la aplicación" else "App Language", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        StudioEnumRow(
                                            title = "",
                                            entries = PrimeLanguageMode.entries,
                                            selected = design.languageMode,
                                            labelOf = { if (es) it.labelEs else it.labelEn },
                                        ) { lang ->
                                            onUpdateDesign { it.copy(languageMode = lang) }
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================
                        // SUBMENU: SERVERS & AD-BLOCK
                        // =========================================================
                        SettingsPage.SERVERS -> {
                            if (serverNotice != null) {
                                item {
                                    CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = palette.primary) {
                                        Text(serverNotice, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(14.dp))
                                    }
                                }
                            }
                            item {
                                val cfg = serverState.serverConfig
                                CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(Icons.Rounded.CloudDone, null, tint = palette.primary)
                                                Text(strings.ytServerSection, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            CroixGlassCard(cornerRadius = 50.dp, onClick = onPingServer) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                ) {
                                                    Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                    Text(strings.pingServer, color = Color.White, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            val statusColor = when {
                                                serverHealth == null -> palette.textSecondary
                                                serverHealth.reachable -> Color(0xFF00E676)
                                                else -> Color(0xFFFF5252)
                                            }
                                            CroixGlassCard(modifier = Modifier.weight(1f), cornerRadius = 14.dp) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(serverHealth?.endpointLabel ?: "music.youtube.com", color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                                    Text(
                                                        text = when {
                                                            serverHealth == null -> "Verificando..."
                                                            serverHealth.reachable -> "● En línea • ${serverHealth.latencyMs} ms"
                                                            else -> "● ${serverHealth.message}"
                                                        },
                                                        color = statusColor,
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                }
                                            }
                                            CroixGlassCard(modifier = Modifier.weight(1f), cornerRadius = 14.dp, accentTint = palette.secondary) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(strings.adsBlockedLabel, color = palette.textSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                                    Text("$adsBlockedCount+", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                                                }
                                            }
                                        }

                                        StudioEnumRow(
                                            title = strings.ytServerModeLabel,
                                            entries = YtMusicServerMode.entries,
                                            selected = cfg.mode,
                                            labelOf = { if (es) it.labelEs else it.labelEn },
                                        ) { m -> onUpdateServerConfig { it.copy(mode = m) } }

                                        if (cfg.mode == YtMusicServerMode.PIPED_RELAY) {
                                            Text(if (es) "Nodo Relay Piped (Sin anuncios)" else "Piped Relay Node", color = Color.White, fontWeight = FontWeight.SemiBold)
                                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                ServerPresets.PipedNodes.forEach { node ->
                                                    CroixGlassCard(
                                                        cornerRadius = 12.dp,
                                                        accentTint = if (cfg.pipedBaseUrl == node.apiBaseUrl) palette.primary else null,
                                                        onClick = { onUpdateServerConfig { it.copy(pipedBaseUrl = node.apiBaseUrl) } },
                                                    ) {
                                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                                            Text(node.name, color = Color.White, style = MaterialTheme.typography.labelMedium)
                                                            Text(node.region, color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (cfg.mode == YtMusicServerMode.CUSTOM_INSTANCE) {
                                            OutlinedTextField(
                                                value = customServerDraft,
                                                onValueChange = { customServerDraft = it },
                                                label = { Text("https://mi-servidor-piped.ejemplo") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                            Button(
                                                onClick = { onUpdateServerConfig { it.copy(customServerUrl = customServerDraft.trim()) } },
                                                modifier = Modifier.fillMaxWidth(),
                                            ) { Text(if (es) "Guardar y Conectar Servidor" else "Save & Connect Server") }
                                        }

                                        StudioSwitch(strings.strictAdBlockLabel, cfg.strictAdBlockEnabled) { enabled ->
                                            onUpdateServerConfig { it.copy(strictAdBlockEnabled = enabled) }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Cuenta YouTube Music", color = Color.White, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    if (serverState.isYtMusicConnected) "${strings.connectedAs}: ${cfg.ytMusicAccountName}" else strings.notConnected,
                                                    color = if (serverState.isYtMusicConnected) Color(0xFF00E676) else palette.textSecondary,
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                            if (serverState.isYtMusicConnected) {
                                                OutlinedButton(onClick = onDisconnectYtMusic) { Text(strings.disconnect) }
                                            } else {
                                                Button(onClick = onOpenYtMusicLogin) { Text(if (es) "Conectar" else "Connect") }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================
                        // SUBMENU: SPOTIFY ALGORITHM & SYNC
                        // =========================================================
                        SettingsPage.SPOTIFY -> {
                            if (serverNotice != null) {
                                item {
                                    CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFF1DB954)) {
                                        Text(serverNotice, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(14.dp))
                                    }
                                }
                            }
                            item {
                                val taste = serverState.spotifyTaste
                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFF1DB954)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFF1DB954))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(strings.spotifySectionTitle, color = Color.White, fontWeight = FontWeight.Bold)
                                                Text(strings.spotifySectionSub, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                                            }
                                            if (spotifySyncing) {
                                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color(0xFF1DB954))
                                            }
                                        }

                                        if (serverState.isSpotifyConnected) {
                                            CroixGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF1DB954))
                                                        Column {
                                                            Text(taste.accountName, color = Color.White, fontWeight = FontWeight.Bold)
                                                            Text(
                                                                "${taste.topArtists.size} artistas • ${taste.allSeedTracks.size} temas • ${taste.playlists.size} playlists",
                                                                color = palette.textSecondary,
                                                                style = MaterialTheme.typography.bodySmall,
                                                            )
                                                        }
                                                    }
                                                    OutlinedButton(onClick = onDisconnectSpotify) { Text(strings.disconnect) }
                                                }
                                            }

                                            StudioSlider(
                                                label = strings.algoBlendLabel,
                                                value = serverState.spotifyBlendWeight,
                                                range = 0f..1f,
                                                valueLabel = "${(serverState.spotifyBlendWeight * 100).roundToInt()}% Spotify / ${((1f - serverState.spotifyBlendWeight) * 100).roundToInt()}% YT",
                                                onChange = onSetSpotifyBlendWeight,
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                Button(onClick = onSyncSpotifyNow, enabled = !spotifySyncing, modifier = Modifier.weight(1f)) {
                                                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(strings.syncSpotifyNow)
                                                }
                                                OutlinedButton(onClick = onApplyAlgorithmBlend, modifier = Modifier.weight(1f)) {
                                                    Text(if (es) "Aplicar al Inicio" else "Apply to Home")
                                                }
                                            }

                                            if (taste.playlists.isNotEmpty()) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(strings.importSpotifyPlaylists, color = Color(0xFF1DB954), fontWeight = FontWeight.Bold)
                                                taste.playlists.take(15).forEach { pl ->
                                                    CroixGlassCard(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        cornerRadius = 12.dp,
                                                        onClick = { if (!spotifySyncing) onImportSpotifyPlaylist(pl) },
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(pl.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                                Text("${pl.trackCount} canciones", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                                                            }
                                                            Icon(Icons.Rounded.Download, null, tint = Color(0xFF1DB954))
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                Button(onClick = onOpenSpotifyLogin, modifier = Modifier.weight(1f)) {
                                                    Icon(Icons.Rounded.Link, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(strings.connectSpotify)
                                                }
                                                OutlinedButton(onClick = { manualSpDc = ""; showManualSpDc = true }) { Text("sp_dc") }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================
                        // SUBMENU: DEEP DYNAMIC APPEARANCE STUDIO + LIVE PREVIEW
                        // =========================================================
                        SettingsPage.STUDIO -> {
                            // Live Interactive Preview Card
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = palette.primary) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(design.artworkCornerDp.coerceAtMost(22f).dp))
                                                        .background(
                                                            if (design.useGradientStyle) Brush.linearGradient(listOf(palette.primary, palette.secondary))
                                                            else Brush.linearGradient(listOf(palette.primary, palette.primary)),
                                                        ),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(Icons.Rounded.GraphicEq, null, tint = Color.White)
                                                }
                                                Column {
                                                    Text(if (es) "Vista previa en vivo" else "Live Preview", color = Color.White, fontWeight = FontWeight.Bold)
                                                    Text("${design.presetName} • ${design.progressStyle.label}", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF0A0910))
                                            }
                                        }
                                        CroixSeekBar(progress = 0.62f, onSeek = {})
                                    }
                                }
                            }

                            val tabs = if (es) {
                                listOf("Cristal", "Colores", "Inicio", "Reproductor", "Transiciones", "Presets")
                            } else {
                                listOf("Glass", "Colors", "Home", "Player", "Transitions", "Presets")
                            }
                            item {
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    tabs.forEachIndexed { idx, label ->
                                        CroixGlassCard(
                                            cornerRadius = 50.dp,
                                            accentTint = if (studioTab == idx) palette.primary else null,
                                            onClick = { studioTab = idx },
                                        ) {
                                            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                                        }
                                    }
                                }
                            }

                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        when (studioTab) {
                                            0 -> {
                                                StudioSlider(if (es) "Transparencia del Cristal" else "Glass Opacity", design.glassOpacity, 0.05f..0.75f, "${(design.glassOpacity * 100).roundToInt()}%") { v ->
                                                    onUpdateDesign { it.copy(glassOpacity = v) }
                                                }
                                                StudioSlider(if (es) "Desenfoque de Barras" else "Floating Bar Blur", design.glassBlurDp, 0f..34f, "${design.glassBlurDp.roundToInt()} dp") { v ->
                                                    onUpdateDesign { it.copy(glassBlurDp = v) }
                                                }
                                                StudioSlider(if (es) "Brillo del Borde" else "Border Glow", design.borderGlowAlpha, 0f..0.65f, "${(design.borderGlowAlpha * 100).roundToInt()}%") { v ->
                                                    onUpdateDesign { it.copy(borderGlowAlpha = v) }
                                                }
                                                StudioSlider(if (es) "Grosor del Borde" else "Border Width", design.borderWidthDp, 0.5f..2.5f, "${"%.1f".format(design.borderWidthDp)} dp") { v ->
                                                    onUpdateDesign { it.copy(borderWidthDp = v) }
                                                }
                                                StudioSlider(if (es) "Redondeo de Esquinas" else "Corner Radius", design.cornerRadiusDp, 8f..32f, "${design.cornerRadiusDp.roundToInt()} dp") { v ->
                                                    onUpdateDesign { it.copy(cornerRadiusDp = v) }
                                                }
                                                StudioSlider(if (es) "Escala de Tipografía" else "Font Scale", design.fontScale, 0.85f..1.20f, "${(design.fontScale * 100).roundToInt()}%") { v ->
                                                    onUpdateDesign { it.copy(fontScale = v) }
                                                }
                                                StudioSlider(if (es) "Resplandor Ambiental" else "Ambient Glow", design.ambientGlowStrength, 0f..0.9f, "${(design.ambientGlowStrength * 100).roundToInt()}%") { v ->
                                                    onUpdateDesign { it.copy(ambientGlowStrength = v) }
                                                }
                                                StudioSwitch(if (es) "Fondo Animado Reactivo" else "Animated Backdrop", design.backdropMotionEnabled) { v ->
                                                    onUpdateDesign { it.copy(backdropMotionEnabled = v) }
                                                }
                                                if (design.backdropMotionEnabled) {
                                                    StudioSlider(if (es) "Velocidad del Fondo" else "Motion Speed", design.backdropMotionSpeed, 0.2f..2.5f, "${"%.1f".format(design.backdropMotionSpeed)}x") { v ->
                                                        onUpdateDesign { it.copy(backdropMotionSpeed = v) }
                                                    }
                                                }
                                            }
                                            1 -> {
                                                Text(if (es) "Modo de Relleno" else "Color Fill Mode", color = Color.White, fontWeight = FontWeight.Bold)
                                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                                    CroixGlassCard(
                                                        modifier = Modifier.weight(1f),
                                                        cornerRadius = 14.dp,
                                                        accentTint = if (design.useGradientStyle) palette.primary else null,
                                                        onClick = { onUpdateDesign { it.copy(useGradientStyle = true) } },
                                                    ) {
                                                        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                                            Text(strings.gradientMode, color = Color.White, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    CroixGlassCard(
                                                        modifier = Modifier.weight(1f),
                                                        cornerRadius = 14.dp,
                                                        accentTint = if (!design.useGradientStyle) palette.primary else null,
                                                        onClick = { onUpdateDesign { it.copy(useGradientStyle = false) } },
                                                    ) {
                                                        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                                            Text(strings.solidColorMode, color = Color.White, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                                StudioSwitch(if (es) "Fondo Negro Puro AMOLED" else "Pure Black AMOLED", design.pureBlackBackground) { v ->
                                                    onUpdateDesign { it.copy(pureBlackBackground = v) }
                                                }
                                                StudioEnumRow(if (es) "Paleta Base" else "Color Palette", PrimeColorMode.entries, design.colorMode, { it.label }) { m ->
                                                    onUpdateDesign { it.copy(colorMode = m) }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(42.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(
                                                            if (design.useGradientStyle) Brush.horizontalGradient(listOf(design.customPrimaryFromHue, design.customSecondaryFromHue))
                                                            else Brush.horizontalGradient(listOf(design.customPrimaryFromHue, design.customPrimaryFromHue)),
                                                        ),
                                                )
                                                StudioSlider(if (es) "Tono Primario" else "Primary Hue", design.primaryHue, 0f..360f, "${design.primaryHue.roundToInt()}°") { h ->
                                                    onUpdateDesign { it.copy(colorMode = PrimeColorMode.CUSTOM_GRADIENT, primaryHue = h) }
                                                }
                                                StudioSlider(if (es) "Saturación Primaria" else "Primary Saturation", design.primarySaturation, 0.25f..1.0f, "${(design.primarySaturation * 100).roundToInt()}%") { s ->
                                                    onUpdateDesign { it.copy(colorMode = PrimeColorMode.CUSTOM_GRADIENT, primarySaturation = s) }
                                                }
                                                if (design.useGradientStyle) {
                                                    StudioSlider(if (es) "Tono Secundario" else "Secondary Hue", design.secondaryHue, 0f..360f, "${design.secondaryHue.roundToInt()}°") { h ->
                                                        onUpdateDesign { it.copy(colorMode = PrimeColorMode.CUSTOM_GRADIENT, secondaryHue = h) }
                                                    }
                                                    StudioSlider(if (es) "Saturación Secundaria" else "Secondary Saturation", design.secondarySaturation, 0.25f..1.0f, "${(design.secondarySaturation * 100).roundToInt()}%") { s ->
                                                        onUpdateDesign { it.copy(colorMode = PrimeColorMode.CUSTOM_GRADIENT, secondarySaturation = s) }
                                                    }
                                                }
                                            }
                                            2 -> {
                                                StudioSwitch(if (es) "Mostrar Rueda Flow Principal" else "Show Flow Hero Wheel", design.showCroixFlowHero) { v ->
                                                    onUpdateDesign { it.copy(showCroixFlowHero = v) }
                                                }
                                                StudioSwitch(if (es) "Mostrar Estados de Ánimo Flow" else "Show Flow Moods", design.showMoodChips) { v ->
                                                    onUpdateDesign { it.copy(showMoodChips = v) }
                                                }
                                                StudioSwitch(if (es) "Mostrar Escuchado Recientemente" else "Show Recently Played", design.showQuickResumeGrid) { v ->
                                                    onUpdateDesign { it.copy(showQuickResumeGrid = v) }
                                                }
                                                StudioSwitch(if (es) "Mostrar Mixes Diarios (Hecho para ti)" else "Show Daily Mixes", design.showDailyMixes) { v ->
                                                    onUpdateDesign { it.copy(showDailyMixes = v) }
                                                }
                                                StudioSwitch(if (es) "Mostrar Selección Rápida" else "Show Quick Picks", design.showQuickPicks) { v ->
                                                    onUpdateDesign { it.copy(showQuickPicks = v) }
                                                }
                                                StudioSwitch(if (es) "Mostrar Artistas para ti" else "Show Artists Row", design.showFavoriteArtistsRow) { v ->
                                                    onUpdateDesign { it.copy(showFavoriteArtistsRow = v) }
                                                }
                                                StudioEnumRow(if (es) "Densidad de Tarjetas" else "Card Density", PrimeCardDensity.entries, design.cardDensity, { it.label }) { d ->
                                                    onUpdateDesign { it.copy(cardDensity = d) }
                                                }
                                                StudioEnumRow(if (es) "Estilo de Barra Inferior" else "Nav Bar Style", PrimeNavBarStyle.entries, design.navBarStyle, { it.label }) { n ->
                                                    onUpdateDesign { it.copy(navBarStyle = n) }
                                                }
                                                StudioSwitch(if (es) "Mostrar Etiquetas de Navegación" else "Show Nav Labels", design.showNavLabels) { v ->
                                                    onUpdateDesign { it.copy(showNavLabels = v) }
                                                }
                                                StudioSwitch(if (es) "Mini-Reproductor Flotante" else "Floating Mini-Player", design.miniPlayerFloating) { v ->
                                                    onUpdateDesign { it.copy(miniPlayerFloating = v) }
                                                }
                                                StudioSlider(if (es) "Altura del Mini-Reproductor" else "Mini-Player Height", design.miniPlayerHeightDp, 56f..84f, "${design.miniPlayerHeightDp.roundToInt()} dp") { v ->
                                                    onUpdateDesign { it.copy(miniPlayerHeightDp = v) }
                                                }
                                            }
                                            3 -> {
                                                StudioEnumRow(if (es) "Diseño del Reproductor" else "Player Layout", PrimePlayerStyle.entries, design.playerStyle, { it.label }) { s ->
                                                    onUpdateDesign { it.copy(playerStyle = s) }
                                                }
                                                StudioEnumRow(if (es) "Barra de Progreso" else "Progress Bar", PrimeProgressStyle.entries, design.progressStyle, { it.label }) { p ->
                                                    onUpdateDesign { it.copy(progressStyle = p) }
                                                }
                                                StudioEnumRow(if (es) "Visualizador en Vivo" else "Live Visualizer", PrimeVisualizerStyle.entries, design.visualizerStyle, { it.label }) { v ->
                                                    onUpdateDesign { it.copy(visualizerStyle = v) }
                                                }
                                                StudioSlider(if (es) "Redondeo de Carátula" else "Artwork Corner", design.artworkCornerDp, 0f..36f, "${design.artworkCornerDp.roundToInt()} dp") { v ->
                                                    onUpdateDesign { it.copy(artworkCornerDp = v) }
                                                }
                                                StudioSwitch(if (es) "Efecto Vinilo Giratorio" else "Vinyl Spin Artwork", design.vinylSpinEnabled) { v ->
                                                    onUpdateDesign { it.copy(vinylSpinEnabled = v) }
                                                }
                                                StudioSwitch(if (es) "Fondo Canvas Animado en Reproductor" else "Animated Canvas Backdrop", design.canvasMotionEnabled) { v ->
                                                    onUpdateDesign { it.copy(canvasMotionEnabled = v) }
                                                }
                                                StudioSwitch(if (es) "Carátula 3D Parallax (Giroscopio)" else "3D Gyroscope Artwork Parallax", design.artworkParallax3d) { v ->
                                                    onUpdateDesign { it.copy(artworkParallax3d = v) }
                                                }
                                                StudioSlider(if (es) "Opacidad del Panel de Controles" else "Control Dock Opacity", design.playerControlDockOpacity, 0.05f..0.80f, "${(design.playerControlDockOpacity * 100).roundToInt()}%") { v ->
                                                    onUpdateDesign { it.copy(playerControlDockOpacity = v) }
                                                }
                                                Text(if (es) "Ícono de la Aplicación (Launcher)" else "App Launcher Icon", color = palette.primary, fontWeight = FontWeight.Bold)
                                                Row(
                                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                ) {
                                                    AppIconManager.options.forEach { opt ->
                                                        val selected = design.appIconId == opt.id
                                                        CroixGlassCard(
                                                            cornerRadius = 16.dp,
                                                            accentTint = if (selected) Color(opt.primaryHex) else null,
                                                            onClick = { onSelectAppIcon(opt.id) },
                                                        ) {
                                                            Column(
                                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(38.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Brush.linearGradient(listOf(Color(opt.primaryHex), Color(opt.secondaryHex)))),
                                                                    contentAlignment = Alignment.Center,
                                                                ) {
                                                                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                                                }
                                                                Text(if (es) opt.labelEs else opt.labelEn, color = Color.White, style = MaterialTheme.typography.labelSmall)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            4 -> {
                                                StudioEnumRow(
                                                    title = strings.transitionStyleLabel,
                                                    entries = PrimeTransitionStyle.entries,
                                                    selected = design.transitionStyle,
                                                    labelOf = { if (es) it.labelEs else it.labelEn },
                                                ) { ts -> onUpdateDesign { it.copy(transitionStyle = ts) } }
                                                if (design.transitionStyle != PrimeTransitionStyle.NONE) {
                                                    StudioSlider(strings.transitionSpeedLabel, design.transitionSpeed, 0.5f..2.0f, "${"%.1f".format(design.transitionSpeed)}x") { v ->
                                                        onUpdateDesign { it.copy(transitionSpeed = v) }
                                                    }
                                                }
                                            }
                                            5 -> {
                                                Text(if (es) "Temas Predefinidos (8)" else "Theme Presets (8)", color = palette.primary, fontWeight = FontWeight.Bold)
                                                PrimePresets.All.forEach { preset ->
                                                    CroixGlassCard(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        cornerRadius = 14.dp,
                                                        accentTint = if (design.presetName == preset.presetName) palette.primary else null,
                                                        onClick = { onApplyPreset(preset) },
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically,
                                                        ) {
                                                            Text(preset.presetName, color = Color.White, fontWeight = FontWeight.SemiBold)
                                                            if (design.presetName == preset.presetName) {
                                                                Text(if (es) "Activo" else "Active", color = palette.primary, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                    Button(
                                                        onClick = {
                                                            val exported = CroixThemeJson.export(design)
                                                            clipboard.setText(AnnotatedString(exported))
                                                            val send = Intent(Intent.ACTION_SEND).apply {
                                                                type = "text/plain"
                                                                putExtra(Intent.EXTRA_SUBJECT, "Tema Croix Player Prime")
                                                                putExtra(Intent.EXTRA_TEXT, exported)
                                                            }
                                                            context.startActivity(Intent.createChooser(send, "Compartir .croixtheme.json"))
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                    ) {
                                                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(if (es) "Compartir" else "Share")
                                                    }
                                                    OutlinedButton(
                                                        onClick = { importJson = ""; importErr = null; showImport = true },
                                                        modifier = Modifier.weight(1f),
                                                    ) { Text(if (es) "Importar JSON" else "Import JSON") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================
                        // SUBMENU: LYRICS & VISUALIZER
                        // =========================================================
                        SettingsPage.LYRICS_UI -> {
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        StudioEnumRow(if (es) "Alineación de Letras" else "Lyrics Alignment", PrimeLyricsAlign.entries, design.lyricsAlign, { it.label }) { a ->
                                            onUpdateDesign { it.copy(lyricsAlign = a) }
                                        }
                                        StudioSlider(if (es) "Tamaño de Letras" else "Lyrics Font Size", design.lyricsFontSizeSp, 16f..36f, "${design.lyricsFontSizeSp.roundToInt()} sp") { v ->
                                            onUpdateDesign { it.copy(lyricsFontSizeSp = v) }
                                        }
                                        StudioSlider(if (es) "Desenfoque de Fondo en Letras" else "Lyrics Backdrop Blur", design.lyricsBackdropBlurDp, 0f..34f, "${design.lyricsBackdropBlurDp.roundToInt()} dp") { v ->
                                            onUpdateDesign { it.copy(lyricsBackdropBlurDp = v) }
                                        }
                                        StudioSlider(
                                            label = if (es) "Ajuste de Sincronización Global" else "Global Sync Offset",
                                            value = design.lyricsOffsetMs.toFloat(),
                                            range = -3000f..3000f,
                                            valueLabel = "${"%.1f".format(design.lyricsOffsetMs / 1000f)}s",
                                        ) { v -> onUpdateDesign { it.copy(lyricsOffsetMs = (v / 100f).roundToInt() * 100L) } }
                                        StudioSwitch(if (es) "Traducir letras al Español automáticamente" else "Auto-translate lyrics to Spanish", design.lyricsAutoTranslateEs) { v ->
                                            onUpdateDesign { it.copy(lyricsAutoTranslateEs = v) }
                                        }
                                        StudioSwitch(if (es) "Romanizar Coreano/Japonés/Ruso automáticamente" else "Auto-romanize non-Latin scripts", design.lyricsAutoRomanize) { v ->
                                            onUpdateDesign { it.copy(lyricsAutoRomanize = v) }
                                        }
                                        StudioEnumRow(if (es) "Visualizador de Audio en Vivo" else "Live Visualizer", PrimeVisualizerStyle.entries, design.visualizerStyle, { it.label }) { v ->
                                            onUpdateDesign { it.copy(visualizerStyle = v) }
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================
                        // SUBMENU: AUDIO ENGINE & PLAYBACK
                        // =========================================================
                        SettingsPage.AUDIO_ENGINE -> {
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(if (es) "Calidad y Motor de Streaming" else "Streaming Quality & Engine", color = palette.primary, fontWeight = FontWeight.Bold)
                                        StudioEnumRow(
                                            title = if (es) "Calidad de Audio" else "Audio Quality",
                                            entries = PrimeAudioQuality.entries,
                                            selected = design.audioQuality,
                                            labelOf = { if (es) it.labelEs else it.labelEn },
                                        ) { q -> onUpdateDesign { it.copy(audioQuality = q) } }

                                        StudioSwitch(if (es) "Normalización de Volumen (Loudness)" else "Audio Loudness Normalization", design.normalizeAudio) { v ->
                                            onUpdateDesign { it.copy(normalizeAudio = v) }
                                        }
                                        StudioSwitch(if (es) "Autoplay Inteligente (Cola Infinita)" else "Smart Autoplay Queue", design.autoPlayRelated) { v ->
                                            onUpdateDesign { it.copy(autoPlayRelated = v) }
                                        }
                                        StudioSwitch(if (es) "Saltar Silencios" else "Skip Silence", playback.skipSilenceEnabled, onSetSkipSilence)

                                        StudioSlider(
                                            label = strings.crossfadeLabel,
                                            value = design.crossfadeSeconds.toFloat(),
                                            range = 0f..10f,
                                            valueLabel = if (design.crossfadeSeconds == 0) strings.crossfadeOff else "${design.crossfadeSeconds}s",
                                        ) { v -> onUpdateDesign { it.copy(crossfadeSeconds = v.roundToInt()) } }

                                        StudioSlider(if (es) "Velocidad de Reproducción" else "Playback Speed", playback.playbackSpeed, 0.5f..2.0f, "${"%.2f".format(playback.playbackSpeed)}x", onSetSpeed)
                                        StudioSlider(if (es) "Tono (Pitch)" else "Playback Pitch", playback.playbackPitch, 0.5f..2.0f, "${"%.2f".format(playback.playbackPitch)}x", onSetPitch)
                                    }
                                }
                            }
                        }

                        // =========================================================
                        // SUBMENU: 8D SPATIAL, REVERB, AUTOEQ, OUTPUT PROFILES & 5-BAND EQ
                        // =========================================================
                        SettingsPage.EQUALIZER -> {
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = palette.primary) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(if (es) "Audio Espacial 8D y Reverb" else "8D Spatial Audio & Reverb", color = palette.primary, fontWeight = FontWeight.Bold)
                                        StudioSwitch(if (es) "Activar Audio Espacial 8D (Estéreo Orbital)" else "Enable 8D Spatial Audio", playback.audio8dEnabled, onSetAudio8dEnabled)
                                        if (playback.audio8dEnabled) {
                                            StudioSlider(if (es) "Velocidad de Órbita 8D" else "8D Orbit Speed", playback.audio8dSpeed, 0.1f..1.0f, "${(playback.audio8dSpeed * 100).roundToInt()}%", onSetAudio8dSpeed)
                                        }
                                        StudioEnumRow(if (es) "Reverb / Sala Acústica" else "Reverb Room", ReverbPresetMode.entries, playback.reverbMode, { it.label }, onSetReverbMode)
                                        StudioEnumRow(if (es) "AutoEq para Audífonos" else "AutoEq Headphone Profile", AutoEqHeadphoneProfile.entries, playback.autoEqProfile, { it.label }, onApplyAutoEq)
                                    }
                                }
                            }
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Rounded.Headphones, null, tint = palette.secondary, modifier = Modifier.size(18.dp))
                                            Text(if (es) "Perfiles Automáticos según Salida" else "Auto Profiles by Output Route", color = palette.secondary, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            if (es) "Salida actual: ${playback.activeOutput.route.labelEs} (${playback.activeOutput.deviceName})" else "Current: ${playback.activeOutput.deviceName}",
                                            color = palette.textSecondary,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        StudioSwitch(if (es) "Cambiar AutoEq automáticamente al conectar audífonos" else "Auto-switch AutoEq on output change", design.autoOutputProfilesEnabled) { v ->
                                            onUpdateDesign { it.copy(autoOutputProfilesEnabled = v) }
                                        }
                                        if (design.autoOutputProfilesEnabled) {
                                            val btSel = AutoEqHeadphoneProfile.entries.firstOrNull { it.name == design.bluetoothAutoEqPreset } ?: AutoEqHeadphoneProfile.SONY_XM
                                            val wiredSel = AutoEqHeadphoneProfile.entries.firstOrNull { it.name == design.wiredAutoEqPreset } ?: AutoEqHeadphoneProfile.STUDIO_IEM
                                            val spkSel = AutoEqHeadphoneProfile.entries.firstOrNull { it.name == design.speakerAutoEqPreset } ?: AutoEqHeadphoneProfile.FLAT
                                            StudioEnumRow("Bluetooth", AutoEqHeadphoneProfile.entries, btSel, { it.label }) { p ->
                                                onUpdateDesign { it.copy(bluetoothAutoEqPreset = p.name) }
                                            }
                                            StudioEnumRow(if (es) "Audífonos Cable / USB" else "Wired / USB", AutoEqHeadphoneProfile.entries, wiredSel, { it.label }) { p ->
                                                onUpdateDesign { it.copy(wiredAutoEqPreset = p.name) }
                                            }
                                            StudioEnumRow(if (es) "Altavoz del teléfono" else "Phone Speaker", AutoEqHeadphoneProfile.entries, spkSel, { it.label }) { p ->
                                                onUpdateDesign { it.copy(speakerAutoEqPreset = p.name) }
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(if (es) "Ecualizador Hardware de 5 Bandas" else "5-Band Hardware Equalizer", color = palette.primary, fontWeight = FontWeight.Bold)
                                        StudioSwitch(if (es) "Activar Ecualizador" else "Enable Equalizer", playback.eqEnabled, onSetEqEnabled)
                                        StudioEnumRow(if (es) "Perfil de Sonido" else "Preset", EqPreset.entries.filter { it != EqPreset.CUSTOM }, playback.eqPreset, { it.label }, onApplyEqPreset)
                                        val bands = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
                                        playback.eqBandsDb.forEachIndexed { idx, db ->
                                            StudioSlider(bands.getOrElse(idx) { "Banda ${idx + 1}" }, db, -12f..12f, "${db.roundToInt()} dB") { v ->
                                                onSetEqBand(idx, v)
                                            }
                                        }
                                        StudioSlider(if (es) "Refuerzo de Graves (Bass Boost)" else "Bass Boost", playback.bassBoostLevel, 0f..1f, "${(playback.bassBoostLevel * 100).roundToInt()}%", onSetBassBoost)
                                        StudioSlider(if (es) "Ganancia de Volumen" else "Loudness Gain", playback.loudnessGainMb.toFloat(), 0f..1200f, "+${playback.loudnessGainMb / 100} dB") { v ->
                                            onSetLoudness(v.roundToInt())
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================
                        // SUBMENU: SMART AUDIO CACHE ([4])
                        // =========================================================
                        SettingsPage.CACHE_STORAGE -> {
                            if (serverNotice != null) {
                                item {
                                    CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFF00E676)) {
                                        Text(serverNotice, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(14.dp))
                                    }
                                }
                            }
                            item {
                                val usedMb = audioCacheBytes / (1024f * 1024f)
                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFFFFB300)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(if (es) "Caché Inteligente LRU de Audio" else "Smart Audio LRU Cache", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (es) "Guarda automáticamente las canciones que escuchas para reproducirlas al instante y ahorrar datos móviles." else "Automatically caches streamed tracks for instant replay and lower mobile data use.",
                                            color = palette.textSecondary,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        Text(
                                            text = if (design.audioCacheLimitMb == 0) {
                                                "Caché desactivada (${"%.1f".format(usedMb)} MB actuales)"
                                            } else {
                                                "${"%.1f".format(usedMb)} MB usados de ${design.audioCacheLimitMb} MB"
                                            },
                                            color = Color(0xFFFFD54F),
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(if (es) "Límite máximo de caché" else "Maximum cache size", color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(0 to "Off", 256 to "256 MB", 512 to "512 MB", 1024 to "1 GB", 2048 to "2 GB").forEach { (mb, lbl) ->
                                                CroixGlassCard(
                                                    cornerRadius = 12.dp,
                                                    accentTint = if (design.audioCacheLimitMb == mb) palette.primary else null,
                                                    onClick = { onUpdateDesign { it.copy(audioCacheLimitMb = mb) } },
                                                ) {
                                                    Text(lbl, color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                                                }
                                            }
                                        }
                                        Button(
                                            onClick = onClearAudioCache,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Icon(Icons.Rounded.DeleteSweep, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(if (es) "Vaciar Caché de Audio" else "Clear Audio Cache")
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================
                        // SUBMENU: DISCORD RICH PRESENCE + LAST.FM + LISTENBRAINZ ([7])
                        // =========================================================
                        SettingsPage.INTEGRATIONS -> {
                            val integ = serverState.integrations
                            if (serverNotice != null) {
                                item {
                                    CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFF5865F2)) {
                                        Text(serverNotice, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(14.dp))
                                    }
                                }
                            }
                            item {
                                var discordTokenDraft by remember(integ.discordToken) { mutableStateOf(integ.discordToken) }
                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFF5865F2)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Discord Rich Presence", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (es) "Muestra «Escuchando Croix Player Prime» en tiempo real en tu perfil de Discord (se pausa en Sesión Privada)." else "Broadcasts live Listening status to your Discord profile.",
                                            color = palette.textSecondary,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        StudioSwitch(if (es) "Activar Discord Rich Presence" else "Enable Discord RPC", integ.discordEnabled) { en ->
                                            onUpdateIntegrations { it.copy(discordEnabled = en) }
                                        }
                                        OutlinedTextField(
                                            value = discordTokenDraft,
                                            onValueChange = { discordTokenDraft = it },
                                            label = { Text("Token de usuario de Discord") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Button(
                                            onClick = {
                                                onUpdateIntegrations { it.copy(discordToken = discordTokenDraft.trim(), discordEnabled = discordTokenDraft.isNotBlank()) }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) { Text(if (es) "Guardar Token Discord" else "Save Discord Token") }
                                    }
                                }
                            }
                            item {
                                var lfmKey by remember(integ.lastFmApiKey) { mutableStateOf(integ.lastFmApiKey) }
                                var lfmSecret by remember(integ.lastFmApiSecret) { mutableStateOf(integ.lastFmApiSecret) }
                                var lfmUser by remember(integ.lastFmUsername) { mutableStateOf(integ.lastFmUsername) }
                                var lfmPass by remember { mutableStateOf("") }
                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFFD51007)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Last.fm Scrobbling", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        if (integ.isLastFmReady) {
                                            Text("✓ Conectado como ${integ.lastFmUsername}", color = Color(0xFF00E676), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                        StudioSwitch(if (es) "Activar Scrobbling en Last.fm" else "Enable Last.fm", integ.lastFmEnabled) { en ->
                                            onUpdateIntegrations { it.copy(lastFmEnabled = en) }
                                        }
                                        OutlinedTextField(value = lfmKey, onValueChange = { lfmKey = it }, label = { Text("Last.fm API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = lfmSecret, onValueChange = { lfmSecret = it }, label = { Text("Last.fm API Secret") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = lfmUser, onValueChange = { lfmUser = it }, label = { Text("Usuario Last.fm") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = lfmPass, onValueChange = { lfmPass = it }, label = { Text("Contraseña Last.fm") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                        Button(
                                            enabled = lfmKey.isNotBlank() && lfmSecret.isNotBlank() && lfmUser.isNotBlank() && lfmPass.isNotBlank(),
                                            onClick = { onLoginLastFm(lfmKey, lfmSecret, lfmUser, lfmPass) },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) { Text(if (es) "Iniciar sesión en Last.fm" else "Sign in to Last.fm") }
                                    }
                                }
                            }
                            item {
                                var lbToken by remember(integ.listenBrainzToken) { mutableStateOf(integ.listenBrainzToken) }
                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFFEB743B)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("ListenBrainz Scrobbling", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        StudioSwitch(if (es) "Activar ListenBrainz" else "Enable ListenBrainz", integ.listenBrainzEnabled) { en ->
                                            onUpdateIntegrations { it.copy(listenBrainzEnabled = en) }
                                        }
                                        OutlinedTextField(value = lbToken, onValueChange = { lbToken = it }, label = { Text("Token de usuario ListenBrainz") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                        Button(
                                            onClick = {
                                                onUpdateIntegrations { it.copy(listenBrainzToken = lbToken.trim(), listenBrainzEnabled = lbToken.isNotBlank()) }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) { Text(if (es) "Guardar Token ListenBrainz" else "Save ListenBrainz Token") }
                                    }
                                }
                            }
                        }

                        // =========================================================
                        // SUBMENU: PRIVATE SESSION (INCOGNITO) & BLOCKED ARTISTS ([Y], [H])
                        // =========================================================
                        SettingsPage.PRIVACY_BLOCKED -> {
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFFCE93D8)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(if (es) "Modo Sesión Privada (Incógnito)" else "Private Session (Incognito)", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (es) "Cuando está activo, nada de lo que escuches se guarda en tu historial, estadísticas, algoritmo ni se envía a Discord / Last.fm / ListenBrainz." else "Pauses history, stats, algorithm updates, Discord RPC & scrobbling.",
                                            color = palette.textSecondary,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        StudioSwitch(if (es) "Activar Sesión Privada" else "Enable Private Session", design.privateSessionEnabled) { en ->
                                            onUpdateDesign { it.copy(privateSessionEnabled = en) }
                                        }
                                    }
                                }
                            }
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Rounded.Block, null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                                            Text(if (es) "Artistas Bloqueados (${blockedArtists.size})" else "Blocked Artists (${blockedArtists.size})", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        if (blockedArtists.isEmpty()) {
                                            Text(
                                                if (es) "No tienes artistas bloqueados. Puedes bloquear un artista desde el menú ⋮ de cualquier canción o desde su perfil." else "No blocked artists yet.",
                                                color = palette.textSecondary,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        } else {
                                            blockedArtists.forEach { b ->
                                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text(b.displayName, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                                        TextButton(onClick = { onUnblockArtist(b.artistKey) }) {
                                                            Text(if (es) "Desbloquear" else "Unblock")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================
                        // SUBMENU: GESTURES & HARDWARE
                        // =========================================================
                        SettingsPage.GESTURES -> {
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        StudioSwitch(if (es) "Deslizar carátula para cambiar canción" else "Swipe Artwork to Skip", design.swipeArtworkToSkip) { v ->
                                            onUpdateDesign { it.copy(swipeArtworkToSkip = v) }
                                        }
                                        StudioSlider(if (es) "Salto con doble toque" else "Double-Tap Seek Step", design.doubleTapSeekSeconds.toFloat(), 5f..30f, "${design.doubleTapSeekSeconds}s") { v ->
                                            onUpdateDesign { it.copy(doubleTapSeekSeconds = v.roundToInt()) }
                                        }
                                        StudioSwitch(if (es) "Agitar celular para saltar canción" else "Shake Device to Skip", design.shakeToSkipEnabled) { v ->
                                            onUpdateDesign { it.copy(shakeToSkipEnabled = v) }
                                        }
                                        if (design.shakeToSkipEnabled) {
                                            StudioSlider(if (es) "Sensibilidad al agitar" else "Shake Sensitivity", design.shakeSensitivity, 0.1f..1.0f, "${(design.shakeSensitivity * 100).roundToInt()}%") { v ->
                                                onUpdateDesign { it.copy(shakeSensitivity = v) }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================
                        // SUBMENU: SPONSORBLOCK
                        // =========================================================
                        SettingsPage.SPONSORBLOCK -> {
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        StudioSwitch(if (es) "Activar SponsorBlock automático" else "Enable SponsorBlock Auto-Skip", design.sponsorBlockEnabled) { v ->
                                            onUpdateDesign { it.copy(sponsorBlockEnabled = v) }
                                        }
                                        StudioSwitch(if (es) "Saltar secciones sin música" else "Skip Non-Music Sections", design.skipMusicOfftopic) { v ->
                                            onUpdateDesign { it.copy(skipMusicOfftopic = v) }
                                        }
                                        StudioSwitch(if (es) "Saltar patrocinios y autopromoción" else "Skip Sponsors & Self-Promo", design.skipSponsors) { v ->
                                            onUpdateDesign { it.copy(skipSponsors = v) }
                                        }
                                        StudioSwitch(if (es) "Saltar intros y outros" else "Skip Intros & Outros", design.skipIntroOutro) { v ->
                                            onUpdateDesign { it.copy(skipIntroOutro = v) }
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================
                        // SUBMENU: UPDATES (PERMANENT KEYSTORE IN-PLACE UPDATES)
                        // =========================================================
                        SettingsPage.UPDATES -> {
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFF00E676)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            if (es) "Versión instalada: v${BuildConfig.VERSION_NAME}" else "Installed: v${BuildConfig.VERSION_NAME}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            text = when {
                                                updateInfo.checking -> if (es) "Buscando actualizaciones en GitHub..." else "Checking GitHub Releases..."
                                                updateInfo.error != null -> "Error: ${updateInfo.error}"
                                                updateInfo.hasUpdate -> if (es) "Nueva versión lista: ${updateInfo.latestLabel}" else "New version ready: ${updateInfo.latestLabel}"
                                                updateInfo.checked -> if (es) "Tienes la última versión instalada." else "You are on the latest version."
                                                else -> if (es) "Toca el botón para verificar si hay una versión más reciente." else "Tap below to check for updates."
                                            },
                                            color = if (updateInfo.hasUpdate) Color(0xFF00E676) else palette.textSecondary,
                                        )
                                        if (updateInfo.releaseNotes.isNotBlank()) {
                                            Text(updateInfo.releaseNotes, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick = onCheckUpdates,
                                                enabled = !updateInfo.checking,
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Text(if (es) "Buscar ahora" else "Check Now")
                                            }
                                            Button(
                                                onClick = { openExternalUrl(updateInfo.downloadUrl) },
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(if (es) "Descargar APK" else "Download APK")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================
                        // SUBMENU: ABOUT
                        // =========================================================
                        SettingsPage.ABOUT -> {
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Croix Player Prime", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                                        Text("v${BuildConfig.VERSION_NAME} (Código ${BuildConfig.VERSION_CODE})", color = palette.primary, fontWeight = FontWeight.Bold)
                                        Text(
                                            if (es) {
                                                "Reproductor musical de alto rendimiento para Android 12+ (ARMv8 / ARMv9) con motor multi-cliente sin anuncios, precarga instantánea, algoritmo híbrido con Spotify, estética Glassmorphism a 120fps y firma digital permanente para actualizaciones directas."
                                            } else {
                                                "High-performance Android 12+ (ARMv8/ARMv9) music player with multi-client ad-free engine, lookahead prefetch, Spotify hybrid algorithm, 120fps Glassmorphism and permanent update signing."
                                            },
                                            color = palette.textSecondary,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        OutlinedButton(
                                            onClick = { openExternalUrl("https://github.com/SmithdelaCroix/CroixPlayerPrime") },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) { Text(if (es) "Ver repositorio en GitHub (GPL-3.0)" else "View GitHub Repository (GPL-3.0)") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualSpDc) {
        AlertDialog(
            onDismissRequest = { showManualSpDc = false },
            title = { Text("Cookie sp_dc de Spotify") },
            text = {
                OutlinedTextField(
                    value = manualSpDc,
                    onValueChange = { manualSpDc = it },
                    label = { Text(if (es) "Pega tu valor sp_dc" else "Paste sp_dc value") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    enabled = manualSpDc.isNotBlank(),
                    onClick = {
                        onConnectSpotifyManual(manualSpDc.trim())
                        showManualSpDc = false
                    },
                ) { Text(strings.connectSpotify) }
            },
            dismissButton = { TextButton(onClick = { showManualSpDc = false }) { Text(strings.cancel) } },
        )
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text(if (es) "Importar .croixtheme.json" else "Import .croixtheme.json") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = importJson,
                        onValueChange = { importJson = it },
                        label = { Text(if (es) "Pega el JSON del tema" else "Paste Theme JSON") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (importErr != null) Text(importErr!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(onClick = {
                    CroixThemeJson.import(importJson)
                        .onSuccess { onApplyPreset(it); showImport = false }
                        .onFailure { importErr = it.message ?: "JSON inválido" }
                }) { Text(if (es) "Aplicar" else "Apply") }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text(strings.cancel) } },
        )
    }
}

@Composable
private fun SegmentedSettingsGroup(
    header: String,
    items: List<SegmentedEntry>,
) {
    val palette = LocalCroixPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = header,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
            color = palette.primary.copy(alpha = 0.92f),
            modifier = Modifier.padding(start = 10.dp, bottom = 5.dp),
        )
        items.forEachIndexed { idx, entry ->
            val topR = if (idx == 0) 22.dp else 6.dp
            val botR = if (idx == items.lastIndex) 22.dp else 6.dp
            val shape = RoundedCornerShape(topStart = topR, topEnd = topR, bottomStart = botR, bottomEnd = botR)
            SegmentedSettingsRow(entry = entry, shape = shape)
        }
    }
}

@Composable
private fun SegmentedSettingsRow(
    entry: SegmentedEntry,
    shape: RoundedCornerShape,
) {
    val palette = LocalCroixPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "segScale",
    )

    CroixGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null, onClick = entry.onClick),
        shapeOverride = shape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(entry.accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(entry.icon, contentDescription = null, tint = entry.accent, modifier = Modifier.size(22.dp))
                if (entry.highlightDot) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5252)),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.subtitle,
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(entry.accent.copy(alpha = 0.22f))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = entry.badge,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textSecondary.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun StudioSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onChange: (Float) -> Unit,
) {
    val palette = LocalCroixPalette.current
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Text(valueLabel, color = palette.primary, style = MaterialTheme.typography.labelMedium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun StudioSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T : Enum<T>> StudioEnumRow(
    title: String,
    entries: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val palette = LocalCroixPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (title.isNotBlank()) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            entries.forEach { item ->
                CroixGlassCard(
                    cornerRadius = 12.dp,
                    accentTint = if (item == selected) palette.primary else null,
                    onClick = { onSelect(item) },
                ) {
                    Text(
                        text = labelOf(item),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
