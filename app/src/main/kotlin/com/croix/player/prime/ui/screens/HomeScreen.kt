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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.croix.prime.design.CroixGlassCard
import com.croix.prime.design.LocalCroixDesign
import com.croix.prime.design.LocalCroixPalette
import com.croix.prime.design.PrimeCardDensity
import com.croix.prime.engine.HomeSection
import com.croix.prime.engine.MoodChip
import com.croix.prime.engine.Track
import dev.chrisbanes.haze.HazeState

@Composable
fun HomeScreen(
    sections: List<HomeSection>,
    recentTracks: List<Track>,
    moods: List<MoodChip>,
    isLoading: Boolean,
    hazeState: HazeState,
    bottomPaddingDp: Int,
    onPlayFlow: () -> Unit,
    onPlayMood: (MoodChip) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onRefresh: () -> Unit,
    onOpenStudio: () -> Unit,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val cardSize = when (design.cardDensity) {
        PrimeCardDensity.COMPACT -> 124.dp
        PrimeCardDensity.EDITORIAL -> 148.dp
        PrimeCardDensity.SPACIOUS -> 168.dp
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 18.dp, bottom = bottomPaddingDp.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Deezer-style Bold Editorial Header + Quick Studio Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Croix Prime",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                    )
                    Text(
                        text = "Music • Translucent & Yours",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CroixGlassCard(
                        hazeState = hazeState,
                        cornerRadius = 16.dp,
                        onClick = onRefresh,
                    ) {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                    }
                    CroixGlassCard(
                        hazeState = hazeState,
                        cornerRadius = 16.dp,
                        accentTint = palette.primary,
                        onClick = onOpenStudio,
                    ) {
                        IconButton(onClick = onOpenStudio, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Rounded.Tune, contentDescription = "Croix Studio", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Deezer Flow-Inspired Translucent Hero Card
        if (design.showCroixFlowHero) {
            item {
                CroixGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .height(152.dp),
                    hazeState = hazeState,
                    accentTint = palette.primary,
                    onClick = onPlayFlow,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        palette.primary.copy(alpha = 0.42f),
                                        palette.secondary.copy(alpha = 0.28f),
                                        Color.Transparent,
                                    ),
                                ),
                            )
                            .padding(20.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "INFINITE MIX",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.85f),
                                    )
                                }
                                Text(
                                    text = "Croix Flow",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                )
                                Text(
                                    text = "Endless soundtrack blended for you, ad-free with SponsorBlock.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.78f),
                                    maxLines = 2,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = "Play Croix Flow",
                                    tint = Color(0xFF09080F),
                                    modifier = Modifier.size(34.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Translucent Deezer Mood Chips
        if (design.showMoodChips) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(moods) { mood ->
                        CroixGlassCard(
                            hazeState = hazeState,
                            cornerRadius = 50.dp,
                            accentTint = Color(mood.accentHex),
                            onClick = { onPlayMood(mood) },
                        ) {
                            Text(
                                text = mood.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }

        // Recently Played Section
        if (design.showRecentSection && recentTracks.isNotEmpty()) {
            item {
                EditorialTrackCarousel(
                    title = "Jump Back In",
                    tracks = recentTracks.take(12),
                    cardSize = cardSize,
                    hazeState = hazeState,
                    onPlayTrack = { t -> onPlayTrack(t, recentTracks) },
                )
            }
        }

        if (isLoading && sections.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = palette.primary)
                }
            }
        }

        // YouTube Music Home Shelves
        items(sections) { section ->
            EditorialTrackCarousel(
                title = section.title,
                tracks = section.tracks,
                cardSize = cardSize,
                hazeState = hazeState,
                onPlayTrack = { t -> onPlayTrack(t, section.tracks) },
            )
        }
    }
}

@Composable
private fun EditorialTrackCarousel(
    title: String,
    tracks: List<Track>,
    cardSize: androidx.compose.ui.unit.Dp,
    hazeState: HazeState,
    onPlayTrack: (Track) -> Unit,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(tracks, key = { it.id }) { track ->
                CroixGlassCard(
                    modifier = Modifier.width(cardSize),
                    hazeState = hazeState,
                    onClick = { onPlayTrack(track) },
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(cardSize - 20.dp)
                                .clip(RoundedCornerShape((design.cornerRadiusDp - 6f).coerceAtLeast(8f).dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                        )
                        Spacer(Modifier.height(10.dp))
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
                }
            }
        }
    }
}
