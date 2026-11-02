package com.mamad.peugeot405

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * منوی ماکت = حالت کارخانه.
 * وقتی این تب باز است ماکت کف‌خواب است (فرمان Y) و از اینجا همه‌چیزش را تنظیم می‌کنی.
 * خروج از تب (یا بستن برنامه) = فرمان V.
 *
 * طراحی «Maison 405»: هاب به‌صورت چهار ماژول بزرگ با پیش‌نمایش زنده‌ی مقدار ،
 * آیکن‌های خطی (Canvas) ، گیج‌ها و نوارهای بازه‌ی زاویه ، بدون ایموجی در نقش آیکن.
 */
@Composable
fun MenuScreen(vm: MainViewModel, onExit: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(0) }     // 0 = خانه ، 1 تعلیق ، 2 افکت‌ها ، 3 باتری ، 4 سیستم
    BackHandler(enabled = page != 0) { page = 0 }

    // وضعیت ماکت هر چند ثانیه یک بار خوانده می‌شود
    LaunchedEffect(vm.connected) {
        while (vm.connected) {
            vm.requestInfo()
            delay(4000)
        }
    }

    Crossfade(targetState = page, animationSpec = tween(280), label = "menuPage") { p ->
        when (p) {
            0 -> MenuHub(vm, onExit) { page = it }
            1 -> SuspensionPage(vm) { page = 0 }
            2 -> EffectsPage(vm) { page = 0 }
            3 -> BatteryPage(vm) { page = 0 }
            else -> SystemPage(vm) { page = 0 }
        }
    }
}

// ======================================================================
//  اجزای مشترک
// ======================================================================

@Composable
private fun Eyebrow(text: String, color: Color) {
    Text(text, color = color, fontSize = 9.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Medium, maxLines = 1)
}

// آیکن‌های خطی ماژول‌ها: 0 تعلیق ، 1 چشم‌ها ، 2 باتری ، 3 سیستم
private fun DrawScope.menuIcon(kind: Int, c: Color) {
    val w = size.width
    val h = size.height
    val sw = w * 0.085f
    when (kind) {
        0 -> {
            val p = Path()
            p.moveTo(w * 0.5f, h * 0.06f)
            p.lineTo(w * 0.22f, h * 0.24f)
            p.lineTo(w * 0.78f, h * 0.40f)
            p.lineTo(w * 0.22f, h * 0.56f)
            p.lineTo(w * 0.78f, h * 0.72f)
            p.lineTo(w * 0.5f, h * 0.94f)
            drawPath(p, c, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        1 -> {
            drawCircle(c, w * 0.20f, Offset(w * 0.28f, h * 0.5f), style = Stroke(sw))
            drawCircle(c, w * 0.20f, Offset(w * 0.72f, h * 0.5f), style = Stroke(sw))
            drawCircle(c, w * 0.07f, Offset(w * 0.31f, h * 0.5f))
            drawCircle(c, w * 0.07f, Offset(w * 0.75f, h * 0.5f))
        }
        2 -> {
            drawRoundRect(c, Offset(w * 0.08f, h * 0.28f), Size(w * 0.78f, h * 0.44f), CornerRadius(w * 0.09f), style = Stroke(sw))
            drawRoundRect(c, Offset(w * 0.88f, h * 0.42f), Size(w * 0.07f, h * 0.16f), CornerRadius(w * 0.02f))
            drawRoundRect(c, Offset(w * 0.16f, h * 0.37f), Size(w * 0.34f, h * 0.26f), CornerRadius(w * 0.03f))
        }
        else -> {
            val ctr = Offset(w / 2f, h / 2f)
            val r = w * 0.26f
            drawCircle(c, r, ctr, style = Stroke(sw))
            drawCircle(c, r * 0.35f, ctr, style = Stroke(sw))
            for (k in 0 until 8) {
                val a = k * 0.7853982f
                val dx = kotlin.math.cos(a)
                val dy = kotlin.math.sin(a)
                drawLine(
                    c, Offset(ctr.x + dx * r * 1.2f, ctr.y + dy * r * 1.2f),
                    Offset(ctr.x + dx * r * 1.7f, ctr.y + dy * r * 1.7f), sw * 1.3f, StrokeCap.Round
                )
            }
        }
    }
}

// فلش «به جلو» (فارسی چپ‌سو ، انگلیسی راست‌سو)
@Composable
private fun ForwardChevron(color: Color, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val p = Path()
        if (rtl) {
            p.moveTo(w * 0.65f, h * 0.1f); p.lineTo(w * 0.25f, h * 0.5f); p.lineTo(w * 0.65f, h * 0.9f)
        } else {
            p.moveTo(w * 0.35f, h * 0.1f); p.lineTo(w * 0.75f, h * 0.5f); p.lineTo(w * 0.35f, h * 0.9f)
        }
        drawPath(p, color, style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun PageHeader(title: String, accent: Color, onBack: () -> Unit, tag: String = "") {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(Surface2)
                .border(1.dp, Brush.verticalGradient(listOf(accent.copy(alpha = 0.7f), Hair)), CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(16.dp)) {
                val w = size.width
                val h = size.height
                val p = Path()
                if (rtl) {                                    // فلش به سمت شروع صفحه: فارسی راست ، انگلیسی چپ
                    p.moveTo(w * 0.35f, h * 0.1f); p.lineTo(w * 0.75f, h * 0.5f); p.lineTo(w * 0.35f, h * 0.9f)
                } else {
                    p.moveTo(w * 0.65f, h * 0.1f); p.lineTo(w * 0.25f, h * 0.5f); p.lineTo(w * 0.65f, h * 0.9f)
                }
                drawPath(p, accent, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column {
            if (tag.isNotEmpty()) Eyebrow(tag, accent.copy(alpha = 0.85f))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Silver)
        }
    }
}

// قاب مات یک بخش
@Composable
private fun Section(accent: Color, active: Boolean = false, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().luxPanel(RoundedCornerShape(26.dp), accent, active).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

@Composable
private fun PulseDot(on: Boolean, color: Color, dotSize: Dp = 14.dp) {
    val inf = rememberInfiniteTransition(label = "dot")
    val t by inf.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dotT"
    )
    Canvas(Modifier.size(dotSize)) {
        val r = size.minDimension / 2f
        if (on) drawCircle(color.copy(alpha = 0.40f * (1f - t)), r, center)
        drawCircle(color, r * 0.5f, center)
    }
}

@Composable
private fun LuxDialog(title: String, text: String?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val S = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2,
        shape = RoundedCornerShape(28.dp),
        titleContentColor = Silver,
        textContentColor = Silver,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { if (text != null) Text(text, color = Steel, fontSize = 13.sp) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(S.save, color = Cyan) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(S.cancel, color = Steel) } }
    )
}

@Composable
private fun WaitingCard(vm: MainViewModel) {
    val S = LocalStrings.current
    Section(Steel) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulseDot(true, Gold)
            Spacer(Modifier.width(10.dp))
            Text(S.sWaiting, color = Steel, fontSize = 13.sp, modifier = Modifier.weight(1f))
        }
        PillButton(
            label = S.refresh, accent = Cyan, filled = false, enabled = true,
            modifier = Modifier.fillMaxWidth(), height = 46.dp, onClick = { vm.requestInfo() }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = Silver) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Steel, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hair))
    }
}

@Composable
private fun ValueSlider(
    label: String, value: Int, lo: Int, hi: Int, accent: Color, unit: String = "°",
    onChange: (Int) -> Unit, onFinish: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Silver, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("$value$unit", color = accent, fontSize = 22.sp, fontWeight = FontWeight.Light)
        }
        Slider(
            value = value.coerceIn(lo, hi).toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = lo.toFloat()..hi.toFloat(),
            onValueChangeFinished = onFinish,
            colors = SliderDefaults.colors(
                thumbColor = accent, activeTrackColor = accent,
                inactiveTrackColor = Surface4
            )
        )
    }
}

// دکمه‌ی «کپی عددها برای کد»: متن آماده‌ی #define ها را در کلیپ‌بورد می‌گذارد
@Composable
private fun CopyCodeButton(code: () -> String) {
    val S = LocalStrings.current
    val clip = LocalClipboardManager.current
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(done) {
        if (done) {
            delay(1800)
            done = false
        }
    }
    PillButton(
        label = if (done) S.copied else S.sCopy, accent = Cyan, filled = false, enabled = true,
        modifier = Modifier.fillMaxWidth(), height = 50.dp,
        onClick = { clip.setText(AnnotatedString(code())); done = true }
    )
}

private fun servoCode(rf: Int, rb: Int, xf: Int, xb: Int, mf: Int, mb: Int): String =
    "#define DEF_REST_F   $rf\n#define DEF_REST_B   $rb\n#define DEF_MAX_F    $xf\n" +
        "#define DEF_MAX_B    $xb\n#define DEF_MID_F    $mf\n#define DEF_MID_B    $mb"

private fun batteryCode(on: Boolean, lo: Int, cr: Int, ca: Int): String =
    "#define BAT_START_ON     " + (if (on) 1 else 0) + "\n#define BAT_LOW_PCT      $lo\n" +
        "#define BAT_CRIT_PCT     $cr\n#define BAT_CAL          " + (ca / 100) + "." + (ca % 100).toString().padStart(2, '0')

// ======================================================================
//  صفحه‌ی خانه: وضعیت کارخانه + چهار ماژول
// ======================================================================

@Composable
private fun ModuleRow(
    kind: Int, tag: String, title: String, value: String, accent: Color, onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().luxPanel(RoundedCornerShape(26.dp), accent)
            .clickable { onClick() }.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(54.dp).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.24f), accent.copy(alpha = 0.04f))))
                .border(1.dp, accent.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(26.dp)) { menuIcon(kind, accent) }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Eyebrow(tag, accent.copy(alpha = 0.85f))
            Text(title, color = Silver, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, maxLines = 1)
            Text(value, color = Steel, fontSize = 12.sp, maxLines = 1)
        }
        ForwardChevron(Steel)
    }
}

@Composable
private fun MenuHub(vm: MainViewModel, onExit: () -> Unit, onOpen: (Int) -> Unit) {
    val S = LocalStrings.current
    var confirmAll by remember { mutableStateOf(false) }
    val i = vm.info
    val suspSub = if (i.containsKey("rf")) "F ${i["rf"]}°–${i["xf"]}°   R ${i["rb"]}°–${i["xb"]}°" else "—"
    val batSub = when {
        (i["bo"] ?: 0) == 1 -> "${i["bp"]}%"
        i.containsKey("bo") -> S.bOffText
        else -> "—"
    }
    val sysSub = if (i.containsKey("v")) "v${i["v"]}" else "—"
    val effSub = "${POSES.size} • ${TEXTS.size} • ${SCENES.size}"

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        FactoryPreview()

        // وضعیت حالت کارخانه
        Section(Cyan, active = vm.factoryActive) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulseDot(vm.factoryActive, if (vm.factoryActive) Ok else Steel, 16.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Eyebrow("FACTORY MODE", Cyan.copy(alpha = 0.85f))
                    Text(S.menuTitle, color = Silver, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                }
                Text(
                    if (vm.factoryActive) S.facActive else S.facInactive,
                    color = if (vm.factoryActive) Ok else Steel, fontSize = 12.sp, fontWeight = FontWeight.Medium
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Hair))
            Text(if (vm.factoryActive) S.menuSub else S.facDesc, color = Steel, fontSize = 12.sp, lineHeight = 18.sp)
            if (!vm.connected) Text(S.facNeedConnect, color = Gold, fontSize = 12.sp)
        }

        ModuleRow(0, "SUSPENSION", S.mSuspension, suspSub, Cyan) { onOpen(1) }
        ModuleRow(1, "EXPRESSION", S.mEffects, effSub, Violet) { onOpen(2) }
        ModuleRow(2, "POWER", S.mBattery, batSub, Ok) { onOpen(3) }
        ModuleRow(3, "SYSTEM", S.mSystem, sysSub, Gold) { onOpen(4) }

        PillButton(
            label = S.sysReset, accent = Danger, filled = false, enabled = vm.connected,
            modifier = Modifier.fillMaxWidth(), height = 50.dp, onClick = { confirmAll = true }
        )
        PillButton(
            label = S.facExit, accent = Cyan, filled = true, enabled = true,
            modifier = Modifier.fillMaxWidth(), height = 54.dp, onClick = onExit
        )
        Spacer(Modifier.height(8.dp))
    }
    if (confirmAll) {
        LuxDialog(S.sysReset, null, { vm.resetCarDefaults(); confirmAll = false }, { confirmAll = false })
    }
}

// ======================================================================
//  تعلیق و سرووها
// ======================================================================

// دکمه‌ی کوچک − و + برای تنظیم دقیق (هر لمس = همان لحظه فرمان به سروو)
@Composable
private fun StepKey(label: String, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.size(width = 46.dp, height = 40.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(Surface4, Surface3)))
            .border(1.dp, Brush.verticalGradient(listOf(accent.copy(alpha = 0.7f), accent.copy(alpha = 0.15f))), shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TestChip(label: String, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.clip(shape).background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.5f), shape)
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = accent, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

// نوار بازه‌ی حرکت: از کف‌خواب تا حداکثر ، خط وسط = ارتفاع متوسط ، نقطه‌ی سفید = زاویه‌ی لحظه‌ای سروو
@Composable
private fun RangeStrip(rest: Int, mid: Int, max: Int, now: Int?, accent: Color) {
    Canvas(Modifier.fillMaxWidth().height(34.dp)) {
        val lo = 50f
        val hi = 150f
        val w = size.width
        val y = size.height * 0.45f
        val th = 6.dp.toPx()
        val rad = CornerRadius(th / 2f)
        val xr = w * ((rest - lo) / (hi - lo)).coerceIn(0f, 1f)
        val xm = w * ((mid - lo) / (hi - lo)).coerceIn(0f, 1f)
        val xx = w * ((max - lo) / (hi - lo)).coerceIn(0f, 1f)
        drawRoundRect(Hair, Offset(0f, y - th / 2f), Size(w, th), rad)
        if (xx > xr) {
            drawRoundRect(
                Brush.horizontalGradient(listOf(accent.copy(alpha = 0.25f), accent), startX = xr, endX = xx.coerceAtLeast(xr + 1f)),
                Offset(xr, y - th / 2f), Size(xx - xr, th), rad
            )
        }
        drawLine(accent, Offset(xm, y - 9.dp.toPx()), Offset(xm, y + 9.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
        for (k in 0..2) {
            val tx = w * k / 2f
            drawLine(Hair2, Offset(tx, y + 12.dp.toPx()), Offset(tx, y + 17.dp.toPx()), 1.dp.toPx())
        }
        if (now != null) {
            val nx = w * ((now - lo) / (hi - lo)).coerceIn(0f, 1f)
            drawCircle(Silver.copy(alpha = 0.25f), 9.dp.toPx(), Offset(nx, y))
            drawCircle(Silver, 4.5.dp.toPx(), Offset(nx, y))
        }
    }
}

// یک عدد زاویه: دکمه‌های −5 −1 +1 +5 (دقیق) + اسلایدر (سریع) + دکمه‌ی تست
@Composable
private fun AngleRow(
    label: String, value: Int, lo: Int, hi: Int, accent: Color,
    onChange: (Int) -> Unit, onTest: () -> Unit
) {
    val S = LocalStrings.current
    fun set(v: Int) { onChange(v.coerceIn(lo, hi)) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Silver, fontSize = 13.sp, modifier = Modifier.weight(1f))
            TestChip(S.sTest, accent) { onTest() }
        }
        // ردیف عددی فیزیکی است: منفی چپ ، مثبت راست
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepKey("−5", accent) { set(value - 5) }
                StepKey("−1", accent) { set(value - 1) }
                Box(Modifier.width(84.dp), contentAlignment = Alignment.Center) {
                    Text("$value°", color = accent, fontSize = 30.sp, fontWeight = FontWeight.Light, maxLines = 1)
                }
                StepKey("+1", accent) { set(value + 1) }
                StepKey("+5", accent) { set(value + 5) }
            }
        }
        Slider(
            value = value.coerceIn(lo, hi).toFloat(),
            onValueChange = { set(Math.round(it)) },
            valueRange = lo.toFloat()..hi.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Surface4
            )
        )
    }
}

@Composable
private fun AxleEditor(
    title: String, front: Boolean, accent: Color, nowAngle: Int?,
    rest: Int, mid: Int, max: Int,
    onRest: (Int) -> Unit, onMid: (Int) -> Unit, onMax: (Int) -> Unit, vm: MainViewModel
) {
    val S = LocalStrings.current
    Section(accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow(if (front) "FRONT AXLE" else "REAR AXLE", accent.copy(alpha = 0.85f))
                Text(title, color = Silver, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(S.sNow, color = Steel, fontSize = 10.5.sp)
                Text(nowAngle?.let { "$it°" } ?: "—", color = Silver, fontSize = 22.sp, fontWeight = FontWeight.Light)
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            RangeStrip(rest, mid, max, nowAngle, accent)
        }
        AngleRow(S.sRest, rest, 50, 110, accent, { onRest(it); vm.testServo(front, it) }, { vm.testServo(front, rest) })
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hair))
        AngleRow(S.sMid, mid, rest, max, accent, { onMid(it); vm.testServo(front, it) }, { vm.testServo(front, mid) })
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hair))
        AngleRow(S.sMax, max, rest + 5, 150, accent, { onMax(it); vm.testServo(front, it) }, { vm.testServo(front, max) })
    }
}

@Composable
private fun SuspensionPage(vm: MainViewModel, onBack: () -> Unit) {
    val S = LocalStrings.current
    val ready = vm.info.containsKey("rf")
    var rf by remember(ready, vm.syncTick) { mutableIntStateOf(vm.info["rf"] ?: 80) }
    var rb by remember(ready, vm.syncTick) { mutableIntStateOf(vm.info["rb"] ?: 84) }
    var xf by remember(ready, vm.syncTick) { mutableIntStateOf(vm.info["xf"] ?: 112) }
    var xb by remember(ready, vm.syncTick) { mutableIntStateOf(vm.info["xb"] ?: 115) }
    var mf by remember(ready, vm.syncTick) { mutableIntStateOf(vm.info["mf"] ?: 96) }
    var mb by remember(ready, vm.syncTick) { mutableIntStateOf(vm.info["mb"] ?: 100) }
    // تا وقتی این صفحه بازه، زاویه‌ی واقعی سروو از ماکت خونده میشه (ببینی واقعا رسیده یا نه)
    LaunchedEffect(Unit) {
        while (true) {
            vm.requestInfo()
            delay(700)
        }
    }
    var savedFlash by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    LaunchedEffect(savedFlash) {
        if (savedFlash) {
            delay(1800)
            savedFlash = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader(S.mSuspension, Cyan, onBack, "SUSPENSION")
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!ready) WaitingCard(vm)
            Text(S.sHint, color = Steel, fontSize = 12.sp, lineHeight = 18.sp)

            AxleEditor(
                S.front, true, Cyan, vm.info["pf"], rf, mf, xf,
                { rf = it; if (mf < it) mf = it; if (xf < it + 5) xf = it + 5 },
                { mf = it }, { xf = it; if (mf > it) mf = it }, vm
            )
            AxleEditor(
                S.rear, false, Violet, vm.info["pb"], rb, mb, xb,
                { rb = it; if (mb < it) mb = it; if (xb < it + 5) xb = it + 5 },
                { mb = it }, { xb = it; if (mb > it) mb = it }, vm
            )

            PillButton(
                label = S.sRelease, accent = Gold, filled = false, enabled = true,
                modifier = Modifier.fillMaxWidth(), height = 50.dp,
                onClick = { vm.releaseServos() }
            )
            PillButton(
                label = if (savedFlash) S.saved else S.sSave, accent = Ok, filled = true, enabled = ready,
                modifier = Modifier.fillMaxWidth(), height = 54.dp,
                onClick = { vm.applyServos(rf, rb, xf, xb, mf, mb); savedFlash = true }
            )
            CopyCodeButton { servoCode(rf, rb, xf, xb, mf, mb) }
            Text(S.sRam, color = Steel, fontSize = 11.sp, lineHeight = 16.sp)
            PillButton(
                label = S.sDefaults, accent = Danger, filled = false, enabled = true,
                modifier = Modifier.fillMaxWidth(), height = 46.dp, onClick = { confirmReset = true }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
    if (confirmReset) {
        LuxDialog(S.sDefaults, S.sysReset, { vm.resetCarDefaults(); confirmReset = false }, { confirmReset = false })
    }
}

// ======================================================================
//  چشم‌ها و افکت‌ها
// ======================================================================

@Composable
private fun UnderTab(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val a by animateFloatAsState(if (selected) 1f else 0f, tween(260), label = "uTab")
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label, color = lerp(Steel, accent, a), fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.width((28f * a).dp).height(2.dp).clip(RoundedCornerShape(1.dp))
                .background(accent.copy(alpha = a))
        )
    }
}

@Composable
private fun GridCell(item: Named, selected: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val S = LocalStrings.current
    val a by animateFloatAsState(if (selected) 1f else 0f, tween(260), label = "cell")
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.height(92.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(lerp(Surface2, accent, 0.18f * a), Panel)))
            .border(1.dp, lerp(Hair, accent, a), shape)
            .clickable { onClick() }.padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.08f + 0.16f * a)),
            contentAlignment = Alignment.Center
        ) {
            Text(item.icon, fontSize = 20.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            item.name(S), color = lerp(Silver, accent, a), fontSize = 10.5.sp,
            maxLines = 1, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ItemGrid(
    items: List<Named>, columns: Int, selected: Int, accent: Color, onPick: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in items.chunked(columns)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (item in row) {
                    GridCell(item, item.id == selected, accent, Modifier.weight(1f)) { onPick(item.id) }
                }
                for (k in 0 until (columns - row.size)) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TextRow(item: Named, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val S = LocalStrings.current
    val en = S.lang == "en"
    val a by animateFloatAsState(if (selected) 1f else 0f, tween(260), label = "txtRow")
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.horizontalGradient(listOf(lerp(Surface2, accent, 0.16f * a), Surface2)))
            .border(1.dp, lerp(Hair, accent, a), shape)
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            (item.id + 1).toString().padStart(2, '0'), color = accent.copy(alpha = 0.8f),
            fontSize = 12.sp, fontWeight = FontWeight.Light, modifier = Modifier.width(32.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(if (en) item.en else item.fa, color = Silver, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(if (en) item.fa else item.en, color = Steel, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TextList(items: List<Named>, selected: Int, accent: Color, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (item in items) {
            TextRow(item, item.id == selected, accent) { onPick(item.id) }
        }
    }
}

@Composable
private fun EffectsPage(vm: MainViewModel, onBack: () -> Unit) {
    val S = LocalStrings.current
    var tab by rememberSaveable { mutableStateOf(0) }
    var picked by remember { mutableStateOf(-1) }

    // تا وقتی این صفحه بازه، نمایشگر ماکت چشم‌ها رو نشون میده (به‌جای صحنه‌ی کارخانه)
    DisposableEffect(Unit) {
        vm.previewEyes(true)
        onDispose { vm.previewEyes(false) }
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader(S.mEffects, Violet, onBack, "EXPRESSION")
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulseDot(true, Violet, 14.dp)
                Spacer(Modifier.width(10.dp))
                Text(S.eLive, color = Steel, fontSize = 12.sp)
            }
            val names = listOf(S.eEyes, S.eReact, S.eScenes, S.eTexts)
            Column {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    for (k in names.indices) {
                        UnderTab(names[k], tab == k, Violet) { tab = k; picked = -1 }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Hair))
            }
            when (tab) {
                0 -> ItemGrid(POSES, 3, picked, Violet) { picked = it; vm.preview("POSE", it) }
                1 -> ItemGrid(REACTIONS, 3, picked, Violet) { picked = it; vm.preview("MICRO", it) }
                2 -> ItemGrid(SCENES, 2, picked, Violet) { picked = it; vm.preview("SCENE", it) }
                else -> {
                    Text(S.eNote, color = Gold, fontSize = 11.sp, lineHeight = 16.sp)
                    TextList(TEXTS, picked, Violet) { picked = it; vm.preview("TEXT", it) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ======================================================================
//  باتری
// ======================================================================

private fun levelText(S: Strings, bl: Int): String = when (bl) {
    4 -> S.lvFull
    3 -> S.lvGood
    2 -> S.lvMid
    1 -> S.lvLow
    else -> S.lvCrit
}

private fun levelColor(bl: Int): Color = when (bl) {
    4, 3 -> Ok
    2 -> Gold
    else -> Danger
}

@Composable
private fun BatteryPage(vm: MainViewModel, onBack: () -> Unit) {
    val S = LocalStrings.current
    val i = vm.info
    val ready = i.containsKey("bo")
    val supported = (i["bo"] ?: 0) >= 0
    val on = (i["bo"] ?: 0) == 1
    val pct = if (on) (i["bp"] ?: 0).coerceIn(0, 100) else 0
    val bl = i["bl"] ?: 4
    val col = if (on) levelColor(bl) else Steel

    var lo by remember(ready, vm.syncTick) { mutableIntStateOf(i["lo"] ?: 20) }
    var cr by remember(ready, vm.syncTick) { mutableIntStateOf(i["cr"] ?: 8) }
    var ca by remember(ready, vm.syncTick) { mutableIntStateOf(i["ca"] ?: 100) }
    var savedFlash by remember { mutableStateOf(false) }
    LaunchedEffect(savedFlash) {
        if (savedFlash) {
            delay(1800)
            savedFlash = false
        }
    }
    val sweep by animateFloatAsState(240f * pct / 100f, tween(300), label = "batSweep")

    Column(Modifier.fillMaxSize()) {
        PageHeader(S.mBattery, Ok, onBack, "POWER")
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!ready) WaitingCard(vm)
            if (ready && !supported) {
                Section(Gold) { Text(S.bNoFw, color = Gold, fontSize = 13.sp) }
            } else {
                // ---- گیج باتری: کمان ۲۴۰ درجه با خط‌های مقیاس ----
                Section(col, active = on) {
                    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(212.dp)) {
                            val sw = 14.dp.toPx()
                            val pad = 24.dp.toPx()
                            val tl = Offset(pad, pad)
                            val sz = Size(size.width - 2f * pad, size.height - 2f * pad)
                            val c = Offset(size.width / 2f, size.height / 2f)
                            drawArc(Hair2, 150f, 240f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                            if (on && sweep > 0.5f) {
                                drawArc(col.copy(alpha = 0.22f), 150f, sweep, false, tl, sz, style = Stroke(sw * 2f, cap = StrokeCap.Round))
                                drawArc(col, 150f, sweep, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                            }
                            val rOut = size.width / 2f - 3.dp.toPx()
                            for (k in 0..24) {
                                val a = Math.toRadians(150.0 + 240.0 * k / 24.0)
                                val rIn = rOut - (if (k % 6 == 0) 8.dp.toPx() else 4.dp.toPx())
                                val ca2 = Math.cos(a).toFloat()
                                val sa2 = Math.sin(a).toFloat()
                                drawLine(
                                    if (k % 6 == 0) Steel else Hair2,
                                    Offset(c.x + ca2 * rIn, c.y + sa2 * rIn),
                                    Offset(c.x + ca2 * rOut, c.y + sa2 * rOut), 1.2.dp.toPx(), StrokeCap.Round
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (on) "$pct%" else "--", color = col, fontSize = 46.sp, fontWeight = FontWeight.Light)
                            Text(
                                if (on) levelText(S, bl) else S.bOffText, color = Steel, fontSize = 13.sp
                            )
                            if (on) Text("${(i["bv"] ?: 0) / 10.0} V", color = Silver, fontSize = 13.sp, fontWeight = FontWeight.Light)
                        }
                    }
                }

                // ---- روشن / خاموش ----
                Section(Ok) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(S.bEnable, color = Silver, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(S.bEnableHint, color = Steel, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                        Switch(
                            checked = on, onCheckedChange = { vm.setBatteryEnabled(it) }, enabled = ready,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Bg, checkedTrackColor = Ok, checkedBorderColor = Ok,
                                uncheckedThumbColor = Steel, uncheckedTrackColor = Surface3, uncheckedBorderColor = Hair2
                            )
                        )
                    }
                }

                // ---- آستانه‌ها ----
                Section(Cyan) {
                    Eyebrow("THRESHOLDS", Cyan.copy(alpha = 0.85f))
                    ValueSlider(S.bLow, lo, 8, 60, Gold, "%", { lo = it; if (cr > it - 3) cr = it - 3 }, {})
                    ValueSlider(S.bCrit, cr, 2, 30, Danger, "%", { cr = it; if (lo < it + 3) lo = it + 3 }, {})
                    ValueSlider(S.bCal + "  (×0.01)", ca, 50, 200, Cyan, "", { ca = it }, {})
                    PillButton(
                        label = if (savedFlash) S.saved else S.sSave, accent = Ok, filled = true, enabled = ready,
                        modifier = Modifier.fillMaxWidth(), height = 52.dp,
                        onClick = { vm.applyBattery(lo, cr, ca); savedFlash = true }
                    )
                    CopyCodeButton { batteryCode(on, lo, cr, ca) }
                    Text(S.sRam, color = Steel, fontSize = 11.sp, lineHeight = 16.sp)
                }

                // ---- شبیه‌سازی (برای دیدن افکت‌ها و الارم بدون باتری واقعی) ----
                Section(Violet) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(S.bSim, color = Silver, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Eyebrow("TEST", Violet.copy(alpha = 0.85f))
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (p in listOf(100, 60, 35, 15, 5)) {
                            SimChip("$p%") { vm.simBattery(p) }
                        }
                        SimChip(S.bReal) { vm.simBattery(-1) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SimChip(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.clip(shape).background(Violet.copy(alpha = 0.08f)).border(1.dp, Violet.copy(alpha = 0.55f), shape)
            .clickable { onClick() }.padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Violet, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ======================================================================
//  سیستم
// ======================================================================

@Composable
private fun SystemPage(vm: MainViewModel, onBack: () -> Unit) {
    val S = LocalStrings.current
    val i = vm.info
    val ready = i.containsKey("v")
    var confirm by remember { mutableStateOf(0) }          // 1 = برگشت به پیش‌فرض ، 2 = ری‌استارت

    val modeText = when {
        (i["sh"] ?: 0) == 1 -> S.modeShow
        (i["fa"] ?: 0) == 1 -> S.modeFactory
        else -> S.modeNormal
    }
    val moodText = when (i["mo"] ?: 0) {
        1 -> S.moodHappy
        2 -> S.moodAnnoyed
        else -> S.moodCalm
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader(S.mSystem, Gold, onBack, "SYSTEM")
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!ready) WaitingCard(vm)

            // نمای کلی: شماره‌ی نسخه‌ی بزرگ + حالت
            Section(Gold) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow("FIRMWARE", Gold.copy(alpha = 0.85f))
                        Text(if (ready) "v${i["v"]}" else "—", color = Silver, fontSize = 40.sp, fontWeight = FontWeight.Light)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Eyebrow("MODE", Steel)
                        Text(if (ready) modeText else "—", color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Column {
                    InfoRow(S.sysFirmware, if (ready) "v${i["v"]}" else "—")
                    InfoRow(S.sysMode, if (ready) modeText else "—")
                    InfoRow(S.sysMood, if (ready) moodText else "—")
                    InfoRow(S.sysLocked, if (ready) (if ((i["lk"] ?: 0) == 1) S.yes else S.no) else "—")
                    InfoRow(
                        S.sysDisplay, if (ready) (if ((i["od"] ?: 0) == 1) S.okText else S.problem) else "—",
                        if (ready && (i["od"] ?: 0) != 1) Danger else Silver
                    )
                    InfoRow("F / R", if (ready) "${i["pf"]}° / ${i["pb"]}°" else "—")
                }
            }
            PillButton(
                label = S.refresh, accent = Cyan, filled = false, enabled = true,
                modifier = Modifier.fillMaxWidth(), height = 50.dp, onClick = { vm.requestInfo() }
            )
            PillButton(
                label = S.sysRestart, accent = Gold, filled = false, enabled = true,
                modifier = Modifier.fillMaxWidth(), height = 50.dp, onClick = { confirm = 2 }
            )
            PillButton(
                label = S.sysReset, accent = Danger, filled = false, enabled = true,
                modifier = Modifier.fillMaxWidth(), height = 50.dp, onClick = { confirm = 1 }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
    if (confirm != 0) {
        LuxDialog(
            if (confirm == 1) S.sysReset else S.sysRestart, null,
            {
                if (confirm == 1) vm.resetCarDefaults() else vm.restartCar()
                confirm = 0
            },
            { confirm = 0 }
        )
    }
}
