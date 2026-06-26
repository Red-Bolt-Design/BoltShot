package com.redbolt.screenshot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.redbolt.screenshot.R
import com.redbolt.screenshot.handler.BoltThemeId
import com.redbolt.screenshot.handler.ScreenshotPreferences

/** Doto Rounded SemiBold — Google Fonts instance (ROND 100, wght 600). */
@OptIn(ExperimentalTextApi::class)
val DotoFont = FontFamily(
    Font(
        R.font.doto_rounded_semibold,
        weight = FontWeight.SemiBold,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontWeight.SemiBold,
            FontStyle.Normal,
            FontVariation.Setting("ROND", 100f),
        ),
    ),
)

/** Roboto Mono for settings body copy — pairs with Doto display headlines. */
val BodyFont = FontFamily(Font(R.font.roboto_mono, FontWeight.Normal))

val LocalBoltColors = staticCompositionLocalOf { BoltThemePresets.BoltRed }

object BoltTheme {
    val colors: BoltColorScheme
        @Composable get() = LocalBoltColors.current
}

@Composable
fun BoltScreenshotTheme(
    themeId: BoltThemeId? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val resolvedId = themeId ?: ScreenshotPreferences(context).themeId
    val boltColors = BoltThemePresets.fromId(resolvedId)
    val materialColors = darkColorScheme(
        primary = boltColors.accent,
        onPrimary = boltColors.background,
        background = boltColors.background,
        onBackground = boltColors.textPrimary,
        surface = boltColors.surface,
        onSurface = boltColors.textPrimary,
        outline = boltColors.border,
    )

    CompositionLocalProvider(LocalBoltColors provides boltColors) {
        MaterialTheme(
            colorScheme = materialColors,
            content = content,
        )
    }
}
