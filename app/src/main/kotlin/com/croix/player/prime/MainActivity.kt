/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.croix.player.prime.ui.CroixPrimeViewModel
import com.croix.player.prime.ui.screens.FavoritesScreen
import com.croix.player.prime.ui.screens.HomeScreen
import com.croix.player.prime.ui.screens.MiniPlayerBar
import com.croix.player.prime.ui.screens.NowPlayingOverlay
import com.croix.player.prime.ui.screens.SearchScreen
import com.croix.player.prime.ui.screens.SettingsAndStudioOverlay
import com.croix.prime.design.CroixAnimatedBackdrop
import com.croix.prime.design.CroixGlassCard
import com.croix.prime.design.CroixNavDestination
import com.croix.prime.design.CroixNavigationBar
import com.croix.prime.design.CroixPrimeTheme
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
    val design by vm.designState.collectAsState()
    val playback by vm.playbackState.collectAsState()
    val homeSections by vm.homeSections.collectAsState()
    val homeLoading by vm.homeLoading.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val history by vm.history.collectAsState()
    val downloads by vm.downloads.collectAsState()
    val downloadProgress by vm.downloadProgress.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val playlistTracks by vm.playlistTracks.collectAsState()
    val stats by vm.listeningStats.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val searchResults by vm.searchResults.collectAsState()
    val searchLoading by vm.searchLoading.collectAsState()
    val lyrics by vm.lyrics.collectAsState()
    val lyricsLoading by vm.lyricsLoading.collectAsState()
    val albumPrimary by vm.albumPrimary.collectAsState()
    val albumSecondary by vm.albumSecondary.collectAsState()

    var currentRoute by remember { mutableStateOf("home") }
    var playerExpanded by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var trackForPlaylistDialog by remember { mutableStateOf<Track?>(null) }
    var quickNewPlaylistName by remember { mutableStateOf("") }

    val backdropHazeState = remember { HazeState() }
    val contentHazeState = remember { HazeState() }

    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }
    val downloadedIds = remember(downloads) { downloads.map { it.id }.toSet() }

    // Clean 3-tab bottom bar (Settings & Studio live in the Top-Right Gear Icon!)
    val destinations = remember {
        listOf(
            CroixNavDestination("home", "Home", Icons.Rounded.Home),
            CroixNavDestination("search", "Search", Icons.Rounded.Search),
            CroixNavDestination("favorites", "Favorites", Icons.Rounded.Favorite),
        )
    }

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
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = contentHazeState),
            ) {
                CroixAnimatedBackdrop(
                    modifier = Modifier.hazeSource(state = backdropHazeState),
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
                    when (currentRoute) {
                        "home" -> HomeScreen(
                            sections = homeSections,
                            recentTracks = history,
                            moods = vm.moods,
                            isLoading = homeLoading,
                            hazeState = backdropHazeState,
                            bottomPaddingDp = bottomClearance,
                            onPlayFlow = vm::startCroixFlow,
                            onPlayMood = vm::playMood,
                            onPlayTrack = vm::playTrack,
                            onRefresh = vm::refreshHome,
                            onOpenSettings = { settingsOpen = true },
                        )
                        "search" -> SearchScreen(
                            query = searchQuery,
                            results = searchResults,
                            favoriteIds = favoriteIds,
                            downloadedIds = downloadedIds,
                            downloadProgress = downloadProgress,
                            isLoading = searchLoading,
                            hazeState = backdropHazeState,
                            bottomPaddingDp = bottomClearance,
                            onQueryChange = vm::onSearchQueryChange,
                            onPlayTrack = vm::playTrack,
                            onToggleFavorite = vm::toggleFavorite,
                            onDownloadTrack = vm::downloadTrack,
                            onRequestAddToPlaylist = { trackForPlaylistDialog = it },
                            onOpenSettings = { settingsOpen = true },
                        )
                        "favorites" -> FavoritesScreen(
                            favorites = favorites,
                            downloads = downloads,
                            playlists = playlists,
                            playlistTracks = playlistTracks,
                            stats = stats,
                            downloadProgress = downloadProgress,
                            hazeState = backdropHazeState,
                            bottomPaddingDp = bottomClearance,
                            onPlayTrack = vm::playTrack,
                            onToggleFavorite = vm::toggleFavorite,
                            onDownloadTrack = vm::downloadTrack,
                            onDeleteDownload = vm::deleteDownload,
                            onCreatePlaylist = vm::createPlaylist,
                            onDeletePlaylist = vm::deletePlaylist,
                            onRequestAddToPlaylist = { trackForPlaylistDialog = it },
                            onOpenSettings = { settingsOpen = true },
                        )
                    }
                }
            }

            // Floating Mini-Player + 3-Tab Glass NavBar (hidden cleanly during IME, Player, or Settings)
            AnimatedVisibility(
                visible = !playerExpanded && !settingsOpen && !isKeyboardOpen,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(5f)
                    .navigationBarsPadding(),
            ) {
                Column {
                    if (playback.currentTrack != null) {
                        MiniPlayerBar(
                            playback = playback,
                            hazeState = contentHazeState,
                            onExpand = { playerExpanded = true },
                            onTogglePlay = vm.playerController::togglePlayPause,
                            onNext = vm.playerController::skipNext,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    CroixNavigationBar(
                        destinations = destinations,
                        currentRoute = currentRoute,
                        hazeState = contentHazeState,
                        onSelect = { currentRoute = it },
                    )
                }
            }

            // Fullscreen Translucent Now Playing Overlay
            AnimatedVisibility(
                visible = playerExpanded && !settingsOpen && playback.currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.zIndex(10f),
            ) {
                BackHandler { playerExpanded = false }
                NowPlayingOverlay(
                    playback = playback,
                    lyrics = lyrics,
                    lyricsLoading = lyricsLoading,
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
                    onToggleShuffle = vm.playerController::toggleShuffle,
                    onToggleRepeat = vm.playerController::toggleRepeatOne,
                    onToggleFavorite = vm::toggleFavorite,
                    onDownloadTrack = vm::downloadTrack,
                    onRequestAddToPlaylist = { trackForPlaylistDialog = it },
                    onSelectQueueIndex = vm.playerController::playAtIndex,
                    onSetSleepTimer = vm.playerController::startSleepTimer,
                    onSetEqEnabled = vm.playerController::setEqEnabled,
                    onApplyEqPreset = vm.playerController::applyEqPreset,
                    onSetEqBand = vm.playerController::setEqBand,
                    onSetBassBoost = vm.playerController::setBassBoost,
                    onSetLoudness = vm.playerController::setLoudnessGain,
                    onOpenSettings = { settingsOpen = true },
                )
            }

            // Top-Right Settings & Croix Studio Overlay
            AnimatedVisibility(
                visible = settingsOpen,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 }),
                modifier = Modifier.zIndex(15f),
            ) {
                BackHandler { settingsOpen = false }
                SettingsAndStudioOverlay(
                    playback = playback,
                    onClose = { settingsOpen = false },
                    onUpdateDesign = vm::updateDesign,
                    onApplyPreset = vm::applyPreset,
                    onSetEqEnabled = vm.playerController::setEqEnabled,
                    onApplyEqPreset = vm.playerController::applyEqPreset,
                    onSetEqBand = vm.playerController::setEqBand,
                )
            }
        }

        // Add-to-Playlist Dialog
        val targetTrack = trackForPlaylistDialog
        if (targetTrack != null) {
            AlertDialog(
                onDismissRequest = { trackForPlaylistDialog = null },
                title = { Text("Add to Playlist") },
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
                            label = { Text("Or create new playlist") },
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
                    ) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { trackForPlaylistDialog = null }) { Text("Close") }
                },
            )
        }
    }
}
