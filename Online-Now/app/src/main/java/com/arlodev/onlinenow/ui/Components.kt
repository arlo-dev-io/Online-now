package com.arlodev.onlinenow.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.arlodev.onlinenow.AppInfo
import com.arlodev.onlinenow.LauncherState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val MonoFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
fun AppIcon(app: AppInfo, size: Dp, mono: Boolean) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }

    val bitmap by produceState<ImageBitmap?>(initialValue = null, app.packageName, px) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                context.packageManager
                    .getApplicationIcon(app.packageName)
                    .toBitmap(px, px)
                    .asImageBitmap()
            }.getOrNull()
        }
    }

    Box(Modifier.size(size).clip(CircleShape)) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
                colorFilter = if (mono) MonoFilter else null,
            )
        }
    }
}

/** Tap = open app. Long-press = menu. Shows an accent dot when the app has notifications. */
@Composable
fun AppIconButton(
    app: AppInfo,
    state: LauncherState,
    size: Dp,
    hasNotification: Boolean,
) {
    val p = LocalPalette.current
    Box(
        Modifier
            .pointerInput(app.packageName) {
                detectTapGestures(
                    onTap = { state.launch(app.packageName) },
                    onLongPress = { state.menuApp = app },
                )
            }
            .padding(8.dp)
    ) {
        AppIcon(app, size, state.monoIcons)
        if (hasNotification) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(10.dp)
                    .background(p.accent, CircleShape)
            )
        }
    }
}
