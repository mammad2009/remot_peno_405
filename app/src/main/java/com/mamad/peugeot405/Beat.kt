package com.mamad.peugeot405

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * تشخیص ضرب موسیقی با میکروفن گوشی: فقط صداهای بم (زیر حدود ۵۰۰ هرتز) را می‌شنود و
 * وقتی انرژی ناگهان از میانگین چند ثانیه‌ی اخیر بالاتر برود «ضرب» اعلام می‌کند.
 */
@SuppressLint("MissingPermission")
class BeatDetector {
    private var job: Job? = null

    fun start(scope: CoroutineScope, onBeat: (Int) -> Unit, onLevel: (Float) -> Unit) {
        stop()
        job = scope.launch(Dispatchers.Default) {
            val sr = 16000
            val minBuf = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val rec = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC, sr, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, 4096)
                )
            } catch (e: Exception) {
                null
            }
            if (rec == null) return@launch
            if (rec.state != AudioRecord.STATE_INITIALIZED) {
                rec.release()
                return@launch
            }
            try {
                rec.startRecording()
                val buf = ShortArray(320)                    // ۲۰ میلی‌ثانیه
                var lp = 0f
                var avg = 0.02f
                var last = 0L
                var frame = 0
                while (isActive) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n <= 0) continue
                    var sum = 0.0
                    for (i in 0 until n) {
                        val x = buf[i] / 32768f
                        lp += 0.18f * (x - lp)               // فیلتر پایین‌گذر: فقط بم
                        sum += (lp * lp).toDouble()
                    }
                    val rms = sqrt(sum / n).toFloat()
                    avg = avg * 0.97f + rms * 0.03f
                    val now = SystemClock.uptimeMillis()
                    if (rms > avg * 1.5f && rms > 0.01f && now - last > 220) {
                        last = now
                        val strength = (40f + (rms / avg - 1.5f) * 60f).coerceIn(35f, 100f).toInt()
                        onBeat(strength)
                    }
                    frame++
                    if (frame % 3 == 0) onLevel((rms * 10f).coerceIn(0f, 1f))
                }
            } finally {
                runCatching { rec.stop() }
                rec.release()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
