package com.redbolt.screenshot.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.redbolt.screenshot.handler.BoltThemeId

@Immutable
data class BoltColorScheme(
    val accent: Color,
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textMuted: Color,
)

object BoltThemePresets {
    val BoltRed = BoltColorScheme(
        accent = Color(0xFFFF3B30),
        background = Color(0xFF000000),
        surface = Color(0xFF27272A),
        border = Color(0xFF2C2C2C),
        textPrimary = Color(0xFFFFFFFF),
        textMuted = Color(0xFF888888),
    )

    val Glyph = BoltColorScheme(
        accent = Color(0xFFF2E900),
        background = Color(0xFF000000),
        surface = Color(0xFF141414),
        border = Color(0xFF2A2A2A),
        textPrimary = Color(0xFFFFFFFF),
        textMuted = Color(0xFF8A8A8A),
    )

    val Mono = BoltColorScheme(
        accent = Color(0xFFFFFFFF),
        background = Color(0xFF000000),
        surface = Color(0xFF1C1C1C),
        border = Color(0xFF333333),
        textPrimary = Color(0xFFFFFFFF),
        textMuted = Color(0xFF7A7A7A),
    )

    val Ash = BoltColorScheme(
        accent = Color(0xFFA1A1AA),
        background = Color(0xFF0A0A0A),
        surface = Color(0xFF1F1F1F),
        border = Color(0xFF3A3A3A),
        textPrimary = Color(0xFFF4F4F5),
        textMuted = Color(0xFF8B8B93),
    )

    val Ember = BoltColorScheme(
        accent = Color(0xFFFF9F0A),
        background = Color(0xFF000000),
        surface = Color(0xFF221A12),
        border = Color(0xFF3D2E1C),
        textPrimary = Color(0xFFFFFFFF),
        textMuted = Color(0xFF9A8B78),
    )

    val Frost = BoltColorScheme(
        accent = Color(0xFF64D2FF),
        background = Color(0xFF000000),
        surface = Color(0xFF101820),
        border = Color(0xFF1E2D38),
        textPrimary = Color(0xFFFFFFFF),
        textMuted = Color(0xFF7E95A3),
    )

    fun fromId(id: BoltThemeId): BoltColorScheme = when (id) {
        BoltThemeId.BOLT_RED -> BoltRed
        BoltThemeId.GLYPH -> Glyph
        BoltThemeId.MONO -> Mono
        BoltThemeId.ASH -> Ash
        BoltThemeId.EMBER -> Ember
        BoltThemeId.FROST -> Frost
    }
}

// Legacy aliases used during migration
val NothingRed get() = BoltThemePresets.BoltRed.accent
val Black get() = BoltThemePresets.BoltRed.background
val CardGrey get() = BoltThemePresets.BoltRed.surface
val BorderGrey get() = BoltThemePresets.BoltRed.border
val MediumGrey get() = BoltThemePresets.BoltRed.textMuted
val White get() = BoltThemePresets.BoltRed.textPrimary
