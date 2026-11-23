package com.arlodev.onlinenow.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arlodev.onlinenow.R

enum class ThemeMode { System, Light, Dark }
enum class IconStyle { Dots, Original }
enum class DrawerLayout { Grid, List }
enum class BackdropMode { Black, Dim, Wallpaper }

data class Palette(
    val background: Color,
    val card: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    /** unlit dots and switched-off controls */
    val dotOff: Color,
    val accent: Color,
    val onAccent: Color,
    val danger: Color,
    val dark: Boolean,
)

val AccentChoices = listOf(
    Color(0xFF4ADE80), // online green (default)
    Color(0xFFD71921), // signal red
    Color(0xFFFF7A59), // coral
    Color(0xFF7CC4FF), // sky
    Color(0xFFB79CFF), // lilac
    Color(0xFFFFD966), // lemon
    Color(0xFFE5E5E5), // mono
)

val DefaultAccent: Color = AccentChoices.first()

fun paletteFor(dark: Boolean, accent: Color): Palette {
    val onAccent = if (accent.luminance() > 0.38f) Color(0xFF0A0A0A) else Color(0xFFFFFFFF)
    return if (dark) {
        Palette(
            background = Color(0xFF000000),
            card = Color(0xFF0C0C0C),
            border = Color(0xFF222222),
            text = Color(0xFFF5F5F5),
            muted = Color(0xFF8C8C8C),
            dotOff = Color(0xFF2E2E2E),
            accent = accent,
            onAccent = onAccent,
            danger = Color(0xFFFF4D45),
            dark = true,
        )
    } else {
        Palette(
            background = Color(0xFFF3F3F1),
            card = Color(0xFFFFFFFF),
            border = Color(0xFFDCDCD8),
            text = Color(0xFF0E0E0E),
            muted = Color(0xFF737373),
            dotOff = Color(0xFFD6D6D2),
            accent = accent,
            onAccent = onAccent,
            danger = Color(0xFFD92D20),
            dark = false,
        )
    }
}

val LocalPalette = staticCompositionLocalOf { paletteFor(true, DefaultAccent) }

/** Space Mono (SIL Open Font License, see /licenses). Bundled in res/font. */
val MonoFont: FontFamily = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_bold, FontWeight.Bold),
)

fun mono(size: Float, color: Color, bold: Boolean = false, spacing: Float = 0.8f): TextStyle = TextStyle(
    color = color,
    fontSize = size.sp,
    fontFamily = MonoFont,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    letterSpacing = spacing.sp,
)
