package com.arlodev.onlinenow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.arlodev.onlinenow.AppInfo
import com.arlodev.onlinenow.LauncherState

@Composable
fun AppMenu(app: AppInfo, state: LauncherState) {
    val p = LocalPalette.current
    val pkg = app.packageName
    val shape = RoundedCornerShape(28.dp)

    val homeIndex = state.homeApps.indexOf(pkg)
    val onHome = homeIndex >= 0
    val pinned = pkg in state.drawerPinned
    val hidden = pkg in state.hidden

    fun close() {
        state.menuApp = null
    }

    Dialog(onDismissRequest = { close() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(p.card)
                .border(1.dp, p.border, shape)
                .padding(vertical = 10.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(app, state, 44.dp, hasNotification = false)
                Spacer(Modifier.width(14.dp))
                BasicText(
                    app.label.uppercase(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = mono(13f, p.text, bold = true, spacing = 1.2f),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))

            MenuItem(if (onHome) "Remove from home" else "Add to home") {
                state.toggleHome(pkg); close()
            }
            if (onHome && homeIndex > 0) {
                MenuItem("Move earlier") { state.moveHome(pkg, -1); close() }
            }
            if (onHome && homeIndex < state.homeApps.size - 1) {
                MenuItem("Move later") { state.moveHome(pkg, 1); close() }
            }
            MenuItem(if (pinned) "Unpin from drawer" else "Pin in drawer") {
                state.toggleDrawerPin(pkg); close()
            }
            MenuItem(if (hidden) "Unhide" else "Hide from drawer") {
                state.toggleHidden(pkg); close()
            }
            MenuItem("App info") {
                state.openAppInfo(pkg); close()
            }
            MenuItem("Uninstall", color = p.danger) {
                state.uninstall(pkg); close()
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, color: Color = LocalPalette.current.text, onClick: () -> Unit) {
    BasicText(
        text.uppercase(),
        style = mono(12f, color, spacing = 1.4f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 14.dp),
    )
}
