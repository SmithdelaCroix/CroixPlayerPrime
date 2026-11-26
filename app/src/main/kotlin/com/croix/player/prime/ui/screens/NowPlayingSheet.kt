/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.croix.prime.design.CroixAudioVisualizer
import com.croix.prime.design.CroixGlassCard
import com.croix.prime.design.CroixLiveMeshBackdrop
import com.croix.prime.design.CroixSeekBar
import com.croix.prime.design.LocalCroixDesign
import com.croix.prime.design.LocalCroixPalette
import com.croix.prime.design.LocalCroixStrings
import com.croix.prime.design.PrimeArtworkShape
import com.croix.prime.design.PrimeLyricsAlign
import com.croix.prime.design.PrimeLyricsAnimStyle
import com.croix.prime.design.PrimeMiniPlayerStyle
import com.croix.prime.design.PrimePlayerBackgroundStyle
import com.croix.prime.design.PrimePlayerButtonStyle
import com.croix.prime.design.PrimePlayerStyle
import com.croix.prime.design.PrimeTransitionStyle
import com.croix.prime.design.resolveArtworkShape
import com.croix.prime.engine.LrcParser
import com.croix.prime.engine.LyricsResult
import com.croix.prime.engine.LyricsRomanizer
import com.croix.prime.engine.Track
import com.croix.prime.playback.AutoEqHeadphoneProfile
import com.croix.prime.playback.EqPreset
import com.croix.prime.playback.PlaybackUiState
import com.croix.prime.playback.QueuePlayMode
import com.croix.prime.playback.ReverbPresetMode
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/**
 * Real-time calibrated 3D Gyroscope + Gravity Parallax sensor state (BETA).
 * Auto-zeroes to the user's natural holding posture on activation so tilting in any direction
 * produces immediate, silky 120fps 3D card tilt + inner artwork parallax + specular light sheen!
 */
internal data class ParallaxTilt(
    val rotX: Float = 0f,
    val rotY: Float = 0f,
    val innerShiftX: Float = 0f,
    val innerShiftY: Float = 0f,
    val sheenNormX: Float = 0.5f,
    val sheenNormY: Float = 0.5f,
    val sensorActive: Boolean = false,
)

@Composable
internal fun rememberParallaxTiltState(
    enabled: Boolean,
    intensity: Float,
): State<ParallaxTilt> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(ParallaxTilt()) }

    DisposableEffect(enabled, intensity) {
        if (!enabled) {
            tiltState.value = ParallaxTilt(sensorActive = false)
            return@DisposableEffect onDispose {}
        }
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val gravitySensor = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroSensor = sm?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (gravitySensor == null && gyroSensor == null) {
            tiltState.value = ParallaxTilt(sensorActive = false)
            return@DisposableEffect onDispose {}
        }

        val scale = intensity.coerceIn(0.5f, 2.2f)
        val maxDeg = 16f * scale

        var baseGx = 0f
        var baseGy = 0f
        var baselineSamples = 0
        var smoothX = 0f
        var smoothY = 0f
        var gyroBoostX = 0f
        var gyroBoostY = 0f
        var lastGyroTs = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> {
                        val gx = event.values.getOrNull(0) ?: 0f
                        val gy = event.values.getOrNull(1) ?: 0f
                        if (baselineSamples < 6) {
                            baseGx = (baseGx * baselineSamples + gx) / (baselineSamples + 1)
                            baseGy = (baseGy * baselineSamples + gy) / (baselineSamples + 1)
                            baselineSamples++
                            return
                        }
                        // Slowly drift baseline so changing hand posture naturally re-centers
                        baseGx = baseGx * 0.996f + gx * 0.004f
                        baseGy = baseGy * 0.996f + gy * 0.004f

                        val deltaY = -(gx - baseGx) * 4.2f * scale
                        val deltaX = (gy - baseGy) * 4.2f * scale
                        smoothX = smoothX * 0.78f + deltaX * 0.22f
                        smoothY = smoothY * 0.78f + deltaY * 0.22f
                        gyroBoostX *= 0.90f
                        gyroBoostY *= 0.90f
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        val ts = event.timestamp
                        if (lastGyroTs != 0L) {
                            val dt = ((ts - lastGyroTs) / 1_000_000_000f).coerceIn(0f, 0.06f)
                            val wx = event.values.getOrNull(0) ?: 0f
                            val wy = event.values.getOrNull(1) ?: 0f
                            gyroBoostX = (gyroBoostX + wx * dt * 28f * scale).coerceIn(-10f, 10f)
                            gyroBoostY = (gyroBoostY + wy * dt * 28f * scale).coerceIn(-10f, 10f)
                        }
                        lastGyroTs = ts
                    }
                }

                val finalRotX = (smoothX + gyroBoostX).coerceIn(-maxDeg, maxDeg)
                val finalRotY = (smoothY + gyroBoostY).coerceIn(-maxDeg, maxDeg)
                tiltState.value = ParallaxTilt(
                    rotX = finalRotX,
                    rotY = finalRotY,
                    innerShiftX = -finalRotY * 1.45f,
                    innerShiftY = finalRotX * 1.45f,
                    sheenNormX = (0.5f + finalRotY / (maxDeg * 2.2f)).coerceIn(0.05f, 0.95f),
                    sheenNormY = (0.5f - finalRotX / (maxDeg * 2.2f)).coerceIn(0.05f, 0.95f),
                    sensorActive = true,
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (gravitySensor != null) {
            sm?.registerListener(listener, gravitySensor, SensorManager.SENSOR_DELAY_GAME)
        }
        if (gyroSensor != null) {
            sm?.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            sm?.unregisterListener(listener)
        }
    }
    return tiltState
}

/**
 * Computes progressive Blind Discovery reveal factor (0f = 0..45s hidden ???, 0f..1f = 45..55s gradual reveal, 1f = revealed).
 */
private fun computeBlindReveal(
    blindMode: Boolean,
    manualReveal: Boolean,
    positionMs: Long,
): Float {
    if (!blindMode || manualReveal) return 1f
    val holdMs = 45_000L
    val fadeMs = 10_000L
    if (positionMs <= holdMs) return 0f
    return ((positionMs - holdMs).toFloat() / fadeMs.toFloat()).coerceIn(0f, 1f)
}

@Composable
fun MiniPlayerBar(
    playback: PlaybackUiState,
    positionMsFlow: StateFlow<Long>,
    hazeState: HazeState,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = playback.currentTrack ?: return
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val blindPos = if (playback.blindDiscoveryMode && !playback.blindManualReveal) {
        positionMsFlow.collectAsState().value
    } else 60_000L
    val reveal = computeBlindReveal(playback.blindDiscoveryMode, playback.blindManualReveal, blindPos)
    val hiddenTitle = playback.blindDiscoveryMode && reveal < 0.65f
    var miniDragX by remember { mutableFloatStateOf(0f) }

    val effectiveStyle = if (!design.miniPlayerFloating && design.miniPlayerStyle == PrimeMiniPlayerStyle.FLOATING_GLASS) {
        PrimeMiniPlayerStyle.DOCKED_MINIMAL
    } else {
        design.miniPlayerStyle
    }

    val hPad = when (effectiveStyle) {
        PrimeMiniPlayerStyle.CAPSULE_PILL -> 22.dp
        PrimeMiniPlayerStyle.DOCKED_MINIMAL -> 0.dp
        PrimeMiniPlayerStyle.FLOATING_GLASS, PrimeMiniPlayerStyle.PURE_AMOLED ->
            if (design.miniPlayerFloating) 16.dp else 0.dp
    }
    val corner = when (effectiveStyle) {
        PrimeMiniPlayerStyle.CAPSULE_PILL -> 50.dp
        PrimeMiniPlayerStyle.DOCKED_MINIMAL -> 0.dp
        PrimeMiniPlayerStyle.FLOATING_GLASS, PrimeMiniPlayerStyle.PURE_AMOLED ->
            if (design.miniPlayerFloating) design.cornerRadiusDp.dp else 0.dp
    }
    val isAmoled = effectiveStyle == PrimeMiniPlayerStyle.PURE_AMOLED
    val coverShape = if (effectiveStyle == PrimeMiniPlayerStyle.CAPSULE_PILL) CircleShape else RoundedCornerShape(12.dp)

    val swipeModifier = Modifier
        .offset { IntOffset(miniDragX.roundToInt(), 0) }
        .pointerInput(design.swipeMiniPlayerToSkip) {
            if (!design.swipeMiniPlayerToSkip) return@pointerInput
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (miniDragX < -95f) onNext()
                    else if (miniDragX > 95f) onPrev()
                    miniDragX = 0f
                },
                onDragCancel = { miniDragX = 0f },
                onHorizontalDrag = { _, dragAmount ->
                    miniDragX = (miniDragX + dragAmount).coerceIn(-160f, 160f)
                },
            )
        }

    val barContent: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = if (effectiveStyle == PrimeMiniPlayerStyle.CAPSULE_PILL) 14.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(coverShape)
                            .blur(((1f - reveal) * 18f).dp)
                            .background(Color.White.copy(alpha = 0.1f)),
                    )
                    if (hiddenTitle) {
                        Text("???", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hiddenTitle) "??? • Canción Misteriosa" else track.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (design.marqueeTrackTitle) Modifier.basicMarquee() else Modifier,
                    )
                    Text(
                        text = if (hiddenTitle) {
                            val secLeft = ((45_000L - blindPos).coerceAtLeast(0L) / 1000L).toInt()
                            if (secLeft > 0) "Modo Misterio • Se revela en ${secLeft}s" else "Revelando pista..."
                        } else track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hiddenTitle) palette.primary else palette.textSecondary,
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
                            contentDescription = "Reproducir o pausar",
                            tint = Color.White,
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = Color.White)
                }
            }
            MiniPlayerProgressLine(
                positionMsFlow = positionMsFlow,
                durationMs = playback.durationMs,
                accent = palette.primary,
                horizontalInset = if (effectiveStyle == PrimeMiniPlayerStyle.CAPSULE_PILL) 24.dp else 0.dp,
            )
        }
    }

    if (isAmoled) {
        val shape = RoundedCornerShape(corner)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = hPad)
                .then(swipeModifier)
                .height(design.miniPlayerHeightDp.dp)
                .clip(shape)
                .background(Color.Black)
                .border(1.2.dp, palette.primary.copy(alpha = 0.55f), shape)
                .clickable(onClick = onExpand),
        ) {
            barContent()
        }
    } else {
        CroixGlassCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = hPad)
                .then(swipeModifier)
                .height(design.miniPlayerHeightDp.dp),
            hazeState = hazeState,
            cornerRadius = corner,
            opacityOverride = (design.glassOpacity + 0.12f).coerceAtMost(0.85f),
            onClick = onExpand,
        ) {
            barContent()
        }
    }
}

@Composable
private fun MiniPlayerProgressLine(
    positionMsFlow: StateFlow<Long>,
    durationMs: Long,
    accent: Color,
    horizontalInset: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val pos by positionMsFlow.collectAsState()
    val progress = if (durationMs > 0L) (pos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalInset)
            .height(2.5.dp),
        color = accent,
        trackColor = Color.White.copy(alpha = 0.12f),
    )
}

@Composable
fun NowPlayingOverlay(
    playback: PlaybackUiState,
    positionMsFlow: StateFlow<Long>,
    lyrics: LyricsResult?,
    lyricsLoading: Boolean,
    translatedLyrics: List<String>,
    translatingLyrics: Boolean,
    translationProviderLabel: String,
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
    onCycleQueueMode: () -> Unit,
    onToggleKaraoke: (Boolean) -> Unit,
    onSetKaraokeStrength: (Float) -> Unit,
    onRevealBlindTrack: () -> Unit,
    onExitBlindMode: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onRequestAddToPlaylist: (Track) -> Unit,
    onOpenArtist: (String) -> Unit,
    onSelectQueueIndex: (Int) -> Unit,
    onRemoveQueueIndex: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onDeduplicateQueue: () -> Int,
    onSaveQueueAsPlaylist: (String) -> Unit,
    onTranslateLyrics: () -> Unit,
    onAdjustLyricsOffset: (Long) -> Unit,
    onShareLyricCard: (List<String>, Int, Int) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
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
    onOpenSettings: () -> Unit,
) {
    val track = playback.currentTrack ?: return
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val strings = LocalCroixStrings.current
    val es = strings.isSpanish
    var activePanel by remember { mutableIntStateOf(0) } // 0=Player, 1=Lyrics, 2=Queue, 3=EQ/8D/Karaoke/Sleep
    var carMode by remember { mutableStateOf(false) }
    var showAudioInspector by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    // Blind Discovery progressive reveal (collects positionMs ONLY when Blind Mode is active!)
    val blindPos = if (playback.blindDiscoveryMode && !playback.blindManualReveal) {
        positionMsFlow.collectAsState().value
    } else 60_000L
    val blindReveal = computeBlindReveal(playback.blindDiscoveryMode, playback.blindManualReveal, blindPos)
    val isMysteryHidden = playback.blindDiscoveryMode && blindReveal < 0.65f
    val mysteryCountdownSec = ((45_000L - blindPos).coerceAtLeast(0L) / 1000L).toInt()

    // Real Calibrated 3D Gyroscope + Gravity Parallax (BETA)
    val tilt by rememberParallaxTiltState(
        enabled = design.artworkParallax3d && activePanel == 0,
        intensity = design.parallax3dIntensity,
    )

    val spin = rememberInfiniteTransition(label = "vinylAndCanvas")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    val canvasScale by spin.animateFloat(
        initialValue = 1.06f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(tween(9_500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "canvasScale",
    )
    val canvasDriftX by spin.animateFloat(
        initialValue = -28f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(11_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "canvasDriftX",
    )

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
            .background(
                if (design.playerBackgroundStyle == PrimePlayerBackgroundStyle.PURE_AMOLED) Color.Black
                else palette.background,
            )
            .clickable(interactionSource = touchBlocker, indication = null) {},
    ) {
        val animateCanvas = design.canvasMotionEnabled && playback.isPlaying
        when (design.playerBackgroundStyle) {
            PrimePlayerBackgroundStyle.BLURRED_COVER -> {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (animateCanvas) {
                                scaleX = canvasScale
                                scaleY = canvasScale
                                translationX = canvasDriftX
                            }
                        }
                        .blur(if (activePanel == 1) design.lyricsBackdropBlurDp.dp else (design.glassBlurDp + 8f).dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.36f),
                                    palette.background.copy(alpha = 0.74f),
                                    palette.background.copy(alpha = 0.94f),
                                ),
                            ),
                        ),
                )
            }
            PrimePlayerBackgroundStyle.DYNAMIC_GRADIENT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    palette.primary.copy(alpha = 0.62f),
                                    palette.secondary.copy(alpha = 0.28f),
                                    palette.background.copy(alpha = 0.96f),
                                ),
                            ),
                        ),
                )
            }
            PrimePlayerBackgroundStyle.LIVE_MESH -> {
                CroixLiveMeshBackdrop(animate = animateCanvas)
            }
            PrimePlayerBackgroundStyle.SOLID_DARK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    palette.primary.copy(alpha = 0.16f),
                                    Color(0xFF0C0B13),
                                    Color(0xFF08070D),
                                ),
                            ),
                        ),
                )
            }
            PrimePlayerBackgroundStyle.PURE_AMOLED -> {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }
        }

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
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Cerrar", tint = Color.White)
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showAudioInspector = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = if (playback.blindDiscoveryMode) "MODO MISTERIO 45s" else strings.playingNow,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (playback.blindDiscoveryMode) palette.primary else palette.textSecondary,
                        )
                        if (design.privateSessionEnabled) {
                            Icon(Icons.Rounded.VisibilityOff, contentDescription = "Sesión privada", tint = Color(0xFFCE93D8), modifier = Modifier.size(13.dp))
                        }
                        if (playback.karaokeEnabled) {
                            Text("KARAOKE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF4081))
                        }
                        if (playback.audio8dEnabled) {
                            Text("8D", fontSize = 10.sp, fontWeight = FontWeight.Black, color = palette.primary)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (playback.isPlayingOfflineFile) {
                            Icon(Icons.Rounded.OfflinePin, null, tint = palette.secondary, modifier = Modifier.size(13.dp))
                            Text("Sin conexión • M4A", fontSize = 11.sp, color = palette.secondary)
                        } else if (playback.activeStreamInfo.isNotBlank()) {
                            Text(playback.activeStreamInfo, fontSize = 11.sp, color = palette.secondary)
                        }
                        Icon(Icons.Rounded.Info, contentDescription = "Inspector técnico", tint = palette.textSecondary, modifier = Modifier.size(12.dp))
                        if (playback.sponsorSegments.isNotEmpty() && design.sponsorBlockEnabled) {
                            Icon(Icons.Rounded.Shield, null, tint = palette.primary, modifier = Modifier.size(12.dp))
                            Text("${playback.sponsorSegments.size}", fontSize = 11.sp, color = palette.primary)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CroixGlassCard(cornerRadius = 50.dp, onClick = { carMode = true }) {
                        IconButton(onClick = { carMode = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.DirectionsCar, contentDescription = "Modo Auto", tint = Color.White)
                        }
                    }
                    CroixGlassCard(cornerRadius = 50.dp, onClick = onOpenSettings) {
                        IconButton(onClick = onOpenSettings, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Configuración", tint = Color.White)
                        }
                    }
                }
            }

            // Live Notice Banners (Queue Mode / SponsorBlock / Blind Discovery Exit / Errors)
            if (playback.queueModeNotice != null) {
                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = palette.secondary, cornerRadius = 14.dp) {
                    Text(
                        text = "✦ ${playback.queueModeNotice}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            } else if (playback.blindDiscoveryMode) {
                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = palette.primary, cornerRadius = 14.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = when {
                                blindReveal >= 0.99f -> "✨ ¡Canción descubierta!"
                                mysteryCountdownSec > 0 -> "🔮 Escucha a ciegas • Se revela en ${mysteryCountdownSec}s"
                                else -> "✨ Revelando canción poco a poco..."
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (blindReveal < 0.99f) {
                                CroixGlassCard(cornerRadius = 50.dp, onClick = onRevealBlindTrack) {
                                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Visibility, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("Revelar", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                            CroixGlassCard(cornerRadius = 50.dp, onClick = onExitBlindMode) {
                                Text("Salir", fontSize = 11.sp, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            } else if (playback.lastSkippedNotice != null) {
                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = palette.primary, cornerRadius = 14.dp) {
                    Text(
                        text = playback.lastSkippedNotice!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }

            if (playback.errorMessage != null) {
                CroixGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentTint = Color(0xFFFF5252),
                    cornerRadius = 14.dp,
                    onClick = onTogglePlay,
                ) {
                    Text(
                        text = "⚠ ${playback.errorMessage}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }

            // Center Dynamic Panel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                val panelDur = (240f / design.transitionSpeed).toInt().coerceIn(80, 480)
                AnimatedContent(
                    targetState = activePanel,
                    transitionSpec = {
                        when (design.transitionStyle) {
                            PrimeTransitionStyle.NONE -> fadeIn(tween(1)).togetherWith(fadeOut(tween(1)))
                            PrimeTransitionStyle.GENTLE_FADE -> fadeIn(tween(panelDur)).togetherWith(fadeOut(tween(panelDur / 2)))
                            else -> (fadeIn(tween(panelDur, easing = FastOutSlowInEasing)) +
                                scaleIn(tween(panelDur, easing = FastOutSlowInEasing), initialScale = 0.96f))
                                .togetherWith(fadeOut(tween((panelDur * 0.7f).toInt(), easing = FastOutSlowInEasing)))
                        }
                    },
                    label = "player_panel_transition",
                ) { panel ->
                    when (panel) {
                        0 -> {
                            val artShape = resolveArtworkShape(design)
                            val styleBaseScale = when (design.playerStyle) {
                                PrimePlayerStyle.FULLSCREEN_COVER -> 0.94f
                                PrimePlayerStyle.MINIMAL_CLEAN -> 0.74f
                                else -> design.artworkScale
                            }
                            val artFill = ((design.artworkScale + styleBaseScale) * 0.5f).coerceIn(0.64f, 0.98f)
                            val isVinyl = design.vinylSpinEnabled || design.artworkShape == PrimeArtworkShape.CIRCLE_VINYL
                            val rotation = if (isVinyl && design.vinylSpinEnabled && playback.isPlaying) angle else 0f
                            val blindBlurDp = ((1f - blindReveal) * 34f).dp

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (design.hidePlayerArtwork) {
                                    // Minimal Cover-Free Focus Mode
                                    CroixGlassCard(
                                        modifier = Modifier.fillMaxWidth(0.88f),
                                        accentTint = palette.primary,
                                        onClick = { if (design.tapArtworkToPlayPause) onTogglePlay() },
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Text(
                                                text = if (isMysteryHidden) "???" else track.title,
                                                style = MaterialTheme.typography.headlineMedium,
                                                color = Color.White,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = if (isMysteryHidden) "Modo Misterio" else track.artist,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = palette.primary,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                            )
                                            if (track.album.isNotBlank() && !isMysteryHidden) {
                                                Text(
                                                    text = track.album,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = palette.textSecondary,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    val shadowMod = if (design.artworkShadowEnabled && design.artworkShadowElevationDp > 0.5f) {
                                        Modifier.shadow(
                                            elevation = design.artworkShadowElevationDp.dp,
                                            shape = artShape,
                                            ambientColor = palette.primary,
                                            spotColor = palette.primary,
                                        )
                                    } else {
                                        Modifier
                                    }
                                    CroixGlassCard(
                                        modifier = Modifier
                                            .fillMaxWidth(artFill)
                                            .aspectRatio(1f)
                                            .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
                                            .then(shadowMod)
                                            .graphicsLayer {
                                                if (design.artworkParallax3d) {
                                                    cameraDistance = 12f * density
                                                    rotationX = tilt.rotX
                                                    rotationY = tilt.rotY
                                                }
                                            }
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
                                            .pointerInput(design.doubleTapSeekSeconds, design.tapArtworkToPlayPause) {
                                                detectTapGestures(
                                                    onTap = {
                                                        if (design.tapArtworkToPlayPause) onTogglePlay()
                                                    },
                                                    onDoubleTap = { offset ->
                                                        val delta = design.doubleTapSeekSeconds * 1_000L
                                                        if (offset.x < size.width / 2f) onSeekRelative(-delta)
                                                        else onSeekRelative(delta)
                                                    },
                                                )
                                            },
                                        shapeOverride = artShape,
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().clip(artShape)) {
                                            AsyncImage(
                                                model = track.thumbnailUrl,
                                                contentDescription = track.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        if (design.artworkParallax3d) {
                                                            scaleX = 1.13f
                                                            scaleY = 1.13f
                                                            translationX = tilt.innerShiftX
                                                            translationY = tilt.innerShiftY
                                                        }
                                                    }
                                                    .rotate(rotation)
                                                    .blur(blindBlurDp),
                                            )

                                            // Vinyl Center Hub Spindle for Circular Vinyl shape
                                            if (isVinyl) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.Center)
                                                        .size(46.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.72f))
                                                        .border(2.dp, palette.primary.copy(alpha = 0.8f), CircleShape),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White.copy(alpha = 0.9f)),
                                                    )
                                                }
                                            }

                                            // Holographic 3D Specular Sheen sliding with phone tilt (BETA)
                                            if (design.artworkParallax3d && tilt.sensorActive) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            Brush.radialGradient(
                                                                colors = listOf(
                                                                    Color.White.copy(alpha = 0.24f),
                                                                    Color.Transparent,
                                                                ),
                                                                center = Offset(tilt.sheenNormX * 900f, tilt.sheenNormY * 900f),
                                                                radius = 650f,
                                                            ),
                                                        ),
                                                )
                                            }

                                            // Blind Discovery 45s ??? Mystery Veil + Smooth Fade-out on Reveal
                                            if (playback.blindDiscoveryMode && blindReveal < 0.98f) {
                                                val veilAlpha = (1f - blindReveal).coerceIn(0f, 1f)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .alpha(veilAlpha)
                                                        .background(
                                                            Brush.radialGradient(
                                                                listOf(
                                                                    palette.primary.copy(alpha = 0.45f),
                                                                    Color(0xFF0A0913).copy(alpha = 0.88f),
                                                                ),
                                                            ),
                                                        ),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(84.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.White.copy(alpha = 0.14f)),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Icon(Icons.Rounded.QuestionMark, null, tint = Color.White, modifier = Modifier.size(48.dp))
                                                        }
                                                        Text(
                                                            text = "???",
                                                            fontSize = 32.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color.White,
                                                            letterSpacing = 4.sp,
                                                        )
                                                        Text(
                                                            text = if (mysteryCountdownSec > 0) "Revelando en ${mysteryCountdownSec}s" else "Revelando...",
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = Color.White.copy(alpha = 0.88f),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                CroixAudioVisualizer(
                                    isPlaying = playback.isPlaying,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                        1 -> SyncedLyricsProPanel(
                            lyrics = lyrics,
                            isLoading = lyricsLoading,
                            translatedLyrics = translatedLyrics,
                            translating = translatingLyrics,
                            translationProviderLabel = translationProviderLabel,
                            karaokeEnabled = playback.karaokeEnabled,
                            karaokeStrength = playback.karaokeStrength,
                            offsetMs = design.lyricsOffsetMs,
                            positionMsFlow = positionMsFlow,
                            onSeekMs = onSeekMs,
                            onTranslate = onTranslateLyrics,
                            onSetKaraokeStrength = onSetKaraokeStrength,
                            onAdjustOffset = onAdjustLyricsOffset,
                            onShareLyricCard = { lines ->
                                onShareLyricCard(lines, palette.primary.toArgb(), palette.secondary.toArgb())
                            },
                        )
                        2 -> QueueProPanel(
                            queue = playback.queue,
                            currentIndex = playback.currentIndex,
                            isExpandingQueue = playback.isExpandingQueue,
                            blindDiscoveryMode = playback.blindDiscoveryMode,
                            onSelect = onSelectQueueIndex,
                            onRemove = onRemoveQueueIndex,
                            onMoveItem = onMoveQueueItem,
                            onDeduplicate = onDeduplicateQueue,
                            onSaveAsPlaylist = onSaveQueueAsPlaylist,
                        )
                        3 -> EqualizerAndSpatialPanel(
                            playback = playback,
                            onSetSleepTimer = onSetSleepTimer,
                            onSetSpeed = onSetSpeed,
                            onSetPitch = onSetPitch,
                            onSetSkipSilence = onSetSkipSilence,
                            onSetKaraokeEnabled = onToggleKaraoke,
                            onSetKaraokeStrength = onSetKaraokeStrength,
                            onSetAudio8dEnabled = onSetAudio8dEnabled,
                            onSetAudio8dSpeed = onSetAudio8dSpeed,
                            onSetReverbMode = onSetReverbMode,
                            onApplyAutoEq = onApplyAutoEq,
                            onSetEqEnabled = onSetEqEnabled,
                            onApplyEqPreset = onApplyEqPreset,
                            onSetEqBand = onSetEqBand,
                            onSetBassBoost = onSetBassBoost,
                            onSetLoudness = onSetLoudness,
                        )
                    }
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
                            val titleMaxLines = if (design.twoLineTrackTitle) 2 else 1
                            Text(
                                text = if (isMysteryHidden) "??? • Pista Misteriosa" else track.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = titleMaxLines,
                                overflow = TextOverflow.Ellipsis,
                                modifier = if (design.marqueeTrackTitle && !design.twoLineTrackTitle) Modifier.basicMarquee() else Modifier,
                            )
                            Text(
                                text = if (isMysteryHidden) "Artista oculto • Escucha sin prejuicios" else track.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable(enabled = !isMysteryHidden) { onOpenArtist(track.artist) },
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
                                    contentDescription = "Descargar",
                                    tint = if (isDownloaded) palette.primary else Color.White,
                                )
                            }
                        }
                        IconButton(onClick = { onRequestAddToPlaylist(track) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.PlaylistAdd, contentDescription = "Añadir a playlist", tint = Color.White)
                        }
                        IconButton(onClick = { onToggleFavorite(track) }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (isFavorite) palette.primary else Color.White,
                            )
                        }
                    }

                    NowPlayingSeekSection(
                        positionMsFlow = positionMsFlow,
                        durationMs = playback.durationMs,
                        onSeekFraction = onSeekFraction,
                    )

                    // Main Transport Row: [3-State Spotify Smart Shuffle] [Prev] [PLAY/PAUSE] [Next] [Karaoke Mic]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val (modeIcon, modeTint) = when (playback.queuePlayMode) {
                            QueuePlayMode.NORMAL -> Icons.Rounded.Shuffle to palette.textSecondary
                            QueuePlayMode.SHUFFLE -> Icons.Rounded.Shuffle to palette.primary
                            QueuePlayMode.REPEAT_ONE -> Icons.Rounded.RepeatOne to palette.secondary
                            QueuePlayMode.SMART_MIX -> Icons.Rounded.AutoAwesome to Color(0xFF00E5FF)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = onCycleQueueMode) {
                                Icon(modeIcon, contentDescription = playback.queuePlayMode.labelEs, tint = modeTint)
                            }
                            if (playback.queuePlayMode != QueuePlayMode.NORMAL) {
                                Text(
                                    text = playback.queuePlayMode.shortTag,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = modeTint,
                                )
                            }
                        }

                        IconButton(onClick = onPrev) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(34.dp))
                        }

                        val playCorner by animateDpAsState(
                            targetValue = when (design.playerButtonStyle) {
                                PrimePlayerButtonStyle.SQUIRCLE_EXPRESSIVE -> if (playback.isPlaying) 18.dp else 32.dp
                                else -> 32.dp
                            },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "playCornerMorph",
                        )
                        val playShape = RoundedCornerShape(playCorner)
                        val playBrush = if (design.useGradientStyle) {
                            Brush.linearGradient(listOf(palette.primary, palette.secondary))
                        } else {
                            Brush.linearGradient(listOf(palette.primary, palette.primary))
                        }
                        val playMod = when (design.playerButtonStyle) {
                            PrimePlayerButtonStyle.GRADIENT_CIRCLE,
                            PrimePlayerButtonStyle.SQUIRCLE_EXPRESSIVE,
                            -> Modifier
                                .size(62.dp)
                                .clip(playShape)
                                .background(playBrush)
                            PrimePlayerButtonStyle.GLASS_FROSTED -> Modifier
                                .size(62.dp)
                                .clip(playShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .border(1.5.dp, palette.glassBorderTop, playShape)
                            PrimePlayerButtonStyle.OUTLINED_NEON -> Modifier
                                .size(62.dp)
                                .clip(playShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .border(2.5.dp, playBrush, playShape)
                        }
                        val playIconTint = when (design.playerButtonStyle) {
                            PrimePlayerButtonStyle.GLASS_FROSTED, PrimePlayerButtonStyle.OUTLINED_NEON -> palette.primary
                            else -> Color.White
                        }

                        Box(
                            modifier = playMod.clickable(onClick = onTogglePlay),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Reproducir o pausar",
                                tint = playIconTint,
                                modifier = Modifier.size(36.dp),
                            )
                        }

                        IconButton(onClick = onNext) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(34.dp))
                        }

                        // Dedicated Karaoke Microphone button ONLY when Karaoke [BETA] is enabled in BETA Labs;
                        // otherwise show a clean Lyrics quick button to keep transport controls symmetrical!
                        if (design.betaKaraokeEnabled) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        val nextKaraoke = !playback.karaokeEnabled
                                        onToggleKaraoke(nextKaraoke)
                                        if (nextKaraoke) activePanel = 1
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (playback.karaokeEnabled) Color(0xFFFF4081).copy(alpha = 0.28f) else Color.Transparent),
                                ) {
                                    Icon(
                                        Icons.Rounded.Mic,
                                        contentDescription = "Modo Karaoke [BETA]",
                                        tint = if (playback.karaokeEnabled) Color(0xFFFF4081) else palette.textSecondary,
                                    )
                                }
                                if (playback.karaokeEnabled) {
                                    Text("Karaoke", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF4081))
                                }
                            }
                        } else {
                            IconButton(
                                onClick = { activePanel = if (activePanel == 1) 0 else 1 },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (activePanel == 1) palette.primary.copy(alpha = 0.24f) else Color.Transparent),
                            ) {
                                Icon(
                                    Icons.Rounded.Lyrics,
                                    contentDescription = "Letras",
                                    tint = if (activePanel == 1) palette.primary else palette.textSecondary,
                                )
                            }
                        }
                    }

                    if (design.showPlayerVolumeBar) {
                        PlayerInlineVolumeBar()
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        PanelToggleIcon(Icons.Rounded.Lyrics, strings.lyrics, activePanel == 1) {
                            activePanel = if (activePanel == 1) 0 else 1
                        }
                        PanelToggleIcon(Icons.Rounded.QueueMusic, "${strings.queue} (${playback.queue.size})", activePanel == 2) {
                            activePanel = if (activePanel == 2) 0 else 2
                        }
                        PanelToggleIcon(Icons.Rounded.Equalizer, strings.audio, activePanel == 3) {
                            activePanel = if (activePanel == 3) 0 else 3
                        }
                    }
                }
            }
        }
    }

    // Live Audio Technical Inspector Modal ([U])
    if (showAudioInspector) {
        val specs = playback.techSpecs
        AlertDialog(
            onDismissRequest = { showAudioInspector = false },
            title = { Text(if (es) "Inspector Técnico de Audio" else "Live Audio Inspector") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InspectorRow(if (es) "Códec" else "Codec", specs.codec)
                    InspectorRow("MIME", specs.mimeType)
                    InspectorRow("Bitrate", if (specs.bitrateKbps > 0) "${specs.bitrateKbps} kbps" else "—")
                    InspectorRow(
                        if (es) "Normalización / Loudness" else "Loudness",
                        specs.loudnessDb?.let { "${"%.1f".format(it)} dB (${if (design.normalizeAudio) "Activa" else "Off"})" } ?: "Estándar",
                    )
                    InspectorRow(if (es) "Origen" else "Source", specs.sourceOrigin)
                    InspectorRow(if (es) "Cliente Motor" else "Engine Client", specs.clientKey)
                    InspectorRow(if (es) "Salida de Audio" else "Output Route", "${playback.activeOutput.route.labelEs} (${playback.activeOutput.deviceName})")
                    InspectorRow("Modo Karaoke DSP", if (playback.karaokeEnabled) "Activo (-${(playback.karaokeStrength * 100).roundToInt()}% voz)" else "Apagado")
                    InspectorRow("Audio Espacial 8D", if (playback.audio8dEnabled) "Activo (${(playback.audio8dSpeed * 100).roundToInt()}%)" else "Apagado")
                    InspectorRow("Reverb", playback.reverbMode.label)
                    InspectorRow("AutoEq / EQ", if (playback.eqEnabled) { if (playback.autoEqProfile != AutoEqHeadphoneProfile.FLAT) playback.autoEqProfile.label else playback.eqPreset.label } else "Plano")
                    InspectorRow("SponsorBlock", if (design.sponsorBlockEnabled) "${playback.sponsorSegments.size} segmentos detectados" else "Desactivado")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioInspector = false }) { Text(strings.close) }
            },
        )
    }
}

@Composable
private fun PlayerInlineVolumeBar() {
    val context = LocalContext.current
    val palette = LocalCroixPalette.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVol = remember(audioManager) { (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1) }
    var volFraction by remember(audioManager) {
        val cur = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVol / 2)
        mutableFloatStateOf((cur.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.VolumeDown, contentDescription = "Menos volumen", tint = palette.textSecondary, modifier = Modifier.size(18.dp))
        Slider(
            value = volFraction,
            onValueChange = { frac ->
                volFraction = frac
                val targetIdx = (frac * maxVol).roundToInt().coerceIn(0, maxVol)
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetIdx, 0)
            },
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f).height(26.dp),
        )
        Icon(Icons.Rounded.VolumeUp, contentDescription = "Más volumen", tint = palette.primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun InspectorRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.width(10.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun NowPlayingSeekSection(
    positionMsFlow: StateFlow<Long>,
    durationMs: Long,
    onSeekFraction: (Float) -> Unit,
) {
    val palette = LocalCroixPalette.current
    val pos by positionMsFlow.collectAsState()
    val progress = if (durationMs > 0L) (pos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    CroixSeekBar(progress = progress, onSeek = onSeekFraction)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(formatTime(pos), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        Text(formatTime(durationMs), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
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
    val es = LocalCroixStrings.current.isSpanish
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
            Text(if (es) "MODO AUTO / PANTALLA" else "CAR / AOD MODE", color = palette.primary, fontWeight = FontWeight.Black)
            CroixGlassCard(cornerRadius = 50.dp, onClick = onExit) {
                Text(if (es) "Salir" else "Exit", color = Color.White, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
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
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(56.dp))
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
                    contentDescription = "Reproducir o pausar",
                    tint = Color.White,
                    modifier = Modifier.size(58.dp),
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(76.dp)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(56.dp))
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
private fun SyncedLyricsProPanel(
    lyrics: LyricsResult?,
    isLoading: Boolean,
    translatedLyrics: List<String>,
    translating: Boolean,
    translationProviderLabel: String,
    karaokeEnabled: Boolean,
    karaokeStrength: Float,
    offsetMs: Long,
    positionMsFlow: StateFlow<Long>,
    onSeekMs: (Long) -> Unit,
    onTranslate: () -> Unit,
    onSetKaraokeStrength: (Float) -> Unit,
    onAdjustOffset: (Long) -> Unit,
    onShareLyricCard: (List<String>) -> Unit,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val es = LocalCroixStrings.current.isSpanish
    val positionMs by positionMsFlow.collectAsState()
    val align = when (design.lyricsAlign) {
        PrimeLyricsAlign.START -> TextAlign.Start
        PrimeLyricsAlign.CENTER -> TextAlign.Center
        PrimeLyricsAlign.END -> TextAlign.End
    }

    var showTranslation by remember { mutableStateOf(design.lyricsAutoTranslateEs) }
    var showRomanized by remember { mutableStateOf(design.lyricsAutoRomanize) }
    var shareMode by remember { mutableStateOf(false) }
    val selectedIndices = remember(lyrics) { mutableStateListOf<Int>() }

    if (isLoading) {
        CircularProgressIndicator(color = palette.primary)
        return
    }
    if (lyrics == null || (lyrics.synced.isEmpty() && lyrics.plain.isBlank())) {
        Text(
            if (es) "No se encontraron letras en LRCLib, Spotify ni YouTube Music." else "No lyrics found across LRCLib, Spotify or YouTube Music.",
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Active Karaoke Quick Vocal-Reduction Bar right above Lyrics!
        if (karaokeEnabled) {
            CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = Color(0xFFFF4081), cornerRadius = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.Mic, null, tint = Color(0xFFFF4081), modifier = Modifier.size(16.dp))
                    Text("Voz -${(karaokeStrength * 100).roundToInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Slider(
                        value = karaokeStrength,
                        onValueChange = onSetKaraokeStrength,
                        valueRange = 0.3f..1.0f,
                        modifier = Modifier.weight(1f).height(28.dp),
                    )
                }
            }
        }

        // Lyrics Pro Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CroixGlassCard(cornerRadius = 50.dp) {
                Text(lyrics.source, fontSize = 11.sp, color = palette.secondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
            CroixGlassCard(
                cornerRadius = 50.dp,
                accentTint = if (showTranslation) palette.primary else null,
                onClick = {
                    showTranslation = !showTranslation
                    if (showTranslation && translatedLyrics.isEmpty()) onTranslate()
                },
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Translate, null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Text(
                        when {
                            translating -> "Traduciendo..."
                            showTranslation && translationProviderLabel.isNotBlank() -> translationProviderLabel
                            else -> "Traducir ES"
                        },
                        fontSize = 11.sp,
                        color = Color.White,
                    )
                }
            }
            CroixGlassCard(
                cornerRadius = 50.dp,
                accentTint = if (showRomanized) palette.primary else null,
                onClick = { showRomanized = !showRomanized },
            ) {
                Text("Romanizar", fontSize = 11.sp, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
            CroixGlassCard(
                cornerRadius = 50.dp,
                accentTint = if (shareMode) palette.secondary else null,
                onClick = {
                    shareMode = !shareMode
                    selectedIndices.clear()
                },
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Share, null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Text(if (shareMode) "Cancelar" else "Tarjeta PNG", fontSize = 11.sp, color = Color.White)
                }
            }
            if (lyrics.synced.isNotEmpty()) {
                CroixGlassCard(cornerRadius = 50.dp, onClick = { onAdjustOffset(offsetMs - 500L) }) {
                    Text("-0.5s", fontSize = 11.sp, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                }
                CroixGlassCard(cornerRadius = 50.dp, onClick = { onAdjustOffset(0L) }) {
                    Text("${"%.1f".format(offsetMs / 1000f)}s", fontSize = 11.sp, color = palette.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                }
                CroixGlassCard(cornerRadius = 50.dp, onClick = { onAdjustOffset(offsetMs + 500L) }) {
                    Text("+0.5s", fontSize = 11.sp, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                }
            }
        }

        if (shareMode) {
            val sourceList = if (lyrics.synced.isNotEmpty()) lyrics.synced.map { it.text } else lyrics.plain.lines().filter { it.isNotBlank() }
            CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = palette.secondary, cornerRadius = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Toca 1 a 4 líneas (${selectedIndices.size}/4)", fontSize = 12.sp, color = Color.White)
                    Button(
                        enabled = selectedIndices.isNotEmpty(),
                        onClick = {
                            val chosen = selectedIndices.sorted().mapNotNull { sourceList.getOrNull(it) }
                            onShareLyricCard(chosen)
                            shareMode = false
                            selectedIndices.clear()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) { Text("Compartir", fontSize = 12.sp) }
                }
            }
        }

        if (lyrics.synced.isEmpty()) {
            val plainLines = remember(lyrics.plain) { lyrics.plain.lines().filter { it.isNotBlank() } }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(plainLines) { idx, line ->
                    val isSel = idx in selectedIndices
                    val rom = if (showRomanized) LyricsRomanizer.romanize(line) else ""
                    val tr = if (showTranslation) translatedLyrics.getOrNull(idx).orEmpty() else ""
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) palette.primary.copy(alpha = 0.28f) else Color.Transparent)
                            .clickable(enabled = shareMode) {
                                if (isSel) selectedIndices.remove(idx)
                                else if (selectedIndices.size < 4) selectedIndices.add(idx)
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    ) {
                        Text(line, fontSize = design.lyricsFontSizeSp.sp, color = Color.White, textAlign = align, modifier = Modifier.fillMaxWidth())
                        if (rom.isNotBlank()) Text(rom, fontSize = (design.lyricsFontSizeSp * 0.56f).sp, color = palette.secondary, textAlign = align, modifier = Modifier.fillMaxWidth())
                        if (tr.isNotBlank()) Text(tr, fontSize = (design.lyricsFontSizeSp * 0.62f).sp, fontStyle = FontStyle.Italic, color = Color.White.copy(alpha = 0.72f), textAlign = align, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        } else {
            val activeIdx = LrcParser.activeLineIndex(lyrics.synced, positionMs + offsetMs)
            val listState = rememberLazyListState()
            LaunchedEffect(activeIdx, shareMode) {
                if (!shareMode && activeIdx >= 0) {
                    listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 18.dp, horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(design.lyricsLineSpacingDp.dp),
            ) {
                itemsIndexed(lyrics.synced) { idx, line ->
                    val isCurrent = idx == activeIdx
                    val isSel = idx in selectedIndices
                    val rom = if (showRomanized) LyricsRomanizer.romanize(line.text) else ""
                    val tr = if (showTranslation) translatedLyrics.getOrNull(idx).orEmpty() else ""

                    val lineScale by animateFloatAsState(
                        targetValue = when {
                            isCurrent && design.lyricsAnimStyle == PrimeLyricsAnimStyle.SCALE_POP -> 1.05f
                            !isCurrent && design.lyricsAnimStyle == PrimeLyricsAnimStyle.SCALE_POP -> 0.96f
                            else -> 1.0f
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "lyricScale",
                    )
                    val lineAlpha by animateFloatAsState(
                        targetValue = if (isCurrent || isSel) 1f else 0.44f,
                        animationSpec = tween(260),
                        label = "lyricAlpha",
                    )
                    val lineOffsetX by animateDpAsState(
                        targetValue = if (isCurrent && design.lyricsAnimStyle == PrimeLyricsAnimStyle.KARAOKE_SLIDE) 10.dp else 0.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "lyricSlide",
                    )

                    val activePillBg = when {
                        isSel -> palette.primary.copy(alpha = 0.32f)
                        isCurrent && (design.lyricsAnimStyle == PrimeLyricsAnimStyle.NEON_GLOW || design.lyricsActiveGlow) ->
                            palette.primary.copy(alpha = if (design.lyricsAnimStyle == PrimeLyricsAnimStyle.NEON_GLOW) 0.22f else 0.12f)
                        else -> Color.Transparent
                    }
                    val activePillBorder = if (isCurrent && design.lyricsAnimStyle == PrimeLyricsAnimStyle.NEON_GLOW) {
                        Modifier.border(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    } else Modifier

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = lineOffsetX)
                            .graphicsLayer {
                                scaleX = lineScale
                                scaleY = lineScale
                            }
                            .clip(RoundedCornerShape(14.dp))
                            .background(activePillBg)
                            .then(activePillBorder)
                            .clickable {
                                if (shareMode) {
                                    if (isSel) selectedIndices.remove(idx)
                                    else if (selectedIndices.size < 4) selectedIndices.add(idx)
                                } else {
                                    onSeekMs((line.timeMs - offsetMs).coerceAtLeast(0L))
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isCurrent && design.lyricsAnimStyle == PrimeLyricsAnimStyle.KARAOKE_SLIDE) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Brush.verticalGradient(listOf(palette.primary, palette.secondary))),
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = line.text,
                                fontSize = if (isCurrent) (design.lyricsFontSizeSp + 2f).sp else design.lyricsFontSizeSp.sp,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isCurrent && design.lyricsActiveGlow) Color.White else Color.White.copy(alpha = lineAlpha),
                                textAlign = align,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (rom.isNotBlank()) {
                                Text(
                                    text = rom,
                                    fontSize = (design.lyricsFontSizeSp * 0.56f).sp,
                                    color = if (isCurrent) palette.secondary else palette.secondary.copy(alpha = 0.55f),
                                    textAlign = align,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (tr.isNotBlank()) {
                                Text(
                                    text = tr,
                                    fontSize = (design.lyricsFontSizeSp * 0.64f).sp,
                                    fontStyle = FontStyle.Italic,
                                    color = if (isCurrent) Color.White.copy(alpha = 0.86f) else Color.White.copy(alpha = 0.36f),
                                    textAlign = align,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueProPanel(
    queue: List<Track>,
    currentIndex: Int,
    isExpandingQueue: Boolean,
    blindDiscoveryMode: Boolean,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onDeduplicate: () -> Int,
    onSaveAsPlaylist: (String) -> Unit,
) {
    val palette = LocalCroixPalette.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var dedupeNotice by remember { mutableStateOf<String?>(null) }
    val remainingCount = (queue.size - currentIndex - 1).coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CroixGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 14.dp,
            accentTint = if (isExpandingQueue) palette.secondary else null,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Cola activa • ${queue.size} canciones",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Text(
                        text = "$remainingCount siguientes • regeneración automática",
                        fontSize = 11.sp,
                        color = palette.textSecondary,
                    )
                }
                if (isExpandingQueue) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CircularProgressIndicator(
                            color = palette.secondary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Ampliando...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.secondary,
                        )
                    }
                } else {
                    Text(
                        text = "∞ Infinita",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary,
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CroixGlassCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 14.dp,
                accentTint = palette.primary,
                onClick = {
                    playlistName = "Cola Prime (${queue.size})"
                    showSaveDialog = true
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Save, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Guardar Playlist", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            CroixGlassCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 14.dp,
                onClick = {
                    val removed = onDeduplicate()
                    dedupeNotice = if (removed > 0) "✓ $removed duplicados eliminados" else "Sin duplicados en cola"
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.FilterList, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Quitar duplicados", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        if (dedupeNotice != null) {
            Text(dedupeNotice!!, fontSize = 11.sp, color = palette.secondary, modifier = Modifier.padding(horizontal = 4.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(queue) { idx, t ->
                val hideFuture = blindDiscoveryMode && idx >= currentIndex
                CroixGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentTint = if (idx == currentIndex) palette.primary else null,
                    onClick = { onSelect(idx) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = t.thumbnailUrl,
                                contentDescription = t.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .blur(if (hideFuture) 18.dp else 0.dp),
                            )
                            if (hideFuture) {
                                Text("???", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hideFuture) "??? • Pista Misteriosa #${idx + 1}" else t.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (hideFuture) "Se revelará tras 45s de escucha" else t.artist,
                                color = palette.textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                            )
                        }
                        if (idx > 0) {
                            IconButton(onClick = { onMoveItem(idx, idx - 1) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Subir", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (idx < queue.lastIndex) {
                            IconButton(onClick = { onMoveItem(idx, idx + 1) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Bajar", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (idx != currentIndex) {
                            IconButton(onClick = { onRemove(idx) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "Quitar", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar cola como playlist") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Nombre de la playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    enabled = playlistName.isNotBlank(),
                    onClick = {
                        onSaveAsPlaylist(playlistName)
                        showSaveDialog = false
                    },
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun EqualizerAndSpatialPanel(
    playback: PlaybackUiState,
    onSetSleepTimer: (Int) -> Unit,
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
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val es = LocalCroixStrings.current.isSpanish
    val bandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")

    CroixGlassCard(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Headphones, null, tint = palette.secondary, modifier = Modifier.size(18.dp))
                Text(
                    "${playback.activeOutput.route.labelEs}: ${playback.activeOutput.deviceName}",
                    color = palette.secondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Karaoke Vocal Remover DSP (shown only when Karaoke [BETA] is enabled in Funciones BETA)
            if (design.betaKaraokeEnabled) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.Mic, null, tint = Color(0xFFFF4081), modifier = Modifier.size(18.dp))
                        Text(if (es) "Modo Karaoke [BETA]" else "Karaoke Vocal Remover [BETA]", color = Color(0xFFFF4081), fontWeight = FontWeight.Bold)
                    }
                    Switch(checked = playback.karaokeEnabled, onCheckedChange = onSetKaraokeEnabled)
                }
                if (playback.karaokeEnabled) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (es) "Reducción de voz" else "Vocal Reduction", color = Color.White, fontSize = 12.sp)
                        Text("${(playback.karaokeStrength * 100).roundToInt()}%", color = Color(0xFFFF4081), fontSize = 12.sp)
                    }
                    Slider(value = playback.karaokeStrength, onValueChange = onSetKaraokeStrength, valueRange = 0.3f..1.0f)
                }
            }

            // 8D Spatial Audio + Reverb Section
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.SurroundSound, null, tint = palette.primary, modifier = Modifier.size(18.dp))
                    Text(if (es) "Audio Espacial 8D" else "8D Spatial Audio", color = palette.primary, fontWeight = FontWeight.Bold)
                }
                Switch(checked = playback.audio8dEnabled, onCheckedChange = onSetAudio8dEnabled)
            }
            if (playback.audio8dEnabled) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (es) "Velocidad de órbita 8D" else "8D Orbit Speed", color = Color.White, fontSize = 12.sp)
                    Text("${(playback.audio8dSpeed * 100).roundToInt()}%", color = palette.primary, fontSize = 12.sp)
                }
                Slider(value = playback.audio8dSpeed, onValueChange = onSetAudio8dSpeed, valueRange = 0.1f..1.0f)
            }

            Text(if (es) "Reverb / Espacialidad" else "Reverb / Space", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ReverbPresetMode.entries.forEach { mode ->
                    CroixGlassCard(
                        cornerRadius = 12.dp,
                        accentTint = if (playback.reverbMode == mode) palette.secondary else null,
                        onClick = { onSetReverbMode(mode) },
                    ) {
                        Text(mode.label, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }

            Text(if (es) "AutoEq para Audífonos" else "AutoEq Headphone Profiles", color = palette.primary, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AutoEqHeadphoneProfile.entries.forEach { prof ->
                    CroixGlassCard(
                        cornerRadius = 12.dp,
                        accentTint = if (playback.autoEqProfile == prof && playback.eqEnabled) palette.primary else null,
                        onClick = { onApplyAutoEq(prof) },
                    ) {
                        Text(prof.label, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (es) "Ecualizador de 5 Bandas" else "5-Band Equalizer", color = palette.primary, fontWeight = FontWeight.Bold)
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

            Text("${if (es) "Refuerzo de Graves" else "Bass Boost"}: ${(playback.bassBoostLevel * 100).roundToInt()}%", color = Color.White, fontSize = 13.sp)
            Slider(value = playback.bassBoostLevel, onValueChange = onSetBassBoost, valueRange = 0f..1f)

            Text("${if (es) "Ganancia de Volumen" else "Loudness Boost"}: +${playback.loudnessGainMb / 100} dB", color = Color.White, fontSize = 13.sp)
            Slider(value = playback.loudnessGainMb.toFloat(), onValueChange = { onSetLoudness(it.roundToInt()) }, valueRange = 0f..1200f)

            Text(if (es) "Velocidad, Tono y Silencios" else "Speed, Pitch & Skip Silence", color = palette.primary, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (es) "Velocidad" else "Speed", color = Color.White, fontSize = 13.sp)
                Text("${"%.2f".format(playback.playbackSpeed)}x", color = palette.primary, fontSize = 13.sp)
            }
            Slider(value = playback.playbackSpeed, onValueChange = onSetSpeed, valueRange = 0.5f..2.0f)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (es) "Tono (Pitch)" else "Pitch", color = Color.White, fontSize = 13.sp)
                Text("${"%.2f".format(playback.playbackPitch)}x", color = palette.primary, fontSize = 13.sp)
            }
            Slider(value = playback.playbackPitch, onValueChange = onSetPitch, valueRange = 0.5f..2.0f)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (es) "Saltar silencios" else "Skip Silence", color = Color.White, fontSize = 13.sp)
                Switch(checked = playback.skipSilenceEnabled, onCheckedChange = onSetSkipSilence)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bedtime, null, tint = palette.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (playback.sleepTimerRemainingSec > 0) {
                        "${if (es) "Temporizador" else "Sleep Timer"} (${playback.sleepTimerRemainingSec / 60}m ${playback.sleepTimerRemainingSec % 60}s)"
                    } else if (es) "Temporizador (Apagado suave)" else "Sleep Timer (Fade-out)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to (if (es) "Apagar" else "Off"), 15 to "15m", 30 to "30m", 60 to "60m").forEach { (mins, lbl) ->
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
