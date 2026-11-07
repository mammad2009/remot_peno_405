package com.mamad.peugeot405

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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

        // ---------- بازی و ساعت ----------
        ChatCard {
            Text(tr("بازی و ساعت", "Game & clock"), color = Silver, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                tr(
                    "بازی حافظه: ماکت راهنمای راست و چپ را به ترتیب چشمک می‌زند ؛ تو در بخش «چراغ‌ها» همان ترتیب را با دکمه‌ی راهنمای راست و چپ تکرار کن. هر دور یک مرحله بلندتر می‌شود (تا ۸). دوباره بزنی بازی تمام می‌شود.",
                    "Memory game: the car blinks the right/left indicators in a sequence; repeat it with the indicator buttons in the Lights tab. Each round gets longer (up to 8). Press again to stop."
                ),
                color = Steel, fontSize = 11.5.sp, lineHeight = 16.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(
                    tr("بازی حافظه (شروع/پایان)", "Memory game (start/stop)"), Gold, true, true,
                    Modifier.weight(1f), 52.dp, onClick = { vm.send("G") }
                )
                PillButton(
                    tr("همگام‌سازی ساعت", "Sync clock"), Mint, false, true,
                    Modifier.weight(1f), 52.dp, onClick = { vm.syncClock() }
                )
            }
            Text(
                tr(
                    "رمز مخفی: راهنمای چپ ، راست ، چپ (در ۵ ثانیه). ۱۰ بار پشت‌سرهم زدن یک چراغ = ۳۰ ثانیه چراغ‌ها دست خود ماکت!",
                    "Secret: left, right, left indicator within 5 seconds. Tapping one light 10 times in a row = the car takes over the lights for 30 s!"
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
