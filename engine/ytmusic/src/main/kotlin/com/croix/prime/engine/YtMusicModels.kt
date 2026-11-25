/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationSeconds: Int = 0,
    val thumbnailUrl: String = "",
)

@Serializable
data class HomeSection(
    val title: String,
    val tracks: List<Track>,
)

@Serializable
data class ArtistProfile(
    val name: String,
    val avatarUrl: String,
    val topTracks: List<Track>,
    val relatedTracks: List<Track> = emptyList(),
)

@Serializable
data class MoodChip(
    val title: String,
    val query: String,
    val accentHex: Long,
)

enum class SponsorCategory(val apiKey: String, val displayName: String) {
    MUSIC_OFFTOPIC("music_offtopic", "Non-Music Sections"),
    SPONSOR("sponsor", "Sponsors"),
    INTRO("intro", "Intros & Intermissions"),
    OUTRO("outro", "Outros & Credits"),
    SELF_PROMO("selfpromo", "Self-Promotion"),
    INTERACTION("interaction", "Subscribe Reminders"),
}

@Serializable
data class SponsorSegment(
    val category: String,
    val startMs: Long,
    val endMs: Long,
    val uuid: String = "",
)

@Serializable
data class SyncedLyricLine(
    val timeMs: Long,
    val text: String,
)

@Serializable
data class LyricsResult(
    val synced: List<SyncedLyricLine>,
    val plain: String = "",
    val source: String = "LRCLib",
)

data class ResolvedStream(
    val url: String,
    val mimeType: String,
    val bitrate: Int,
    val expiresAtEpochMs: Long = System.currentTimeMillis() + 4 * 60 * 60 * 1000L,
)
