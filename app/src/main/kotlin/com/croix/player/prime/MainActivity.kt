/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToQueue
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.croix.player.prime.ui.CroixPrimeViewModel
import com.croix.player.prime.ui.screens.AlbumDetailOverlay
import com.croix.player.prime.ui.screens.ArtistProfileOverlay
import com.croix.player.prime.ui.screens.FavoritesScreen
import com.croix.player.prime.ui.screens.HomeScreen
import com.croix.player.prime.ui.screens.MiniPlayerBar
import com.croix.player.prime.ui.screens.NowPlayingOverlay
import com.croix.player.prime.ui.screens.SearchScreen
import com.croix.player.prime.ui.screens.SettingsAndStudioOverlay
import com.croix.player.prime.ui.screens.ShazamRadarOverlay
import com.croix.player.prime.ui.screens.SpotifyLoginOverlay
import com.croix.player.prime.ui.screens.YtMusicLoginOverlay
import com.croix.prime.design.CroixAnimatedBackdrop
import com.croix.prime.design.CroixGlassCard
import com.croix.prime.design.CroixNavDestination
import com.croix.prime.design.CroixNavigationBar
import com.croix.prime.design.CroixPrimeTheme
import com.croix.prime.design.LocalCroixStrings
import com.croix.prime.design.PrimeTransitionStyle
import com.croix.prime.engine.HybridAlgorithmEngine
import com.croix.prime.engine.Track
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

class MainActivity : ComponentActivity() {
    private val vm: CroixPrimeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        setContent {
            CroixPrimeRoot(vm)
        }
    }
}

@Composable
private fun CroixPrimeRoot(vm: CroixPrimeViewModel) {
    val context = LocalContext.current
    val design by vm.designState.collectAsState()
    val playback by vm.playbackState.collectAsState()
    val homeFeed by vm.homeFeed.collectAsState()
    val homeLoading by vm.homeLoading.collectAsState()
    val hiddenGemsLoading by vm.hiddenGemsLoading.collectAsState()
    val blindDiscoveryLoading by vm.blindDiscoveryLoading.collectAsState()
    val aiCurator by vm.aiCuratorState.collectAsState()
    val translationProviderLabel by vm.translationProviderLabel.collectAsState()
    val updateInfo by vm.updateInfo.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val history by vm.history.collectAsState()
    val historyEntities by vm.historyEntities.collectAsState()
    val blockedArtists by vm.blockedArtists.collectAsState()
    val downloads by vm.downloads.collectAsState()
    val downloadProgress by vm.downloadProgress.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val playlistTracks by vm.playlistTracks.collectAsState()
    val stats by vm.listeningStats.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val searchFilter by vm.searchFilter.collectAsState()
    val searchResults by vm.searchResults.collectAsState()
    val artistSearchResults by vm.artistSearchResults.collectAsState()
    val albumSearchResults by vm.albumSearchResults.collectAsState()
    val recentSearches by vm.recentSearches.collectAsState()
    val searchLoading by vm.searchLoading.collectAsState()
    val importStatus by vm.importStatus.collectAsState()
    val lyrics by vm.lyrics.collectAsState()
    val lyricsLoading by vm.lyricsLoading.collectAsState()
    val translatedLyrics by vm.translatedLyrics.collectAsState()
    val translatingLyrics by vm.translatingLyrics.collectAsState()
    val albumPrimary by vm.albumPrimary.collectAsState()
    val albumSecondary by vm.albumSecondary.collectAsState()
    val artistProfile by vm.artistProfile.collectAsState()
    val artistLoading by vm.artistLoading.collectAsState()
    val albumDetail by vm.albumDetail.collectAsState()
    val albumLoading by vm.albumLoading.collectAsState()
    val serverState by vm.serverState.collectAsState()
    val serverHealth by vm.serverHealth.collectAsState()
    val adsBlockedCount by vm.adsBlockedCount.collectAsState()
    val spotifySyncing by vm.spotifySyncing.collectAsState()
    val ytMusicSyncing by vm.ytMusicSyncing.collectAsState()
    val serverNotice by vm.serverNotice.collectAsState()
    val audioCacheBytes by vm.audioCacheBytes.collectAsState()
    val shazamState by vm.shazamState.collectAsState()

    var currentRoute by remember { mutableStateOf(design.defaultStartTab.route) }
    var startTabApplied by remember { mutableStateOf(false) }
    var playerExpanded by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var spotifyLoginOpen by remember { mutableStateOf(false) }
    var ytMusicLoginOpen by remember { mutableStateOf(false) }
    var ytMusicAddAnother by remember { mutableStateOf(false) }
    var trackForMenu by remember { mutableStateOf<Track?>(null) }
    var trackForPlaylistDialog by remember { mutableStateOf<Track?>(null) }
    var quickNewPlaylistName by remember { mutableStateOf("") }

    // Apply persisted start tab once on cold launch without overriding manual tab switches
    LaunchedEffect(design.defaultStartTab) {
        if (!startTabApplied && design.defaultStartTab.route != "home") {
            currentRoute = design.defaultStartTab.route
            startTabApplied = true
        }
    }

    // Keep screen awake while fullscreen player / lyrics are open if enabled by user
    DisposableEffect(playerExpanded, design.keepScreenOnPlayer) {
        val window = (context as? Activity)?.window
        val keepAwake = playerExpanded && design.keepScreenOnPlayer
        if (keepAwake) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Runtime Microphone Permission Launcher for Shazam Recognition
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            vm.startShazamListening()
        }
    }

    fun triggerShazamListen() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            vm.startShazamListening()
        } else {
            vm.openShazam()
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // SINGLE HazeState strictly for the floating MiniPlayerBar & NavBar (120fps zero-lag!)
    val contentHazeState = remember { HazeState() }

    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }
    val downloadedIds = remember(downloads) { downloads.map { it.id }.toSet() }
    val blockedKeys = remember(blockedArtists) { blockedArtists.map { it.artistKey }.toSet() }

    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
    val navBarInsetDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value.toInt()
    val bottomClearance = navBarInsetDp + if (playback.currentTrack != null && !isKeyboardOpen) {
        (design.miniPlayerHeightDp + 126f).toInt()
    } else if (!isKeyboardOpen) {
        118
    } else {
        24
    }

    CroixPrimeTheme(
        design = design,
        albumSeedPrimary = albumPrimary,
        albumSeedSecondary = albumSecondary,
    ) {
        val strings = LocalCroixStrings.current
        val navDestinations = remember(strings) {
            listOf(
                CroixNavDestination("home", strings.navHome, Icons.Rounded.Home),
                CroixNavDestination("search", strings.navSearch, Icons.Rounded.Search),
                CroixNavDestination("favorites", strings.navFavorites, Icons.Rounded.Favorite),
            )
        }
        val animDurationMs = (260f / design.transitionSpeed).toInt().coerceIn(90, 500)

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = contentHazeState),
            ) {
                CroixAnimatedBackdrop()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
                    val routeOrder = mapOf("home" to 0, "search" to 1, "favorites" to 2)
                    AnimatedContent(
                        targetState = currentRoute,
                        transitionSpec = {
                            val forward = (routeOrder[targetState] ?: 0) >= (routeOrder[initialState] ?: 0)
                            val dir = if (forward) 1 else -1
                            when (design.transitionStyle) {
                                PrimeTransitionStyle.NONE -> fadeIn(tween(1)).togetherWith(fadeOut(tween(1)))
                                PrimeTransitionStyle.GENTLE_FADE -> fadeIn(tween(animDurationMs)).togetherWith(fadeOut(tween(animDurationMs / 2)))
                                PrimeTransitionStyle.SNAPPY_ZOOM ->
                                    (fadeIn(tween(animDurationMs, easing = FastOutSlowInEasing)) +
                                        scaleIn(tween(animDurationMs, easing = FastOutSlowInEasing), initialScale = 0.94f))
                                        .togetherWith(
                                            fadeOut(tween((animDurationMs * 0.7f).toInt())) +
                                                scaleOut(tween((animDurationMs * 0.7f).toInt()), targetScale = 1.03f),
                                        )
                                PrimeTransitionStyle.SMOOTH_SLIDE ->
                                    (fadeIn(tween(animDurationMs, easing = FastOutSlowInEasing)) +
                                        slideInHorizontally(tween(animDurationMs, easing = FastOutSlowInEasing)) { dir * (it / 10) } +
                                        scaleIn(tween(animDurationMs, easing = FastOutSlowInEasing), initialScale = 0.98f))
                                        .togetherWith(
                                            fadeOut(tween((animDurationMs * 0.7f).toInt(), easing = FastOutSlowInEasing)) +
                                                slideOutHorizontally(tween((animDurationMs * 0.8f).toInt(), easing = FastOutSlowInEasing)) { -dir * (it / 12) },
                                        )
                            }
                        },
                        label = "main_tab_transition",
                    ) { route ->
                        when (route) {
                            "home" -> HomeScreen(
                                feed = homeFeed,
                                recentTracks = history,
                                moods = vm.moods,
                                favoriteIds = favoriteIds,
                                ytMusicConnected = serverState.isYtMusicConnected,
                                spotifyConnected = serverState.isSpotifyConnected,
                                aiReady = serverState.aiConfig.isReady,
                                aiCurator = aiCurator,
                                privateSessionEnabled = design.privateSessionEnabled,
                                hiddenGemsLoading = hiddenGemsLoading,
                                blindDiscoveryLoading = blindDiscoveryLoading,
                                isLoading = homeLoading,
                                bottomPaddingDp = bottomClearance,
                                onPlayFlow = vm::startCroixFlow,
                                onStartHiddenGems = vm::startHiddenGemsRadar,
                                onStartBlindDiscovery = {
                                    vm.startBlindDiscoveryMode()
                                    playerExpanded = true
                                },
                                onGenerateAiCurator = vm::generateAiRecommendations,
                                onPlayAiCurated = vm::playAiCuratedList,
                                onOpenShazam = { triggerShazamListen() },
                                onTogglePrivateSession = {
                                    vm.updateDesign { it.copy(privateSessionEnabled = !it.privateSessionEnabled) }
                                },
                                onPlayTrack = vm::playTrack,
                                onToggleFavorite = vm::toggleFavorite,
                                onOpenTrackMenu = { trackForMenu = it },
                                onOpenArtist = { name, bid -> vm.openArtist(name, bid) },
                                onRefresh = vm::refreshHome,
                                onOpenSettings = {
                                    vm.refreshCacheSize()
                                    settingsOpen = true
                                },
                            )
                            "search" -> SearchScreen(
                                query = searchQuery,
                                activeFilter = searchFilter,
                                results = searchResults,
                                artistResults = artistSearchResults,
                                albumResults = albumSearchResults,
                                recentSearches = recentSearches,
                                favoriteIds = favoriteIds,
                                downloadedIds = downloadedIds,
                                downloadProgress = downloadProgress,
                                isLoading = searchLoading,
                                bottomPaddingDp = bottomClearance,
                                onQueryChange = vm::onSearchQueryChange,
                                onFilterChange = vm::onSearchFilterChange,
                                onDeleteRecentSearch = vm::deleteRecentSearch,
                                onClearRecentSearches = vm::clearRecentSearches,
                                onPlayTrack = vm::playTrack,
                                onToggleFavorite = vm::toggleFavorite,
                                onOpenTrackMenu = { trackForMenu = it },
                                onOpenArtist = { name, bid -> vm.openArtist(name, bid) },
                                onOpenAlbum = vm::openAlbum,
                                onOpenShazam = { triggerShazamListen() },
                                onOpenSettings = {
                                    vm.refreshCacheSize()
                                    settingsOpen = true
                                },
                            )
                            "favorites" -> FavoritesScreen(
                                favorites = favorites,
                                historyEntities = historyEntities,
                                downloads = downloads,
                                playlists = playlists,
                                playlistTracks = playlistTracks,
                                stats = stats,
                                downloadProgress = downloadProgress,
                                importStatus = importStatus,
                                bottomPaddingDp = bottomClearance,
                                onPlayTrack = vm::playTrack,
                                onToggleFavorite = vm::toggleFavorite,
                                onDeleteDownload = vm::deleteDownload,
                                onCreatePlaylist = vm::createPlaylist,
                                onImportPlaylistUrl = vm::importPlaylistUrl,
                                onClearImportStatus = vm::clearImportStatus,
                                onDeletePlaylist = vm::deletePlaylist,
                                onShareMusicalReceipt = vm::shareMusicalReceipt,
                                onOpenTrackMenu = { trackForMenu = it },
                                onOpenArtist = { vm.openArtist(it) },
                                onOpenSettings = {
                                    vm.refreshCacheSize()
                                    settingsOpen = true
                                },
                            )
                        }
                    }
                }
            }

            // Translucent Artist Profile Overlay
            AnimatedVisibility(
                visible = artistProfile != null && albumDetail == null && !playerExpanded && !settingsOpen && !shazamState.isOpen,
                enter = fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 5 } +
                    scaleIn(tween(300, easing = FastOutSlowInEasing), initialScale = 0.97f),
                exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { it / 6 },
                modifier = Modifier.zIndex(4f),
            ) {
                val prof = artistProfile
                if (prof != null) {
                    BackHandler { vm.closeArtist() }
                    val isBlocked = HybridAlgorithmEngine.isBlockedArtist(prof.name, blockedKeys)
                    ArtistProfileOverlay(
                        profile = prof,
                        isLoading = artistLoading,
                        favoriteIds = favoriteIds,
                        isArtistBlocked = isBlocked,
                        bottomPaddingDp = bottomClearance,
                        onClose = vm::closeArtist,
                        onToggleBlockArtist = { name ->
                            if (isBlocked) vm.unblockArtist(HybridAlgorithmEngine.normalizeArtistKey(name))
                            else vm.blockArtist(name)
                        },
                        onPlayTrack = vm::playTrack,
                        onToggleFavorite = vm::toggleFavorite,
                        onOpenTrackMenu = { trackForMenu = it },
                        onOpenAlbum = vm::openAlbum,
                        onOpenRelatedArtist = { rel -> vm.openArtist(rel.name, rel.browseId) },
                    )
                }
            }

            // Album / Single Detail Overlay
            AnimatedVisibility(
                visible = albumDetail != null && !playerExpanded && !settingsOpen && !shazamState.isOpen,
                enter = fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 5 } +
                    scaleIn(tween(300, easing = FastOutSlowInEasing), initialScale = 0.97f),
                exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { it / 6 },
                modifier = Modifier.zIndex(5f),
            ) {
                val alb = albumDetail
                if (alb != null) {
                    BackHandler { vm.closeAlbum() }
                    AlbumDetailOverlay(
                        album = alb,
                        isLoading = albumLoading,
                        favoriteIds = favoriteIds,
                        bottomPaddingDp = bottomClearance,
                        onClose = vm::closeAlbum,
                        onPlayTrack = vm::playTrack,
                        onToggleFavorite = vm::toggleFavorite,
                        onOpenTrackMenu = { trackForMenu = it },
                    )
                }
            }

            // Floating Mini-Player + 3-Tab Glass NavBar (the ONLY real Haze blur targets -> 120fps!)
            AnimatedVisibility(
                visible = !playerExpanded && !settingsOpen && !shazamState.isOpen && !isKeyboardOpen,
                enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 2 },
                exit = fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                    slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(6f)
                    .navigationBarsPadding(),
            ) {
                Column {
                    if (playback.currentTrack != null) {
                        MiniPlayerBar(
                            playback = playback,
                            positionMsFlow = vm.positionMs,
                            hazeState = contentHazeState,
                            onExpand = { playerExpanded = true },
                            onTogglePlay = vm.playerController::togglePlayPause,
                            onNext = vm.playerController::skipNext,
                            onPrev = vm.playerController::skipPrevious,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    CroixNavigationBar(
                        destinations = navDestinations,
                        currentRoute = currentRoute,
                        hazeState = contentHazeState,
                        onSelect = {
                            vm.closeAlbum()
                            vm.closeArtist()
                            currentRoute = it
                        },
                    )
                }
            }

            // Fullscreen Translucent Now Playing Overlay
            AnimatedVisibility(
                visible = playerExpanded && !settingsOpen && !shazamState.isOpen && playback.currentTrack != null,
                enter = fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it },
                exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                    slideOutVertically(tween(280, easing = FastOutSlowInEasing)) { it },
                modifier = Modifier.zIndex(10f),
            ) {
                BackHandler { playerExpanded = false }
                NowPlayingOverlay(
                    playback = playback,
                    positionMsFlow = vm.positionMs,
                    lyrics = lyrics,
                    lyricsLoading = lyricsLoading,
                    translatedLyrics = translatedLyrics,
                    translatingLyrics = translatingLyrics,
                    translationProviderLabel = translationProviderLabel,
                    isFavorite = playback.currentTrack?.id in favoriteIds,
                    isDownloaded = playback.currentTrack?.id in downloadedIds,
                    downloadProgress = playback.currentTrack?.id?.let { downloadProgress[it] },
                    onCollapse = { playerExpanded = false },
                    onTogglePlay = vm.playerController::togglePlayPause,
                    onSeekFraction = vm.playerController::seekToFraction,
                    onSeekMs = vm.playerController::seekTo,
                    onSeekRelative = vm.playerController::seekRelative,
                    onNext = vm.playerController::skipNext,
                    onPrev = vm.playerController::skipPrevious,
                    onCycleQueueMode = vm::cycleQueuePlayMode,
                    onToggleKaraoke = vm.playerController::setKaraokeEnabled,
                    onSetKaraokeStrength = vm.playerController::setKaraokeStrength,
                    onRevealBlindTrack = vm.playerController::revealBlindTrackNow,
                    onExitBlindMode = vm.playerController::exitBlindDiscoveryMode,
                    onToggleFavorite = vm::toggleFavorite,
                    onDownloadTrack = vm::downloadTrack,
                    onRequestAddToPlaylist = { trackForPlaylistDialog = it },
                    onOpenArtist = { artist ->
                        playerExpanded = false
                        vm.openArtist(artist)
                    },
                    onSelectQueueIndex = vm.playerController::playAtIndex,
                    onRemoveQueueIndex = vm.playerController::removeFromQueue,
                    onMoveQueueItem = vm.playerController::moveQueueItem,
                    onDeduplicateQueue = vm.playerController::deduplicateQueue,
                    onSaveQueueAsPlaylist = vm::saveCurrentQueueAsPlaylist,
                    onTranslateLyrics = vm::translateCurrentLyrics,
                    onAdjustLyricsOffset = { ms -> vm.updateDesign { it.copy(lyricsOffsetMs = ms) } },
                    onShareLyricCard = vm::shareLyricCard,
                    onSetSleepTimer = vm.playerController::startSleepTimer,
                    onSetSpeed = vm.playerController::setPlaybackSpeed,
                    onSetPitch = vm.playerController::setPlaybackPitch,
                    onSetSkipSilence = vm.playerController::setSkipSilence,
                    onSetAudio8dEnabled = vm.playerController::setAudio8dEnabled,
                    onSetAudio8dSpeed = vm.playerController::setAudio8dSpeed,
                    onSetReverbMode = vm.playerController::setReverbMode,
                    onApplyAutoEq = vm.playerController::applyAutoEqProfile,
                    onSetEqEnabled = vm.playerController::setEqEnabled,
                    onApplyEqPreset = vm.playerController::applyEqPreset,
                    onSetEqBand = vm.playerController::setEqBand,
                    onSetBassBoost = vm.playerController::setBassBoost,
                    onSetLoudness = vm.playerController::setLoudnessGain,
                    onOpenSettings = {
                        vm.refreshCacheSize()
                        settingsOpen = true
                    },
                )
            }

            // Top-Right Settings Overlay
            AnimatedVisibility(
                visible = settingsOpen,
                enter = fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 4 } +
                    scaleIn(tween(300, easing = FastOutSlowInEasing), initialScale = 0.97f),
                exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { it / 5 } +
                    scaleOut(tween(220, easing = FastOutSlowInEasing), targetScale = 0.98f),
                modifier = Modifier.zIndex(15f),
            ) {
                BackHandler { settingsOpen = false }
                SettingsAndStudioOverlay(
                    playback = playback,
                    serverState = serverState,
                    serverHealth = serverHealth,
                    adsBlockedCount = adsBlockedCount,
                    spotifySyncing = spotifySyncing,
                    ytMusicSyncing = ytMusicSyncing,
                    serverNotice = serverNotice,
                    updateInfo = updateInfo,
                    audioCacheBytes = audioCacheBytes,
                    blockedArtists = blockedArtists,
                    onClose = { settingsOpen = false },
                    onUpdateDesign = vm::updateDesign,
                    onApplyPreset = vm::applyPreset,
                    onResetDesign = vm::resetDesignToDefaults,
                    onSelectAppIcon = vm::selectAppIcon,
                    onSetSpeed = vm.playerController::setPlaybackSpeed,
                    onSetPitch = vm.playerController::setPlaybackPitch,
                    onSetSkipSilence = vm.playerController::setSkipSilence,
                    onSetKaraokeEnabled = vm.playerController::setKaraokeEnabled,
                    onSetKaraokeStrength = vm.playerController::setKaraokeStrength,
                    onSetAudio8dEnabled = vm.playerController::setAudio8dEnabled,
                    onSetAudio8dSpeed = vm.playerController::setAudio8dSpeed,
                    onSetReverbMode = vm.playerController::setReverbMode,
                    onApplyAutoEq = vm.playerController::applyAutoEqProfile,
                    onSetEqEnabled = vm.playerController::setEqEnabled,
                    onApplyEqPreset = vm.playerController::applyEqPreset,
                    onSetEqBand = vm.playerController::setEqBand,
                    onSetBassBoost = vm.playerController::setBassBoost,
                    onSetLoudness = vm.playerController::setLoudnessGain,
                    onClearAudioCache = vm::clearAudioCache,
                    onRefreshCacheSize = vm::refreshCacheSize,
                    onUnblockArtist = vm::unblockArtist,
                    onUpdateIntegrations = vm::updateIntegrations,
                    onLoginLastFm = vm::authenticateLastFm,
                    onUpdateAiConfig = vm::updateAiConfig,
                    onTestAiConnection = vm::testAiConnection,
                    onUpdateServerConfig = vm::updateServerConfig,
                    onPingServer = vm::pingServer,
                    onOpenYtMusicLogin = {
                        ytMusicAddAnother = false
                        ytMusicLoginOpen = true
                    },
                    onOpenAddYtMusicAccount = {
                        ytMusicAddAnother = true
                        ytMusicLoginOpen = true
                    },
                    onSyncYtMusicNow = { vm.syncYtMusicLibrary(silent = false) },
                    onRefreshYtMusicProfiles = vm::refreshYtMusicBrandProfiles,
                    onSwitchYtMusicProfile = vm::switchYtMusicBrandProfile,
                    onSwitchSavedYtAccount = vm::switchSavedYtMusicAccount,
                    onRemoveSavedYtAccount = vm::removeSavedYtMusicAccount,
                    onImportYtMusicPlaylist = vm::importYtMusicPlaylist,
                    onDisconnectYtMusic = vm::disconnectYtMusic,
                    onOpenSpotifyLogin = { spotifyLoginOpen = true },
                    onConnectSpotifyManual = { dc -> vm.connectSpotifyCookies(dc) },
                    onSyncSpotifyNow = vm::syncSpotifyTaste,
                    onSetSpotifyBlendWeight = vm::setSpotifyBlendWeight,
                    onApplyAlgorithmBlend = {
                        vm.refreshHome()
                        settingsOpen = false
                        currentRoute = "home"
                    },
                    onDisconnectSpotify = vm::disconnectSpotify,
                    onImportSpotifyPlaylist = vm::importSpotifyPlaylist,
                    onCheckUpdates = vm::checkForUpdates,
                )
            }

            // Shazam Acoustic Recognition Overlay
            AnimatedVisibility(
                visible = shazamState.isOpen,
                enter = fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.94f),
                exit = fadeOut(tween(180)) + scaleOut(tween(200), targetScale = 0.96f),
                modifier = Modifier.zIndex(18f),
            ) {
                BackHandler { vm.closeShazam() }
                ShazamRadarOverlay(
                    state = shazamState,
                    onStartListening = { triggerShazamListen() },
                    onPlayMatch = vm::playShazamMatch,
                    onClose = vm::closeShazam,
                )
            }

            // In-App Spotify Login WebView
            AnimatedVisibility(
                visible = spotifyLoginOpen,
                enter = fadeIn(tween(220)) + slideInVertically(tween(280)) { it / 3 },
                exit = fadeOut(tween(180)) + slideOutVertically(tween(220)) { it / 3 },
                modifier = Modifier.zIndex(20f),
            ) {
                SpotifyLoginOverlay(
                    onDismiss = { spotifyLoginOpen = false },
                    onCookiesCaptured = { spDc, spKey ->
                        spotifyLoginOpen = false
                        vm.connectSpotifyCookies(spDc, spKey)
                    },
                )
            }

            // In-App YouTube Music Account Login WebView
            AnimatedVisibility(
                visible = ytMusicLoginOpen,
                enter = fadeIn(tween(220)) + slideInVertically(tween(280)) { it / 3 },
                exit = fadeOut(tween(180)) + slideOutVertically(tween(220)) { it / 3 },
                modifier = Modifier.zIndex(20f),
            ) {
                YtMusicLoginOverlay(
                    isAddingAnotherAccount = ytMusicAddAnother,
                    onDismiss = { ytMusicLoginOpen = false },
                    onAuthCaptured = { cookie, visitorData, dataSyncId, authUser ->
                        ytMusicLoginOpen = false
                        vm.connectYtMusicAccount(cookie, visitorData, dataSyncId, authUser)
                    },
                )
            }
        }

        // Quick Track Action Menu (⋮) — now includes Block Artist!
        val menuTrack = trackForMenu
        if (menuTrack != null) {
            AlertDialog(
                onDismissRequest = { trackForMenu = null },
                title = { Text(menuTrack.title, maxLines = 1) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrackActionItem(Icons.Rounded.QueuePlayNext, strings.playNext) {
                            vm.playerController.playNext(menuTrack)
                            trackForMenu = null
                        }
                        TrackActionItem(Icons.Rounded.AddToQueue, strings.addToQueue) {
                            vm.playerController.addToQueue(menuTrack)
                            trackForMenu = null
                        }
                        TrackActionItem(Icons.Rounded.Person, "${strings.goToArtist} (${menuTrack.artist})") {
                            trackForMenu = null
                            vm.openArtist(menuTrack.artist)
                        }
                        TrackActionItem(Icons.Rounded.Download, strings.downloadOffline) {
                            vm.downloadTrack(menuTrack)
                            trackForMenu = null
                        }
                        TrackActionItem(Icons.Rounded.PlaylistAdd, strings.addToPlaylist) {
                            trackForMenu = null
                            trackForPlaylistDialog = menuTrack
                        }
                        TrackActionItem(
                            Icons.Rounded.Block,
                            if (strings.isSpanish) "Bloquear artista (${menuTrack.artist})" else "Block artist (${menuTrack.artist})",
                        ) {
                            vm.blockArtist(menuTrack.artist)
                            trackForMenu = null
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { trackForMenu = null }) { Text(strings.close) }
                },
            )
        }

        // Add-to-Playlist Dialog
        val targetTrack = trackForPlaylistDialog
        if (targetTrack != null) {
            AlertDialog(
                onDismissRequest = { trackForPlaylistDialog = null },
                title = { Text(strings.addToPlaylist) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        playlists.forEach { pl ->
                            CroixGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 12.dp,
                                onClick = {
                                    vm.addTrackToPlaylist(pl.playlistId, targetTrack)
                                    trackForPlaylistDialog = null
                                },
                            ) {
                                Text(pl.name, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(12.dp))
                            }
                        }
                        OutlinedTextField(
                            value = quickNewPlaylistName,
                            onValueChange = { quickNewPlaylistName = it },
                            label = { Text(if (strings.isSpanish) "O crear nueva playlist" else "Or create new playlist") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = quickNewPlaylistName.isNotBlank(),
                        onClick = {
                            vm.createPlaylist(quickNewPlaylistName)
                            quickNewPlaylistName = ""
                        },
                    ) { Text(strings.create) }
                },
                dismissButton = {
                    TextButton(onClick = { trackForPlaylistDialog = null }) { Text(strings.close) }
                },
            )
        }
    }
}

@Composable
private fun TrackActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    CroixGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(12.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}
