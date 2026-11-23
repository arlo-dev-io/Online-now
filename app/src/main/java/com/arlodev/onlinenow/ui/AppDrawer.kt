package com.arlodev.onlinenow.ui

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.arlodev.onlinenow.AppInfo
import com.arlodev.onlinenow.LauncherState
import com.arlodev.onlinenow.NotificationState
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val GRID_COLUMNS = 4

/**
 * App drawer.
 *  - Search box on top, filters as you type (Enter opens the best match).
 *  - Pinned apps first, then everything A-Z. A-Z rail on the right edge.
 *  - "Hidden" button (or swipe left) opens the hidden apps page, "Back" (or swipe right) returns.
 *  - Swipe down while at the top closes the drawer.
 */
@Composable
fun AppDrawer(state: LauncherState) {
    val p = LocalPalette.current
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val notified by NotificationState.packages.collectAsState()

    var showHidden by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var touchedLetter by remember { mutableStateOf<Char?>(null) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    val grid = state.drawerLayout == DrawerLayout.Grid
    val columns = if (grid) GRID_COLUMNS else 1

    val byPackage = remember(state.apps) { state.apps.associateBy { it.packageName } }
    val visibleApps = remember(state.apps, state.hidden) {
        state.apps.filter { it.packageName !in state.hidden }
    }
    val hiddenApps = remember(state.apps, state.hidden) {
        state.apps.filter { it.packageName in state.hidden }
    }
    val pinnedApps = remember(byPackage, state.drawerPinned, state.hidden) {
        state.drawerPinned.mapNotNull { byPackage[it] }.filter { it.packageName !in state.hidden }
    }
    val model = remember(visibleApps, hiddenApps, pinnedApps, query, showHidden, columns) {
        if (showHidden) {
            DrawerModelBuilder.build(hiddenApps, emptyList(), query, columns)
        } else {
            DrawerModelBuilder.build(visibleApps, pinnedApps, query, columns)
        }
    }

    val railLetters = remember(model) { model.letterIndex.keys.sorted() }
    val showRail = query.isEmpty() && railLetters.size >= 4

    val activeLetter by remember(model) {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            var best: Char? = null
            var bestIndex = -1
            for ((letter, index) in model.letterIndex) {
                if (index <= first && index > bestIndex) {
                    best = letter
                    bestIndex = index
                }
            }
            best
        }
    }

    BackHandler {
        when {
            query.isNotEmpty() -> query = ""
            showHidden -> showHidden = false
            else -> state.drawerOpen = false
        }
    }

    LaunchedEffect(showHidden, query) { listState.scrollToItem(0) }
    LaunchedEffect(state.drawerOpen) {
        if (!state.drawerOpen) hideKeyboard(view)
    }
    LaunchedEffect(Unit) {
        if (state.autoKeyboard) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    val railWidthPx = with(density) { 40.dp.toPx() }
    val swipeThresholdPx = with(density) { 96.dp.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .background(p.background.copy(alpha = state.drawerAlpha))
            // Watches every touch BEFORE the list sees it (without blocking it), so swipes are
            // detected even while the list is handling the drag.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    // touches on the A-Z rail belong to the rail
                    if (down.position.x > size.width - railWidthPx) return@awaitEachGesture

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
                        if (totalX < -swipeThresholdPx && !showHidden) showHidden = true
                        else if (totalX > swipeThresholdPx && showHidden) showHidden = false
                    } else if (totalY > swipeThresholdPx && startedAtTop) {
                        state.drawerOpen = false
                    }
                }
            }
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            // ---- top bar ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onGo = {
                        if (query.isNotBlank()) model.firstApp?.let { state.launch(it.packageName) }
                    },
                    focusRequester = focusRequester,
                    modifier = Modifier.weight(1f),
                )
                val hiddenLabel = if (hiddenApps.isEmpty()) "Hidden" else "Hidden ${hiddenApps.size}"
                Pill(if (showHidden) "Back" else hiddenLabel) { showHidden = !showHidden }
            }

            if (showHidden) HiddenBanner(hiddenApps.size)

            // ---- list ----
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (model.items.isEmpty()) {
                    EmptyDrawer(showHidden = showHidden && query.isEmpty())
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = if (showRail) 34.dp else 20.dp,
                            top = 4.dp,
                            bottom = 28.dp,
                        ),
                    ) {
                        items(model.items, key = { it.key }) { item ->
                            when (item) {
                                is DrawerItem.Header -> HeaderRow(item.text, letter = columns == 1 && item.text.length == 1)
                                is DrawerItem.Apps -> {
                                    if (grid) {
                                        GridRow(item.apps, state, notified)
                                    } else {
                                        AppRow(item.apps.first(), state, item.apps.first().packageName in notified)
                                    }
                                }
                            }
                        }
                    }
                }

                if (showRail) {
                    AlphabetRail(
                        letters = railLetters,
                        active = activeLetter,
                        onSelect = { letter ->
                            touchedLetter = letter
                            val target = model.letterIndex[letter]
                            if (target != null) scope.launch { listState.scrollToItem(target) }
                        },
                        onRelease = { touchedLetter = null },
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }

                touchedLetter?.let { letter ->
                    val shape = RoundedCornerShape(24.dp)
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(96.dp)
                            .clip(shape)
                            .background(p.card)
                            .border(1.dp, p.border, shape),
                        contentAlignment = Alignment.Center,
                    ) {
                        DotText(letter.toString(), pitch = 6.dp, color = p.accent)
                    }
                }
            }
        }
    }
}

private fun hideKeyboard(view: View) {
    val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(view.windowToken, 0)
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onGo: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(p.card)
            .border(1.dp, p.border, shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(16.dp)) {
            val stroke = 1.5f.dp.toPx()
            drawCircle(p.muted, radius = 5.5f.dp.toPx(), center = Offset(6.5f.dp.toPx(), 6.5f.dp.toPx()), style = Stroke(stroke))
            drawLine(
                p.muted,
                start = Offset(10.5f.dp.toPx(), 10.5f.dp.toPx()),
                end = Offset(14.5f.dp.toPx(), 14.5f.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                BasicText("SEARCH APPS", style = mono(12f, p.muted, spacing = 1.6f))
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = mono(14f, p.text, spacing = 0.4f),
                cursorBrush = SolidColor(p.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onGo() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
        if (query.isNotEmpty()) {
            BasicText(
                "CLEAR",
                style = mono(10f, p.muted, spacing = 1.4f),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .pointerInput(Unit) { detectTapGestures(onTap = { onQueryChange("") }) },
            )
        }
    }
}

@Composable
private fun HiddenBanner(count: Int) {
    val p = LocalPalette.current
    DotCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        radius = 24.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DotText(count.coerceAtMost(99).toString(), pitch = 4.dp, color = p.accent)
            Spacer(Modifier.width(16.dp))
            Column {
                BasicText("HIDDEN APPS", style = mono(12f, p.text, bold = true, spacing = 2f))
                Spacer(Modifier.height(4.dp))
                BasicText("Long-press an app to unhide it.", style = mono(10.5f, p.muted, spacing = 0.2f))
            }
        }
    }
}

@Composable
private fun EmptyDrawer(showHidden: Boolean) {
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DotText(if (showHidden) "0" else "?", pitch = 7.dp, color = p.dotOff)
        Spacer(Modifier.height(18.dp))
        BasicText(
            if (showHidden) "NOTHING HIDDEN" else "NO MATCH",
            style = mono(11f, p.muted, spacing = 3f),
        )
        if (showHidden) {
            Spacer(Modifier.height(8.dp))
            BasicText(
                "Long-press any app, then \"Hide from drawer\".",
                style = mono(10f, p.muted, spacing = 0.2f),
            )
        }
    }
}

@Composable
private fun HeaderRow(text: String, letter: Boolean) {
    val p = LocalPalette.current
    if (letter) {
        DotText(text, pitch = 3.dp, color = p.muted, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
    } else {
        SectionLabel(text, Modifier.padding(top = 16.dp, bottom = 10.dp))
    }
}

@Composable
private fun GridRow(
    apps: List<AppInfo>,
    state: LauncherState,
    notified: Set<String>,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        for (i in 0 until GRID_COLUMNS) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                val app = apps.getOrNull(i)
                if (app != null) {
                    AppTile(app, state, app.packageName in notified, Modifier.fillMaxWidth(), iconSize = 60.dp)
                }
            }
        }
    }
}

/** A-Z strip on the right edge. Touch or drag on it to jump through the list. */
@Composable
private fun AlphabetRail(
    letters: List<Char>,
    active: Char?,
    onSelect: (Char) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val currentLetters by rememberUpdatedState(letters)
    val select by rememberUpdatedState(onSelect)
    val release by rememberUpdatedState(onRelease)

    Column(
        modifier
            .fillMaxHeight()
            .width(30.dp)
            .padding(vertical = 8.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()

                    fun pick(y: Float) {
                        val list = currentLetters
                        if (list.isEmpty() || size.height <= 0) return
                        val index = (y / size.height * list.size).toInt().coerceIn(0, list.size - 1)
                        select(list[index])
                    }

                    down.consume()
                    pick(down.position.y)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        pick(change.position.y)
                    }
                    release()
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (letter in letters) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    letter.toString(),
                    style = mono(9.5f, if (letter == active) p.accent else p.muted, bold = letter == active, spacing = 0f),
                )
            }
        }
    }
}
