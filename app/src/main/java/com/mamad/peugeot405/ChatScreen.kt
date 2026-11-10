package com.mamad.peugeot405

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// ======================================================================
//  تب «چت»: نوشتن متن فارسی روی نمایشگر ماکت (#SAY:) ، بازی حافظه با راهنماها (G) ،
//  تنظیم حرکت سروو برای احساسات (#EMO:) و همگام‌سازی ساعت (#TIME:)
//  هماهنگ با car_emo_eyes_v30 یا جدیدتر
// ======================================================================

@Composable
private fun chatFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Silver, unfocusedTextColor = Silver, cursorColor = Cyan,
    focusedBorderColor = Cyan, unfocusedBorderColor = Hair2,
    focusedLabelColor = Cyan, unfocusedLabelColor = Steel
)

@Composable
private fun ChatChip(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Text(
        label, color = Silver, fontSize = 13.sp,
        modifier = Modifier.clip(shape).background(Surface3).border(1.dp, Hair, shape)
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 9.dp)
    )
}

@Composable
private fun ChatCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Surface2).border(1.dp, Hair, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) { content() }
}


// ---------- مغز ماکت (v33) ----------
@Composable
private fun BrainBar(label: String, v: Int, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Steel, fontSize = 11.sp, modifier = Modifier.width(96.dp))
        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Hair2)) {
            Box(Modifier.fillMaxWidth(v.coerceIn(0, 100) / 100f).fillMaxHeight().background(color))
        }
    }
}

@Composable
private fun BrainCard(vm: MainViewModel) {
    val S = LocalStrings.current
    val en = S.lang == "en"
    fun tr(fa: String, e: String) = if (en) e else fa
    val a = vm.brain.st
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) vm.voiceCmd.start() }
    val ctx = LocalContext.current
    ChatCard {
        Text(tr("مغز ماکت", "Car brain"), color = Silver, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        if (a.size < 17) {
            Text(tr("منتظر اتصال یا کد ماکت v33...", "Waiting for a connection / v33 sketch..."), color = Steel, fontSize = 11.5.sp)
        } else {
            val stage = (if (en) BrainState.STAGE_EN else BrainState.STAGE_FA)[a[9].coerceIn(0, 3)]
            Text(tr("سطح ", "Level ") + (a[1] / 100 + 1) + "  ·  " + a[1] + " XP  ·  " + stage + " (" + a[0] + "%)", color = Gold, fontSize = 14.sp)
            BrainBar(tr("انرژی", "Energy"), a[10], Mint)
            BrainBar(tr("حوصله‌سررفتگی", "Boredom"), a[11], Gold)
            BrainBar(tr("دلتنگی", "Missing you"), a[12], Cyan)
            val kind = a[14]
            val chal = (if (en) BrainState.CHAL_EN else BrainState.CHAL_FA).getOrNull(kind)
            Text(
                if (chal == null) tr("چالش روزانه: منتظر ساعت گوشی", "Daily challenge: waiting for the phone clock")
                else if (a[8] != 0) tr("✅ چالش امروز انجام شد", "✅ Today's challenge done")
                else tr("🎯 چالش امروز: ", "🎯 Today: ") + chal + "  (" + a[15] + "/" + a[16] + ")",
                color = Silver, fontSize = 12.5.sp
            )
            val names = if (en) BrainState.ACH_EN else BrainState.ACH_FA
            Text(
                names.indices.joinToString("   ") { (if ((a[2] shr it) and 1 == 1) "🏆 " else "▫ ") + names[it] },
                color = Steel, fontSize = 11.5.sp, lineHeight = 18.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(
                tr("لرزش: ", "Vibrate: ") + (if (vm.brain.vibrate) tr("روشن", "on") else tr("خاموش", "off")), Mint, vm.brain.vibrate, true,
                Modifier.weight(1f), 46.dp, onClick = { vm.brain.changeVibrate(!vm.brain.vibrate) }
            )
            PillButton(
                tr("صدای ماکت: ", "Voice: ") + (if (vm.game.voice.speak) tr("روشن", "on") else tr("خاموش", "off")), Cyan, vm.game.voice.speak, true,
                Modifier.weight(1f), 46.dp, onClick = { vm.game.setSpeak(!vm.game.voice.speak, en) }
            )
        }
        PillButton(
            if (vm.voiceCmd.on) tr("🎙 فرمان صوتی روشن است (لمس = خاموش)", "🎙 Voice commands on (tap to stop)") else tr("🎙 فرمان صوتی (سلام ، قفل ، برقص ، بخواب ...)", "🎙 Voice commands (Persian)"),
            Gold, vm.voiceCmd.on, true, Modifier.fillMaxWidth(), 48.dp,
            onClick = {
                if (vm.voiceCmd.on) vm.voiceCmd.stop()
                else if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED) vm.voiceCmd.start()
                else launcher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        )
        PillButton(
            if (vm.shake.on) tr("📳 تکون دادن گوشی روشن است (لمس = خاموش)", "📳 Shake detection on (tap to stop)") else tr("📳 تکون دادن گوشی (۳ تکون تند)", "📳 Shake the phone (3 quick shakes)"),
            Violet, vm.shake.on, true, Modifier.fillMaxWidth(), 46.dp,
            onClick = { if (vm.shake.on) vm.shake.stop() else vm.shake.start() }
        )
        if (vm.voiceCmd.err == "NA") Text(tr("تشخیص گفتار روی این گوشی نیست.", "Speech recognition isn't available."), color = Steel, fontSize = 11.5.sp)
        if (vm.brain.note.isNotEmpty()) Text(vm.brain.note, color = Silver, fontSize = 12.sp)
        Text(tr("پیش‌نمایش انیمیشن‌ها روی نمایشگر ماکت:", "Preview animations on the car's display:"), color = Steel, fontSize = 11.5.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(tr("🎂 تولد", "🎂 Birthday"), tr("🤧 عطسه", "🤧 Sneeze"), tr("🌅 صبح", "🌅 Morning"), tr("🏆 جام", "🏆 Trophy"), tr("🔌 شارژ", "🔌 Charge"), tr("🚗 استارت", "🚗 Engine start"))
                .forEachIndexed { i, l -> ChatChip(l) { vm.sendLine("CINE:$i") } }
            ChatChip(tr("💕 بارش قلب", "💕 Hearts")) { vm.sendLine("HEARTS") }
        }
        Text(
            tr("امتیاز و رابطه روی همین گوشی ذخیره می‌شود و موقع اتصال به ماکت برمی‌گردد (ماکت چیزی در EEPROM نگه نمی‌دارد).",
               "Score and relationship are saved on this phone and restored to the car on connect (the car stores nothing in EEPROM)."),
            color = Steel, fontSize = 11.5.sp, lineHeight = 16.sp
        )
    }
}

@Composable
fun ChatScreen(vm: MainViewModel) {
    val S = LocalStrings.current
    val en = S.lang == "en"
    fun tr(fa: String, e: String) = if (en) e else fa

    var text by remember { mutableStateOf("") }
    var quiet by remember { mutableStateOf(6f) }
    var amp by remember { mutableStateOf(100f) }

    val phrases = if (en) {
        listOf("سلام!", "دوستت دارم", "صبح بخیر", "شب بخیر", "آفرین", "بریم؟")
    } else {
        listOf("سلام!", "دوستت دارم", "صبح بخیر", "شب بخیر", "آفرین", "بریم؟", "خوشگل شدی!", "قهر نکن")
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---------- چت ----------
        ChatCard {
            Text(tr("چت با ماکت", "Chat with the car"), color = Silver, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                tr(
                    "هر متن فارسی که بنویسی روی نمایشگر ماکت نوشته می‌شود (تا حدود ۴۰ حرف ، روی دو خط).",
                    "Persian text is drawn on the car's OLED (about 40 characters, two lines). Latin letters are not shown."
                ),
                color = Steel, fontSize = 11.5.sp, lineHeight = 16.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it.take(90) },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(16.dp), colors = chatFieldColors(),
                    placeholder = { Text(tr("یه چیزی بنویس...", "Say something..."), color = Steel, fontSize = 13.sp) }
                )
                Spacer(Modifier.width(10.dp))
                PillButton(
                    S.send, Cyan, true, text.isNotBlank(), Modifier.width(88.dp), 54.dp,
                    onClick = { if (text.isNotBlank()) { vm.say(text); text = "" } }
                )
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                phrases.forEach { p -> ChatChip(p) { vm.say(p) } }
            }
            vm.chatLog.takeLast(4).forEach { m ->
                Text("›  $m", color = Steel, fontSize = 12.sp)
            }
        }

        BrainCard(vm)

        // ---------- ساعت و رمز مخفی ----------
        ChatCard {
            Text(tr("ساعت و رمز مخفی", "Clock & secret"), color = Silver, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                tr(
                    "ساعت گوشی هنگام هر اتصال خودکار به ماکت فرستاده می‌شود (صبح بخیر ، شب بخیر ، خواب‌آلودی شبانه). بازی‌ها در تب «بازی» هستند.",
                    "The phone clock is sent on every connection (good morning / good night / sleepy at night). Games are in the Games tab."
                ),
                color = Steel, fontSize = 11.5.sp, lineHeight = 16.sp
            )
            PillButton(
                tr("همگام‌سازی ساعت", "Sync clock"), Mint, false, true,
                Modifier.fillMaxWidth(), 52.dp, onClick = { vm.syncClock() }
            )
            Text(
                tr(
                    "رمز مخفی: راهنمای چپ ، راست ، چپ (در ۵ ثانیه). ۱۰ بار پشت‌سرهم زدن یک چراغ = ۳۰ ثانیه چراغ‌ها دست خود ماکت! (وسط بازی غیرفعال است.)",
                    "Secret: left, right, left indicator within 5 seconds. Tapping one light 10 times in a row = the car takes over the lights for 30 s (disabled during games)."
                ),
                color = Steel, fontSize = 11.5.sp, lineHeight = 16.sp
            )
        }

        // ---------- سروو و احساسات ----------
        ChatCard {
            Text(tr("حرکت سروو برای احساسات", "Servo emotions"), color = Silver, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                tr(
                    "فرمان دستی همیشه اولویت دارد: ماکت بعد از آخرین فرمان دستی این‌قدر ثانیه دست به سروو نمی‌زند.",
                    "Manual control always wins: the car leaves the servos alone for this many seconds after your last manual command."
                ),
                color = Steel, fontSize = 11.5.sp, lineHeight = 16.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("صبر بعد از فرمان دستی", "Wait after manual"), color = Steel, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("${quiet.roundToInt()} " + tr("ثانیه", "s"), color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Light)
            }
            Slider(
                value = quiet, onValueChange = { quiet = it }, valueRange = 0f..60f,
                colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Hair2)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("شدت حرکت", "Movement strength"), color = Steel, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("${amp.roundToInt()}%", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Light)
            }
            Slider(
                value = amp, onValueChange = { amp = it }, valueRange = 0f..200f,
                colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Hair2)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(
                    tr("اعمال روی ماکت", "Apply to car"), Gold, true, true, Modifier.weight(1f), 50.dp,
                    onClick = { vm.sendLine("EMO:" + quiet.roundToInt() + "," + amp.roundToInt()) }
                )
                PillButton(
                    tr("پیش‌فرض", "Default"), Steel, false, true, Modifier.weight(1f), 50.dp,
                    onClick = { quiet = 6f; amp = 100f; vm.sendLine("EMO:6,100") }
                )
            }
            Text(
                tr("این مقدار تا خاموش شدن ماکت می‌ماند ؛ برای دائمی کردن ، عددها را بالای کد ماکت (EMO_QUIET_SEC و EMO_AMP_PCT) بنویس.",
                   "Applies until the car is powered off; to make it permanent set EMO_QUIET_SEC and EMO_AMP_PCT at the top of the sketch."),
                color = Steel, fontSize = 11.5.sp, lineHeight = 16.sp
            )
        }
        Spacer(Modifier.padding(bottom = 8.dp))
    }
}
