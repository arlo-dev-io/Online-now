package com.arlodev.onlinenow.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arlodev.onlinenow.AppInfo
import com.arlodev.onlinenow.LauncherState

/** Tap and long-press with a small "press in" animation. */
@Composable
fun Modifier.pressable(onTap: () -> Unit, onLongPress: (() -> Unit)? = null): Modifier {
    val tap by rememberUpdatedState(onTap)
    val longTap by rememberUpdatedState(onLongPress)
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f)
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                },
                onLongPress = { longTap?.invoke() },
                onTap = { tap() },
            )
        }
}

/** Rounded dark surface with a thin border: the base of every widget. */
@Composable
fun DotCard(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .clip(shape)
            .background(p.card)
            .border(1.dp, p.border, shape),
        content = content,
    )
}

/** Small rounded button. [selected] fills it, used for options and the main action. */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(50)
    BasicText(
        text.uppercase(),
        style = mono(11f, if (selected) p.background else p.text, bold = selected, spacing = 1.2f),
        modifier = modifier
            .pressable(onClick)
            .clip(shape)
            .background(if (selected) p.text else p.card)
            .border(1.dp, if (selected) p.text else p.border, shape)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
fun DotToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    val knobOffset by animateDpAsState(if (checked) 18.dp else 0.dp)
    Box(
        Modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(RoundedCornerShape(50))
            .background(if (checked) p.accent else p.dotOff)
            .pointerInput(checked, onChange) {
                detectTapGestures(onTap = { onChange(!checked) })
            }
            .padding(3.dp)
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(20.dp)
                .background(if (checked) p.onAccent else p.muted, CircleShape)
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    BasicText(text, style = mono(10.5f, p.muted, spacing = 2.4f), modifier = modifier)
}

/** One app in a grid: round icon with its name underneath. Tap opens it, long-press opens the menu. */
@Composable
fun AppTile(
    app: AppInfo,
    state: LauncherState,
    hasNotification: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 64.dp,
) {
    val p = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    Column(
        modifier.pressable(
            onTap = { state.launch(app.packageName) },
            onLongPress = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                state.menuApp = app
            },
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(app, state, iconSize, hasNotification)
        Spacer(Modifier.height(8.dp))
        BasicText(
            app.label.uppercase(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = mono(10f * state.textScale, p.muted, spacing = 0.6f).copy(textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
        )
    }
}

/** One app in the list layout: icon on the left, name on the right. */
@Composable
fun AppRow(
    app: AppInfo,
    state: LauncherState,
    hasNotification: Boolean,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    Row(
        modifier
            .pressable(
                onTap = { state.launch(app.packageName) },
                onLongPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.menuApp = app
                },
            )
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app, state, 46.dp, hasNotification)
        Spacer(Modifier.width(16.dp))
        BasicText(
            app.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = mono(14f * state.textScale, p.text, spacing = 0.3f),
            modifier = Modifier.weight(1f),
        )
    }
}
