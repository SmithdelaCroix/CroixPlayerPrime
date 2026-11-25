/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.croix.prime.design.CroixGlassCard
import com.croix.prime.design.LocalCroixDesign
import com.croix.prime.design.LocalCroixPalette
import com.croix.prime.engine.Track
import dev.chrisbanes.haze.HazeState

@Composable
fun SearchScreen(
    query: String,
    results: List<Track>,
    favoriteIds: Set<String>,
    isLoading: Boolean,
    hazeState: HazeState,
    bottomPaddingDp: Int,
    onQueryChange: (String) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
) {
    val palette = LocalCroixPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Search", style = MaterialTheme.typography.headlineLarge, color = Color.White)

        CroixGlassCard(
            modifier = Modifier.fillMaxWidth(),
            hazeState = hazeState,
        ) {
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
                            "Songs, artists, albums on YouTube Music...",
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
                        hazeState = hazeState,
                        onPlay = { onPlayTrack(track, results) },
                        onToggleFavorite = { onToggleFavorite(track) },
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(
    favorites: List<Track>,
    history: List<Track>,
    hazeState: HazeState,
    bottomPaddingDp: Int,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
) {
    val palette = LocalCroixPalette.current
    var tab by remember { mutableIntStateOf(0) }
    val activeList = if (tab == 0) favorites else history
    val favIds = remember(favorites) { favorites.map { it.id }.toSet() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Library", style = MaterialTheme.typography.headlineLarge, color = Color.White)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Favorites (${favorites.size})", "History (${history.size})").forEachIndexed { idx, label ->
                CroixGlassCard(
                    modifier = Modifier.weight(1f),
                    hazeState = hazeState,
                    accentTint = if (tab == idx) palette.primary else null,
                    onClick = { tab = idx },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                }
            }
        }

        if (activeList.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CroixGlassCard(
                    modifier = Modifier.weight(1f),
                    hazeState = hazeState,
                    accentTint = palette.primary,
                    onClick = { onPlayTrack(activeList.first(), activeList) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
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
                    hazeState = hazeState,
                    onClick = {
                        val shuffled = activeList.shuffled()
                        onPlayTrack(shuffled.first(), shuffled)
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Shuffle, null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (activeList.isEmpty()) {
            CroixGlassCard(
                modifier = Modifier.fillMaxWidth(),
                hazeState = hazeState,
            ) {
                Text(
                    text = if (tab == 0) "Tap the heart on any song to save it here." else "Songs you play will appear here.",
                    color = palette.textSecondary,
                    modifier = Modifier.padding(20.dp),
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = bottomPaddingDp.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(activeList, key = { it.id }) { track ->
                    GlassTrackRow(
                        track = track,
                        isFavorite = track.id in favIds,
                        hazeState = hazeState,
                        onPlay = { onPlayTrack(track, activeList) },
                        onToggleFavorite = { onToggleFavorite(track) },
                    )
                }
            }
        }
    }
}

@Composable
fun GlassTrackRow(
    track: Track,
    isFavorite: Boolean,
    hazeState: HazeState,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current

    CroixGlassCard(
        modifier = Modifier.fillMaxWidth(),
        hazeState = hazeState,
        onClick = onPlay,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
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
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) palette.primary else palette.textSecondary,
                )
            }
        }
    }
}
