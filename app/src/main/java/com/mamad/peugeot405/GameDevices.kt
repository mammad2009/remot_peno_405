package com.mamad.peugeot405

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// ======================================================================
//  ۱) سنسور کج شدن گوشی (فرمان ماشین!)
//  pitch = کج شدن بالا/پایین گوشی (الاکلنگی جلو/عقب) ، roll = کج شدن چپ/راست
// ======================================================================
class TiltSensor(ctx: Context) : SensorEventListener {
    private val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? =
        sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val isGravity = sensor?.type == Sensor.TYPE_GRAVITY

    val available: Boolean get() = sensor != null
    var started = false; private set
    var pitch by mutableFloatStateOf(0f); private set
    var roll by mutableFloatStateOf(0f); private set

    private var fx = 0f
    private var fy = 0f
    private var fz = 0f
    private var first = true

    fun start() {
        val s = sensor ?: return
        if (started) return
        first = true
        started = sm.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        if (!started) return
        sm.unregisterListener(this)
        started = false
    }

    override fun onSensorChanged(e: SensorEvent) {
        val a = if (isGravity) 0.5f else 0.18f          // شتاب‌سنج خام نویز دارد؛ نرمش می‌کنیم
        if (first) {
            fx = e.values[0]; fy = e.values[1]; fz = e.values[2]
            first = false
        } else {
            fx += a * (e.values[0] - fx)
            fy += a * (e.values[1] - fy)
            fz += a * (e.values[2] - fz)
        }
        pitch = Math.toDegrees(atan2(fz.toDouble(), fy.toDouble())).toFloat()
        roll = Math.toDegrees(atan2(-fx.toDouble(), hypot(fy.toDouble(), fz.toDouble()))).toFloat()
    }

    override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
}

// ======================================================================
//  ۲) تشخیص ضرب موسیقی با میکروفون گوشی
//  صدا رو فیلتر پایین‌گذر میکنیم (طبل/بیس) و هر وقت انرژیش از میانگین یک ثانیه‌ی اخیر بیشتر شد = یک ضرب
// ======================================================================
class BeatListener(private val onBeat: (Int) -> Unit) {
    var active by mutableStateOf(false); private set
    var level by mutableFloatStateOf(0f); private set
    var bpm by mutableIntStateOf(0); private set
    var count by mutableIntStateOf(0); private set
    @Volatile var sensitivity = 0.8f                       // 0 .. 1  (پیش‌فرض حساس)
    @Volatile private var running = false
    private val main = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running) return true
        val sr = 16000
        val minBuf = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) return false
        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, sr, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, max(minBuf, 4096)
            )
        } catch (e: Throwable) { return false }
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return false }
        try { rec.startRecording() } catch (e: Throwable) { rec.release(); return false }
        running = true
        active = true
        count = 0
        bpm = 0
        Thread { loop(rec, sr) }.also { it.isDaemon = true; it.start() }
        return true
    }

    fun stop() {
        running = false
        active = false
        level = 0f
        bpm = 0
    }

    private fun loop(rec: AudioRecord, sr: Int) {
        val frame = 512                                     // ≈ ۳۲ میلی‌ثانیه
        val buf = ShortArray(frame)
        val alpha = 1f - exp(-2f * PI.toFloat() * 170f / sr)  // پایین‌گذر ≈ ۱۷۰ هرتز
        var lp = 0f
        val hist = FloatArray(43)                           // ≈ ۱.۴ ثانیه
        var hn = 0
        var hi = 0
        var last = 0L
        val iv = ArrayList<Long>()
        var smooth = 0f
        try {
            while (running) {
                val n = rec.read(buf, 0, frame)
                if (n < 0) break
                if (n == 0) continue
                var eLow = 0.0
                for (i in 0 until n) {
                    val s = buf[i] / 32768f
                    lp += alpha * (s - lp)
                    eLow += (lp * lp).toDouble()
                }
                val low = sqrt(eLow / n).toFloat()
                var avg = 0f
                for (i in 0 until hn) avg += hist[i]
                if (hn > 0) avg /= hn
                val now = SystemClock.uptimeMillis()
                val thr = 1.5f - 0.42f * sensitivity                 // آخر اسلایدر: فقط ۸٪ بالاتر از میانگین = ضرب
                if (hn >= 12 && low > 0.0015f + 0.004f * (1f - sensitivity) && low > avg * thr && now - last > 230L) {
                    val strength = ((low / avg - 1f) * 120f + 60f).toInt().coerceIn(55, 100)   // همیشه قوی
                    if (last != 0L && now - last < 2000L) {
                        iv.add(now - last)
                        if (iv.size > 8) iv.removeAt(0)
                    } else iv.clear()
                    last = now
                    var b = 0
                    if (iv.size >= 3) {
                        val s = iv.sorted()
                        b = (60000L / s[s.size / 2]).toInt()
                        while (b > 180) b /= 2
                        while (b in 1..59) b *= 2
                    }
                    main.post {
                        if (running) {
                            count++
                            bpm = b
                            onBeat(strength)
                        }
                    }
                }
                hist[hi] = low
                hi = (hi + 1) % hist.size
                if (hn < hist.size) hn++
                smooth += 0.35f * (min(1f, low * 14f) - smooth)
                level = smooth
            }
        } catch (e: Throwable) {
        } finally {
            try { rec.stop() } catch (e: Throwable) {}
            rec.release()
            running = false
            main.post { active = false; level = 0f }
        }
    }
}

// ======================================================================
//  ۳) صدای ماکت: بوق‌های رباتی (ساخته‌شده با کد ، بدون فایل صوتی) + خوندن متن‌ها با TTS گوشی
//  ماکت وقتی #EV:1 باشه حالت چشم / متن / واکنش کوچکش رو میفرسته و اینجا صداش درمیاد
// ======================================================================
// باز کردن تنظیمات «تبدیل متن به گفتار» گوشی (برای نصب صدای فارسی یا عوض کردن موتور)
fun openTtsSettings(ctx: Context) {
    val tries = listOf(Intent("com.android.settings.TTS_SETTINGS"), Intent(android.provider.Settings.ACTION_SETTINGS))
    for (i in tries) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
            return
        } catch (e: Throwable) {
        }
    }
}

class CarVoice(private val ctx: Context) {
    var chirps by mutableStateOf(true)
    var speak by mutableStateOf(false)
    var speakReady by mutableStateOf(true)                  // false = صدای زبان انتخابی روی گوشی نصب نیست
    @Volatile var muted = false                             // وقتی میکروفون برای رقص باز است صدا قطع (که ضرب اشتباه نگیرد)

    private val ex = Executors.newSingleThreadExecutor()
    private val pending = AtomicInteger(0)
    private var tts: TextToSpeech? = null
    private var ttsOk = false
    private var ttsLang = ""
    @Volatile private var fallbackEn = false                 // فارسی نصب نیست ← با صدای انگلیسی بخون

    private class N(val f0: Float, val f1: Float, val ms: Int, val v: Float = 0.5f, val noise: Boolean = false)

    private fun n(f0: Int, f1: Int, ms: Int, v: Float = 0.5f) = N(f0.toFloat(), f1.toFloat(), ms, v)
    private fun r(ms: Int) = N(0f, 0f, ms)

    private fun synth(notes: List<N>): ShortArray {
        val sr = 22050
        var total = 0
        for (x in notes) total += x.ms * sr / 1000
        val out = ShortArray(total)
        var p = 0
        var phase = 0.0
        val rnd = java.util.Random(7)
        for (x in notes) {
            val len = x.ms * sr / 1000
            for (i in 0 until len) {
                val t = i.toFloat() / max(1, len)
                val env = min(1f, min(i / 176f, (len - i) / 264f)).coerceAtLeast(0f)
                var s = 0f
                if (x.noise) {
                    s = rnd.nextFloat() * 2f - 1f
                } else if (x.f0 > 0f) {
                    val f = x.f0 + (x.f1 - x.f0) * t
                    phase += 2.0 * PI * f / sr
                    s = (sin(phase) + 0.25 * sin(2.0 * phase)).toFloat()
                }
                out[p + i] = (s * env * x.v * 20000f).toInt().coerceIn(-32000, 32000).toShort()
            }
            p += len
        }
        return out
    }

    private fun play(notes: List<N>) {
        if (!chirps || muted) return
        if (pending.get() > 1) return
        pending.incrementAndGet()
        ex.execute {
            try {
                val pcm = synth(notes)
                val at = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(22050).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                at.write(pcm, 0, pcm.size)
                at.play()
                Thread.sleep(pcm.size * 1000L / 22050 + 60)
                at.release()
            } catch (e: Throwable) {
            } finally {
                pending.decrementAndGet()
            }
        }
    }

    // حالت چشم (شماره‌ها مثل POSES در MenuData)
    fun onPose(id: Int) {
        val s: List<N>? = when (id) {
            7 -> listOf(n(600, 900, 90), n(900, 1200, 120))
            8, 25 -> listOf(n(700, 1000, 70), r(30), n(800, 1100, 70), r(30), n(900, 1400, 110))
            9 -> listOf(n(500, 1500, 200, 0.4f), n(1500, 1200, 80))
            10, 27 -> listOf(n(220, 150, 220, 0.6f))
            11 -> listOf(n(700, 400, 300, 0.45f))
            12 -> listOf(n(400, 250, 350, 0.35f))
            13 -> listOf(n(300, 180, 500, 0.3f))
            14, 26 -> listOf(n(500, 1400, 130))
            15 -> (0 until 5).flatMap { listOf(n(1100, 1300, 40), n(1300, 1100, 40)) }
            16, 17 -> listOf(n(900, 1300, 60))
            18 -> listOf(n(500, 500, 80), r(30), n(700, 700, 100))
            19 -> listOf(n(600, 900, 120), n(900, 600, 140))
            20 -> listOf(n(300, 420, 160))
            21, 31 -> listOf(n(900, 1250, 110, 0.35f), n(1250, 1000, 90, 0.3f))
            22 -> listOf(n(300, 900, 260, 0.4f))
            23 -> listOf(n(900, 300, 260, 0.4f))
            24 -> listOf(n(1200, 200, 350, 0.45f))
            28 -> listOf(n(700, 900, 90), r(20), n(900, 1200, 140))
            29 -> listOf(n(1500, 1500, 50), n(1900, 1900, 50), n(2400, 2400, 90, 0.4f))
            30 -> listOf(n(500, 900, 100), n(900, 400, 100), n(400, 800, 100), n(800, 300, 140))
            32 -> listOf(n(400, 300, 110), n(300, 200, 110), n(250, 150, 200, 0.55f))
            33 -> listOf(n(800, 800, 60), r(40), n(800, 800, 60))
            else -> null
        }
        if (s != null) play(s)
    }

    // واکنش‌های کوچک (شماره‌ها مثل REACTIONS)
    fun onMicro(k: Int) {
        val s: List<N>? = when (k) {
            0 -> listOf(n(300, 900, 120, 0.4f), r(40), N(0f, 0f, 160, 0.5f, true))
            1 -> listOf(n(500, 450, 50), r(120), n(520, 430, 50))
            2 -> listOf(n(700, 700, 30, 0.3f), r(150), n(800, 800, 30, 0.3f))
            3 -> listOf(n(500, 800, 160, 0.4f))
            4 -> listOf(n(600, 200, 700, 0.35f))
            5 -> listOf(n(800, 1100, 60), r(30), n(850, 1150, 60), r(30), n(900, 1200, 60))
            6 -> listOf(n(900, 1300, 60))
            7 -> listOf(n(400, 900, 150, 0.4f), n(900, 400, 150, 0.4f))
            8 -> listOf(n(200, 140, 260, 0.6f))
            9 -> listOf(n(700, 700, 90), r(20), n(900, 900, 130))
            10 -> listOf(n(2000, 2000, 40, 0.3f), n(2600, 2600, 40, 0.3f), n(3200, 3200, 70, 0.3f))
            11 -> listOf(n(500, 500, 220, 0.3f))
            12 -> listOf(n(350, 250, 120), n(300, 200, 160))
            13 -> listOf(n(500, 900, 100), n(900, 400, 100), n(400, 800, 120))
            else -> null
        }
        if (s != null) play(s)
    }

    // متن ماکت (شماره‌ی TEXTS) با صدای گوشی
    fun say(i: Int, en: Boolean) {
        if (!speak || muted) return
        ensureTts(en)
        if (!ttsOk) return
        val t = TEXTS.getOrNull(i) ?: return
        val raw = if (en || fallbackEn) t.en else t.fa
        val clean = raw.replace("♥", "").replace("…", " ").replace("...", " ").replace("؟!", "؟").trim()
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "405")
    }

    fun prepareSpeak(en: Boolean) = ensureTts(en)

    private fun ensureTts(en: Boolean) {
        val lang = if (en) "en" else "fa"
        if (tts != null && ttsLang == lang) return
        tts?.shutdown()
        tts = null
        ttsOk = false
        fallbackEn = false
        ttsLang = lang
        // موتورهای مختلف فارسی رو با اسم‌های مختلفی معرفی میکنن ؛ همه رو امتحان کن
        val cands = if (en) listOf(Locale.US, Locale.ENGLISH)
        else listOf(Locale("fa", "IR"), Locale("fa"), Locale("fa", "AF"))
        var holder: TextToSpeech? = null
        holder = TextToSpeech(ctx.applicationContext) { st ->
            val x = holder
            var wanted = false
            if (st == TextToSpeech.SUCCESS && x != null) {
                for (l in cands) {
                    val r = x.setLanguage(l)
                    if (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED) { wanted = true; break }
                }
                ttsOk = wanted
                if (!wanted && !en) {                       // صدای فارسی نیست: حداقل انگلیسی بخونه
                    val r = x.setLanguage(Locale.US)
                    if (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED) {
                        ttsOk = true
                        fallbackEn = true
                    }
                }
                x.setPitch(1.45f)
                x.setSpeechRate(1.05f)
                // موتور پیش‌فرض فارسی نداره: بقیه‌ی موتورهای نصب‌شده رو یکی‌یکی امتحان کن
                // (مثلاً eSpeak NG ؛ لازم نیست پیش‌فرض گوشی باشه)
                if (!wanted && !en) {
                    val others = try {
                        x.engines.map { it.name }.filter { it != x.defaultEngine }
                    } catch (e: Throwable) { emptyList() }
                    tryEngine(others, 0, cands, x)
                }
            } else ttsOk = false
            speakReady = wanted
        }
        tts = holder
    }

    private fun tryEngine(list: List<String>, i: Int, cands: List<Locale>, base: TextToSpeech) {
        if (i >= list.size) return
        var h: TextToSpeech? = null
        h = TextToSpeech(ctx.applicationContext, TextToSpeech.OnInitListener { st ->
            val y = h
            var ok = false
            if (st == TextToSpeech.SUCCESS && y != null && ttsLang == "fa") {
                for (l in cands) {
                    val r = y.setLanguage(l)
                    if (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED) { ok = true; break }
                }
            }
            if (ok && y != null) {
                y.setPitch(1.45f)
                y.setSpeechRate(1.05f)
                tts = y
                ttsOk = true
                fallbackEn = false
                speakReady = true
                try { base.shutdown() } catch (e: Throwable) {}
            } else {
                try { y?.shutdown() } catch (e: Throwable) {}
                if (ttsLang == "fa") tryEngine(list, i + 1, cands, base)
            }
        }, list[i])
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ex.shutdown()
    }
}
