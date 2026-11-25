/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CroixDesignStateTest {
    @Test
    fun `json export and import preserves full design state`() {
        val custom = PrimePresets.YumaFrostedNeon.copy(
            presetName = "My Prime Mix",
            glassOpacity = 0.35f,
            playerStyle = PrimePlayerStyle.DEEZER_TRANSLUCENT,
            progressStyle = PrimeProgressStyle.WAVEFORM,
        )
        val encoded = CroixThemeJson.export(custom)
        val decoded = CroixThemeJson.import(encoded).getOrThrow()
        assertEquals(custom, decoded)
    }

    @Test
    fun `clamped protects extreme slider inputs`() {
        val extreme = CroixDesignState(
            glassOpacity = 4.2f,
            glassBlurDp = -12f,
            cornerRadiusDp = 999f,
            lyricsFontSizeSp = 4f,
        ).clamped()

        assertEquals(0.75f, extreme.glassOpacity)
        assertEquals(0f, extreme.glassBlurDp)
        assertEquals(32f, extreme.cornerRadiusDp)
        assertEquals(16f, extreme.lyricsFontSizeSp)
    }

    @Test
    fun `sponsorblock enabled categories map cleanly`() {
        val state = CroixDesignState(
            sponsorBlockEnabled = true,
            skipMusicOfftopic = true,
            skipSponsors = false,
            skipIntroOutro = true,
        )
        val cats = state.enabledSponsorCategories()
        assertTrue("music_offtopic" in cats)
        assertTrue("intro" in cats)
        assertTrue("outro" in cats)
        assertTrue("sponsor" !in cats)
    }
}
