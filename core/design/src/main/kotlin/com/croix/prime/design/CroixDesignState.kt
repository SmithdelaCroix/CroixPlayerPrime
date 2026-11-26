/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.design

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class PrimeColorMode(val label: String) {
    VIOLET_GLASS("Cristal Violeta"),
    NEON_CYAN("Neón Cian"),
    ALBUM_MORPH("Color de la canción"),
    DYNAMIC_SYSTEM("Material You"),
    AMOLED_BLACK("AMOLED Negro"),
    CUSTOM_GRADIENT("Color Libre"),
}

@Serializable
enum class PrimePlayerStyle(val label: String) {
    TRANSLUCENT_CARD("Tarjeta Cristal"),
    FROSTED_GLASS("Cristal Escarchado"),
    FULLSCREEN_COVER("Carátula Grande"),
    MINIMAL_CLEAN("Minimalista"),
}

@Serializable
enum class PrimePlayerBackgroundStyle(val label: String) {
    BLURRED_COVER("Carátula Difuminada"),
    DYNAMIC_GRADIENT("Gradiente Vivo"),
    LIVE_MESH("Aurora Viva"),
    SOLID_DARK("Oscuro Minimal"),
    PURE_AMOLED("Negro Puro AMOLED"),
}

@Serializable
enum class PrimePlayerButtonStyle(val label: String) {
    GRADIENT_CIRCLE("Círculo Gradiente"),
    GLASS_FROSTED("Cristal Translúcido"),
    SQUIRCLE_EXPRESSIVE("Squircle Expresivo"),
    OUTLINED_NEON("Borde Neón"),
}

@Serializable
enum class PrimeMiniPlayerStyle(val label: String) {
    FLOATING_GLASS("Cristal Flotante"),
    CAPSULE_PILL("Cápsula Compacta"),
    DOCKED_MINIMAL("Barra Acoplada"),
    PURE_AMOLED("AMOLED Negro"),
}

@Serializable
enum class PrimeArtworkShape(val label: String) {
    ROUNDED("Redondeada"),
    SQUIRCLE("Squircle"),
    SQUARE("Cuadrada"),
    CIRCLE_VINYL("Vinilo Circular"),
    CUT_CORNERS("Esquinas Cortadas"),
}

@Serializable
enum class PrimeFontStyle(val label: String) {
    MODERN_SANS("Moderna Sans"),
    EDITORIAL_SERIF("Editorial Serif"),
    TECH_MONO("Técnica Mono"),
    COMPACT_CLEAN("Limpia Compacta"),
}

@Serializable
enum class PrimeLyricsAnimStyle(val label: String) {
    SMOOTH_FADE("Fundido Suave"),
    SCALE_POP("Escala Viva"),
    NEON_GLOW("Brillo Neón"),
    KARAOKE_SLIDE("Desplazamiento"),
}

@Serializable
enum class PrimeStartTab(val label: String, val route: String) {
    HOME("Inicio", "home"),
    SEARCH("Buscar", "search"),
    FAVORITES("Favoritos", "favorites"),
}

@Serializable
enum class PrimeProgressStyle(val label: String) {
    WAVEFORM("Onda"),
    CAPSULE("Cápsula"),
    THIN_LINE("Línea Fina"),
    SQUIGGLY("Ondulada"),
    DASHED_EQ("Segmentada"),
    GLOW_BAR("Barra Neón"),
}

@Serializable
enum class PrimeVisualizerStyle(val label: String) {
    OFF("Apagado"),
    SPECTRUM_BARS("Barras Espectro"),
    PULSE_WAVE("Onda Pulso"),
    ORBIT_RINGS("Anillos Órbita"),
}

@Serializable
enum class PrimeNavBarStyle(val label: String) {
    FLOATING_GLASS_PILL("Píldora Flotante"),
    DOCKED_TRANSLUCENT("Barra Acoplada"),
    COMPACT_ICONS("Iconos Compactos"),
}

@Serializable
enum class PrimeCardDensity(val label: String) {
    COMPACT("Compacto"),
    EDITORIAL("Editorial"),
    SPACIOUS("Amplio"),
}

@Serializable
enum class PrimeLyricsAlign(val label: String) {
    START("Izquierda"),
    CENTER("Centro"),
    END("Derecha"),
}

@Serializable
enum class PrimeTransitionStyle(val labelEn: String, val labelEs: String) {
    SMOOTH_SLIDE("Smooth Slide", "Deslizamiento Suave"),
    GENTLE_FADE("Gentle Fade", "Fundido Suave"),
    SNAPPY_ZOOM("Snappy Zoom", "Zoom Fluido"),
    NONE("Instant", "Sin Animación"),
}

@Serializable
enum class PrimeLanguageMode(val labelEn: String, val labelEs: String) {
    SPANISH("Español", "Español"),
    SYSTEM("System Auto", "Automático del Sistema"),
    ENGLISH("English", "Inglés"),
}

@Serializable
enum class PrimeAudioQuality(val labelEs: String, val labelEn: String) {
    MAX_QUALITY("Máxima (Opus/AAC)", "Maximum (Opus/AAC)"),
    BALANCED("Balanceada (128 kbps)", "Balanced (128 kbps)"),
    DATA_SAVER("Ahorro de Datos", "Data Saver"),
}

@Immutable
data class PrimeColorSwatch(
    val id: String,
    val label: String,
    val primaryHue: Float,
    val secondaryHue: Float,
    val primarySat: Float = 0.85f,
    val secondarySat: Float = 0.85f,
    val previewArgb: Long,
    val secondaryPreviewArgb: Long,
    val isDynamicAlbum: Boolean = false,
)

object PrimeColorSwatches {
    val All: List<PrimeColorSwatch> = listOf(
        PrimeColorSwatch("dynamic", "Canción", 272f, 330f, 0.85f, 0.85f, 0xFFFF4081, 0xFF00E5FF, isDynamicAlbum = true),
        PrimeColorSwatch("violet", "Violeta", 272f, 330f, 0.82f, 0.86f, 0xFFA238FF, 0xFFFF2E93),
        PrimeColorSwatch("magenta", "Magenta", 322f, 275f, 0.88f, 0.84f, 0xFFFF2E93, 0xFFA238FF),
        PrimeColorSwatch("crimson", "Carmesí", 352f, 22f, 0.88f, 0.86f, 0xFFFF1744, 0xFFFF6D00),
        PrimeColorSwatch("coral", "Coral", 14f, 338f, 0.88f, 0.84f, 0xFFFF5E5B, 0xFFFF2E93),
        PrimeColorSwatch("solar", "Naranja", 28f, 48f, 0.92f, 0.88f, 0xFFFF7A00, 0xFFFFCA28),
        PrimeColorSwatch("gold", "Oro", 44f, 24f, 0.90f, 0.82f, 0xFFFFC107, 0xFFFF6F00),
        PrimeColorSwatch("lime", "Lima Neón", 86f, 152f, 0.90f, 0.86f, 0xFFAEEA00, 0xFF00E676),
        PrimeColorSwatch("emerald", "Esmeralda", 148f, 190f, 0.86f, 0.82f, 0xFF00E676, 0xFF00B8D4),
        PrimeColorSwatch("turquoise", "Turquesa", 174f, 212f, 0.86f, 0.84f, 0xFF1DE9B6, 0xFF2979FF),
        PrimeColorSwatch("cyan", "Cian", 188f, 260f, 0.90f, 0.84f, 0xFF00E5FF, 0xFF7C4DFF),
        PrimeColorSwatch("sky", "Azul Cielo", 206f, 242f, 0.86f, 0.84f, 0xFF29B6F6, 0xFF536DFE),
        PrimeColorSwatch("indigo", "Índigo", 236f, 286f, 0.84f, 0.84f, 0xFF536DFE, 0xFFAA00FF),
        PrimeColorSwatch("lavender", "Lavanda", 262f, 304f, 0.56f, 0.62f, 0xFFB388FF, 0xFFEA80FC),
        PrimeColorSwatch("sakura", "Sakura", 336f, 294f, 0.54f, 0.52f, 0xFFFF80AB, 0xFFCE93D8),
        PrimeColorSwatch("mono", "Plata Mono", 220f, 220f, 0.25f, 0.25f, 0xFFD0D5E2, 0xFF8C94A8),
    )
}

@Immutable
@Serializable
data class CroixDesignState(
    val presetName: String = "Cristal Violeta",
    // Glassmorphism translucency controls (applied across the ENTIRE app)
    val glassOpacity: Float = 0.16f,
    val glassBlurDp: Float = 22f,
    val glassNoiseEnabled: Boolean = true,
    val borderGlowAlpha: Float = 0.24f,
    val borderWidthDp: Float = 1.0f,
    val cornerRadiusDp: Float = 22f,
    val ambientGlowStrength: Float = 0.50f,
    val backdropMotionEnabled: Boolean = true,
    val backdropMotionSpeed: Float = 1.0f,
    // Typography & Text Behavior
    val fontScale: Float = 1.0f,
    val fontStyle: PrimeFontStyle = PrimeFontStyle.MODERN_SANS,
    val twoLineTrackTitle: Boolean = false,
    val marqueeTrackTitle: Boolean = true,
    // Deep Dynamic Color & Gradient Engine
    val colorMode: PrimeColorMode = PrimeColorMode.VIOLET_GLASS,
    val useGradientStyle: Boolean = true,
    val customAccentArgb: Long = 0xFFA238FF,
    val secondaryAccentArgb: Long = 0xFFFF2E93,
    val primaryHue: Float = 272f,
    val secondaryHue: Float = 330f,
    val primarySaturation: Float = 0.80f,
    val secondarySaturation: Float = 0.84f,
    val pureBlackBackground: Boolean = false,
    val randomThemeOnStartup: Boolean = false,
    // Home Screen Editorial Customization
    val showCroixFlowHero: Boolean = true,
    val showMoodChips: Boolean = true,
    val showDiscoveryDuo: Boolean = true,
    val showAiCuratorCard: Boolean = true,
    val showQuickResumeGrid: Boolean = true,
    val showDailyMixes: Boolean = true,
    val showQuickPicks: Boolean = true,
    val showFavoriteArtistsRow: Boolean = true,
    val cardDensity: PrimeCardDensity = PrimeCardDensity.EDITORIAL,
    // Now Playing, Artwork & Visualizer Customization
    val playerStyle: PrimePlayerStyle = PrimePlayerStyle.TRANSLUCENT_CARD,
    val playerBackgroundStyle: PrimePlayerBackgroundStyle = PrimePlayerBackgroundStyle.BLURRED_COVER,
    val playerButtonStyle: PrimePlayerButtonStyle = PrimePlayerButtonStyle.GRADIENT_CIRCLE,
    val artworkShape: PrimeArtworkShape = PrimeArtworkShape.ROUNDED,
    val artworkScale: Float = 0.86f,
    val artworkShadowEnabled: Boolean = true,
    val artworkShadowElevationDp: Float = 18f,
    val hidePlayerArtwork: Boolean = false,
    val tapArtworkToPlayPause: Boolean = false,
    val showPlayerVolumeBar: Boolean = false,
    val keepScreenOnPlayer: Boolean = false,
    val progressStyle: PrimeProgressStyle = PrimeProgressStyle.WAVEFORM,
    val visualizerStyle: PrimeVisualizerStyle = PrimeVisualizerStyle.SPECTRUM_BARS,
    val artworkCornerDp: Float = 24f,
    val vinylSpinEnabled: Boolean = false,
    val playerControlDockOpacity: Float = 0.22f,
    // Playback & Audio Quality
    val audioQuality: PrimeAudioQuality = PrimeAudioQuality.MAX_QUALITY,
    val normalizeAudio: Boolean = true,
    val autoPlayRelated: Boolean = true,
    // Gestures & Hardware
    val swipeArtworkToSkip: Boolean = true,
    val swipeMiniPlayerToSkip: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val shakeToSkipEnabled: Boolean = false,
    val shakeSensitivity: Float = 0.55f,
    // Navigation & Mini-Player
    val navBarStyle: PrimeNavBarStyle = PrimeNavBarStyle.FLOATING_GLASS_PILL,
    val showNavLabels: Boolean = true,
    val slimNavBar: Boolean = false,
    val defaultStartTab: PrimeStartTab = PrimeStartTab.HOME,
    val miniPlayerStyle: PrimeMiniPlayerStyle = PrimeMiniPlayerStyle.FLOATING_GLASS,
    val miniPlayerFloating: Boolean = true,
    val miniPlayerHeightDp: Float = 66f,
    // Lyrics Customization
    val lyricsFontSizeSp: Float = 24f,
    val lyricsAlign: PrimeLyricsAlign = PrimeLyricsAlign.START,
    val lyricsAnimStyle: PrimeLyricsAnimStyle = PrimeLyricsAnimStyle.SCALE_POP,
    val lyricsLineSpacingDp: Float = 12f,
    val lyricsActiveGlow: Boolean = true,
    val lyricsBackdropBlurDp: Float = 26f,
    val lyricsOffsetMs: Long = 0L,
    val lyricsAutoRomanize: Boolean = false,
    val lyricsAutoTranslateEs: Boolean = false,
    // SponsorBlock
    val sponsorBlockEnabled: Boolean = true,
    val skipMusicOfftopic: Boolean = true,
    val skipSponsors: Boolean = true,
    val skipIntroOutro: Boolean = true,
    // Transitions, Crossfade & Language (Default 100% Spanish!)
    val transitionStyle: PrimeTransitionStyle = PrimeTransitionStyle.SMOOTH_SLIDE,
    val transitionSpeed: Float = 1.0f,
    val crossfadeSeconds: Int = 0,
    val languageMode: PrimeLanguageMode = PrimeLanguageMode.SPANISH,
    // v4.0 / v4.5 Prime Pro Features
    val audioCacheLimitMb: Int = 512,
    val canvasMotionEnabled: Boolean = true,
    val artworkParallax3d: Boolean = false,
    val parallax3dIntensity: Float = 1.35f,
    val privateSessionEnabled: Boolean = false,
    val appIconId: String = "violet",
    val autoOutputProfilesEnabled: Boolean = true,
    val bluetoothAutoEqPreset: String = "SONY_XM",
    val wiredAutoEqPreset: String = "STUDIO_IEM",
    val speakerAutoEqPreset: String = "FLAT",
) {
    fun clamped(): CroixDesignState = copy(
        glassOpacity = glassOpacity.coerceIn(0.05f, 0.75f),
        glassBlurDp = glassBlurDp.coerceIn(0f, 34f),
        borderGlowAlpha = borderGlowAlpha.coerceIn(0.0f, 0.65f),
        borderWidthDp = borderWidthDp.coerceIn(0.5f, 2.5f),
        cornerRadiusDp = cornerRadiusDp.coerceIn(8f, 32f),
        ambientGlowStrength = ambientGlowStrength.coerceIn(0f, 0.9f),
        backdropMotionSpeed = backdropMotionSpeed.coerceIn(0.2f, 2.5f),
        fontScale = fontScale.coerceIn(0.85f, 1.20f),
        primaryHue = primaryHue.coerceIn(0f, 360f),
        secondaryHue = secondaryHue.coerceIn(0f, 360f),
        primarySaturation = primarySaturation.coerceIn(0.25f, 1.0f),
        secondarySaturation = secondarySaturation.coerceIn(0.25f, 1.0f),
        artworkCornerDp = artworkCornerDp.coerceIn(0f, 36f),
        artworkScale = artworkScale.coerceIn(0.64f, 0.98f),
        artworkShadowElevationDp = artworkShadowElevationDp.coerceIn(0f, 32f),
        playerControlDockOpacity = playerControlDockOpacity.coerceIn(0.05f, 0.80f),
        doubleTapSeekSeconds = doubleTapSeekSeconds.coerceIn(5, 30),
        shakeSensitivity = shakeSensitivity.coerceIn(0.1f, 1.0f),
        miniPlayerHeightDp = miniPlayerHeightDp.coerceIn(56f, 84f),
        lyricsFontSizeSp = lyricsFontSizeSp.coerceIn(16f, 36f),
        lyricsLineSpacingDp = lyricsLineSpacingDp.coerceIn(6f, 26f),
        lyricsBackdropBlurDp = lyricsBackdropBlurDp.coerceIn(0f, 34f),
        lyricsOffsetMs = lyricsOffsetMs.coerceIn(-5_000L, 5_000L),
        transitionSpeed = transitionSpeed.coerceIn(0.5f, 2.0f),
        crossfadeSeconds = crossfadeSeconds.coerceIn(0, 10),
        audioCacheLimitMb = audioCacheLimitMb.coerceIn(0, 4096),
        parallax3dIntensity = parallax3dIntensity.coerceIn(0.5f, 2.2f),
    )

    val customPrimaryFromHue: Color
        get() = Color.hsv(primaryHue.coerceIn(0f, 360f), primarySaturation.coerceIn(0.25f, 1f), 1.0f)
    val customSecondaryFromHue: Color
        get() = Color.hsv(secondaryHue.coerceIn(0f, 360f), secondarySaturation.coerceIn(0.25f, 1f), 1.0f)

    fun enabledSponsorCategories(): Set<String> = buildSet {
        if (!sponsorBlockEnabled) return@buildSet
        if (skipMusicOfftopic) add("music_offtopic")
        if (skipSponsors) {
            add("sponsor")
            add("selfpromo")
            add("interaction")
        }
        if (skipIntroOutro) {
            add("intro")
            add("outro")
        }
    }
}

object PrimePresets {
    val PrimeVioletGlass = CroixDesignState(
        presetName = "Cristal Violeta",
        glassOpacity = 0.16f,
        glassBlurDp = 22f,
        borderGlowAlpha = 0.26f,
        cornerRadiusDp = 22f,
        colorMode = PrimeColorMode.VIOLET_GLASS,
        primaryHue = 272f,
        secondaryHue = 330f,
        playerStyle = PrimePlayerStyle.TRANSLUCENT_CARD,
        playerBackgroundStyle = PrimePlayerBackgroundStyle.BLURRED_COVER,
        playerButtonStyle = PrimePlayerButtonStyle.GRADIENT_CIRCLE,
        miniPlayerStyle = PrimeMiniPlayerStyle.FLOATING_GLASS,
        artworkShape = PrimeArtworkShape.ROUNDED,
        progressStyle = PrimeProgressStyle.WAVEFORM,
        visualizerStyle = PrimeVisualizerStyle.SPECTRUM_BARS,
    )

    val FrostedNeonCyan = CroixDesignState(
        presetName = "Neón Cian",
        glassOpacity = 0.20f,
        glassBlurDp = 28f,
        borderGlowAlpha = 0.34f,
        borderWidthDp = 1.2f,
        cornerRadiusDp = 26f,
        colorMode = PrimeColorMode.NEON_CYAN,
        primaryHue = 186f,
        secondaryHue = 262f,
        playerStyle = PrimePlayerStyle.FROSTED_GLASS,
        playerBackgroundStyle = PrimePlayerBackgroundStyle.LIVE_MESH,
        playerButtonStyle = PrimePlayerButtonStyle.OUTLINED_NEON,
        miniPlayerStyle = PrimeMiniPlayerStyle.FLOATING_GLASS,
        artworkShape = PrimeArtworkShape.SQUIRCLE,
        progressStyle = PrimeProgressStyle.GLOW_BAR,
        visualizerStyle = PrimeVisualizerStyle.PULSE_WAVE,
    )

    val AlbumMorphLive = CroixDesignState(
        presetName = "Carátula Viva",
        glassOpacity = 0.18f,
        glassBlurDp = 26f,
        borderGlowAlpha = 0.28f,
        ambientGlowStrength = 0.65f,
        colorMode = PrimeColorMode.ALBUM_MORPH,
        playerStyle = PrimePlayerStyle.FULLSCREEN_COVER,
        playerBackgroundStyle = PrimePlayerBackgroundStyle.DYNAMIC_GRADIENT,
        playerButtonStyle = PrimePlayerButtonStyle.SQUIRCLE_EXPRESSIVE,
        miniPlayerStyle = PrimeMiniPlayerStyle.CAPSULE_PILL,
        artworkShape = PrimeArtworkShape.ROUNDED,
        artworkScale = 0.92f,
        progressStyle = PrimeProgressStyle.CAPSULE,
        visualizerStyle = PrimeVisualizerStyle.ORBIT_RINGS,
        lyricsAlign = PrimeLyricsAlign.CENTER,
        lyricsAnimStyle = PrimeLyricsAnimStyle.NEON_GLOW,
    )

    val SakuraVapor = CroixDesignState(
        presetName = "Sakura Vapor",
        glassOpacity = 0.20f,
        glassBlurDp = 26f,
        borderGlowAlpha = 0.30f,
        cornerRadiusDp = 26f,
        ambientGlowStrength = 0.62f,
        fontStyle = PrimeFontStyle.EDITORIAL_SERIF,
        colorMode = PrimeColorMode.CUSTOM_GRADIENT,
        primaryHue = 336f,
        secondaryHue = 286f,
        primarySaturation = 0.56f,
        secondarySaturation = 0.58f,
        playerBackgroundStyle = PrimePlayerBackgroundStyle.LIVE_MESH,
        playerButtonStyle = PrimePlayerButtonStyle.SQUIRCLE_EXPRESSIVE,
        miniPlayerStyle = PrimeMiniPlayerStyle.CAPSULE_PILL,
        artworkShape = PrimeArtworkShape.SQUIRCLE,
        progressStyle = PrimeProgressStyle.SQUIGGLY,
        lyricsAlign = PrimeLyricsAlign.CENTER,
        lyricsAnimStyle = PrimeLyricsAnimStyle.SCALE_POP,
    )

    val CyberLime = CroixDesignState(
        presetName = "Ciber Lima",
        glassOpacity = 0.12f,
        glassBlurDp = 18f,
        borderGlowAlpha = 0.40f,
        borderWidthDp = 1.4f,
        cornerRadiusDp = 14f,
        fontStyle = PrimeFontStyle.TECH_MONO,
        colorMode = PrimeColorMode.CUSTOM_GRADIENT,
        primaryHue = 88f,
        secondaryHue = 178f,
        primarySaturation = 0.92f,
        secondarySaturation = 0.88f,
        pureBlackBackground = true,
        playerBackgroundStyle = PrimePlayerBackgroundStyle.SOLID_DARK,
        playerButtonStyle = PrimePlayerButtonStyle.OUTLINED_NEON,
        miniPlayerStyle = PrimeMiniPlayerStyle.PURE_AMOLED,
        artworkShape = PrimeArtworkShape.CUT_CORNERS,
        progressStyle = PrimeProgressStyle.DASHED_EQ,
        visualizerStyle = PrimeVisualizerStyle.SPECTRUM_BARS,
        lyricsAnimStyle = PrimeLyricsAnimStyle.KARAOKE_SLIDE,
    )

    val NordicAurora = CroixDesignState(
        presetName = "Aurora Nórdica",
        glassOpacity = 0.18f,
        glassBlurDp = 24f,
        borderGlowAlpha = 0.26f,
        ambientGlowStrength = 0.68f,
        colorMode = PrimeColorMode.CUSTOM_GRADIENT,
        primaryHue = 166f,
        secondaryHue = 236f,
        primarySaturation = 0.82f,
        secondarySaturation = 0.86f,
        playerBackgroundStyle = PrimePlayerBackgroundStyle.LIVE_MESH,
        playerButtonStyle = PrimePlayerButtonStyle.GLASS_FROSTED,
        miniPlayerStyle = PrimeMiniPlayerStyle.FLOATING_GLASS,
        artworkShape = PrimeArtworkShape.SQUIRCLE,
        progressStyle = PrimeProgressStyle.GLOW_BAR,
        lyricsAnimStyle = PrimeLyricsAnimStyle.NEON_GLOW,
    )

    val SunsetCoral = CroixDesignState(
        presetName = "Atardecer Coral",
        glassOpacity = 0.18f,
        glassBlurDp = 24f,
        borderGlowAlpha = 0.30f,
        ambientGlowStrength = 0.58f,
        colorMode = PrimeColorMode.CUSTOM_GRADIENT,
        primaryHue = 16f,
        secondaryHue = 338f,
        primarySaturation = 0.88f,
        secondarySaturation = 0.85f,
        playerStyle = PrimePlayerStyle.TRANSLUCENT_CARD,
        playerBackgroundStyle = PrimePlayerBackgroundStyle.DYNAMIC_GRADIENT,
        progressStyle = PrimeProgressStyle.WAVEFORM,
    )

    val DeepEmerald = CroixDesignState(
        presetName = "Esmeralda Profunda",
        glassOpacity = 0.17f,
        glassBlurDp = 22f,
        borderGlowAlpha = 0.28f,
        ambientGlowStrength = 0.54f,
        colorMode = PrimeColorMode.CUSTOM_GRADIENT,
        primaryHue = 148f,
        secondaryHue = 192f,
        primarySaturation = 0.85f,
        secondarySaturation = 0.82f,
        playerStyle = PrimePlayerStyle.FROSTED_GLASS,
        playerBackgroundStyle = PrimePlayerBackgroundStyle.BLURRED_COVER,
        progressStyle = PrimeProgressStyle.CAPSULE,
    )

    val MidnightGold = CroixDesignState(
        presetName = "Oro Nocturno",
        glassOpacity = 0.14f,
        glassBlurDp = 18f,
        borderGlowAlpha = 0.32f,
        ambientGlowStrength = 0.44f,
        pureBlackBackground = true,
        fontStyle = PrimeFontStyle.EDITORIAL_SERIF,
        colorMode = PrimeColorMode.CUSTOM_GRADIENT,
        primaryHue = 42f,
        secondaryHue = 22f,
        primarySaturation = 0.90f,
        secondarySaturation = 0.82f,
        playerStyle = PrimePlayerStyle.MINIMAL_CLEAN,
        playerBackgroundStyle = PrimePlayerBackgroundStyle.PURE_AMOLED,
        playerButtonStyle = PrimePlayerButtonStyle.OUTLINED_NEON,
        artworkShape = PrimeArtworkShape.CIRCLE_VINYL,
        vinylSpinEnabled = true,
        progressStyle = PrimeProgressStyle.THIN_LINE,
    )

    val MonoStudio = CroixDesignState(
        presetName = "Estudio Monocromo",
        glassOpacity = 0.11f,
        glassBlurDp = 16f,
        borderGlowAlpha = 0.22f,
        cornerRadiusDp = 12f,
        ambientGlowStrength = 0.18f,
        useGradientStyle = false,
        fontStyle = PrimeFontStyle.COMPACT_CLEAN,
        colorMode = PrimeColorMode.CUSTOM_GRADIENT,
        primaryHue = 220f,
        secondaryHue = 220f,
        primarySaturation = 0.25f,
        secondarySaturation = 0.25f,
        playerBackgroundStyle = PrimePlayerBackgroundStyle.SOLID_DARK,
        playerButtonStyle = PrimePlayerButtonStyle.GLASS_FROSTED,
        miniPlayerStyle = PrimeMiniPlayerStyle.DOCKED_MINIMAL,
        artworkShape = PrimeArtworkShape.SQUARE,
        progressStyle = PrimeProgressStyle.THIN_LINE,
        slimNavBar = true,
    )

    val AmoledObsidian = CroixDesignState(
        presetName = "Obsidiana AMOLED",
        glassOpacity = 0.08f,
        glassBlurDp = 10f,
        borderGlowAlpha = 0.16f,
        ambientGlowStrength = 0.12f,
        backdropMotionEnabled = false,
        pureBlackBackground = true,
        cornerRadiusDp = 16f,
        colorMode = PrimeColorMode.AMOLED_BLACK,
        playerStyle = PrimePlayerStyle.MINIMAL_CLEAN,
        playerBackgroundStyle = PrimePlayerBackgroundStyle.PURE_AMOLED,
        miniPlayerStyle = PrimeMiniPlayerStyle.PURE_AMOLED,
        progressStyle = PrimeProgressStyle.THIN_LINE,
        visualizerStyle = PrimeVisualizerStyle.OFF,
        miniPlayerFloating = false,
    )

    val DynamicGlass = CroixDesignState(
        presetName = "Cristal Dinámico",
        glassOpacity = 0.20f,
        glassBlurDp = 20f,
        borderGlowAlpha = 0.22f,
        colorMode = PrimeColorMode.DYNAMIC_SYSTEM,
        playerStyle = PrimePlayerStyle.TRANSLUCENT_CARD,
        playerButtonStyle = PrimePlayerButtonStyle.SQUIRCLE_EXPRESSIVE,
        progressStyle = PrimeProgressStyle.CAPSULE,
    )

    val All: List<CroixDesignState> = listOf(
        PrimeVioletGlass,
        FrostedNeonCyan,
        AlbumMorphLive,
        SakuraVapor,
        CyberLime,
        NordicAurora,
        SunsetCoral,
        DeepEmerald,
        MidnightGold,
        MonoStudio,
        AmoledObsidian,
        DynamicGlass,
    )
}

object CroixThemeJson {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun export(state: CroixDesignState): String = json.encodeToString(CroixDesignState.serializer(), state.clamped())

    fun import(raw: String): Result<CroixDesignState> = runCatching {
        json.decodeFromString(CroixDesignState.serializer(), raw.trim()).clamped()
    }
}

private val Context.croixDesignDataStore: DataStore<Preferences> by preferencesDataStore(name = "croix_prime_design")

class CroixDesignRepository(private val context: Context) {
    private val key = stringPreferencesKey("design_state_v1")

    val designFlow: Flow<CroixDesignState> = context.croixDesignDataStore.data.map { prefs ->
        val raw = prefs[key] ?: return@map PrimePresets.PrimeVioletGlass
        CroixThemeJson.import(raw).getOrElse { PrimePresets.PrimeVioletGlass }
    }

    suspend fun update(transform: (CroixDesignState) -> CroixDesignState) {
        context.croixDesignDataStore.edit { prefs ->
            val current = prefs[key]?.let { CroixThemeJson.import(it).getOrNull() } ?: PrimePresets.PrimeVioletGlass
            val updated = transform(current).clamped()
            prefs[key] = CroixThemeJson.export(updated)
        }
    }

    suspend fun applyPreset(preset: CroixDesignState) {
        context.croixDesignDataStore.edit { prefs ->
            val existing = prefs[key]?.let { CroixThemeJson.import(it).getOrNull() }
            // Preserve user's language, privacy, cache, SponsorBlock, app icon & startup prefs when applying visual presets!
            val merged = if (existing != null) {
                preset.copy(
                    languageMode = existing.languageMode,
                    privateSessionEnabled = existing.privateSessionEnabled,
                    audioCacheLimitMb = existing.audioCacheLimitMb,
                    sponsorBlockEnabled = existing.sponsorBlockEnabled,
                    skipMusicOfftopic = existing.skipMusicOfftopic,
                    skipSponsors = existing.skipSponsors,
                    skipIntroOutro = existing.skipIntroOutro,
                    appIconId = existing.appIconId,
                    defaultStartTab = existing.defaultStartTab,
                    randomThemeOnStartup = existing.randomThemeOnStartup,
                    audioQuality = existing.audioQuality,
                    normalizeAudio = existing.normalizeAudio,
                    autoPlayRelated = existing.autoPlayRelated,
                    crossfadeSeconds = existing.crossfadeSeconds,
                    autoOutputProfilesEnabled = existing.autoOutputProfilesEnabled,
                    bluetoothAutoEqPreset = existing.bluetoothAutoEqPreset,
                    wiredAutoEqPreset = existing.wiredAutoEqPreset,
                    speakerAutoEqPreset = existing.speakerAutoEqPreset,
                ).clamped()
            } else {
                preset.clamped()
            }
            prefs[key] = CroixThemeJson.export(merged)
        }
    }

    suspend fun resetToDefaults() {
        context.croixDesignDataStore.edit { prefs ->
            prefs[key] = CroixThemeJson.export(PrimePresets.PrimeVioletGlass)
        }
    }
}
