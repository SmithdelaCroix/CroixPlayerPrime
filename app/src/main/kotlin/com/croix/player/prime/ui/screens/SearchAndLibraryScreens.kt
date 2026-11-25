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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.croix.player.prime.data.DownloadedTrackEntity
import com.croix.player.prime.data.HistoryTrackEntity
import com.croix.player.prime.data.ListeningStatsSummary
import com.croix.player.prime.data.UserPlaylistEntity
import com.croix.player.prime.ui.SearchFilterTab
import com.croix.player.prime.ui.ShazamPhase
import com.croix.player.prime.ui.ShazamUiState
import com.croix.prime.design.CroixAnimatedBackdrop
import com.croix.prime.design.CroixGlassCard
import com.croix.prime.design.LocalCroixDesign
import com.croix.prime.design.LocalCroixPalette
import com.croix.prime.design.LocalCroixStrings
import com.croix.prime.engine.AlbumSummary
import com.croix.prime.engine.RelatedArtist
import com.croix.prime.engine.Track
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SearchScreen(
    query: String,
    activeFilter: SearchFilterTab,
    results: List<Track>,
    artistResults: List<RelatedArtist>,
    albumResults: List<AlbumSummary>,
    recentSearches: List<String>,
    favoriteIds: Set<String>,
    downloadedIds: Set<String>,
    downloadProgress: Map<String, Float>,
    isLoading: Boolean,
    bottomPaddingDp: Int,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SearchFilterTab) -> Unit,
    onDeleteRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
    onOpenArtist: (String, String?) -> Unit,
    onOpenAlbum: (AlbumSummary) -> Unit,
    onOpenShazam: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val palette = LocalCroixPalette.current
    val strings = LocalCroixStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(start = 18.dp, end = 18.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(strings.searchTitle, style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CroixGlassCard(cornerRadius = 50.dp, accentTint = palette.secondary, onClick = onOpenShazam) {
                    IconButton(onClick = onOpenShazam, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = "Reconocer canción", tint = Color.White)
                    }
                }
                CroixGlassCard(cornerRadius = 50.dp, onClick = onOpenSettings) {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.Rounded.Settings, contentDescription = strings.settingsTitle, tint = Color.White)
                    }
                }
            }
        }

        CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.primary)
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            strings.searchPlaceholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.textSecondary,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                        cursorBrush = SolidColor(palette.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Limpiar", tint = Color.White)
                    }
                }
            }
        }

        // Filter Chips: Songs / Artists / Albums
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val filters = listOf(
                SearchFilterTab.SONGS to strings.filterSongs,
                SearchFilterTab.ARTISTS to strings.filterArtists,
                SearchFilterTab.ALBUMS to strings.filterAlbums,
            )
            filters.forEach { (tab, label) ->
                CroixGlassCard(
                    cornerRadius = 50.dp,
                    accentTint = if (activeFilter == tab) palette.primary else null,
                    onClick = { onFilterChange(tab) },
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    )
                }
            }
        }

        if (query.isBlank() && recentSearches.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(strings.recentSearches, style = MaterialTheme.typography.titleMedium, color = Color.White)
                TextButton(onClick = onClearRecentSearches) { Text(strings.clearAll) }
            }
            LazyColumn(
                contentPadding = PaddingValues(bottom = bottomPaddingDp.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(recentSearches, key = { it }) { item ->
                    CroixGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        onClick = { onQueryChange(item) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.History, null, tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(item, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { onDeleteRecentSearch(item) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "Borrar", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = palette.primary)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = bottomPaddingDp.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (activeFilter) {
                    SearchFilterTab.SONGS -> {
                        items(results, key = { it.id }) { track ->
                            GlassTrackRow(
                                track = track,
                                isFavorite = track.id in favoriteIds,
                                isDownloaded = track.id in downloadedIds,
                                downloadingProgress = downloadProgress[track.id],
                                // Start smart radio from the chosen track instead of queueing identically named search results
                                onPlay = { onPlayTrack(track, listOf(track)) },
                                onToggleFavorite = { onToggleFavorite(track) },
                                onOpenMenu = { onOpenTrackMenu(track) },
                            )
                        }
                    }
                    SearchFilterTab.ARTISTS -> {
                        items(artistResults, key = { it.browseId }) { artist ->
                            CroixGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenArtist(artist.name, artist.browseId) },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AsyncImage(
                                        model = artist.thumbnailUrl,
                                        contentDescription = artist.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(54.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(artist.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (artist.subscribers.isNotBlank()) {
                                            Text(artist.subscribers, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    SearchFilterTab.ALBUMS -> {
                        items(albumResults, key = { it.browseId }) { album ->
                            CroixGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenAlbum(album) },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AsyncImage(
                                        model = album.thumbnailUrl,
                                        contentDescription = album.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(album.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (album.year.isNotBlank()) {
                                            Text(album.year, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    favorites: List<Track>,
    historyEntities: List<HistoryTrackEntity>,
    downloads: List<DownloadedTrackEntity>,
    playlists: List<UserPlaylistEntity>,
    playlistTracks: Map<Long, List<Track>>,
    stats: ListeningStatsSummary,
    downloadProgress: Map<String, Float>,
    importStatus: String?,
    bottomPaddingDp: Int,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onImportPlaylistUrl: (String) -> Unit,
    onClearImportStatus: () -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onShareMusicalReceipt: () -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val palette = LocalCroixPalette.current
    val strings = LocalCroixStrings.current
    val es = strings.isSpanish
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showImportUrl by remember { mutableStateOf(false) }
    var importUrlInput by remember { mutableStateOf("") }

    val favIds = remember(favorites) { favorites.map { it.id }.toSet() }
    val downloadedIds = remember(downloads) { downloads.map { it.id }.toSet() }
    val tabLabels = listOf(
        "${strings.tabLiked} (${favorites.size})",
        "${strings.tabPlaylists} (${playlists.size})",
        "${strings.tabOffline} (${downloads.size})",
        if (es) "Historial (${historyEntities.size})" else "History (${historyEntities.size})",
        strings.tabStats,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, end = 18.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(strings.favoritesTitle, style = MaterialTheme.typography.headlineLarge, color = Color.White)
            CroixGlassCard(cornerRadius = 50.dp, onClick = onOpenSettings) {
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Rounded.Settings, contentDescription = strings.settingsTitle, tint = Color.White)
                }
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabLabels.forEachIndexed { idx, label ->
                CroixGlassCard(
                    cornerRadius = 50.dp,
                    accentTint = if (selectedTab == idx) palette.primary else null,
                    onClick = {
                        selectedTab = idx
                        selectedPlaylistId = null
                    },
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }

        if (importStatus != null) {
            CroixGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentTint = palette.primary,
                cornerRadius = 14.dp,
                onClick = onClearImportStatus,
            ) {
                Text(importStatus, color = Color.White, modifier = Modifier.padding(12.dp))
            }
        }

        when (selectedTab) {
            0 -> TrackListSection(
                tracks = favorites,
                emptyText = if (es) "Toca el corazón en cualquier canción para guardarla aquí." else "Tap the heart on any track to save it here.",
                favIds = favIds,
                downloadedIds = downloadedIds,
                downloadProgress = downloadProgress,
                bottomPaddingDp = bottomPaddingDp,
                onPlayTrack = onPlayTrack,
                onToggleFavorite = onToggleFavorite,
                onOpenTrackMenu = onOpenTrackMenu,
            )

            1 -> {
                val openPlaylist = playlists.firstOrNull { it.playlistId == selectedPlaylistId }
                if (openPlaylist != null) {
                    val pTracks = playlistTracks[openPlaylist.playlistId].orEmpty()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(openPlaylist.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
                        TextButton(onClick = { selectedPlaylistId = null }) { Text(strings.tabPlaylists) }
                    }
                    TrackListSection(
                        tracks = pTracks,
                        emptyText = if (es) "Añade canciones desde el menú ⋮." else "Add tracks using the ⋮ menu.",
                        favIds = favIds,
                        downloadedIds = downloadedIds,
                        downloadProgress = downloadProgress,
                        bottomPaddingDp = bottomPaddingDp,
                        onPlayTrack = onPlayTrack,
                        onToggleFavorite = onToggleFavorite,
                        onOpenTrackMenu = onOpenTrackMenu,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        CroixGlassCard(
                            modifier = Modifier.weight(1f),
                            accentTint = palette.primary,
                            onClick = {
                                newPlaylistName = ""
                                showCreatePlaylist = true
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(Icons.Rounded.Add, null, tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.createPlaylist, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        CroixGlassCard(
                            modifier = Modifier.weight(1f),
                            accentTint = palette.secondary,
                            onClick = {
                                importUrlInput = ""
                                showImportUrl = true
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(Icons.Rounded.Link, null, tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.importPlaylistUrl, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = bottomPaddingDp.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(playlists, key = { it.playlistId }) { pl ->
                            val count = playlistTracks[pl.playlistId]?.size ?: 0
                            CroixGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectedPlaylistId = pl.playlistId },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(pl.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("$count ${strings.filterSongs.lowercase()}", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { onDeletePlaylist(pl.playlistId) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Eliminar", tint = palette.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                if (downloads.isEmpty()) {
                    CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (es) "Las canciones descargadas suenan sin internet. Abre ⋮ en cualquier canción para descargar." else "Downloaded tracks play offline with zero internet. Open ⋮ on any track to download.",
                            color = palette.textSecondary,
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                } else {
                    val dlTracks = downloads.map { it.toTrack() }
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = bottomPaddingDp.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(downloads, key = { it.id }) { item ->
                            val mb = "%.1f MB".format(item.fileSizeBytes / (1024f * 1024f))
                            CroixGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onPlayTrack(item.toTrack(), dlTracks) },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AsyncImage(
                                        model = item.thumbnailUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${item.artist} • $mb • Offline", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    }
                                    IconButton(onClick = { onDeleteDownload(item.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Quitar offline", tint = palette.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab 3: Date-Grouped History ("Hoy / Ayer / Esta semana / Anteriores")
            3 -> {
                DateGroupedHistorySection(
                    historyEntities = historyEntities,
                    favIds = favIds,
                    downloadedIds = downloadedIds,
                    downloadProgress = downloadProgress,
                    bottomPaddingDp = bottomPaddingDp,
                    isSpanish = es,
                    onPlayTrack = onPlayTrack,
                    onToggleFavorite = onToggleFavorite,
                    onOpenTrackMenu = onOpenTrackMenu,
                )
            }

            // Tab 4: Pro Stats + Hourly/Weekday Bars + Shareable Musical Receipt PNG
            4 -> {
                ProStatsAndReceiptSection(
                    stats = stats,
                    isSpanish = es,
                    bottomPaddingDp = bottomPaddingDp,
                    onOpenArtist = onOpenArtist,
                    onShareReceipt = onShareMusicalReceipt,
                )
            }
        }
    }

    if (showCreatePlaylist) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false },
            title = { Text(strings.createPlaylist) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text(if (es) "Nombre de la playlist" else "Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    onCreatePlaylist(newPlaylistName)
                    showCreatePlaylist = false
                }) { Text(strings.create) }
            },
            dismissButton = { TextButton(onClick = { showCreatePlaylist = false }) { Text(strings.cancel) } },
        )
    }

    if (showImportUrl) {
        AlertDialog(
            onDismissRequest = { showImportUrl = false },
            title = { Text(strings.importPlaylistUrl) },
            text = {
                OutlinedTextField(
                    value = importUrlInput,
                    onValueChange = { importUrlInput = it },
                    label = { Text(strings.importPlaylistHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    enabled = importUrlInput.isNotBlank(),
                    onClick = {
                        onImportPlaylistUrl(importUrlInput)
                        showImportUrl = false
                    },
                ) { Text(strings.importButton) }
            },
            dismissButton = { TextButton(onClick = { showImportUrl = false }) { Text(strings.cancel) } },
        )
    }
}

@Composable
private fun DateGroupedHistorySection(
    historyEntities: List<HistoryTrackEntity>,
    favIds: Set<String>,
    downloadedIds: Set<String>,
    downloadProgress: Map<String, Float>,
    bottomPaddingDp: Int,
    isSpanish: Boolean,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
) {
    val palette = LocalCroixPalette.current
    if (historyEntities.isEmpty()) {
        CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                if (isSpanish) "Tu historial agrupado por fecha aparecerá aquí conforme escuches música." else "Your date-grouped listening history will appear here.",
                color = palette.textSecondary,
                modifier = Modifier.padding(18.dp),
            )
        }
        return
    }

    val grouped = remember(historyEntities, isSpanish) {
        val nowCal = Calendar.getInstance()
        val todayYear = nowCal.get(Calendar.YEAR)
        val todayDay = nowCal.get(Calendar.DAY_OF_YEAR)
        val weekAgoMs = System.currentTimeMillis() - 7L * 86_400_000L

        val buckets = linkedMapOf<String, MutableList<HistoryTrackEntity>>()
        val labelToday = if (isSpanish) "Hoy" else "Today"
        val labelYesterday = if (isSpanish) "Ayer" else "Yesterday"
        val labelWeek = if (isSpanish) "Esta semana" else "This week"
        val labelOlder = if (isSpanish) "Anteriores" else "Earlier"

        val cal = Calendar.getInstance()
        historyEntities.forEach { item ->
            cal.timeInMillis = item.playedAtMs
            val y = cal.get(Calendar.YEAR)
            val d = cal.get(Calendar.DAY_OF_YEAR)
            val key = when {
                y == todayYear && d == todayDay -> labelToday
                y == todayYear && d == todayDay - 1 -> labelYesterday
                item.playedAtMs >= weekAgoMs -> labelWeek
                else -> labelOlder
            }
            buckets.getOrPut(key) { mutableListOf() }.add(item)
        }
        buckets.toList()
    }

    val allTracks = remember(historyEntities) { historyEntities.map { it.toTrack() } }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomPaddingDp.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        grouped.forEach { (sectionLabel, itemsInBucket) ->
            item(key = "hdr_$sectionLabel") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(sectionLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primary)
                    Text("${itemsInBucket.size} temas", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                }
            }
            items(itemsInBucket, key = { "hist_${sectionLabel}_${it.id}" }) { row ->
                val track = row.toTrack()
                val timeStr = timeFmt.format(Date(row.playedAtMs))
                GlassTrackRow(
                    track = track.copy(artist = "${track.artist} • ${row.playCount}x • $timeStr"),
                    isFavorite = track.id in favIds,
                    isDownloaded = track.id in downloadedIds,
                    downloadingProgress = downloadProgress[track.id],
                    onPlay = { onPlayTrack(track, allTracks) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onOpenMenu = { onOpenTrackMenu(track) },
                )
            }
        }
    }
}

@Composable
private fun ProStatsAndReceiptSection(
    stats: ListeningStatsSummary,
    isSpanish: Boolean,
    bottomPaddingDp: Int,
    onOpenArtist: (String) -> Unit,
    onShareReceipt: () -> Unit,
) {
    val palette = LocalCroixPalette.current
    val dayNames = if (isSpanish) listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    else listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomPaddingDp.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CroixGlassCard(modifier = Modifier.weight(1f), accentTint = palette.primary) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${stats.totalPlays}", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text(if (isSpanish) "Reproducciones" else "Total Plays", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                CroixGlassCard(modifier = Modifier.weight(1f), accentTint = palette.secondary) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${stats.estimatedMinutes} min", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text(if (isSpanish) "Tiempo escuchado" else "Listening Time", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Hourly Activity Chart (00h..23h)
        item {
            val maxHour = (stats.hourlyPlays.maxOrNull() ?: 1).coerceAtLeast(1)
            CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isSpanish) "Actividad por hora del día" else "Listening by hour of day", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        if (stats.peakHour in 0..23) {
                            Text(
                                text = "Pico ${stats.peakHour.toString().padStart(2, '0')}:00h",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(74.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        stats.hourlyPlays.forEachIndexed { hour, count ->
                            val frac = (count.toFloat() / maxHour.toFloat()).coerceIn(0.08f, 1f)
                            val isPeak = hour == stats.peakHour && count > 0
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(frac)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (isPeak) palette.primary else if (count > 0) palette.secondary.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.14f)),
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("00h", "06h", "12h", "18h", "23h").forEach { lbl ->
                            Text(lbl, fontSize = 10.sp, color = palette.textSecondary)
                        }
                    }
                }
            }
        }

        // Weekday Activity Chart (Lun..Dom)
        item {
            val maxDay = (stats.weekdayPlays.maxOrNull() ?: 1).coerceAtLeast(1)
            CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (isSpanish) "Actividad por día de la semana" else "Listening by day of week", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(84.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        stats.weekdayPlays.forEachIndexed { idx, count ->
                            val frac = (count.toFloat() / maxDay.toFloat()).coerceIn(0.10f, 1f)
                            val isPeak = idx == stats.peakWeekday && count > 0
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .weight(1f),
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(frac)
                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                            .background(
                                                if (isPeak) Brush.verticalGradient(listOf(palette.primary, palette.secondary))
                                                else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.16f))),
                                            ),
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(dayNames.getOrElse(idx) { "" }, fontSize = 11.sp, color = if (isPeak) Color.White else palette.textSecondary, fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }

        // Shareable Thermal Musical Receipt Preview ("CROIX RECEIPT")
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF7F5EE))
                    .padding(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.ReceiptLong, null, tint = Color(0xFF141418), modifier = Modifier.size(20.dp))
                            Text("CROIX RECEIPT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color(0xFF141418))
                        }
                        Text("${stats.totalPlays} PLAYS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF4A4A55))
                    }
                    Text("--------------------------------------", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF8A8A96), maxLines = 1)
                    if (stats.topTracks.isEmpty()) {
                        Text("01  Escucha canciones para llenar tu recibo", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFF33333D))
                    } else {
                        stats.topTracks.take(5).forEachIndexed { idx, (track, plays) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "${(idx + 1).toString().padStart(2, '0')} ${track.title}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1A1A22),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("${plays}x", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1A1A22))
                            }
                        }
                    }
                    Text("--------------------------------------", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF8A8A96), maxLines = 1)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TIEMPO TOTAL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF141418))
                        Text("${stats.estimatedMinutes} MIN", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF141418))
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = onShareReceipt,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isSpanish) "Compartir Recibo Musical (PNG)" else "Share Musical Receipt (PNG)")
                    }
                }
            }
        }

        if (stats.topArtists.isNotEmpty()) {
            item {
                CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (isSpanish) "Top Artistas" else "Top Artists", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        stats.topArtists.forEachIndexed { i, (artist, plays) ->
                            CroixGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 10.dp,
                                onClick = { onOpenArtist(artist) },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("${i + 1}. $artist", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text("${plays}x", color = palette.primary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen Shazam Acoustic Recognition Overlay with animated soundwave radar.
 */
@Composable
fun ShazamRadarOverlay(
    state: ShazamUiState,
    onStartListening: () -> Unit,
    onPlayMatch: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalCroixPalette.current
    val es = LocalCroixStrings.current.isSpanish
    val blocker = remember { MutableInteractionSource() }
    val pulse = rememberInfiniteTransition(label = "shazamPulse")
    val ring1 by pulse.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.48f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "ring1",
    )
    val ring2 by pulse.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.78f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "ring2",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = blocker, indication = null) {},
    ) {
        CroixAnimatedBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        if (es) "Reconocer Música" else "Identify Song",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Text(
                        if (es) "Huella acústica en vivo • Shazam Engine" else "Live acoustic fingerprinting",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
                CroixGlassCard(cornerRadius = 50.dp, onClick = onClose) {
                    IconButton(onClick = onClose, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }
            }

            // Center Radar or Found Match Card
            when (state.phase) {
                ShazamPhase.FOUND -> {
                    val m = state.match
                    if (m != null) {
                        CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = palette.primary) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                AsyncImage(
                                    model = m.coverUrl.ifBlank { state.matchedTrack?.thumbnailUrl.orEmpty() },
                                    contentDescription = m.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(190.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color.White.copy(alpha = 0.08f)),
                                )
                                Text(m.title, style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
                                Text(m.artist, style = MaterialTheme.typography.titleMedium, color = palette.primary, textAlign = TextAlign.Center)
                                if (m.album.isNotBlank() || m.genre.isNotBlank()) {
                                    Text(
                                        listOf(m.album, m.genre).filter { it.isNotBlank() }.joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Button(onClick = onPlayMatch, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Rounded.PlayArrow, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (es) "Reproducir ahora" else "Play Now")
                                }
                                OutlinedButton(onClick = onStartListening, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (es) "Escuchar otra canción" else "Identify another")
                                }
                            }
                        }
                    }
                }
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        val active = state.phase == ShazamPhase.LISTENING || state.phase == ShazamPhase.ANALYZING
                        val reactiveBoost = 1f + state.micLevel * 0.35f
                        Box(
                            modifier = Modifier.size(230.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (active) {
                                Box(
                                    modifier = Modifier
                                        .size(150.dp)
                                        .scale(ring2 * reactiveBoost)
                                        .clip(CircleShape)
                                        .background(palette.secondary.copy(alpha = 0.16f)),
                                )
                                Box(
                                    modifier = Modifier
                                        .size(150.dp)
                                        .scale(ring1 * reactiveBoost)
                                        .clip(CircleShape)
                                        .background(palette.primary.copy(alpha = 0.26f)),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(144.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(palette.primary, palette.secondary)),
                                    )
                                    .clickable(enabled = !active, onClick = onStartListening),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (state.phase == ShazamPhase.ANALYZING) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
                                } else {
                                    Icon(
                                        if (state.phase == ShazamPhase.LISTENING) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                                        contentDescription = "Escuchar",
                                        tint = Color.White,
                                        modifier = Modifier.size(58.dp),
                                    )
                                }
                            }
                        }

                        Text(
                            text = when (state.phase) {
                                ShazamPhase.IDLE -> if (es) "Toca el círculo para escuchar música cercana" else "Tap to listen to nearby music"
                                ShazamPhase.LISTENING -> if (es) "Escuchando audio del micrófono..." else "Listening to audio..."
                                ShazamPhase.ANALYZING -> if (es) "Analizando huella acústica..." else "Analyzing acoustic fingerprint..."
                                ShazamPhase.ERROR -> state.error ?: "No se encontró coincidencia"
                                ShazamPhase.FOUND -> ""
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (state.phase == ShazamPhase.ERROR) Color(0xFFFF8A80) else Color.White,
                            textAlign = TextAlign.Center,
                        )

                        if (state.phase == ShazamPhase.LISTENING) {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50.dp)),
                                color = palette.primary,
                                trackColor = Color.White.copy(alpha = 0.15f),
                            )
                        }

                        if (state.phase == ShazamPhase.ERROR) {
                            Button(onClick = onStartListening) {
                                Text(if (es) "Intentar de nuevo" else "Try again")
                            }
                        }
                    }
                }
            }

            Text(
                text = if (es) "El audio se analiza solo para reconocer la canción y no se almacena." else "Audio is fingerprinted transiently and never stored.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TrackListSection(
    tracks: List<Track>,
    emptyText: String,
    favIds: Set<String>,
    downloadedIds: Set<String>,
    downloadProgress: Map<String, Float>,
    bottomPaddingDp: Int,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
) {
    val palette = LocalCroixPalette.current
    val strings = LocalCroixStrings.current
    if (tracks.isEmpty()) {
        CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(emptyText, color = palette.textSecondary, modifier = Modifier.padding(18.dp))
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        CroixGlassCard(
            modifier = Modifier.weight(1f),
            accentTint = palette.primary,
            onClick = { onPlayTrack(tracks.first(), tracks) },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text(strings.playAll, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        CroixGlassCard(
            modifier = Modifier.weight(1f),
            onClick = {
                val s = tracks.shuffled()
                onPlayTrack(s.first(), s)
            },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Shuffle, null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text(strings.shuffle, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomPaddingDp.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(tracks, key = { it.id }) { track ->
            GlassTrackRow(
                track = track,
                isFavorite = track.id in favIds,
                isDownloaded = track.id in downloadedIds,
                downloadingProgress = downloadProgress[track.id],
                onPlay = { onPlayTrack(track, tracks) },
                onToggleFavorite = { onToggleFavorite(track) },
                onOpenMenu = { onOpenTrackMenu(track) },
            )
        }
    }
}

@Composable
fun GlassTrackRow(
    track: Track,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    downloadingProgress: Float?,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current

    CroixGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onPlay,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape((design.cornerRadiusDp - 8f).coerceAtLeast(8f).dp))
                    .background(Color.White.copy(alpha = 0.08f)),
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (isDownloaded) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = palette.primary, modifier = Modifier.size(13.dp))
                    }
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (downloadingProgress != null) {
                CircularProgressIndicator(
                    progress = { downloadingProgress },
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = palette.primary,
                )
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (isFavorite) palette.primary else palette.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onOpenMenu, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Opciones",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
