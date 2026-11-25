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
 * Combines Spotify listening taste (Top Artists, Top Tracks, Recently Played, Liked Songs)
 * with YouTube Music ad-free streams & local Croix listening history to generate
 * personalized Home shelves and Flow radio mixes.
 */
class HybridAlgorithmEngine(
    private val innerTube: InnerTubeClient,
) {
    /**
     * Resolves a list of Spotify seed tracks onto playable YouTube Music ad-free Track entries.
     */
    suspend fun matchSpotifyTracksToYtMusic(
        seeds: List<SpotifySeedTrack>,
        maxTracks: Int = 25,
    ): List<Track> = withContext(Dispatchers.IO) {
        val limited = seeds.take(maxTracks)
        val matched = mutableListOf<Track>()
        // Batch in groups of 4 to avoid flooding InnerTube while keeping latency snappy
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
     * Builds blended Home shelves according to [blendWeight]:
     * - 0.0f = Pure YouTube Music server recommendations
     * - 0.5f = Balanced Hybrid (Spotify taste + YouTube Music discovery + Local history)
     * - 1.0f = Heavy Spotify Taste Priority (Spotify top tracks & artist mixes lead the Home feed)
     */
    suspend fun buildBlendedHomeSections(
        ytSections: List<HomeSection>,
        spotifyTaste: SpotifyTasteSnapshot?,
        localFavorites: List<Track>,
        localHistory: List<Track>,
        blendWeight: Float,
        isSpanish: Boolean,
    ): List<HomeSection> = withContext(Dispatchers.IO) {
        val weight = blendWeight.coerceIn(0f, 1f)
        val customSections = mutableListOf<HomeSection>()

        val hasSpotify = spotifyTaste != null && spotifyTaste.connected &&
            (spotifyTaste.allSeedTracks.isNotEmpty() || spotifyTaste.allSeedArtistNames.isNotEmpty())

        if (hasSpotify && weight > 0.05f) {
            val targetMixCount = (6 + (weight * 10)).toInt().coerceIn(6, 16)
            val spotifySeeds = spotifyTaste!!.allSeedTracks.shuffled().take((targetMixCount * 0.7f).toInt().coerceAtLeast(4))
            val matchedSpotify = matchSpotifyTracksToYtMusic(spotifySeeds, maxTracks = spotifySeeds.size)

            // Fetch YT Music related discovery from top Spotify match + local favorite
            val radioSeed = matchedSpotify.firstOrNull() ?: localFavorites.firstOrNull() ?: localHistory.firstOrNull()
            val ytDiscovery = if (radioSeed != null) {
                innerTube.fetchRadioQueue(radioSeed.id, limit = 10)
            } else {
                emptyList()
            }

            // Interleave Spotify tracks and YT Music discovery proportionally to blendWeight
            val blendedMix = interleaveByWeight(
                primary = matchedSpotify,
                secondary = (ytDiscovery + localFavorites.shuffled().take(4)).distinctBy { it.id },
                primaryWeight = weight,
                limit = 16,
            )

            if (blendedMix.isNotEmpty()) {
                val pct = (weight * 100).toInt()
                val title = if (isSpanish) {
                    "Mezcla Híbrida • Spotify $pct% × YT"
                } else {
                    "Hybrid Mix • Spotify $pct% × YT"
                }
                customSections += HomeSection(title = title, tracks = blendedMix)
            }

            // Artist-driven shelf inspired by top Spotify artists
            val pickedArtists = spotifyTaste.allSeedArtistNames.shuffled().take(2)
            if (pickedArtists.isNotEmpty()) {
                val artistTracks = coroutineScope {
                    pickedArtists.map { artist ->
                        async { innerTube.searchSongs("$artist top songs", limit = 6) }
                    }.awaitAll().flatten().distinctBy { it.id }.shuffled().take(12)
                }
                if (artistTracks.isNotEmpty()) {
                    val label = pickedArtists.joinToString(" & ")
                    val title = if (isSpanish) {
                        "Por tu algoritmo de Spotify • $label"
                    } else {
                        "From your Spotify taste • $label"
                    }
                    customSections += HomeSection(title = title, tracks = artistTracks)
                }
            }
        } else if (localFavorites.isNotEmpty() || localHistory.isNotEmpty()) {
            val seed = (localFavorites + localHistory).shuffled().firstOrNull()
            if (seed != null) {
                val radio = innerTube.fetchRadioQueue(seed.id, limit = 12)
                if (radio.isNotEmpty()) {
                    val title = if (isSpanish) "Recomendado para ti • ${seed.artist}" else "Picked for you • ${seed.artist}"
                    customSections += HomeSection(title = title, tracks = radio)
                }
            }
        }

        if (customSections.isEmpty()) return@withContext ytSections

        // Order sections based on blend weight: high Spotify weight puts hybrid shelves on top;
        // moderate weight puts the Hybrid Mix first, then interleaves YT Music shelves.
        if (weight >= 0.65f) {
            customSections + ytSections
        } else {
            buildList {
                add(customSections.first())
                ytSections.firstOrNull()?.let { add(it) }
                customSections.drop(1).forEach { add(it) }
                ytSections.drop(1).forEach { add(it) }
            }
        }
    }

    /**
     * Generates a fresh Flow radio queue blending Spotify taste, local likes, and YT Music radio.
     */
    suspend fun buildHybridFlowQueue(
        spotifyTaste: SpotifyTasteSnapshot?,
        localFavorites: List<Track>,
        localHistory: List<Track>,
        fallbackTracks: List<Track>,
        blendWeight: Float,
    ): List<Track> = withContext(Dispatchers.IO) {
        val weight = blendWeight.coerceIn(0f, 1f)
        val spotifyMatched = if (spotifyTaste != null && spotifyTaste.connected && weight > 0.1f) {
            val seeds = spotifyTaste.allSeedTracks.shuffled().take(5)
            matchSpotifyTracksToYtMusic(seeds, maxTracks = 5)
        } else {
            emptyList()
        }

        val anchor = spotifyMatched.firstOrNull()
            ?: localFavorites.shuffled().firstOrNull()
            ?: localHistory.shuffled().firstOrNull()
            ?: fallbackTracks.firstOrNull()

        val radio = if (anchor != null) {
            innerTube.fetchRadioQueue(anchor.id, limit = 18)
        } else {
            innerTube.searchSongs("top global music hits", limit = 15)
        }

        interleaveByWeight(
            primary = spotifyMatched,
            secondary = (radio + localFavorites.shuffled().take(5)).distinctBy { it.id },
            primaryWeight = weight.coerceAtLeast(0.35f),
            limit = 22,
        )
    }

    internal fun pickBestMatch(seed: SpotifySeedTrack, candidates: List<Track>): Track? {
        if (candidates.isEmpty()) return null
        val seedTitle = seed.title.lowercase()
        val seedArtist = seed.artist.split(",", "&").firstOrNull()?.trim()?.lowercase().orEmpty()
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
