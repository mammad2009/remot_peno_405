package com.mamad.peugeot405

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ======================================================================
//  تب «بازی»: سه بازی روی نمایشگر ماکت (نیاز به car_emo_eyes_v31 یا جدیدتر)
//    ۱) حافظه با راهنماها      ۲) گل یا پوچ      ۳) سنگ کاغذ قیچی
//  فرمان‌ها: #GAME:n (شروع ؛ 0 = پایان)  و  #GP:k (انتخاب بازیکن)
// ======================================================================

@Composable
private fun GameCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Surface2).border(1.dp, Hair, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

@Composable
fun GamesScreen(vm: MainViewModel) {
    val S = LocalStrings.current
    val en = S.lang == "en"
    fun tr(fa: String, e: String) = if (en) e else fa

    var game by remember { mutableStateOf(2) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---------- انتخاب بازی ----------
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(tr("حافظه", "Memory"), Cyan, game == 1, true, Modifier.weight(1f), 46.dp, onClick = { game = 1 })
            PillButton(tr("گل یا پوچ", "Gol/Pooch"), Gold, game == 2, true, Modifier.weight(1f), 46.dp, onClick = { game = 2 })
            PillButton(tr("سنگ کاغذ قیچی", "Rock Paper"), Mint, game == 3, true, Modifier.weight(1f), 46.dp, onClick = { game = 3 })
        }

        GameCard {
            val title = when (game) {
                1 -> tr("بازی حافظه با راهنماها", "Memory game (indicators)")
                2 -> tr("گل یا پوچ", "Gol ya Pooch (hidden coin)")
                else -> tr("سنگ کاغذ قیچی", "Rock Paper Scissors")
            }
            val info = when (game) {
                1 -> tr(
                    "ماکت راهنمای چپ و راست را به ترتیب چشمک می‌زند ؛ همان ترتیب را با دکمه‌های چپ و راست تکرار کن. هر دور یک مرحله بلندتر می‌شود (تا ۸).",
                    "The car blinks left/right in a sequence; repeat it with the buttons. Each round gets longer (up to 8)."
                )
                2 -> tr(
                    "یک دور ربات سکه را توی یکی از مشت‌هایش قایم می‌کند و تو حدس می‌زنی ؛ دور بعد تو قایم می‌کنی (دست چپ یا راست را انتخاب کن) و ربات حدس می‌زند. اولین نفر که به ۳ امتیاز برسد برنده است.",
                    "One round the car hides the coin and you guess; the next round you pick a hand to hide it and the car guesses. First to 3 points wins."
                )
                else -> tr(
                    "ربات سه ضربه می‌زند: سنگ ... کاغذ ... قیچی ! همان موقع (یا تا چند ثانیه بعد) انتخابت را بزن. ربات بعد از انتخاب تو تصمیم می‌گیرد ، پس منصفانه است. اولین نفر که به ۳ امتیاز برسد برنده است.",
                    "The car counts rock... paper... scissors! Tap your pick during the count or just after. The car chooses after you, so it's fair. First to 3 wins."
                )
            }
            Text(title, color = Silver, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(info, color = Steel, fontSize = 11.5.sp, lineHeight = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(
                    tr("شروع بازی", "Start"), Mint, true, true, Modifier.weight(1f), 50.dp,
                    onClick = { vm.sendLine("GAME:" + game) }
                )
                PillButton(
                    tr("پایان بازی", "Stop"), Steel, false, true, Modifier.weight(1f), 50.dp,
                    onClick = { vm.sendLine("GAME:0") }
                )
            }
        }

        // ---------- دکمه‌های هر بازی ----------
        GameCard {
            Text(tr("دکمه‌های بازی", "Game buttons"), color = Silver, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            when (game) {
                1 -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton(tr("◀  چپ", "◀  Left"), Cyan, true, true, Modifier.weight(1f), 96.dp, onClick = { vm.sendLine("GP:0") })
                    PillButton(tr("راست  ▶", "Right  ▶"), Cyan, true, true, Modifier.weight(1f), 96.dp, onClick = { vm.sendLine("GP:1") })
                }
                2 -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton(tr("✊  دست چپ", "✊  Left hand"), Gold, true, true, Modifier.weight(1f), 96.dp, onClick = { vm.sendLine("GP:0") })
                    PillButton(tr("دست راست  ✊", "Right hand  ✊"), Gold, true, true, Modifier.weight(1f), 96.dp, onClick = { vm.sendLine("GP:1") })
                }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(tr("✊\nسنگ", "✊\nRock"), Mint, true, true, Modifier.weight(1f), 96.dp, onClick = { vm.sendLine("GP:0") })
                    PillButton(tr("✋\nکاغذ", "✋\nPaper"), Mint, true, true, Modifier.weight(1f), 96.dp, onClick = { vm.sendLine("GP:1") })
                    PillButton(tr("✌\nقیچی", "✌\nScissors"), Mint, true, true, Modifier.weight(1f), 96.dp, onClick = { vm.sendLine("GP:2") })
                }
            }
            Text(
                tr(
                    "بازی‌ها روی نمایشگر ماکت انجام می‌شوند. وسط بازی ، دکمه‌های چراغ نادیده گرفته می‌شوند و ماکت چراغ‌ها را برنمی‌دارد.",
                    "Games run on the car's OLED. During a game the light buttons are ignored and the car won't take over the lights."
                ),
                color = Steel, fontSize = 11.5.sp, lineHeight = 16.sp
            )
        }
        Spacer(Modifier.padding(bottom = 8.dp))
    }
}
