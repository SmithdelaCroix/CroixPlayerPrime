/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
}
