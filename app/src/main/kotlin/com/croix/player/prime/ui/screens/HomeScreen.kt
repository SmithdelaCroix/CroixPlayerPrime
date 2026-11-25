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
import androidx.compose.material.icons.rounded.Settings
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
    onOpenSettings: () -> Unit,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val cardSize = when (design.cardDensity) {
        PrimeCardDensity.COMPACT -> 124.dp
        PrimeCardDensity.EDITORIAL -> 150.dp
        PrimeCardDensity.SPACIOUS -> 170.dp
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = bottomPaddingDp.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        // Bold Editorial Top Bar with Top-Right Settings Gear
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Home",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CroixGlassCard(
                        hazeState = hazeState,
                        cornerRadius = 50.dp,
                        onClick = onRefresh,
                    ) {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                    }
                    CroixGlassCard(
                        hazeState = hazeState,
                        cornerRadius = 50.dp,
                        accentTint = palette.primary,
                        onClick = onOpenSettings,
                    ) {
                        IconButton(onClick = onOpenSettings, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Translucent Flow Wheel Hero Card
        if (design.showCroixFlowHero) {
            item {
                CroixGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .height(168.dp),
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
                                        palette.primary.copy(alpha = 0.50f),
                                        palette.secondary.copy(alpha = 0.34f),
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
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "FLOW",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.90f),
                                    )
                                }
                                Text(
                                    text = "Your infinite mix",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                )
                                Text(
                                    text = "Tailored tracks blended non-stop with SponsorBlock.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.80f),
                                    maxLines = 2,
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            // Iconic circular Play wheel
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color.White, Color.White.copy(alpha = 0.88f)),
                                        ),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = "Play Flow",
                                    tint = Color(0xFF0A0910),
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Translucent Mood Wheels (Horizontal Circles / Pills)
        if (design.showMoodChips) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Moods",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(moods) { mood ->
                            CroixGlassCard(
                                hazeState = hazeState,
                                cornerRadius = 50.dp,
                                accentTint = Color(mood.accentHex),
                                onClick = { onPlayMood(mood) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(mood.accentHex)),
                                    )
                                    Text(
                                        text = mood.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Compact 2-Column Quick-Resume Grid (Recently Played)
        if (design.showQuickResumeGrid && recentTracks.isNotEmpty()) {
            item {
                val quickSix = recentTracks.take(6)
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Recently played",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    quickSix.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            pair.forEach { track ->
                                CroixGlassCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp),
                                    hazeState = hazeState,
                                    cornerRadius = 14.dp,
                                    onClick = { onPlayTrack(track, recentTracks) },
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        AsyncImage(
                                            model = track.thumbnailUrl,
                                            contentDescription = track.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(60.dp)
                                                .background(Color.White.copy(alpha = 0.08f)),
                                        )
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                        )
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
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

        // Editorial Square Carousels
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
