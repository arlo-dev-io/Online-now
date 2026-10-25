package com.arlodev.onlinenow.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arlodev.onlinenow.LauncherState

@Composable
fun SettingsScreen(state: LauncherState) {
    val p = LocalPalette.current
    BackHandler { state.settingsOpen = false }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.background.copy(alpha = 0.97f))
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Label("Settings", size = 34, color = p.text)
            Pill("Done", selected = true) { state.settingsOpen = false }
        }

        Section("Theme") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeMode.entries.forEach { mode ->
                    Pill(mode.name, state.themeMode == mode) { state.setTheme(mode) }
                }
            }
        }

        Section("Accent color") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentChoices.forEach { color ->
                    Swatch(color, selected = state.accent == color) { state.updateAccent(color) }
                }
            }
        }

        Section("Icons") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Monochrome", state.monoIcons) { state.updateMonoIcons(true) }
                Pill("Original", !state.monoIcons) { state.updateMonoIcons(false) }
            }
        }

        Section("Notification dots") {
            Pill("Allow notification access", selected = false) {
                state.start(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            Spacer(Modifier.height(8.dp))
            Label(
                "If it is greyed out: Settings > Apps > Online Now > three dots > Allow restricted settings.",
                size = 13,
                color = p.muted,
            )
        }

        Section("Wallpaper") {
            Pill("Change wallpaper", selected = false) {
                state.start(
                    Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper")
                )
            }
        }

        Section("Hidden apps (tap to hide or show in drawer)") {
            state.apps.forEach { app ->
                val isHidden = app.packageName in state.hidden
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { state.toggleHidden(app.packageName) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Label(app.label, size = 18, color = if (isHidden) p.muted else p.text)
                    Label(if (isHidden) "Hidden" else "Visible", size = 14, color = if (isHidden) p.accent else p.muted)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Spacer(Modifier.height(28.dp))
    Label(title.uppercase(), size = 12, color = p.muted)
    Spacer(Modifier.height(12.dp))
    Column(content = content)
}

@Composable
private fun Label(text: String, size: Int, color: Color) {
    BasicText(
        text,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            fontWeight = FontWeight.Light,
            fontFamily = LauncherFont,
        ),
    )
}

@Composable
private fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    BasicText(
        text,
        style = TextStyle(
            color = if (selected) p.onAccent else p.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = LauncherFont,
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) p.accent else p.text.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .then(if (selected) Modifier.border(2.dp, p.text, CircleShape) else Modifier)
            .clickable { onClick() }
    ) {}
}
