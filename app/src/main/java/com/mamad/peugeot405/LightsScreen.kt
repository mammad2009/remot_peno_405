package com.mamad.peugeot405

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Amber = Color(0xFFFFB020)
private val Lamp = Color(0xFFBFE8FF)
private val RedLamp = Color(0xFFFF4D4D)

/**
 * صفحه‌ی چراغ‌ها: ظاهرش عمداً با دکمه‌های پنوماتیک فرق دارد.
 * بالا: ماشین از بالا که لامپ‌هایش همان‌طور که روی ماکت روشن می‌شوند روشن می‌شوند.
 */
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LightCarView(vm, clock)

        // ---- چراغ‌های ثابت: سه دکمه‌ی گرد ----
        CardBox(Lamp) {
            CardTitle(S.lightsSteady, "LIGHTS", Lamp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RoundLightButton(S.lSmall, 0, vm.lightSmall, Lamp) { vm.setSmall(!vm.lightSmall) }
                RoundLightButton(S.lHigh, 1, vm.lightHigh, Color.White) { vm.setHigh(!vm.lightHigh) }
                RoundLightButton(S.lRear, 2, vm.lightRear, RedLamp) { vm.setRear(!vm.lightRear) }
            }
            HoldHighButton(vm)
        }

        // ---- راهنماها ----
        CardBox(Amber) {
            CardTitle(S.lTurn, "SIGNALS", Amber)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TurnButton(S.tLeft, 2, vm.turnMode == 2, clock) { vm.toggleTurn(2) }
                TurnButton(S.tHazard, 3, vm.turnMode == 3, clock) { vm.toggleTurn(3) }
                TurnButton(S.tAlt, 4, vm.turnMode == 4, clock) { vm.toggleTurn(4) }
                TurnButton(S.tRight, 1, vm.turnMode == 1, clock) { vm.toggleTurn(1) }
            }
        }

        // ---- افکت‌های چراغ (کیت راهنما) ----
        CardBox(Violet) {
            CardTitle(S.lShows, "SHOWS", Violet)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (fx in LIGHT_FX) {
                    ShowCard(fx, vm.turnMode == 5 && vm.lightFx == fx.id, clock) { vm.toggleFx(fx.id) }
                }
            }
        }

        PillButton(
            label = S.allOff, accent = Danger, filled = false, enabled = true,
            modifier = Modifier.fillMaxWidth(), height = 52.dp, onClick = { vm.lightsOff() }
        )
        Spacer(Modifier.height(8.dp))
    }
}

// ---------------- ماشین از بالا ----------------

private fun DrawScope.lamp(c: Offset, r: Float, color: Color, on: Boolean) {
    if (on) {
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent), center = c, radius = r * 3.2f),
            radius = r * 3.2f, center = c
        )
        drawCircle(color, r, c)
    } else {
        drawCircle(color.copy(alpha = 0.16f), r, c)
    }
}

@Composable
private fun LightCarView(vm: MainViewModel, clock: () -> Long) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier.fillMaxWidth().height(230.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1118), Color(0xFF07080B))))
            .border(1.dp, Color(0x22FFFFFF), shape)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val carW = h * 0.40f
            val carH = h * 0.82f
            val top = h * 0.12f
            val bottom = top + carH
            val left = cx - carW / 2f
            val tOut = turnOutput(vm.turnMode, vm.lightFx, clock())
            val rightOn = (tOut and 1) != 0
            val leftOn = (tOut and 2) != 0

            // پرتوی نور بالا / کوچک
            if (vm.lightHigh || vm.lightSmall) {
                val reach = if (vm.lightHigh) top - h * 0.12f else top - h * 0.05f
                val a = if (vm.lightHigh) 0.34f else 0.13f
                for (sgn in listOf(-1f, 1f)) {
                    val x = cx + sgn * carW * 0.30f
                    val p = Path().apply {
                        moveTo(x - carW * 0.10f, top + 4f)
                        lineTo(x + carW * 0.10f, top + 4f)
                        lineTo(x + sgn * carW * 0.28f + carW * 0.22f, reach)
                        lineTo(x + sgn * carW * 0.28f - carW * 0.22f, reach)
                        close()
                    }
                    drawPath(p, Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = a)), startY = reach, endY = top))
                }
            }

            // چرخ‌ها
            val wheelW = carW * 0.12f
            val wheelH = carH * 0.17f
            for (sgn in listOf(-1f, 1f)) {
                for (yy in listOf(top + carH * 0.14f, top + carH * 0.68f)) {
                    val x = if (sgn < 0) left - wheelW * 0.55f else left + carW - wheelW * 0.45f
                    drawRoundRect(Color(0xFF252B36), Offset(x, yy), Size(wheelW, wheelH), CornerRadius(4f))
                }
            }

            // بدنه
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFF2A303C), Color(0xFF171B23))),
                Offset(left, top), Size(carW, carH), CornerRadius(carW * 0.30f)
            )
            drawRoundRect(
                Silver.copy(alpha = 0.35f), Offset(left, top), Size(carW, carH), CornerRadius(carW * 0.30f),
                style = Stroke(1.2f)
            )
            // کابین و شیشه‌ها
            drawRoundRect(Color(0xFF0B0E14), Offset(left + carW * 0.14f, top + carH * 0.30f), Size(carW * 0.72f, carH * 0.38f), CornerRadius(carW * 0.16f))
            drawRoundRect(Color(0xFF1B2230), Offset(left + carW * 0.20f, top + carH * 0.34f), Size(carW * 0.60f, carH * 0.12f), CornerRadius(carW * 0.06f))
            drawRoundRect(Color(0xFF1B2230), Offset(left + carW * 0.20f, top + carH * 0.56f), Size(carW * 0.60f, carH * 0.08f), CornerRadius(carW * 0.06f))

            // چراغ‌های جلو
            val fy = top + carH * 0.045f
            lamp(Offset(cx - carW * 0.30f, fy), 7f, if (vm.lightHigh) Color.White else Lamp, vm.lightSmall || vm.lightHigh)
            lamp(Offset(cx + carW * 0.30f, fy), 7f, if (vm.lightHigh) Color.White else Lamp, vm.lightSmall || vm.lightHigh)
            // چراغ‌های عقب
            val ry = bottom - carH * 0.045f
            lamp(Offset(cx - carW * 0.30f, ry), 7f, RedLamp, vm.lightRear)
            lamp(Offset(cx + carW * 0.30f, ry), 7f, RedLamp, vm.lightRear)
            // راهنماها (چپ و راست ماشین = چپ و راست صفحه)
            lamp(Offset(left + carW * 0.07f, top + carH * 0.12f), 5.5f, Amber, leftOn)
            lamp(Offset(left + carW * 0.93f, top + carH * 0.12f), 5.5f, Amber, rightOn)
            lamp(Offset(left + carW * 0.07f, bottom - carH * 0.12f), 5.5f, Amber, leftOn)
            lamp(Offset(left + carW * 0.93f, bottom - carH * 0.12f), 5.5f, Amber, rightOn)
        }
    }
}

// ---------------- دکمه‌ی گرد چراغ ثابت ----------------

private fun DrawScope.lampIcon(kind: Int, color: Color) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension * 0.17f
    drawCircle(color, r, c)
    val rays = if (kind == 1) 5 else 3
    val len = size.minDimension * 0.17f
    for (i in 0 until rays) {
        // پرتوها به جلو (بالا) برای جلوها و به عقب (پایین) برای چراغ عقب
        val spread = (i - (rays - 1) / 2f) * 0.42f
        val dirY = if (kind == 2) 1f else -1f
        val dx = kotlin.math.sin(spread)
        val dy = dirY * kotlin.math.cos(spread)
        drawLine(
            color, Offset(c.x + dx * (r + 5f), c.y + dy * (r + 5f)),
            Offset(c.x + dx * (r + 5f + len), c.y + dy * (r + 5f + len)), 3f, StrokeCap.Round
        )
    }
}

@Composable
private fun RoundLightButton(label: String, kind: Int, on: Boolean, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(78.dp).clip(CircleShape)
                .background(
                    if (on) Brush.radialGradient(listOf(color.copy(alpha = 0.40f), color.copy(alpha = 0.10f)))
                    else Brush.radialGradient(listOf(Color(0xFF1B212C), Color(0xFF10141B)))
                )
                .border(if (on) 2.dp else 1.dp, if (on) color else Color(0x33FFFFFF), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(46.dp)) { lampIcon(kind, if (on) color else Steel) }
        }
        Text(label, color = if (on) color else Steel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------- دکمه‌ی راهنما ----------------

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
            drawPath(p, color, style = Stroke(w * 0.09f))
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
private fun TurnButton(label: String, mode: Int, on: Boolean, clock: () -> Long, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val shape = RoundedCornerShape(16.dp)
        Box(
            Modifier.size(62.dp).clip(shape)
                .background(if (on) Amber.copy(alpha = 0.22f) else Color(0xFF141922))
                .border(if (on) 2.dp else 1.dp, if (on) Amber else Color(0x33FFFFFF), shape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(34.dp)) {
                val blinkOn = !on || turnOutput(3, 0, clock()) != 0
                turnIcon(mode, if (on && blinkOn) Amber else if (on) Amber.copy(alpha = 0.35f) else Steel)
            }
        }
        Text(label, color = if (on) Amber else Steel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------- کارت افکت چراغ با پیش‌نمایش متحرک ----------------

@Composable
private fun ShowCard(fx: Named, selected: Boolean, clock: () -> Long, onClick: () -> Unit) {
    val S = LocalStrings.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.width(104.dp).clip(shape)
            .background(if (selected) Violet.copy(alpha = 0.20f) else Color(0xFF141922))
            .border(if (selected) 2.dp else 1.dp, if (selected) Violet else Color(0x33FFFFFF), shape)
            .clickable { onClick() }.padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(fx.icon, fontSize = 24.sp)
        Text(fx.name(S), color = if (selected) Violet else Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Canvas(Modifier.width(56.dp).height(14.dp)) {
            val o = seqOutput(fx.id, clock())
            val r = size.height / 2f
            lamp(Offset(r, r), r * 0.75f, Amber, (o and 2) != 0)
            lamp(Offset(size.width - r, r), r * 0.75f, Amber, (o and 1) != 0)
        }
    }
}

// ---------------- نور بالای فشاری: تا وقتی دست روی دکمه است روشن ----------------

@Composable
private fun HoldHighButton(vm: MainViewModel) {
    val S = LocalStrings.current
    var pressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.fillMaxWidth().height(58.dp).clip(shape)
            .background(if (pressed) Color.White.copy(alpha = 0.26f) else Color(0xFF141922))
            .border(if (pressed) 2.dp else 1.dp, if (pressed) Color.White else Color(0x55FFFFFF), shape)
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
        Text(S.lHold, color = if (pressed) Color.White else Steel, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
