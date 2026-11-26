/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CroixDesignStateTest {
    @Test
    fun `json export and import preserves full design state including v5 customization`() {
        val custom = PrimePresets.SakuraVapor.copy(
            presetName = "Mi Estilo Prime v5",
            glassOpacity = 0.35f,
            glassNoiseEnabled = false,
            playerStyle = PrimePlayerStyle.TRANSLUCENT_CARD,
            playerBackgroundStyle = PrimePlayerBackgroundStyle.LIVE_MESH,
            playerButtonStyle = PrimePlayerButtonStyle.SQUIRCLE_EXPRESSIVE,
            miniPlayerStyle = PrimeMiniPlayerStyle.CAPSULE_PILL,
            artworkShape = PrimeArtworkShape.CUT_CORNERS,
            artworkScale = 0.90f,
            artworkShadowEnabled = true,
            artworkShadowElevationDp = 24f,
            hidePlayerArtwork = false,
            tapArtworkToPlayPause = true,
            showPlayerVolumeBar = true,
            keepScreenOnPlayer = true,
            swipeMiniPlayerToSkip = true,
            fontStyle = PrimeFontStyle.EDITORIAL_SERIF,
            twoLineTrackTitle = true,
            marqueeTrackTitle = false,
            slimNavBar = true,
            defaultStartTab = PrimeStartTab.FAVORITES,
            lyricsAnimStyle = PrimeLyricsAnimStyle.NEON_GLOW,
            lyricsLineSpacingDp = 18f,
            lyricsActiveGlow = true,
            randomThemeOnStartup = true,
            showDiscoveryDuo = false,
            showAiCuratorCard = false,
            progressStyle = PrimeProgressStyle.GLOW_BAR,
        )
        val encoded = CroixThemeJson.export(custom)
        val decoded = CroixThemeJson.import(encoded).getOrThrow()
        assertEquals(custom, decoded)
    }

    @Test
    fun `clamped protects extreme slider inputs including v5 artwork and lyrics sliders`() {
        val extreme = CroixDesignState(
            glassOpacity = 4.2f,
            glassBlurDp = -12f,
            cornerRadiusDp = 999f,
            lyricsFontSizeSp = 4f,
            artworkScale = 5.0f,
            artworkShadowElevationDp = 120f,
            lyricsLineSpacingDp = 2f,
        ).clamped()

        assertEquals(0.75f, extreme.glassOpacity)
        assertEquals(0f, extreme.glassBlurDp)
        assertEquals(32f, extreme.cornerRadiusDp)
        assertEquals(16f, extreme.lyricsFontSizeSp)
        assertEquals(0.98f, extreme.artworkScale)
        assertEquals(32f, extreme.artworkShadowElevationDp)
        assertEquals(6f, extreme.lyricsLineSpacingDp)
    }

    @Test
    fun `presets and color swatches contain curated v5 entries`() {
        assertEquals(12, PrimePresets.All.size)
        assertEquals(16, PrimeColorSwatches.All.size)
        assertTrue(PrimePresets.All.map { it.presetName }.distinct().size == PrimePresets.All.size)
        assertTrue(PrimeColorSwatches.All.any { it.isDynamicAlbum })
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
