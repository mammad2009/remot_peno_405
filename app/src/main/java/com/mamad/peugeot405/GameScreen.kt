package com.mamad.peugeot405

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt

private val EMOTES = listOf(
    7 to "😊", 9 to "🤩", 25 to "😂", 28 to "😍", 16 to "😉", 14 to "😲", 15 to "😱",
    10 to "😠", 11 to "😢", 20 to "😏", 32 to "😈", 30 to "😵", 12 to "🥱"
)
private val Line = Color(0x22FFFFFF)

// ======================================================================
//  «i-Cockpit 405» : کابین بازی ماکت
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CockpitHeader(vm, en)

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel)
                .border(1.dp, Line, RoundedCornerShape(22.dp)).padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            GameTab("🧭", tr("کج کردن", "Tilt"), g.sub == 0, Cyan, Modifier.weight(1f)) { g.sub = 0 }
            GameTab("👆", tr("پد لمسی", "Pad"), g.sub == 1, Violet, Modifier.weight(1f)) { g.sub = 1 }
            GameTab("🎵", tr("رقص", "Dance"), g.sub == 2, Gold, Modifier.weight(1f)) { g.sub = 2 }
        }

        Box {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GameCard {
                    when (g.sub) {
                        0 -> TiltPane(g, en)
                        1 -> PadPane(g, en)
                        else -> BeatPane(g, en)
                    }
                }
                MemoryBar(g, en)
                SoundRow(g, en)
            }
            if (blocked) {
                Box(
                    Modifier.matchParentSize().clip(RoundedCornerShape(22.dp)).background(Color(0xE6060709))
                        .pointerInput(Unit) { detectTapGestures { } },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tr("🔒 ماشین قفله\nاز ریموت پژو بازش کن، بعد بازی کن", "🔒 The car is locked\nUnlock it from the remote first"),
                        color = Danger, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

// ---------- هدر: حال و حرف‌های ماکت ----------
@Composable
private fun CockpitHeader(vm: MainViewModel, en: Boolean) {
    val S = LocalStrings.current
    val g = vm.game
    val shape = RoundedCornerShape(22.dp)
    val mood = POSES.getOrNull(g.mood)?.icon ?: "🤖"
    val words = TEXTS.getOrNull(g.wordsIdx)?.name(S)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.horizontalGradient(listOf(Cyan.copy(alpha = 0.16f), Violet.copy(alpha = 0.12f))))
            .border(1.dp, Cyan.copy(alpha = 0.35f), shape).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(mood, fontSize = 38.sp)
        Column(Modifier.weight(1f)) {
            Text("i-COCKPIT  405", color = Cyan, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text(
                words?.let { "💬 $it" } ?: if (en) "Your phone is the cockpit. Drive the car's body, eyes and mood."
                else "گوشی تو کابینه؛ بدنه، چشم‌ها و حال ماکت دست توئه.",
                color = Silver, fontSize = 12.sp, lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun GameTab(icon: String, label: String, selected: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.clip(shape).background(if (selected) accent.copy(alpha = 0.2f) else Color.Transparent)
            .then(if (selected) Modifier.border(1.5.dp, accent, shape) else Modifier)
            .clickable { onClick() }.padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 19.sp)
        Text(label, color = if (selected) accent else Steel, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun GameCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Panel).border(1.dp, Line, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

@Composable
private fun GChip(text: String, selected: Boolean, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.clip(shape).background(if (selected) accent.copy(alpha = 0.22f) else Color(0x14FFFFFF))
            .border(1.dp, if (selected) accent else Line, shape)
            .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) accent else Silver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

// ======================================================================
//  ۱) کج کردن: گوشی = فرمان. جلو/عقب کج کن → الاکلنگی ؛ نوار کناری = ارتفاع
// ======================================================================
@Composable
private fun TiltPane(g: GameController, en: Boolean) {
    fun tr(fa: String, e: String) = if (en) e else fa
    if (!g.tilt.available) {
        Text(tr("این گوشی سنسور کج شدن ندارد؛ از «پد لمسی» استفاده کن", "No tilt sensor on this phone; use the Pad"), color = Danger, fontSize = 13.sp)
        return
    }
    Row(Modifier.fillMaxWidth().height(210.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxHeight().fillMaxWidth()) {
                val c = center
                val r = min(size.width, size.height) / 2f - 4.dp.toPx()
                drawCircle(Color(0x12FFFFFF), r, c)
                drawCircle(Cyan.copy(alpha = 0.45f), r, c, style = Stroke(1.5.dp.toPx()))
                drawCircle(Line, r * 0.5f, c, style = Stroke(1.dp.toPx()))
                drawLine(Line, Offset(c.x - r, c.y), Offset(c.x + r, c.y), 1.dp.toPx())
                drawLine(Line, Offset(c.x, c.y - r), Offset(c.x, c.y + r), 1.dp.toPx())
                val rx = ((g.tilt.roll - g.zeroR) / 28f).coerceIn(-1f, 1f)
                val ry = (g.d / 100f).coerceIn(-1f, 1f)
                val p = Offset(c.x + rx * r * 0.82f, c.y - ry * r * 0.82f)
                drawCircle(Gold.copy(alpha = 0.28f), 26.dp.toPx(), p)
                drawCircle(Gold, 11.dp.toPx(), p)
            }
            Text(
                (if (g.d > 0) "+" else "") + g.d + "%",
                color = Silver, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        HeightBar(g.h, Cyan, tr("ارتفاع", "Height"), { g.userHeight(it) }, Modifier.width(70.dp).fillMaxHeight())
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GChip("🎯 " + tr("صفر کن", "Zero"), false, Cyan) { g.calibrate() }
        GChip("⇅ " + tr("برعکس", "Flip"), g.invert, Violet) { g.updateInvert(!g.invert) }
        GChip("🎢 " + tr("ارتفاع با کج‌شدن چپ/راست", "Height by roll"), g.heightByRoll, Gold) { g.updateHeightByRoll(!g.heightByRoll) }
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
private fun HeightBar(value: Int, accent: Color, label: String, onChange: (Int) -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(20.dp)
    val cb by rememberUpdatedState(onChange)
    Box(
        modifier.clip(shape).background(Color(0x14FFFFFF)).border(1.dp, Line, shape)
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
                .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.3f))))
        )
        Column(Modifier.align(Alignment.TopCenter).padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value%", color = Silver, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Steel, fontSize = 9.5.sp)
        }
    }
}

// ======================================================================
//  ۲) پد لمسی: چپ = بدنه (بالا/پایین = ارتفاع ، چپ/راست = الاکلنگی) ؛ راست = نگاه چشم‌ها
// ======================================================================
@Composable
private fun PadPane(g: GameController, en: Boolean) {
    fun tr(fa: String, e: String) = if (en) e else fa
    Row(Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PadBox(
            tr("بدنه", "Body"), g.d / 100f, g.h / 50f - 1f, true, Cyan,
            { x, y -> g.userBody(((y + 1f) * 50f).roundToInt(), (x * 100f).roundToInt()) }, { },
            Modifier.weight(1f).fillMaxHeight()
        )
        PadBox(
            tr("چشم‌ها", "Eyes"), g.ex / 100f, -g.ey / 100f, g.eyesHeld, Violet,
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
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(EMOTES) { (id, e) ->
            Box(
                Modifier.clip(CircleShape).background(Color(0x14FFFFFF)).border(1.dp, Line, CircleShape)
                    .clickable { g.emote(id) }.size(46.dp),
                contentAlignment = Alignment.Center
            ) { Text(e, fontSize = 22.sp) }
        }
    }
}

@Composable
private fun PadBox(
    title: String, px: Float, py: Float, active: Boolean, accent: Color,
    onMove: (Float, Float) -> Unit, onEnd: () -> Unit, modifier: Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    val move by rememberUpdatedState(onMove)
    val end by rememberUpdatedState(onEnd)
    Box(
        modifier.clip(shape).background(Color(0x0FFFFFFF)).border(1.dp, accent.copy(alpha = 0.5f), shape)
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
        Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
            val c = center
            drawLine(Line, Offset(0f, c.y), Offset(size.width, c.y), 1.dp.toPx())
            drawLine(Line, Offset(c.x, 0f), Offset(c.x, size.height), 1.dp.toPx())
            drawCircle(Line, size.minDimension * 0.33f, c, style = Stroke(1.dp.toPx()))
            val p = Offset(
                c.x + px.coerceIn(-1f, 1f) * (size.width / 2f - 20.dp.toPx()),
                c.y - py.coerceIn(-1f, 1f) * (size.height / 2f - 20.dp.toPx())
            )
            drawCircle(accent.copy(alpha = if (active) 0.35f else 0.14f), 26.dp.toPx(), p)
            drawCircle(if (active) accent else accent.copy(alpha = 0.55f), 11.dp.toPx(), p)
        }
        Text(title, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
    }
}

// ======================================================================
//  ۳) رقص با موسیقی: میکروفون گوشی ضرب رو میگیره ، ماکت میپره و چراغ‌ها نوبتی میشن
// ======================================================================
@Composable
private fun BeatPane(g: GameController, en: Boolean) {
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

    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
            val c = center
            val base = size.minDimension * 0.26f
            val lv = g.beat.level
            drawCircle(Gold.copy(alpha = 0.10f + 0.25f * lv), base * (1.7f + 0.25f * flash.value), c)
            drawCircle(Violet.copy(alpha = 0.35f), base * 1.38f, c, style = Stroke(2.dp.toPx()))
            drawCircle(
                Brush.radialGradient(listOf(Gold, Gold.copy(alpha = 0.55f)), center = c, radius = base * 1.2f),
                base * (1f + 0.2f * flash.value), c
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (g.beat.active && g.beat.bpm > 0) "${g.beat.bpm}" else "♪", color = Color(0xFF1A1405), fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text(if (g.beat.active) "BPM" else tr("آماده", "Ready"), color = Color(0xFF1A1405), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        GChip(
            if (g.beat.active) "■ " + tr("توقف رقص", "Stop dancing") else "🎤 " + tr("شروع رقص با موسیقی", "Start dancing"),
            g.beat.active, Gold, Modifier
        ) { toggle() }
    }
    Column {
        Text(tr("حساسیت ضرب", "Beat sensitivity"), color = Steel, fontSize = 11.sp)
        Slider(
            value = sens, onValueChange = { sens = it; g.beat.sensitivity = it },
            colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Color(0x22FFFFFF))
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
private fun MemoryBar(g: GameController, en: Boolean) {
    fun tr(fa: String, e: String) = if (en) e else fa
    GameCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MEMORY", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
            GChip("∞ " + tr("تکرار", "Loop"), g.loop, Mint) { g.loop = !g.loop }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GChip(
                if (g.recording) "■ ${g.recSecs}s" else "⏺ " + tr("ضبط", "REC"),
                g.recording, Danger, Modifier.width(86.dp)
            ) { if (g.recording) g.stopRec(true) else g.startRec() }
            for (i in 0..2) {
                val filled = g.slots[i] != null
                val sel = g.slotSel == i
                val isPlaying = g.playing == i
                val shape = RoundedCornerShape(14.dp)
                val col = if (isPlaying) Ok else Mint
                Box(
                    Modifier.weight(1f).clip(shape)
                        .background(if (sel || isPlaying) col.copy(alpha = 0.2f) else Color(0x14FFFFFF))
                        .border(if (sel || isPlaying) 1.5.dp else 1.dp, if (sel || isPlaying) col else Line, shape)
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
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (if (isPlaying) "▶ " else "") + "M${i + 1} " + (if (filled) "●" else "○"),
                        color = if (sel || isPlaying) col else Silver, fontSize = 13.sp, fontWeight = FontWeight.Bold
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

@Composable
private fun SoundRow(g: GameController, en: Boolean) {
    fun tr(fa: String, e: String) = if (en) e else fa
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GChip("🔊 " + tr("صدای احساسات ماکت", "Mood sounds"), g.voice.chirps, Cyan) { g.setChirps(!g.voice.chirps) }
            GChip("🗣 " + tr("ماکت حرف بزنه", "Let it talk"), g.voice.speak, Violet) { g.setSpeak(!g.voice.speak, en) }
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
