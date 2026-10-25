package com.arlodev.onlinenow.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.arlodev.onlinenow.LauncherState
import com.arlodev.onlinenow.NotificationState

/**
 * Drawer = words only. Favorites are pinned at the top, then every other app A-Z.
 * Pull down while the list is at the top (or press back/home) to close.
 */
@Composable
fun AppDrawer(state: LauncherState) {
    val p = LocalPalette.current
    val notified by NotificationState.packages.collectAsState()

    BackHandler { state.drawerOpen = false }

    val byPackage = remember(state.apps) { state.apps.associateBy { it.packageName } }
    val favorites = remember(byPackage, state.favorites, state.hidden) {
        state.favorites.mapNotNull { byPackage[it] }.filter { it.packageName !in state.hidden }
    }
    val others = remember(state.apps, state.favorites, state.hidden) {
        state.apps.filter { it.packageName !in state.hidden && it.packageName !in state.favorites }
    }

    // swipe down at the top of the list -> close
    val closeOnPull = remember(state) {
        object : NestedScrollConnection {
            var pulled = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (consumed.y != 0f) pulled = 0f
                else if (available.y > 0f) pulled += available.y
                else if (available.y < 0f) pulled = 0f

                if (pulled > 220f) {
                    pulled = 0f
                    state.drawerOpen = false
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background.copy(alpha = 0.94f))
            .nestedScroll(closeOnPull)
            .systemBarsPadding(),
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
    }
}
