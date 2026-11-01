package com.mamad.peugeot405

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * صداهای گوشی برای ماکت: صدای موتور (ساخته‌شده با کد ، فرکانسش با ارتفاع بدنه بالا میره) ، بوق ،
 * افکت‌های احساسات و خواندن جمله‌های ماکت با موتور تبدیل متن به صدای گوشی.
 */
class SoundEngine(private val context: Context) {
    private val sr = 22050
    @Volatile private var revs = 0f
    private var engineJob: Job? = null
    private val main = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsOk = false
    private var ttsLang = ""

    private fun attrs(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private fun format(): AudioFormat = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(sr)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    fun setRevs(r: Float) {
        revs = r.coerceIn(0f, 1f)
    }

    val engineOn: Boolean get() = engineJob?.isActive == true

    fun startEngine(scope: CoroutineScope) {
        if (engineOn) return
        engineJob = scope.launch(Dispatchers.Default) {
            val minBuf = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder()
                .setAudioAttributes(attrs())
                .setAudioFormat(format())
                .setBufferSizeInBytes(maxOf(minBuf, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            try {
                track.play()
                val buf = ShortArray(441)                       // ۲۰ میلی‌ثانیه
                var phase = 0.0
                var f = 40.0
                var rnd = 1
                while (isActive) {
                    val r = revs
                    val target = 38.0 + 120.0 * r
                    f += (target - f) * 0.12
                    val step = 2.0 * PI * f / sr
                    val vol = 0.16 + 0.14 * r
                    for (i in buf.indices) {
                        phase += step
                        if (phase > 2.0 * PI) phase -= 2.0 * PI
                        rnd = rnd * 1103515245 + 12345
                        val noise = ((rnd shr 16) and 0x7fff) / 32768.0 - 0.5
                        val v = sin(phase) + 0.7 * sin(2.0 * phase + 0.3) + 0.45 * sin(3.0 * phase + 0.9) +
                            0.25 * sin(5.0 * phase) + 0.18 * noise
                        buf[i] = (v * vol * 9000.0).toInt().coerceIn(-32000, 32000).toShort()
                    }
                    track.write(buf, 0, buf.size)
                }
            } finally {
                runCatching { track.stop() }
                track.release()
            }
        }
    }

    fun stopEngine() {
        engineJob?.cancel()
        engineJob = null
    }

    // ---------- ساخت صدا از چند قطعه‌ی ساده ----------
    private class Part(
        val f0: Double, val f1: Double, val ms: Int,
        val vol: Double = 0.5, val square: Boolean = false, val noise: Boolean = false
    )

    private fun render(parts: List<Part>): ShortArray {
        var total = 0
        for (p in parts) total += sr * p.ms / 1000
        val out = ShortArray(total)
        var idx = 0
        var phase = 0.0
        var rnd = 7
        for (p in parts) {
            val n = sr * p.ms / 1000
            for (i in 0 until n) {
                val t = i.toDouble() / n
                val f = p.f0 + (p.f1 - p.f0) * t
                phase += 2.0 * PI * f / sr
                val env = minOf(1.0, minOf(t * 12.0, (1.0 - t) * 6.0))
                val v: Double
                if (p.noise) {
                    rnd = rnd * 1103515245 + 12345
                    v = ((rnd shr 16) and 0x7fff) / 32768.0 - 0.5
                } else {
                    val s = sin(phase)
                    v = if (p.square) (if (s >= 0) 0.6 else -0.6) else s
                }
                out[idx++] = (v * env * p.vol * 20000.0).toInt().coerceIn(-32000, 32000).toShort()
            }
        }
        return out
    }

    private fun playPcm(pcm: ShortArray) {
        if (pcm.isEmpty()) return
        val track = AudioTrack.Builder()
            .setAudioAttributes(attrs())
            .setAudioFormat(format())
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        track.play()
        val ms = pcm.size * 1000L / sr + 300L
        main.postDelayed({
            runCatching { track.stop() }
            track.release()
        }, ms)
    }

    fun horn() {
        playPcm(render(listOf(Part(392.0, 392.0, 260, 0.6, true), Part(523.0, 523.0, 420, 0.6, true))))
    }

    // kind: happy love scared angry sad wow yawn dizzy sneeze hiccup
    fun effect(kind: String) {
        val parts = when (kind) {
            "happy" -> listOf(Part(500.0, 900.0, 140, 0.45), Part(900.0, 1150.0, 130, 0.45))
            "love" -> listOf(Part(880.0, 880.0, 120, 0.35), Part(0.0, 0.0, 40, 0.0), Part(1040.0, 1040.0, 170, 0.35))
            "scared" -> listOf(Part(900.0, 300.0, 280, 0.5, true))
            "angry" -> listOf(Part(120.0, 80.0, 300, 0.6, true))
            "sad" -> listOf(Part(520.0, 300.0, 380, 0.4))
            "wow" -> listOf(Part(300.0, 950.0, 170, 0.45))
            "yawn" -> listOf(Part(420.0, 190.0, 650, 0.3))
            "dizzy" -> listOf(Part(400.0, 700.0, 90, 0.4), Part(700.0, 400.0, 90, 0.4), Part(400.0, 700.0, 90, 0.4))
            "sneeze" -> listOf(Part(0.0, 0.0, 220, 0.0), Part(0.0, 0.0, 170, 0.5, false, true))
            "hiccup" -> listOf(Part(700.0, 480.0, 80, 0.5))
            else -> return
        }
        playPcm(render(parts))
    }

    // ---------- خواندن جمله‌ها ----------
    private fun applyLang(lang: String) {
        val t = tts ?: return
        if (ttsLang == lang) return
        ttsLang = lang
        val loc = if (lang == "fa") Locale("fa") else Locale.US
        val r = t.setLanguage(loc)
        t.setPitch(1.35f)
        t.setSpeechRate(1.05f)
        ttsOk = (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED)
    }

    fun speak(text: String, lang: String) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsReady = true
                    applyLang(lang)
                    if (ttsOk) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "robot")
                }
            }
            return
        }
        if (!ttsReady) return
        applyLang(lang)
        if (ttsOk) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "robot")
    }

    fun release() {
        stopEngine()
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        ttsLang = ""
    }
}
