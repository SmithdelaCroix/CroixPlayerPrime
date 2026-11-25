/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.croix.prime.design.CroixAnimatedBackdrop
import com.croix.prime.design.CroixGlassCard
import com.croix.prime.design.LocalCroixDesign
import com.croix.prime.design.LocalCroixPalette
import com.croix.prime.engine.AlbumDetail
import com.croix.prime.engine.AlbumSummary
import com.croix.prime.engine.ArtistProfile
import com.croix.prime.engine.RelatedArtist
import com.croix.prime.engine.Track

@Composable
fun ArtistProfileOverlay(
    profile: ArtistProfile,
    isLoading: Boolean,
    favoriteIds: Set<String>,
    bottomPaddingDp: Int,
    onClose: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
    onOpenAlbum: (AlbumSummary) -> Unit,
    onOpenRelatedArtist: (RelatedArtist) -> Unit,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val blocker = remember { MutableInteractionSource() }
    var showAllPopular by remember { mutableStateOf(false) }
    var bioExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = blocker, indication = null) {},
    ) {
        CroixAnimatedBackdrop()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = bottomPaddingDp.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Full-bleed Spotify-style Hero Banner with Artist Name & Monthly Listeners
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp),
                ) {
                    AsyncImage(
                        model = profile.bannerUrl,
                        contentDescription = profile.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.06f)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Transparent,
                                        palette.background.copy(alpha = 0.85f),
                                        palette.background,
                                    ),
                                ),
                            ),
                    )
                    // Back button top-left
                    CroixGlassCard(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(16.dp)
                            .align(Alignment.TopStart),
                        cornerRadius = 50.dp,
                        onClick = onClose,
                    ) {
                        IconButton(onClick = onClose, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }

                    // Artist Title + Monthly Audience anchored at bottom of banner
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = profile.name,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1.0).sp,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (profile.monthlyListeners.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = profile.monthlyListeners,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary,
                            )
                        }
                    }
                }
            }

            // Action Row: Shuffle + Big Play Circle
            if (profile.topTracks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        CroixGlassCard(
                            cornerRadius = 50.dp,
                            onClick = {
                                val shuffled = profile.topTracks.shuffled()
                                onPlayTrack(shuffled.first(), shuffled)
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Rounded.Shuffle, null, tint = palette.primary, modifier = Modifier.size(20.dp))
                                Text("Shuffle", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(
                                    if (design.useGradientStyle) Brush.linearGradient(listOf(palette.primary, palette.secondary))
                                    else Brush.linearGradient(listOf(palette.primary, palette.primary)),
                                )
                                .clickable { onPlayTrack(profile.topTracks.first(), profile.topTracks) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = palette.primary)
                    }
                }
            } else {
                // Popular Numbered Tracks (Spotify-style 1..5 / 1..10)
                if (profile.topTracks.isNotEmpty()) {
                    val visibleCount = if (showAllPopular) profile.topTracks.size else minOf(5, profile.topTracks.size)
                    val shownTracks = profile.topTracks.take(visibleCount)

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Popular", style = MaterialTheme.typography.titleLarge, color = Color.White)
                            if (profile.topTracks.size > 5) {
                                TextButton(onClick = { showAllPopular = !showAllPopular }) {
                                    Text(if (showAllPopular) "Show less" else "See more")
                                }
                            }
                        }
                    }

                    itemsIndexed(shownTracks, key = { idx, t -> "pop_${idx}_${t.id}" }) { index, track ->
                        NumberedTrackRow(
                            rank = index + 1,
                            track = track,
                            isFavorite = track.id in favoriteIds,
                            onPlay = { onPlayTrack(track, profile.topTracks) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            onOpenMenu = { onOpenTrackMenu(track) },
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                    }
                }

                // Albums Carousel
                if (profile.albums.isNotEmpty()) {
                    item {
                        DiscographyCarousel(
                            title = "Albums",
                            items = profile.albums,
                            onOpenAlbum = onOpenAlbum,
                        )
                    }
                }

                // Singles & EPs Carousel
                if (profile.singles.isNotEmpty()) {
                    item {
                        DiscographyCarousel(
                            title = "Singles & EPs",
                            items = profile.singles,
                            onOpenAlbum = onOpenAlbum,
                        )
                    }
                }

                // Fans Also Like (Circular Artist Avatars)
                if (profile.relatedArtists.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Fans also like",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 18.dp),
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                items(profile.relatedArtists, key = { it.browseId }) { rel ->
                                    CroixGlassCard(
                                        modifier = Modifier.width(126.dp),
                                        onClick = { onOpenRelatedArtist(rel) },
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            AsyncImage(
                                                model = rel.thumbnailUrl,
                                                contentDescription = rel.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(86.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.08f)),
                                            )
                                            Text(
                                                text = rel.name,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // About Bio Card (Spotify-style artist info)
                if (profile.description.isNotBlank() || profile.monthlyListeners.isNotBlank()) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("About", style = MaterialTheme.typography.titleLarge, color = Color.White)
                            CroixGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { bioExpanded = !bioExpanded },
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    if (profile.monthlyListeners.isNotBlank()) {
                                        Text(
                                            text = profile.monthlyListeners,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = palette.primary,
                                        )
                                    }
                                    if (profile.description.isNotBlank()) {
                                        Text(
                                            text = profile.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = if (bioExpanded) Int.MAX_VALUE else 4,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = if (bioExpanded) "Show less" else "Read more",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.primary,
                                        )
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
private fun NumberedTrackRow(
    rank: Int,
    track: Track,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalCroixPalette.current
    CroixGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        onClick = onPlay,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textSecondary,
                modifier = Modifier.width(26.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(8.dp))
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
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
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(34.dp)) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) palette.primary else palette.textSecondary,
                    modifier = Modifier.size(19.dp),
                )
            }
            IconButton(onClick = onOpenMenu, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Options", tint = palette.textSecondary, modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun DiscographyCarousel(
    title: String,
    items: List<AlbumSummary>,
    onOpenAlbum: (AlbumSummary) -> Unit,
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
            items(items, key = { it.browseId }) { album ->
                CroixGlassCard(
                    modifier = Modifier.width(152.dp),
                    onClick = { onOpenAlbum(album) },
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        AsyncImage(
                            model = album.thumbnailUrl,
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(132.dp)
                                .clip(RoundedCornerShape((design.cornerRadiusDp - 6f).coerceAtLeast(8f).dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = buildString {
                                if (album.year.isNotBlank()) append(album.year)
                                if (album.year.isNotBlank()) append(" • ")
                                append(if (album.isSingle) "Single" else "Album")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumDetailOverlay(
    album: AlbumDetail,
    isLoading: Boolean,
    favoriteIds: Set<String>,
    bottomPaddingDp: Int,
    onClose: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val blocker = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = blocker, indication = null) {},
    ) {
        CroixAnimatedBackdrop()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = bottomPaddingDp.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CroixGlassCard(cornerRadius = 50.dp, onClick = onClose) {
                        IconButton(onClick = onClose, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                    Text("Release", style = MaterialTheme.typography.titleLarge, color = palette.textSecondary)
                }
            }

            item {
                CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AsyncImage(
                            model = album.thumbnailUrl,
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(190.dp)
                                .clip(RoundedCornerShape(design.artworkCornerDp.coerceAtMost(24f).dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                        )
                        Text(album.title, style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
                        Text(
                            text = listOf(album.artist, album.year).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                        )
                        if (album.tracks.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                CroixGlassCard(
                                    modifier = Modifier.weight(1f),
                                    accentTint = palette.primary,
                                    onClick = { onPlayTrack(album.tracks.first(), album.tracks) },
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Play", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                CroixGlassCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val s = album.tracks.shuffled()
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
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = palette.primary)
                    }
                }
            } else {
                itemsIndexed(album.tracks, key = { idx, t -> "alb_${idx}_${t.id}" }) { idx, track ->
                    NumberedTrackRow(
                        rank = idx + 1,
                        track = track,
                        isFavorite = track.id in favoriteIds,
                        onPlay = { onPlayTrack(track, album.tracks) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onOpenMenu = { onOpenTrackMenu(track) },
                    )
                }
            }
        }
    }
}
