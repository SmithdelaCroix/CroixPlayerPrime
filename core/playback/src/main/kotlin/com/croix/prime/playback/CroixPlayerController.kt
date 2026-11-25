/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.playback

import android.content.ComponentName
import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.croix.prime.engine.InnerTubeClient
import com.croix.prime.engine.SponsorBlockClient
import com.croix.prime.engine.SponsorBlockSkipper
import com.croix.prime.engine.SponsorCategory
import com.croix.prime.engine.SponsorSegment
import com.croix.prime.engine.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class EqPreset(val label: String, val bandsDb: List<Float>) {
    FLAT("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
    BASS_BOOST("Bass Boost", listOf(7f, 4f, 0f, 1f, 2f)),
    ROCK("Rock", listOf(5f, 2f, -1f, 3f, 5f)),
    POP("Pop", listOf(-1f, 3f, 5f, 3f, -1f)),
    VOCAL("Vocal", listOf(-2f, 1f, 5f, 4f, 1f)),
    ELECTRONIC("Electronic", listOf(6f, 3f, 0f, 3f, 6f)),
    CUSTOM("Custom", listOf(0f, 0f, 0f, 0f, 0f)),
}

data class PlaybackUiState(
    val currentTrack: Track? = null,
    val isPlayingOfflineFile: Boolean = false,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatOne: Boolean = false,
    val sponsorSegments: List<SponsorSegment> = emptyList(),
    val lastSkippedNotice: String? = null,
    val sleepTimerRemainingSec: Int = 0,
    val eqEnabled: Boolean = false,
    val eqPreset: EqPreset = EqPreset.FLAT,
    val eqBandsDb: List<Float> = listOf(0f, 0f, 0f, 0f, 0f),
    val bassBoostLevel: Float = 0f,
    val loudnessGainMb: Int = 0,
    val errorMessage: String? = null,
)

class CroixPlayerController(
    context: Context,
    private val innerTube: InnerTubeClient,
    private val sponsorBlock: SponsorBlockClient,
    private val localFileProvider: (String) -> File?,
    private val enabledCategoriesProvider: () -> Set<String>,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var playJob: Job? = null
    private var sleepJob: Job? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudness: LoudnessEnhancer? = null
    private val skippedUuids = mutableSetOf<String>()

    init {
        val token = SessionToken(appContext, ComponentName(appContext, CroixPlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener(
            {
                runCatching {
                    val mc = future.get()
                    controller = mc
                    mc.addListener(playerListener)
                    startPositionTicker()
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { it.copy(isBuffering = playbackState == Player.STATE_BUFFERING) }
            if (playbackState == Player.STATE_ENDED) {
                if (_state.value.repeatOne) {
                    seekTo(0L)
                    controller?.play()
                } else {
                    skipNext()
                }
            }
        }
    }

    private fun startPositionTicker() {
        scope.launch {
            while (true) {
                val mc = controller
                if (mc != null && _state.value.currentTrack != null) {
                    val pos = mc.currentPosition.coerceAtLeast(0L)
                    val dur = mc.duration.takeIf { it > 0L }
                        ?: ((_state.value.currentTrack?.durationSeconds ?: 0) * 1000L)
                    _state.update { it.copy(positionMs = pos, durationMs = dur) }

                    val segs = _state.value.sponsorSegments
                    if (segs.isNotEmpty() && mc.isPlaying) {
                        val hit = SponsorBlockSkipper.findTargetSkip(
                            positionMs = pos,
                            segments = segs,
                            alreadySkippedUuids = skippedUuids,
                            enabledCategories = enabledCategoriesProvider(),
                        )
                        if (hit != null) {
                            skippedUuids += hit.uuid
                            mc.seekTo(hit.endMs)
                            _state.update { it.copy(lastSkippedNotice = "SponsorBlock skipped ${hit.category.replace('_', ' ')}") }
                            scope.launch {
                                delay(3_000L)
                                _state.update { it.copy(lastSkippedNotice = null) }
                            }
                        }
                    }
                }
                delay(350L)
            }
        }
    }

    fun playTrack(track: Track, newQueue: List<Track> = listOf(track), autoFetchRadio: Boolean = true) {
        val queueList = if (newQueue.any { it.id == track.id }) newQueue else listOf(track) + newQueue
        val idx = queueList.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _state.update {
            it.copy(
                currentTrack = track,
                queue = queueList,
                currentIndex = idx,
                isBuffering = true,
                positionMs = 0L,
                durationMs = track.durationSeconds * 1000L,
                sponsorSegments = emptyList(),
                errorMessage = null,
            )
        }
        startPlayingInternal(track)

        if (autoFetchRadio && queueList.size <= 3) {
            scope.launch {
                val radio = innerTube.fetchRadioQueue(track.id)
                if (radio.isNotEmpty()) {
                    _state.update { cur ->
                        val existingIds = cur.queue.map { it.id }.toSet()
                        cur.copy(queue = cur.queue + radio.filter { it.id !in existingIds })
                    }
                }
            }
        }
    }

    fun playAtIndex(index: Int) {
        val list = _state.value.queue
        if (index !in list.indices) return
        val track = list[index]
        _state.update {
            it.copy(
                currentTrack = track,
                currentIndex = index,
                isBuffering = true,
                positionMs = 0L,
                durationMs = track.durationSeconds * 1000L,
                sponsorSegments = emptyList(),
                errorMessage = null,
            )
        }
        startPlayingInternal(track)
    }

    private fun startPlayingInternal(track: Track) {
        playJob?.cancel()
        skippedUuids.clear()
        playJob = scope.launch {
            launch {
                val enabled = enabledCategoriesProvider()
                val cats = SponsorCategory.entries.filter { it.apiKey in enabled }.toSet()
                val segs = sponsorBlock.fetchSegments(track.id, cats)
                if (_state.value.currentTrack?.id == track.id) {
                    _state.update { it.copy(sponsorSegments = segs) }
                }
            }

            // Offline-first playback: if track is downloaded, play straight from local file!
            val offlineFile = localFileProvider(track.id)
            val uriToPlay: Result<Uri> = if (offlineFile != null) {
                _state.update { it.copy(isPlayingOfflineFile = true) }
                Result.success(Uri.fromFile(offlineFile))
            } else {
                _state.update { it.copy(isPlayingOfflineFile = false) }
                innerTube.resolveAudioStream(track.id).map { Uri.parse(it.url) }
            }

            uriToPlay.onSuccess { mediaUri ->
                val mc = controller ?: return@onSuccess
                val metadata = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(track.thumbnailUrl.takeIf { it.isNotBlank() }?.let(Uri::parse))
                    .build()
                val mediaItem = MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(mediaUri)
                    .setMediaMetadata(metadata)
                    .build()
                mc.setMediaItem(mediaItem)
                mc.prepare()
                mc.play()
                applyAudioEffects()
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isBuffering = false,
                        errorMessage = "Could not play track: ${err.message ?: "Network error"}",
                    )
                }
            }
        }
    }

    fun togglePlayPause() {
        val mc = controller ?: return
        if (mc.isPlaying) mc.pause() else mc.play()
    }

    fun seekTo(positionMs: Long) {
        val dur = _state.value.durationMs
        val clamped = if (dur > 0L) positionMs.coerceIn(0L, dur) else positionMs.coerceAtLeast(0L)
        controller?.seekTo(clamped)
        _state.update { it.copy(positionMs = clamped) }
    }

    fun seekRelative(deltaMs: Long) {
        seekTo(_state.value.positionMs + deltaMs)
    }

    fun seekToFraction(fraction: Float) {
        val dur = _state.value.durationMs
        if (dur > 0L) seekTo((dur * fraction.coerceIn(0f, 1f)).toLong())
    }

    fun skipNext() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        val next = if (s.shuffleEnabled && s.queue.size > 1) {
            (s.queue.indices - s.currentIndex).random()
        } else {
            (s.currentIndex + 1) % s.queue.size
        }
        playAtIndex(next)
    }

    fun skipPrevious() {
        val s = _state.value
        if (s.positionMs > 3_500L) {
            seekTo(0L)
            return
        }
        if (s.queue.isEmpty()) return
        val prev = if (s.currentIndex > 0) s.currentIndex - 1 else s.queue.lastIndex
        playAtIndex(prev)
    }

    fun toggleShuffle() = _state.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    fun toggleRepeatOne() = _state.update { it.copy(repeatOne = !it.repeatOne) }

    fun setEqEnabled(enabled: Boolean) {
        _state.update { it.copy(eqEnabled = enabled) }
        applyAudioEffects()
    }

    fun applyEqPreset(preset: EqPreset) {
        _state.update {
            it.copy(
                eqEnabled = true,
                eqPreset = preset,
                eqBandsDb = preset.bandsDb,
            )
        }
        applyAudioEffects()
    }

    fun setEqBand(bandIndex: Int, gainDb: Float) {
        _state.update { cur ->
            val updated = cur.eqBandsDb.toMutableList()
            if (bandIndex in updated.indices) {
                updated[bandIndex] = gainDb.coerceIn(-12f, 12f)
            }
            cur.copy(eqEnabled = true, eqPreset = EqPreset.CUSTOM, eqBandsDb = updated)
        }
        applyAudioEffects()
    }

    fun setBassBoost(level: Float) {
        _state.update { it.copy(bassBoostLevel = level.coerceIn(0f, 1f)) }
        applyAudioEffects()
    }

    fun setLoudnessGain(gainMb: Int) {
        _state.update { it.copy(loudnessGainMb = gainMb.coerceIn(0, 1200)) }
        applyAudioEffects()
    }

    private fun applyAudioEffects() {
        val sessionId = CroixAudioSessionBridge.currentAudioSessionId
        if (sessionId <= 0) return
        val s = _state.value

        runCatching {
            if (equalizer == null) equalizer = Equalizer(0, sessionId)
            equalizer?.apply {
                enabled = s.eqEnabled
                val range = bandLevelRange
                val minMb = range[0].toInt()
                val maxMb = range[1].toInt()
                val hwBands = numberOfBands.toInt()
                for (b in 0 until minOf(hwBands, s.eqBandsDb.size)) {
                    val targetMb = (s.eqBandsDb[b] * 100f).toInt().coerceIn(minMb, maxMb)
                    setBandLevel(b.toShort(), targetMb.toShort())
                }
            }
        }
        runCatching {
            if (bassBoost == null) bassBoost = BassBoost(0, sessionId)
            bassBoost?.apply {
                enabled = s.bassBoostLevel > 0.02f
                if (strengthSupported) setStrength((s.bassBoostLevel * 1000f).toInt().toShort())
            }
        }
        runCatching {
            if (loudness == null) loudness = LoudnessEnhancer(sessionId)
            loudness?.apply {
                enabled = s.loudnessGainMb > 0
                setTargetGain(s.loudnessGainMb)
            }
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) {
            controller?.volume = 1f
            _state.update { it.copy(sleepTimerRemainingSec = 0) }
            return
        }
        var remaining = minutes * 60
        sleepJob = scope.launch {
            while (remaining > 0) {
                _state.update { it.copy(sleepTimerRemainingSec = remaining) }
                if (remaining <= 15) {
                    controller?.volume = (remaining / 15f).coerceIn(0.05f, 1f)
                }
                delay(1_000L)
                remaining--
            }
            controller?.pause()
            controller?.volume = 1f
            _state.update { it.copy(sleepTimerRemainingSec = 0) }
        }
    }

    fun release() {
        sleepJob?.cancel()
        playJob?.cancel()
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { loudness?.release() }
        controller?.release()
        controller = null
    }
}
