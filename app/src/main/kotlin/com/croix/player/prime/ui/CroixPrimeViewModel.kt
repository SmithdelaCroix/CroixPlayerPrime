/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.croix.player.prime.data.AppIconManager
import com.croix.player.prime.data.AppUpdateChecker
import com.croix.player.prime.data.AppUpdateInfo
import com.croix.player.prime.data.AudioFingerprintRecorder
import com.croix.player.prime.data.BlockedArtistEntity
import com.croix.player.prime.data.CroixDatabase
import com.croix.player.prime.data.DownloadedTrackEntity
import com.croix.player.prime.data.FavoriteTrackEntity
import com.croix.player.prime.data.HistoryTrackEntity
import com.croix.player.prime.data.ListeningStatsSummary
import com.croix.player.prime.data.PlayEventEntity
import com.croix.player.prime.data.PlaylistTrackEntity
import com.croix.player.prime.data.RecentSearchEntity
import com.croix.player.prime.data.ServerSessionRepository
import com.croix.player.prime.data.ServerSessionState
import com.croix.player.prime.data.ShareCardGenerator
import com.croix.player.prime.data.UserPlaylistEntity
import com.croix.prime.design.CroixDesignRepository
import com.croix.prime.design.CroixDesignState
import com.croix.prime.design.PrimeAudioQuality
import com.croix.prime.design.PrimePresets
import com.croix.prime.design.resolveCroixStrings
import com.croix.prime.engine.AdBlockEngine
import com.croix.prime.engine.AlbumDetail
import com.croix.prime.engine.AlbumSummary
import com.croix.prime.engine.ArtistProfile
import com.croix.prime.engine.AudioQualityMode
import com.croix.prime.engine.DiscordRpcClient
import com.croix.prime.engine.ExternalIntegrationsConfig
import com.croix.prime.engine.HomeFeedBundle
import com.croix.prime.engine.HybridAlgorithmEngine
import com.croix.prime.engine.InnerTubeClient
import com.croix.prime.engine.LrclibClient
import com.croix.prime.engine.LyricsResult
import com.croix.prime.engine.LyricsTranslator
import com.croix.prime.engine.MoodChip
import com.croix.prime.engine.RelatedArtist
import com.croix.prime.engine.ScrobbleClient
import com.croix.prime.engine.ServerConnectionConfig
import com.croix.prime.engine.ServerHealthStatus
import com.croix.prime.engine.ShazamMatch
import com.croix.prime.engine.ShazamRecognitionClient
import com.croix.prime.engine.SponsorBlockClient
import com.croix.prime.engine.SpotifyPlaylistSummary
import com.croix.prime.engine.SpotifyTasteClient
import com.croix.prime.engine.SpotifyTasteSnapshot
import com.croix.prime.engine.Track
import com.croix.prime.playback.AudioOutputRoute
import com.croix.prime.playback.AutoEqHeadphoneProfile
import com.croix.prime.playback.CroixAudioCache
import com.croix.prime.playback.CroixDownloadManager
import com.croix.prime.playback.CroixPlayerController
import com.croix.prime.playback.CroixShakeDetector
import com.croix.prime.playback.PlaybackUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class SearchFilterTab { SONGS, ARTISTS, ALBUMS }

enum class ShazamPhase { IDLE, LISTENING, ANALYZING, FOUND, ERROR }

data class ShazamUiState(
    val isOpen: Boolean = false,
    val phase: ShazamPhase = ShazamPhase.IDLE,
    val micLevel: Float = 0f,
    val progress: Float = 0f,
    val match: ShazamMatch? = null,
    val matchedTrack: Track? = null,
    val error: String? = null,
)

class CroixPrimeViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val innerTube = InnerTubeClient()
    private val sponsorBlock = SponsorBlockClient()
    private val lrclib = LrclibClient()
    private val lyricsTranslator = LyricsTranslator()
    private val spotifyClient = SpotifyTasteClient()
    private val hybridEngine = HybridAlgorithmEngine(innerTube)
    private val shazamClient = ShazamRecognitionClient()
    private val discordRpc = DiscordRpcClient()
    private val scrobbleClient = ScrobbleClient()
    private val dao = CroixDatabase.get(appContext).dao()
    private val designRepo = CroixDesignRepository(appContext)
    private val serverRepo = ServerSessionRepository(appContext)
    private val imageLoader = ImageLoader(appContext)
    val downloadManager = CroixDownloadManager(appContext, innerTube)

    val designState: StateFlow<CroixDesignState> = designRepo.designFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PrimePresets.PrimeVioletGlass,
    )

    val serverState: StateFlow<ServerSessionState> = serverRepo.sessionFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ServerSessionState(),
    )

    private val _serverHealth = MutableStateFlow<ServerHealthStatus?>(null)
    val serverHealth: StateFlow<ServerHealthStatus?> = _serverHealth.asStateFlow()

    private val _adsBlockedCount = MutableStateFlow(0)
    val adsBlockedCount: StateFlow<Int> = _adsBlockedCount.asStateFlow()

    private val _spotifySyncing = MutableStateFlow(false)
    val spotifySyncing: StateFlow<Boolean> = _spotifySyncing.asStateFlow()

    private val _serverNotice = MutableStateFlow<String?>(null)
    val serverNotice: StateFlow<String?> = _serverNotice.asStateFlow()

    private val _audioCacheBytes = MutableStateFlow(0L)
    val audioCacheBytes: StateFlow<Long> = _audioCacheBytes.asStateFlow()

    private val _hiddenGemsLoading = MutableStateFlow(false)
    val hiddenGemsLoading: StateFlow<Boolean> = _hiddenGemsLoading.asStateFlow()

    private val _shazamState = MutableStateFlow(ShazamUiState())
    val shazamState: StateFlow<ShazamUiState> = _shazamState.asStateFlow()

    val playerController = CroixPlayerController(
        context = appContext,
        innerTube = innerTube,
        sponsorBlock = sponsorBlock,
        localFileProvider = { id -> downloadManager.getDownloadedFile(id) },
        enabledCategoriesProvider = { designState.value.enabledSponsorCategories() },
        crossfadeSecondsProvider = { designState.value.crossfadeSeconds },
        normalizeAudioProvider = { designState.value.normalizeAudio },
        autoPlayRelatedProvider = { designState.value.autoPlayRelated },
        blockedArtistKeysProvider = { blockedArtists.value.map { it.artistKey }.toSet() },
        autoOutputProfileResolver = { route ->
            val cfg = designState.value
            if (!cfg.autoOutputProfilesEnabled) null
            else {
                val key = when (route) {
                    AudioOutputRoute.BLUETOOTH -> cfg.bluetoothAutoEqPreset
                    AudioOutputRoute.WIRED_HEADSET -> cfg.wiredAutoEqPreset
                    AudioOutputRoute.SPEAKER -> cfg.speakerAutoEqPreset
                }
                AutoEqHeadphoneProfile.entries.firstOrNull { it.name == key }
            }
        },
    )

    private val shakeDetector = CroixShakeDetector(appContext) {
        playerController.skipNext()
    }

    val playbackState: StateFlow<PlaybackUiState> = playerController.state
    val positionMs: StateFlow<Long> = playerController.positionMs
    val downloadProgress: StateFlow<Map<String, Float>> = downloadManager.activeProgress

    private val _artistProfile = MutableStateFlow<ArtistProfile?>(null)
    val artistProfile: StateFlow<ArtistProfile?> = _artistProfile.asStateFlow()

    private val _artistLoading = MutableStateFlow(false)
    val artistLoading: StateFlow<Boolean> = _artistLoading.asStateFlow()

    private val _albumDetail = MutableStateFlow<AlbumDetail?>(null)
    val albumDetail: StateFlow<AlbumDetail?> = _albumDetail.asStateFlow()

    private val _albumLoading = MutableStateFlow(false)
    val albumLoading: StateFlow<Boolean> = _albumLoading.asStateFlow()

    val favorites: StateFlow<List<Track>> = dao.observeFavorites()
        .map { list -> list.map { it.toTrack() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val historyEntities: StateFlow<List<HistoryTrackEntity>> = dao.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<Track>> = historyEntities
        .map { list -> list.map { it.toTrack() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playEvents: StateFlow<List<PlayEventEntity>> = dao.observePlayEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val blockedArtists: StateFlow<List<BlockedArtistEntity>> = dao.observeBlockedArtists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val downloads: StateFlow<List<DownloadedTrackEntity>> = dao.observeDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<UserPlaylistEntity>> = dao.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlistTracks: StateFlow<Map<Long, List<Track>>> = dao.observeAllPlaylistTracks()
        .map { rows -> rows.groupBy({ it.playlistId }, { it.toTrack() }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val listeningStats: StateFlow<ListeningStatsSummary> = combine(historyEntities, playEvents) { rows, events ->
        val totalPlays = rows.sumOf { it.playCount }
        val estMin = rows.sumOf { (if (it.durationSeconds > 0) it.durationSeconds else 210) * it.playCount } / 60
        val topArtists = rows.groupBy { it.artist }
            .mapValues { (_, items) -> items.sumOf { it.playCount } }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }
        val topTracks = rows.sortedByDescending { it.playCount }
            .take(5)
            .map { it.toTrack() to it.playCount }

        val hourly = IntArray(24)
        val weekday = IntArray(7)
        if (events.isNotEmpty()) {
            events.forEach { ev ->
                if (ev.hourOfDay in 0..23) hourly[ev.hourOfDay]++
                if (ev.dayOfWeek in 0..6) weekday[ev.dayOfWeek]++
            }
        } else {
            // Seed distribution gracefully from existing history timestamps for upgrading users
            val cal = Calendar.getInstance()
            rows.forEach { r ->
                cal.timeInMillis = r.playedAtMs
                val h = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
                val d = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7).coerceIn(0, 6)
                hourly[h] += r.playCount
                weekday[d] += r.playCount
            }
        }
        val peakHour = hourly.indices.maxByOrNull { hourly[it] }?.takeIf { hourly[it] > 0 } ?: -1
        val peakWeekday = weekday.indices.maxByOrNull { weekday[it] }?.takeIf { weekday[it] > 0 } ?: -1

        ListeningStatsSummary(
            totalPlays = totalPlays,
            estimatedMinutes = estMin,
            topArtists = topArtists,
            topTracks = topTracks,
            hourlyPlays = hourly.toList(),
            weekdayPlays = weekday.toList(),
            peakHour = peakHour,
            peakWeekday = peakWeekday,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListeningStatsSummary())

    val moods: List<MoodChip> = innerTube.defaultMoods

    private val _homeFeed = MutableStateFlow(HomeFeedBundle())
    val homeFeed: StateFlow<HomeFeedBundle> = _homeFeed.asStateFlow()

    private val _homeLoading = MutableStateFlow(true)
    val homeLoading: StateFlow<Boolean> = _homeLoading.asStateFlow()

    private val _updateInfo = MutableStateFlow(AppUpdateInfo())
    val updateInfo: StateFlow<AppUpdateInfo> = _updateInfo.asStateFlow()

    val recentSearches: StateFlow<List<String>> = dao.observeRecentSearches()
        .map { rows -> rows.map { it.query } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchFilterTab.SONGS)
    val searchFilter: StateFlow<SearchFilterTab> = _searchFilter.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _artistSearchResults = MutableStateFlow<List<RelatedArtist>>(emptyList())
    val artistSearchResults: StateFlow<List<RelatedArtist>> = _artistSearchResults.asStateFlow()

    private val _albumSearchResults = MutableStateFlow<List<AlbumSummary>>(emptyList())
    val albumSearchResults: StateFlow<List<AlbumSummary>> = _albumSearchResults.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    private val _importStatus = MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _importStatus.asStateFlow()

    private val _lyrics = MutableStateFlow<LyricsResult?>(null)
    val lyrics: StateFlow<LyricsResult?> = _lyrics.asStateFlow()

    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading.asStateFlow()

    private val _translatedLyrics = MutableStateFlow<List<String>>(emptyList())
    val translatedLyrics: StateFlow<List<String>> = _translatedLyrics.asStateFlow()

    private val _translatingLyrics = MutableStateFlow(false)
    val translatingLyrics: StateFlow<Boolean> = _translatingLyrics.asStateFlow()

    private val _albumPrimary = MutableStateFlow<Color?>(null)
    val albumPrimary: StateFlow<Color?> = _albumPrimary.asStateFlow()

    private val _albumSecondary = MutableStateFlow<Color?>(null)
    val albumSecondary: StateFlow<Color?> = _albumSecondary.asStateFlow()

    private var searchJob: Job? = null
    private var shazamJob: Job? = null
    private var scrobbleJob: Job? = null
    private var cachedArtworkBitmap: Bitmap? = null

    init {
        observeServerConfig()
        refreshHome()
        refreshCacheSize()
        checkForUpdates()
        observeCurrentTrackSideEffects()
        observePresenceAndScrobbling()
        observeDesignPreferences()
    }

    private fun observeServerConfig() {
        viewModelScope.launch {
            var initialized = false
            serverState.collect { session ->
                innerTube.updateServerConfig(session.serverConfig)
                _adsBlockedCount.value = AdBlockEngine.blockedCount.get()
                if (!initialized) {
                    initialized = true
                    pingServer()
                }
            }
        }
    }

    private fun observeDesignPreferences() {
        viewModelScope.launch {
            designState.collect { cfg ->
                shakeDetector.configure(cfg.shakeToSkipEnabled, cfg.shakeSensitivity)
                val q = when (cfg.audioQuality) {
                    PrimeAudioQuality.MAX_QUALITY -> AudioQualityMode.MAX_QUALITY
                    PrimeAudioQuality.BALANCED -> AudioQualityMode.BALANCED
                    PrimeAudioQuality.DATA_SAVER -> AudioQualityMode.DATA_SAVER
                }
                innerTube.updateAudioQuality(q)
                CroixAudioCache.configureLimitMb(appContext, cfg.audioCacheLimitMb)
                if (cfg.privateSessionEnabled) {
                    discordRpc.clearPresence()
                }
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateInfo.value = _updateInfo.value.copy(checking = true, error = null)
            _updateInfo.value = AppUpdateChecker.checkForUpdate()
        }
    }

    private fun observeCurrentTrackSideEffects() {
        viewModelScope.launch {
            playbackState
                .map { it.currentTrack }
                .distinctUntilChangedBy { it?.id }
                .collect { track ->
                    if (track == null) return@collect
                    _translatedLyrics.value = emptyList()

                    // Private Session pauses ALL local history, stats & play-event logging!
                    if (!designState.value.privateSessionEnabled) {
                        launch(Dispatchers.IO) {
                            val now = System.currentTimeMillis()
                            val existing = dao.getHistoryById(track.id)
                            dao.upsertHistory(
                                HistoryTrackEntity(
                                    id = track.id,
                                    title = track.title,
                                    artist = track.artist,
                                    album = track.album,
                                    durationSeconds = track.durationSeconds,
                                    thumbnailUrl = track.thumbnailUrl,
                                    playCount = (existing?.playCount ?: 0) + 1,
                                    playedAtMs = now,
                                ),
                            )
                            val cal = Calendar.getInstance().apply { timeInMillis = now }
                            val hour = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
                            val dow = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7).coerceIn(0, 6) // 0=Mon..6=Sun
                            dao.insertPlayEvent(
                                PlayEventEntity(
                                    trackId = track.id,
                                    title = track.title,
                                    artist = track.artist,
                                    durationSeconds = track.durationSeconds,
                                    playedAtMs = now,
                                    hourOfDay = hour,
                                    dayOfWeek = dow,
                                ),
                            )
                            _audioCacheBytes.value = CroixAudioCache.getCacheSizeBytes(appContext)
                        }
                    }

                    // Multi-source lyrics: LRCLib -> Spotify Color-Lyrics -> YouTube Music Lyrics
                    launch {
                        _lyricsLoading.value = true
                        val session = serverState.value
                        val res = lrclib.fetchLyricsMultiSource(
                            track = track,
                            innerTube = innerTube,
                            spotifyClient = spotifyClient,
                            spotifySpDc = session.spotifySpDc,
                            spotifySpKey = session.spotifySpKey,
                        )
                        _lyrics.value = res
                        _lyricsLoading.value = false
                        if (designState.value.lyricsAutoTranslateEs && res != null) {
                            translateCurrentLyrics()
                        }
                    }

                    launch(Dispatchers.IO) {
                        extractArtworkColors(track.thumbnailUrl)
                    }
                }
        }
    }

    private fun observePresenceAndScrobbling() {
        viewModelScope.launch {
            var lastScrobbledTrackId: String? = null
            combine(
                playbackState.map { Triple(it.currentTrack, it.isPlaying, it.durationMs) },
                serverState.map { it.integrations },
                designState.map { it.privateSessionEnabled },
            ) { playTriple, integrations, isPrivate ->
                Triple(playTriple, integrations, isPrivate)
            }.collect { (playTriple, integrations, isPrivate) ->
                val (track, isPlaying, durationMs) = playTriple

                if (isPrivate || track == null) {
                    if (integrations.isDiscordReady) discordRpc.clearPresence()
                    scrobbleJob?.cancel()
                    return@collect
                }

                // Discord Rich Presence
                if (integrations.isDiscordReady) {
                    discordRpc.updatePresence(
                        token = integrations.discordToken,
                        track = track,
                        isPlaying = isPlaying,
                        positionMs = positionMs.value,
                        durationMs = durationMs,
                    )
                } else {
                    discordRpc.disconnect()
                }

                // Last.fm + ListenBrainz Now Playing & Scrobble after 28s active play
                if (isPlaying && lastScrobbledTrackId != track.id && (integrations.isLastFmReady || integrations.isListenBrainzReady)) {
                    val startedSec = System.currentTimeMillis() / 1000L
                    launch(Dispatchers.IO) {
                        if (integrations.isLastFmReady) scrobbleClient.updateNowPlayingLastFm(integrations, track)
                        if (integrations.isListenBrainzReady) scrobbleClient.submitListenBrainz(integrations.listenBrainzToken, track, isPlayingNow = true)
                    }
                    scrobbleJob?.cancel()
                    scrobbleJob = launch(Dispatchers.IO) {
                        val waitMs = if (track.durationSeconds in 15..55) (track.durationSeconds * 500L) else 28_000L
                        delay(waitMs)
                        val stillPlaying = playbackState.value.currentTrack?.id == track.id &&
                            playbackState.value.isPlaying &&
                            !designState.value.privateSessionEnabled
                        if (stillPlaying) {
                            lastScrobbledTrackId = track.id
                            if (integrations.isLastFmReady) scrobbleClient.scrobbleLastFm(integrations, track, startedSec)
                            if (integrations.isListenBrainzReady) {
                                scrobbleClient.submitListenBrainz(integrations.listenBrainzToken, track, isPlayingNow = false, listenedAtEpochSec = startedSec)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun extractArtworkColors(url: String) {
        if (url.isBlank()) {
            cachedArtworkBitmap = null
            return
        }
        runCatching {
            val req = ImageRequest.Builder(appContext)
                .data(url)
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(req)
            if (result is SuccessResult) {
                val bmp = result.image.toBitmap()
                cachedArtworkBitmap = bmp
                val p = Palette.from(bmp).generate()
                val vibrant = p.getVibrantColor(0xFFA238FF.toInt())
                val lightOrMuted = p.getLightVibrantColor(p.getMutedColor(0xFF00E5FF.toInt()))
                _albumPrimary.value = Color(vibrant)
                _albumSecondary.value = Color(lightOrMuted)
            }
        }
    }

    fun translateCurrentLyrics() {
        val track = playbackState.value.currentTrack ?: return
        val curLyrics = _lyrics.value ?: return
        val sourceLines = if (curLyrics.synced.isNotEmpty()) {
            curLyrics.synced.map { it.text }
        } else {
            curLyrics.plain.lines().filter { it.isNotBlank() }
        }
        if (sourceLines.isEmpty()) return
        viewModelScope.launch {
            _translatingLyrics.value = true
            _translatedLyrics.value = lyricsTranslator.translateLinesToSpanish(track.id, sourceLines)
            _translatingLyrics.value = false
        }
    }

    fun shareLyricCard(selectedLines: List<String>, primaryArgb: Int, secondaryArgb: Int) {
        val track = playbackState.value.currentTrack ?: return
        if (selectedLines.isEmpty()) return
        viewModelScope.launch {
            ShareCardGenerator.shareLyricCard(
                context = appContext,
                track = track,
                lines = selectedLines,
                artwork = cachedArtworkBitmap,
                primaryArgb = primaryArgb,
                secondaryArgb = secondaryArgb,
            )
        }
    }

    fun shareMusicalReceipt() {
        viewModelScope.launch {
            val name = serverState.value.spotifyTaste.accountName
                .ifBlank { serverState.value.serverConfig.ytMusicAccountName }
                .ifBlank { "Oyente Prime" }
            ShareCardGenerator.shareMusicalReceipt(
                context = appContext,
                stats = listeningStats.value,
                listenerName = name,
            )
        }
    }

    // === Shazam Acoustic Recognition ===

    fun openShazam() {
        _shazamState.value = ShazamUiState(isOpen = true, phase = ShazamPhase.IDLE)
    }

    fun closeShazam() {
        shazamJob?.cancel()
        _shazamState.value = ShazamUiState(isOpen = false, phase = ShazamPhase.IDLE)
    }

    fun startShazamListening() {
        shazamJob?.cancel()
        val wasPlaying = playbackState.value.isPlaying
        if (wasPlaying) playerController.togglePlayPause()

        _shazamState.value = ShazamUiState(isOpen = true, phase = ShazamPhase.LISTENING, micLevel = 0.08f, progress = 0f)
        shazamJob = viewModelScope.launch {
            val sigResult = AudioFingerprintRecorder.recordSignature(durationMs = 4_600L) { level, prog ->
                _shazamState.value = _shazamState.value.copy(micLevel = level, progress = prog)
            }
            val sig = sigResult.getOrElse { err ->
                _shazamState.value = _shazamState.value.copy(
                    phase = ShazamPhase.ERROR,
                    error = err.message ?: "Error al grabar audio del micrófono",
                )
                return@launch
            }

            _shazamState.value = _shazamState.value.copy(phase = ShazamPhase.ANALYZING, progress = 1f)
            val recResult = shazamClient.recognize(sig)
            recResult.onSuccess { match ->
                // Immediately resolve playable track on YouTube Music
                val foundTrack = innerTube.searchSongs("${match.title} ${match.artist}", limit = 3).firstOrNull()
                _shazamState.value = _shazamState.value.copy(
                    phase = ShazamPhase.FOUND,
                    match = match,
                    matchedTrack = foundTrack,
                )
            }.onFailure { err ->
                _shazamState.value = _shazamState.value.copy(
                    phase = ShazamPhase.ERROR,
                    error = err.message ?: "No se reconoció ninguna canción. Acerca más el teléfono al sonido.",
                )
            }
        }
    }

    fun playShazamMatch() {
        val state = _shazamState.value
        val track = state.matchedTrack
        if (track != null) {
            closeShazam()
            playerController.playTrack(track)
        } else if (state.match != null) {
            val query = "${state.match.title} ${state.match.artist}"
            closeShazam()
            onSearchQueryChange(query)
        }
    }

    // === Audio Cache, App Icon, Blocked Artists, Hidden Gems, Queue Pro ===

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            _audioCacheBytes.value = CroixAudioCache.getCacheSizeBytes(appContext)
        }
    }

    fun clearAudioCache() {
        viewModelScope.launch {
            _audioCacheBytes.value = CroixAudioCache.clearCache(appContext)
            _serverNotice.value = "✓ Caché de audio limpiada correctamente"
        }
    }

    fun selectAppIcon(iconId: String) {
        updateDesign { it.copy(appIconId = iconId) }
        AppIconManager.applyIcon(appContext, iconId)
        _serverNotice.value = "✓ Ícono de la app actualizado (el launcher puede tardar unos segundos)"
    }

    fun blockArtist(artistName: String) {
        val key = HybridAlgorithmEngine.normalizeArtistKey(artistName)
        if (key.isBlank()) return
        val display = artistName.split(",", "&", "feat.", "ft.", ignoreCase = true).firstOrNull()?.trim().orEmpty().ifBlank { artistName }
        viewModelScope.launch(Dispatchers.IO) {
            dao.blockArtist(BlockedArtistEntity(artistKey = key, displayName = display))
            withContext(Dispatchers.Main) {
                refreshHome()
            }
        }
    }

    fun unblockArtist(artistKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.unblockArtist(artistKey)
            withContext(Dispatchers.Main) {
                refreshHome()
            }
        }
    }

    fun isArtistBlocked(artistName: String): Boolean {
        val keys = blockedArtists.value.map { it.artistKey }.toSet()
        return HybridAlgorithmEngine.isBlockedArtist(artistName, keys)
    }

    fun saveCurrentQueueAsPlaylist(name: String) {
        val queue = playbackState.value.queue
        val clean = name.trim()
        if (clean.isEmpty() || queue.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val pid = dao.insertPlaylist(UserPlaylistEntity(name = clean))
            queue.distinctBy { it.id }.forEach { t ->
                dao.addTrackToPlaylist(
                    PlaylistTrackEntity(
                        playlistId = pid,
                        trackId = t.id,
                        title = t.title,
                        artist = t.artist,
                        album = t.album,
                        durationSeconds = t.durationSeconds,
                        thumbnailUrl = t.thumbnailUrl,
                    ),
                )
            }
            _serverNotice.value = "✓ Playlist «$clean» guardada (${queue.size} temas)"
        }
    }

    fun startHiddenGemsRadar() {
        viewModelScope.launch {
            _hiddenGemsLoading.value = true
            val session = serverState.value
            val knownIds = (history.value.map { it.id } + favorites.value.map { it.id }).toSet()
            val blockedKeys = blockedArtists.value.map { it.artistKey }.toSet()
            val fallback = _homeFeed.value.quickPicks + _homeFeed.value.shelves.flatMap { it.tracks }
            val gems = hybridEngine.buildHiddenGemsRadar(
                knownTrackIds = knownIds,
                blockedArtistKeys = blockedKeys,
                spotifyTaste = session.spotifyTaste.takeIf { session.isSpotifyConnected },
                localHistory = history.value,
                fallbackTracks = fallback,
            )
            _hiddenGemsLoading.value = false
            if (gems.isNotEmpty()) {
                playerController.playTrack(gems.first(), gems, autoFetchRadio = true)
            }
        }
    }

    fun updateIntegrations(transform: (ExternalIntegrationsConfig) -> ExternalIntegrationsConfig) {
        viewModelScope.launch {
            serverRepo.update { state ->
                state.copy(integrations = transform(state.integrations))
            }
        }
    }

    fun authenticateLastFm(apiKey: String, apiSecret: String, username: String, password: String) {
        viewModelScope.launch {
            _serverNotice.value = "⏳ Conectando con Last.fm..."
            val res = scrobbleClient.authenticateLastFm(apiKey, apiSecret, username, password)
            res.onSuccess { sessionKey ->
                serverRepo.update { s ->
                    s.copy(
                        integrations = s.integrations.copy(
                            lastFmEnabled = true,
                            lastFmApiKey = apiKey.trim(),
                            lastFmApiSecret = apiSecret.trim(),
                            lastFmSessionKey = sessionKey,
                            lastFmUsername = username.trim(),
                        ),
                    )
                }
                _serverNotice.value = "✓ Last.fm conectado como $username"
            }.onFailure { err ->
                _serverNotice.value = "✕ Last.fm: ${err.message ?: "Error de inicio de sesión"}"
            }
        }
    }

    // === Home Feed, Flow, Search, Artist, Album ===

    fun refreshHome() {
        viewModelScope.launch {
            _homeLoading.value = true
            val session = serverState.value
            innerTube.updateServerConfig(session.serverConfig)
            val ytSections = innerTube.fetchHomeSections()
            val isSpanish = resolveCroixStrings(designState.value.languageMode).isSpanish
            val records = historyEntities.value.map {
                HybridAlgorithmEngine.WeightedPlayRecord(
                    track = it.toTrack(),
                    playCount = it.playCount,
                    playedAtMs = it.playedAtMs,
                )
            }
            val blockedKeys = blockedArtists.value.map { it.artistKey }.toSet()
            val bundle = hybridEngine.buildRichHomeFeed(
                ytSections = ytSections,
                spotifyTaste = session.spotifyTaste.takeIf { session.isSpotifyConnected },
                localFavorites = favorites.value,
                localHistoryRecords = records,
                blendWeight = session.spotifyBlendWeight,
                isSpanish = isSpanish,
                blockedArtistKeys = blockedKeys,
            )
            _homeFeed.value = bundle
            _adsBlockedCount.value = AdBlockEngine.blockedCount.get()
            _homeLoading.value = false
        }
    }

    fun startCroixFlow(mood: MoodChip? = null) {
        viewModelScope.launch {
            val session = serverState.value
            val fallback = _homeFeed.value.quickPicks.ifEmpty {
                _homeFeed.value.shelves.firstOrNull()?.tracks.orEmpty()
            }
            val blockedKeys = blockedArtists.value.map { it.artistKey }.toSet()
            val queue = hybridEngine.buildHybridFlowQueue(
                spotifyTaste = session.spotifyTaste.takeIf { session.isSpotifyConnected },
                localFavorites = favorites.value,
                localHistory = history.value,
                fallbackTracks = fallback,
                blendWeight = session.spotifyBlendWeight,
                moodQuery = mood?.query,
                blockedArtistKeys = blockedKeys,
            )
            if (queue.isNotEmpty()) {
                playerController.playTrack(queue.first(), queue, autoFetchRadio = true)
            }
            _adsBlockedCount.value = AdBlockEngine.blockedCount.get()
        }
    }

    fun playMood(mood: MoodChip) {
        viewModelScope.launch {
            val blockedKeys = blockedArtists.value.map { it.artistKey }.toSet()
            val tracks = innerTube.searchSongs(mood.query, limit = 18)
                .filterNot { HybridAlgorithmEngine.isBlockedArtist(it.artist, blockedKeys) }
            if (tracks.isNotEmpty()) {
                playerController.playTrack(tracks.first(), tracks, autoFetchRadio = true)
            }
        }
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        playerController.playTrack(track, queue)
    }

    fun openArtist(artistName: String, browseId: String? = null) {
        viewModelScope.launch {
            _albumDetail.value = null
            _artistLoading.value = true
            _artistProfile.value = ArtistProfile(browseId = browseId.orEmpty(), name = artistName)
            _artistProfile.value = innerTube.fetchArtistProfile(artistName, browseId)
            _artistLoading.value = false
        }
    }

    fun closeArtist() {
        _artistProfile.value = null
        _artistLoading.value = false
    }

    fun openAlbum(summary: AlbumSummary) {
        viewModelScope.launch {
            _albumLoading.value = true
            _albumDetail.value = AlbumDetail(
                browseId = summary.browseId,
                title = summary.title,
                artist = _artistProfile.value?.name.orEmpty(),
                year = summary.year,
                thumbnailUrl = summary.thumbnailUrl,
            )
            _albumDetail.value = innerTube.fetchAlbumDetail(summary)
            _albumLoading.value = false
        }
    }

    fun closeAlbum() {
        _albumDetail.value = null
        _albumLoading.value = false
    }

    fun onSearchQueryChange(q: String) {
        _searchQuery.value = q
        triggerSearch(q, _searchFilter.value)
    }

    fun onSearchFilterChange(filter: SearchFilterTab) {
        _searchFilter.value = filter
        if (_searchQuery.value.isNotBlank()) {
            triggerSearch(_searchQuery.value, filter)
        }
    }

    private fun triggerSearch(q: String, filter: SearchFilterTab) {
        searchJob?.cancel()
        if (q.isBlank()) {
            _searchResults.value = emptyList()
            _artistSearchResults.value = emptyList()
            _albumSearchResults.value = emptyList()
            _searchLoading.value = false
            return
        }
        searchJob = viewModelScope.launch {
            _searchLoading.value = true
            delay(350L)
            val trimmed = q.trim()
            if (trimmed.length >= 2 && !designState.value.privateSessionEnabled) {
                launch(Dispatchers.IO) { dao.saveRecentSearch(RecentSearchEntity(trimmed)) }
            }
            when (filter) {
                SearchFilterTab.SONGS -> _searchResults.value = innerTube.searchSongs(trimmed, limit = 25)
                SearchFilterTab.ARTISTS -> _artistSearchResults.value = innerTube.searchArtists(trimmed, limit = 15)
                SearchFilterTab.ALBUMS -> _albumSearchResults.value = innerTube.searchAlbums(trimmed, limit = 15)
            }
            _searchLoading.value = false
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteRecentSearch(query) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch(Dispatchers.IO) { dao.clearRecentSearches() }
    }

    fun importPlaylistUrl(url: String) {
        viewModelScope.launch {
            _importStatus.value = "..."
            val trimmed = url.trim()
            val res: Result<Pair<String, List<Track>>> = if (trimmed.contains("open.spotify.com/")) {
                spotifyClient.fetchPublicSpotifyPlaylist(trimmed).mapCatching { (title, seeds) ->
                    val matched = hybridEngine.matchSpotifyTracksToYtMusic(seeds, maxTracks = 50)
                    if (matched.isEmpty()) error("No se encontraron coincidencias en YouTube Music")
                    title to matched
                }
            } else {
                innerTube.importPlaylistFromUrl(trimmed)
            }
            res.onSuccess { (title, tracks) ->
                withContext(Dispatchers.IO) {
                    val pid = dao.insertPlaylist(UserPlaylistEntity(name = title))
                    tracks.forEach { t ->
                        dao.addTrackToPlaylist(
                            PlaylistTrackEntity(
                                playlistId = pid,
                                trackId = t.id,
                                title = t.title,
                                artist = t.artist,
                                album = t.album,
                                durationSeconds = t.durationSeconds,
                                thumbnailUrl = t.thumbnailUrl,
                            ),
                        )
                    }
                }
                _importStatus.value = "✓ $title (${tracks.size})"
            }.onFailure { err ->
                _importStatus.value = "✕ ${err.message ?: "Error"}"
            }
        }
    }

    fun clearImportStatus() {
        _importStatus.value = null
    }

    // === Server, Ad-Block & Spotify Algorithm Actions ===

    fun updateServerConfig(transform: (ServerConnectionConfig) -> ServerConnectionConfig) {
        viewModelScope.launch {
            serverRepo.update { state ->
                val updatedCfg = transform(state.serverConfig)
                innerTube.updateServerConfig(updatedCfg)
                state.copy(serverConfig = updatedCfg)
            }
            pingServer()
        }
    }

    fun pingServer() {
        viewModelScope.launch {
            _serverHealth.value = innerTube.pingActiveServer()
            _adsBlockedCount.value = AdBlockEngine.blockedCount.get()
        }
    }

    fun connectYtMusicCookie(cookie: String) {
        viewModelScope.launch {
            _serverNotice.value = "..."
            val res = innerTube.verifyYtMusicAccount(cookie)
            res.onSuccess { accountName ->
                serverRepo.update { s ->
                    val cfg = s.serverConfig.copy(ytMusicCookie = cookie, ytMusicAccountName = accountName)
                    innerTube.updateServerConfig(cfg)
                    s.copy(serverConfig = cfg)
                }
                _serverNotice.value = "✓ YouTube Music: $accountName"
                refreshHome()
            }.onFailure { err ->
                _serverNotice.value = "✕ ${err.message ?: "Error al conectar YT Music"}"
            }
        }
    }

    fun disconnectYtMusic() {
        viewModelScope.launch {
            serverRepo.update { s ->
                val cfg = s.serverConfig.copy(ytMusicCookie = "", ytMusicAccountName = "")
                innerTube.updateServerConfig(cfg)
                s.copy(serverConfig = cfg)
            }
            _serverNotice.value = null
            refreshHome()
        }
    }

    fun connectSpotifyCookies(spDc: String, spKey: String = "") {
        val cleanDc = spDc.trim()
        if (cleanDc.isBlank()) return
        viewModelScope.launch {
            _spotifySyncing.value = true
            _serverNotice.value = "..."
            spotifyClient.clearCachedToken()
            val res = spotifyClient.syncTasteProfile(cleanDc, spKey.trim())
            res.onSuccess { snapshot ->
                serverRepo.update { s ->
                    s.copy(
                        spotifySpDc = cleanDc,
                        spotifySpKey = spKey.trim(),
                        spotifyTaste = snapshot,
                    )
                }
                _serverNotice.value = "✓ Spotify: ${snapshot.accountName} (${snapshot.topArtists.size} artistas, ${snapshot.allSeedTracks.size} temas)"
                refreshHome()
            }.onFailure { err ->
                _serverNotice.value = "✕ Spotify: ${err.message ?: "Error de sesión"}"
            }
            _spotifySyncing.value = false
        }
    }

    fun syncSpotifyTaste() {
        val s = serverState.value
        if (s.spotifySpDc.isBlank()) return
        connectSpotifyCookies(s.spotifySpDc, s.spotifySpKey)
    }

    fun setSpotifyBlendWeight(weight: Float) {
        viewModelScope.launch {
            serverRepo.update { it.copy(spotifyBlendWeight = weight.coerceIn(0f, 1f)) }
        }
    }

    fun disconnectSpotify() {
        viewModelScope.launch {
            spotifyClient.clearCachedToken()
            serverRepo.update {
                it.copy(
                    spotifySpDc = "",
                    spotifySpKey = "",
                    spotifyTaste = SpotifyTasteSnapshot(),
                )
            }
            _serverNotice.value = null
            refreshHome()
        }
    }

    fun importSpotifyPlaylist(playlist: SpotifyPlaylistSummary) {
        val session = serverState.value
        if (session.spotifySpDc.isBlank()) return
        viewModelScope.launch {
            _spotifySyncing.value = true
            _serverNotice.value = "⏳ ${playlist.name}..."
            val seedsRes = spotifyClient.fetchPlaylistTracks(
                spDc = session.spotifySpDc,
                spKey = session.spotifySpKey,
                playlistId = playlist.id,
                limit = 60,
            )
            seedsRes.onSuccess { seeds ->
                val matched = hybridEngine.matchSpotifyTracksToYtMusic(seeds, maxTracks = 50)
                if (matched.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        val pid = dao.insertPlaylist(UserPlaylistEntity(name = "${playlist.name} (Spotify)"))
                        matched.forEach { t ->
                            dao.addTrackToPlaylist(
                                PlaylistTrackEntity(
                                    playlistId = pid,
                                    trackId = t.id,
                                    title = t.title,
                                    artist = t.artist,
                                    album = t.album,
                                    durationSeconds = t.durationSeconds,
                                    thumbnailUrl = t.thumbnailUrl,
                                ),
                            )
                        }
                    }
                    _serverNotice.value = "✓ ${playlist.name}: ${matched.size} temas importados"
                } else {
                    _serverNotice.value = "✕ No se encontraron pistas para ${playlist.name}"
                }
            }.onFailure { err ->
                _serverNotice.value = "✕ ${err.message ?: "Error al importar"}"
            }
            _spotifySyncing.value = false
        }
    }

    fun clearServerNotice() {
        _serverNotice.value = null
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            val isFav = favorites.value.any { it.id == track.id }
            if (isFav) {
                dao.removeFavorite(track.id)
            } else {
                dao.addFavorite(
                    FavoriteTrackEntity(
                        id = track.id,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        durationSeconds = track.durationSeconds,
                        thumbnailUrl = track.thumbnailUrl,
                    ),
                )
            }
        }
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            val res = downloadManager.downloadTrack(track)
            res.onSuccess { file ->
                launch(Dispatchers.IO) {
                    dao.upsertDownload(
                        DownloadedTrackEntity(
                            id = track.id,
                            title = track.title,
                            artist = track.artist,
                            album = track.album,
                            durationSeconds = track.durationSeconds,
                            thumbnailUrl = track.thumbnailUrl,
                            fileSizeBytes = file.length(),
                        ),
                    )
                }
            }
        }
    }

    fun deleteDownload(trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadManager.deleteDownload(trackId)
            dao.deleteDownloadRecord(trackId)
        }
    }

    fun createPlaylist(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertPlaylist(UserPlaylistEntity(name = clean))
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deletePlaylist(playlistId)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.addTrackToPlaylist(
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    durationSeconds = track.durationSeconds,
                    thumbnailUrl = track.thumbnailUrl,
                ),
            )
        }
    }

    fun updateDesign(transform: (CroixDesignState) -> CroixDesignState) {
        viewModelScope.launch { designRepo.update(transform) }
    }

    fun applyPreset(preset: CroixDesignState) {
        viewModelScope.launch { designRepo.applyPreset(preset) }
    }

    override fun onCleared() {
        super.onCleared()
        shazamJob?.cancel()
        scrobbleJob?.cancel()
        discordRpc.disconnect()
        shakeDetector.release()
        playerController.release()
    }
}
