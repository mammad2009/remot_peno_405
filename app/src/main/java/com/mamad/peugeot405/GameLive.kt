package com.mamad.peugeot405

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// ======================================================================
//  وضعیت زنده‌ی بازی‌های روی نمایشگر ماکت (نیاز به car_emo_eyes_v32 یا جدیدتر)
//  ماکت خط  GS:بازی,رویداد,امتیاز تو,امتیاز ماکت,a,b  میفرسته:
//    رویداد 0 شروع (a=target , b=level)        1 دور تازه        2 منتظر انتخاب تو (a=تعداد گزینه‌ها , b=ثانیه یا برعکس)
//    3 نتیجه‌ی دور (a: 0 مساوی / 1 تو / 2 ماکت ؛ حافظه: 1 درست , b=امتیاز)
//    4 پایان (a=برنده ؛ حافظه: b=رکورد)        5 توقف        6 برد پشت‌هم
// ======================================================================

data class GState(
    val game: Int = 0,              // 1 حافظه ، 2 گل یا پوچ ، 3 سنگ کاغذ قیچی
    val running: Boolean = false,
    val ev: Int = -1,               // آخرین رویداد
    val seq: Int = 0,               // با هر خط تازه زیاد میشه (برای لرزش گوشی)
    val ps: Int = 0,                // امتیاز تو (حافظه: امتیاز فعلی)
    val rs: Int = 0,                // امتیاز ماکت (حافظه: رکورد)
    val target: Int = 3,
    val level: Int = 1,
    val options: Int = 2,           // تعداد دکمه‌های فعال
    val waiting: Boolean = false,   // الان نوبت انتخاب توئه
    val robotHides: Boolean = true, // گل یا پوچ: ماکت قایم کرده و تو حدس میزنی
    val reverse: Boolean = false,   // حافظه: برعکس بزن
    val round: Int = 0,
    val lastWho: Int = -1,          // آخرین دور: 0 مساوی ، 1 تو ، 2 ماکت
    val lastB: Int = 0,             // سنگ‌کاغذ: انتخاب ماکت ؛ گل‌پوچ: جای سکه
    val streak: Int = 0,
    val streakWho: Int = 0,
    val matchWho: Int = 0,          // پایان مسابقه: 1 تو بردی ، 2 ماکت برد
    val memFail: Boolean = false    // حافظه: اشتباه زدی
)

class GameLive(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("game_stats", Context.MODE_PRIVATE)

    var s by mutableStateOf(GState()); private set

    // آمار همیشگی (روی گوشی میمونه)
    var winsGol by mutableStateOf(prefs.getInt("wg", 0)); private set
    var lossGol by mutableStateOf(prefs.getInt("lg", 0)); private set
    var winsRps by mutableStateOf(prefs.getInt("wr", 0)); private set
    var lossRps by mutableStateOf(prefs.getInt("lr", 0)); private set
    var bestMem by mutableStateOf(prefs.getInt("bm", 0)); private set

    fun resetStats() {
        winsGol = 0; lossGol = 0; winsRps = 0; lossRps = 0; bestMem = 0
        prefs.edit().clear().apply()
    }

    fun onLine(line: String) {
        val p = line.substring(3).split(',').mapNotNull { it.trim().toIntOrNull() }
        if (p.size < 6) return
        val g = p[0]; val ev = p[1]; val ps = p[2]; val rs = p[3]; val a = p[4]; val b = p[5]
        var n = s.copy(game = g, ev = ev, seq = s.seq + 1, ps = ps, rs = rs)
        when (ev) {
            0 -> n = n.copy(
                running = true, waiting = false, target = a, level = b, round = 0,
                lastWho = -1, streak = 0, streakWho = 0, matchWho = 0, memFail = false
            )
            1 -> n = n.copy(
                running = true, waiting = false, matchWho = 0, memFail = false,
                round = if (g == 2) n.round + 1 else a,
                robotHides = if (g == 2) a == 1 else n.robotHides,
                options = if (g == 3) 3 else b
            )
            2 -> n = n.copy(waiting = true, options = a, reverse = (g == 1 && b == 1))
            3 -> {
                var st = n.streak; var sw = n.streakWho
                if (g != 1 && a != 0) { if (sw == a) st++ else { st = 1; sw = a } }
                n = n.copy(waiting = false, lastWho = a, lastB = b, streak = st, streakWho = sw)
            }
            4 -> {
                n = n.copy(waiting = false, matchWho = if (g == 1) 0 else a, memFail = (g == 1 && a == 2))
                when (g) {
                    2 -> if (a == 1) { winsGol++; prefs.edit().putInt("wg", winsGol).apply() } else { lossGol++; prefs.edit().putInt("lg", lossGol).apply() }
                    3 -> if (a == 1) { winsRps++; prefs.edit().putInt("wr", winsRps).apply() } else { lossRps++; prefs.edit().putInt("lr", lossRps).apply() }
                    1 -> if (b > bestMem) { bestMem = b; prefs.edit().putInt("bm", bestMem).apply() }
                }
            }
            5 -> n = GState(game = g, ev = 5, seq = n.seq)
        }
        s = n
    }

    fun clear() { s = GState(seq = s.seq + 1) }
}
