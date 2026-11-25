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
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.croix.prime.engine.InnerTubeClient
import com.croix.prime.engine.ResolvedStream
import com.croix.prime.engine.SponsorBlockClient
import com.croix.prime.engine.SponsorBlockSkipper
import com.croix.prime.engine.SponsorCategory
import com.croix.prime.engine.SponsorSegment
import com.croix.prime.engine.StreamRequestHeaders
import com.croix.prime.engine.Track
import kotlinx.coroutines.CompletableDeferred
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
    FLAT("Plano", listOf(0f, 0f, 0f, 0f, 0f)),
    BASS_BOOST("Graves+", listOf(7f, 4f, 0f, 1f, 2f)),
    ROCK("Rock", listOf(5f, 2f, -1f, 3f, 5f)),
    POP("Pop", listOf(-1f, 3f, 5f, 3f, -1f)),
    VOCAL("Vocal", listOf(-2f, 1f, 5f, 4f, 1f)),
    ELECTRONIC("Electrónica", listOf(6f, 3f, 0f, 3f, 6f)),
    CUSTOM("Personalizado", listOf(0f, 0f, 0f, 0f, 0f)),
}

data class PlaybackUiState(
    val currentTrack: Track? = null,
    val isPlayingOfflineFile: Boolean = false,
    val activeStreamInfo: String = "",
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatOne: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val skipSilenceEnabled: Boolean = false,
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
    private val crossfadeSecondsProvider: () -> Int = { 0 },
    private val normalizeAudioProvider: () -> Boolean = { true },
    private val autoPlayRelatedProvider: () -> Boolean = { true },
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    // Separated high-frequency playback position so root screens NEVER recompose every 350ms!
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private var controller: MediaController? = null
    private val controllerReady = CompletableDeferred<MediaController>()
    private var playJob: Job? = null
    private var sleepJob: Job? = null
    private var crossfadeTriggeredForTrackId: String? = null
    private var activeClientKey: String = ""
    private var currentTrackRetryCount: Int = 0
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
                    if (!controllerReady.isCompleted) controllerReady.complete(mc)
                    startPositionTicker()
                }.onFailure { err ->
                    if (!controllerReady.isCompleted) controllerReady.completeExceptionally(err)
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update {
                if (isPlaying) it.copy(isPlaying = true, isBuffering = false, errorMessage = null)
                else it.copy(isPlaying = false)
            }
            if (isPlaying) currentTrackRetryCount = 0
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { it.copy(isBuffering = playbackState == Player.STATE_BUFFERING) }
            if (playbackState == Player.STATE_READY) {
                currentTrackRetryCount = 0
                _state.update { it.copy(errorMessage = null) }
            }
            if (playbackState == Player.STATE_ENDED) {
                if (_state.value.repeatOne) {
                    seekTo(0L)
                    controller?.play()
                } else {
                    skipNext()
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val failedTrack = _state.value.currentTrack
            if (failedTrack != null && !_state.value.isPlayingOfflineFile && currentTrackRetryCount < 2) {
                currentTrackRetryCount++
                if (activeClientKey.isNotBlank()) {
                    innerTube.markStreamClientFailed(failedTrack.id, activeClientKey)
                } else {
                    innerTube.invalidateStreamCache(failedTrack.id)
                }
                _state.update { it.copy(isBuffering = true, errorMessage = null) }
                startPlayingInternal(failedTrack, bypassCache = true)
            } else {
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = error.errorCodeName + (error.cause?.message?.let { m -> ": $m" } ?: ""),
                    )
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
                    _positionMs.value = pos

                    val dur = mc.duration.takeIf { it > 0L }
                        ?: ((_state.value.currentTrack?.durationSeconds ?: 0) * 1000L)
                    if (dur != _state.value.durationMs) {
                        _state.update { it.copy(durationMs = dur) }
                    }

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
                            _positionMs.value = hit.endMs
                            _state.update { it.copy(lastSkippedNotice = "SponsorBlock skipped ${hit.category.replace('_', ' ')}") }
                            scope.launch {
                                delay(3_000L)
                                _state.update { it.copy(lastSkippedNotice = null) }
                            }
                        }
                    }

                    // Optional smooth Crossfade when approaching end of track (0s = disabled)
                    val cfSec = crossfadeSecondsProvider().coerceIn(0, 10)
                    val curId = _state.value.currentTrack?.id
                    if (cfSec > 0 && mc.isPlaying && dur > (cfSec + 4) * 1000L && !_state.value.repeatOne && _state.value.sleepTimerRemainingSec == 0) {
                        val remainingMs = dur - pos
                        val cfMs = cfSec * 1000L
                        if (remainingMs in 1L..cfMs) {
                            val vol = (remainingMs.toFloat() / cfMs.toFloat()).coerceIn(0.06f, 1f)
                            mc.volume = vol
                            if (remainingMs <= 500L && crossfadeTriggeredForTrackId != curId) {
                                crossfadeTriggeredForTrackId = curId
                                skipNext()
                            }
                        } else if (mc.volume < 0.99f && pos > cfMs) {
                            mc.volume = 1f
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
        _positionMs.value = 0L
        _state.update {
            it.copy(
                currentTrack = track,
                queue = queueList,
                currentIndex = idx,
                isBuffering = true,
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

    fun playNext(track: Track) {
        val cur = _state.value
        if (cur.currentTrack == null) {
            playTrack(track)
            return
        }
        val list = cur.queue.toMutableList()
        val insertAt = (cur.currentIndex + 1).coerceIn(0, list.size)
        list.add(insertAt, track)
        _state.update { it.copy(queue = list) }
    }

    fun addToQueue(track: Track) {
        val cur = _state.value
        if (cur.currentTrack == null) {
            playTrack(track)
            return
        }
        _state.update { it.copy(queue = it.queue + track) }
    }

    fun removeFromQueue(index: Int) {
        val cur = _state.value
        if (index !in cur.queue.indices || index == cur.currentIndex) return
        val list = cur.queue.toMutableList()
        list.removeAt(index)
        val adjustedIdx = if (index < cur.currentIndex) cur.currentIndex - 1 else cur.currentIndex
        _state.update { it.copy(queue = list, currentIndex = adjustedIdx) }
    }

    fun playAtIndex(index: Int) {
        val list = _state.value.queue
        if (index !in list.indices) return
        val track = list[index]
        _positionMs.value = 0L
        _state.update {
            it.copy(
                currentTrack = track,
                currentIndex = index,
                isBuffering = true,
                durationMs = track.durationSeconds * 1000L,
                sponsorSegments = emptyList(),
                errorMessage = null,
            )
        }
        startPlayingInternal(track)
    }

    fun retryCurrentTrack() {
        val track = _state.value.currentTrack ?: return
        currentTrackRetryCount = 0
        innerTube.invalidateStreamCache(track.id)
        _state.update { it.copy(isBuffering = true, errorMessage = null) }
        startPlayingInternal(track, bypassCache = true)
    }

    @OptIn(UnstableApi::class)
    private fun startPlayingInternal(track: Track, bypassCache: Boolean = false) {
        playJob?.cancel()
        skippedUuids.clear()
        if (!bypassCache) currentTrackRetryCount = 0
        playJob = scope.launch {
            launch {
                val enabled = enabledCategoriesProvider()
                val cats = SponsorCategory.entries.filter { it.apiKey in enabled }.toSet()
                val segs = sponsorBlock.fetchSegments(track.id, cats)
                if (_state.value.currentTrack?.id == track.id) {
                    _state.update { it.copy(sponsorSegments = segs) }
                }
            }

            val offlineFile = localFileProvider(track.id)
            val resolvedResult: Result<Pair<Uri, ResolvedStream?>> = if (offlineFile != null) {
                _state.update { it.copy(isPlayingOfflineFile = true, activeStreamInfo = "Offline • M4A") }
                activeClientKey = "OFFLINE"
                Result.success(Uri.fromFile(offlineFile) to null)
            } else {
                _state.update { it.copy(isPlayingOfflineFile = false, activeStreamInfo = "") }
                innerTube.resolveAudioStream(track.id, bypassCache = bypassCache).map { stream ->
                    activeClientKey = stream.clientKey
                    val info = "${stream.codecLabel} • ${stream.bitrateKbps} kbps"
                    _state.update { it.copy(activeStreamInfo = info) }
                    CroixAudioSessionBridge.registerStreamHeaders(
                        stream.url,
                        StreamRequestHeaders(
                            userAgent = stream.userAgent,
                            origin = stream.origin,
                            referer = stream.referer,
                            clientKey = stream.clientKey,
                        ),
                    )
                    Uri.parse(stream.url) to stream
                }
            }

            resolvedResult.onSuccess { (mediaUri, resolvedStream) ->
                val mc = controller ?: runCatching { controllerReady.await() }.getOrNull()
                if (mc == null) {
                    _state.update {
                        it.copy(isBuffering = false, errorMessage = "Servicio de audio no disponible")
                    }
                    return@onSuccess
                }
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
                mc.setPlaybackParameters(PlaybackParameters(_state.value.playbackSpeed, _state.value.playbackPitch))
                CroixAudioSessionBridge.exoPlayerRef?.skipSilenceEnabled = _state.value.skipSilenceEnabled
                mc.prepare()

                // Target normalized volume from YouTube loudnessDb metadata when enabled
                val targetVol = if (normalizeAudioProvider() && resolvedStream?.loudnessDb != null) {
                    val db = resolvedStream.loudnessDb!!.coerceIn(-12f, 12f)
                    val gain = Math.pow(10.0, (-db / 20.0)).toFloat()
                    gain.coerceIn(0.45f, 1.0f)
                } else {
                    1.0f
                }

                val cfSec = crossfadeSecondsProvider().coerceIn(0, 10)
                if (cfSec > 0 && crossfadeTriggeredForTrackId != null) {
                    crossfadeTriggeredForTrackId = null
                    mc.volume = 0.08f
                    mc.play()
                    scope.launch {
                        val steps = (cfSec * 5).coerceIn(5, 25)
                        for (i in 1..steps) {
                            delay(100L)
                            mc.volume = ((i.toFloat() / steps.toFloat()) * targetVol).coerceIn(0.08f, targetVol)
                        }
                        mc.volume = targetVol
                    }
                } else {
                    mc.volume = targetVol
                    mc.play()
                }
                applyAudioEffects()

                // Lookahead prefetch for the NEXT track so skipping / auto-advancing is instant!
                launch(Dispatchers.IO) {
                    val q = _state.value.queue
                    val nextIdx = _state.value.currentIndex + 1
                    val nextTrack = q.getOrNull(nextIdx)
                    if (nextTrack != null && localFileProvider(nextTrack.id) == null) {
                        innerTube.prefetchAudioStream(nextTrack.id)
                    }
                    // Smart Autoplay: keep queue alive when approaching the last 2 tracks
                    if (autoPlayRelatedProvider() && q.size - nextIdx <= 2) {
                        val more = innerTube.fetchRadioQueue(track.id, limit = 12)
                        if (more.isNotEmpty()) {
                            _state.update { cur ->
                                val seen = cur.queue.map { it.id }.toSet()
                                cur.copy(queue = cur.queue + more.filter { it.id !in seen })
                            }
                        }
                    }
                }
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = err.message ?: "No se pudo resolver el audio",
                    )
                }
            }
        }
    }

    fun togglePlayPause() {
        val cur = _state.value
        if (cur.errorMessage != null && cur.currentTrack != null) {
            retryCurrentTrack()
            return
        }
        val mc = controller ?: return
        if (mc.playbackState == Player.STATE_IDLE && cur.currentTrack != null) {
            retryCurrentTrack()
            return
        }
        if (mc.isPlaying) mc.pause() else mc.play()
    }

    fun seekTo(positionMs: Long) {
        val dur = _state.value.durationMs
        val clamped = if (dur > 0L) positionMs.coerceIn(0L, dur) else positionMs.coerceAtLeast(0L)
        controller?.seekTo(clamped)
        _positionMs.value = clamped
    }

    fun seekRelative(deltaMs: Long) {
        seekTo(_positionMs.value + deltaMs)
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
        if (_positionMs.value > 3_500L) {
            seekTo(0L)
            return
        }
        val s = _state.value
        if (s.queue.isEmpty()) return
        val prev = if (s.currentIndex > 0) s.currentIndex - 1 else s.queue.lastIndex
        playAtIndex(prev)
    }

    fun toggleShuffle() = _state.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    fun toggleRepeatOne() = _state.update { it.copy(repeatOne = !it.repeatOne) }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        _state.update { it.copy(playbackSpeed = clamped) }
        controller?.setPlaybackParameters(PlaybackParameters(clamped, _state.value.playbackPitch))
    }

    fun setPlaybackPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        _state.update { it.copy(playbackPitch = clamped) }
        controller?.setPlaybackParameters(PlaybackParameters(_state.value.playbackSpeed, clamped))
    }

    @OptIn(UnstableApi::class)
    fun setSkipSilence(enabled: Boolean) {
        _state.update { it.copy(skipSilenceEnabled = enabled) }
        runCatching { CroixAudioSessionBridge.exoPlayerRef?.skipSilenceEnabled = enabled }
    }

    fun setEqEnabled(enabled: Boolean) {
        _state.update { it.copy(eqEnabled = enabled) }
        applyAudioEffects()
    }

    fun applyEqPreset(preset: EqPreset) {
        _state.update {
            it.copy(eqEnabled = true, eqPreset = preset, eqBandsDb = preset.bandsDb)
        }
        applyAudioEffects()
    }

    fun setEqBand(bandIndex: Int, gainDb: Float) {
        _state.update { cur ->
            val updated = cur.eqBandsDb.toMutableList()
            if (bandIndex in updated.indices) updated[bandIndex] = gainDb.coerceIn(-12f, 12f)
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
