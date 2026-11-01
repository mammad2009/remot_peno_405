package com.mamad.peugeot405

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// یک رویداد ضبط‌شده: t = میلی‌ثانیه از شروع ، k = نوع
//  G = ارتفاع+الاکلنگی (a=h , b=d)   E = نگاه چشم (a=x , b=y)   R = ول کردن پد چشم
//  B = ضرب موسیقی (a=قدرت)           D = رقص روشن/خاموش (a=1/0)   P = ایموجی چشم (a=شماره‌ی حالت)
class GameEv(val t: Int, val k: Char, val a: Int, val b: Int)
class GameRec(val dur: Int, val ev: List<GameEv>)

/**
 * «i-Cockpit 405» ؛ حالت بازی ماکت (کد ماکت v27 : #MODE:1)
 *  کج کردن گوشی → #GM:h,d      پد لمسی → #GM و #EYE:x,y      موسیقی → #DANCE و #BEAT:n
 *  حافظه (مثل صندلی حافظه‌دار پژو): ضبط و پخش دوباره‌ی همه‌ی اینها در سه خانه‌ی M1 M2 M3
 */
class GameController(
    private val ctx: Context,
    private val scope: CoroutineScope,
    private val tx: (String) -> Unit
) {
    val tilt = TiltSensor(ctx)
    val voice = CarVoice(ctx)
    val beat = BeatListener { s -> onBeat(s) }
    private val prefs = ctx.getSharedPreferences("game", Context.MODE_PRIVATE)

    var on by mutableStateOf(false); private set
    var sub by mutableIntStateOf(0)                       // 0 کج کردن ، 1 پد ، 2 رقص

    // آنچه به ماکت میگیم
    var h by mutableIntStateOf(0); private set            // ارتفاع هر دو سروو 0..100
    var d by mutableIntStateOf(0); private set            // الاکلنگی -100..100 (مثبت = جلو بالا)
    var ex by mutableIntStateOf(0); private set           // نگاه چشم‌ها (جهت صفحه‌ی ماکت: y مثبت = پایین)
    var ey by mutableIntStateOf(0); private set
    var eyesHeld by mutableStateOf(false); private set

    // کج کردن
    var tiltOn by mutableStateOf(false); private set
    var invert by mutableStateOf(false)
    var heightByRoll by mutableStateOf(false)
    var zeroP by mutableFloatStateOf(0f); private set
    var zeroR by mutableFloatStateOf(0f); private set
    private var calAt = 0L

    // آنچه ماکت میگه
    var mood by mutableIntStateOf(-1); private set        // آخرین حالت چشم ماکت
    var wordsIdx by mutableIntStateOf(-1); private set    // آخرین متن ماکت

    // حافظه
    val slots = mutableStateListOf<GameRec?>(null, null, null)
    var slotSel by mutableIntStateOf(0)
    var recording by mutableStateOf(false); private set
    var recSecs by mutableIntStateOf(0); private set
    var playing by mutableIntStateOf(-1); private set
    var loop by mutableStateOf(false)

    private var pump: Job? = null
    private var playJob: Job? = null
    private var playTok = 0
    private var sentH = -1
    private var sentD = 999
    private var sentEx = 999
    private var sentEy = 999
    private var eyeAt = 0L
    private val recBuf = ArrayList<GameEv>()
    private var recT0 = 0L

    init {
        for (i in 0..2) slots[i] = loadSlot(i)
        voice.chirps = prefs.getBoolean("chirps", true)
        voice.speak = prefs.getBoolean("speak", false)
        invert = prefs.getBoolean("invert", false)
        heightByRoll = prefs.getBoolean("roll", false)
    }

    // ---------------- ورود / خروج ----------------
    fun enter(info: Map<String, Int>) {
        if (on) return
        val rf = info["rf"] ?: 80
        val xf = info["xf"] ?: 112
        val pf = info["pf"] ?: rf
        h = if (xf > rf) ((pf - rf) * 100 / (xf - rf)).coerceIn(0, 100) else 0   // از جای فعلی شروع کن (بدون پرش)
        d = 0; ex = 0; ey = 0
        eyesHeld = false
        sentH = h; sentD = 0; sentEx = 999; sentEy = 999
        on = true
        tx("#EV:1\n")
        startPump()
    }

    fun leave() {
        if (!on) return
        stopPlay()
        stopRec(true)
        stopBeat()
        tiltStop()
        pump?.cancel()
        pump = null
        on = false
        eyesHeld = false
        tx("#DANCE:0\n")
        tx("#EV:0\n")
    }

    fun shutdown() {
        tilt.stop()
        beat.stop()
        voice.shutdown()
    }

    // ---------------- فرستادن (هر ۶۶ میلی‌ثانیه فقط وقتی چیزی عوض شده) ----------------
    private fun startPump() {
        pump?.cancel()
        pump = scope.launch {
            while (true) {
                delay(66)
                val now = SystemClock.uptimeMillis()
                if (tiltOn && playing < 0 && tilt.started) tiltStep(now)
                if (h != sentH || d != sentD) {
                    sentH = h; sentD = d
                    tx("#GM:$h,$d\n")
                    rec('G', h, d)
                }
                if (eyesHeld) {
                    val changed = ex != sentEx || ey != sentEy
                    if (changed) rec('E', ex, ey)
                    if (changed || now - eyeAt > 300L) {          // ماکت نگاه رو ۰.۷ ثانیه نگه میداره ؛ پس مرتب تازه‌اش کن
                        sentEx = ex; sentEy = ey; eyeAt = now
                        tx("#EYE:$ex,$ey\n")
                    }
                }
                if (recording) {
                    recSecs = ((now - recT0) / 1000L).toInt()
                    if (now - recT0 > 90000L) stopRec(true)
                }
            }
        }
    }

    // ---------------- کج کردن ----------------
    fun tiltStart() {
        if (!on || !tilt.available) return
        tilt.start()
        tiltOn = true
        calAt = SystemClock.uptimeMillis() + 300L
    }

    fun tiltStop() {
        tiltOn = false
        tilt.stop()
    }

    fun calibrate() {
        zeroP = tilt.pitch
        zeroR = tilt.roll
    }

    private fun tiltStep(now: Long) {
        if (calAt != 0L) {
            if (now < calAt) return
            calibrate()
            calAt = 0L
        }
        val sgn = if (invert) -1f else 1f
        var v = (tilt.pitch - zeroP) * sgn
        if (abs(v) < 1.5f) v = 0f
        d = (v / 28f * 100f).roundToInt().coerceIn(-100, 100)
        if (heightByRoll) {
            val r = (tilt.roll - zeroR) / 28f
            h = (50f + r * 50f).roundToInt().coerceIn(0, 100)
        }
    }

    fun updateInvert(v: Boolean) { invert = v; prefs.edit().putBoolean("invert", v).apply() }
    fun updateHeightByRoll(v: Boolean) { heightByRoll = v; prefs.edit().putBoolean("roll", v).apply() }

    // ---------------- ورودی کاربر (هنگام پخش حافظه نادیده) ----------------
    fun userBody(nh: Int, nd: Int) {
        if (playing >= 0) return
        h = nh.coerceIn(0, 100)
        d = nd.coerceIn(-100, 100)
    }

    fun userHeight(nh: Int) {
        if (playing >= 0) return
        h = nh.coerceIn(0, 100)
    }

    fun userEyes(x: Int, y: Int) {
        if (playing >= 0) return
        ex = x.coerceIn(-100, 100)
        ey = y.coerceIn(-100, 100)
        eyesHeld = true
    }

    fun eyesRelease() {
        if (!eyesHeld) return
        eyesHeld = false
        sentEx = 999
        rec('R', 0, 0)
    }

    fun emote(id: Int) {
        if (!on || playing >= 0) return
        tx("#POSE:$id\n")
        rec('P', id, 0)
    }

    // ---------------- رقص با موسیقی ----------------
    private fun onBeat(s: Int) {
        if (!on) return
        tx("#BEAT:$s\n")
        rec('B', s, 0)
    }

    private fun setDance(v: Boolean) {
        tx(if (v) "#DANCE:1\n" else "#DANCE:0\n")
        rec('D', if (v) 1 else 0, 0)
    }

    fun startBeat(): Boolean {
        if (!on || playing >= 0) return false
        if (beat.active) return true
        val ok = beat.start()
        if (ok) {
            voice.muted = true                 // صدای ماکت قاطی میکروفون نشه
            setDance(true)
        }
        return ok
    }

    fun stopBeat() {
        if (!beat.active) return
        beat.stop()
        voice.muted = false
        setDance(false)
    }

    // ---------------- صدا ----------------
    fun setChirps(v: Boolean) { voice.chirps = v; prefs.edit().putBoolean("chirps", v).apply() }

    fun setSpeak(v: Boolean, en: Boolean) {
        voice.speak = v
        prefs.edit().putBoolean("speak", v).apply()
        if (v) voice.prepareSpeak(en)
    }

    // خط‌هایی که ماکت میفرسته: EV:P7  EV:T12  EV:M3
    fun onEvent(line: String, en: Boolean) {
        if (!on || line.length < 5) return
        val v = line.substring(4).toIntOrNull() ?: return
        when (line[3]) {
            'P' -> { mood = v; voice.onPose(v) }
            'T' -> { wordsIdx = v; voice.say(v, en) }
            'M' -> voice.onMicro(v)
        }
    }

    // ---------------- حافظه (ضبط / پخش) ----------------
    private fun rec(k: Char, a: Int, b: Int) {
        if (!recording) return
        recBuf.add(GameEv((SystemClock.uptimeMillis() - recT0).toInt(), k, a, b))
    }

    fun startRec() {
        if (!on || recording || playing >= 0) return
        recBuf.clear()
        recT0 = SystemClock.uptimeMillis()
        recording = true
        recSecs = 0
        recBuf.add(GameEv(0, 'G', h, d))                       // شروع از همین حالت
        if (beat.active) recBuf.add(GameEv(0, 'D', 1, 0))
    }

    fun stopRec(save: Boolean) {
        if (!recording) return
        recording = false
        if (save && recBuf.size > 1) {
            val dur = (SystemClock.uptimeMillis() - recT0).toInt().coerceAtLeast(300)
            slots[slotSel] = GameRec(dur, ArrayList(recBuf))
            saveSlot(slotSel)
        }
        recBuf.clear()
    }

    fun play(i: Int) {
        val r = slots[i] ?: return
        if (!on) return
        stopRec(true)
        stopBeat()
        playJob?.cancel()
        val tok = ++playTok
        playing = i
        playJob = scope.launch {
            var danced = false
            try {
                do {
                    val t0 = SystemClock.uptimeMillis()
                    for (e in r.ev) {
                        val wait = t0 + e.t - SystemClock.uptimeMillis()
                        if (wait > 0) delay(wait)
                        when (e.k) {
                            'G' -> { h = e.a; d = e.b }
                            'E' -> { ex = e.a; ey = e.b; eyesHeld = true }
                            'R' -> { eyesHeld = false; sentEx = 999 }
                            'B' -> tx("#BEAT:${e.a}\n")
                            'D' -> { danced = e.a == 1; tx(if (e.a == 1) "#DANCE:1\n" else "#DANCE:0\n") }
                            'P' -> tx("#POSE:${e.a}\n")
                        }
                    }
                    val rest = t0 + r.dur - SystemClock.uptimeMillis()
                    if (rest > 0) delay(rest)
                } while (loop)
            } finally {
                eyesHeld = false
                sentEx = 999
                if (danced) tx("#DANCE:0\n")
                if (tok == playTok) playing = -1
            }
        }
    }

    fun stopPlay() {
        playJob?.cancel()
        playJob = null
        playTok++
        playing = -1
    }

    fun deleteSlot(i: Int) {
        if (playing == i) stopPlay()
        slots[i] = null
        saveSlot(i)
    }

    private fun saveSlot(i: Int) {
        val r = slots[i]
        if (r == null) { prefs.edit().remove("s$i").apply(); return }
        val sb = StringBuilder().append(r.dur).append('|')
        for (e in r.ev) sb.append(e.t).append(',').append(e.k).append(',').append(e.a).append(',').append(e.b).append(';')
        prefs.edit().putString("s$i", sb.toString()).apply()
    }

    private fun loadSlot(i: Int): GameRec? {
        val raw = prefs.getString("s$i", null) ?: return null
        return runCatching {
            val p = raw.split('|')
            val dur = p[0].toInt()
            val ev = p[1].split(';').filter { it.isNotEmpty() }.map {
                val q = it.split(',')
                GameEv(q[0].toInt(), q[1][0], q[2].toInt(), q[3].toInt())
            }
            GameRec(dur, ev)
        }.getOrNull()
    }
}
