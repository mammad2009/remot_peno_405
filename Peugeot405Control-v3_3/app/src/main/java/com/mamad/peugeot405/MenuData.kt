package com.mamad.peugeot405

// آیتم‌های منوی ماکت (شماره‌ها دقیقا با کد ESP یکی است؛ ترتیب را عوض نکن)
data class Named(val id: Int, val icon: String, val fa: String, val en: String) {
    fun name(S: Strings): String = if (S.lang == "en") en else fa
}

// حالت‌های چشم (enum P_ در کد ماکت)
val POSES = listOf(
    Named(0, "😐", "عادی", "Normal"), Named(1, "👈", "نگاه چپ", "Look left"), Named(2, "👉", "نگاه راست", "Look right"),
    Named(3, "👆", "نگاه بالا", "Look up"), Named(4, "👇", "نگاه پایین", "Look down"), Named(5, "↖️", "بالا-چپ", "Up-left"),
    Named(6, "↘️", "پایین-راست", "Down-right"), Named(7, "😊", "خوشحال", "Happy"), Named(8, "😄", "خیلی خوشحال", "Very happy"),
    Named(9, "🤩", "هیجان‌زده", "Excited"), Named(10, "😠", "عصبانی", "Angry"), Named(11, "😢", "غمگین", "Sad"),
    Named(12, "🥱", "خواب‌آلود", "Sleepy"), Named(13, "😴", "خواب", "Asleep"), Named(14, "😲", "متعجب", "Surprised"),
    Named(15, "😱", "ترسیده", "Scared"), Named(16, "😉", "چشمک راست", "Wink right"), Named(17, "😜", "چشمک چپ", "Wink left"),
    Named(18, "🤨", "مشکوک", "Suspicious"), Named(19, "😕", "گیج", "Confused"), Named(20, "😏", "پوزخند", "Smug"),
    Named(21, "🥺", "بامزه", "Cute"), Named(22, "⬆️", "بلند شدن", "Rising"), Named(23, "⬇️", "نشستن", "Lowering"),
    Named(24, "💫", "افتادن", "Drop"), Named(25, "😂", "خنده", "Laugh"), Named(26, "😮", "وای!", "Wow"),
    Named(27, "😒", "غرغرو", "Grumpy"), Named(28, "😍", "عاشق", "Love"), Named(29, "✨", "ستاره‌ای", "Star eyes"),
    Named(30, "😵", "سرگیجه", "Dizzy"), Named(31, "😳", "خجالتی", "Shy"), Named(32, "😈", "شیطان", "Evil"),
    Named(33, "🧐", "متمرکز", "Focused")
)

// واکنش‌های کوچک بین صحنه‌ها (MK_ در کد ماکت)
val REACTIONS = listOf(
    Named(0, "🤧", "عطسه", "Sneeze"), Named(1, "😬", "سکسکه", "Hiccup"), Named(2, "👀", "نگاه به اطراف", "Look around"),
    Named(3, "🔍", "کنجکاوی", "Curious"), Named(4, "🥱", "خمیازه", "Yawn"), Named(5, "🤭", "قهقهه", "Giggle"),
    Named(6, "😉", "چشمک", "Wink"), Named(7, "🙄", "چرخاندن چشم", "Eye roll"), Named(8, "😤", "غرغر", "Grumble"),
    Named(9, "💕", "عشق", "Love"), Named(10, "🌟", "برق چشم", "Sparkle"), Named(11, "🎯", "تمرکز", "Focus"),
    Named(12, "😈", "شیطنت", "Mischief"), Named(13, "🌀", "سرگیجه", "Dizzy")
)

// صحنه‌های کامل (scripts در کد ماکت)
val SCENES = listOf(
    Named(0, "😎", "پز دادن: من ۴۰۵ام", "Showing off"), Named(1, "🛣️", "جاده‌ی خالی", "Empty road"),
    Named(2, "☕", "چایی", "Tea time"), Named(3, "😨", "ترس و لاتی", "Scare & tough talk"),
    Named(4, "🤝", "آشتی", "Making up"), Named(5, "😘", "مخ‌زنی", "Flirting")
)

// متن‌های ماکت (به ترتیب menuTxtTab در کد ماکت)
val TEXTS = listOf(
    Named(0, "💬", "ای بابا خواب بودما!", "Hey, I was asleep!"), Named(1, "💬", "بذار بخوابم بابا", "Let me sleep, man"),
    Named(2, "💬", "ممد! دعوا داری؟!", "Mamad! Want a fight?!"), Named(3, "💬", "همین الان میام!", "Coming right now!"),
    Named(4, "💬", "هاا؟! چی شد؟!", "Huh?! What happened?!"), Named(5, "💬", "دزد اومد؟!", "A thief?!"),
    Named(6, "💬", "خوابم میاد", "I'm sleepy"), Named(7, "💬", "حالا چی می‌خوای؟", "What do you want now?"),
    Named(8, "💬", "اومدی؟ دلم تنگ شد!", "You're back? I missed you!"), Named(9, "💬", "کسی نیست...؟", "Anybody there...?"),
    Named(10, "💬", "تنهام گذاشتی...", "You left me alone..."), Named(11, "💬", "دیشبم نیومدی!", "You didn't come last night!"),
    Named(12, "💬", "منو یادت رفت؟", "Did you forget me?"), Named(13, "💬", "خسته‌ام... می‌خوابم", "I'm tired... going to sleep"),
    Named(14, "💬", "پلیس نیست؟ راه بیا!", "No cops? Let's go!"), Named(15, "💬", "من ۴۰۵ام، جانم!", "I'm a 405, baby!"),
    Named(16, "💬", "چی؟ من چطورم؟", "What? How am I?"), Named(17, "💬", "چیه داداش؟ هاا؟", "What's up bro? Huh?"),
    Named(18, "💬", "عشقی ♥", "Lovely ♥"), Named(19, "💬", "هاهاها! خنده داشت!", "Hahaha! That was funny!"),
    Named(20, "💬", "خیابون خالیه بگازیم؟", "Street's empty, floor it?"), Named(21, "💬", "یه چایی نمی‌خوای؟", "Want some tea?"),
    Named(22, "💬", "پس گاز بده! ویییی!", "Then step on it! Weeee!"), Named(23, "💬", "ماشین پرواز کرد لامصب!", "The car just flew!"),
    Named(24, "💬", "عشق کردی!", "You enjoyed that!"), Named(25, "💬", "ویییییر...", "Weeeeer..."),
    Named(26, "💬", "آخیش... راحت", "Ahh... relaxed"), Named(27, "💬", "میباخ! بزن بریم!", "Maybach! Let's go!"),
    Named(28, "💬", "خوب بود، نه؟", "Good, wasn't it?"), Named(29, "💬", "شما خیاطی؟", "Are you a tailor?"),
    Named(30, "💬", "چون نگاهمو بهت دوختی!", "Because I've sewn my gaze on you!"), Named(31, "💬", "جات تو آینه بغل ماشینه", "Your place is in my side mirror"),
    Named(32, "💬", "هی! پلیس اومد؟!", "Hey! Cops?!"), Named(33, "💬", "نه بابا، ترسیدم!", "No, I just got scared!"),
    Named(34, "💬", "ما اهل دعوا نیستیم...", "We don't fight..."), Named(35, "💬", "ولی دعوا کنی، هستیم!", "But if you do, we're in!"),
    Named(36, "💬", "چاکریم، مخلصیم!", "Your humble servant!"), Named(37, "💬", "قفل شدم! دست نزنین!", "I'm locked! Don't touch!"),
    Named(38, "💬", "ماشین قفله داداش!", "Car's locked, bro!"), Named(39, "💬", "سلام ممد! منتظرت بودم", "Hi Mamad! I was waiting")
)

// افکت‌های چراغ (کیت راهنمای عربی در کد ماکت): out 1 = راست ، 2 = چپ ، 3 = هر دو ، 0 = خاموش ، ms = مدت
class LStep(val out: Int, val ms: Int)

val LIGHT_FX = listOf(
    Named(0, "🚨", "عربی", "Arabic"), Named(1, "👮", "پلیسی", "Police"), Named(2, "⚡", "استروب", "Strobe"),
    Named(3, "🌊", "موج", "Wave"), Named(4, "💓", "ضربان قلب", "Heartbeat"), Named(5, "🏁", "سریع", "Fast")
)

val LIGHT_SEQ: List<List<LStep>> = listOf(
    listOf(LStep(1, 90), LStep(0, 90), LStep(1, 90), LStep(0, 90), LStep(2, 90), LStep(0, 90), LStep(2, 90), LStep(0, 90)),
    listOf(LStep(1, 50), LStep(0, 50), LStep(1, 50), LStep(0, 50), LStep(1, 50), LStep(0, 50),
        LStep(2, 50), LStep(0, 50), LStep(2, 50), LStep(0, 50), LStep(2, 50), LStep(0, 50)),
    listOf(LStep(3, 45), LStep(0, 45), LStep(3, 45), LStep(0, 45), LStep(3, 45), LStep(0, 45),
        LStep(3, 45), LStep(0, 45), LStep(3, 45), LStep(0, 45), LStep(0, 300)),
    listOf(LStep(1, 120), LStep(3, 120), LStep(2, 120), LStep(3, 120)),
    listOf(LStep(3, 80), LStep(0, 80), LStep(3, 80), LStep(0, 500)),
    listOf(LStep(1, 70), LStep(2, 70))
)

// خروجی راهنماها در لحظه‌ی t میلی‌ثانیه (همان منطق lightsTick کد ماکت): 1 = راست ، 2 = چپ ، 3 = هر دو
fun turnOutput(mode: Int, fx: Int, t: Long): Int = when (mode) {
    1 -> if ((t / 350) % 2 == 0L) 1 else 0
    2 -> if ((t / 350) % 2 == 0L) 2 else 0
    3 -> if ((t / 350) % 2 == 0L) 3 else 0
    4 -> when ((t / 250) % 4) { 0L -> 1; 2L -> 2; else -> 0 }
    5 -> seqOutput(fx, t)
    else -> 0
}

fun seqOutput(fx: Int, t: Long): Int {
    val seq = LIGHT_SEQ[fx.coerceIn(0, LIGHT_SEQ.size - 1)]
    var total = 0L
    for (st in seq) total += st.ms
    var x = t % total
    for (st in seq) {
        if (x < st.ms) return st.out
        x -= st.ms
    }
    return 0
}
