/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.ui

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.croix.player.prime.data.AppUpdateChecker
import com.croix.player.prime.data.AppUpdateInfo
import com.croix.player.prime.data.CroixDatabase
import com.croix.player.prime.data.DownloadedTrackEntity
import com.croix.player.prime.data.FavoriteTrackEntity
import com.croix.player.prime.data.HistoryTrackEntity
import com.croix.player.prime.data.ListeningStatsSummary
import com.croix.player.prime.data.PlaylistTrackEntity
import com.croix.player.prime.data.RecentSearchEntity
import com.croix.player.prime.data.ServerSessionRepository
import com.croix.player.prime.data.ServerSessionState
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
import com.croix.prime.engine.HomeFeedBundle
import com.croix.prime.engine.HomeSection
import com.croix.prime.engine.HybridAlgorithmEngine
import com.croix.prime.engine.InnerTubeClient
import com.croix.prime.engine.LrclibClient
import com.croix.prime.engine.LyricsResult
import com.croix.prime.engine.MoodChip
import com.croix.prime.engine.RelatedArtist
import com.croix.prime.engine.ServerConnectionConfig
import com.croix.prime.engine.ServerHealthStatus
import com.croix.prime.engine.SponsorBlockClient
import com.croix.prime.engine.SpotifyPlaylistSummary
import com.croix.prime.engine.SpotifyTasteClient
import com.croix.prime.engine.SpotifyTasteSnapshot
import com.croix.prime.engine.Track
import com.croix.prime.playback.CroixDownloadManager
import com.croix.prime.playback.CroixPlayerController
import com.croix.prime.playback.CroixShakeDetector
import com.croix.prime.playback.PlaybackUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SearchFilterTab { SONGS, ARTISTS, ALBUMS }

class CroixPrimeViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val innerTube = InnerTubeClient()
    private val sponsorBlock = SponsorBlockClient()
    private val lrclib = LrclibClient()
    private val spotifyClient = SpotifyTasteClient()
    private val hybridEngine = HybridAlgorithmEngine(innerTube)
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

    val playerController = CroixPlayerController(
        context = appContext,
        innerTube = innerTube,
        sponsorBlock = sponsorBlock,
        localFileProvider = { id -> downloadManager.getDownloadedFile(id) },
        enabledCategoriesProvider = { designState.value.enabledSponsorCategories() },
        crossfadeSecondsProvider = { designState.value.crossfadeSeconds },
        normalizeAudioProvider = { designState.value.normalizeAudio },
        autoPlayRelatedProvider = { designState.value.autoPlayRelated },
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

    val downloads: StateFlow<List<DownloadedTrackEntity>> = dao.observeDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<UserPlaylistEntity>> = dao.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlistTracks: StateFlow<Map<Long, List<Track>>> = dao.observeAllPlaylistTracks()
        .map { rows -> rows.groupBy({ it.playlistId }, { it.toTrack() }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val listeningStats: StateFlow<ListeningStatsSummary> = historyEntities
        .map { rows ->
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
            ListeningStatsSummary(totalPlays, estMin, topArtists, topTracks)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListeningStatsSummary())

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

    private val _albumPrimary = MutableStateFlow<Color?>(null)
    val albumPrimary: StateFlow<Color?> = _albumPrimary.asStateFlow()

    private val _albumSecondary = MutableStateFlow<Color?>(null)
    val albumSecondary: StateFlow<Color?> = _albumSecondary.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeServerConfig()
        refreshHome()
        checkForUpdates()
        observeCurrentTrackSideEffects()
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
                    launch(Dispatchers.IO) {
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
                                playedAtMs = System.currentTimeMillis(),
                            ),
                        )
                    }
                    launch {
                        _lyricsLoading.value = true
                        _lyrics.value = lrclib.fetchLyrics(track)
                        _lyricsLoading.value = false
                    }
                    launch(Dispatchers.IO) {
                        extractArtworkColors(track.thumbnailUrl)
                    }
                }
        }
    }

    private suspend fun extractArtworkColors(url: String) {
        if (url.isBlank()) return
        runCatching {
            val req = ImageRequest.Builder(appContext)
                .data(url)
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(req)
            if (result is SuccessResult) {
                val bmp = result.image.toBitmap()
                val p = Palette.from(bmp).generate()
                val vibrant = p.getVibrantColor(0xFFA238FF.toInt())
                val lightOrMuted = p.getLightVibrantColor(p.getMutedColor(0xFF00E5FF.toInt()))
                _albumPrimary.value = Color(vibrant)
                _albumSecondary.value = Color(lightOrMuted)
            }
        }
    }

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
            val bundle = hybridEngine.buildRichHomeFeed(
                ytSections = ytSections,
                spotifyTaste = session.spotifyTaste.takeIf { session.isSpotifyConnected },
                localFavorites = favorites.value,
                localHistoryRecords = records,
                blendWeight = session.spotifyBlendWeight,
                isSpanish = isSpanish,
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
            val queue = hybridEngine.buildHybridFlowQueue(
                spotifyTaste = session.spotifyTaste.takeIf { session.isSpotifyConnected },
                localFavorites = favorites.value,
                localHistory = history.value,
                fallbackTracks = fallback,
                blendWeight = session.spotifyBlendWeight,
                moodQuery = mood?.query,
            )
            if (queue.isNotEmpty()) {
                playerController.playTrack(queue.first(), queue, autoFetchRadio = true)
            }
            _adsBlockedCount.value = AdBlockEngine.blockedCount.get()
        }
    }

    fun playMood(mood: MoodChip) {
        viewModelScope.launch {
            val tracks = innerTube.searchSongs(mood.query, limit = 18)
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
            kotlinx.coroutines.delay(350L)
            val trimmed = q.trim()
            if (trimmed.length >= 2) {
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
                    if (matched.isEmpty()) error("Could not match Spotify tracks on YouTube Music")
                    title to matched
                }
            } else {
                innerTube.importPlaylistFromUrl(trimmed)
            }
            res.onSuccess { (title, tracks) ->
                kotlinx.coroutines.withContext(Dispatchers.IO) {
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
                _serverNotice.value = "✕ ${err.message ?: "YT Music login failed"}"
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
                _serverNotice.value = "✓ Spotify: ${snapshot.accountName} (${snapshot.topArtists.size} artists, ${snapshot.allSeedTracks.size} tracks)"
                refreshHome()
            }.onFailure { err ->
                _serverNotice.value = "✕ Spotify: ${err.message ?: "Session error"}"
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
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
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
                    _serverNotice.value = "✓ ${playlist.name}: ${matched.size} tracks imported"
                } else {
                    _serverNotice.value = "✕ No playable tracks matched for ${playlist.name}"
                }
            }.onFailure { err ->
                _serverNotice.value = "✕ ${err.message ?: "Import error"}"
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
        shakeDetector.release()
        playerController.release()
    }
}
