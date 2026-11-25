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
import com.croix.player.prime.data.CroixDatabase
import com.croix.player.prime.data.DownloadedTrackEntity
import com.croix.player.prime.data.FavoriteTrackEntity
import com.croix.player.prime.data.HistoryTrackEntity
import com.croix.player.prime.data.ListeningStatsSummary
import com.croix.player.prime.data.PlaylistTrackEntity
import com.croix.player.prime.data.UserPlaylistEntity
import com.croix.prime.design.CroixDesignRepository
import com.croix.prime.design.CroixDesignState
import com.croix.prime.design.PrimePresets
import com.croix.prime.engine.AlbumDetail
import com.croix.prime.engine.AlbumSummary
import com.croix.prime.engine.ArtistProfile
import com.croix.prime.engine.HomeSection
import com.croix.prime.engine.InnerTubeClient
import com.croix.prime.engine.LrclibClient
import com.croix.prime.engine.LyricsResult
import com.croix.prime.engine.MoodChip
import com.croix.prime.engine.SponsorBlockClient
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

class CroixPrimeViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val innerTube = InnerTubeClient()
    private val sponsorBlock = SponsorBlockClient()
    private val lrclib = LrclibClient()
    private val dao = CroixDatabase.get(appContext).dao()
    private val designRepo = CroixDesignRepository(appContext)
    private val imageLoader = ImageLoader(appContext)
    val downloadManager = CroixDownloadManager(appContext, innerTube)

    val designState: StateFlow<CroixDesignState> = designRepo.designFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PrimePresets.PrimeVioletGlass,
    )

    val playerController = CroixPlayerController(
        context = appContext,
        innerTube = innerTube,
        sponsorBlock = sponsorBlock,
        localFileProvider = { id -> downloadManager.getDownloadedFile(id) },
        enabledCategoriesProvider = { designState.value.enabledSponsorCategories() },
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

    private val _homeSections = MutableStateFlow<List<HomeSection>>(emptyList())
    val homeSections: StateFlow<List<HomeSection>> = _homeSections.asStateFlow()

    private val _homeLoading = MutableStateFlow(true)
    val homeLoading: StateFlow<Boolean> = _homeLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

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
        refreshHome()
        observeCurrentTrackSideEffects()
        observeShakePreference()
    }

    private fun observeShakePreference() {
        viewModelScope.launch {
            designState.collect { cfg ->
                shakeDetector.configure(cfg.shakeToSkipEnabled, cfg.shakeSensitivity)
            }
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
            _homeSections.value = innerTube.fetchHomeSections()
            _homeLoading.value = false
        }
    }

    fun startCroixFlow() {
        viewModelScope.launch {
            val seed = favorites.value.firstOrNull()
                ?: history.value.firstOrNull()
                ?: _homeSections.value.firstOrNull()?.tracks?.firstOrNull()
            if (seed != null) {
                playerController.playTrack(seed, autoFetchRadio = true)
            } else {
                val hits = innerTube.searchSongs("top global music hits", limit = 15)
                if (hits.isNotEmpty()) {
                    playerController.playTrack(hits.first(), hits, autoFetchRadio = true)
                }
            }
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
        searchJob?.cancel()
        if (q.isBlank()) {
            _searchResults.value = emptyList()
            _searchLoading.value = false
            return
        }
        searchJob = viewModelScope.launch {
            _searchLoading.value = true
            kotlinx.coroutines.delay(350L)
            _searchResults.value = innerTube.searchSongs(q, limit = 25)
            _searchLoading.value = false
        }
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
