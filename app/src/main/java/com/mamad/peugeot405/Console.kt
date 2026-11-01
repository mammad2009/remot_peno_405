package com.mamad.peugeot405

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val KeyInk = Color(0xFF07080B)

// ======================================================================
//  کنسول «Maison 405»
//  ۱) HeroDial  : شیر پژو وسط دو کمان زنده‌ی ارتفاع (عقب چپ ، جلو راست)
//  ۲) AxleConsole : هر محور یک ردیف افقی  ▼▼  ▼  [ نوار ارتفاع ]  ▲  ▲▲
//  ۳) BothConsole : هر دو محور + دکمه‌ی M (میباخ) + ارتفاع فابریک
//  فرمان‌ها دقیقاً مثل قبل: دو فلش = یک‌بار فرمان سریع ، یک فلش = تا وقتی دستت روشه آرام
// ======================================================================

// ---------- ۱) صفحه‌ی اصلی: شیر و دو کمان ارتفاع ----------
@Composable
fun HeroDial(
    posF: () -> Float,
    posB: () -> Float,
    pctF: Int,
    pctB: Int,
    maybach: Boolean,
    modifier: Modifier = Modifier
) {
    val S = LocalStrings.current
    val gold by animateFloatAsState(if (maybach) 1f else 0f, tween(600), label = "heroGold")
    val inf = rememberInfiniteTransition(label = "hero")
    val pulse by inf.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "heroPulse"
    )
    // کمان‌ها فیزیکی‌اند: عقب همیشه چپ ، جلو همیشه راست (مستقل از زبان)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val accentF = lerp(Cyan, Gold, gold)
                val accentB = lerp(Violet, Gold, gold)
                val c = Offset(size.width / 2f, size.height * 0.46f)
                val r = size.height * 0.37f
                val sw = 7.dp.toPx()
                val tl = Offset(c.x - r, c.y - r)
                val sz = Size(r * 2f, r * 2f)
                val nF = ((posF() - 80f) / 32f).coerceIn(0f, 1f)
                val nB = ((posB() - 84f) / 31f).coerceIn(0f, 1f)

                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accentF.copy(alpha = 0.14f + 0.06f * pulse), Color.Transparent),
                        center = c, radius = r * 2.1f
                    ),
                    radius = r * 2.1f, center = c
                )
                drawCircle(Hair, r * 0.80f, c, style = Stroke(1.dp.toPx()))
                // ریل کمان‌ها
                drawArc(Hair, -65f, 130f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                drawArc(Hair, 115f, 130f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                // پر شدن از پایین به بالا
                if (nF > 0.005f) {
                    drawArc(accentF.copy(alpha = 0.22f), 65f, -130f * nF, false, tl, sz, style = Stroke(sw * 2.2f, cap = StrokeCap.Round))
                    drawArc(accentF, 65f, -130f * nF, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                }
                if (nB > 0.005f) {
                    drawArc(accentB.copy(alpha = 0.22f), 115f, 130f * nB, false, tl, sz, style = Stroke(sw * 2.2f, cap = StrokeCap.Round))
                    drawArc(accentB, 115f, 130f * nB, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                }
                // نقطه‌های مقیاس
                for (i in 0..4) {
                    val a = Math.toRadians((65.0 - 32.5 * i))
                    val rr = r + 14.dp.toPx()
                    drawCircle(Hair2, 1.6.dp.toPx(), Offset(c.x + (rr * Math.cos(a)).toFloat(), c.y + (rr * Math.sin(a)).toFloat()))
                    val b = Math.toRadians((115.0 + 32.5 * i))
                    drawCircle(Hair2, 1.6.dp.toPx(), Offset(c.x + (rr * Math.cos(b)).toFloat(), c.y + (rr * Math.sin(b)).toFloat()))
                }
            }
            Image(
                painterResource(R.drawable.peugeot_lion), null,
                Modifier.align(Alignment.Center).padding(bottom = 18.dp)
                    .size(width = 92.dp, height = 92.dp * LION_RATIO)
                    .graphicsLayer {
                        val nF = (posF() - 80f) / 32f
                        val nB = (posB() - 84f) / 31f
                        rotationZ = -(nF - nB) * 7f
                        val sc = 1f + 0.015f * pulse
                        scaleX = sc
                        scaleY = sc
                    },
                contentScale = ContentScale.Fit
            )
            Column(Modifier.align(Alignment.CenterStart).padding(start = 4.dp, bottom = 14.dp)) {
                Text("REAR", color = lerp(Violet, Gold, gold), fontSize = 9.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Medium)
                Text("$pctB%", color = Silver, fontSize = 24.sp, fontWeight = FontWeight.Light)
                Text(S.rear, color = Steel, fontSize = 11.sp)
            }
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 4.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text("FRONT", color = lerp(Cyan, Gold, gold), fontSize = 9.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Medium)
                Text("$pctF%", color = Silver, fontSize = 24.sp, fontWeight = FontWeight.Light)
                Text(S.front, color = Steel, fontSize = 11.sp)
            }
            GlxBadge(0.85f, Modifier.align(Alignment.BottomCenter))
        }
    }
}

// ---------- کلید کنسول: فقط فلش ----------
@Composable
private fun ConsoleKey(
    cmd: Cmd?, accent: Color, up: Boolean, fast: Boolean,
    onPress: (Cmd) -> Unit, onHoldStart: (Cmd) -> Unit, onHoldEnd: () -> Unit,
    modifier: Modifier
) {
    PressBox(
        modifier = modifier, accent = accent, filled = fast, enabled = cmd != null,
        shape = RoundedCornerShape(18.dp),
        // سریع: همان لحظه‌ی لمس یک‌بار فرمان ؛ آرام: تا وقتی دست روی دکمه است
        onDown = { cmd?.let { if (fast) onPress(it) else onHoldStart(it) } },
        onUp = { if (!fast) onHoldEnd() }
    ) { _ ->
        Chevrons(up, if (fast) 2 else 1, if (fast) KeyInk else accent, 26.dp)
    }
}

// نوار افقی ارتفاع فعلی (فقط دوباره‌کشی می‌شود ، ترکیب مجدد نه)
@Composable
private fun HLevel(level: () -> Float, accent: Color, modifier: Modifier) {
    Canvas(modifier) {
        val h = 10.dp.toPx()
        val y = size.height / 2f - h / 2f
        val rad = CornerRadius(h / 2f)
        val k = 7.dp.toPx()
        drawRoundRect(Hair, Offset(0f, y), Size(size.width, h), rad)
        val fw = size.width * level().coerceIn(0f, 1f)
        if (fw > 0f) {
            drawRoundRect(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.30f), accent),
                    startX = 0f, endX = size.width.coerceAtLeast(1f)
                ),
                Offset(0f, y), Size(fw, h), rad
            )
        }
        for (i in 0..4) {
            val x = size.width * i / 4f
            drawLine(Hair2, Offset(x, y + h + 5.dp.toPx()), Offset(x, y + h + 10.dp.toPx()), 1.dp.toPx())
        }
        drawCircle(accent.copy(alpha = 0.25f), k * 1.8f, Offset(fw.coerceIn(k, size.width - k), size.height / 2f))
        drawCircle(accent, k, Offset(fw.coerceIn(k, size.width - k), size.height / 2f))
        drawCircle(KeyInk, k * 0.35f, Offset(fw.coerceIn(k, size.width - k), size.height / 2f))
    }
}

// ---------- ۲) یک ردیف برای هر محور ----------
@Composable
fun AxleConsole(
    title: String, tag: String, accent: Color,
    upSlow: Cmd?, upFast: Cmd?, downSlow: Cmd?, downFast: Cmd?,
    level: () -> Float, percent: Int,
    onPress: (Cmd) -> Unit, onHoldStart: (Cmd) -> Unit, onHoldEnd: () -> Unit
) {
    val S = LocalStrings.current
    val en = S.lang == "en"
    CardBox(accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 3.dp, height = 16.dp).clip(RoundedCornerShape(2.dp)).background(accent))
            Spacer(Modifier.width(10.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Silver)
            Spacer(Modifier.width(8.dp))
            Text(tag, fontSize = 9.sp, letterSpacing = 3.sp, color = accent.copy(alpha = 0.8f))
            Spacer(Modifier.weight(1f))
            Text("$percent%", fontSize = 20.sp, fontWeight = FontWeight.Light, color = Silver)
        }
        // ردیف کلیدها فیزیکی است: پایین چپ ، بالا راست
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier.fillMaxWidth().height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val km = Modifier.weight(1f).fillMaxHeight()
                ConsoleKey(downFast, accent, false, true, onPress, onHoldStart, onHoldEnd, km)
                ConsoleKey(downSlow, accent, false, false, onPress, onHoldStart, onHoldEnd, km)
                HLevel(level, accent, Modifier.weight(1.5f).fillMaxHeight())
                ConsoleKey(upSlow, accent, true, false, onPress, onHoldStart, onHoldEnd, km)
                ConsoleKey(upFast, accent, true, true, onPress, onHoldStart, onHoldEnd, km)
            }
        }
        Text(
            if (en) "Double arrow = fast, one tap  •  Single arrow = slow, hold"
            else "دو فلش = سریع (یک لمس)  •  یک فلش = آرام (نگه دار)",
            color = Steel, fontSize = 10.5.sp
        )
    }
}

// ---------- ۳) هر دو محور + میباخ + فابریک ----------
@Composable
fun BothConsole(
    bothUp: Cmd?, bothDown: Cmd?, factory: Cmd?, mOn: Cmd?, mOff: Cmd?, maybachOn: Boolean,
    onHoldStart: (Cmd) -> Unit, onHoldEnd: () -> Unit, onFactory: (Cmd) -> Unit, onPress: (Cmd) -> Unit
) {
    val S = LocalStrings.current
    CardBox(Silver) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 3.dp, height = 16.dp).clip(RoundedCornerShape(2.dp)).background(Silver))
            Spacer(Modifier.width(10.dp))
            Text(S.both, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Silver)
            Spacer(Modifier.width(8.dp))
            Text("SYNC", fontSize = 9.sp, letterSpacing = 3.sp, color = Silver.copy(alpha = 0.7f))
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier.fillMaxWidth().height(76.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PressBox(
                    Modifier.weight(1f).fillMaxHeight(), Silver, filled = true, enabled = bothDown != null,
                    shape = RoundedCornerShape(20.dp),
                    onDown = { bothDown?.let(onHoldStart) },      // تا وقتی دست روی دکمه است هر دو سروو آرام پایین میروند
                    onUp = { onHoldEnd() }
                ) { _ ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Chevrons(false, 2, KeyInk, 28.dp)
                        Text(bothDown?.let { S.cmdLabel(it) } ?: "—", color = KeyInk, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PressBox(
                        Modifier.size(60.dp), Gold, filled = maybachOn, shape = CircleShape,
                        enabled = mOn != null,
                        onDown = { if (maybachOn) mOff?.let(onPress) else mOn?.let(onPress) }
                    ) { _ ->
                        Text("M", color = if (maybachOn) KeyInk else Gold, fontWeight = FontWeight.Medium, fontSize = 24.sp)
                    }
                    Text(
                        if (maybachOn) S.stop else S.maybach, fontSize = 9.5.sp,
                        color = Gold.copy(alpha = 0.85f), modifier = Modifier.padding(top = 2.dp), maxLines = 1
                    )
                }
                PressBox(
                    Modifier.weight(1f).fillMaxHeight(), Silver, filled = true, enabled = bothUp != null,
                    shape = RoundedCornerShape(20.dp),
                    onDown = { bothUp?.let(onHoldStart) },        // تا وقتی دست روی دکمه است هر دو سروو آرام بالا میروند
                    onUp = { onHoldEnd() }
                ) { _ ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Chevrons(true, 2, KeyInk, 28.dp)
                        Text(bothUp?.let { S.cmdLabel(it) } ?: "—", color = KeyInk, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
        PressBox(
            Modifier.fillMaxWidth().height(46.dp), Silver, enabled = factory != null,
            shape = RoundedCornerShape(18.dp),
            onDown = { factory?.let(onFactory) }
        ) { _ ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, null, tint = Silver, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(S.stock, color = Silver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        }
    }
}
