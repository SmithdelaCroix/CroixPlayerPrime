/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.croix.prime.design.CroixGlassCard
import com.croix.prime.design.LocalCroixDesign
import com.croix.prime.design.LocalCroixPalette
import com.croix.prime.design.LocalCroixStrings
import com.croix.prime.design.PrimeCardDensity
import com.croix.prime.engine.DailyMix
import com.croix.prime.engine.HomeFeedBundle
import com.croix.prime.engine.HomeSection
import com.croix.prime.engine.MoodChip
import com.croix.prime.engine.RelatedArtist
import com.croix.prime.engine.Track
import java.util.Calendar

@Composable
fun HomeScreen(
    feed: HomeFeedBundle,
    recentTracks: List<Track>,
    moods: List<MoodChip>,
    favoriteIds: Set<String>,
    spotifyConnected: Boolean,
    isLoading: Boolean,
    bottomPaddingDp: Int,
    onPlayFlow: (MoodChip?) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
    onOpenArtist: (String, String?) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val strings = LocalCroixStrings.current
    var selectedFlowMood by remember { mutableStateOf<MoodChip?>(null) }

    val cardSize = when (design.cardDensity) {
        PrimeCardDensity.COMPACT -> 128.dp
        PrimeCardDensity.EDITORIAL -> 154.dp
        PrimeCardDensity.SPACIOUS -> 174.dp
    }

    val greeting = remember(strings.isSpanish) {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (strings.isSpanish) {
            when (h) {
                in 5..11 -> "Buenos días"
                in 12..18 -> "Buenas tardes"
                else -> "Buenas noches"
            }
        } else {
            when (h) {
                in 5..11 -> "Good morning"
                in 12..18 -> "Good afternoon"
                else -> "Good evening"
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 14.dp, bottom = bottomPaddingDp.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // 1. Editorial Greeting Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                    )
                    Text(
                        text = if (spotifyConnected) {
                            if (strings.isSpanish) "Algoritmo Híbrido Spotify × YouTube activo" else "Spotify × YouTube Hybrid Algorithm active"
                        } else {
                            if (strings.isSpanish) "Tu música sin anuncios, a tu ritmo" else "Ad-free music tailored to you"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (spotifyConnected) Color(0xFF1DB954) else palette.textSecondary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CroixGlassCard(cornerRadius = 50.dp, onClick = onRefresh) {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Actualizar", tint = Color.White)
                        }
                    }
                    CroixGlassCard(
                        cornerRadius = 50.dp,
                        accentTint = palette.primary,
                        onClick = onOpenSettings,
                    ) {
                        IconButton(onClick = onOpenSettings, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Rounded.Settings, contentDescription = strings.settingsTitle, tint = Color.White)
                        }
                    }
                }
            }
        }

        // 2. Signature Editorial FLOW Hero Wheel + Integrated Mood Pills
        if (design.showCroixFlowHero) {
            item {
                val activeFlowAccent by animateColorAsState(
                    targetValue = selectedFlowMood?.let { Color(it.accentHex) } ?: palette.primary,
                    animationSpec = tween(420),
                    label = "flowAccent",
                )
                val pulse = rememberInfiniteTransition(label = "flowPulse")
                val wheelScale by pulse.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
                    label = "wheelScale",
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CroixGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .height(176.dp),
                        accentTint = activeFlowAccent,
                        onClick = { onPlayFlow(selectedFlowMood) },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            activeFlowAccent.copy(alpha = 0.58f),
                                            palette.secondary.copy(alpha = 0.36f),
                                            Color.Black.copy(alpha = 0.18f),
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
                                            text = selectedFlowMood?.let { "FLOW • ${it.title.uppercase()}" } ?: "FLOW INFINITO",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.92f),
                                        )
                                    }
                                    Text(
                                        text = selectedFlowMood?.let {
                                            if (strings.isSpanish) "Modo ${it.title}" else "${it.title} Flow"
                                        } ?: strings.flowTitle,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White,
                                    )
                                    Text(
                                        text = strings.flowSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.84f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                // Glowing layered circular Flow Play Wheel
                                Box(contentAlignment = Alignment.Center) {
                                    Box(
                                        modifier = Modifier
                                            .size(88.dp)
                                            .scale(wheelScale)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.18f)),
                                    )
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
                                            contentDescription = "Reproducir Flow",
                                            tint = Color(0xFF0A0910),
                                            modifier = Modifier.size(40.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Flow Mood Selector Pills right under the Flow Wheel
                    if (design.showMoodChips) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CroixGlassCard(
                                cornerRadius = 50.dp,
                                accentTint = if (selectedFlowMood == null) palette.primary else null,
                                onClick = {
                                    selectedFlowMood = null
                                    onPlayFlow(null)
                                },
                            ) {
                                Text(
                                    text = if (strings.isSpanish) "Tu Flow" else "Your Flow",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                )
                            }
                            moods.forEach { mood ->
                                val isSel = selectedFlowMood?.title == mood.title
                                CroixGlassCard(
                                    cornerRadius = 50.dp,
                                    accentTint = if (isSel) Color(mood.accentHex) else null,
                                    onClick = {
                                        selectedFlowMood = mood
                                        onPlayFlow(mood)
                                    },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
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
        }

        // 3. Compact 2-Column Quick-Resume Grid ("Escuchado recientemente")
        if (design.showQuickResumeGrid && recentTracks.isNotEmpty()) {
            item {
                val quickSix = recentTracks.take(6)
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = strings.recentlyPlayed,
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
                                    cornerRadius = 15.dp,
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
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 10.dp),
                                        ) {
                                            Text(
                                                text = track.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = track.artist,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 4. Editorial "Hecho para ti — Mixes Diarios" Poster Cards
        if (design.showDailyMixes && feed.dailyMixes.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                        Text(
                            text = if (strings.isSpanish) "Hecho para ti" else "Made for you",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                        )
                        Text(
                            text = if (strings.isSpanish) "Mixes diarios actualizados según tus gustos" else "Daily mixes tailored to your taste",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(feed.dailyMixes, key = { it.id }) { mix ->
                            DailyMixPosterCard(
                                mix = mix,
                                onPlay = {
                                    mix.tracks.firstOrNull()?.let { first -> onPlayTrack(first, mix.tracks) }
                                },
                            )
                        }
                    }
                }
            }
        }

        // 5. Swipeable 4-Row "Selección Rápida" (Quick Picks dense pager)
        if (design.showQuickPicks && feed.quickPicks.isNotEmpty()) {
            item {
                val pages = remember(feed.quickPicks) { feed.quickPicks.chunked(4) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = if (strings.isSpanish) "Selección rápida" else "Quick picks",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                            )
                            Text(
                                text = if (strings.isSpanish) "Inicia una radio instantánea desde cualquier tema" else "Start an instant mix from any track",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                            )
                        }
                        CroixGlassCard(
                            cornerRadius = 50.dp,
                            accentTint = palette.primary,
                            onClick = { feed.quickPicks.firstOrNull()?.let { onPlayTrack(it, feed.quickPicks) } },
                        ) {
                            Text(
                                text = strings.playAll,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(pages.size) { pageIdx ->
                            val columnTracks = pages[pageIdx]
                            CroixGlassCard(
                                modifier = Modifier.width(304.dp),
                                cornerRadius = design.cornerRadiusDp.dp,
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    columnTracks.forEach { track ->
                                        QuickPickTrackRow(
                                            track = track,
                                            isFavorite = track.id in favoriteIds,
                                            onPlay = { onPlayTrack(track, feed.quickPicks) },
                                            onToggleFavorite = { onToggleFavorite(track) },
                                            onOpenMenu = { onOpenTrackMenu(track) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Circular "Tus Artistas Favoritos" Row
        if (design.showFavoriteArtistsRow && feed.favoriteArtists.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (strings.isSpanish) "Artistas para ti" else "Artists for you",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(feed.favoriteArtists, key = { it.name }) { artist ->
                            FavoriteArtistBubble(
                                artist = artist,
                                ringBrush = Brush.linearGradient(listOf(palette.primary, palette.secondary)),
                                onClick = { onOpenArtist(artist.name, artist.browseId.ifBlank { null }) },
                            )
                        }
                    }
                }
            }
        }

        // 7. Loading indicator or Personalized Editorial Shelves
        if (isLoading && feed.shelves.isEmpty() && feed.quickPicks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(color = palette.primary)
                        Text(
                            text = if (strings.isSpanish) "Preparando tu mezcla personalizada..." else "Crafting your personalized feed...",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                        )
                    }
                }
            }
        } else {
            items(feed.shelves, key = { it.title }) { section ->
                EditorialHomeShelf(
                    section = section,
                    cardSize = cardSize,
                    onPlayTrack = onPlayTrack,
                )
            }
        }
    }
}

@Composable
private fun DailyMixPosterCard(
    mix: DailyMix,
    onPlay: () -> Unit,
) {
    val c1 = Color(mix.accentHex)
    val c2 = Color(mix.secondaryHex)
    val heroArt = mix.tracks.firstOrNull()?.thumbnailUrl.orEmpty()

    CroixGlassCard(
        modifier = Modifier
            .width(184.dp)
            .height(226.dp),
        accentTint = c1,
        onClick = onPlay,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = heroArt,
                contentDescription = mix.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                c1.copy(alpha = 0.42f),
                                Color.Black.copy(alpha = 0.48f),
                                c2.copy(alpha = 0.88f),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "${mix.tracks.size} temas",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = "Reproducir Mix",
                            tint = Color(0xFF0A0910),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = mix.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = mix.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPickTrackRow(
    track: Track,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    val palette = LocalCroixPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onPlay)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
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
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
            Icon(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorito",
                tint = if (isFavorite) palette.primary else palette.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onOpenMenu, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = "Más",
                tint = palette.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun FavoriteArtistBubble(
    artist: RelatedArtist,
    ringBrush: Brush,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(86.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .border(width = 2.dp, brush = ringBrush, shape = CircleShape)
                .padding(3.dp),
        ) {
            AsyncImage(
                model = artist.thumbnailUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
            )
        }
        Text(
            text = artist.name,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EditorialHomeShelf(
    section: HomeSection,
    cardSize: Dp,
    onPlayTrack: (Track, List<Track>) -> Unit,
) {
    val palette = LocalCroixPalette.current
    val design = LocalCroixDesign.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(section.tracks, key = { "${section.title}_${it.id}" }) { track ->
                CroixGlassCard(
                    modifier = Modifier.width(cardSize),
                    onClick = { onPlayTrack(track, section.tracks) },
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(cardSize)
                                .clip(RoundedCornerShape(topStart = design.cornerRadiusDp.dp, topEnd = design.cornerRadiusDp.dp)),
                        ) {
                            AsyncImage(
                                model = track.thumbnailUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = 0.08f)),
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(palette.primary.copy(alpha = 0.90f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = "Reproducir",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium,
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
}
