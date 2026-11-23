package com.arlodev.onlinenow.ui

import android.content.Intent
import android.provider.AlarmClock
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arlodev.onlinenow.AppInfo
import com.arlodev.onlinenow.LauncherState
import com.arlodev.onlinenow.NotificationState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val GRID_COLUMNS = 4

@Composable
fun HomeScreen(state: LauncherState) {
    val p = LocalPalette.current
    val notified by NotificationState.packages.collectAsState()
    var dragTotal by remember { mutableFloatStateOf(0f) }

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    val byPackage = remember(state.apps) { state.apps.associateBy { it.packageName } }
    val homeApps = state.homeApps.mapNotNull { byPackage[it] }

    Box(
        Modifier
            .fillMaxSize()
            .background(p.background.copy(alpha = state.homeAlpha))
            // swipe up anywhere -> open app drawer
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = { if (dragTotal < -90f) state.drawerOpen = true },
                    onDragCancel = { dragTotal = 0f },
                ) { _, dy -> dragTotal += dy }
            }
            // long-press empty space -> settings
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { state.settingsOpen = true })
            }
            .systemBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))

            if (state.showPills) StatusPills()

            Spacer(Modifier.weight(0.9f))

            ClockBlock(state, now)

            Spacer(Modifier.weight(0.7f))

            if (state.showCalendar) {
                CalendarCard(state, now.toLocalDate())
                Spacer(Modifier.height(12.dp))
            }

            if (state.showMedia || state.showPlay) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(148.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.showMedia) MediaCard(state, Modifier.weight(1f).fillMaxHeight())
                    if (state.showPlay) PlayCard(Modifier.weight(1f).fillMaxHeight())
                }
            }

            Spacer(Modifier.weight(1f))

            HomeGrid(homeApps, state, notified)

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ClockBlock(state: LauncherState, now: LocalDateTime) {
    val p = LocalPalette.current
    val context = LocalContext.current

    val is24 = DateFormat.is24HourFormat(context)
    val timeText = now.format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm", Locale.ENGLISH))
    val suffix = if (is24) null else if (now.hour < 12) "AM" else "PM"

    Box(
        Modifier.pressable(
            onTap = { state.start(Intent(AlarmClock.ACTION_SHOW_ALARMS)) },
            onLongPress = { state.settingsOpen = true },
        )
    ) {
        DotText(timeText, pitch = 9.dp, color = p.text, spacing = 2)
        if (suffix != null) {
            DotText(
                suffix,
                pitch = 3.dp,
                color = p.muted,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 40.dp),
            )
        }
    }
}

@Composable
private fun HomeGrid(apps: List<AppInfo>, state: LauncherState, notified: Set<String>) {
    val p = LocalPalette.current

    if (apps.isEmpty()) {
        DotCard(Modifier.fillMaxWidth()) {
            BasicText(
                "SWIPE UP.\nLONG-PRESS AN APP.\nTAP \"ADD TO HOME\".",
                style = mono(11f, p.muted, spacing = 1.6f).copy(textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        for (rowApps in apps.chunked(GRID_COLUMNS)) {
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until GRID_COLUMNS) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        val app = rowApps.getOrNull(i)
                        if (app != null) {
                            AppTile(app, state, app.packageName in notified, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}
