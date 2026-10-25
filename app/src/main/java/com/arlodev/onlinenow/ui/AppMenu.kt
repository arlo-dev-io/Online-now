package com.arlodev.onlinenow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.arlodev.onlinenow.AppInfo
import com.arlodev.onlinenow.LauncherState

@Composable
fun AppMenu(app: AppInfo, state: LauncherState) {
    val p = LocalPalette.current
    val pkg = app.packageName
    val isFavorite = pkg in state.favorites
    val isInDock = pkg in state.dock
    val isHidden = pkg in state.hidden

    fun close() {
        state.menuApp = null
    }

    Dialog(onDismissRequest = { close() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(p.surface)
                .padding(vertical = 14.dp)
        ) {
            BasicText(
                app.label,
                style = TextStyle(
                    color = p.muted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = LauncherFont,
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            MenuItem(if (isFavorite) "Remove from home" else "Add to home") {
                state.toggleFavorite(pkg); close()
            }
            MenuItem(if (isInDock) "Remove from dock" else "Add to dock") {
                state.toggleDock(pkg); close()
            }
            MenuItem(if (isHidden) "Unhide from drawer" else "Hide from drawer") {
                state.toggleHidden(pkg); close()
            }
            MenuItem("App info") {
                state.openAppInfo(pkg); close()
            }
            MenuItem("Uninstall") {
                state.uninstall(pkg); close()
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    BasicText(
        text,
        style = TextStyle(
            color = p.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Light,
            fontFamily = LauncherFont,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
    )
}
