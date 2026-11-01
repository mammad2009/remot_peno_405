package com.mamad.peugeot405

import android.content.Context

// یک نمونه از حرکت ضبط‌شده: t = میلی‌ثانیه از شروع ، h = ارتفاع هر دو (0..100) ، d = الاکلنگی (-100..100)
data class MotionSample(val t: Int, val h: Int, val d: Int)

data class Motion(val name: String, val samples: List<MotionSample>) {
    val durationMs: Int get() = samples.lastOrNull()?.t ?: 0
}

/** حرکت‌های ضبط‌شده در حافظه‌ی گوشی ذخیره می‌شوند (نه ماکت). */
class MotionStore(context: Context) {
    private val prefs = context.getSharedPreferences("motions", Context.MODE_PRIVATE)

    fun load(): List<Motion> {
        val raw = prefs.getString("list", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        val out = ArrayList<Motion>()
        for (line in raw.split('\n')) {
            val i = line.indexOf('\t')
            if (i <= 0) continue
            val name = line.substring(0, i)
            val samples = ArrayList<MotionSample>()
            for (p in line.substring(i + 1).split(';')) {
                val a = p.split(':')
                if (a.size != 3) continue
                val t = a[0].toIntOrNull()
                val h = a[1].toIntOrNull()
                val d = a[2].toIntOrNull()
                if (t != null && h != null && d != null) samples.add(MotionSample(t, h, d))
            }
            if (samples.size >= 2) out.add(Motion(name, samples))
        }
        return out
    }

    fun save(list: List<Motion>) {
        val text = list.joinToString("\n") { m ->
            m.name.replace('\n', ' ').replace('\t', ' ') + "\t" +
                m.samples.joinToString(";") { "${it.t}:${it.h}:${it.d}" }
        }
        prefs.edit().putString("list", text).apply()
    }
}
