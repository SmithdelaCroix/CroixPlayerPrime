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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.croix.player.prime.ui.CroixPrimeViewModel
import com.croix.player.prime.ui.screens.CroixStudioScreen
import com.croix.player.prime.ui.screens.HomeScreen
import com.croix.player.prime.ui.screens.LibraryScreen
import com.croix.player.prime.ui.screens.MiniPlayerBar
import com.croix.player.prime.ui.screens.NowPlayingOverlay
import com.croix.player.prime.ui.screens.SearchScreen
import com.croix.prime.design.CroixAnimatedBackdrop
import com.croix.prime.design.CroixNavDestination
import com.croix.prime.design.CroixNavigationBar
import com.croix.prime.design.CroixPrimeTheme
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
    val searchQuery by vm.searchQuery.collectAsState()
    val searchResults by vm.searchResults.collectAsState()
    val searchLoading by vm.searchLoading.collectAsState()
    val lyrics by vm.lyrics.collectAsState()
    val lyricsLoading by vm.lyricsLoading.collectAsState()
    val albumPrimary by vm.albumPrimary.collectAsState()
    val albumSecondary by vm.albumSecondary.collectAsState()

    var currentRoute by remember { mutableStateOf("home") }
    var playerExpanded by remember { mutableStateOf(false) }
    val backdropHazeState = remember { HazeState() }
    val contentHazeState = remember { HazeState() }

    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }
    val destinations = remember {
        listOf(
            CroixNavDestination("home", "Home", Icons.Rounded.Home),
            CroixNavDestination("search", "Search", Icons.Rounded.Search),
            CroixNavDestination("library", "Library", Icons.Rounded.LibraryMusic),
            CroixNavDestination("studio", "Studio", Icons.Rounded.Tune),
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
            // Content Haze source layer: living backdrop + active screen (for floating mini-player & nav bar)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = contentHazeState),
            ) {
                // Dedicated backdrop haze source (no recursion for screen cards!)
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
                            onOpenStudio = { currentRoute = "studio" },
                        )
                        "search" -> SearchScreen(
                            query = searchQuery,
                            results = searchResults,
                            favoriteIds = favoriteIds,
                            isLoading = searchLoading,
                            hazeState = backdropHazeState,
                            bottomPaddingDp = bottomClearance,
                            onQueryChange = vm::onSearchQueryChange,
                            onPlayTrack = vm::playTrack,
                            onToggleFavorite = vm::toggleFavorite,
                        )
                        "library" -> LibraryScreen(
                            favorites = favorites,
                            history = history,
                            hazeState = backdropHazeState,
                            bottomPaddingDp = bottomClearance,
                            onPlayTrack = vm::playTrack,
                            onToggleFavorite = vm::toggleFavorite,
                        )
                        "studio" -> CroixStudioScreen(
                            hazeState = backdropHazeState,
                            bottomPaddingDp = bottomClearance,
                            onUpdateDesign = vm::updateDesign,
                            onApplyPreset = vm::applyPreset,
                        )
                    }
                }
            }

            // Floating Mini-Player + Floating Translucent Glass NavBar (hidden cleanly during IME typing or fullscreen player!)
            AnimatedVisibility(
                visible = !playerExpanded && !isKeyboardOpen,
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

            // Fullscreen Translucent Now Playing Sheet (strictly top zIndex + touch blocker)
            AnimatedVisibility(
                visible = playerExpanded && playback.currentTrack != null,
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
                    onCollapse = { playerExpanded = false },
                    onTogglePlay = vm.playerController::togglePlayPause,
                    onSeekFraction = vm.playerController::seekToFraction,
                    onSeekMs = vm.playerController::seekTo,
                    onNext = vm.playerController::skipNext,
                    onPrev = vm.playerController::skipPrevious,
                    onToggleShuffle = vm.playerController::toggleShuffle,
                    onToggleRepeat = vm.playerController::toggleRepeatOne,
                    onToggleFavorite = vm::toggleFavorite,
                    onSelectQueueIndex = vm.playerController::playAtIndex,
                    onSetSleepTimer = vm.playerController::startSleepTimer,
                    onSetBassBoost = vm.playerController::setBassBoost,
                    onSetLoudness = vm.playerController::setLoudnessGain,
                    onOpenStudio = {
                        playerExpanded = false
                        currentRoute = "studio"
                    },
                )
            }
        }
    }
}
