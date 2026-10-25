package com.arlodev.onlinenow.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arlodev.onlinenow.LauncherState
import com.arlodev.onlinenow.NotificationState
import kotlin.math.abs

/**
 * Drawer = words only.
 *  - Drawer favorites (their own list, max 10) are pinned at the top, then every other app A-Z.
 *  - Swipe DOWN (when the list is at the top) closes the drawer.
 *  - Swipe LEFT opens the hidden apps page. Swipe RIGHT goes back.
 */
@Composable
fun AppDrawer(state: LauncherState) {
    val p = LocalPalette.current
    val notified by NotificationState.packages.collectAsState()

    var showHidden by remember { mutableStateOf(false) }
    val mainListState = rememberLazyListState()
    val hiddenListState = rememberLazyListState()

    BackHandler {
        if (showHidden) showHidden = false else state.drawerOpen = false
    }

    val byPackage = remember(state.apps) { state.apps.associateBy { it.packageName } }
    val favorites = remember(byPackage, state.drawerFavorites, state.hidden) {
        state.drawerFavorites.mapNotNull { byPackage[it] }.filter { it.packageName !in state.hidden }
    }
    val others = remember(state.apps, state.drawerFavorites, state.hidden) {
        state.apps.filter { it.packageName !in state.hidden && it.packageName !in state.drawerFavorites }
    }
    val hiddenApps = remember(state.apps, state.hidden) {
        state.apps.filter { it.packageName in state.hidden }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(p.background.copy(alpha = 0.94f))
            // Watches every touch BEFORE the list sees it (without blocking it),
            // so swipes are detected even while the list is handling the drag.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val listState = if (showHidden) hiddenListState else mainListState
                    val startedAtTop = listState.firstVisibleItemIndex == 0 &&
                        listState.firstVisibleItemScrollOffset == 0

                    var totalX = 0f
                    var totalY = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val delta = change.positionChange()
                        totalX += delta.x
                        totalY += delta.y
                    }

                    if (abs(totalX) > abs(totalY)) {
                        if (totalX < -140f && !showHidden) showHidden = true
                        else if (totalX > 140f && showHidden) showHidden = false
                    } else if (totalY > 140f && startedAtTop) {
                        state.drawerOpen = false
                    }
                }
            }
            .systemBarsPadding()
    ) {
        if (showHidden) {
            LazyColumn(
                state = hiddenListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            ) {
                item(key = "hidden_header") {
                    Hint("HIDDEN APPS  -  swipe right to go back, long-press to unhide")
                }
                if (hiddenApps.isEmpty()) {
                    item(key = "hidden_empty") {
                        Hint("Nothing hidden yet. Long-press any app and choose \"Hide from drawer\".")
                    }
                }
                items(hiddenApps, key = { "hid_" + it.packageName }) { app ->
                    AppWord(
                        app = app,
                        state = state,
                        hasNotification = app.packageName in notified,
                        baseSize = 24f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            LazyColumn(
                state = mainListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            ) {
                items(favorites, key = { "fav_" + it.packageName }) { app ->
                    AppWord(
                        app = app,
                        state = state,
                        hasNotification = app.packageName in notified,
                        baseSize = 26f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (favorites.isNotEmpty()) {
                    item(key = "divider") {
                        Box(
                            Modifier
                                .padding(vertical = 14.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(p.muted.copy(alpha = 0.35f))
                        )
                    }
                }

                items(others, key = { it.packageName }) { app ->
                    AppWord(
                        app = app,
                        state = state,
                        hasNotification = app.packageName in notified,
                        baseSize = 24f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item(key = "footer") {
                    Box(Modifier.padding(top = 20.dp)) {
                        Hint("swipe left for hidden apps")
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    val p = LocalPalette.current
    BasicText(
        text,
        style = TextStyle(
            color = p.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Light,
            fontFamily = LauncherFont,
        ),
        modifier = Modifier.padding(vertical = 8.dp),
    )
}
