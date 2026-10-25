package com.arlodev.onlinenow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arlodev.onlinenow.AppInfo
import com.arlodev.onlinenow.LauncherState

/**
 * An app shown as plain text.
 * Tap = open app. Long-press = menu. A small accent dot shows when it has notifications.
 * Text size = baseSize x the user's text-size setting.
 */
@Composable
fun AppWord(
    app: AppInfo,
    state: LauncherState,
    hasNotification: Boolean,
    baseSize: Float,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
) {
    val p = LocalPalette.current
    Row(
        modifier
            .pointerInput(app.packageName) {
                detectTapGestures(
                    onTap = { state.launch(app.packageName) },
                    onLongPress = { state.menuApp = app },
                )
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start,
    ) {
        BasicText(
            app.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = p.text,
                fontSize = (baseSize * state.textScale).sp,
                fontWeight = FontWeight.Light,
                fontFamily = LauncherFont,
            ),
            modifier = Modifier.weight(1f, fill = false),
        )
        if (hasNotification) {
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .size(7.dp)
                    .background(p.accent, CircleShape)
            )
        }
    }
}
