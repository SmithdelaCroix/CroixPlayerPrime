/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class EngineUnitTests {
    @Test
    fun `lrc parser extracts sorted timestamps and finds active line`() {
        val lrc = """
            [00:12.50]First verse line
            [00:05.00]Intro line
            [01:02.125]Chorus drop
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertEquals(3, parsed.size)
        assertEquals(5_000L, parsed[0].timeMs)
        assertEquals("Intro line", parsed[0].text)
        assertEquals(12_500L, parsed[1].timeMs)
        assertEquals(62_125L, parsed[2].timeMs)

        assertEquals(-1, LrcParser.activeLineIndex(parsed, 1_000L))
        assertEquals(0, LrcParser.activeLineIndex(parsed, 5_500L))
        assertEquals(1, LrcParser.activeLineIndex(parsed, 40_000L))
        assertEquals(2, LrcParser.activeLineIndex(parsed, 65_000L))
    }

    @Test
    fun `sponsorblock skipper skips matching segment once without loop`() {
        val seg = SponsorSegment(category = "music_offtopic", startMs = 0L, endMs = 18_000L, uuid = "seg-1")
        val enabled = setOf("music_offtopic", "sponsor")

        val match = SponsorBlockSkipper.findTargetSkip(
            positionMs = 3_000L,
            segments = listOf(seg),
            alreadySkippedUuids = emptySet(),
            enabledCategories = enabled,
        )
        assertEquals("seg-1", match?.uuid)

        val repeat = SponsorBlockSkipper.findTargetSkip(
            positionMs = 3_000L,
            segments = listOf(seg),
            alreadySkippedUuids = setOf("seg-1"),
            enabledCategories = enabled,
        )
        assertNull(repeat)
    }

    @Test
    fun `lyrics romanizer converts korean japanese and cyrillic cleanly`() {
        assertFalse(LyricsRomanizer.needsRomanization("Hola mundo"))
        assertTrue(LyricsRomanizer.needsRomanization("사랑해"))
        assertEquals("saranghae", LyricsRomanizer.romanize("사랑해"))
        assertEquals("arigatou", LyricsRomanizer.romanize("ありがとう"))
        assertEquals("tokyo", LyricsRomanizer.romanize("トキョ"))
        assertEquals("Privet mir", LyricsRomanizer.romanize("Привет мир"))
    }

    @Test
    fun `shazam signature generator produces valid base64 landmark uri`() {
        val gen = ShazamSignatureGenerator(maxTimeSeconds = 3.2)
        val sr = 16000
        val totalSamples = sr * 4
        val pcm = ShortArray(totalSamples) { i ->
            val t = i.toDouble() / sr
            val s = 0.35 * sin(2.0 * PI * 440.0 * t) +
                0.30 * sin(2.0 * PI * 880.0 * t) +
                0.25 * sin(2.0 * PI * 1760.0 * t) +
                0.10 * sin(2.0 * PI * (600.0 + 300.0 * sin(2.0 * PI * 2.0 * t)) * t)
            (s * 26000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        gen.feedPcm16Mono(pcm)
        val sig = gen.buildSignatureOrNull()
        assertNotNull(sig)
        assertTrue(sig!!.uri.startsWith("data:audio/vnd.shazam.sig;base64,"))
        assertTrue(sig.sampleDurationMs >= 3000L)
    }

    @Test
    fun `lastfm md5 api signature matches spec`() {
        val sig = ScrobbleClient.buildLastFmApiSignature(
            params = mapOf("method" to "track.scrobble", "api_key" to "abc", "artist" to "Daft Punk"),
            secret = "mysecret",
        )
        assertEquals(32, sig.length)
        assertTrue(sig.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `blocked artists and hidden gems radar exclude unwanted tracks`() {
        val blocked = setOf("bad bunny")
        assertTrue(HybridAlgorithmEngine.isBlockedArtist("Bad Bunny", blocked))
        assertTrue(HybridAlgorithmEngine.isBlockedArtist("J Balvin feat. Bad Bunny", blocked))
        assertFalse(HybridAlgorithmEngine.isBlockedArtist("Daft Punk", blocked))

        val engine = HybridAlgorithmEngine(InnerTubeClient())
        val candidates = listOf(
            Track("t1", "Known Song", "Daft Punk"),
            Track("t2", "Blocked Artist Song", "Bad Bunny"),
            Track("t3", "Hidden Gem", "Justice"),
        )
        val gems = engine.filterHiddenGems(
            candidates = candidates,
            knownTrackIds = setOf("t1"),
            blockedArtistKeys = blocked,
        )
        assertEquals(1, gems.size)
        assertEquals("t3", gems.first().id)
    }

    @Test
    fun `related radio filter skips identically named uploads and blocked artists`() {
        val played = Track("v1", "Blinding Lights", "The Weeknd")
        val candidates = listOf(
            Track("v2", "Blinding Lights (Official Video)", "The Weeknd"),
            Track("v3", "Blinding Lights (Lyrics)", "Pop Lyrics Channel"),
            Track("v4", "Save Your Tears", "The Weeknd"),
            Track("v5", "Save Your Tears (Official Audio)", "The Weeknd"),
            Track("v6", "Starboy", "Blocked Artist"),
            Track("v7", "After Hours", "The Weeknd"),
        )
        val filtered = HybridAlgorithmEngine.filterRelatedRadioTracks(
            candidates = candidates,
            existingQueue = listOf(played),
            blockedArtistKeys = setOf("blocked artist"),
        )
        assertEquals(listOf("v4", "v7"), filtered.map { it.id })
    }
}
