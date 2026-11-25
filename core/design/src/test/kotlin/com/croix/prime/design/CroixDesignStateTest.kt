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
        val custom = PrimePresets.FrostedNeonCyan.copy(
            presetName = "My Prime Mix",
            glassOpacity = 0.35f,
            playerStyle = PrimePlayerStyle.TRANSLUCENT_CARD,
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

    @Test
    fun `clamped protects crossfade and transition speed`() {
        val clampedHigh = CroixDesignState(
            crossfadeSeconds = 42,
            transitionSpeed = 9.0f,
        ).clamped()
        assertEquals(10, clampedHigh.crossfadeSeconds)
        assertEquals(2.0f, clampedHigh.transitionSpeed)

        val clampedLow = CroixDesignState(
            crossfadeSeconds = -3,
            transitionSpeed = 0.05f,
        ).clamped()
        assertEquals(0, clampedLow.crossfadeSeconds)
        assertEquals(0.5f, clampedLow.transitionSpeed)
    }

    @Test
    fun `language resolver maps explicit spanish and english cleanly`() {
        val es = resolveCroixStrings(PrimeLanguageMode.SPANISH)
        val en = resolveCroixStrings(PrimeLanguageMode.ENGLISH)
        assertTrue(es.isSpanish)
        assertEquals("Inicio", es.navHome)
        assertEquals("Configuración", es.settingsTitle)
        assertEquals("Home", en.navHome)
        assertEquals("Settings", en.settingsTitle)
    }
}
