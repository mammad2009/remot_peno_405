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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ======================================================================
//  تب «بازی» (v5.7 ، نیاز به car_emo_eyes_v32)
//    ۱) حافظه (۲ دکمه / ۴ دکمه / برعکس)   ۲) گل یا پوچ (۲ یا ۳ مشت)   ۳) سنگ کاغذ قیچی
//  فرمان‌ها: #GAME:n,target,level (شروع ؛ 0 = پایان)  و  #GP:k (انتخاب بازیکن)
//  ماکت امتیاز و نتیجه‌ی هر دور رو زنده میفرسته (GS:...) و اینجا با لرزش و آمار نشون داده میشه.
// ======================================================================

@Composable
private fun GameCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Surface2).border(1.dp, Hair, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

private fun pips(score: Int, total: Int): String =
    (0 until total).joinToString(" ") { if (it < score) "●" else "○" }

private fun handEmoji(i: Int) = when (i) { 0 -> "✊"; 1 -> "✋"; else -> "✌" }

private fun levelName(l: Int, en: Boolean) = when (l) {
    0 -> if (en) "Easy" else "آسان"
    1 -> if (en) "Normal" else "معمولی"
    else -> if (en) "Hard" else "سخت"
}

private fun levelInfo(game: Int, level: Int, en: Boolean): String = when (game) {
    1 -> when (level) {
        0 -> if (en) "2 pads (left / right), like the classic game." else "دو پد (چپ و راست) ؛ مثل بازی کلاسیک."
        1 -> if (en) "4 pads on the display. Same order, more to remember." else "چهار پد روی نمایشگر ؛ ترتیب بلندتر و سخت‌تر."
        else -> if (en) "4 pads and you must repeat the sequence BACKWARDS!" else "چهار پد و باید ترتیب رو برعکس تکرار کنی!"
    }
    2 -> when (level) {
        0 -> if (en) "2 fists, slow shuffle. Easy to follow the coin." else "دو مشت و جابه‌جایی آهسته ؛ دنبال کردن سکه آسونه."
        1 -> if (en) "2 fists, fast shuffle. When you hide, the car reads your habits." else "دو مشت و جابه‌جایی تند ؛ وقتی تو قایم میکنی ماکت عادتت رو میخونه."
        else -> if (en) "3 fists, very fast shuffle, and a smart car. Good luck!" else "سه مشت ، جابه‌جایی خیلی تند و ماکت زرنگ. موفق باشی!"
    }
    else -> when (level) {
        0 -> if (en) "The car picks at random." else "ماکت کاملاً شانسی انتخاب میکنه."
        1 -> if (en) "The car learns your habits (it never sees your pick)." else "ماکت از عادت‌هات یاد میگیره (انتخابت رو نمیبینه)."
        else -> if (en) "The car studies you hard: streaks, switches, everything." else "ماکت کامل مطالعه‌ت میکنه: تکرارها ، عوض کردن‌ها ، همه چی."
    }
}

private fun gameInfo(game: Int, en: Boolean): String = when (game) {
    1 -> if (en) "Watch the pads light up, then repeat the sequence. It gets longer and faster each round. Beat your record!"
         else "پدها یکی‌یکی روشن میشن ؛ همون ترتیب رو تکرار کن. هر مرحله بلندتر و تندتر میشه. رکوردت رو بشکن!"
    2 -> if (en) "One round the car hides the coin and shuffles the fists: follow it! Next round YOU hide and the car guesses. First to the target wins."
         else "یک دور ماکت سکه رو قایم و مشت‌ها رو جابه‌جا میکنه: دنبالش کن! دور بعد تو قایم میکنی و ماکت حدس میزنه. اولین نفر به هدف برنده است."
    else -> if (en) "The car counts rock... paper... scissors! Tap during the count or right after. The car locks its pick BEFORE you, so no cheating."
            else "ماکت میشمره: سنگ... کاغذ... قیچی! همون موقع یا کمی بعد بزن. ماکت قبل از تو انتخابش رو قفل میکنه ، پس تقلب نداره."
}

@Composable
fun GamesScreen(vm: MainViewModel) {
    val S = LocalStrings.current
    val en = S.lang == "en"
    fun tr(fa: String, e: String) = if (en) e else fa
    val haptic = LocalHapticFeedback.current
    val live = vm.live
    val st = live.s

    var game by remember { mutableStateOf(2) }
    var level by remember { mutableStateOf(1) }
    var target by remember { mutableStateOf(3) }

    // اگه بازی از خود ماکت شروع شد (مثلاً دکمه‌ی G) صفحه باهاش هماهنگ میشه
    LaunchedEffect(st.game, st.running) {
        if (st.running && st.game in 1..3) { game = st.game; level = st.level; if (st.game != 1) target = st.target }
    }
    // لرزش با اتفاق‌های بازی
    LaunchedEffect(st.seq) {
        when (st.ev) {
            3 -> when {
                st.game == 1 -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                st.lastWho == 1 -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                else -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            4 -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(160)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            2 -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

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

        // ---------- تنظیم و شروع ----------
        GameCard {
            val title = when (game) {
                1 -> tr("بازی حافظه", "Memory game")
                2 -> tr("گل یا پوچ", "Gol ya Pooch (hidden coin)")
                else -> tr("سنگ کاغذ قیچی", "Rock Paper Scissors")
            }
            Text(title, color = Silver, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(gameInfo(game, en), color = Steel, fontSize = 11.5.sp, lineHeight = 16.sp)

            Text(tr("سطح", "Level"), color = Silver, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (l in 0..2) {
                    PillButton(levelName(l, en), Violet, level == l, true, Modifier.weight(1f), 42.dp, onClick = { level = l })
                }
            }
            Text(levelInfo(game, level, en), color = Steel, fontSize = 11.5.sp, lineHeight = 16.sp)

            if (game != 1) {
                Text(tr("اولین نفر به چند امتیاز برسه؟", "First to how many points?"), color = Silver, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (t in listOf(3, 5, 7)) {
                        PillButton(t.toString(), Cyan, target == t, true, Modifier.weight(1f), 42.dp, onClick = { target = t })
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(
                    if (st.running && st.game == game) tr("از اول", "Restart") else tr("شروع بازی", "Start"),
                    Mint, true, true, Modifier.weight(1f), 50.dp,
                    onClick = { vm.sendLine("GAME:" + game + "," + (if (game == 1) 3 else target) + "," + level) }
                )
                PillButton(
                    tr("پایان بازی", "Stop"), Steel, false, true, Modifier.weight(1f), 50.dp,
                    onClick = { vm.sendLine("GAME:0") }
                )
            }
            if (!vm.connected) {
                Text(tr("اول به ماکت وصل شو.", "Connect to the car first."), color = Danger, fontSize = 11.5.sp)
            }
        }

        // ---------- امتیاز زنده ----------
        if (st.running && st.game in 1..3) {
            GameCard {
                if (st.game == 1) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text(tr("مرحله", "Level"), color = Steel, fontSize = 12.sp)
                            Text((if (st.round > 0) st.round else 1).toString(), color = Cyan, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text(tr("رکورد", "Best"), color = Steel, fontSize = 12.sp)
                            Text(live.bestMem.toString(), color = Gold, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text(tr("تو", "YOU"), color = Steel, fontSize = 12.sp)
                            Text(st.ps.toString(), color = Mint, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                            Text(pips(st.ps, st.target), color = Mint, fontSize = 13.sp)
                        }
                        Text(":", color = Steel, fontSize = 36.sp, modifier = Modifier.padding(top = 18.dp))
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text(tr("ماکت", "CAR"), color = Steel, fontSize = 12.sp)
                            Text(st.rs.toString(), color = Danger, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                            Text(pips(st.rs, st.target), color = Danger, fontSize = 13.sp)
                        }
                    }
                }

                // پیام وضعیت
                val (msg, col) = when {
                    st.game != 1 && st.matchWho == 1 -> Pair(tr("🏆 تو برنده‌ی بازی شدی!", "🏆 You won the match!"), Mint)
                    st.game != 1 && st.matchWho == 2 -> Pair(tr("🤖 ماکت برنده شد. دوباره؟", "🤖 The car won. Again?"), Danger)
                    st.game == 1 && st.memFail -> Pair(tr("اشتباه شد! دوباره شروع میشه…", "Wrong! Starting over…"), Danger)
                    st.waiting && st.game == 1 && st.reverse -> Pair(tr("🔁 برعکس تکرار کن!", "🔁 Repeat BACKWARDS!"), Gold)
                    st.waiting && st.game == 2 && !st.robotHides -> Pair(tr("🫳 کجا قایم میکنی؟", "🫳 Where do you hide it?"), Gold)
                    st.waiting -> Pair(tr("👉 نوبت توئه!", "👉 Your turn!"), Mint)
                    else -> Pair(tr("👀 ماکت داره بازی میکنه…", "👀 The car is playing…"), Steel)
                }
                Text(msg, color = col, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

                // نتیجه‌ی آخرین دور
                if (st.ev >= 3 && st.matchWho == 0 && !st.memFail) {
                    val last = when (st.game) {
                        3 -> when (st.lastWho) {
                            1 -> tr("این دور مال تو بود (ماکت: " + handEmoji(st.lastB) + ")", "You won this round (car: " + handEmoji(st.lastB) + ")")
                            2 -> tr("این دور مال ماکت بود (ماکت: " + handEmoji(st.lastB) + ")", "The car won this round (car: " + handEmoji(st.lastB) + ")")
                            else -> tr("مساوی (هر دو " + handEmoji(st.lastB) + ")", "Draw (both " + handEmoji(st.lastB) + ")")
                        }
                        2 -> {
                            val where = if (st.options == 3) listOf(tr("چپ", "left"), tr("وسط", "middle"), tr("راست", "right")) else listOf(tr("چپ", "left"), tr("راست", "right"))
                            val w = where.getOrElse(st.lastB) { "" }
                            when (st.lastWho) {
                                1 -> tr("درست حدس زدی! سکه دست $w بود", "You got it! The coin was in the $w hand")
                                else -> tr("نشد! سکه دست $w بود", "Missed! The coin was in the $w hand")
                            }
                        }
                        else -> tr("✔ مرحله‌ی " + st.ps + " درست بود", "✔ Level " + st.ps + " cleared")
                    }
                    Text(last, color = Steel, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                if (st.game != 1 && st.streak >= 2 && st.matchWho == 0) {
                    val who = if (st.streakWho == 1) tr("تو", "You") else tr("ماکت", "The car")
                    Text("🔥 " + who + " " + st.streak + tr(" تا پشت هم برده!", " wins in a row!"),
                        color = Gold, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // ---------- دکمه‌های بازی ----------
        GameCard {
            val ready = vm.connected
            val hot = st.waiting          // الان نوبت توئه: دکمه‌ها پررنگ
            val send = { k: Int ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                vm.sendLine("GP:" + k)
            }
            val ctlTitle = when {
                game == 2 && st.running && st.game == 2 && !st.robotHides -> tr("سکه رو توی کدوم دست قایم کنی؟", "Which hand hides the coin?")
                game == 2 -> tr("سکه کدوم دسته؟", "Which hand has the coin?")
                game == 1 -> tr("پدها", "Pads")
                else -> tr("انتخابت", "Your pick")
            }
            Text(ctlTitle, color = Silver, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            when (game) {
                1 -> {
                    val pads = if (st.running && st.game == 1) st.options else (if (level == 0) 2 else 4)
                    if (pads <= 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PillButton(tr("◀  چپ", "◀  Left"), Cyan, hot, ready, Modifier.weight(1f), 110.dp, onDown = { send(0) })
                            PillButton(tr("راست  ▶", "Right  ▶"), Gold, hot, ready, Modifier.weight(1f), 110.dp, onDown = { send(1) })
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PillButton("●", Cyan, hot, ready, Modifier.weight(1f), 80.dp, onDown = { send(0) })
                            PillButton("▲", Gold, hot, ready, Modifier.weight(1f), 80.dp, onDown = { send(1) })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PillButton("■", Mint, hot, ready, Modifier.weight(1f), 80.dp, onDown = { send(2) })
                            PillButton("◆", Violet, hot, ready, Modifier.weight(1f), 80.dp, onDown = { send(3) })
                        }
                    }
                }
                2 -> {
                    val hands = if (st.running && st.game == 2) st.options else (if (level >= 2) 3 else 2)
                    if (hands >= 3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillButton(tr("✊\nچپ", "✊\nLeft"), Gold, hot, ready, Modifier.weight(1f), 96.dp, onDown = { send(0) })
                            PillButton(tr("✊\nوسط", "✊\nMiddle"), Gold, hot, ready, Modifier.weight(1f), 96.dp, onDown = { send(1) })
                            PillButton(tr("✊\nراست", "✊\nRight"), Gold, hot, ready, Modifier.weight(1f), 96.dp, onDown = { send(2) })
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PillButton(tr("✊  دست چپ", "✊  Left hand"), Gold, hot, ready, Modifier.weight(1f), 96.dp, onDown = { send(0) })
                            PillButton(tr("دست راست  ✊", "Right hand  ✊"), Gold, hot, ready, Modifier.weight(1f), 96.dp, onDown = { send(1) })
                        }
                    }
                }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(tr("✊\nسنگ", "✊\nRock"), Mint, hot, ready, Modifier.weight(1f), 96.dp, onDown = { send(0) })
                    PillButton(tr("✋\nکاغذ", "✋\nPaper"), Mint, hot, ready, Modifier.weight(1f), 96.dp, onDown = { send(1) })
                    PillButton(tr("✌\nقیچی", "✌\nScissors"), Mint, hot, ready, Modifier.weight(1f), 96.dp, onDown = { send(2) })
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

        // ---------- آمار ----------
        GameCard {
            Text(tr("آمار تو", "Your stats"), color = Silver, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("گل یا پوچ", "Gol/Pooch"), color = Steel, fontSize = 13.sp)
                Text(tr("برد ", "W ") + live.winsGol + tr("  /  باخت ", "  /  L ") + live.lossGol, color = Gold, fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("سنگ کاغذ قیچی", "Rock Paper"), color = Steel, fontSize = 13.sp)
                Text(tr("برد ", "W ") + live.winsRps + tr("  /  باخت ", "  /  L ") + live.lossRps, color = Mint, fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("رکورد حافظه", "Memory record"), color = Steel, fontSize = 13.sp)
                Text(tr(live.bestMem.toString() + " مرحله", live.bestMem.toString() + " levels"), color = Cyan, fontSize = 13.sp)
            }
            PillButton(tr("پاک کردن آمار", "Reset stats"), Steel, false, true, Modifier.fillMaxWidth(), 40.dp, onClick = { live.resetStats() })
        }
        Spacer(Modifier.padding(bottom = 8.dp))
    }
}
