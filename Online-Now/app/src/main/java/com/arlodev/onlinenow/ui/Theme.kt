package com.arlodev.onlinenow.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

enum class ThemeMode { System, Light, Dark }

data class Palette(
    val background: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val dark: Boolean,
)

val AccentChoices = listOf(
    Color(0xFFFF7A59), // coral (default)
    Color(0xFF6EE7B7), // mint
    Color(0xFF7CC4FF), // sky
    Color(0xFFB79CFF), // lilac
    Color(0xFFFFD966), // lemon
    Color(0xFFE5E5E5), // mono
)

val DefaultAccent: Color = AccentChoices.first()

fun paletteFor(dark: Boolean, accent: Color): Palette =
    if (dark) {
        Palette(
            background = Color(0xFF0B0B0C),
            surface = Color(0xFF1B1B1D),
            text = Color(0xFFF2F2F2),
            muted = Color(0xFF8E8E93),
            accent = accent,
            onAccent = Color(0xFF0B0B0C),
            dark = true,
        )
    } else {
        Palette(
            background = Color(0xFFF6F5F2),
            surface = Color(0xFFFFFFFF),
            text = Color(0xFF111111),
            muted = Color(0xFF77777B),
            accent = accent,
            onAccent = Color(0xFF0B0B0C),
            dark = false,
        )
    }

val LocalPalette = staticCompositionLocalOf { paletteFor(true, DefaultAccent) }

/** Light-weight system sans: clean and minimal, no font files needed. */
val LauncherFont: FontFamily = FontFamily.SansSerif
