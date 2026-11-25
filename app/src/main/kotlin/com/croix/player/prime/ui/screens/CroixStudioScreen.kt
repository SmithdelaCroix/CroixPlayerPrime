/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.croix.prime.design.CroixAnimatedBackdrop
import com.croix.prime.design.CroixDesignState
import com.croix.prime.design.CroixGlassCard
import com.croix.prime.design.CroixThemeJson
import com.croix.prime.design.LocalCroixDesign
import com.croix.prime.design.LocalCroixPalette
import com.croix.prime.design.PrimeCardDensity
import com.croix.prime.design.PrimeColorMode
import com.croix.prime.design.PrimeLyricsAlign
import com.croix.prime.design.PrimeNavBarStyle
import com.croix.prime.design.PrimePlayerStyle
import com.croix.prime.design.PrimePresets
import com.croix.prime.design.PrimeProgressStyle
import com.croix.prime.design.PrimeVisualizerStyle
import com.croix.prime.playback.EqPreset
import com.croix.prime.playback.PlaybackUiState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlin.math.roundToInt

@Composable
fun SettingsAndStudioOverlay(
    playback: PlaybackUiState,
    onClose: () -> Unit,
    onUpdateDesign: ((CroixDesignState) -> CroixDesignState) -> Unit,
    onApplyPreset: (CroixDesignState) -> Unit,
    onSetEqEnabled: (Boolean) -> Unit,
    onApplyEqPreset: (EqPreset) -> Unit,
    onSetEqBand: (Int, Float) -> Unit,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val bgHaze = remember { HazeState() }
    val touchBlocker = remember { MutableInteractionSource() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showImport by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }
    var importErr by remember { mutableStateOf<String?>(null) }

    val tabs = listOf("Glass", "Gradient", "Home & Nav", "Player", "Gestures & EQ", "Sponsor & Presets")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = touchBlocker, indication = null) {},
    ) {
        CroixAnimatedBackdrop(modifier = Modifier.hazeSource(state = bgHaze))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CroixGlassCard(hazeState = bgHaze, cornerRadius = 50.dp, onClick = onClose) {
                        IconButton(onClick = onClose, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                    Column {
                        Text("Settings & Studio", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text(
                            "Live customization • ARMv8 / ARMv9 • Android 12+",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tabs.forEachIndexed { idx, label ->
                        CroixGlassCard(
                            hazeState = bgHaze,
                            cornerRadius = 50.dp,
                            accentTint = if (selectedTab == idx) palette.primary else null,
                            onClick = { selectedTab = idx },
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }

            item {
                CroixGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = bgHaze) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (selectedTab) {
                            0 -> {
                                StudioSlider("Glass Translucency", design.glassOpacity, 0.05f..0.75f, "${(design.glassOpacity * 100).roundToInt()}%") { v ->
                                    onUpdateDesign { it.copy(glassOpacity = v) }
                                }
                                StudioSlider("Real Hardware Blur", design.glassBlurDp, 0f..34f, "${design.glassBlurDp.roundToInt()} dp") { v ->
                                    onUpdateDesign { it.copy(glassBlurDp = v) }
                                }
                                StudioSlider("Border Glow", design.borderGlowAlpha, 0f..0.65f, "${(design.borderGlowAlpha * 100).roundToInt()}%") { v ->
                                    onUpdateDesign { it.copy(borderGlowAlpha = v) }
                                }
                                StudioSlider("Border Thickness", design.borderWidthDp, 0.5f..2.5f, "${"%.1f".format(design.borderWidthDp)} dp") { v ->
                                    onUpdateDesign { it.copy(borderWidthDp = v) }
                                }
                                StudioSlider("Corner Roundness", design.cornerRadiusDp, 8f..32f, "${design.cornerRadiusDp.roundToInt()} dp") { v ->
                                    onUpdateDesign { it.copy(cornerRadiusDp = v) }
                                }
                                StudioSlider("Ambient Glow", design.ambientGlowStrength, 0f..0.9f, "${(design.ambientGlowStrength * 100).roundToInt()}%") { v ->
                                    onUpdateDesign { it.copy(ambientGlowStrength = v) }
                                }
                                StudioSwitch("Reactive Animated Backdrop", design.backdropMotionEnabled) { v ->
                                    onUpdateDesign { it.copy(backdropMotionEnabled = v) }
                                }
                                if (design.backdropMotionEnabled) {
                                    StudioSlider("Animation Speed", design.backdropMotionSpeed, 0.2f..2.5f, "${"%.1f".format(design.backdropMotionSpeed)}x") { v ->
                                        onUpdateDesign { it.copy(backdropMotionSpeed = v) }
                                    }
                                }
                            }
                            1 -> {
                                StudioEnumRow("Color Engine", PrimeColorMode.entries, design.colorMode, { it.label }) { m ->
                                    onUpdateDesign { it.copy(colorMode = m) }
                                }
                                Text("Free Dual-Color Gradient Editor", color = Color.White, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(design.customPrimaryFromHue, design.customSecondaryFromHue),
                                            ),
                                        ),
                                )
                                StudioSlider("Primary Hue", design.primaryHue, 0f..360f, "${design.primaryHue.roundToInt()}°") { h ->
                                    onUpdateDesign { it.copy(colorMode = PrimeColorMode.CUSTOM_GRADIENT, primaryHue = h) }
                                }
                                StudioSlider("Secondary Hue", design.secondaryHue, 0f..360f, "${design.secondaryHue.roundToInt()}°") { h ->
                                    onUpdateDesign { it.copy(colorMode = PrimeColorMode.CUSTOM_GRADIENT, secondaryHue = h) }
                                }
                            }
                            2 -> {
                                StudioSwitch("Show Flow Hero Wheel", design.showCroixFlowHero) { v ->
                                    onUpdateDesign { it.copy(showCroixFlowHero = v) }
                                }
                                StudioSwitch("Show Moods Row", design.showMoodChips) { v ->
                                    onUpdateDesign { it.copy(showMoodChips = v) }
                                }
                                StudioSwitch("Show Recently Played Grid", design.showQuickResumeGrid) { v ->
                                    onUpdateDesign { it.copy(showQuickResumeGrid = v) }
                                }
                                StudioEnumRow("Card Density", PrimeCardDensity.entries, design.cardDensity, { it.label }) { d ->
                                    onUpdateDesign { it.copy(cardDensity = d) }
                                }
                                StudioEnumRow("Nav Bar Style", PrimeNavBarStyle.entries, design.navBarStyle, { it.label }) { n ->
                                    onUpdateDesign { it.copy(navBarStyle = n) }
                                }
                                StudioSwitch("Show Nav Bar Labels", design.showNavLabels) { v ->
                                    onUpdateDesign { it.copy(showNavLabels = v) }
                                }
                                StudioSwitch("Floating Glass Mini-Player", design.miniPlayerFloating) { v ->
                                    onUpdateDesign { it.copy(miniPlayerFloating = v) }
                                }
                                StudioSlider("Mini-Player Height", design.miniPlayerHeightDp, 56f..84f, "${design.miniPlayerHeightDp.roundToInt()} dp") { v ->
                                    onUpdateDesign { it.copy(miniPlayerHeightDp = v) }
                                }
                            }
                            3 -> {
                                StudioEnumRow("Player Layout", PrimePlayerStyle.entries, design.playerStyle, { it.label }) { s ->
                                    onUpdateDesign { it.copy(playerStyle = s) }
                                }
                                StudioEnumRow("Progress Bar", PrimeProgressStyle.entries, design.progressStyle, { it.label }) { p ->
                                    onUpdateDesign { it.copy(progressStyle = p) }
                                }
                                StudioEnumRow("Live Audio Visualizer", PrimeVisualizerStyle.entries, design.visualizerStyle, { it.label }) { v ->
                                    onUpdateDesign { it.copy(visualizerStyle = v) }
                                }
                                StudioSlider("Artwork Corner Radius", design.artworkCornerDp, 0f..36f, "${design.artworkCornerDp.roundToInt()} dp") { v ->
                                    onUpdateDesign { it.copy(artworkCornerDp = v) }
                                }
                                StudioSwitch("Vinyl Spin Artwork", design.vinylSpinEnabled) { v ->
                                    onUpdateDesign { it.copy(vinylSpinEnabled = v) }
                                }
                                StudioSlider("Control Dock Opacity", design.playerControlDockOpacity, 0.05f..0.80f, "${(design.playerControlDockOpacity * 100).roundToInt()}%") { v ->
                                    onUpdateDesign { it.copy(playerControlDockOpacity = v) }
                                }
                                StudioEnumRow("Lyrics Alignment", PrimeLyricsAlign.entries, design.lyricsAlign, { it.label }) { a ->
                                    onUpdateDesign { it.copy(lyricsAlign = a) }
                                }
                                StudioSlider("Lyrics Font Size", design.lyricsFontSizeSp, 16f..36f, "${design.lyricsFontSizeSp.roundToInt()} sp") { v ->
                                    onUpdateDesign { it.copy(lyricsFontSizeSp = v) }
                                }
                            }
                            4 -> {
                                Text("Pro Gestures", color = palette.primary, fontWeight = FontWeight.Bold)
                                StudioSwitch("Swipe Artwork to Skip Track", design.swipeArtworkToSkip) { v ->
                                    onUpdateDesign { it.copy(swipeArtworkToSkip = v) }
                                }
                                StudioSlider("Double-Tap Seek Step", design.doubleTapSeekSeconds.toFloat(), 5f..30f, "${design.doubleTapSeekSeconds}s") { v ->
                                    onUpdateDesign { it.copy(doubleTapSeekSeconds = v.roundToInt()) }
                                }
                                StudioSwitch("Shake Device to Skip", design.shakeToSkipEnabled) { v ->
                                    onUpdateDesign { it.copy(shakeToSkipEnabled = v) }
                                }
                                if (design.shakeToSkipEnabled) {
                                    StudioSlider("Shake Sensitivity", design.shakeSensitivity, 0.1f..1.0f, "${(design.shakeSensitivity * 100).roundToInt()}%") { v ->
                                        onUpdateDesign { it.copy(shakeSensitivity = v) }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("5-Band Hardware Equalizer", color = palette.primary, fontWeight = FontWeight.Bold)
                                StudioSwitch("Enable Equalizer", playback.eqEnabled, onSetEqEnabled)
                                StudioEnumRow("EQ Preset", EqPreset.entries.filter { it != EqPreset.CUSTOM }, playback.eqPreset, { it.label }) { p ->
                                    onApplyEqPreset(p)
                                }
                                val bands = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
                                playback.eqBandsDb.forEachIndexed { idx, db ->
                                    StudioSlider(bands.getOrElse(idx) { "Band ${idx + 1}" }, db, -12f..12f, "${db.roundToInt()} dB") { v ->
                                        onSetEqBand(idx, v)
                                    }
                                }
                            }
                            5 -> {
                                Text("SponsorBlock", color = palette.primary, fontWeight = FontWeight.Bold)
                                StudioSwitch("Enable SponsorBlock Auto-Skip", design.sponsorBlockEnabled) { v ->
                                    onUpdateDesign { it.copy(sponsorBlockEnabled = v) }
                                }
                                StudioSwitch("Skip Non-Music Sections", design.skipMusicOfftopic) { v ->
                                    onUpdateDesign { it.copy(skipMusicOfftopic = v) }
                                }
                                StudioSwitch("Skip Sponsors & Self-Promo", design.skipSponsors) { v ->
                                    onUpdateDesign { it.copy(skipSponsors = v) }
                                }
                                StudioSwitch("Skip Intros & Outros", design.skipIntroOutro) { v ->
                                    onUpdateDesign { it.copy(skipIntroOutro = v) }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("Theme Presets", color = palette.primary, fontWeight = FontWeight.Bold)
                                PrimePresets.All.forEach { preset ->
                                    CroixGlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 14.dp,
                                        accentTint = if (design.presetName == preset.presetName) palette.primary else null,
                                        onClick = { onApplyPreset(preset) },
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(preset.presetName, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            if (design.presetName == preset.presetName) {
                                                Text("Active", color = palette.primary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            val exported = CroixThemeJson.export(design)
                                            clipboard.setText(AnnotatedString(exported))
                                            val send = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "Croix Player Prime Theme")
                                                putExtra(Intent.EXTRA_TEXT, exported)
                                            }
                                            context.startActivity(Intent.createChooser(send, "Share .croixtheme.json"))
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Share JSON")
                                    }
                                    OutlinedButton(
                                        onClick = { importJson = ""; importErr = null; showImport = true },
                                        modifier = Modifier.weight(1f),
                                    ) { Text("Import JSON") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Import .croixtheme.json") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = importJson,
                        onValueChange = { importJson = it },
                        label = { Text("Paste Croix Theme JSON") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (importErr != null) Text(importErr!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(onClick = {
                    CroixThemeJson.import(importJson)
                        .onSuccess { onApplyPreset(it); showImport = false }
                        .onFailure { importErr = it.message ?: "Invalid JSON" }
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StudioSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onChange: (Float) -> Unit,
) {
    val palette = LocalCroixPalette.current
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Text(valueLabel, color = palette.primary, style = MaterialTheme.typography.labelMedium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun StudioSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T : Enum<T>> StudioEnumRow(
    title: String,
    entries: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val palette = LocalCroixPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            entries.forEach { item ->
                CroixGlassCard(
                    cornerRadius = 12.dp,
                    accentTint = if (item == selected) palette.primary else null,
                    onClick = { onSelect(item) },
                ) {
                    Text(
                        text = labelOf(item),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
