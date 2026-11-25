/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.croix.player.prime.data.DownloadedTrackEntity
import com.croix.player.prime.data.ListeningStatsSummary
import com.croix.player.prime.data.UserPlaylistEntity
import com.croix.prime.design.CroixGlassCard
import com.croix.prime.design.LocalCroixDesign
import com.croix.prime.design.LocalCroixPalette
import com.croix.prime.engine.Track

@Composable
fun SearchScreen(
    query: String,
    results: List<Track>,
    favoriteIds: Set<String>,
    downloadedIds: Set<String>,
    downloadProgress: Map<String, Float>,
    isLoading: Boolean,
    bottomPaddingDp: Int,
    onQueryChange: (String) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val palette = LocalCroixPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(start = 18.dp, end = 18.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Search", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            CroixGlassCard(cornerRadius = 50.dp, onClick = onOpenSettings) {
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Color.White)
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
                            "Tracks, artists, albums...",
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
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.White)
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = palette.primary)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = bottomPaddingDp.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(results, key = { it.id }) { track ->
                    GlassTrackRow(
                        track = track,
                        isFavorite = track.id in favoriteIds,
                        isDownloaded = track.id in downloadedIds,
                        downloadingProgress = downloadProgress[track.id],
                        onPlay = { onPlayTrack(track, results) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onOpenMenu = { onOpenTrackMenu(track) },
                    )
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    favorites: List<Track>,
    downloads: List<DownloadedTrackEntity>,
    playlists: List<UserPlaylistEntity>,
    playlistTracks: Map<Long, List<Track>>,
    stats: ListeningStatsSummary,
    downloadProgress: Map<String, Float>,
    bottomPaddingDp: Int,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val palette = LocalCroixPalette.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val favIds = remember(favorites) { favorites.map { it.id }.toSet() }
    val downloadedIds = remember(downloads) { downloads.map { it.id }.toSet() }
    val tabLabels = listOf(
        "Liked (${favorites.size})",
        "Playlists (${playlists.size})",
        "Offline (${downloads.size})",
        "Stats",
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
            Text("Favorites", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            CroixGlassCard(cornerRadius = 50.dp, onClick = onOpenSettings) {
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Color.White)
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

        when (selectedTab) {
            0 -> TrackListSection(
                tracks = favorites,
                emptyText = "Tap the heart on any track to save it to Favorites.",
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
                        TextButton(onClick = { selectedPlaylistId = null }) { Text("All Playlists") }
                    }
                    TrackListSection(
                        tracks = pTracks,
                        emptyText = "Add tracks to \"${openPlaylist.name}\" using the ⋮ menu.",
                        favIds = favIds,
                        downloadedIds = downloadedIds,
                        downloadProgress = downloadProgress,
                        bottomPaddingDp = bottomPaddingDp,
                        onPlayTrack = onPlayTrack,
                        onToggleFavorite = onToggleFavorite,
                        onOpenTrackMenu = onOpenTrackMenu,
                    )
                } else {
                    CroixGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentTint = palette.primary,
                        onClick = {
                            newPlaylistName = ""
                            showCreatePlaylist = true
                        },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Create New Playlist", color = Color.White, fontWeight = FontWeight.Bold)
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
                                        Text("$count tracks", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { onDeletePlaylist(pl.playlistId) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = palette.textSecondary)
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
                            "Downloaded tracks play offline with zero internet. Open ⋮ on any track to download.",
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
                                        Icon(Icons.Rounded.Delete, contentDescription = "Remove offline", tint = palette.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = bottomPaddingDp.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            CroixGlassCard(modifier = Modifier.weight(1f), accentTint = palette.primary) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("${stats.totalPlays}", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                                    Text("Total Plays", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            CroixGlassCard(modifier = Modifier.weight(1f), accentTint = palette.secondary) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("${stats.estimatedMinutes} min", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                                    Text("Listening Time", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    if (stats.topArtists.isNotEmpty()) {
                        item {
                            CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Top Artists (tap to open profile)", style = MaterialTheme.typography.titleMedium, color = Color.White)
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
                                                Text("$plays plays", color = palette.primary, style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (stats.topTracks.isNotEmpty()) {
                        item {
                            CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Most Played Tracks", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                    stats.topTracks.forEachIndexed { i, (track, plays) ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${i + 1}. ${track.title}", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                            Text("${plays}x", color = palette.secondary, style = MaterialTheme.typography.labelMedium)
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

    if (showCreatePlaylist) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    onCreatePlaylist(newPlaylistName)
                    showCreatePlaylist = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreatePlaylist = false }) { Text("Cancel") } },
        )
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
                Text("Play All", color = Color.White, fontWeight = FontWeight.Bold)
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
                Text("Shuffle", color = Color.White, fontWeight = FontWeight.Bold)
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
                    contentDescription = "Favorite",
                    tint = if (isFavorite) palette.primary else palette.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onOpenMenu, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Track Options",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
