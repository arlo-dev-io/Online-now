package com.arlodev.onlinenow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.arlodev.onlinenow.LauncherState

/**
 * Walks the user through the three things Android only allows from system screens.
 * Each button jumps straight to the right place; the status dots update when you come back.
 */
@Composable
fun SetupDialog(state: LauncherState) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(28.dp)

    Dialog(onDismissRequest = { state.dismissSetup() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(p.card)
                .border(1.dp, p.border, shape)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DotText("SETUP", pitch = 4.dp, color = p.text)
            BasicText(
                "Android only lets you switch these on from system screens, so each button takes you straight there.",
                style = mono(10.5f, p.muted, spacing = 0.2f),
            )

            SetupStep(
                number = "1",
                title = "Default launcher",
                detail = "So the Home button opens Online Now.",
                done = state.isDefaultLauncher,
                action = "Allow",
            ) { state.requestDefaultLauncher() }

            SetupStep(
                number = "2",
                title = "Restricted settings",
                detail = "Needed once because the app was installed from a file. Tap the three dots (top right), then \"Allow restricted settings\".",
                done = null,
                action = "App info",
            ) { state.openOwnAppInfo() }

            SetupStep(
                number = "3",
                title = "Notification access",
                detail = "For the music card and notification dots. Turn on Online Now.",
                done = state.notificationAccess,
                action = "Allow",
            ) { state.openNotificationAccess() }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Pill("Done", selected = true) { state.dismissSetup() }
            }
        }
    }
}

/** [done] = null means "can't be checked" (shown as an empty ring). */
@Composable
private fun SetupStep(
    number: String,
    title: String,
    detail: String,
    done: Boolean?,
    action: String,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 3.dp)
                .size(10.dp)
                .background(if (done == true) p.accent else p.card, CircleShape)
                .border(1.dp, if (done == true) p.accent else p.muted, CircleShape)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            BasicText("$number  ${title.uppercase()}", style = mono(11.5f, p.text, bold = true, spacing = 1.2f))
            Spacer(Modifier.height(4.dp))
            BasicText(detail, style = mono(10f, p.muted, spacing = 0.2f))
            if (done == true) {
                Spacer(Modifier.height(6.dp))
                BasicText("ALLOWED", style = mono(10f, p.accent, bold = true, spacing = 2f))
            } else {
                Spacer(Modifier.height(10.dp))
                Pill(action, onClick = onClick)
            }
        }
    }
}
