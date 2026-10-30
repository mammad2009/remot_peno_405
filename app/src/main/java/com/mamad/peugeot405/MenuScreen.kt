package com.mamad.peugeot405

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * منوی ماکت = حالت کارخانه.
 * وقتی این تب باز است ماکت کف‌خواب است (فرمان Y) و از اینجا همه‌چیزش را تنظیم می‌کنی.
 * خروج از تب (یا بستن برنامه) = فرمان V.
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

    when (page) {
        0 -> MenuHub(vm, onExit) { page = it }
        1 -> SuspensionPage(vm) { page = 0 }
        2 -> EffectsPage(vm) { page = 0 }
        3 -> BatteryPage(vm) { page = 0 }
        else -> SystemPage(vm) { page = 0 }
    }
}

// ---------------- اجزای مشترک ----------------

@Composable
private fun PageHeader(title: String, accent: Color, onBack: () -> Unit) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Panel)
                .border(1.dp, accent.copy(alpha = 0.6f), CircleShape).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(18.dp)) {
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
        Spacer(Modifier.width(12.dp))
        Text(title, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
    }
}

@Composable
private fun WaitingCard(vm: MainViewModel) {
    val S = LocalStrings.current
    CardBox(Steel) {
        Text(S.sWaiting, color = Steel, fontSize = 13.sp)
        PillButton(
            label = S.refresh, accent = Cyan, filled = false, enabled = true,
            modifier = Modifier.fillMaxWidth(), height = 46.dp, onClick = { vm.requestInfo() }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Steel, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ValueSlider(
    label: String, value: Int, lo: Int, hi: Int, accent: Color, unit: String = "°",
    onChange: (Int) -> Unit, onFinish: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("$value$unit", color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.coerceIn(lo, hi).toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = lo.toFloat()..hi.toFloat(),
            onValueChangeFinished = onFinish,
            colors = SliderDefaults.colors(
                thumbColor = accent, activeTrackColor = accent,
                inactiveTrackColor = Color(0xFF232A36)
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

// ---------------- صفحه‌ی خانه ----------------

@Composable
private fun MenuTile(
    icon: String, title: String, subtitle: String, accent: Color,
    modifier: Modifier, onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.height(132.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f), Panel)))
            .border(1.dp, accent.copy(alpha = 0.45f), shape)
            .clickable { onClick() }.padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(icon, fontSize = 30.sp)
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2)
            Text(subtitle, color = accent, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun MenuHub(vm: MainViewModel, onExit: () -> Unit, onOpen: (Int) -> Unit) {
    val S = LocalStrings.current
    val i = vm.info
    val suspSub = if (i.containsKey("rf")) "F ${i["rf"]}°–${i["xf"]}°  R ${i["rb"]}°–${i["xb"]}°" else "—"
    val batSub = when {
        (i["bo"] ?: 0) == 1 -> "${i["bp"]}%"
        i.containsKey("bo") -> S.bOffText
        else -> "—"
    }
    val sysSub = if (i.containsKey("v")) "v${i["v"]}" else "—"

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        FactoryPreview()
        CardBox(Cyan, active = vm.factoryActive) {
            CardTitle(S.menuTitle, "FACTORY MODE", Cyan)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (vm.factoryActive) Ok else Steel))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (vm.factoryActive) S.facActive else S.facInactive,
                    color = if (vm.factoryActive) Ok else Steel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
            }
            Text(if (vm.factoryActive) S.menuSub else S.facDesc, color = Steel, fontSize = 12.sp, lineHeight = 18.sp)
            if (!vm.connected) Text(S.facNeedConnect, color = Gold, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuTile("🛞", S.mSuspension, suspSub, Cyan, Modifier.weight(1f)) { onOpen(1) }
            MenuTile("👀", S.mEffects, "${POSES.size} • ${TEXTS.size} • ${SCENES.size}", Violet, Modifier.weight(1f)) { onOpen(2) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuTile("🔋", S.mBattery, batSub, Ok, Modifier.weight(1f)) { onOpen(3) }
            MenuTile("⚙️", S.mSystem, sysSub, Gold, Modifier.weight(1f)) { onOpen(4) }
        }
        PillButton(
            label = S.facExit, accent = Cyan, filled = true, enabled = true,
            modifier = Modifier.fillMaxWidth(), height = 52.dp, onClick = onExit
        )
        Spacer(Modifier.height(8.dp))
    }
}

// ---------------- تعلیق و سرووها ----------------

@Composable
private fun AxleEditor(
    title: String, front: Boolean, accent: Color,
    rest: Int, mid: Int, max: Int,
    onRest: (Int) -> Unit, onMid: (Int) -> Unit, onMax: (Int) -> Unit, vm: MainViewModel
) {
    val S = LocalStrings.current
    CardBox(accent) {
        CardTitle(title, if (front) "FRONT" else "REAR", accent)
        ValueSlider(S.sRest, rest, 50, 110, accent, "°",
            { onRest(it); vm.testServo(front, it) }, { vm.testServo(front, rest, true) })
        ValueSlider(S.sMid, mid, rest, max, accent, "°",
            { onMid(it); vm.testServo(front, it) }, { vm.testServo(front, mid, true) })
        ValueSlider(S.sMax, max, rest + 5, 150, accent, "°",
            { onMax(it); vm.testServo(front, it) }, { vm.testServo(front, max, true) })
    }
}

@Composable
private fun SuspensionPage(vm: MainViewModel, onBack: () -> Unit) {
    val S = LocalStrings.current
    val ready = vm.info.containsKey("rf")
    var rf by remember(ready) { mutableIntStateOf(vm.info["rf"] ?: 80) }
    var rb by remember(ready) { mutableIntStateOf(vm.info["rb"] ?: 84) }
    var xf by remember(ready) { mutableIntStateOf(vm.info["xf"] ?: 112) }
    var xb by remember(ready) { mutableIntStateOf(vm.info["xb"] ?: 115) }
    var mf by remember(ready) { mutableIntStateOf(vm.info["mf"] ?: 96) }
    var mb by remember(ready) { mutableIntStateOf(vm.info["mb"] ?: 100) }
    var savedFlash by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    LaunchedEffect(savedFlash) {
        if (savedFlash) {
            delay(1800)
            savedFlash = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader(S.mSuspension, Cyan, onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!ready) WaitingCard(vm)
            Text(S.sHint, color = Steel, fontSize = 12.sp, lineHeight = 18.sp)

            AxleEditor(
                S.front, true, Cyan, rf, mf, xf,
                { rf = it; if (mf < it) mf = it; if (xf < it + 5) xf = it + 5 },
                { mf = it }, { xf = it; if (mf > it) mf = it }, vm
            )
            AxleEditor(
                S.rear, false, Violet, rb, mb, xb,
                { rb = it; if (mb < it) mb = it; if (xb < it + 5) xb = it + 5 },
                { mb = it }, { xb = it; if (mb > it) mb = it }, vm
            )

            PillButton(
                label = S.sRelease, accent = Gold, filled = false, enabled = true,
                modifier = Modifier.fillMaxWidth(), height = 50.dp, onClick = { vm.releaseServos() }
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
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(S.sDefaults) },
            text = { Text(S.sysReset) },
            confirmButton = { TextButton(onClick = { vm.resetCarDefaults(); confirmReset = false }) { Text(S.save) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(S.cancel) } }
        )
    }
}

// ---------------- چشم‌ها و افکت‌ها ----------------

@Composable
private fun ItemGrid(
    items: List<Named>, columns: Int, selected: Int, accent: Color, onPick: (Int) -> Unit
) {
    val S = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in items.chunked(columns)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (item in row) {
                    val sel = item.id == selected
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        Modifier.weight(1f).height(78.dp).clip(shape)
                            .background(if (sel) accent.copy(alpha = 0.22f) else Color(0xFF141922))
                            .border(if (sel) 2.dp else 1.dp, if (sel) accent else Color(0x33FFFFFF), shape)
                            .clickable { onPick(item.id) }.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(item.icon, fontSize = 24.sp)
                        Text(
                            item.name(S), color = if (sel) accent else Color.White, fontSize = 11.sp,
                            maxLines = 1, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                for (k in 0 until (columns - row.size)) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TextList(items: List<Named>, selected: Int, accent: Color, onPick: (Int) -> Unit) {
    val S = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (item in items) {
            val sel = item.id == selected
            val shape = RoundedCornerShape(14.dp)
            val en = S.lang == "en"
            Row(
                Modifier.fillMaxWidth().clip(shape)
                    .background(if (sel) accent.copy(alpha = 0.20f) else Color(0xFF141922))
                    .border(if (sel) 2.dp else 1.dp, if (sel) accent else Color(0x22FFFFFF), shape)
                    .clickable { onPick(item.id) }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${item.id + 1}", color = accent.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.width(26.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (en) item.en else item.fa, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (en) item.fa else item.en, color = Steel, fontSize = 11.sp)
                }
            }
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
        PageHeader(S.mEffects, Violet, onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("👁️  " + S.eLive, color = Steel, fontSize = 12.sp)
            val names = listOf(S.eEyes, S.eReact, S.eScenes, S.eTexts)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (k in names.indices) {
                    val sel = tab == k
                    val shape = RoundedCornerShape(50)
                    Box(
                        Modifier.clip(shape)
                            .background(if (sel) Violet.copy(alpha = 0.25f) else Color(0xFF1A2230))
                            .border(1.dp, if (sel) Violet else Color(0x22FFFFFF), shape)
                            .clickable { tab = k; picked = -1 }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(names[k], color = if (sel) Violet else Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
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

// ---------------- باتری ----------------

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

    var lo by remember(ready) { mutableIntStateOf(i["lo"] ?: 20) }
    var cr by remember(ready) { mutableIntStateOf(i["cr"] ?: 8) }
    var ca by remember(ready) { mutableIntStateOf(i["ca"] ?: 100) }
    var savedFlash by remember { mutableStateOf(false) }
    LaunchedEffect(savedFlash) {
        if (savedFlash) {
            delay(1800)
            savedFlash = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader(S.mBattery, Ok, onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!ready) WaitingCard(vm)
            if (ready && !supported) {
                CardBox(Gold) { Text(S.bNoFw, color = Gold, fontSize = 13.sp) }
            } else {
                // ---- درجه‌ی باتری ----
                CardBox(col) {
                    Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(180.dp)) {
                            val sw = 16.dp.toPx()
                            val tl = Offset(sw / 2f, sw / 2f)
                            val sz = Size(size.width - sw, size.height - sw)
                            drawArc(Color(0xFF222936), 150f, 240f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                            if (on) drawArc(col, 150f, 240f * pct / 100f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (on) "$pct%" else "--", color = col, fontSize = 40.sp, fontWeight = FontWeight.Black)
                            Text(
                                if (on) levelText(S, bl) else S.bOffText, color = Steel, fontSize = 13.sp
                            )
                            if (on) Text("${(i["bv"] ?: 0) / 10.0} V", color = Steel, fontSize = 12.sp)
                        }
                    }
                }

                // ---- روشن / خاموش ----
                CardBox(Ok) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(S.bEnable, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(S.bEnableHint, color = Steel, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                        Switch(checked = on, onCheckedChange = { vm.setBatteryEnabled(it) }, enabled = ready)
                    }
                }

                // ---- تنظیمات ----
                CardBox(Cyan) {
                    ValueSlider(S.bLow, lo, 8, 60, Gold, "%", { lo = it; if (cr > it - 3) cr = it - 3 }, {})
                    ValueSlider(S.bCrit, cr, 2, 30, Danger, "%", { cr = it; if (lo < it + 3) lo = it + 3 }, {})
                    ValueSlider(S.bCal + "  (×0.01)", ca, 50, 200, Cyan, "", { ca = it }, {})
                    PillButton(
                        label = if (savedFlash) S.saved else S.sSave, accent = Ok, filled = true, enabled = ready,
                        modifier = Modifier.fillMaxWidth(), height = 50.dp,
                        onClick = { vm.applyBattery(lo, cr, ca); savedFlash = true }
                    )
                    CopyCodeButton { batteryCode(on, lo, cr, ca) }
                    Text(S.sRam, color = Steel, fontSize = 11.sp, lineHeight = 16.sp)
                }

                // ---- شبیه‌سازی (برای دیدن افکت‌ها و الارم بدون باتری واقعی) ----
                CardBox(Violet) {
                    CardTitle(S.bSim, "TEST", Violet)
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
        Modifier.clip(shape).background(Color(0xFF1A2230)).border(1.dp, Violet.copy(alpha = 0.6f), shape)
            .clickable { onClick() }.padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Violet, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------- سیستم ----------------

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
        PageHeader(S.mSystem, Gold, onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!ready) WaitingCard(vm)
            CardBox(Gold) {
                InfoRow(S.sysFirmware, if (ready) "v${i["v"]}" else "—")
                InfoRow(S.sysMode, if (ready) modeText else "—")
                InfoRow(S.sysMood, if (ready) moodText else "—")
                InfoRow(S.sysLocked, if (ready) (if ((i["lk"] ?: 0) == 1) S.yes else S.no) else "—")
                InfoRow(
                    S.sysDisplay, if (ready) (if ((i["od"] ?: 0) == 1) S.okText else S.problem) else "—",
                    if (ready && (i["od"] ?: 0) != 1) Danger else Color.White
                )
                InfoRow("F / R", if (ready) "${i["pf"]}° / ${i["pb"]}°" else "—")
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
        AlertDialog(
            onDismissRequest = { confirm = 0 },
            title = { Text(if (confirm == 1) S.sysReset else S.sysRestart) },
            confirmButton = {
                TextButton(onClick = {
                    if (confirm == 1) vm.resetCarDefaults() else vm.restartCar()
                    confirm = 0
                }) { Text(S.save) }
            },
            dismissButton = { TextButton(onClick = { confirm = 0 }) { Text(S.cancel) } }
        )
    }
}
