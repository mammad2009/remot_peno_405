package com.mamad.peugeot405

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Grey = Color(0xFF8B93A5)

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // زبان فارسی = راست‌به‌چپ ، انگلیسی = چپ‌به‌راست
            val dir = if (vm.lang == "en") LayoutDirection.Ltr else LayoutDirection.Rtl
            CompositionLocalProvider(LocalLayoutDirection provides dir, LocalStrings provides vm.strings) {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        primary = Cyan, onPrimary = Bg, secondary = Gold, tertiary = Violet,
                        background = Bg, onBackground = Silver, surface = Panel, onSurface = Silver,
                        surfaceVariant = Surface3, onSurfaceVariant = Steel, outline = Hair2, error = Danger
                    )
                ) {
                    AppRoot(vm, this@MainActivity::openWifiSettings)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshNetwork()
    }

    // وقتی برنامه از جلوی چشم رفت (یا بسته شد) ماکت از حالت کارخانه بیرون می‌آید؛ برگشتی دوباره وارد می‌شود
    override fun onStart() {
        super.onStart()
        vm.updateForeground(true)
    }

    override fun onStop() {
        super.onStop()
        vm.updateForeground(false)
    }

    private fun openWifiSettings() {
        startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
    }
}


// مراحل ورود: -1 = صفحه‌ی شروع (آرم) ، 0 = دکمه‌ی اتصال ، 1 = ریموت پژو ، 2 = منوی اصلی
@Composable
fun AppRoot(
    vm: MainViewModel,
    onOpenWifi: () -> Unit
) {
    var phase by rememberSaveable { mutableStateOf(if (vm.connected) 2 else -1) }
    var showConnect by remember { mutableStateOf(false) }
    var showRemote by remember { mutableStateOf(false) }
    var transition by remember { mutableStateOf(false) }

    // بعد از اتصال موفق: دکمه محو می‌شود و بعدش ریموت ظاهر می‌شود
    LaunchedEffect(vm.connected) {
        if (vm.connected && phase == 0) {
            delay(1050)
            if (phase == 0) phase = 1
        }
    }

    Backdrop(Modifier.fillMaxSize()) {
        if (phase == 2 || transition) {
            MenuLayer(
                vm,
                { vm.refreshNetwork(); showConnect = true },
                { showRemote = true }
            )
        }
        if (phase == -1) {
            SplashScreen(onFinished = { phase = 0 })
        }
        if (phase == 0) {
            ConnectScreen(vm, onPick = { vm.refreshNetwork(); showConnect = true }, onSkip = { phase = 2 })
        }
        if (phase == 1) {
            RemoteIntro(vm, onLeaving = { transition = true }, onDone = { phase = 2; transition = false })
        }
        RemoteOverlay(vm, showRemote) { showRemote = false }
    }
    if (showConnect) {
        ConnectDialog(vm, { showConnect = false }, onOpenWifi)
    }
}

@Composable
fun MenuLayer(
    vm: MainViewModel,
    onConnect: () -> Unit,
    onRemote: () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(700), label = "menuIn")
    Scaffold(
        modifier = Modifier.graphicsLayer { alpha = a },
        containerColor = Color.Transparent,
        contentColor = Color.White,
        bottomBar = { FloatingNav(vm.tab, vm::selectTab) }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Header(vm, onConnect, onRemote)
            Crossfade(targetState = vm.tab, animationSpec = tween(260), label = "tab") { t ->
                when (t) {
                    0 -> ControlScreen(vm)
                    1 -> LightsScreen(vm)
                    2 -> MenuScreen(vm, onExit = { vm.selectTab(0) })
                    else -> SettingsScreen(vm)
                }
            }
        }
    }
}

// ---------- نوار پایین: گرافیتی با خط نشانگر نقره‌ای زیر تب فعال ----------

@Composable
fun FloatingNav(tab: Int, onTab: (Int) -> Unit) {
    val S = LocalStrings.current
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
            .luxPanel(shape).padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        NavItem(tab == 0, S.navControl, Cyan, Modifier.weight(1f), { onTab(0) }) { tint ->
            Icon(Icons.Default.Home, null, tint = tint)
        }
        NavItem(tab == 1, S.navLights, Cyan, Modifier.weight(1f), { onTab(1) }) { tint ->
            Canvas(Modifier.size(24.dp)) { drawBulbIcon(tint) }
        }
        NavItem(tab == 2, S.navMenu, Gold, Modifier.weight(1f), { onTab(2) }) { tint ->
            Canvas(Modifier.size(24.dp)) { drawGearIcon(tint) }
        }
        NavItem(tab == 3, S.navCommands, Violet, Modifier.weight(1f), { onTab(3) }) { tint ->
            Icon(Icons.Default.Settings, null, tint = tint)
        }
    }
}

@Composable
private fun NavItem(
    selected: Boolean, label: String, accent: Color, modifier: Modifier,
    onClick: () -> Unit, icon: @Composable (Color) -> Unit
) {
    val a by animateFloatAsState(if (selected) 1f else 0f, tween(300), label = "nav")
    val tint = lerp(Grey, accent, a)
    Column(
        modifier.clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f * a), Color.Transparent)))
            .clickable { onClick() }
            .padding(top = 8.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon(tint)
        Text(label, color = tint, fontSize = 10.5.sp, letterSpacing = 0.4.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.width((26f * a).dp).height(2.dp).clip(RoundedCornerShape(1.dp))
                .background(accent.copy(alpha = a))
        )
    }
}

// آیکن لامپ (برای تب چراغ‌ها)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBulbIcon(color: Color) {
    val w = size.width
    val h = size.height
    drawCircle(color, w * 0.28f, Offset(w / 2f, h * 0.40f), style = Stroke(w * 0.09f))
    drawLine(color, Offset(w * 0.38f, h * 0.80f), Offset(w * 0.62f, h * 0.80f), w * 0.09f, StrokeCap.Round)
    drawLine(color, Offset(w * 0.42f, h * 0.93f), Offset(w * 0.58f, h * 0.93f), w * 0.09f, StrokeCap.Round)
}

// آیکن چرخ‌دنده (برای تب منو)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGearIcon(color: Color) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension * 0.30f
    val sw = size.minDimension * 0.10f
    drawCircle(color, r, c, style = Stroke(sw))
    drawCircle(color, r * 0.35f, c, style = Stroke(sw))
    for (k in 0 until 8) {
        val a = k * 0.7853982f
        val dx = kotlin.math.cos(a)
        val dy = kotlin.math.sin(a)
        drawLine(
            color, Offset(c.x + dx * r * 1.1f, c.y + dy * r * 1.1f),
            Offset(c.x + dx * r * 1.6f, c.y + dy * r * 1.6f), sw * 1.3f, StrokeCap.Round
        )
    }
}

@Composable
fun ConnectDialog(
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onOpenWifi: () -> Unit
) {
    val S = LocalStrings.current
    // نشانگر وضعیت وای‌فای گوشی زنده به‌روز می‌شود (وقتی از تنظیمات برمی‌گردی)
    LaunchedEffect(Unit) {
        while (true) {
            vm.refreshNetwork()
            delay(1200)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2,
        shape = RoundedCornerShape(28.dp),
        titleContentColor = Silver,
        textContentColor = Silver,
        confirmButton = { TextButton(onClick = onDismiss) { Text(S.close) } },
        dismissButton = {
            if (vm.connected) TextButton(onClick = { vm.disconnect(); onDismiss() }) { Text(S.disconnect) }
        },
        title = { Text(S.dlgTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(S.wifiSteps, fontSize = 13.sp)
                Text(
                    (if (vm.wifiReady) "\uD83D\uDFE2 " + S.wifiOn else "\uD83D\uDD34 " + S.wifiOffTxt),
                    fontSize = 13.sp, color = if (vm.wifiReady) Ok else Danger
                )
                PillButton(
                    S.openWifi, Cyan, false, true, Modifier.fillMaxWidth(), 48.dp,
                    onClick = onOpenWifi
                )
                PillButton(
                    S.connectNow, Cyan, true, !vm.connecting && !vm.connected, Modifier.fillMaxWidth(), 52.dp,
                    onClick = { vm.connect(); onDismiss() }
                )
                Text(S.connectHint, fontSize = 11.sp, color = Grey)
                Text(S.status(vm.statusType, vm.statusArg), fontSize = 11.sp, color = Grey)
            }
        }
    )
}

@Composable
fun CardBox(accent: Color, active: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier.fillMaxWidth().luxPanel(shape, accent, active).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun CardTitle(title: String, tag: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 3.dp, height = 16.dp).clip(RoundedCornerShape(2.dp)).background(accent))
        Spacer(Modifier.width(10.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Silver)
        Spacer(Modifier.weight(1f))
        Text(tag, fontSize = 9.5.sp, letterSpacing = 2.sp, color = accent.copy(alpha = 0.75f))
    }
}

@Composable
fun PillButton(
    label: String,
    accent: Color,
    filled: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    onDown: () -> Unit = {},
    onUp: () -> Unit = {},
    onClick: () -> Unit = {},
    leading: @Composable () -> Unit = {}
) {
    val shape = RoundedCornerShape(20.dp)
    var pressed by remember { mutableStateOf(false) }
    val fg = if (filled) Color(0xFF0B0D12) else if (enabled) accent else Steel
    val down by rememberUpdatedState(onDown)
    val up by rememberUpdatedState(onUp)
    val click by rememberUpdatedState(onClick)
    val bg = if (filled) Brush.verticalGradient(listOf(accent.copy(alpha = if (pressed) 0.70f else 1f), accent.copy(alpha = if (pressed) 0.55f else 0.78f)))
             else Brush.verticalGradient(listOf(accent.copy(alpha = if (pressed) 0.34f else 0.16f), accent.copy(alpha = if (pressed) 0.20f else 0.04f)))
    Row(
        modifier.height(height).clip(shape).background(bg)
            .border(1.dp, accent.copy(alpha = if (enabled) 0.55f else 0.2f), shape)
            .pointerInput(enabled) {
                detectTapGestures(onPress = {
                    if (enabled) {
                        pressed = true
                        down()
                        var released = false
                        try {
                            released = tryAwaitRelease()
                        } finally {
                            pressed = false
                            up()
                        }
                        if (released) click()
                    }
                })
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        leading()
        Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.3.sp, maxLines = 1)
    }
}

@Composable
fun ExtraButtons(extras: List<Cmd>, accent: Color, onPress: (Cmd) -> Unit) {
    extras.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { c ->
                PillButton(
                    label = c.label, accent = accent, filled = false, enabled = true,
                    modifier = Modifier.weight(1f), height = 46.dp, onClick = { onPress(c) }
                )
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

// هر گروه دلخواه یک کارت جدا با اسم خودش
@Composable
fun GroupCard(name: String, list: List<Cmd>, onPress: (Cmd) -> Unit) {
    val accent = Color(0xFF7DD3C0)
    CardBox(accent) {
        CardTitle(name, "CUSTOM", accent)
        ExtraButtons(list, accent, onPress)
    }
}

@Composable
private fun LangChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.clip(shape)
            .background(if (selected) Cyan.copy(alpha = 0.18f) else Surface3)
            .border(1.dp, if (selected) Cyan else Hair2, shape)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Cyan else Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val S = LocalStrings.current
    var editing by remember { mutableStateOf<Cmd?>(null) }
    var raw by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            CardBox(Cyan) {
                CardTitle(S.language, "LANGUAGE", Cyan)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LangChip("فارسی", vm.lang == "fa") { vm.switchLang("fa") }
                    LangChip("English", vm.lang == "en") { vm.switchLang("en") }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(raw, { raw = it }, label = { Text(S.sendCustom) },
                    modifier = Modifier.weight(1f), singleLine = true)
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (raw.isNotEmpty()) vm.send(raw) }) { Text(S.send) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { editing = Cmd(label = "", command = "", group = "سایر") }) {
                    Icon(Icons.Default.Add, null); Text(S.newCommand)
                }
                OutlinedButton(onClick = { vm.resetDefaults() }) { Text(S.defaults) }
            }
        }
        items(vm.commands, key = { it.id }) { c ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Surface2)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${S.group(c.group)} \u2022 ${S.cmdLabel(c)}")
                        Text(S.sentCommand(c.command), fontSize = 11.sp, color = Grey)
                    }
                    IconButton(onClick = { editing = c }) { Icon(Icons.Default.Edit, null) }
                    if (c.action == "NONE") {
                        IconButton(onClick = { vm.delete(c.id) }) { Icon(Icons.Default.Delete, null) }
                    }
                }
            }
        }
    }
    editing?.let { cmd ->
        var label by remember(cmd.id) { mutableStateOf(S.cmdLabel(cmd)) }
        var command by remember(cmd.id) { mutableStateOf(cmd.command) }
        var group by remember(cmd.id) { mutableStateOf(cmd.group) }
        val groups = (listOf("جلو", "عقب", "میباخ", "سایر") + vm.commands.map { it.group })
            .filter { it != "سیستمی" }.distinct()
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(S.editCommand) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(label, { label = it }, label = { Text(S.buttonName) }, singleLine = true)
                    OutlinedTextField(command, { command = it }, label = { Text(S.commandField) }, singleLine = true)
                    if (cmd.action == "NONE") {
                        OutlinedTextField(group, { group = it }, label = { Text(S.groupField) }, singleLine = true)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            groups.forEach { g ->
                                Text(
                                    S.group(g), fontSize = 12.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                        .background(if (group.trim() == g) Cyan.copy(alpha = 0.3f) else Color(0xFF222B3A))
                                        .clickable { group = g }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (label.isNotBlank() && command.isNotEmpty()) {
                        // اگر اسم دکمه را دست نزده‌ای، اسم اصلی (پیش‌فرض) می‌ماند تا با تغییر زبان ترجمه شود
                        val newLabel = if (label.trim() == S.cmdLabel(cmd)) cmd.label else label.trim()
                        vm.upsert(cmd.copy(label = newLabel, command = command, group = group.ifBlank { "سایر" }.trim()))
                        editing = null
                    }
                }) { Text(S.save) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(S.cancel) } }
        )
    }
}
