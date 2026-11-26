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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
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
import com.croix.prime.design.PrimeArtworkShape
import com.croix.prime.design.PrimeAudioQuality
import com.croix.prime.design.PrimeCardDensity
import com.croix.prime.design.PrimeColorMode
import com.croix.prime.design.PrimeColorSwatches
import com.croix.prime.design.PrimeFontStyle
import com.croix.prime.design.PrimeLanguageMode
import com.croix.prime.design.PrimeLyricsAlign
import com.croix.prime.design.PrimeLyricsAnimStyle
import com.croix.prime.design.PrimeMiniPlayerStyle
import com.croix.prime.design.PrimeNavBarStyle
import com.croix.prime.design.PrimePlayerBackgroundStyle
import com.croix.prime.design.PrimePlayerButtonStyle
import com.croix.prime.design.PrimePlayerStyle
import com.croix.prime.design.PrimePresets
import com.croix.prime.design.PrimeProgressStyle
import com.croix.prime.design.PrimeStartTab
import com.croix.prime.design.PrimeTransitionStyle
import com.croix.prime.design.PrimeVisualizerStyle
import com.croix.prime.design.resolveArtworkShape
import com.croix.prime.engine.AiApiConfig
import com.croix.prime.engine.AiProviderMode
import com.croix.prime.engine.ExternalIntegrationsConfig
import com.croix.prime.engine.SavedYtMusicAccount
import com.croix.prime.engine.ServerConnectionConfig
import com.croix.prime.engine.ServerHealthStatus
import com.croix.prime.engine.ServerPresets
import com.croix.prime.engine.SpotifyPlaylistSummary
import com.croix.prime.engine.YtMusicAccountProfile
import com.croix.prime.engine.YtMusicPlaylistSummary
import com.croix.prime.engine.YtMusicServerMode
import com.croix.prime.playback.AutoEqHeadphoneProfile
import com.croix.prime.playback.EqPreset
import com.croix.prime.playback.PlaybackUiState
import com.croix.prime.playback.ReverbPresetMode
import kotlin.math.roundToInt

private enum class SettingsPage {
    HOME,
    ACCOUNT,
    SERVERS,
    SPOTIFY,
    AI_INTEGRATION,
    INTEGRATIONS,
    PRIVACY_BLOCKED,
    STUDIO,
    LYRICS_UI,
    AUDIO_ENGINE,
    EQUALIZER,
    CACHE_STORAGE,
    BETA_LABS,
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
    ytMusicSyncing: Boolean,
    serverNotice: String?,
    updateInfo: AppUpdateInfo,
    audioCacheBytes: Long,
    blockedArtists: List<BlockedArtistEntity>,
    onClose: () -> Unit,
    onUpdateDesign: ((CroixDesignState) -> CroixDesignState) -> Unit,
    onApplyPreset: (CroixDesignState) -> Unit,
    onResetDesign: () -> Unit,
    onSelectAppIcon: (String) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetPitch: (Float) -> Unit,
    onSetSkipSilence: (Boolean) -> Unit,
    onSetKaraokeEnabled: (Boolean) -> Unit,
    onSetKaraokeStrength: (Float) -> Unit,
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
    onUpdateAiConfig: ((AiApiConfig) -> AiApiConfig) -> Unit,
    onTestAiConnection: () -> Unit,
    onUpdateServerConfig: ((ServerConnectionConfig) -> ServerConnectionConfig) -> Unit,
    onPingServer: () -> Unit,
    onOpenYtMusicLogin: () -> Unit,
    onOpenAddYtMusicAccount: () -> Unit,
    onSyncYtMusicNow: () -> Unit,
    onRefreshYtMusicProfiles: () -> Unit,
    onSwitchYtMusicProfile: (YtMusicAccountProfile) -> Unit,
    onSwitchSavedYtAccount: (SavedYtMusicAccount) -> Unit,
    onRemoveSavedYtAccount: (String) -> Unit,
    onImportYtMusicPlaylist: (YtMusicPlaylistSummary) -> Unit,
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
                        SettingsPage.ACCOUNT -> if (es) "Cuenta" else "Account"
                        SettingsPage.SERVERS -> if (es) "Servidores" else "Servers"
                        SettingsPage.SPOTIFY -> "Spotify"
                        SettingsPage.AI_INTEGRATION -> if (es) "Integración IA" else "AI"
                        SettingsPage.INTEGRATIONS -> if (es) "Conexiones" else "Connections"
                        SettingsPage.PRIVACY_BLOCKED -> if (es) "Privacidad" else "Privacy"
                        SettingsPage.STUDIO -> if (es) "Apariencia" else "Appearance"
                        SettingsPage.LYRICS_UI -> if (es) "Letras" else "Lyrics"
                        SettingsPage.AUDIO_ENGINE -> if (es) "Reproductor" else "Player"
                        SettingsPage.EQUALIZER -> if (es) "Ecualizador" else "Equalizer"
                        SettingsPage.CACHE_STORAGE -> if (es) "Almacenamiento" else "Storage"
                        SettingsPage.BETA_LABS -> if (es) "Funciones BETA" else "BETA Labs"
                        SettingsPage.GESTURES -> if (es) "Gestos" else "Gestures"
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

                            // Gaussian-Blurred Account Hero Card at top of Settings!
                            item {
                                val cfg = serverState.serverConfig
                                val avatar = cfg.ytMusicAccountAvatarUrl
                                CroixGlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 24.dp,
                                    accentTint = palette.primary,
                                    onClick = { page = SettingsPage.ACCOUNT },
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        // Rich Gaussian Blur Backdrop from user's own avatar!
                                        if (avatar.isNotBlank()) {
                                            AsyncImage(
                                                model = avatar,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .blur(26.dp),
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(
                                                                palette.primary.copy(alpha = 0.42f),
                                                                palette.secondary.copy(alpha = 0.28f),
                                                                Color.Black.copy(alpha = 0.35f),
                                                            ),
                                                        ),
                                                    ),
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color.Black.copy(alpha = 0.52f), Color.Black.copy(alpha = 0.72f)),
                                                    ),
                                                ),
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(58.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.primary.copy(alpha = 0.28f))
                                                    .border(2.dp, Color.White.copy(alpha = 0.65f), CircleShape),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                if (avatar.isNotBlank()) {
                                                    AsyncImage(
                                                        model = avatar,
                                                        contentDescription = cfg.ytMusicAccountName,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                    )
                                                } else {
                                                    Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(30.dp))
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = if (serverState.isYtMusicConnected) cfg.ytMusicAccountName else (if (es) "Iniciar sesión" else "Sign in"),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    text = when {
                                                        serverState.isYtMusicConnected && cfg.ytMusicAccountHandle.isNotBlank() ->
                                                            "${cfg.ytMusicAccountHandle}${if (serverState.ytMusicBrandProfiles.size > 1) " • ${serverState.ytMusicBrandProfiles.size} perfiles" else ""}"
                                                        serverState.isYtMusicConnected && cfg.ytMusicAccountEmail.isNotBlank() -> cfg.ytMusicAccountEmail
                                                        serverState.isYtMusicConnected -> serverState.ytMusicSyncSummary.ifBlank { "Cuenta conectada" }
                                                        else -> if (es) "Conecta tu cuenta, perfiles y sincronización" else "Connect your account, profiles & sync"
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.84f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White)
                                        }
                                    }
                                }
                            }

                            // Simplified, crystal-clear Settings Groups
                            item {
                                SegmentedSettingsGroup(
                                    header = if (es) "CUENTA Y CONTENIDO" else "ACCOUNT & CONTENT",
                                    items = listOf(
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Person,
                                            title = if (es) "Cuenta" else "Account",
                                            subtitle = if (serverState.isYtMusicConnected) {
                                                serverState.ytMusicSyncSummary.ifBlank { cfgNameOrSync(serverState) }
                                            } else {
                                                if (es) "Iniciar sesión, cambiar cuenta y perfiles" else "Sign in, switch account & profiles"
                                            },
                                            accent = palette.primary,
                                            badge = if (serverState.isYtMusicConnected) "Activa" else null,
                                            onClick = { page = SettingsPage.ACCOUNT },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.AutoAwesome,
                                            title = "Spotify",
                                            subtitle = if (serverState.isSpotifyConnected) {
                                                "${serverState.spotifyTaste.accountName} • ${(serverState.spotifyBlendWeight * 100).roundToInt()}%"
                                            } else {
                                                if (es) "Algoritmo híbrido y playlists" else "Hybrid algorithm & playlists"
                                            },
                                            accent = Color(0xFF1DB954),
                                            badge = if (serverState.isSpotifyConnected) "On" else null,
                                            onClick = { page = SettingsPage.SPOTIFY },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Dns,
                                            title = if (es) "Servidores" else "Servers",
                                            subtitle = buildString {
                                                append(if (es) serverState.serverConfig.mode.labelEs else serverState.serverConfig.mode.labelEn)
                                                if (serverHealth?.reachable == true) append(" • ${serverHealth.latencyMs} ms")
                                            },
                                            accent = Color(0xFF00E5FF),
                                            badge = "$adsBlockedCount+ ads",
                                            onClick = { page = SettingsPage.SERVERS },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Psychology,
                                            title = if (es) "Integración IA" else "AI",
                                            subtitle = if (serverState.aiConfig.isReady) {
                                                "${serverState.aiConfig.provider.label} • ${serverState.aiConfig.effectiveModel}"
                                            } else {
                                                if (es) "Traductor de letras y recomendador" else "Smart lyrics & recommendations"
                                            },
                                            accent = Color(0xFFB388FF),
                                            badge = if (serverState.aiConfig.isReady) "On" else null,
                                            onClick = { page = SettingsPage.AI_INTEGRATION },
                                        ),
                                    ),
                                )
                            }

                            item {
                                SegmentedSettingsGroup(
                                    header = if (es) "APARIENCIA" else "APPEARANCE",
                                    items = listOf(
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Palette,
                                            title = if (es) "Apariencia" else "Appearance",
                                            subtitle = if (es) "Colores, cristal, fuentes, reproductor y temas" else "Colors, glass, fonts, player & themes",
                                            accent = palette.primary,
                                            badge = design.presetName,
                                            onClick = { page = SettingsPage.STUDIO },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Lyrics,
                                            title = if (es) "Letras" else "Lyrics",
                                            subtitle = "${design.lyricsFontSizeSp.roundToInt()} sp • ${design.lyricsAnimStyle.label}",
                                            accent = palette.secondary,
                                            onClick = { page = SettingsPage.LYRICS_UI },
                                        ),
                                    ),
                                )
                            }

                            item {
                                SegmentedSettingsGroup(
                                    header = if (es) "REPRODUCTOR Y AUDIO" else "PLAYER & AUDIO",
                                    items = listOf(
                                        SegmentedEntry(
                                            icon = Icons.Rounded.PlayArrow,
                                            title = if (es) "Reproductor" else "Player",
                                            subtitle = "${if (es) design.audioQuality.labelEs else design.audioQuality.labelEn} • ${"%.2f".format(playback.playbackSpeed)}x",
                                            accent = palette.primary,
                                            onClick = { page = SettingsPage.AUDIO_ENGINE },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Equalizer,
                                            title = if (es) "Ecualizador" else "Equalizer",
                                            subtitle = "${if (playback.audio8dEnabled) "8D On" else "8D Off"} • ${playback.reverbMode.label} • ${playback.activeOutput.route.labelEs}",
                                            accent = palette.secondary,
                                            badge = if (playback.audio8dEnabled) "8D" else null,
                                            onClick = { page = SettingsPage.EQUALIZER },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Storage,
                                            title = if (es) "Almacenamiento" else "Storage",
                                            subtitle = if (design.audioCacheLimitMb == 0) {
                                                if (es) "Caché desactivada" else "Cache disabled"
                                            } else {
                                                "${"%.1f".format(audioCacheBytes / (1024f * 1024f))} MB / ${design.audioCacheLimitMb} MB"
                                            },
                                            accent = Color(0xFFFFB300),
                                            onClick = {
                                                onRefreshCacheSize()
                                                page = SettingsPage.CACHE_STORAGE
                                            },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Shield,
                                            title = "SponsorBlock",
                                            subtitle = if (design.sponsorBlockEnabled) {
                                                if (es) "Activo • Saltando intros y patrocinios" else "Active • Skipping non-music"
                                            } else {
                                                if (es) "Desactivado" else "Off"
                                            },
                                            accent = Color(0xFF00E5FF),
                                            onClick = { page = SettingsPage.SPONSORBLOCK },
                                        ),
                                    ),
                                )
                            }

                            item {
                                val betaCount = listOf(design.artworkParallax3d, design.betaBlindDiscoveryEnabled, design.betaKaraokeEnabled).count { it }
                                SegmentedSettingsGroup(
                                    header = if (es) "SISTEMA Y EXTRAS" else "SYSTEM & EXTRAS",
                                    items = listOf(
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Science,
                                            title = if (es) "Funciones BETA" else "BETA Labs",
                                            subtitle = if (es) "Parallax 3D, Modo Misterio 45s y Karaoke" else "3D Parallax, Blind Mode & Karaoke",
                                            accent = Color(0xFF00E5FF),
                                            badge = if (betaCount > 0) "$betaCount activas" else "BETA",
                                            onClick = { page = SettingsPage.BETA_LABS },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Hub,
                                            title = if (es) "Conexiones" else "Connections",
                                            subtitle = "Discord RPC, Last.fm y ListenBrainz",
                                            accent = Color(0xFF5865F2),
                                            badge = if (serverState.integrations.isDiscordReady || serverState.integrations.isLastFmReady || serverState.integrations.isListenBrainzReady) "On" else null,
                                            onClick = { page = SettingsPage.INTEGRATIONS },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.VisibilityOff,
                                            title = if (es) "Privacidad" else "Privacy",
                                            subtitle = if (design.privateSessionEnabled) {
                                                if (es) "Sesión privada activa • ${blockedArtists.size} bloqueados" else "Incognito ON • ${blockedArtists.size} blocked"
                                            } else {
                                                if (es) "Sesión privada y ${blockedArtists.size} artistas bloqueados" else "Private session & ${blockedArtists.size} blocked"
                                            },
                                            accent = Color(0xFFCE93D8),
                                            badge = if (design.privateSessionEnabled) "Privada" else null,
                                            onClick = { page = SettingsPage.PRIVACY_BLOCKED },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.TouchApp,
                                            title = if (es) "Gestos" else "Gestures",
                                            subtitle = if (es) "Deslizar, doble toque ±${design.doubleTapSeekSeconds}s y agitar" else "Swipe, double-tap & shake",
                                            accent = palette.primary,
                                            onClick = { page = SettingsPage.GESTURES },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.SystemUpdate,
                                            title = if (es) "Actualizaciones" else "Updates",
                                            subtitle = if (updateInfo.hasUpdate) "${updateInfo.latestLabel}" else "v${BuildConfig.VERSION_NAME}",
                                            accent = Color(0xFF00E676),
                                            highlightDot = updateInfo.hasUpdate,
                                            onClick = { page = SettingsPage.UPDATES },
                                        ),
                                        SegmentedEntry(
                                            icon = Icons.Rounded.Info,
                                            title = if (es) "Acerca de" else "About",
                                            subtitle = "Croix Player Prime • v${BuildConfig.VERSION_NAME}",
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
                        // SUBMENU: ACCOUNT HUB (GAUSSIAN BLUR PROFILE + BRAND CHANNELS + MULTI-ACCOUNT)
                        // =========================================================
                        SettingsPage.ACCOUNT -> {
                            val cfg = serverState.serverConfig
                            if (serverNotice != null) {
                                item {
                                    CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = palette.primary) {
                                        Text(serverNotice, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(14.dp))
                                    }
                                }
                            }
                            // Hero Profile Card with Gaussian-Blurred Avatar Backdrop
                            item {
                                val avatar = cfg.ytMusicAccountAvatarUrl
                                CroixGlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 26.dp,
                                    accentTint = palette.primary,
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (avatar.isNotBlank()) {
                                            AsyncImage(
                                                model = avatar,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .blur(30.dp),
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(
                                                                palette.primary.copy(alpha = 0.55f),
                                                                palette.secondary.copy(alpha = 0.35f),
                                                                Color(0xFF0A0912),
                                                            ),
                                                        ),
                                                    ),
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.78f)),
                                                    ),
                                                ),
                                        )
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(84.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.primary.copy(alpha = 0.3f))
                                                    .border(2.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                if (avatar.isNotBlank()) {
                                                    AsyncImage(
                                                        model = avatar,
                                                        contentDescription = cfg.ytMusicAccountName,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                    )
                                                } else {
                                                    Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(42.dp))
                                                }
                                            }
                                            Text(
                                                text = if (serverState.isYtMusicConnected) cfg.ytMusicAccountName else (if (es) "Sin cuenta conectada" else "Not signed in"),
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            val subLine = listOf(cfg.ytMusicAccountHandle, cfg.ytMusicAccountEmail)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" • ")
                                            if (subLine.isNotBlank()) {
                                                Text(subLine, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall)
                                            }
                                            if (serverState.ytMusicSyncSummary.isNotBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50.dp))
                                                        .background(Color.White.copy(alpha = 0.16f))
                                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                                ) {
                                                    Text(serverState.ytMusicSyncSummary, color = Color.White, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                if (serverState.isYtMusicConnected) {
                                                    Button(
                                                        enabled = !ytMusicSyncing,
                                                        onClick = onSyncYtMusicNow,
                                                        modifier = Modifier.weight(1f),
                                                    ) {
                                                        if (ytMusicSyncing) {
                                                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                                            Spacer(Modifier.width(6.dp))
                                                        } else {
                                                            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                                                            Spacer(Modifier.width(6.dp))
                                                        }
                                                        Text(if (es) "Sincronizar" else "Sync Now")
                                                    }
                                                    OutlinedButton(
                                                        onClick = onDisconnectYtMusic,
                                                        modifier = Modifier.weight(1f),
                                                    ) { Text(strings.disconnect) }
                                                } else {
                                                    Button(
                                                        onClick = onOpenYtMusicLogin,
                                                        modifier = Modifier.fillMaxWidth(),
                                                    ) {
                                                        Icon(Icons.Rounded.Link, null, modifier = Modifier.size(18.dp))
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(if (es) "Conectar cuenta de YouTube Music" else "Sign in to YouTube Music")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Automatic Synchronization Toggle
                            if (serverState.isYtMusicConnected) {
                                item {
                                    CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            StudioSwitch(
                                                if (es) "Sincronización automática al iniciar" else "Auto-sync library on launch",
                                                cfg.ytMusicAutoSync,
                                            ) { en ->
                                                onUpdateServerConfig { it.copy(ytMusicAutoSync = en) }
                                            }
                                            Text(
                                                if (es) "Sincroniza tus Me gusta, Historial y Playlists automáticamente y personaliza tus Selecciones Rápidas en Inicio." else "Automatically syncs Likes, History & Playlists and drives Home Quick Picks.",
                                                color = palette.textSecondary,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }

                                // Brand Profiles / Channels on the SAME Gmail!
                                item {
                                    CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Icon(Icons.Rounded.SwapHoriz, null, tint = palette.primary, modifier = Modifier.size(20.dp))
                                                    Text(if (es) "Perfiles / Canales de este correo" else "Profiles / Channels", color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                                TextButton(onClick = onRefreshYtMusicProfiles) {
                                                    Text(if (es) "Buscar" else "Refresh")
                                                }
                                            }
                                            if (serverState.ytMusicBrandProfiles.isEmpty()) {
                                                Text(
                                                    if (es) "Toca «Buscar» para listar todos los canales o perfiles de marca asociados a tu Gmail." else "Tap Refresh to detect brand channels under your Gmail.",
                                                    color = palette.textSecondary,
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            } else {
                                                serverState.ytMusicBrandProfiles.forEach { prof ->
                                                    val isCurrent = prof.isSelected ||
                                                        (cfg.ytMusicPageId.isNotBlank() && cfg.ytMusicPageId == prof.pageId) ||
                                                        (cfg.ytMusicPageId.isBlank() && prof.name.equals(cfg.ytMusicAccountName, ignoreCase = true))
                                                    CroixGlassCard(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        cornerRadius = 14.dp,
                                                        accentTint = if (isCurrent) palette.primary else null,
                                                        onClick = { if (!isCurrent && !ytMusicSyncing) onSwitchYtMusicProfile(prof) },
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                        ) {
                                                            Box(
                                                                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                if (prof.avatarUrl.isNotBlank()) {
                                                                    AsyncImage(
                                                                        model = prof.avatarUrl,
                                                                        contentDescription = prof.name,
                                                                        contentScale = ContentScale.Crop,
                                                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                                    )
                                                                } else {
                                                                    Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                                                }
                                                            }
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(prof.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                                val meta = prof.channelHandle.ifBlank { prof.byline }.ifBlank { "Canal YouTube" }
                                                                Text(meta, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                                            }
                                                            if (isCurrent) {
                                                                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
                                                            } else {
                                                                Text(if (es) "Usar" else "Switch", color = palette.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Switch Account / Saved Google Accounts
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(if (es) "Cambiar de cuenta" else "Switch Account", color = Color.White, fontWeight = FontWeight.Bold)
                                            CroixGlassCard(
                                                cornerRadius = 50.dp,
                                                accentTint = palette.secondary,
                                                onClick = onOpenAddYtMusicAccount,
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                ) {
                                                    Icon(Icons.Rounded.PersonAdd, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                                    Text(if (es) "Añadir cuenta" else "Add Account", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                        if (serverState.savedYtAccounts.isEmpty()) {
                                            Text(
                                                if (es) "Agrega una o más cuentas de Google para cambiar entre ellas en 1 toque." else "Add one or more Google accounts to switch in 1 tap.",
                                                color = palette.textSecondary,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        } else {
                                            serverState.savedYtAccounts.forEach { acc ->
                                                val isActive = serverState.isYtMusicConnected && acc.cookie == cfg.ytMusicCookie
                                                CroixGlassCard(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    cornerRadius = 14.dp,
                                                    accentTint = if (isActive) Color(0xFF00E676) else null,
                                                    onClick = { if (!isActive && !ytMusicSyncing) onSwitchSavedYtAccount(acc) },
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            if (acc.avatarUrl.isNotBlank()) {
                                                                AsyncImage(
                                                                    model = acc.avatarUrl,
                                                                    contentDescription = acc.name,
                                                                    contentScale = ContentScale.Crop,
                                                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                                )
                                                            } else {
                                                                Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                                            }
                                                        }
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(acc.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                            Text(acc.email.ifBlank { acc.channelHandle }.ifBlank { "Cuenta guardada" }, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                                        }
                                                        if (isActive) {
                                                            Text(if (es) "Activa" else "Active", color = Color(0xFF00E676), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                        } else {
                                                            TextButton(onClick = { onRemoveSavedYtAccount(acc.id) }) {
                                                                Text(if (es) "Quitar" else "Remove", fontSize = 11.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Playlists synced from active YT Music profile
                            if (serverState.isYtMusicConnected && serverState.ytMusicPlaylists.isNotEmpty()) {
                                item {
                                    CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(if (es) "Tus Playlists de YouTube Music (${serverState.ytMusicPlaylists.size})" else "Your YouTube Music Playlists", color = palette.primary, fontWeight = FontWeight.Bold)
                                            serverState.ytMusicPlaylists.take(15).forEach { pl ->
                                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(pl.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                            if (pl.subtitle.isNotBlank()) {
                                                                Text(pl.subtitle, color = palette.textSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                                            }
                                                        }
                                                        TextButton(onClick = { onImportYtMusicPlaylist(pl) }) { Text(if (es) "Importar" else "Import") }
                                                    }
                                                }
                                            }
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
                                                Text("Cuenta Real YouTube Music", color = Color.White, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    if (serverState.isYtMusicConnected) "${strings.connectedAs}: ${cfg.ytMusicAccountName}" else strings.notConnected,
                                                    color = if (serverState.isYtMusicConnected) Color(0xFF00E676) else palette.textSecondary,
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                                if (serverState.ytMusicSyncSummary.isNotBlank()) {
                                                    Text(serverState.ytMusicSyncSummary, color = palette.secondary, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            if (serverState.isYtMusicConnected) {
                                                OutlinedButton(onClick = onDisconnectYtMusic) { Text(strings.disconnect) }
                                            } else {
                                                Button(onClick = onOpenYtMusicLogin) { Text(if (es) "Conectar" else "Connect") }
                                            }
                                        }

                                        if (serverState.isYtMusicConnected) {
                                            Button(
                                                enabled = !ytMusicSyncing,
                                                onClick = onSyncYtMusicNow,
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                if (ytMusicSyncing) {
                                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                }
                                                Text(if (es) "Sincronizar Me gusta, Historial y Playlists de YT Music" else "Sync Real YT Music Library")
                                            }
                                            if (serverState.ytMusicPlaylists.isNotEmpty()) {
                                                Text(if (es) "Tus Playlists reales de YouTube Music" else "Your YT Music Playlists", color = palette.primary, fontWeight = FontWeight.Bold)
                                                serverState.ytMusicPlaylists.take(12).forEach { pl ->
                                                    CroixGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically,
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(pl.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                                if (pl.subtitle.isNotBlank()) {
                                                                    Text(pl.subtitle, color = palette.textSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                                                }
                                                            }
                                                            TextButton(onClick = { onImportYtMusicPlaylist(pl) }) { Text("Importar") }
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
                        // SUBMENU: DEEP DYNAMIC APPEARANCE STUDIO + LIVE PREVIEW (v5.0 PRO)
                        // =========================================================
                        SettingsPage.STUDIO -> {
                            // Live Interactive Preview Card reflecting shape, shadow, font, button style, seekbar & colors!
                            item {
                                var previewPlaying by remember { mutableStateOf(true) }
                                var previewProgress by remember { mutableFloatStateOf(0.62f) }
                                val previewArtShape = resolveArtworkShape(design)
                                val previewPlayCorner by animateDpAsState(
                                    targetValue = if (design.playerButtonStyle == PrimePlayerButtonStyle.SQUIRCLE_EXPRESSIVE && previewPlaying) 12.dp else 24.dp,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "studioPreviewPlayCorner",
                                )
                                val previewPlayShape = RoundedCornerShape(previewPlayCorner)
                                val previewBrush = if (design.useGradientStyle) {
                                    Brush.linearGradient(listOf(palette.primary, palette.secondary))
                                } else {
                                    Brush.linearGradient(listOf(palette.primary, palette.primary))
                                }
                                val previewCardTint = when (design.playerBackgroundStyle) {
                                    PrimePlayerBackgroundStyle.PURE_AMOLED -> Color.Black
                                    else -> palette.primary
                                }

                                CroixGlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    accentTint = previewCardTint,
                                    opacityOverride = if (design.playerBackgroundStyle == PrimePlayerBackgroundStyle.PURE_AMOLED) 0.78f else null,
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                val artShadow = if (design.artworkShadowEnabled && design.artworkShadowElevationDp > 0.5f) {
                                                    Modifier.shadow(
                                                        elevation = (design.artworkShadowElevationDp * 0.55f).dp,
                                                        shape = previewArtShape,
                                                        ambientColor = palette.primary,
                                                        spotColor = palette.primary,
                                                    )
                                                } else Modifier
                                                Box(
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .then(artShadow)
                                                        .clip(previewArtShape)
                                                        .background(previewBrush),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    if (design.vinylSpinEnabled || design.artworkShape == PrimeArtworkShape.CIRCLE_VINYL) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.Black.copy(alpha = 0.72f))
                                                                .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                                                        )
                                                    } else {
                                                        Icon(Icons.Rounded.GraphicEq, null, tint = Color.White)
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        if (es) "Vista previa en vivo" else "Live Preview",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        "${design.presetName} • ${design.fontStyle.label} • ${design.progressStyle.label}",
                                                        color = palette.textSecondary,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                            val btnMod = when (design.playerButtonStyle) {
                                                PrimePlayerButtonStyle.GRADIENT_CIRCLE,
                                                PrimePlayerButtonStyle.SQUIRCLE_EXPRESSIVE,
                                                -> Modifier.size(40.dp).clip(previewPlayShape).background(previewBrush)
                                                PrimePlayerButtonStyle.GLASS_FROSTED -> Modifier
                                                    .size(40.dp)
                                                    .clip(previewPlayShape)
                                                    .background(Color.White.copy(alpha = 0.16f))
                                                    .border(1.5.dp, palette.glassBorderTop, previewPlayShape)
                                                PrimePlayerButtonStyle.OUTLINED_NEON -> Modifier
                                                    .size(40.dp)
                                                    .clip(previewPlayShape)
                                                    .background(Color.Black.copy(alpha = 0.45f))
                                                    .border(2.dp, previewBrush, previewPlayShape)
                                            }
                                            val iconTint = when (design.playerButtonStyle) {
                                                PrimePlayerButtonStyle.GLASS_FROSTED,
                                                PrimePlayerButtonStyle.OUTLINED_NEON,
                                                -> palette.primary
                                                else -> Color.White
                                            }
                                            Box(
                                                modifier = btnMod.clickable { previewPlaying = !previewPlaying },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    if (previewPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                    contentDescription = "Probar botón",
                                                    tint = iconTint,
                                                    modifier = Modifier.size(22.dp),
                                                )
                                            }
                                        }
                                        CroixSeekBar(progress = previewProgress, onSeek = { previewProgress = it })
                                    }
                                }
                            }

                            val tabs = if (es) {
                                listOf("Colores", "Cristal", "Tipografía", "Reproductor", "Mini y Nav", "Inicio", "Letras y Anim.", "Presets")
                            } else {
                                listOf("Colors", "Glass", "Typography", "Player", "Mini & Nav", "Home", "Lyrics & Anim", "Presets")
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
                                            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp))
                                        }
                                    }
                                }
                            }

                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        when (studioTab) {
                                            // TAB 0: COLORES & 16 PALETAS RÁPIDAS
                                            0 -> {
                                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = palette.primary, cornerRadius = 16.dp) {
                                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        StudioSwitch(
                                                            if (es) "Color dinámico según la canción actual" else "Dynamic Color from Playing Track",
                                                            design.colorMode == PrimeColorMode.ALBUM_MORPH,
                                                        ) { enabled ->
                                                            onUpdateDesign {
                                                                it.copy(
                                                                    colorMode = if (enabled) PrimeColorMode.ALBUM_MORPH else PrimeColorMode.VIOLET_GLASS,
                                                                    pureBlackBackground = false,
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            if (es) "Cambia toda la app suavemente al color dominante de la música que estás reproduciendo." else "Smoothly morphs the whole app to the current song's cover art colors.",
                                                            color = palette.textSecondary,
                                                            style = MaterialTheme.typography.bodySmall,
                                                        )
                                                    }
                                                }

                                                Text(if (es) "Paleta Rápida (16 colores)" else "Quick Color Palette (16)", color = Color.White, fontWeight = FontWeight.Bold)
                                                PrimeColorSwatches.All.chunked(4).forEach { rowSwatches ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    ) {
                                                        rowSwatches.forEach { sw ->
                                                            val selected = if (sw.isDynamicAlbum) {
                                                                design.colorMode == PrimeColorMode.ALBUM_MORPH
                                                            } else {
                                                                design.colorMode == PrimeColorMode.CUSTOM_GRADIENT &&
                                                                    kotlin.math.abs(design.primaryHue - sw.primaryHue) < 4f &&
                                                                    kotlin.math.abs(design.secondaryHue - sw.secondaryHue) < 4f
                                                            }
                                                            Column(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(14.dp))
                                                                    .background(if (selected) Color(sw.previewArgb).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.04f))
                                                                    .border(
                                                                        width = if (selected) 1.6.dp else 0.7.dp,
                                                                        color = if (selected) Color(sw.previewArgb) else Color.White.copy(alpha = 0.12f),
                                                                        shape = RoundedCornerShape(14.dp),
                                                                    )
                                                                    .clickable {
                                                                        if (sw.isDynamicAlbum) {
                                                                            onUpdateDesign {
                                                                                it.copy(
                                                                                    presetName = "Carátula Viva",
                                                                                    colorMode = PrimeColorMode.ALBUM_MORPH,
                                                                                    pureBlackBackground = false,
                                                                                )
                                                                            }
                                                                        } else {
                                                                            onUpdateDesign {
                                                                                it.copy(
                                                                                    presetName = sw.label,
                                                                                    colorMode = PrimeColorMode.CUSTOM_GRADIENT,
                                                                                    primaryHue = sw.primaryHue,
                                                                                    secondaryHue = sw.secondaryHue,
                                                                                    primarySaturation = sw.primarySat,
                                                                                    secondarySaturation = sw.secondarySat,
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(30.dp)
                                                                        .clip(CircleShape)
                                                                        .background(
                                                                            Brush.linearGradient(
                                                                                listOf(Color(sw.previewArgb), Color(sw.secondaryPreviewArgb)),
                                                                            ),
                                                                        ),
                                                                    contentAlignment = Alignment.Center,
                                                                ) {
                                                                    if (selected) {
                                                                        Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                                    }
                                                                }
                                                                Text(
                                                                    sw.label,
                                                                    color = Color.White,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

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
                                                StudioSwitch(if (es) "Tema aleatorio al abrir la app" else "Random theme on startup", design.randomThemeOnStartup) { v ->
                                                    onUpdateDesign { it.copy(randomThemeOnStartup = v) }
                                                }
                                                StudioEnumRow(if (es) "Paleta Base" else "Color Palette", PrimeColorMode.entries, design.colorMode, { it.label }) { m ->
                                                    onUpdateDesign { it.copy(colorMode = m) }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(40.dp)
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

                                            // TAB 1: CRISTAL & AMBIENTE
                                            1 -> {
                                                StudioSlider(if (es) "Transparencia del Cristal" else "Glass Opacity", design.glassOpacity, 0.05f..0.75f, "${(design.glassOpacity * 100).roundToInt()}%") { v ->
                                                    onUpdateDesign { it.copy(glassOpacity = v) }
                                                }
                                                StudioSlider(if (es) "Desenfoque de Barras" else "Floating Bar Blur", design.glassBlurDp, 0f..34f, "${design.glassBlurDp.roundToInt()} dp") { v ->
                                                    onUpdateDesign { it.copy(glassBlurDp = v) }
                                                }
                                                StudioSwitch(if (es) "Textura de grano cristalino (Haze)" else "Frosted Glass Grain Texture", design.glassNoiseEnabled) { v ->
                                                    onUpdateDesign { it.copy(glassNoiseEnabled = v) }
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

                                            // TAB 2: TIPOGRAFÍA Y TEXTOS
                                            2 -> {
                                                StudioEnumRow(if (es) "Estilo de Fuente Global" else "Global Font Family", PrimeFontStyle.entries, design.fontStyle, { it.label }) { f ->
                                                    onUpdateDesign { it.copy(fontStyle = f) }
                                                }
                                                StudioSlider(if (es) "Escala de Tipografía" else "Font Scale", design.fontScale, 0.85f..1.20f, "${(design.fontScale * 100).roundToInt()}%") { v ->
                                                    onUpdateDesign { it.copy(fontScale = v) }
                                                }
                                                StudioSwitch(if (es) "Permitir 2 líneas en títulos largos" else "Two-line Track Titles", design.twoLineTrackTitle) { v ->
                                                    onUpdateDesign { it.copy(twoLineTrackTitle = v) }
                                                }
                                                StudioSwitch(if (es) "Texto deslizante (Marquee) en reproductor" else "Marquee Scrolling Titles", design.marqueeTrackTitle) { v ->
                                                    onUpdateDesign { it.copy(marqueeTrackTitle = v) }
                                                }
                                            }

                                            // TAB 3: REPRODUCTOR Y CARÁTULA
                                            3 -> {
                                                StudioEnumRow(if (es) "Fondo del Reproductor" else "Player Background", PrimePlayerBackgroundStyle.entries, design.playerBackgroundStyle, { it.label }) { bg ->
                                                    onUpdateDesign { it.copy(playerBackgroundStyle = bg) }
                                                }
                                                StudioEnumRow(if (es) "Estilo de Botón Play/Pausa" else "Play Button Style", PrimePlayerButtonStyle.entries, design.playerButtonStyle, { it.label }) { b ->
                                                    onUpdateDesign { it.copy(playerButtonStyle = b) }
                                                }
                                                StudioEnumRow(if (es) "Forma de la Carátula" else "Artwork Shape", PrimeArtworkShape.entries, design.artworkShape, { it.label }) { sh ->
                                                    onUpdateDesign { it.copy(artworkShape = sh) }
                                                }
                                                StudioEnumRow(if (es) "Diseño del Reproductor" else "Player Layout", PrimePlayerStyle.entries, design.playerStyle, { it.label }) { s ->
                                                    onUpdateDesign { it.copy(playerStyle = s) }
                                                }
                                                StudioEnumRow(if (es) "Barra de Progreso (6 estilos)" else "Progress Bar (6 styles)", PrimeProgressStyle.entries, design.progressStyle, { it.label }) { p ->
                                                    onUpdateDesign { it.copy(progressStyle = p) }
                                                }
                                                StudioEnumRow(if (es) "Visualizador en Vivo" else "Live Visualizer", PrimeVisualizerStyle.entries, design.visualizerStyle, { it.label }) { v ->
                                                    onUpdateDesign { it.copy(visualizerStyle = v) }
                                                }
                                                StudioSlider(if (es) "Tamaño de la Carátula" else "Artwork Size", design.artworkScale, 0.64f..0.98f, "${(design.artworkScale * 100).roundToInt()}%") { v ->
                                                    onUpdateDesign { it.copy(artworkScale = v) }
                                                }
                                                StudioSlider(if (es) "Redondeo / Corte de Carátula" else "Artwork Corner", design.artworkCornerDp, 0f..36f, "${design.artworkCornerDp.roundToInt()} dp") { v ->
                                                    onUpdateDesign { it.copy(artworkCornerDp = v) }
                                                }
                                                StudioSwitch(if (es) "Sombra luminosa en la carátula" else "Colored Artwork Glow Shadow", design.artworkShadowEnabled) { v ->
                                                    onUpdateDesign { it.copy(artworkShadowEnabled = v) }
                                                }
                                                if (design.artworkShadowEnabled) {
                                                    StudioSlider(if (es) "Intensidad de sombra" else "Shadow Elevation", design.artworkShadowElevationDp, 0f..32f, "${design.artworkShadowElevationDp.roundToInt()} dp") { v ->
                                                        onUpdateDesign { it.copy(artworkShadowElevationDp = v) }
                                                    }
                                                }
                                                StudioSwitch(if (es) "Efecto Vinilo Giratorio" else "Vinyl Spin Artwork", design.vinylSpinEnabled) { v ->
                                                    onUpdateDesign { it.copy(vinylSpinEnabled = v) }
                                                }
                                                StudioSwitch(if (es) "Ocultar carátula (Modo Enfoque)" else "Hide Artwork (Focus Mode)", design.hidePlayerArtwork) { v ->
                                                    onUpdateDesign { it.copy(hidePlayerArtwork = v) }
                                                }
                                                StudioSwitch(if (es) "Tocar carátula para Play/Pausa" else "Tap Artwork to Play/Pause", design.tapArtworkToPlayPause) { v ->
                                                    onUpdateDesign { it.copy(tapArtworkToPlayPause = v) }
                                                }
                                                StudioSwitch(if (es) "Mostrar barra de volumen en el reproductor" else "Show Volume Slider in Player", design.showPlayerVolumeBar) { v ->
                                                    onUpdateDesign { it.copy(showPlayerVolumeBar = v) }
                                                }
                                                StudioSwitch(if (es) "Mantener pantalla encendida en reproductor" else "Keep Screen On in Player", design.keepScreenOnPlayer) { v ->
                                                    onUpdateDesign { it.copy(keepScreenOnPlayer = v) }
                                                }
                                                StudioSwitch(if (es) "Fondo Canvas Animado en Reproductor" else "Animated Canvas Backdrop", design.canvasMotionEnabled) { v ->
                                                    onUpdateDesign { it.copy(canvasMotionEnabled = v) }
                                                }
                                                StudioSwitch(if (es) "Carátula 3D Parallax Giroscopio [BETA]" else "3D Gyroscope Parallax [BETA]", design.artworkParallax3d) { v ->
                                                    onUpdateDesign { it.copy(artworkParallax3d = v) }
                                                }
                                                if (design.artworkParallax3d) {
                                                    StudioSlider(if (es) "Intensidad 3D Parallax [BETA]" else "3D Parallax Intensity", design.parallax3dIntensity, 0.5f..2.2f, "${"%.1f".format(design.parallax3dIntensity)}x") { v ->
                                                        onUpdateDesign { it.copy(parallax3dIntensity = v) }
                                                    }
                                                }
                                                StudioSlider(if (es) "Opacidad del Panel de Controles" else "Control Dock Opacity", design.playerControlDockOpacity, 0.05f..0.80f, "${(design.playerControlDockOpacity * 100).roundToInt()}%") { v ->
                                                    onUpdateDesign { it.copy(playerControlDockOpacity = v) }
                                                }
                                            }

                                            // TAB 4: MINI-REPRODUCTOR, NAVEGACIÓN E ÍCONO
                                            4 -> {
                                                StudioEnumRow(if (es) "Estilo del Mini-Reproductor" else "Mini-Player Style", PrimeMiniPlayerStyle.entries, design.miniPlayerStyle, { it.label }) { m ->
                                                    onUpdateDesign {
                                                        it.copy(
                                                            miniPlayerStyle = m,
                                                            miniPlayerFloating = m != PrimeMiniPlayerStyle.DOCKED_MINIMAL,
                                                        )
                                                    }
                                                }
                                                StudioSwitch(if (es) "Mini-Reproductor Flotante" else "Floating Mini-Player", design.miniPlayerFloating) { v ->
                                                    onUpdateDesign { it.copy(miniPlayerFloating = v) }
                                                }
                                                StudioSwitch(if (es) "Deslizar Mini-Reproductor para cambiar tema" else "Swipe Mini-Player to Skip", design.swipeMiniPlayerToSkip) { v ->
                                                    onUpdateDesign { it.copy(swipeMiniPlayerToSkip = v) }
                                                }
                                                StudioSlider(if (es) "Altura del Mini-Reproductor" else "Mini-Player Height", design.miniPlayerHeightDp, 56f..84f, "${design.miniPlayerHeightDp.roundToInt()} dp") { v ->
                                                    onUpdateDesign { it.copy(miniPlayerHeightDp = v) }
                                                }
                                                StudioEnumRow(if (es) "Estilo de Barra Inferior" else "Nav Bar Style", PrimeNavBarStyle.entries, design.navBarStyle, { it.label }) { n ->
                                                    onUpdateDesign { it.copy(navBarStyle = n) }
                                                }
                                                StudioSwitch(if (es) "Barra de navegación delgada (Slim)" else "Slim Navigation Bar", design.slimNavBar) { v ->
                                                    onUpdateDesign { it.copy(slimNavBar = v) }
                                                }
                                                StudioSwitch(if (es) "Mostrar Etiquetas de Navegación" else "Show Nav Labels", design.showNavLabels) { v ->
                                                    onUpdateDesign { it.copy(showNavLabels = v) }
                                                }
                                                StudioEnumRow(if (es) "Pestaña al abrir la app" else "Default Start Tab", PrimeStartTab.entries, design.defaultStartTab, { it.label }) { tab ->
                                                    onUpdateDesign { it.copy(defaultStartTab = tab) }
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

                                            // TAB 5: PANTALLA DE INICIO
                                            5 -> {
                                                StudioEnumRow(if (es) "Densidad de Tarjetas" else "Card Density", PrimeCardDensity.entries, design.cardDensity, { it.label }) { d ->
                                                    onUpdateDesign { it.copy(cardDensity = d) }
                                                }
                                                StudioSwitch(if (es) "Mostrar Rueda Flow Principal" else "Show Flow Hero Wheel", design.showCroixFlowHero) { v ->
                                                    onUpdateDesign { it.copy(showCroixFlowHero = v) }
                                                }
                                                StudioSwitch(if (es) "Mostrar Estados de Ánimo Flow" else "Show Flow Moods", design.showMoodChips) { v ->
                                                    onUpdateDesign { it.copy(showMoodChips = v) }
                                                }
                                                StudioSwitch(if (es) "Mostrar Joyas Ocultas y Modo Misterio" else "Show Hidden Gems & Mystery Mode", design.showDiscoveryDuo) { v ->
                                                    onUpdateDesign { it.copy(showDiscoveryDuo = v) }
                                                }
                                                StudioSwitch(if (es) "Mostrar Curador Musical IA" else "Show AI Music Curator", design.showAiCuratorCard) { v ->
                                                    onUpdateDesign { it.copy(showAiCuratorCard = v) }
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
                                            }

                                            // TAB 6: LETRAS Y ANIMACIONES
                                            6 -> {
                                                StudioEnumRow(if (es) "Animación de Letras" else "Lyrics Animation", PrimeLyricsAnimStyle.entries, design.lyricsAnimStyle, { it.label }) { a ->
                                                    onUpdateDesign { it.copy(lyricsAnimStyle = a) }
                                                }
                                                StudioEnumRow(if (es) "Alineación de Letras" else "Lyrics Alignment", PrimeLyricsAlign.entries, design.lyricsAlign, { it.label }) { a ->
                                                    onUpdateDesign { it.copy(lyricsAlign = a) }
                                                }
                                                StudioSwitch(if (es) "Resplandor en línea activa" else "Active Lyric Glow", design.lyricsActiveGlow) { v ->
                                                    onUpdateDesign { it.copy(lyricsActiveGlow = v) }
                                                }
                                                StudioSlider(if (es) "Tamaño de Letras" else "Lyrics Font Size", design.lyricsFontSizeSp, 16f..36f, "${design.lyricsFontSizeSp.roundToInt()} sp") { v ->
                                                    onUpdateDesign { it.copy(lyricsFontSizeSp = v) }
                                                }
                                                StudioSlider(if (es) "Espaciado entre líneas" else "Lyrics Line Spacing", design.lyricsLineSpacingDp, 6f..26f, "${design.lyricsLineSpacingDp.roundToInt()} dp") { v ->
                                                    onUpdateDesign { it.copy(lyricsLineSpacingDp = v) }
                                                }
                                                StudioSlider(if (es) "Desenfoque de Fondo en Letras" else "Lyrics Backdrop Blur", design.lyricsBackdropBlurDp, 0f..34f, "${design.lyricsBackdropBlurDp.roundToInt()} dp") { v ->
                                                    onUpdateDesign { it.copy(lyricsBackdropBlurDp = v) }
                                                }
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

                                            // TAB 7: 12 TEMAS PREDEFINIDOS + IMPORT/EXPORT + RESTABLECER
                                            7 -> {
                                                Text(if (es) "Temas Predefinidos (${PrimePresets.All.size})" else "Theme Presets (${PrimePresets.All.size})", color = palette.primary, fontWeight = FontWeight.Bold)
                                                PrimePresets.All.forEach { preset ->
                                                    val isActive = design.presetName == preset.presetName
                                                    val pColor = when (preset.colorMode) {
                                                        PrimeColorMode.VIOLET_GLASS -> Color(0xFFA238FF)
                                                        PrimeColorMode.NEON_CYAN -> Color(0xFF00E5FF)
                                                        PrimeColorMode.ALBUM_MORPH -> Color(0xFFFF4081)
                                                        PrimeColorMode.AMOLED_BLACK -> Color(0xFF424250)
                                                        PrimeColorMode.DYNAMIC_SYSTEM -> Color(0xFF80CBC4)
                                                        PrimeColorMode.CUSTOM_GRADIENT -> preset.customPrimaryFromHue
                                                    }
                                                    val sColor = when (preset.colorMode) {
                                                        PrimeColorMode.VIOLET_GLASS -> Color(0xFFFF2E93)
                                                        PrimeColorMode.NEON_CYAN -> Color(0xFF7C4DFF)
                                                        PrimeColorMode.ALBUM_MORPH -> Color(0xFF00E5FF)
                                                        PrimeColorMode.AMOLED_BLACK -> Color.Black
                                                        PrimeColorMode.DYNAMIC_SYSTEM -> Color(0xFF536DFE)
                                                        PrimeColorMode.CUSTOM_GRADIENT -> preset.customSecondaryFromHue
                                                    }
                                                    CroixGlassCard(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        cornerRadius = 14.dp,
                                                        accentTint = if (isActive) pColor else null,
                                                        onClick = { onApplyPreset(preset) },
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(34.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Brush.linearGradient(listOf(pColor, sColor)))
                                                                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                                                            )
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(preset.presetName, color = Color.White, fontWeight = FontWeight.Bold)
                                                                Text(
                                                                    "${preset.fontStyle.label} • ${preset.playerBackgroundStyle.label} • ${preset.artworkShape.label}",
                                                                    color = palette.textSecondary,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                )
                                                            }
                                                            if (isActive) {
                                                                Text(if (es) "Activo" else "Active", color = palette.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
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
                                                OutlinedButton(
                                                    onClick = onResetDesign,
                                                    modifier = Modifier.fillMaxWidth(),
                                                ) {
                                                    Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(if (es) "Restablecer diseño a valores originales" else "Reset Appearance to Defaults")
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
                                        StudioEnumRow(if (es) "Animación de Letras" else "Lyrics Animation", PrimeLyricsAnimStyle.entries, design.lyricsAnimStyle, { it.label }) { a ->
                                            onUpdateDesign { it.copy(lyricsAnimStyle = a) }
                                        }
                                        StudioEnumRow(if (es) "Alineación de Letras" else "Lyrics Alignment", PrimeLyricsAlign.entries, design.lyricsAlign, { it.label }) { a ->
                                            onUpdateDesign { it.copy(lyricsAlign = a) }
                                        }
                                        StudioSwitch(if (es) "Resplandor en línea activa" else "Active Lyric Glow", design.lyricsActiveGlow) { v ->
                                            onUpdateDesign { it.copy(lyricsActiveGlow = v) }
                                        }
                                        StudioSlider(if (es) "Tamaño de Letras" else "Lyrics Font Size", design.lyricsFontSizeSp, 16f..36f, "${design.lyricsFontSizeSp.roundToInt()} sp") { v ->
                                            onUpdateDesign { it.copy(lyricsFontSizeSp = v) }
                                        }
                                        StudioSlider(if (es) "Espaciado entre líneas" else "Lyrics Line Spacing", design.lyricsLineSpacingDp, 6f..26f, "${design.lyricsLineSpacingDp.roundToInt()} dp") { v ->
                                            onUpdateDesign { it.copy(lyricsLineSpacingDp = v) }
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
                                        StudioSwitch(if (es) "Mantener pantalla encendida en reproductor/letras" else "Keep Screen On", design.keepScreenOnPlayer) { v ->
                                            onUpdateDesign { it.copy(keepScreenOnPlayer = v) }
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
                                        Text(if (es) "Audio Espacial 8D, Reverb y AutoEq" else "8D Spatial Audio, Reverb & AutoEq", color = palette.primary, fontWeight = FontWeight.Bold)
                                        if (design.betaKaraokeEnabled) {
                                            StudioSwitch(if (es) "Modo Karaoke [BETA] (Reducir voz)" else "Karaoke Vocal Remover [BETA]", playback.karaokeEnabled, onSetKaraokeEnabled)
                                            if (playback.karaokeEnabled) {
                                                StudioSlider(if (es) "Reducción de Voz Karaoke" else "Vocal Reduction", playback.karaokeStrength, 0.3f..1.0f, "${(playback.karaokeStrength * 100).roundToInt()}%", onSetKaraokeStrength)
                                            }
                                        }
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
                        // SUBMENU: AI INTEGRATION (GEMINI / OPENAI API)
                        // =========================================================
                        SettingsPage.AI_INTEGRATION -> {
                            val ai = serverState.aiConfig
                            if (serverNotice != null) {
                                item {
                                    CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFFB388FF)) {
                                        Text(serverNotice, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(14.dp))
                                    }
                                }
                            }
                            item {
                                var keyDraft by remember(ai.apiKey) { mutableStateOf(ai.apiKey) }
                                var modelDraft by remember(ai.model) { mutableStateOf(ai.model) }
                                var baseDraft by remember(ai.customBaseUrl) { mutableStateOf(ai.customBaseUrl) }
                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFFB388FF)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(if (es) "Integración con IA (API)" else "AI API Integration", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (es) "Usa tu API Key de Google Gemini (gratis en AI Studio) u OpenAI/OpenRouter/Groq para:\n• Traducir letras con contexto musical y poético.\n• Recomendar canciones personalizadas según tu historial en Inicio." else "Connect Gemini or OpenAI-compatible API for natural lyric translation and history-aware music curation.",
                                            color = palette.textSecondary,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        StudioSwitch(if (es) "Activar funciones con IA" else "Enable AI Features", ai.enabled) { en ->
                                            onUpdateAiConfig { it.copy(enabled = en) }
                                        }
                                        StudioEnumRow(
                                            title = if (es) "Proveedor de IA" else "AI Provider",
                                            entries = AiProviderMode.entries,
                                            selected = ai.provider,
                                            labelOf = { it.label },
                                        ) { prov ->
                                            modelDraft = prov.defaultModel
                                            onUpdateAiConfig { it.copy(provider = prov, model = prov.defaultModel) }
                                        }
                                        OutlinedTextField(
                                            value = keyDraft,
                                            onValueChange = { keyDraft = it },
                                            label = { Text("API Key") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        OutlinedTextField(
                                            value = modelDraft,
                                            onValueChange = { modelDraft = it },
                                            label = { Text("Modelo (ej: ${ai.provider.defaultModel})") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        if (ai.provider == AiProviderMode.OPENAI_COMPAT) {
                                            OutlinedTextField(
                                                value = baseDraft,
                                                onValueChange = { baseDraft = it },
                                                label = { Text("URL Base opcional (ej: https://api.groq.com/openai/v1)") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            Button(
                                                onClick = {
                                                    onUpdateAiConfig {
                                                        it.copy(
                                                            enabled = keyDraft.isNotBlank(),
                                                            apiKey = keyDraft.trim(),
                                                            model = modelDraft.trim().ifBlank { it.provider.defaultModel },
                                                            customBaseUrl = baseDraft.trim(),
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                            ) { Text(if (es) "Guardar IA" else "Save AI") }
                                            OutlinedButton(
                                                onClick = {
                                                    onUpdateAiConfig {
                                                        it.copy(
                                                            enabled = keyDraft.isNotBlank(),
                                                            apiKey = keyDraft.trim(),
                                                            model = modelDraft.trim().ifBlank { it.provider.defaultModel },
                                                            customBaseUrl = baseDraft.trim(),
                                                        )
                                                    }
                                                    onTestAiConnection()
                                                },
                                                modifier = Modifier.weight(1f),
                                            ) { Text(if (es) "Probar API" else "Test API") }
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================
                        // SUBMENU: BETA LABS (PARALLAX 3D + MODO MISTERIO + KARAOKE — ALL OFF BY DEFAULT!)
                        // =========================================================
                        SettingsPage.BETA_LABS -> {
                            // BETA 1: Modo Misterio 45s ???
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFFFF4081)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Rounded.QuestionMark, null, tint = Color(0xFFFF4081), modifier = Modifier.size(20.dp))
                                            Text(if (es) "Modo Misterio 45s ??? [BETA]" else "Blind Discovery 45s ??? [BETA]", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        }
                                        Text(
                                            if (es) "Desactivado por defecto. Al activarlo aparece la playlist misteriosa en Inicio: los primeros 45s de cada canción se difuminan con «???» y luego se revelan poco a poco." else "Off by default. Adds the 45s blurred mystery playlist card to Home.",
                                            color = palette.textSecondary,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        StudioSwitch(if (es) "Activar Modo Misterio en Inicio" else "Enable Mystery Mode on Home", design.betaBlindDiscoveryEnabled) { v ->
                                            onUpdateDesign { it.copy(betaBlindDiscoveryEnabled = v) }
                                        }
                                    }
                                }
                            }

                            // BETA 2: Modo Karaoke DSP
                            item {
                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFFFF4081)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Rounded.Mic, null, tint = Color(0xFFFF4081), modifier = Modifier.size(20.dp))
                                            Text(if (es) "Modo Karaoke en Vivo [BETA]" else "Live Karaoke Mode [BETA]", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        }
                                        Text(
                                            if (es) "Desactivado por defecto. Al activarlo aparece el botón de micrófono junto a Play/Pausa que reduce la voz en tiempo real y abre las letras sincronizadas." else "Off by default. Adds the vocal-remover Karaoke mic button in the player dock.",
                                            color = palette.textSecondary,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        StudioSwitch(if (es) "Activar Modo Karaoke [BETA]" else "Enable Karaoke Mode [BETA]", design.betaKaraokeEnabled) { v ->
                                            onUpdateDesign { it.copy(betaKaraokeEnabled = v) }
                                            if (!v && playback.karaokeEnabled) onSetKaraokeEnabled(false)
                                        }
                                        if (design.betaKaraokeEnabled) {
                                            StudioSlider(if (es) "Reducción de Voz Karaoke" else "Vocal Reduction", playback.karaokeStrength, 0.3f..1.0f, "${(playback.karaokeStrength * 100).roundToInt()}%", onSetKaraokeStrength)
                                        }
                                    }
                                }
                            }

                            // BETA 3: Carátula 3D Parallax Giroscopio + Tarjeta en vivo
                            item {
                                val liveTilt by rememberParallaxTiltState(
                                    enabled = design.artworkParallax3d,
                                    intensity = design.parallax3dIntensity,
                                )
                                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFF00E5FF)) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("PARALLAX 3D GIROSCOPIO", color = Color(0xFF00E5FF), fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50.dp))
                                                    .background(Color(0xFF00E5FF).copy(alpha = 0.22f))
                                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                            ) {
                                                Text("BETA", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }

                                        StudioSwitch(
                                            if (es) "Carátula 3D Parallax con Giroscopio [BETA]" else "3D Gyroscope Artwork Parallax [BETA]",
                                            design.artworkParallax3d,
                                        ) { v ->
                                            onUpdateDesign { it.copy(artworkParallax3d = v) }
                                        }

                                        StudioSlider(
                                            label = if (es) "Intensidad de inclinación 3D" else "3D Tilt Intensity",
                                            value = design.parallax3dIntensity,
                                            range = 0.5f..2.2f,
                                            valueLabel = "${"%.1f".format(design.parallax3dIntensity)}x",
                                        ) { v -> onUpdateDesign { it.copy(parallax3dIntensity = v) } }

                                        Text(
                                            text = if (design.artworkParallax3d && liveTilt.sensorActive) {
                                                "Sensor activo • Inclina tu teléfono (X: ${"%.1f".format(liveTilt.rotX)}° | Y: ${"%.1f".format(liveTilt.rotY)}°)"
                                            } else if (design.artworkParallax3d) {
                                                "Calibrando giroscopio/acelerómetro..."
                                            } else {
                                                "Activa el interruptor de arriba para probar la carátula 3D aquí mismo."
                                            },
                                            color = palette.textSecondary,
                                            style = MaterialTheme.typography.bodySmall,
                                        )

                                        // Interactive Live 3D Holographic Card Test right inside BETA screen!
                                        Box(
                                            modifier = Modifier
                                                .size(188.dp)
                                                .graphicsLayer {
                                                    if (design.artworkParallax3d) {
                                                        cameraDistance = 12f * density
                                                        rotationX = liveTilt.rotX
                                                        rotationY = liveTilt.rotY
                                                    }
                                                }
                                                .clip(RoundedCornerShape(26.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(palette.primary, palette.secondary, Color(0xFF00E5FF)),
                                                    ),
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.graphicsLayer {
                                                    if (design.artworkParallax3d) {
                                                        translationX = liveTilt.innerShiftX
                                                        translationY = liveTilt.innerShiftY
                                                    }
                                                },
                                            ) {
                                                Icon(Icons.Rounded.GraphicEq, null, tint = Color.White, modifier = Modifier.size(54.dp))
                                                Text("CROIX 3D", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                                                Text("BETA LIVE", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                                            }
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
                                        StudioSwitch(if (es) "Deslizar Mini-Reproductor para cambiar canción" else "Swipe Mini-Player to Skip", design.swipeMiniPlayerToSkip) { v ->
                                            onUpdateDesign { it.copy(swipeMiniPlayerToSkip = v) }
                                        }
                                        StudioSwitch(if (es) "Tocar carátula para Play/Pausa" else "Tap Artwork to Play/Pause", design.tapArtworkToPlayPause) { v ->
                                            onUpdateDesign { it.copy(tapArtworkToPlayPause = v) }
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

private fun cfgNameOrSync(state: ServerSessionState): String =
    state.serverConfig.ytMusicAccountName.ifBlank { "Cuenta conectada" }

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
