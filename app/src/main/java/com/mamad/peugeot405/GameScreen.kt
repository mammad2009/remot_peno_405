package com.mamad.peugeot405

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

// صدای مناسب هر احساس (شماره‌ها = enum حالت‌های چشم و واکنش‌ها در کد ماکت)
private fun poseSound(id: Int): String? = when (id) {
    7, 8, 9, 25, 29 -> "happy"
    28, 31, 21 -> "love"
    15 -> "scared"
    10, 32, 27 -> "angry"
    11 -> "sad"
    14, 26 -> "wow"
    12, 13 -> "yawn"
    30 -> "dizzy"
    else -> null
}

private fun microSound(k: Int): String? = when (k) {
    0 -> "sneeze"
    1 -> "hiccup"
    4 -> "yawn"
    13 -> "dizzy"
    else -> null
}

/**
 * پنل «بازی»: گوشی سرووها را می‌چرخاند.
 * کج کردن راست/چپ = بالا و پایین رفتن هر دو سروو ؛ پد لمسی ؛ رقص با موسیقی ؛ ضبط حرکت ؛ صدا.
 */
@Composable
fun GamePanel(vm: MainViewModel) {
    val S = LocalStrings.current
    val ctx = LocalContext.current
    val view = LocalView.current

    var tiltOn by rememberSaveable { mutableStateOf(false) }
    var invert by rememberSaveable { mutableStateOf(false) }
    var rangeDeg by rememberSaveable { mutableStateOf(35) }
    var engineOn by rememberSaveable { mutableStateOf(false) }
    var voiceOn by rememberSaveable { mutableStateOf(false) }
    var fxOn by rememberSaveable { mutableStateOf(false) }
    var micDenied by remember { mutableStateOf(false) }
    var roll by remember { mutableStateOf(0f) }
    var centerRoll by remember { mutableStateOf(0f) }
    var padTouching by remember { mutableStateOf(false) }
    val tilt = remember { TiltSensor(ctx) }

    // تا وقتی توی بازی هستیم صفحه خاموش نشود؛ موقع خروج همه‌چیز (رقص ، صدا ، ضبط) خاموش شود
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            vm.leaveGame()
        }
    }

    // ---- کج کردن گوشی ----
    DisposableEffect(tiltOn) {
        if (tiltOn) tilt.start { r -> roll = r }
        onDispose { tilt.stop() }
    }
    LaunchedEffect(tiltOn, invert, rangeDeg) {
        if (!tiltOn) return@LaunchedEffect
        snapshotFlow { roll }.collect { r ->
            if (!padTouching) {
                var x = r - centerRoll
                if (abs(x) < 2f) x = 0f
                val sgn = if (invert) -1f else 1f
                val h = (50f + sgn * 50f * x / rangeDeg).coerceIn(0f, 100f).roundToInt()
                vm.setGame(h, vm.gameD)
            }
        }
    }

    // ---- صدا ----
    LaunchedEffect(engineOn) { vm.engine(engineOn) }
    LaunchedEffect(Unit) {
        snapshotFlow { vm.gameH }.collect { vm.sound.setRevs(it / 100f) }       // دور موتور = ارتفاع بدنه
    }
    LaunchedEffect(voiceOn, fxOn) { vm.setEvents(voiceOn || fxOn) }
    LaunchedEffect(voiceOn, fxOn, S.lang) {
        if (!voiceOn && !fxOn) return@LaunchedEffect
        vm.carEvents.collect { ev ->
            val n = ev.drop(1).toIntOrNull() ?: return@collect
            when (ev.firstOrNull()) {
                'T' -> if (voiceOn) {
                    val t = TEXTS.getOrNull(n)
                    if (t != null) vm.sound.speak(if (S.lang == "en") t.en else t.fa, S.lang)
                }
                'P' -> if (fxOn) poseSound(n)?.let { vm.sound.effect(it) }
                'M' -> if (fxOn) microSound(n)?.let { vm.sound.effect(it) }
                else -> {}
            }
        }
    }

    // ---- رقص: اول مجوز میکروفن ----
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            micDenied = false
            vm.startDance()
        } else {
            micDenied = true
        }
    }
    val toggleDance: () -> Unit = {
        if (vm.danceOn) {
            vm.stopDance()
        } else if (ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            micDenied = false
            vm.startDance()
        } else {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MeterCard(vm)
        TiltCard(
            available = tilt.available, tiltOn = tiltOn, onTilt = { tiltOn = it },
            invert = invert, onInvert = { invert = it },
            rangeDeg = rangeDeg, onRange = { rangeDeg = it },
            roll = roll, onCenter = { centerRoll = roll }
        )
        PadCard(vm) { padTouching = it }
        DanceCard(vm, micDenied, toggleDance)
        RecorderCard(vm)
        SoundCard(
            vm, engineOn, { engineOn = it }, voiceOn, { voiceOn = it }, fxOn, { fxOn = it }
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ---------------- نمایشگر ارتفاع ----------------

@Composable
private fun MeterCard(vm: MainViewModel) {
    val S = LocalStrings.current
    CardBox(Cyan) {
        CardTitle(S.gMeter, "GAME", Cyan)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${vm.gameH}%", color = Cyan, fontSize = 44.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Canvas(Modifier.fillMaxWidth().height(16.dp)) {
                    drawRoundRect(Color(0xFF222936), Offset.Zero, size, CornerRadius(size.height / 2f))
                    drawRoundRect(Cyan, Offset.Zero, Size(size.width * vm.gameH / 100f, size.height), CornerRadius(size.height / 2f))
                }
                Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                    drawRoundRect(Color(0xFF222936), Offset.Zero, size, CornerRadius(size.height / 2f))
                    val cx = size.width * (0.5f + vm.gameD / 200f)
                    drawCircle(Violet, size.height * 0.9f, Offset(cx, size.height / 2f))
                }
            }
        }
    }
}

// ---------------- کج کردن گوشی ----------------

@Composable
private fun TiltCard(
    available: Boolean, tiltOn: Boolean, onTilt: (Boolean) -> Unit,
    invert: Boolean, onInvert: (Boolean) -> Unit,
    rangeDeg: Int, onRange: (Int) -> Unit,
    roll: Float, onCenter: () -> Unit
) {
    val S = LocalStrings.current
    CardBox(Violet, active = tiltOn) {
        CardTitle(S.gTilt, "TILT", Violet)
        Text(S.gTiltHint, color = Steel, fontSize = 12.sp, lineHeight = 17.sp)
        if (!available) Text(S.gNoSensor, color = Danger, fontSize = 12.sp)
        SwitchRow(S.gTilt, tiltOn, onTilt)
        if (tiltOn) {
            SwitchRow(S.gInvert, invert, onInvert)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(S.gRange, color = Steel, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("${roll.roundToInt()}° / $rangeDeg°", color = Violet, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = rangeDeg.toFloat(),
                onValueChange = { onRange(it.roundToInt()) },
                valueRange = 15f..60f,
                colors = SliderDefaults.colors(
                    thumbColor = Violet, activeTrackColor = Violet, inactiveTrackColor = Color(0xFF232A36)
                )
            )
            PillButton(
                label = S.gCenter, accent = Violet, filled = false, enabled = true,
                modifier = Modifier.fillMaxWidth(), height = 46.dp, onClick = onCenter
            )
        }
    }
}

// ---------------- پد لمسی ----------------

@Composable
private fun PadCard(vm: MainViewModel, onTouching: (Boolean) -> Unit) {
    val S = LocalStrings.current
    var pt by remember { mutableStateOf<Offset?>(null) }       // موقعیت انگشت (۰ تا ۱)
    val shape = RoundedCornerShape(22.dp)
    CardBox(Gold) {
        CardTitle(S.gPad, "PAD", Gold)
        Text(S.gPadHint, color = Steel, fontSize = 12.sp, lineHeight = 17.sp)
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.5f).clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF1C2230), Color(0xFF0C0F15))))
                .border(1.dp, Gold.copy(alpha = 0.5f), shape)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        onTouching(true)
                        fun update(p: Offset) {
                            val x = (p.x / size.width).coerceIn(0f, 1f)
                            val y = (p.y / size.height).coerceIn(0f, 1f)
                            pt = Offset(x, y)
                            val h = ((1f - y) * 100f).roundToInt()
                            val d = ((x - 0.5f) * 200f).roundToInt()
                            vm.setGame(h, d)
                            vm.setGaze(d, -(h - 50) * 2, true)           // چشم‌های ماکت دنبال انگشت
                        }
                        update(down.position)
                        down.consume()
                        do {
                            val ev = awaitPointerEvent()
                            val c = ev.changes.firstOrNull()
                            if (c != null && c.pressed) {
                                update(c.position)
                                c.consume()
                            }
                        } while (ev.changes.any { it.pressed })
                        pt = null
                        vm.setGaze(0, 0, false)
                        onTouching(false)
                    }
                }
        ) {
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawLine(Color(0x33FFFFFF), Offset(w / 2f, 0f), Offset(w / 2f, h), 2f)
                drawLine(Color(0x33FFFFFF), Offset(0f, h / 2f), Offset(w, h / 2f), 2f)
                val p = pt
                if (p != null) {
                    val c = Offset(p.x * w, p.y * h)
                    drawCircle(Gold.copy(alpha = 0.25f), 46f, c)
                    drawCircle(Gold, 18f, c)
                }
            }
        }
    }
}

// ---------------- رقص با موسیقی ----------------

@Composable
private fun DanceCard(vm: MainViewModel, micDenied: Boolean, onToggle: () -> Unit) {
    val S = LocalStrings.current
    CardBox(Danger, active = vm.danceOn) {
        CardTitle(S.gDance, "DANCE", Danger)
        Text(S.gDanceHint, color = Steel, fontSize = 12.sp, lineHeight = 17.sp)
        Canvas(Modifier.fillMaxWidth().height(34.dp)) {
            val n = 20
            val bw = size.width / n
            for (i in 0 until n) {
                val on = vm.danceOn && (i + 1f) / n <= vm.micLevel + 0.05f
                val col = if (!on) Color(0xFF222936) else if (i > 14) Danger else if (i > 9) Gold else Ok
                drawRoundRect(col, Offset(i * bw + bw * 0.15f, 0f), Size(bw * 0.7f, size.height), CornerRadius(4f))
            }
        }
        if (micDenied) Text(S.gMicDenied, color = Danger, fontSize = 12.sp)
        PillButton(
            label = if (vm.danceOn) S.gDanceStop else S.gDanceStart, accent = Danger,
            filled = !vm.danceOn, enabled = vm.connected,
            modifier = Modifier.fillMaxWidth(), height = 54.dp, onClick = onToggle
        )
    }
}

// ---------------- ضبط و پخش حرکت ----------------

@Composable
private fun RecorderCard(vm: MainViewModel) {
    val S = LocalStrings.current
    var nameDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var loop by remember { mutableStateOf(false) }

    CardBox(Ok, active = vm.recording) {
        CardTitle(S.gRec, "REC", Ok)
        Text(S.gRecHint, color = Steel, fontSize = 12.sp, lineHeight = 17.sp)
        if (vm.recording) Text("🔴 " + S.gRecording, color = Danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        PillButton(
            label = if (vm.recording) S.gRecStop else S.gRecStart,
            accent = if (vm.recording) Danger else Ok, filled = true,
            enabled = vm.connected || vm.recording,
            modifier = Modifier.fillMaxWidth(), height = 52.dp,
            onClick = {
                if (vm.recording) {
                    name = "Motion " + (vm.motions.size + 1)
                    nameDialog = true
                } else {
                    vm.startRecording()
                }
            }
        )
        Text(S.gMotions, color = Steel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        SwitchRow(S.gLoop, loop) { loop = it }
        if (vm.motions.isEmpty()) Text(S.gNoMotions, color = Steel, fontSize = 12.sp)
        for (m in vm.motions) {
            val playing = vm.playingMotion == m.name
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF141922))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(m.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(m.durationMs / 100) / 10.0} s", color = Steel, fontSize = 11.sp)
                }
                TextButton(onClick = { if (playing) vm.stopMotion() else vm.playMotion(m, loop) }) {
                    Text(if (playing) S.gStopPlay else S.gPlay, color = Ok)
                }
                TextButton(onClick = { vm.deleteMotion(m.name) }) { Text(S.gDelete, color = Danger) }
            }
        }
    }
    if (nameDialog) {
        AlertDialog(
            onDismissRequest = { nameDialog = false; vm.stopRecording(null) },
            title = { Text(S.gRec) },
            text = { OutlinedTextField(name, { name = it }, label = { Text(S.gName) }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.stopRecording(name); nameDialog = false }) { Text(S.save) } },
            dismissButton = { TextButton(onClick = { vm.stopRecording(null); nameDialog = false }) { Text(S.cancel) } }
        )
    }
}

// ---------------- صدا ----------------

@Composable
private fun SoundCard(
    vm: MainViewModel,
    engineOn: Boolean, onEngine: (Boolean) -> Unit,
    voiceOn: Boolean, onVoice: (Boolean) -> Unit,
    fxOn: Boolean, onFx: (Boolean) -> Unit
) {
    val S = LocalStrings.current
    CardBox(Cyan) {
        CardTitle(S.gSound, "SOUND", Cyan)
        SwitchRow(S.gEngine, engineOn, onEngine)
        SwitchRow(S.gVoice, voiceOn, onVoice)
        SwitchRow(S.gFx, fxOn, onFx)
        Text(S.gVoiceNote, color = Steel, fontSize = 11.sp, lineHeight = 16.sp)
        PillButton(
            label = "📯  " + S.gHorn, accent = Gold, filled = true, enabled = true,
            modifier = Modifier.fillMaxWidth(), height = 54.dp, onClick = { vm.sound.horn() }
        )
    }
}
