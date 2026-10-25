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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arlodev.onlinenow.AppInfo
import com.arlodev.onlinenow.LauncherState
import com.arlodev.onlinenow.NotificationState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(state: LauncherState) {
    val p = LocalPalette.current
    val notified by NotificationState.packages.collectAsState()
    var dragTotal by remember { mutableFloatStateOf(0f) }

    val byPackage = remember(state.apps) { state.apps.associateBy { it.packageName } }
    val favoriteApps = state.favorites.mapNotNull { byPackage[it] }
    val dockApps = state.dock.mapNotNull { byPackage[it] }

    Box(
        Modifier
            .fillMaxSize()
            .background(p.background.copy(alpha = 0.35f))
            // swipe up anywhere -> open app drawer
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = { if (dragTotal < -100f) state.drawerOpen = true },
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
                .padding(horizontal = 32.dp)
        ) {
            Spacer(Modifier.height(48.dp))

            ClockBlock(state)

            Spacer(Modifier.weight(1f))

            // favorites: 1 column x 5 rows
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (favoriteApps.isEmpty()) {
                    BasicText(
                        "Swipe up, long-press an app, then tap \"Add to home\".",
                        style = TextStyle(
                            color = p.muted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = LauncherFont,
                        ),
                    )
                }
                favoriteApps.forEach { app ->
                    FavoriteRow(app, state, app.packageName in notified)
                }
            }

            Spacer(Modifier.height(32.dp))

            // dock
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dockApps.forEach { app ->
                    AppIconButton(app, state, 52.dp, app.packageName in notified)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FavoriteRow(app: AppInfo, state: LauncherState, hasNotification: Boolean) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .pointerInput(app.packageName) {
                detectTapGestures(
                    onTap = { state.launch(app.packageName) },
                    onLongPress = { state.menuApp = app },
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            app.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = p.text,
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
                fontFamily = LauncherFont,
            ),
            modifier = Modifier.weight(1f, fill = false),
        )
        if (hasNotification) {
            Box(
                Modifier
                    .padding(start = 12.dp)
                    .size(8.dp)
                    .background(p.accent, CircleShape)
            )
        }
    }
}

@Composable
private fun ClockBlock(state: LauncherState) {
    val p = LocalPalette.current
    val context = LocalContext.current

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    val is24 = DateFormat.is24HourFormat(context)
    val timeText = now.format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm"))
    val dateText = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

    Column(
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { state.start(Intent(AlarmClock.ACTION_SHOW_ALARMS)) },
                onLongPress = { state.settingsOpen = true },
            )
        }
    ) {
        BasicText(
            timeText,
            style = TextStyle(
                color = p.text,
                fontSize = 84.sp,
                fontWeight = FontWeight.ExtraLight,
                fontFamily = LauncherFont,
                letterSpacing = (-2).sp,
            ),
        )
        BasicText(
            dateText,
            style = TextStyle(
                color = p.muted,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                fontFamily = LauncherFont,
            ),
        )
    }
}
