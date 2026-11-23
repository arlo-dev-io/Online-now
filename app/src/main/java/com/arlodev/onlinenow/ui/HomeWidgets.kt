package com.arlodev.onlinenow.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.arlodev.onlinenow.LauncherState
import com.arlodev.onlinenow.MediaState
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// ---------------------------------------------------------------------------------------------
// Status pills: connection + battery
// ---------------------------------------------------------------------------------------------

private data class BatteryInfo(val level: Int, val charging: Boolean)

private enum class Link { Offline, WiFi, Mobile, Other }

@Composable
private fun rememberBattery(): BatteryInfo {
    val context = LocalContext.current
    var info by remember { mutableStateOf(BatteryInfo(100, false)) }
    DisposableEffect(context) {
        fun read(intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            if (level >= 0 && scale > 0) {
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                info = BatteryInfo(level * 100 / scale, charging)
            }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) = read(intent)
        }
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        read(sticky)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return info
}

@Composable
private fun rememberLink(): Link {
    val context = LocalContext.current
    var link by remember { mutableStateOf(Link.Offline) }
    DisposableEffect(context) {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return@DisposableEffect onDispose { }

        fun classify(caps: NetworkCapabilities?): Link = when {
            caps == null -> Link.Offline
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Link.WiFi
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Link.Mobile
            else -> Link.Other
        }

        link = classify(cm.getNetworkCapabilities(cm.activeNetwork))
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                link = classify(networkCapabilities)
            }

            override fun onLost(network: Network) {
                link = Link.Offline
            }
        }
        cm.registerDefaultNetworkCallback(callback)
        onDispose { cm.unregisterNetworkCallback(callback) }
    }
    return link
}

@Composable
fun StatusPills(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val link = rememberLink()
    val battery = rememberBattery()
    val shape = RoundedCornerShape(50)

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        // connection
        Row(
            Modifier
                .height(32.dp)
                .clip(shape)
                .background(p.card)
                .border(1.dp, p.border, shape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .background(if (link == Link.Offline) p.dotOff else p.accent, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            val label = when (link) {
                Link.Offline -> "OFFLINE"
                Link.WiFi -> "WI-FI"
                Link.Mobile -> "MOBILE"
                Link.Other -> "ONLINE"
            }
            BasicText(label, style = mono(11f, p.text, spacing = 1.4f))
        }

        // battery
        Row(
            Modifier
                .height(32.dp)
                .clip(shape)
                .background(p.card)
                .border(1.dp, p.border, shape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText("${battery.level}%", style = mono(12f, p.text, bold = true, spacing = 0.4f))
            Spacer(Modifier.width(8.dp))
            BatteryGlyph(battery)
        }
    }
}

@Composable
private fun BatteryGlyph(battery: BatteryInfo) {
    val p = LocalPalette.current
    val color = when {
        battery.charging -> p.accent
        battery.level <= 15 -> p.danger
        else -> p.text
    }
    Canvas(Modifier.size(width = 24.dp, height = 12.dp)) {
        val stroke = 1.5f.dp.toPx()
        val bodyWidth = size.width - 3f.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(bodyWidth - stroke, size.height - stroke),
            cornerRadius = CornerRadius(3f.dp.toPx()),
            style = Stroke(stroke),
        )
        val inner = (bodyWidth - stroke * 3f) * (battery.level / 100f)
        if (inner > 0f) {
            drawRoundRect(
                color = color,
                topLeft = Offset(stroke * 1.5f, stroke * 1.5f),
                size = Size(inner, size.height - stroke * 3f),
                cornerRadius = CornerRadius(1.5f.dp.toPx()),
            )
        }
        drawRoundRect(
            color = color,
            topLeft = Offset(bodyWidth + 0.5f.dp.toPx(), size.height * 0.3f),
            size = Size(2f.dp.toPx(), size.height * 0.4f),
            cornerRadius = CornerRadius(1f.dp.toPx()),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Calendar card: month + 7 days around today
// ---------------------------------------------------------------------------------------------

@Composable
fun CalendarCard(state: LauncherState, today: LocalDate, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val month = today.month
        .getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
        .uppercase()

    DotCard(
        modifier
            .fillMaxWidth()
            .height(100.dp)
            .pressable(onTap = {
                state.start(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR))
            }),
        radius = 28.dp,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(p.text, CircleShape))
                Spacer(Modifier.width(10.dp))
                BasicText("$month ${today.year}", style = mono(12f, p.text, bold = true, spacing = 3f))
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (offset in -3..3) {
                    DayCell(today.plusDays(offset.toLong()), isToday = offset == 0)
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, isToday: Boolean) {
    val p = LocalPalette.current
    val letter = date.dayOfWeek
        .getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault())
        .uppercase()
    Column(Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(letter, style = mono(9.5f, if (isToday) p.text else p.muted, bold = isToday, spacing = 0f))
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            val ringSize = if (isToday) 36.dp else 30.dp
            Box(
                Modifier
                    .size(ringSize)
                    .background(if (isToday) p.text else p.card, CircleShape)
                    .border(1.dp, if (isToday) p.text else p.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    date.dayOfMonth.toString(),
                    style = mono(12f, if (isToday) p.background else p.muted, bold = isToday, spacing = 0f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Music card: shows what is playing, play / pause button
// ---------------------------------------------------------------------------------------------

@Composable
fun MediaCard(state: LauncherState, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val info by MediaState.info.collectAsState()
    val current = info

    val title: String
    val subtitle: String
    when {
        current != null -> {
            title = current.title
            subtitle = current.artist
        }
        !state.notificationAccess -> {
            title = "Music"
            subtitle = "Enable access"
        }
        else -> {
            title = "Not playing"
            subtitle = "Open a player"
        }
    }
    val playing = current?.playing == true

    DotCard(modifier, radius = 28.dp) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .pressable(onTap = {
                        if (current != null) {
                            state.launch(current.packageName)
                        } else if (!state.notificationAccess) {
                            state.start(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    })
            ) {
                BasicText(
                    title.uppercase(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = mono(14f, if (current != null) p.text else p.muted, bold = true, spacing = 1f),
                )
                Spacer(Modifier.height(3.dp))
                BasicText(
                    subtitle.uppercase(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = mono(10.5f, p.muted, spacing = 1.2f),
                )
            }

            Spacer(Modifier.weight(1f))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                EqualizerDots(playing)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(46.dp)
                        .pressable(onTap = { MediaState.playPause(context) })
                        .background(p.text, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    PlayPauseGlyph(playing)
                }
            }
        }
    }
}

@Composable
private fun PlayPauseGlyph(playing: Boolean) {
    val p = LocalPalette.current
    Canvas(Modifier.size(18.dp)) {
        if (playing) {
            val barWidth = size.width * 0.28f
            drawRoundRect(p.background, Offset(size.width * 0.12f, 0f), Size(barWidth, size.height), CornerRadius(1.5f.dp.toPx()))
            drawRoundRect(p.background, Offset(size.width * 0.60f, 0f), Size(barWidth, size.height), CornerRadius(1.5f.dp.toPx()))
        } else {
            val path = Path().apply {
                moveTo(size.width * 0.18f, 0f)
                lineTo(size.width, size.height / 2f)
                lineTo(size.width * 0.18f, size.height)
                close()
            }
            drawPath(path, p.background)
        }
    }
}

/** 8 columns of dots that dance while music plays and rest on the bottom row when it does not. */
@Composable
private fun EqualizerDots(playing: Boolean) {
    val p = LocalPalette.current
    val phase = if (playing) {
        val transition = rememberInfiniteTransition()
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        )
        value
    } else {
        0f
    }

    val columns = 8
    val rows = 5
    Canvas(Modifier.size(width = 7.dp * 8, height = 7.dp * 5)) {
        val step = 7f.dp.toPx()
        val radius = 1.7f.dp.toPx()
        for (c in 0 until columns) {
            val lit = if (playing) {
                val wave = sin(2.0 * PI * (phase * (1 + c % 3)) + c * 1.3)
                1 + (((wave + 1.0) / 2.0) * (rows - 1)).toInt()
            } else {
                1
            }
            for (r in 0 until rows) {
                val fromBottom = rows - 1 - r
                val on = fromBottom < lit
                drawCircle(
                    color = if (on) p.text else p.dotOff,
                    radius = radius,
                    center = Offset(c * step + step / 2f, r * step + step / 2f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// "Tap to play" card: a little yes/no oracle drawn in dots
// ---------------------------------------------------------------------------------------------

private val Answers = listOf("YES", "NO", "MAYBE", "SURE", "NOPE", "LATER", "SOON", "NEVER")

@Composable
fun PlayCard(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    var answer by remember { mutableStateOf<String?>(null) }
    var scramble by remember { mutableStateOf<String?>(null) }
    var rolls by remember { mutableIntStateOf(0) }

    LaunchedEffect(rolls) {
        if (rolls == 0) return@LaunchedEffect
        answer = null
        repeat(9) {
            scramble = (1..4).map { ('A'..'Z').random() }.joinToString("")
            delay(70)
        }
        scramble = null
        answer = Answers[Random.nextInt(Answers.size)]
    }

    val busy = scramble != null
    DotCard(
        modifier.pointerInput(busy) {
            detectTapGestures(onTap = { if (!busy) rolls += 1 })
        },
        radius = 28.dp,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val shown = scramble ?: answer ?: "?"
                val color = when {
                    scramble != null -> p.muted
                    answer != null -> p.text
                    else -> p.dotOff
                }
                val cols = DotFont.layout(shown, 1).cols.coerceAtLeast(1)
                val pitch = minOf(if (shown == "?") 8.dp else 6.dp, maxWidth / cols)
                DotText(shown, pitch, color)
            }
            BasicText(
                if (answer == null && scramble == null) "TAP TO PLAY" else "TAP AGAIN",
                style = mono(10f, p.muted, spacing = 3f),
            )
        }
    }
}
