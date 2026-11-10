package com.mamad.peugeot405

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// ======================================================================
//  «مغز ماکت» (v33) : وضعیت رابطه / نیازها / امتیاز که ماکت با خط ST: میفرسته ،
//  ذخیره‌ی امتیازها روی گوشی (برای #SCORE موقع اتصال) ، لرزش (V:) و فرمان صوتی (#CMD:)
//  هماهنگ با car_emo_eyes_v33 (emo_brain.h)
//  ST:aff,xp,ach,lights,games,dances,chats,chalDay,chalDone,stage,energy,boredom,need,part,kind,prog,target
// ======================================================================
class BrainState(private val ctx: Context) {
    private val p = ctx.getSharedPreferences("brain", Context.MODE_PRIVATE)

    var st by mutableStateOf<List<Int>>(emptyList()); private set
    var note by mutableStateOf(""); private set
    var vibrate by mutableStateOf(p.getBoolean("vib", true)); private set

    // ۹ عدد اول ST = همون چیزی که #SCORE برمیگردونه
    fun savedScore(): String? = p.getString("score", null)

    fun onStatus(line: String) {
        val a = line.removePrefix("ST:").split(',').mapNotNull { it.trim().toIntOrNull() }
        if (a.size < 9) return
        st = a
        p.edit().putString("score", a.take(9).joinToString(",")).apply()
    }

    // عادت اومدن (v34): ماکت HB:آخرین_روز,c0..c23 میفرسته ؛ اینجا نگه میداریم و موقع اتصال با #HAB: برمیگردونیم
    fun habLine(): String = p.getString("hab", null) ?: ("0," + List(24) { 0 }.joinToString(","))

    fun onHab(line: String) {
        val a = line.removePrefix("HB:").split(',').mapNotNull { it.trim().toIntOrNull() }
        if (a.size >= 25) p.edit().putString("hab", a.take(25).joinToString(",")).apply()
    }

    fun setNote(s: String) { note = s }

    fun setVibrate(v: Boolean) {
        vibrate = v
        p.edit().putBoolean("vib", v).apply()
    }

    @Suppress("DEPRECATION")
    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31)
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    fun buzz(ms: Int) {
        if (!vibrate) return
        try {
            vibrator()?.vibrate(VibrationEffect.createOneShot(ms.toLong().coerceIn(20L, 800L), VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Throwable) {
        }
    }

    companion object {
        val ACH_FA = listOf("صد بار چراغ", "رفیق جونی", "شب‌زنده‌دار", "بازیکن حرفه‌ای", "رقصنده", "سحرخیز")
        val ACH_EN = listOf("100 light taps", "Best friend", "Night owl", "Pro gamer", "Dancer", "Early bird")
        val CHAL_FA = listOf("۱۰ بار چراغ بزن", "۳ بار با من حرف بزن", "۲ بازی بکن", "یه رقص میباخ ببین")
        val CHAL_EN = listOf("Tap the lights 10 times", "Talk to me 3 times", "Play 2 games", "Watch one Maybach dance")
        val STAGE_FA = listOf("غریبه", "آشنا", "دوست", "صمیمی")
        val STAGE_EN = listOf("Stranger", "Acquaintance", "Friend", "Best friend")
    }
}

// فرمان صوتی با SpeechRecognizer اندروید (فارسی ؛ معمولا به اینترنت گوشی نیاز داره)
class VoiceCmd(private val ctx: Context, private val onCmd: (String, String) -> Unit) {
    var on by mutableStateOf(false); private set
    var err by mutableStateOf(""); private set

    private var sr: SpeechRecognizer? = null
    private val h = Handler(Looper.getMainLooper())

    private fun toCmd(t: String): String? {
        val w = t.trim().split(' ')
        return when {
            "دوستت دارم" in t || "دوست دارم" in t -> "LOVE"
            "سلام" in t -> "HELLO"
            "بخواب" in t || "شب بخیر" in t -> "SLEEP"
            "بیدار" in t -> "WAKE"
            "قفل" in t -> "LOCK"
            "باز کن" in t || "باز شو" in t -> "UNLOCK"
            "برقص" in t || "میباخ" in t -> "DANCE"
            "بس" in w || "بایست" in t || "وایسا" in t -> "STOP"
            "مرسی" in t || "ممنون" in t -> "THANKS"
            else -> null
        }
    }

    fun start() {
        if (on) return
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) { err = "NA"; return }
        err = ""
        on = true
        listen()
    }

    fun stop() {
        on = false
        h.removeCallbacksAndMessages(null)
        runCatching { sr?.destroy() }
        sr = null
    }

    private fun listen() {
        if (!on) return
        if (sr == null) {
            sr = SpeechRecognizer.createSpeechRecognizer(ctx).also { r ->
                r.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                    override fun onResults(results: Bundle?) {
                        val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (t != null) toCmd(t)?.let { onCmd(it, t) }
                        again(250)
                    }
                    override fun onError(error: Int) {
                        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) { err = "PERM"; stop(); return }
                        again(if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) 200 else 1200)
                    }
                })
            }
        }
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        runCatching { sr?.startListening(i) }
    }

    private fun again(ms: Long) {
        if (!on) return
        h.postDelayed({ listen() }, ms)
    }
}

// تکون دادن گوشی: سه تکون تند پشت‌سرهم ← #SHAKE (ماکت گیج میشه ، بار چهارم عصبانی)
class ShakeDetector(ctx: Context, private val onShake: () -> Unit) : SensorEventListener {
    private val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    var on by mutableStateOf(false); private set
    private val hits = ArrayList<Long>()
    private var last = 0L

    fun start(): Boolean {
        if (on) return true
        val s = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return false
        on = sm.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME)
        return on
    }

    fun stop() {
        runCatching { sm.unregisterListener(this) }
        on = false
        hits.clear()
    }

    override fun onSensorChanged(e: SensorEvent) {
        val g = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2])
        if (abs(g - 9.8f) < 14f) return
        val now = SystemClock.uptimeMillis()
        if (hits.isNotEmpty() && now - hits.last() < 120L) return
        hits.add(now)
        hits.removeAll { now - it > 1200L }
        if (hits.size >= 3 && now - last > 2500L) {
            last = now
            hits.clear()
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
