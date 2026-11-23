package com.arlodev.onlinenow.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.arlodev.onlinenow.LauncherState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(state: LauncherState) {
    val p = LocalPalette.current
    BackHandler { state.settingsOpen = false }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.background.copy(alpha = 0.98f))
            .systemBarsPadding()
            // swallow touches so nothing behind the settings reacts
            .pointerInput(Unit) { detectTapGestures { } }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DotText("SETTINGS", pitch = 3.dp, color = p.text)
            Pill("Done", selected = true) { state.settingsOpen = false }
        }

        Group("Appearance") {
            Option("Theme") {
                ThemeMode.entries.forEach { mode ->
                    Pill(mode.name, selected = state.themeMode == mode) { state.updateTheme(mode) }
                }
            }
            Option("Background") {
                BackdropMode.entries.forEach { mode ->
                    Pill(mode.name, selected = state.backdrop == mode) { state.updateBackdrop(mode) }
                }
            }
            Option("Icons") {
                Pill("Dots", selected = state.iconStyle == IconStyle.Dots) { state.updateIconStyle(IconStyle.Dots) }
                Pill("Original", selected = state.iconStyle == IconStyle.Original) { state.updateIconStyle(IconStyle.Original) }
            }
            Option("Accent color") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentChoices.forEach { color ->
                        Swatch(color, selected = state.accent == color) { state.updateAccent(color) }
                    }
                }
            }
            Option("Label size") {
                Pill("A-") { state.changeTextScale(-0.1f) }
                Label("${(state.textScale * 100).roundToInt()}%", p.text)
                Pill("A+") { state.changeTextScale(0.1f) }
                Pill("Reset") { state.resetTextScale() }
            }
        }

        Group("Home screen") {
            Switch("Status pills", "Connection and battery", state.showPills) { state.updateShowPills(it) }
            Switch("Calendar", "Week around today", state.showCalendar) { state.updateShowCalendar(it) }
            Switch("Music", "What is playing, play / pause", state.showMedia) { state.updateShowMedia(it) }
            Switch("Tap to play", "Ask the dots a question", state.showPlay) { state.updateShowPlay(it) }
        }

        Group("App drawer") {
            Option("Layout") {
                Pill("Grid", selected = state.drawerLayout == DrawerLayout.Grid) { state.updateDrawerLayout(DrawerLayout.Grid) }
                Pill("List", selected = state.drawerLayout == DrawerLayout.List) { state.updateDrawerLayout(DrawerLayout.List) }
            }
            Switch("Open keyboard", "Start typing as soon as it opens", state.autoKeyboard) { state.updateAutoKeyboard(it) }
        }

        Group("Access") {
            ActionRow(
                title = "Notification access",
                subtitle = if (state.notificationAccess) {
                    "ON  -  notification dots and music card work"
                } else {
                    "OFF  -  needed for notification dots and the music card"
                },
                action = if (state.notificationAccess) "Manage" else "Allow",
            ) {
                state.start(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            Hint("If it is greyed out: Settings > Apps > Online Now > three dots > Allow restricted settings.")
            ActionRow("Wallpaper", "Pick a new one", "Change") {
                state.start(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper"))
            }
            ActionRow("Default launcher", "Make Online Now your home app", "Open") {
                state.start(Intent(Settings.ACTION_HOME_SETTINGS))
            }
        }

        Spacer(Modifier.height(28.dp))
        BasicText(
            "ONLINE NOW  ${state.versionName}",
            style = mono(10f, p.muted, spacing = 2.4f),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** A titled card holding a few settings. */
@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(22.dp))
    SectionLabel(title.uppercase(), Modifier.padding(start = 6.dp, bottom = 10.dp))
    DotCard(Modifier.fillMaxWidth(), radius = 24.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/** A small caption above a row of controls. */
@Composable
private fun Option(title: String, controls: @Composable () -> Unit) {
    val p = LocalPalette.current
    Column {
        BasicText(title.uppercase(), style = mono(10f, p.muted, spacing = 1.8f))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            controls()
        }
    }
}

@Composable
private fun Switch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = mono(13f, p.text, spacing = 0.3f))
            Spacer(Modifier.height(3.dp))
            BasicText(subtitle, style = mono(10f, p.muted, spacing = 0.2f))
        }
        DotToggle(checked, onChange)
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, action: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = mono(13f, p.text, spacing = 0.3f))
            Spacer(Modifier.height(3.dp))
            BasicText(subtitle, style = mono(10f, p.muted, spacing = 0.2f))
        }
        Spacer(Modifier.size(10.dp))
        Pill(action, onClick = onClick)
    }
}

@Composable
private fun Hint(text: String) {
    val p = LocalPalette.current
    BasicText(text, style = mono(10f, p.muted, spacing = 0.2f))
}

@Composable
private fun Label(text: String, color: Color) {
    BasicText(text, style = mono(13f, color, spacing = 0.5f))
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .size(32.dp)
            .background(color, CircleShape)
            .border(if (selected) 2.dp else 1.dp, if (selected) p.text else p.border, CircleShape)
            .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }
    )
}
