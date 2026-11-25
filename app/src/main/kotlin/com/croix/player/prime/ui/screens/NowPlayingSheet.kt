/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.croix.prime.design.CroixAudioVisualizer
import com.croix.prime.design.CroixGlassCard
import com.croix.prime.design.CroixSeekBar
import com.croix.prime.design.LocalCroixDesign
import com.croix.prime.design.LocalCroixPalette
import com.croix.prime.design.PrimeLyricsAlign
import com.croix.prime.design.PrimePlayerStyle
import com.croix.prime.engine.LrcParser
import com.croix.prime.engine.LyricsResult
import com.croix.prime.engine.Track
import com.croix.prime.playback.EqPreset
import com.croix.prime.playback.PlaybackUiState
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

@Composable
fun MiniPlayerBar(
    playback: PlaybackUiState,
    hazeState: HazeState,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = playback.currentTrack ?: return
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val progress = if (playback.durationMs > 0L) {
        (playback.positionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val hPad = if (design.miniPlayerFloating) 16.dp else 0.dp
    val corner = if (design.miniPlayerFloating) design.cornerRadiusDp.dp else 0.dp

    CroixGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = hPad)
            .height(design.miniPlayerHeightDp.dp),
        hazeState = hazeState,
        cornerRadius = corner,
        opacityOverride = (design.glassOpacity + 0.12f).coerceAtMost(0.85f),
        onClick = onExpand,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (playback.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = palette.primary,
                    )
                } else {
                    IconButton(onClick = onTogglePlay) {
                        Icon(
                            if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White)
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = palette.primary,
                trackColor = Color.White.copy(alpha = 0.12f),
            )
        }
    }
}

@Composable
fun NowPlayingOverlay(
    playback: PlaybackUiState,
    lyrics: LyricsResult?,
    lyricsLoading: Boolean,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    onCollapse: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onSeekMs: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onRequestAddToPlaylist: (Track) -> Unit,
    onSelectQueueIndex: (Int) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onSetEqEnabled: (Boolean) -> Unit,
    onApplyEqPreset: (EqPreset) -> Unit,
    onSetEqBand: (Int, Float) -> Unit,
    onSetBassBoost: (Float) -> Unit,
    onSetLoudness: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val track = playback.currentTrack ?: return
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    var activePanel by remember { mutableIntStateOf(0) } // 0=Player, 1=Lyrics, 2=Queue, 3=EQ & Sleep
    var carMode by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    val spin = rememberInfiniteTransition(label = "vinyl")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )

    val progress = if (playback.durationMs > 0L) {
        (playback.positionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val touchBlocker = remember { MutableInteractionSource() }

    if (carMode) {
        CarAodModeOverlay(
            track = track,
            playback = playback,
            onExit = { carMode = false },
            onTogglePlay = onTogglePlay,
            onNext = onNext,
            onPrev = onPrev,
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable(interactionSource = touchBlocker, indication = null) {},
    ) {
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(if (activePanel == 1) design.lyricsBackdropBlurDp.dp else (design.glassBlurDp + 8f).dp),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.42f),
                            palette.background.copy(alpha = 0.78f),
                            palette.background.copy(alpha = 0.94f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CroixGlassCard(cornerRadius = 50.dp, onClick = onCollapse) {
                    IconButton(onClick = onCollapse, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close", tint = Color.White)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PLAYING NOW", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (playback.isPlayingOfflineFile) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.OfflinePin, null, tint = palette.secondary, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Offline", fontSize = 11.sp, color = palette.secondary)
                            }
                        }
                        if (playback.sponsorSegments.isNotEmpty() && design.sponsorBlockEnabled) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Shield, null, tint = palette.primary, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("SponsorBlock (${playback.sponsorSegments.size})", fontSize = 11.sp, color = palette.primary)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CroixGlassCard(cornerRadius = 50.dp, onClick = { carMode = true }) {
                        IconButton(onClick = { carMode = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.DirectionsCar, contentDescription = "Car / AOD Mode", tint = Color.White)
                        }
                    }
                    CroixGlassCard(cornerRadius = 50.dp, onClick = onOpenSettings) {
                        IconButton(onClick = onOpenSettings, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                }
            }

            if (playback.lastSkippedNotice != null) {
                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = palette.primary, cornerRadius = 14.dp) {
                    Text(
                        text = playback.lastSkippedNotice!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }

            // Center Dynamic Panel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (activePanel) {
                    0 -> {
                        val artShape = if (design.vinylSpinEnabled) CircleShape else RoundedCornerShape(design.artworkCornerDp.dp)
                        val artFill = when (design.playerStyle) {
                            PrimePlayerStyle.FULLSCREEN_COVER -> 0.94f
                            PrimePlayerStyle.MINIMAL_CLEAN -> 0.70f
                            else -> 0.84f
                        }
                        val rotation = if (design.vinylSpinEnabled && playback.isPlaying) angle else 0f

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CroixGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth(artFill)
                                    .aspectRatio(1f)
                                    .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
                                    .pointerInput(design.swipeArtworkToSkip) {
                                        if (!design.swipeArtworkToSkip) return@pointerInput
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (dragOffsetX < -130f) onNext()
                                                else if (dragOffsetX > 130f) onPrev()
                                                dragOffsetX = 0f
                                            },
                                            onDragCancel = { dragOffsetX = 0f },
                                            onHorizontalDrag = { _, dragAmount ->
                                                dragOffsetX = (dragOffsetX + dragAmount).coerceIn(-240f, 240f)
                                            },
                                        )
                                    }
                                    .pointerInput(design.doubleTapSeekSeconds) {
                                        detectTapGestures(
                                            onDoubleTap = { offset ->
                                                val delta = design.doubleTapSeekSeconds * 1_000L
                                                if (offset.x < size.width / 2f) onSeekRelative(-delta)
                                                else onSeekRelative(delta)
                                            },
                                        )
                                    },
                                cornerRadius = if (design.vinylSpinEnabled) 200.dp else design.artworkCornerDp.dp,
                            ) {
                                AsyncImage(
                                    model = track.thumbnailUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(artShape)
                                        .rotate(rotation),
                                )
                            }
                            CroixAudioVisualizer(
                                isPlaying = playback.isPlaying,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                    1 -> SyncedLyricsPanel(
                        lyrics = lyrics,
                        isLoading = lyricsLoading,
                        positionMs = playback.positionMs,
                        onSeekMs = onSeekMs,
                    )
                    2 -> QueuePanel(
                        queue = playback.queue,
                        currentIndex = playback.currentIndex,
                        onSelect = onSelectQueueIndex,
                    )
                    3 -> EqualizerAndSleepPanel(
                        playback = playback,
                        onSetSleepTimer = onSetSleepTimer,
                        onSetEqEnabled = onSetEqEnabled,
                        onApplyEqPreset = onApplyEqPreset,
                        onSetEqBand = onSetEqBand,
                        onSetBassBoost = onSetBassBoost,
                        onSetLoudness = onSetLoudness,
                    )
                }
            }

            // Translucent Control Dock
            CroixGlassCard(
                modifier = Modifier.fillMaxWidth(),
                opacityOverride = design.playerControlDockOpacity,
                cornerRadius = (design.cornerRadiusDp + 4f).coerceAtMost(34f).dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (downloadProgress != null) {
                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = palette.primary,
                            )
                        } else {
                            IconButton(onClick = { onDownloadTrack(track) }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                                    contentDescription = "Download",
                                    tint = if (isDownloaded) palette.primary else Color.White,
                                )
                            }
                        }
                        IconButton(onClick = { onRequestAddToPlaylist(track) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.PlaylistAdd, contentDescription = "Add to playlist", tint = Color.White)
                        }
                        IconButton(onClick = { onToggleFavorite(track) }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) palette.primary else Color.White,
                            )
                        }
                    }

                    CroixSeekBar(progress = progress, onSeek = onSeekFraction)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatTime(playback.positionMs), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        Text(formatTime(playback.durationMs), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onToggleShuffle) {
                            Icon(
                                Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (playback.shuffleEnabled) palette.primary else palette.textSecondary,
                            )
                        }
                        IconButton(onClick = onPrev) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(palette.primary, palette.secondary)))
                                .clickable(onClick = onTogglePlay),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                        IconButton(onClick = onNext) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        IconButton(onClick = onToggleRepeat) {
                            Icon(
                                if (playback.repeatOne) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                contentDescription = "Repeat",
                                tint = if (playback.repeatOne) palette.primary else palette.textSecondary,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        PanelToggleIcon(Icons.Rounded.Lyrics, "Lyrics", activePanel == 1) {
                            activePanel = if (activePanel == 1) 0 else 1
                        }
                        PanelToggleIcon(Icons.Rounded.QueueMusic, "Queue", activePanel == 2) {
                            activePanel = if (activePanel == 2) 0 else 2
                        }
                        PanelToggleIcon(Icons.Rounded.Equalizer, "EQ & Sleep", activePanel == 3) {
                            activePanel = if (activePanel == 3) 0 else 3
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CarAodModeOverlay(
    track: Track,
    playback: PlaybackUiState,
    onExit: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
) {
    val palette = LocalCroixPalette.current
    val blocker = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = blocker, indication = null) {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("CAR / AOD MODE", color = palette.primary, fontWeight = FontWeight.Black)
            CroixGlassCard(cornerRadius = 50.dp, onClick = onExit) {
                Text("Exit", color = Color.White, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(track.title, style = MaterialTheme.typography.headlineLarge, color = Color.White, textAlign = TextAlign.Center, maxLines = 2)
            Text(track.artist, style = MaterialTheme.typography.titleLarge, color = palette.textSecondary, textAlign = TextAlign.Center, maxLines = 1)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.size(76.dp)) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(56.dp))
            }
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(palette.primary)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(58.dp),
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(76.dp)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(56.dp))
            }
        }
    }
}

@Composable
private fun PanelToggleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalCroixPalette.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) palette.primary.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = if (active) palette.primary else palette.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 12.sp, color = if (active) Color.White else palette.textSecondary)
    }
}

@Composable
private fun SyncedLyricsPanel(
    lyrics: LyricsResult?,
    isLoading: Boolean,
    positionMs: Long,
    onSeekMs: (Long) -> Unit,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val align = when (design.lyricsAlign) {
        PrimeLyricsAlign.START -> TextAlign.Start
        PrimeLyricsAlign.CENTER -> TextAlign.Center
        PrimeLyricsAlign.END -> TextAlign.End
    }

    if (isLoading) {
        CircularProgressIndicator(color = palette.primary)
        return
    }
    if (lyrics == null || (lyrics.synced.isEmpty() && lyrics.plain.isBlank())) {
        Text("No lyrics found on LRCLib for this track.", color = palette.textSecondary)
        return
    }
    if (lyrics.synced.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
            item {
                Text(
                    text = lyrics.plain,
                    fontSize = design.lyricsFontSizeSp.sp,
                    color = Color.White,
                    textAlign = align,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        return
    }

    val activeIdx = LrcParser.activeLineIndex(lyrics.synced, positionMs)
    val listState = rememberLazyListState()
    LaunchedEffect(activeIdx) {
        if (activeIdx >= 0) {
            listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        itemsIndexed(lyrics.synced) { idx, line ->
            val isCurrent = idx == activeIdx
            Text(
                text = line.text,
                fontSize = if (isCurrent) (design.lyricsFontSizeSp + 2f).sp else design.lyricsFontSizeSp.sp,
                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.42f),
                textAlign = align,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeekMs(line.timeMs) },
            )
        }
    }
}

@Composable
private fun QueuePanel(
    queue: List<Track>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val palette = LocalCroixPalette.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(queue) { idx, t ->
            CroixGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentTint = if (idx == currentIndex) palette.primary else null,
                onClick = { onSelect(idx) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = t.thumbnailUrl,
                        contentDescription = t.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(t.artist, color = palette.textSecondary, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualizerAndSleepPanel(
    playback: PlaybackUiState,
    onSetSleepTimer: (Int) -> Unit,
    onSetEqEnabled: (Boolean) -> Unit,
    onApplyEqPreset: (EqPreset) -> Unit,
    onSetEqBand: (Int, Float) -> Unit,
    onSetBassBoost: (Float) -> Unit,
    onSetLoudness: (Int) -> Unit,
) {
    val palette = LocalCroixPalette.current
    val bandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")

    CroixGlassCard(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("5-Band Hardware Equalizer", color = Color.White, fontWeight = FontWeight.Bold)
                Switch(checked = playback.eqEnabled, onCheckedChange = onSetEqEnabled)
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EqPreset.entries.filter { it != EqPreset.CUSTOM }.forEach { preset ->
                    CroixGlassCard(
                        cornerRadius = 12.dp,
                        accentTint = if (playback.eqPreset == preset && playback.eqEnabled) palette.primary else null,
                        onClick = { onApplyEqPreset(preset) },
                    ) {
                        Text(preset.label, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }
            playback.eqBandsDb.forEachIndexed { idx, gain ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(bandLabels.getOrElse(idx) { "B${idx + 1}" }, color = palette.textSecondary, fontSize = 11.sp, modifier = Modifier.width(46.dp))
                    Slider(
                        value = gain,
                        onValueChange = { onSetEqBand(idx, it) },
                        valueRange = -12f..12f,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${gain.roundToInt()}dB", color = palette.primary, fontSize = 11.sp, modifier = Modifier.width(38.dp))
                }
            }

            Text("Bass Boost: ${(playback.bassBoostLevel * 100).roundToInt()}%", color = Color.White, fontSize = 13.sp)
            Slider(value = playback.bassBoostLevel, onValueChange = onSetBassBoost, valueRange = 0f..1f)

            Text("Loudness Boost: +${playback.loudnessGainMb / 100} dB", color = Color.White, fontSize = 13.sp)
            Slider(value = playback.loudnessGainMb.toFloat(), onValueChange = { onSetLoudness(it.roundToInt()) }, valueRange = 0f..1200f)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bedtime, null, tint = palette.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (playback.sleepTimerRemainingSec > 0) {
                        "Sleep Timer (${playback.sleepTimerRemainingSec / 60}m ${playback.sleepTimerRemainingSec % 60}s)"
                    } else "Sleep Timer (Fade-out)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "60m").forEach { (mins, lbl) ->
                    CroixGlassCard(cornerRadius = 12.dp, onClick = { onSetSleepTimer(mins) }) {
                        Text(lbl, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}
