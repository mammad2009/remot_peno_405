package com.mamad.peugeot405

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// ایموت‌های چشم ماکت (شماره‌ی حالت ، ایموجی) ؛ محتوای ماکت‌اند و همین‌طور می‌مانند
private val EMOTES = listOf(
    7 to "😊", 9 to "🤩", 25 to "😂", 28 to "😍", 16 to "😉", 14 to "😲", 15 to "😱",
    10 to "😠", 11 to "😢", 20 to "😏", 32 to "😈", 30 to "😵", 12 to "🥱"
)

// ======================================================================
//  «i-Cockpit 405» : کابین بازی ماکت  (طراحی Maison 405)
//  سه حالت: کج کردن (گوشی = فرمان) ، پد لمسی (بدنه + چشم‌ها) ، رقص با موسیقی  +  حافظه‌ی M1 M2 M3
// ======================================================================
@Composable
fun GamePanel(vm: MainViewModel) {
    val S = LocalStrings.current
    val en = S.lang == "en"
    val g = vm.game
    fun tr(fa: String, e: String) = if (en) e else fa
    val blocked = vm.locked ?: ((vm.info["lk"] ?: 0) == 1)      // قفل: از آخرین دکمه‌ی ریموت ، وگرنه از وضعیت ماکت

    // شیر و نوارهای بالای صفحه همراه بدنه‌ی واقعی حرکت کنند
    LaunchedEffect(Unit) {
        while (true) {
            val rf = vm.info["rf"] ?: 80
            val rb = vm.info["rb"] ?: 84
            val xf = vm.info["xf"] ?: 112
            val xb = vm.info["xb"] ?: 115
            val hh = g.h / 100f
            val sw = g.d * 0.12f
            val f = (rf + (xf - rf) * hh + sw).coerceIn(rf.toFloat(), xf.toFloat())
            val b = (rb + (xb - rb) * hh - sw).coerceIn(rb.toFloat(), xb.toFloat())
            vm.sim.jumpTo(f.roundToInt(), b.roundToInt())
            delay(60)
        }
    }
    // سنسور فقط وقتی تب «کج کردن» جلوی چشمه روشنه
    DisposableEffect(g.sub, vm.foreground) {
        if (g.sub == 0 && vm.foreground) g.tiltStart()
        onDispose { g.tiltStop() }
    }
    LaunchedEffect(vm.foreground) { if (!vm.foreground) g.stopBeat() }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CpHeader(vm, en)
        CpTabs(g.sub, listOf(tr("کج کردن", "Tilt"), tr("پد لمسی", "Pad"), tr("رقص", "Dance"))) { g.sub = it }

        Box {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CpCard {
                    when (g.sub) {
                        0 -> CpTilt(g, en)
                        1 -> CpPad(g, en)
                        else -> CpBeat(g, en)
                    }
                }
                CpMemory(g, en)
                CpSound(g, en)
            }
            if (blocked) {
                Box(
                    Modifier.matchParentSize().clip(RoundedCornerShape(26.dp)).background(Color(0xE6050607))
                        .pointerInput(Unit) { detectTapGestures { } },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Canvas(Modifier.size(38.dp)) {
                            drawPadlock(Offset(size.width / 2f, size.height / 2f), size.width * 0.9f, Danger, false, 2.dp.toPx())
                        }
                        Text(tr("ماشین قفله", "The car is locked"), color = Danger, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            tr("از ریموت پژو بازش کن، بعد بازی کن", "Unlock it from the remote first"),
                            color = Steel, fontSize = 12.sp, textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ---------- اجزای مشترک ----------

@Composable
private fun CpCard(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().luxPanel(RoundedCornerShape(26.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) { content() }
}

@Composable
private fun CpChip(text: String, selected: Boolean, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.clip(shape)
            .background(if (selected) accent.copy(alpha = 0.16f) else Surface3)
            .border(1.dp, if (selected) accent else Hair2, shape)
            .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, color = if (selected) accent else Silver, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1
        )
    }
}

// ---------- هدر: حال و حرف‌های ماکت ----------
@Composable
private fun CpHeader(vm: MainViewModel, en: Boolean) {
    val S = LocalStrings.current
    val g = vm.game
    val shape = RoundedCornerShape(24.dp)
    val mood = POSES.getOrNull(g.mood)?.icon ?: "🤖"
    val words = TEXTS.getOrNull(g.wordsIdx)?.name(S)
    val state = when {
        g.recording -> "REC"
        g.playing >= 0 -> "PLAY"
        g.beat.active -> "BEAT"
        else -> "LIVE"
    }
    val stCol = when {
        g.recording -> Danger
        g.playing >= 0 -> Ok
        g.beat.active -> Gold
        else -> Cyan
    }
    Row(
        Modifier.fillMaxWidth().luxPanel(shape, Cyan).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center
                val r = size.minDimension / 2f - 1.dp.toPx()
                drawCircle(Cyan.copy(alpha = 0.10f), r, c)
                drawCircle(Hair2, r, c, style = Stroke(1.dp.toPx()))
                drawArc(
                    Cyan, -90f, 70f, false, Offset(c.x - r, c.y - r), Size(r * 2f, r * 2f),
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(mood, fontSize = 28.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("i-COCKPIT  405", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 3.sp)
            Text(
                words?.let { "💬 $it" } ?: if (en) "Your phone is the cockpit. Drive the car's body, eyes and mood."
                else "گوشی تو کابینه؛ بدنه، چشم‌ها و حال ماکت دست توئه.",
                color = Silver, fontSize = 12.sp, lineHeight = 17.sp
            )
        }
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(stCol.copy(alpha = 0.12f))
                .border(1.dp, stCol.copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(stCol))
            Spacer(Modifier.width(6.dp))
            Text(state, color = stCol, fontSize = 9.5.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ---------- تب‌ها با نشانگر لغزنده ----------
@Composable
private fun CpTabs(sel: Int, labels: List<String>, onSel: (Int) -> Unit) {
    val accents = listOf(Cyan, Violet, Gold)
    val pos by animateFloatAsState(sel.toFloat(), tween(320), label = "cpTab")
    val i = pos.toInt().coerceIn(0, 2)
    val col = lerp(accents[i], accents[min(i + 1, 2)], (pos - i).coerceIn(0f, 1f))
    val shape = RoundedCornerShape(22.dp)
    // نشانگر فیزیکی است: همیشه از چپ به راست
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            Modifier.fillMaxWidth().luxPanel(shape).drawBehind {
                val inset = 4.dp.toPx()
                val w = (size.width - inset * 2f) / 3f
                val x = inset + w * pos
                drawRoundRect(col.copy(alpha = 0.16f), Offset(x, inset), Size(w, size.height - inset * 2f), CornerRadius(18.dp.toPx()))
                drawRoundRect(
                    col, Offset(x + w * 0.3f, size.height - inset - 3.dp.toPx()),
                    Size(w * 0.4f, 2.dp.toPx()), CornerRadius(1.dp.toPx())
                )
            }.padding(4.dp)
        ) {
            Row {
                for (k in 0..2) {
                    val tint = lerp(Steel, accents[k], (1f - abs(pos - k)).coerceIn(0f, 1f))
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable { onSel(k) }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Canvas(Modifier.size(22.dp)) {
                            when (k) {
                                0 -> cpIconTilt(tint)
                                1 -> cpIconPad(tint)
                                else -> cpIconBars(tint)
                            }
                        }
                        Text(
                            labels[k], color = tint, fontSize = 11.5.sp, letterSpacing = 0.3.sp,
                            fontWeight = if (sel == k) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// آیکن‌های خطی تب‌ها
private fun DrawScope.cpIconTilt(c: Color) {
    val w = size.width
    val h = size.height
    val sw = w * 0.085f
    rotate(-18f, Offset(w / 2f, h / 2f)) {
        drawRoundRect(c, Offset(w * 0.30f, h * 0.10f), Size(w * 0.40f, h * 0.80f), CornerRadius(w * 0.09f), style = Stroke(sw))
        drawLine(c, Offset(w * 0.43f, h * 0.20f), Offset(w * 0.57f, h * 0.20f), sw, StrokeCap.Round)
    }
    drawArc(
        c.copy(alpha = 0.6f), 200f, 50f, false, Offset(w * 0.02f, h * 0.02f), Size(w * 0.96f, h * 0.96f),
        style = Stroke(sw * 0.8f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.cpIconPad(c: Color) {
    val w = size.width
    val h = size.height
    val sw = w * 0.085f
    val ctr = Offset(w / 2f, h / 2f)
    drawCircle(c, w * 0.40f, ctr, style = Stroke(sw))
    drawLine(c.copy(alpha = 0.6f), Offset(w * 0.10f, h / 2f), Offset(w * 0.90f, h / 2f), sw * 0.7f, StrokeCap.Round)
    drawLine(c.copy(alpha = 0.6f), Offset(w / 2f, h * 0.10f), Offset(w / 2f, h * 0.90f), sw * 0.7f, StrokeCap.Round)
    drawCircle(c, w * 0.08f, ctr)
}

private fun DrawScope.cpIconBars(c: Color) {
    val w = size.width
    val h = size.height
    val sw = w * 0.11f
    val hs = listOf(0.35f, 0.75f, 0.5f, 0.9f)
    for (k in 0..3) {
        val x = w * (0.15f + 0.233f * k)
        val half = h * 0.5f * hs[k]
        drawLine(c, Offset(x, h / 2f - half), Offset(x, h / 2f + half), sw, StrokeCap.Round)
    }
}

// ======================================================================
// ۱) کج کردن: گوشی = فرمان. جلو/عقب کج کن → الاکلنگی ؛ نوار کناری = ارتفاع
// ======================================================================
@Composable
private fun CpTilt(g: GameController, en: Boolean) {
    fun tr(fa: String, e: String) = if (en) e else fa
    if (!g.tilt.available) {
        Text(tr("این گوشی سنسور کج شدن ندارد؛ از «پد لمسی» استفاده کن", "No tilt sensor on this phone; use the Pad"), color = Danger, fontSize = 13.sp)
        return
    }
    Row(Modifier.fillMaxWidth().height(224.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center
                val r = min(size.width, size.height) / 2f - 6.dp.toPx()
                drawCircle(Brush.radialGradient(listOf(Surface3, Panel), center = c, radius = r), r, c)
                drawCircle(Hair2, r, c, style = Stroke(1.dp.toPx()))
                // مقیاس دور صفحه
                for (k in 0 until 36) {
                    val a = k * 10.0 * PI / 180.0
                    val big = k % 9 == 0
                    val r1 = r - (if (big) 10.dp.toPx() else 5.dp.toPx())
                    drawLine(
                        if (big) Cyan.copy(alpha = 0.8f) else Hair2,
                        Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat()),
                        Offset(c.x + (r1 * cos(a)).toFloat(), c.y + (r1 * sin(a)).toFloat()),
                        1.dp.toPx()
                    )
                }
                drawCircle(Hair, r * 0.55f, c, style = Stroke(1.dp.toPx()))
                drawCircle(Hair, r * 0.28f, c, style = Stroke(1.dp.toPx()))
                drawLine(Hair, Offset(c.x - r * 0.8f, c.y), Offset(c.x + r * 0.8f, c.y), 1.dp.toPx())
                drawLine(Hair, Offset(c.x, c.y - r * 0.8f), Offset(c.x, c.y + r * 0.8f), 1.dp.toPx())
                val rx = ((g.tilt.roll - g.zeroR) / 28f).coerceIn(-1f, 1f)
                val ry = (g.d / 100f).coerceIn(-1f, 1f)
                val p = Offset(c.x + rx * r * 0.72f, c.y - ry * r * 0.72f)
                drawLine(Gold.copy(alpha = 0.35f), c, p, 1.5.dp.toPx(), StrokeCap.Round)
                drawCircle(Gold.copy(alpha = 0.22f), 26.dp.toPx(), p)
                drawCircle(Gold, 10.dp.toPx(), p)
                drawCircle(Color(0xFF1A1405), 3.dp.toPx(), p)
            }
            Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PITCH", color = Steel, fontSize = 8.5.sp, letterSpacing = 3.sp)
                Text(
                    (if (g.d > 0) "+" else "") + g.d + "%",
                    color = Silver, fontSize = 18.sp, fontWeight = FontWeight.Light
                )
            }
        }
        CpHeightBar(g.h, Cyan, tr("ارتفاع", "Height"), { g.userHeight(it) }, Modifier.width(64.dp).fillMaxHeight())
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CpChip(tr("صفر کن", "Zero"), false, Cyan, Modifier.weight(1f)) { g.calibrate() }
        CpChip(tr("برعکس", "Flip"), g.invert, Violet, Modifier.weight(1f)) { g.updateInvert(!g.invert) }
        CpChip(tr("ارتفاع با رول", "Height by roll"), g.heightByRoll, Gold, Modifier.weight(1.2f)) { g.updateHeightByRoll(!g.heightByRoll) }
    }
    Text(
        tr(
            "گوشی رو جوری که راحتی بگیر و «صفر کن» رو بزن. بالای گوشی رو عقب ببر = جلوی ماکت بالا میاد. نوار کناری ارتفاع کل ماکته.",
            "Hold the phone comfortably and tap Zero. Lean the top back = the nose rises. The side bar sets the overall height."
        ),
        color = Steel, fontSize = 11.5.sp, lineHeight = 16.sp
    )
}

@Composable
private fun CpHeightBar(value: Int, accent: Color, label: String, onChange: (Int) -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(24.dp)
    val cb by rememberUpdatedState(onChange)
    Box(
        modifier.luxPanel(shape, accent)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val hgt = size.height.toFloat().coerceAtLeast(1f)
                    val down = awaitFirstDown()
                    cb(((1f - down.position.y / hgt) * 100f).roundToInt().coerceIn(0, 100))
                    do {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull() ?: break
                        if (c.pressed) {
                            cb(((1f - c.position.y / hgt) * 100f).roundToInt().coerceIn(0, 100))
                            c.consume()
                        }
                    } while (ev.changes.any { it.pressed })
                }
            }
    ) {
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight((value / 100f).coerceAtLeast(0.02f))
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.18f))))
        )
        Canvas(Modifier.fillMaxSize()) {
            for (k in 0..10) {
                val y = size.height * k / 10f
                val len = if (k % 5 == 0) 12.dp.toPx() else 6.dp.toPx()
                drawLine(Color(0x66000000), Offset(0f, y), Offset(len, y), 1.dp.toPx())
                drawLine(Color(0x66000000), Offset(size.width, y), Offset(size.width - len, y), 1.dp.toPx())
            }
        }
        Column(Modifier.align(Alignment.TopCenter).padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", color = Silver, fontSize = 20.sp, fontWeight = FontWeight.Light)
            Text(label, color = Steel, fontSize = 9.sp, letterSpacing = 1.sp)
        }
    }
}

// ======================================================================
// ۲) پد لمسی: چپ = بدنه (بالا/پایین = ارتفاع ، چپ/راست = الاکلنگی) ؛ راست = نگاه چشم‌ها
// ======================================================================
@Composable
private fun CpPad(g: GameController, en: Boolean) {
    fun tr(fa: String, e: String) = if (en) e else fa
    Row(Modifier.fillMaxWidth().height(190.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CpPadBox(
            tr("بدنه", "BODY"), g.d / 100f, g.h / 50f - 1f, true, Cyan,
            { x, y -> g.userBody(((y + 1f) * 50f).roundToInt(), (x * 100f).roundToInt()) }, { },
            Modifier.weight(1f).fillMaxHeight()
        )
        CpPadBox(
            tr("چشم‌ها", "EYES"), g.ex / 100f, -g.ey / 100f, g.eyesHeld, Violet,
            { x, y -> g.userEyes((x * 100f).roundToInt(), (-y * 100f).roundToInt()) }, { g.eyesRelease() },
            Modifier.weight(1f).fillMaxHeight()
        )
    }
    Text(
        tr(
            "پد بدنه: بالا = ماکت بلند ، راست = جلو بالا ، چپ = عقب بالا (وقتی ول کنی همون‌جا میمونه). پد چشم‌ها: انگشتت رو بذار، ماکت نگاهت میکنه!",
            "Body pad: up = taller, right = nose up, left = tail up (it stays when you let go). Eye pad: hold a finger down and the car watches it!"
        ),
        color = Steel, fontSize = 11.5.sp, lineHeight = 16.sp
    )
    Text("EMOTES", color = Violet.copy(alpha = 0.8f), fontSize = 9.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Medium)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(EMOTES) { (id, e) ->
            Box(
                Modifier.size(48.dp).luxPanel(CircleShape, Violet).clickable { g.emote(id) },
                contentAlignment = Alignment.Center
            ) { Text(e, fontSize = 22.sp) }
        }
    }
}

@Composable
private fun CpPadBox(
    title: String, px: Float, py: Float, active: Boolean, accent: Color,
    onMove: (Float, Float) -> Unit, onEnd: () -> Unit, modifier: Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    val move by rememberUpdatedState(onMove)
    val end by rememberUpdatedState(onEnd)
    Box(
        modifier.luxPanel(shape, accent, active)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val hh = size.height.toFloat().coerceAtLeast(1f)
                    val down = awaitFirstDown()
                    move((down.position.x / w * 2f - 1f).coerceIn(-1f, 1f), (1f - down.position.y / hh * 2f).coerceIn(-1f, 1f))
                    do {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull() ?: break
                        if (c.pressed) {
                            move((c.position.x / w * 2f - 1f).coerceIn(-1f, 1f), (1f - c.position.y / hh * 2f).coerceIn(-1f, 1f))
                            c.consume()
                        }
                    } while (ev.changes.any { it.pressed })
                    end()
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            // شبکه‌ی نقطه‌ای
            for (gx in 1..5) {
                for (gy in 1..5) {
                    drawCircle(Hair2, 1.2.dp.toPx(), Offset(size.width * gx / 6f, size.height * gy / 6f))
                }
            }
            drawLine(Hair, Offset(0f, c.y), Offset(size.width, c.y), 1.dp.toPx())
            drawLine(Hair, Offset(c.x, 0f), Offset(c.x, size.height), 1.dp.toPx())
            drawCircle(Hair, size.minDimension * 0.33f, c, style = Stroke(1.dp.toPx()))
            val p = Offset(
                c.x + px.coerceIn(-1f, 1f) * (size.width / 2f - 22.dp.toPx()),
                c.y - py.coerceIn(-1f, 1f) * (size.height / 2f - 22.dp.toPx())
            )
            drawCircle(accent.copy(alpha = if (active) 0.30f else 0.12f), 28.dp.toPx(), p)
            drawCircle(if (active) accent else accent.copy(alpha = 0.55f), 11.dp.toPx(), p)
            drawCircle(Color(0xFF07080B), 3.dp.toPx(), p)
        }
        Text(
            title, color = accent, fontSize = 9.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
        )
        Text(
            "${(px * 100f).roundToInt()} , ${(py * 100f).roundToInt()}", color = Steel, fontSize = 10.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
        )
    }
}

// ======================================================================
// ۳) رقص با موسیقی: میکروفون گوشی ضرب رو میگیره ، ماکت میپره و چراغ‌ها نوبتی میشن
// ======================================================================
@Composable
private fun CpBeat(g: GameController, en: Boolean) {
    fun tr(fa: String, e: String) = if (en) e else fa
    val ctx = LocalContext.current
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) { denied = !g.startBeat() } else denied = true
    }
    fun toggle() {
        if (g.beat.active) { g.stopBeat(); return }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            denied = !g.startBeat()
        } else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(g.beat.count) {
        if (g.beat.count > 0) {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(320))
        }
    }
    var sens by remember { mutableStateOf(g.beat.sensitivity) }
    val inf = rememberInfiniteTransition(label = "cpBeat")
    val tt by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "cpBeatT")

    Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val base = size.minDimension * 0.27f
            val lv = g.beat.level
            val fl = flash.value
            drawCircle(Gold.copy(alpha = 0.05f + 0.16f * lv + 0.08f * fl), base * (1.95f + 0.3f * fl), c)
            drawCircle(Gold.copy(alpha = 0.5f * fl), base * (1.25f + 0.5f * (1f - fl)), c, style = Stroke(2.dp.toPx()))
            // حلقه‌ی سطح صدا: ۴۸ خط که با بلندی صدا روشن میشن
            val n = 48
            for (k in 0 until n) {
                val a = (k.toDouble() / n) * 2.0 * PI - PI / 2.0
                val lit = k.toFloat() / n < lv
                val r0 = base * 1.38f
                val r1 = r0 + (if (lit) 12.dp.toPx() else 6.dp.toPx())
                drawLine(
                    if (lit) Gold else Hair2,
                    Offset(c.x + (r0 * cos(a)).toFloat(), c.y + (r0 * sin(a)).toFloat()),
                    Offset(c.x + (r1 * cos(a)).toFloat(), c.y + (r1 * sin(a)).toFloat()),
                    1.6.dp.toPx(), StrokeCap.Round
                )
            }
            drawCircle(
                Brush.radialGradient(listOf(Surface3, Panel), center = c, radius = base * 1.2f),
                base * (1f + 0.06f * fl), c
            )
            drawCircle(Gold.copy(alpha = 0.7f), base * (1f + 0.06f * fl), c, style = Stroke(1.5.dp.toPx()))
            if (g.beat.active) {
                drawArc(
                    Gold, tt * 360f, 60f, false, Offset(c.x - base * 1.12f, c.y - base * 1.12f), Size(base * 2.24f, base * 2.24f),
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (g.beat.active && g.beat.bpm > 0) "${g.beat.bpm}" else "♪",
                color = Silver, fontSize = 38.sp, fontWeight = FontWeight.Light
            )
            Text(
                if (g.beat.active) "BPM" else tr("آماده", "READY"),
                color = Gold, fontSize = 9.5.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Medium
            )
        }
    }
    // موج صدا
    Canvas(Modifier.fillMaxWidth().height(44.dp)) {
        val bars = 40
        val bw = size.width / bars
        val lv = g.beat.level
        val ph = tt * 2f * PI.toFloat()
        for (k in 0 until bars) {
            val amp = 0.10f + 0.90f * lv * abs(sin(ph * 2f + k * 0.45f))
            val bh = (size.height * amp).coerceAtLeast(3.dp.toPx())
            drawRoundRect(
                if (g.beat.active) Gold.copy(alpha = 0.35f + 0.55f * amp) else Hair2,
                Offset(k * bw + bw * 0.25f, (size.height - bh) / 2f), Size(bw * 0.5f, bh), CornerRadius(2.dp.toPx())
            )
        }
    }
    PillButton(
        label = if (g.beat.active) "■  " + tr("توقف رقص", "Stop dancing") else tr("شروع رقص با موسیقی", "Start dancing"),
        accent = Gold, filled = g.beat.active, enabled = true,
        modifier = Modifier.fillMaxWidth(), height = 52.dp, onClick = { toggle() }
    )
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("حساسیت ضرب", "Beat sensitivity"), color = Steel, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("${(sens * 100f).roundToInt()}%", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Light)
        }
        Slider(
            value = sens, onValueChange = { sens = it; g.beat.sensitivity = it },
            colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Hair2)
        )
    }
    Text(
        if (denied) tr("اجازه‌ی میکروفون داده نشد؛ از تنظیمات گوشی بده", "Microphone permission denied; allow it in phone settings")
        else tr(
            "یک آهنگ از بلندگوی گوشی (یا نزدیک گوشی) پخش کن؛ هر ضرب = یک پرش بدنه + تپش چشم‌ها + چراغ‌های نوبتی. وسط رقص صدای ماکت قطع میشه.",
            "Play a song from the phone speaker (or nearby). Each beat = a body hop, eye pulse and alternating lights. The car's own sounds mute while dancing."
        ),
        color = if (denied) Danger else Steel, fontSize = 11.5.sp, lineHeight = 16.sp
    )
}

// ======================================================================
//  حافظه (مثل صندلی حافظه‌دار پژو): ضبط هر حرکت ، ضرب ، نگاه و ایموجی و پخش دوباره
// ======================================================================
@Composable
private fun CpMemory(g: GameController, en: Boolean) {
    fun tr(fa: String, e: String) = if (en) e else fa
    CpCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 3.dp, height = 14.dp).clip(RoundedCornerShape(2.dp)).background(Mint))
            Spacer(Modifier.width(10.dp))
            Text("MEMORY", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 3.sp, modifier = Modifier.weight(1f))
            CpChip("∞  " + tr("تکرار", "Loop"), g.loop, Mint) { g.loop = !g.loop }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            // دکمه‌ی ضبط
            val recShape = RoundedCornerShape(20.dp)
            Column(
                Modifier.width(88.dp).height(70.dp).clip(recShape)
                    .background(if (g.recording) Danger.copy(alpha = 0.16f) else Surface3)
                    .border(1.dp, if (g.recording) Danger else Hair2, recShape)
                    .clickable { if (g.recording) g.stopRec(true) else g.startRec() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Canvas(Modifier.size(16.dp)) {
                    if (g.recording) drawCircle(Danger, size.minDimension / 2f)
                    else drawCircle(Danger, size.minDimension / 2f - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    if (g.recording) "■ ${g.recSecs}s" else "REC",
                    color = if (g.recording) Danger else Silver, fontSize = 11.sp,
                    letterSpacing = if (g.recording) 0.sp else 3.sp, fontWeight = FontWeight.Medium
                )
            }
            // سه خانه‌ی حافظه
            for (i in 0..2) {
                val rec = g.slots[i]
                val filled = rec != null
                val sel = g.slotSel == i
                val isPlaying = g.playing == i
                val shape = RoundedCornerShape(20.dp)
                val col = if (isPlaying) Ok else Mint
                val on = sel || isPlaying
                Column(
                    Modifier.weight(1f).height(70.dp).clip(shape)
                        .background(if (on) col.copy(alpha = 0.14f) else Surface3)
                        .border(if (on) 1.5.dp else 1.dp, if (on) col else Hair2, shape)
                        .pointerInput(i) {
                            detectTapGestures(
                                onTap = {
                                    g.slotSel = i
                                    if (!g.recording && g.slots[i] != null) {
                                        if (g.playing == i) g.stopPlay() else g.play(i)
                                    }
                                },
                                onLongPress = { if (!g.recording) g.deleteSlot(i) }
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "M${i + 1}", color = if (on) col else Silver, fontSize = 17.sp, fontWeight = FontWeight.Light
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        when {
                            isPlaying -> "▶ PLAY"
                            rec != null -> "● ${rec.dur / 1000}s"
                            else -> "—"
                        },
                        color = if (isPlaying) Ok else if (filled) Mint else Steel, fontSize = 10.sp, letterSpacing = 1.sp
                    )
                }
            }
        }
        Text(
            when {
                g.recording -> tr("در حال ضبط → M${g.slotSel + 1} ؛ هر کاری با کج کردن، پد یا رقص بکنی ثبت میشه. دوباره بزن تا تموم شه.", "Recording → M${g.slotSel + 1}. Everything you do with tilt, pads or dance is captured. Tap again to finish.")
                g.playing >= 0 -> tr("M${g.playing + 1} در حال پخشه؛ کنترل‌ها قفل‌اند. دوباره روش بزن تا قطع شه.", "M${g.playing + 1} is playing; controls are locked. Tap it again to stop.")
                else -> tr("یک خونه رو انتخاب کن و ⏺ بزن. روی خونه‌ی پر بزن = پخش ، نگه داشتن = پاک کردن.", "Pick a slot and press ⏺. Tap a filled slot = play, long-press = delete.")
            },
            color = Steel, fontSize = 11.5.sp, lineHeight = 16.sp
        )
    }
}

// ---------- صدا ----------
@Composable
private fun CpToggle(label: String, on: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.luxPanel(RoundedCornerShape(20.dp), if (on) accent else Silver, on)
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label, color = if (on) Silver else Steel, fontSize = 12.sp, maxLines = 1,
            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Canvas(Modifier.size(width = 30.dp, height = 16.dp)) {
            val h = size.height
            drawRoundRect(
                if (on) accent.copy(alpha = 0.35f) else Hair2, Offset.Zero, Size(size.width, h), CornerRadius(h / 2f)
            )
            drawCircle(if (on) accent else Steel, h / 2f - 2.dp.toPx(), Offset(if (on) size.width - h / 2f else h / 2f, h / 2f))
        }
    }
}

@Composable
private fun CpSound(g: GameController, en: Boolean) {
    fun tr(fa: String, e: String) = if (en) e else fa
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CpToggle(tr("صدای احساسات", "Mood sounds"), g.voice.chirps, Cyan, Modifier.weight(1f)) { g.setChirps(!g.voice.chirps) }
            CpToggle(tr("ماکت حرف بزنه", "Let it talk"), g.voice.speak, Violet, Modifier.weight(1f)) { g.setSpeak(!g.voice.speak, en) }
        }
        if (g.voice.speak && !g.voice.speakReady) {
            Text(
                tr("صدای این زبان روی گوشی نصب نیست (تنظیمات گوشی ← تبدیل متن به گفتار)", "This language's voice isn't installed (Phone settings → Text-to-speech)"),
                color = Gold, fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}
