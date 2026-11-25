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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.croix.prime.design.CroixAnimatedBackdrop
import com.croix.prime.design.CroixGlassCard
import com.croix.prime.design.LocalCroixPalette
import com.croix.prime.engine.ArtistProfile
import com.croix.prime.engine.Track

@Composable
fun ArtistProfileOverlay(
    profile: ArtistProfile,
    isLoading: Boolean,
    favoriteIds: Set<String>,
    downloadedIds: Set<String>,
    downloadProgress: Map<String, Float>,
    bottomPaddingDp: Int,
    onClose: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenTrackMenu: (Track) -> Unit,
) {
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
                    Text("Artist Profile", style = MaterialTheme.typography.titleLarge, color = palette.textSecondary)
                }
            }

            item {
                CroixGlassCard(modifier = Modifier.fillMaxWidth(), accentTint = palette.primary) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = profile.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(116.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f)),
                        )
                        Text(profile.name, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        if (profile.topTracks.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                CroixGlassCard(
                                    modifier = Modifier.weight(1f),
                                    accentTint = palette.primary,
                                    onClick = { onPlayTrack(profile.topTracks.first(), profile.topTracks) },
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Play Top", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                CroixGlassCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val s = profile.topTracks.shuffled()
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
                if (profile.topTracks.isNotEmpty()) {
                    item {
                        Text("Top Tracks", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                    items(profile.topTracks, key = { it.id }) { track ->
                        GlassTrackRow(
                            track = track,
                            isFavorite = track.id in favoriteIds,
                            isDownloaded = track.id in downloadedIds,
                            downloadingProgress = downloadProgress[track.id],
                            onPlay = { onPlayTrack(track, profile.topTracks) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            onOpenMenu = { onOpenTrackMenu(track) },
                        )
                    }
                }
                if (profile.relatedTracks.isNotEmpty()) {
                    item {
                        Text("More Like This", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                    items(profile.relatedTracks, key = { "rel_${it.id}" }) { track ->
                        GlassTrackRow(
                            track = track,
                            isFavorite = track.id in favoriteIds,
                            isDownloaded = track.id in downloadedIds,
                            downloadingProgress = downloadProgress[track.id],
                            onPlay = { onPlayTrack(track, profile.relatedTracks) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            onOpenMenu = { onOpenTrackMenu(track) },
                        )
                    }
                }
            }
        }
    }
}
