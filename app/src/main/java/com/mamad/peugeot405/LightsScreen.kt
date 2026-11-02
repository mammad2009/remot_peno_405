package com.mamad.peugeot405

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Amber = Color(0xFFFFB347)
private val Lamp = Color(0xFFDDEEFF)
private val RedLamp = Color(0xFFFF5A52)

// ======================================================================
//  «Lighting Atelier» : صفحه‌ی چراغ‌ها
//  ۱) صحنه: ماکت از بالا ، با خط‌های ظریف و پرتوهای نور
//  ۲) چراغ‌های ثابت: سه سلول بلند با نشانگر LED
//  ۳) راهنما: دسته‌ی راهنما (چپ / فلاشر / راست) و نوبتی
//  ۴) افکت‌ها: شبکه‌ی ۳×۲ با پیش‌نمایش زنده
// ======================================================================
@Composable
fun LightsScreen(vm: MainViewModel) {
    val S = LocalStrings.current
    LaunchedEffect(Unit) { vm.requestInfo() }
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val t0 = withFrameNanos { it }
        while (true) {
            withFrameNanos { now = (it - t0) / 1_000_000L }
        }
    }
    val clock: () -> Long = { now }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LightStage(vm, clock)

        // ---- چراغ‌های ثابت ----
        LightSection(S.lightsSteady, "EXTERIOR", Cyan) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LightCell(S.lSmall, "POSITION", 0, vm.lightSmall, Lamp, Modifier.weight(1f)) { vm.setSmall(!vm.lightSmall) }
                LightCell(S.lHigh, "HIGH BEAM", 1, vm.lightHigh, Color.White, Modifier.weight(1f)) { vm.setHigh(!vm.lightHigh) }
                LightCell(S.lRear, "TAIL", 2, vm.lightRear, RedLamp, Modifier.weight(1f)) { vm.setRear(!vm.lightRear) }
            }
            HoldHighButton(vm)
        }

        // ---- راهنماها ----
        LightSection(S.lTurn, "SIGNALS", Amber) {
            // دسته‌ی راهنما فیزیکی است: چپ سمت چپ ، راست سمت راست
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TurnKey(S.tLeft, 2, vm.turnMode == 2, clock, Modifier.weight(1f).height(84.dp)) { vm.toggleTurn(2) }
                    TurnKey(S.tHazard, 3, vm.turnMode == 3, clock, Modifier.weight(1f).height(84.dp)) { vm.toggleTurn(3) }
                    TurnKey(S.tRight, 1, vm.turnMode == 1, clock, Modifier.weight(1f).height(84.dp)) { vm.toggleTurn(1) }
                }
            }
            TurnKey(S.tAlt, 4, vm.turnMode == 4, clock, Modifier.fillMaxWidth().height(54.dp), wide = true) { vm.toggleTurn(4) }
        }

        // ---- افکت‌های چراغ ----
        LightSection(S.lShows, "SHOWS", Violet) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LIGHT_FX.chunked(3).forEach { rowItems ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowItems.forEach { fx ->
                            ShowCell(fx, vm.turnMode == 5 && vm.lightFx == fx.id, clock, Modifier.weight(1f)) { vm.toggleFx(fx.id) }
                        }
                        repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        PillButton(
            label = S.allOff, accent = Danger, filled = false, enabled = true,
            modifier = Modifier.fillMaxWidth(), height = 54.dp, onClick = { vm.lightsOff() }
        )
        Spacer(Modifier.height(8.dp))
    }
}

// ---------------- قاب هر بخش ----------------

@Composable
private fun LightSection(title: String, tag: String, accent: Color, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier.fillMaxWidth().luxPanel(shape, accent).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tag, color = accent.copy(alpha = 0.85f), fontSize = 9.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Medium)
            Text(title, color = Silver, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

// ---------------- صحنه: ماکت از بالا ----------------

private fun DrawScope.glowBar(c: Offset, w: Float, h: Float, color: Color, on: Boolean) {
    val tl = Offset(c.x - w / 2f, c.y - h / 2f)
    if (on) {
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = 0.50f), Color.Transparent), center = c, radius = w * 1.5f),
            radius = w * 1.5f, center = c
        )
        drawRoundRect(color, tl, Size(w, h), CornerRadius(h / 2f))
    } else {
        drawRoundRect(color.copy(alpha = 0.14f), tl, Size(w, h), CornerRadius(h / 2f))
    }
}

private fun DrawScope.glowDot(c: Offset, r: Float, color: Color, on: Boolean) {
    if (on) {
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent), center = c, radius = r * 3.4f),
            radius = r * 3.4f, center = c
        )
        drawCircle(color, r, c)
    } else {
        drawCircle(color.copy(alpha = 0.16f), r, c)
    }
}

@Composable
private fun LightStage(vm: MainViewModel, clock: () -> Long) {
    val shape = RoundedCornerShape(28.dp)
    Box(Modifier.fillMaxWidth().height(260.dp).luxPanel(shape, Cyan)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val carW = h * 0.36f
            val carH = h * 0.74f
            val top = h * 0.13f
            val bottom = top + carH
            val left = cx - carW / 2f
            val tOut = turnOutput(vm.turnMode, vm.lightFx, clock())
            val rightOn = (tOut and 1) != 0
            val leftOn = (tOut and 2) != 0
            val hair = 1.dp.toPx()

            // هاله‌ی پشت ماکت و خط راهنمای وسط
            drawCircle(
                brush = Brush.radialGradient(listOf(Cyan.copy(alpha = 0.07f), Color.Transparent), center = Offset(cx, h / 2f), radius = h * 0.8f),
                radius = h * 0.8f, center = Offset(cx, h / 2f)
            )
            drawLine(
                Hair, Offset(cx, 0f), Offset(cx, h), hair,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 9.dp.toPx()), 0f)
            )

            // پرتوی نور بالا / کوچک
            if (vm.lightHigh || vm.lightSmall) {
                val reach = if (vm.lightHigh) top - h * 0.12f else top - h * 0.05f
                val a = if (vm.lightHigh) 0.36f else 0.14f
                for (sgn in listOf(-1f, 1f)) {
                    val x = cx + sgn * carW * 0.28f
                    val p = Path().apply {
                        moveTo(x - carW * 0.10f, top + 4f)
                        lineTo(x + carW * 0.10f, top + 4f)
                        lineTo(x + sgn * carW * 0.26f + carW * 0.22f, reach)
                        lineTo(x + sgn * carW * 0.26f - carW * 0.22f, reach)
                        close()
                    }
                    drawPath(p, Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = a)), startY = reach, endY = top))
                }
            }

            // چرخ‌ها
            val wheelW = carW * 0.11f
            val wheelH = carH * 0.17f
            for (sgn in listOf(-1f, 1f)) {
                for (yy in listOf(top + carH * 0.13f, top + carH * 0.70f)) {
                    val x = if (sgn < 0) left - wheelW * 0.50f else left + carW - wheelW * 0.50f
                    drawRoundRect(Surface4, Offset(x, yy), Size(wheelW, wheelH), CornerRadius(wheelW * 0.4f))
                }
            }

            // بدنه: پرشده با گرادیان گرافیتی و لبه‌ی نقره‌ای ظریف
            val body = CornerRadius(carW * 0.34f)
            drawRoundRect(Brush.verticalGradient(listOf(Surface4, Surface2)), Offset(left, top), Size(carW, carH), body)
            drawRoundRect(Silver.copy(alpha = 0.40f), Offset(left, top), Size(carW, carH), body, style = Stroke(hair * 1.2f))
            // خط‌های کاپوت و صندوق
            drawLine(Hair2, Offset(cx - carW * 0.20f, top + carH * 0.07f), Offset(cx - carW * 0.20f, top + carH * 0.26f), hair, StrokeCap.Round)
            drawLine(Hair2, Offset(cx + carW * 0.20f, top + carH * 0.07f), Offset(cx + carW * 0.20f, top + carH * 0.26f), hair, StrokeCap.Round)
            // کابین و شیشه‌ها
            drawRoundRect(Bg, Offset(left + carW * 0.13f, top + carH * 0.30f), Size(carW * 0.74f, carH * 0.38f), CornerRadius(carW * 0.18f))
            drawRoundRect(Hair2, Offset(left + carW * 0.13f, top + carH * 0.30f), Size(carW * 0.74f, carH * 0.38f), CornerRadius(carW * 0.18f), style = Stroke(hair))
            drawRoundRect(Surface3, Offset(left + carW * 0.20f, top + carH * 0.34f), Size(carW * 0.60f, carH * 0.11f), CornerRadius(carW * 0.06f))
            drawRoundRect(Surface3, Offset(left + carW * 0.20f, top + carH * 0.56f), Size(carW * 0.60f, carH * 0.07f), CornerRadius(carW * 0.06f))

            // چراغ‌های جلو (میله‌ای ظریف) و عقب
            val frontOn = vm.lightSmall || vm.lightHigh
            val fc = if (vm.lightHigh) Color.White else Lamp
            val fy = top + carH * 0.05f
            glowBar(Offset(cx - carW * 0.27f, fy), carW * 0.24f, 5.dp.toPx(), fc, frontOn)
            glowBar(Offset(cx + carW * 0.27f, fy), carW * 0.24f, 5.dp.toPx(), fc, frontOn)
            val ry = bottom - carH * 0.05f
            glowBar(Offset(cx - carW * 0.27f, ry), carW * 0.24f, 5.dp.toPx(), RedLamp, vm.lightRear)
            glowBar(Offset(cx + carW * 0.27f, ry), carW * 0.24f, 5.dp.toPx(), RedLamp, vm.lightRear)
            // راهنماها
            glowDot(Offset(left + carW * 0.07f, top + carH * 0.13f), 3.6.dp.toPx(), Amber, leftOn)
            glowDot(Offset(left + carW * 0.93f, top + carH * 0.13f), 3.6.dp.toPx(), Amber, rightOn)
            glowDot(Offset(left + carW * 0.07f, bottom - carH * 0.13f), 3.6.dp.toPx(), Amber, leftOn)
            glowDot(Offset(left + carW * 0.93f, bottom - carH * 0.13f), 3.6.dp.toPx(), Amber, rightOn)
        }
        Text(
            "FRONT", color = Steel, fontSize = 9.sp, letterSpacing = 3.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
        )
        Text(
            "REAR", color = Steel, fontSize = 9.sp, letterSpacing = 3.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
        )
    }
}

// ---------------- سلول چراغ ثابت ----------------

private fun DrawScope.lampIcon(kind: Int, color: Color) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension * 0.15f
    val sw = size.minDimension * 0.07f
    drawCircle(color, r, c, style = Stroke(sw))
    drawCircle(color, r * 0.38f, c)
    val rays = if (kind == 1) 5 else 3
    val len = size.minDimension * 0.17f
    for (i in 0 until rays) {
        // پرتوها به جلو (بالا) برای جلوها و به عقب (پایین) برای چراغ عقب
        val spread = (i - (rays - 1) / 2f) * 0.42f
        val dirY = if (kind == 2) 1f else -1f
        val dx = kotlin.math.sin(spread)
        val dy = dirY * kotlin.math.cos(spread)
        drawLine(
            color, Offset(c.x + dx * (r + sw * 2f), c.y + dy * (r + sw * 2f)),
            Offset(c.x + dx * (r + sw * 2f + len), c.y + dy * (r + sw * 2f + len)), sw, StrokeCap.Round
        )
    }
}

@Composable
private fun LightCell(label: String, tag: String, kind: Int, on: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val a by animateFloatAsState(if (on) 1f else 0f, tween(280), label = "lightCell")
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.height(124.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(lerp(Surface3, color, 0.24f * a), Surface2)))
            .border(1.dp, Brush.verticalGradient(listOf(lerp(Hair2, color, a), Hair)), shape)
            .clickable { onClick() }.padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Canvas(Modifier.size(44.dp)) { lampIcon(kind, lerp(Steel, color, a)) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = lerp(Silver, color, a * 0.4f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(tag, color = Steel, fontSize = 8.sp, letterSpacing = 2.sp, maxLines = 1)
        }
        Box(
            Modifier.width((10f + 26f * a).dp).height(3.dp).clip(RoundedCornerShape(2.dp))
                .background(lerp(Surface4, color, a))
        )
    }
}

// ---------------- دسته‌ی راهنما ----------------

private fun arrowPath(x0: Float, y0: Float, w: Float, h: Float, dir: Int): Path {
    val p = Path()
    // فلش رو به راست در جعبه‌ی (x0, y0, w, h) ؛ برای چپ آینه می‌شود
    fun px(f: Float): Float = if (dir > 0) x0 + w * f else x0 + w * (1f - f)
    p.moveTo(px(0.05f), y0 + h * 0.36f)
    p.lineTo(px(0.55f), y0 + h * 0.36f)
    p.lineTo(px(0.55f), y0 + h * 0.10f)
    p.lineTo(px(0.97f), y0 + h * 0.50f)
    p.lineTo(px(0.55f), y0 + h * 0.90f)
    p.lineTo(px(0.55f), y0 + h * 0.64f)
    p.lineTo(px(0.05f), y0 + h * 0.64f)
    p.close()
    return p
}

private fun DrawScope.turnIcon(mode: Int, color: Color) {
    val w = size.width
    val h = size.height
    when (mode) {
        1 -> drawPath(arrowPath(0f, h * 0.1f, w, h * 0.8f, 1), color)
        2 -> drawPath(arrowPath(0f, h * 0.1f, w, h * 0.8f, -1), color)
        3 -> {                                            // مثلث فلاشر
            val p = Path().apply {
                moveTo(w * 0.5f, h * 0.10f)
                lineTo(w * 0.92f, h * 0.86f)
                lineTo(w * 0.08f, h * 0.86f)
                close()
            }
            drawPath(p, color, style = Stroke(w * 0.09f, join = StrokeJoin.Round))
            drawLine(color, Offset(w * 0.5f, h * 0.38f), Offset(w * 0.5f, h * 0.60f), w * 0.08f, StrokeCap.Round)
            drawCircle(color, w * 0.05f, Offset(w * 0.5f, h * 0.73f))
        }
        else -> {                                         // نوبتی: دو فلش رو به رو
            drawPath(arrowPath(0f, h * 0.05f, w, h * 0.42f, 1), color)
            drawPath(arrowPath(0f, h * 0.53f, w, h * 0.42f, -1), color)
        }
    }
}

@Composable
private fun TurnKey(label: String, mode: Int, on: Boolean, clock: () -> Long, modifier: Modifier, wide: Boolean = false, onClick: () -> Unit) {
    val a by animateFloatAsState(if (on) 1f else 0f, tween(260), label = "turnKey")
    val shape = RoundedCornerShape(if (wide) 18.dp else 22.dp)
    val content: @Composable () -> Unit = {
        Canvas(Modifier.size(if (wide) 26.dp else 34.dp)) {
            val blinkOn = !on || turnOutput(3, 0, clock()) != 0
            turnIcon(mode, if (on && blinkOn) Amber else if (on) Amber.copy(alpha = 0.35f) else Steel)
        }
        Text(
            label, color = lerp(Steel, Amber, a), fontSize = if (wide) 13.sp else 12.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1
        )
    }
    val m = modifier.clip(shape)
        .background(Brush.verticalGradient(listOf(lerp(Surface3, Amber, 0.20f * a), Surface2)))
        .border(1.dp, Brush.verticalGradient(listOf(lerp(Hair2, Amber, a), Hair)), shape)
        .clickable { onClick() }
    if (wide) {
        Row(m, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            content()
        }
    } else {
        Column(m, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            content()
        }
    }
}

// ---------------- افکت‌ها ----------------

private fun DrawScope.fxIcon(id: Int, color: Color) {
    val w = size.width
    val h = size.height
    val sw = w * 0.085f
    when (id) {
        0 -> {                                            // عربی: دو چراغ و پرتوهای پشت‌سرهم
            drawCircle(color, w * 0.11f, Offset(w * 0.22f, h * 0.5f))
            drawCircle(color, w * 0.11f, Offset(w * 0.78f, h * 0.5f))
            for (i in 0..2) {
                val x = w * (0.38f + i * 0.12f)
                drawLine(color.copy(alpha = 0.35f + 0.25f * i), Offset(x, h * 0.34f), Offset(x, h * 0.66f), sw, StrokeCap.Round)
            }
        }
        1 -> {                                            // پلیسی: دو نیمه
            drawCircle(color, w * 0.34f, Offset(w / 2f, h / 2f), style = Stroke(sw))
            drawLine(color, Offset(w / 2f, h * 0.16f), Offset(w / 2f, h * 0.84f), sw, StrokeCap.Round)
            drawArc(color, 90f, 180f, true, Offset(w * 0.16f, h * 0.16f), Size(w * 0.68f, h * 0.68f))
        }
        2 -> {                                            // استروب: رعد
            val p = Path().apply {
                moveTo(w * 0.58f, h * 0.06f)
                lineTo(w * 0.22f, h * 0.56f)
                lineTo(w * 0.48f, h * 0.56f)
                lineTo(w * 0.40f, h * 0.94f)
                lineTo(w * 0.78f, h * 0.40f)
                lineTo(w * 0.52f, h * 0.40f)
                close()
            }
            drawPath(p, color)
        }
        3 -> {                                            // موج
            val p = Path()
            p.moveTo(0f, h * 0.5f)
            val n = 24
            for (i in 1..n) {
                val x = w * i / n
                p.lineTo(x, h * 0.5f + kotlin.math.sin(i * 0.5f * 3.1416f / 2f * 1.2f) * h * 0.26f)
            }
            drawPath(p, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        4 -> {                                            // ضربان قلب
            val p = Path().apply {
                moveTo(0f, h * 0.56f)
                lineTo(w * 0.28f, h * 0.56f)
                lineTo(w * 0.38f, h * 0.22f)
                lineTo(w * 0.52f, h * 0.86f)
                lineTo(w * 0.62f, h * 0.44f)
                lineTo(w * 0.70f, h * 0.56f)
                lineTo(w, h * 0.56f)
            }
            drawPath(p, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        else -> {                                         // سریع: دو شورون
            for (k in 0..1) {
                val x0 = w * (0.14f + 0.34f * k)
                val p = Path().apply {
                    moveTo(x0, h * 0.18f)
                    lineTo(x0 + w * 0.30f, h * 0.5f)
                    lineTo(x0, h * 0.82f)
                }
                drawPath(p, color, style = Stroke(sw * 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable
private fun ShowCell(fx: Named, selected: Boolean, clock: () -> Long, modifier: Modifier, onClick: () -> Unit) {
    val S = LocalStrings.current
    val a by animateFloatAsState(if (selected) 1f else 0f, tween(280), label = "showCell")
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.height(104.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(lerp(Surface3, Violet, 0.22f * a), Surface2)))
            .border(1.dp, Brush.verticalGradient(listOf(lerp(Hair2, Violet, a), Hair)), shape)
            .clickable { onClick() }.padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Canvas(Modifier.size(28.dp)) { fxIcon(fx.id, lerp(Steel, Violet, a)) }
        Text(fx.name(S), color = lerp(Silver, Violet, a * 0.5f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Canvas(Modifier.width(52.dp).height(12.dp)) {
            val o = seqOutput(fx.id, clock())
            val r = size.height / 2f
            glowDot(Offset(r, r), r * 0.62f, Amber, (o and 2) != 0)
            glowDot(Offset(size.width - r, r), r * 0.62f, Amber, (o and 1) != 0)
        }
    }
}

// ---------------- نور بالای فشاری: تا وقتی دست روی دکمه است روشن ----------------

@Composable
private fun HoldHighButton(vm: MainViewModel) {
    val S = LocalStrings.current
    var pressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().height(58.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(if (pressed) Color.White.copy(alpha = 0.26f) else Surface3, Surface2)))
            .border(1.dp, Brush.verticalGradient(listOf(if (pressed) Color.White else Hair2, Hair)), shape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val was = vm.lightHigh            // حالت قبلی نور بالا (اگر از قبل روشن بود روشن می‌ماند)
                    pressed = true
                    vm.setHigh(true)
                    try {
                        tryAwaitRelease()
                    } finally {
                        pressed = false
                        vm.setHigh(was)
                    }
                })
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(28.dp)) { lampIcon(1, if (pressed) Color.White else Steel) }
        Spacer(Modifier.width(10.dp))
        Text(S.lHold, color = if (pressed) Color.White else Steel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
