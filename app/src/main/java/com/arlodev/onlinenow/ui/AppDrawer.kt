package com.arlodev.onlinenow.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.arlodev.onlinenow.LauncherState
import com.arlodev.onlinenow.NotificationState

@Composable
fun AppDrawer(state: LauncherState) {
    val p = LocalPalette.current
    val notified by NotificationState.packages.collectAsState()
    val gridState = rememberLazyGridState()

    val visibleApps = remember(state.apps, state.hidden) {
        state.apps.filter { it.packageName !in state.hidden }
    }

    BackHandler { state.drawerOpen = false }

    var dragTotal by remember { mutableFloatStateOf(0f) }
    var startedAtTop by remember { mutableStateOf(true) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(p.background.copy(alpha = 0.94f))
            // swipe down from the top of the list -> close drawer
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        dragTotal = 0f
                        startedAtTop = gridState.firstVisibleItemIndex == 0 &&
                            gridState.firstVisibleItemScrollOffset == 0
                    },
                    onDragEnd = {
                        if (startedAtTop && dragTotal > 220f) state.drawerOpen = false
                    },
                    onDragCancel = { dragTotal = 0f },
                ) { _, dy -> dragTotal += dy }
            }
            .systemBarsPadding(),
    ) {
        items(visibleApps, key = { it.packageName }) { app ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppIconButton(app, state, 56.dp, app.packageName in notified)
            }
        }
    }
}
