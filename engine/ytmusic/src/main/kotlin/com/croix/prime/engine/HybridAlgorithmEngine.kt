/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Multi-signal Recommendation & Hybrid Algorithm Engine:
 * Combines weighted local listening history (play frequency + recency decay),
 * local favorites, Spotify listening profile (Top Artists, Top Tracks, Recently Played, Likes),
 * and YouTube Music radio/related discovery to craft:
 * - Selección Rápida (Quick Picks)
 * - Mixes Diarios personalizados (Daily Mix 1, 2, 3 + Mezcla Spotify × YT)
 * - Tus Artistas Favoritos (circulares navegables)
 * - Estantes personalizados ("Porque escuchaste a...", "Para redescubrir")
 * - Flow Infinito inteligente sin repeticiones.
 */
class HybridAlgorithmEngine(
    private val innerTube: InnerTubeClient,
) {
    data class WeightedPlayRecord(
        val track: Track,
        val playCount: Int,
        val playedAtMs: Long,
    )

    suspend fun matchSpotifyTracksToYtMusic(
        seeds: List<SpotifySeedTrack>,
        maxTracks: Int = 25,
    ): List<Track> = withContext(Dispatchers.IO) {
        val limited = seeds.take(maxTracks)
        val matched = mutableListOf<Track>()
        limited.chunked(4).forEach { batch ->
            val results = coroutineScope {
                batch.map { seed ->
                    async {
                        val query = "${seed.title} ${seed.artist}".trim()
                        val candidates = innerTube.searchSongs(query, limit = 3)
                        pickBestMatch(seed, candidates)
                    }
                }.awaitAll()
            }
            matched.addAll(results.filterNotNull())
        }
        matched.distinctBy { it.id }
    }

    /**
     * Builds the full Deezer-style editorial HomeFeedBundle with Quick Picks, Daily Mixes,
     * Favorite Artists, and personalized shelves.
     */
    suspend fun buildRichHomeFeed(
        ytSections: List<HomeSection>,
        spotifyTaste: SpotifyTasteSnapshot?,
        localFavorites: List<Track>,
        localHistoryRecords: List<WeightedPlayRecord>,
        blendWeight: Float,
        isSpanish: Boolean,
    ): HomeFeedBundle = withContext(Dispatchers.IO) {
        val weight = blendWeight.coerceIn(0f, 1f)
        val now = System.currentTimeMillis()

        // 1. Score local tracks & artists with recency decay (plays in last 7 days count extra)
        val scoredTracks = localHistoryRecords.map { rec ->
            val ageDays = ((now - rec.playedAtMs).coerceAtLeast(0L) / 86_400_000.0)
            val recencyFactor = 1.0 / (1.0 + ageDays / 7.0)
            val isFavBonus = if (localFavorites.any { it.id == rec.track.id }) 2.5 else 0.0
            rec.track to (rec.playCount * (0.6 + recencyFactor) + isFavBonus)
        }.sortedByDescending { it.second }

        val topLocalTracks = (scoredTracks.map { it.first } + localFavorites).distinctBy { it.id }
        val forgottenGems = localHistoryRecords
            .filter { (now - it.playedAtMs) > 5L * 86_400_000L && it.playCount >= 2 }
            .sortedByDescending { it.playCount }
            .map { it.track }
            .take(6)

        val localArtistRankings = buildMap<String, Double> {
            scoredTracks.forEach { (t, score) ->
                val mainArtist = primaryArtistName(t.artist)
                if (mainArtist.isNotBlank() && !mainArtist.equals("Unknown Artist", true)) {
                    put(mainArtist, (get(mainArtist) ?: 0.0) + score)
                }
            }
            localFavorites.forEach { t ->
                val mainArtist = primaryArtistName(t.artist)
                if (mainArtist.isNotBlank() && !mainArtist.equals("Unknown Artist", true)) {
                    put(mainArtist, (get(mainArtist) ?: 0.0) + 2.0)
                }
            }
        }.entries.sortedByDescending { it.value }.map { it.key }

        val hasSpotify = spotifyTaste != null && spotifyTaste.connected &&
            (spotifyTaste.allSeedTracks.isNotEmpty() || spotifyTaste.allSeedArtistNames.isNotEmpty())

        // 2. Gather top seed artists combining Spotify + Local + YT Home
        val ytFallbackArtists = ytSections.flatMap { it.tracks }
            .map { primaryArtistName(it.artist) }
            .filter { it.isNotBlank() && !it.equals("Unknown Artist", true) && !it.equals("YouTube Music", true) }
            .distinct()

        val combinedArtistNames = buildList {
            if (hasSpotify && weight > 0.1f) {
                addAll(spotifyTaste!!.allSeedArtistNames.take(6))
            }
            addAll(localArtistRankings.take(6))
            addAll(ytFallbackArtists.take(6))
        }.distinctBy { it.lowercase() }.take(8)

        // 3. Run parallel discovery calls for Quick Picks, Daily Mixes & Spotify matching
        val matchedSpotifyTracks = if (hasSpotify && weight > 0.05f) {
            val seeds = spotifyTaste!!.allSeedTracks.shuffled().take(10)
            matchSpotifyTracksToYtMusic(seeds, maxTracks = 10)
        } else {
            emptyList()
        }

        val quickPickSeedA = matchedSpotifyTracks.firstOrNull() ?: topLocalTracks.firstOrNull() ?: ytSections.firstOrNull()?.tracks?.firstOrNull()
        val quickPickSeedB = topLocalTracks.drop(1).firstOrNull() ?: ytSections.getOrNull(1)?.tracks?.firstOrNull()

        val (radioA, radioB, mixTracksByArtist) = coroutineScope {
            val rA = async { quickPickSeedA?.let { innerTube.fetchRadioQueue(it.id, limit = 14) }.orEmpty() }
            val rB = async { quickPickSeedB?.let { innerTube.fetchRadioQueue(it.id, limit = 10) }.orEmpty() }
            val mixArtists = combinedArtistNames.take(3)
            val mDefs = mixArtists.map { artist ->
                async {
                    val found = innerTube.searchSongs("$artist top hits", limit = 10)
                    val extra = found.firstOrNull()?.let { innerTube.fetchRadioQueue(it.id, limit = 8) }.orEmpty()
                    artist to (found + extra).distinctBy { it.id }.take(15)
                }
            }
            Triple(rA.await(), rB.await(), mDefs.awaitAll())
        }

        // 4. Build Quick Picks (16 dense high-affinity tracks for 4-row horizontal pager)
        val quickPicks = interleaveByWeight(
            primary = (matchedSpotifyTracks + forgottenGems + topLocalTracks.take(4)).distinctBy { it.id },
            secondary = (radioA + radioB + ytSections.firstOrNull()?.tracks.orEmpty()).distinctBy { it.id },
            primaryWeight = if (hasSpotify) weight.coerceIn(0.3f, 0.85f) else 0.4f,
            limit = 16,
        )

        // 5. Build Editorial Daily Mixes (Deezer "Made For You" poster cards!)
        val mixGradients = listOf(
            0xFFA238FFL to 0xFFFF2E93L,
            0xFF00E5FFL to 0xFF3D5AFEL,
            0xFFFF6D00L to 0xFFFF1744L,
            0xFF1DB954L to 0xFF00BFA5L,
        )
        val dailyMixes = mutableListOf<DailyMix>()

        if (hasSpotify && matchedSpotifyTracks.isNotEmpty()) {
            val pct = (weight * 100).toInt()
            val hybridTracks = interleaveByWeight(
                primary = matchedSpotifyTracks,
                secondary = (radioA + topLocalTracks.take(5)).distinctBy { it.id },
                primaryWeight = weight,
                limit = 16,
            )
            val topNames = spotifyTaste!!.allSeedArtistNames.take(3).joinToString(", ")
            dailyMixes += DailyMix(
                id = "mix_spotify_hybrid",
                title = if (isSpanish) "Mix Híbrido Spotify $pct%" else "Spotify $pct% Hybrid Mix",
                subtitle = topNames.ifBlank { if (isSpanish) "Tu algoritmo combinado" else "Your blended taste" },
                accentHex = 0xFF1DB954L,
                secondaryHex = 0xFFA238FFL,
                tracks = hybridTracks,
            )
        }

        mixTracksByArtist.forEachIndexed { idx, (artist, tracks) ->
            if (tracks.size >= 4) {
                val (c1, c2) = mixGradients[idx % mixGradients.size]
                val relatedNames = tracks.map { primaryArtistName(it.artist) }
                    .distinctBy { it.lowercase() }
                    .take(3)
                    .joinToString(", ")
                dailyMixes += DailyMix(
                    id = "daily_mix_${idx + 1}",
                    title = if (isSpanish) "Mix Diario ${idx + 1}" else "Daily Mix ${idx + 1}",
                    subtitle = relatedNames.ifBlank { artist },
                    accentHex = c1,
                    secondaryHex = c2,
                    tracks = tracks,
                )
            }
        }

        if (forgottenGems.size >= 3) {
            dailyMixes += DailyMix(
                id = "mix_rediscover",
                title = if (isSpanish) "Para Redescubrir" else "Rediscover Mix",
                subtitle = if (isSpanish) "Favoritas que hace días no escuchas" else "Favorites you haven't played lately",
                accentHex = 0xFFFF4081L,
                secondaryHex = 0xFF7C4DFFL,
                tracks = (forgottenGems + radioB).distinctBy { it.id }.take(15),
            )
        }

        // 6. Build Favorite Artists circular row (using Spotify avatars or track thumbnails)
        val thumbByArtist = mutableMapOf<String, String>()
        spotifyTaste?.topArtists?.forEach { a ->
            if (a.thumbnailUrl.isNotBlank()) thumbByArtist[a.name.lowercase()] = a.thumbnailUrl
        }
        (topLocalTracks + ytSections.flatMap { it.tracks }).forEach { t ->
            val key = primaryArtistName(t.artist).lowercase()
            if (key.isNotBlank() && !thumbByArtist.containsKey(key) && t.thumbnailUrl.isNotBlank()) {
                thumbByArtist[key] = t.thumbnailUrl
            }
        }
        val favoriteArtists = combinedArtistNames.map { name ->
            RelatedArtist(
                browseId = "",
                name = name,
                subscribers = "",
                thumbnailUrl = thumbByArtist[name.lowercase()].orEmpty(),
            )
        }

        // 7. Build Personalized Shelves + Clean YT Music Shelves
        val personalizedShelves = mutableListOf<HomeSection>()
        mixTracksByArtist.firstOrNull()?.let { (artist, tracks) ->
            if (tracks.isNotEmpty()) {
                personalizedShelves += HomeSection(
                    title = if (isSpanish) "Porque escuchaste a $artist" else "Because you listen to $artist",
                    tracks = tracks,
                )
            }
        }
        if (hasSpotify && matchedSpotifyTracks.isNotEmpty()) {
            personalizedShelves += HomeSection(
                title = if (isSpanish) "De tu perfil de Spotify" else "From your Spotify profile",
                tracks = matchedSpotifyTracks,
            )
        }

        val finalShelves = (personalizedShelves + ytSections).distinctBy { it.title }
        HomeFeedBundle(
            quickPicks = quickPicks,
            dailyMixes = dailyMixes,
            favoriteArtists = favoriteArtists,
            shelves = finalShelves,
        )
    }

    suspend fun buildHybridFlowQueue(
        spotifyTaste: SpotifyTasteSnapshot?,
        localFavorites: List<Track>,
        localHistory: List<Track>,
        fallbackTracks: List<Track>,
        blendWeight: Float,
        moodQuery: String? = null,
        excludeIds: Set<String> = emptySet(),
    ): List<Track> = withContext(Dispatchers.IO) {
        if (!moodQuery.isNullOrBlank()) {
            val moodTracks = innerTube.searchSongs(moodQuery, limit = 18)
                .filter { it.id !in excludeIds }
            val extra = moodTracks.firstOrNull()?.let { innerTube.fetchRadioQueue(it.id, limit = 12) }.orEmpty()
                .filter { it.id !in excludeIds }
            return@withContext (moodTracks + extra).distinctBy { it.id }.take(25)
        }

        val weight = blendWeight.coerceIn(0f, 1f)
        val spotifyMatched = if (spotifyTaste != null && spotifyTaste.connected && weight > 0.1f) {
            val seeds = spotifyTaste.allSeedTracks.shuffled().take(6)
            matchSpotifyTracksToYtMusic(seeds, maxTracks = 6)
        } else {
            emptyList()
        }

        val anchor = spotifyMatched.firstOrNull()
            ?: localFavorites.shuffled().firstOrNull()
            ?: localHistory.shuffled().firstOrNull()
            ?: fallbackTracks.firstOrNull()

        val radio = if (anchor != null) {
            innerTube.fetchRadioQueue(anchor.id, limit = 20)
        } else {
            innerTube.searchSongs("top global music hits", limit = 18)
        }

        interleaveByWeight(
            primary = spotifyMatched.filter { it.id !in excludeIds },
            secondary = (radio + localFavorites.shuffled().take(6)).filter { it.id !in excludeIds }.distinctBy { it.id },
            primaryWeight = weight.coerceAtLeast(0.35f),
            limit = 25,
        )
    }

    private fun primaryArtistName(raw: String): String =
        raw.split(",", "&", "feat.", "ft.", ignoreCase = true).firstOrNull()?.trim().orEmpty()

    internal fun pickBestMatch(seed: SpotifySeedTrack, candidates: List<Track>): Track? {
        if (candidates.isEmpty()) return null
        val seedTitle = seed.title.lowercase()
        val seedArtist = primaryArtistName(seed.artist).lowercase()
        return candidates.maxByOrNull { c ->
            var score = 0
            val cTitle = c.title.lowercase()
            val cArtist = c.artist.lowercase()
            if (cTitle == seedTitle) score += 50
            else if (cTitle.contains(seedTitle) || seedTitle.contains(cTitle)) score += 30
            if (seedArtist.isNotBlank() && (cArtist.contains(seedArtist) || seedArtist.contains(cArtist))) score += 35
            if ("remix" in cTitle && "remix" !in seedTitle) score -= 12
            if ("live" in cTitle && "live" !in seedTitle) score -= 10
            if ("cover" in cTitle && "cover" !in seedTitle) score -= 15
            score
        }?.let { best ->
            if (best.thumbnailUrl.isBlank() && seed.thumbnailUrl.isNotBlank()) {
                best.copy(thumbnailUrl = seed.thumbnailUrl)
            } else {
                best
            }
        }
    }

    internal fun interleaveByWeight(
        primary: List<Track>,
        secondary: List<Track>,
        primaryWeight: Float,
        limit: Int,
    ): List<Track> {
        if (primary.isEmpty()) return secondary.distinctBy { it.id }.take(limit)
        if (secondary.isEmpty()) return primary.distinctBy { it.id }.take(limit)

        val out = mutableListOf<Track>()
        val seen = HashSet<String>()
        var pi = 0
        var si = 0
        var acc = 0f

        while (out.size < limit && (pi < primary.size || si < secondary.size)) {
            acc += primaryWeight
            val takePrimary = (acc >= 1f && pi < primary.size) || si >= secondary.size
            val next = if (takePrimary && pi < primary.size) {
                acc -= 1f
                primary[pi++]
            } else if (si < secondary.size) {
                secondary[si++]
            } else {
                null
            }
            if (next != null && seen.add(next.id)) {
                out.add(next)
            }
        }
        return out
    }
}
