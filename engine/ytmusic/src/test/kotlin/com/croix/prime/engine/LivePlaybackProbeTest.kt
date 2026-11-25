/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Opt-in live playback verification against real YouTube Music streams.
 * Skipped in CI unless `-DrunLiveStreamProbe=true` is passed.
 */
class LivePlaybackProbeTest {
    @Test
    fun `live youtube music search and stream resolution yields verified playable audio`() = runBlocking {
        assumeTrue("Live probe disabled", System.getProperty("runLiveStreamProbe") == "true")
        val client = InnerTubeClient()
        val songs = client.searchSongs("Daft Punk Instant Crush", limit = 5)
        println("LIVE_PROBE search results: ${songs.size} -> ${songs.firstOrNull()}")
        assertTrue("Expected non-empty YouTube Music search results", songs.isNotEmpty())

        val first = songs.first()
        val res = client.resolveAudioStream(first.id)
        println("LIVE_PROBE resolve result for ${first.id} (${first.title}): ${res.map { "${it.clientKey} | ${it.mimeType} | ${it.bitrate}bps | ${it.url.take(80)}..." }}")
        if (res.isFailure) {
            println("LIVE_PROBE FAILURE: ${res.exceptionOrNull()?.message}")
        }
        assertTrue("Expected verified playable stream, got: ${res.exceptionOrNull()?.message}", res.isSuccess)
    }
}
