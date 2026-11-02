package com.mamad.peugeot405

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun seg(p: Float, a: Float, b: Float): Float = ((p - a) / (b - a)).coerceIn(0f, 1f)
private fun easeOut(x: Float): Float = 1f - (1f - x) * (1f - x) * (1f - x)

/**
 * ورود «Maison 405»:
 *  ۱) یک خط نور افقی از وسط باز میشه  ۲) شیر پژو از تاریکی بیرون میاد و نور روش میکشه
 *  ۳) حلقه‌های موئی شامپاینی مثل موج پخش میشن  ۴) اسم و آرم ۴۰۵ GLX و شعار میان
 * با یک لمس رد میشه.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val prog = remember { Animatable(0f) }
    var done by remember { mutableStateOf(false) }
    val finish = {
        if (!done) {
            done = true
            onFinished()
        }
    }
    LaunchedEffect(Unit) {
        prog.animateTo(1f, tween(3800, easing = LinearEasing))
        finish()
    }

    val p = prog.value
    val beam = easeOut(seg(p, 0.02f, 0.30f))
    val lionIn = easeOut(seg(p, 0.14f, 0.50f))
    val sweep = seg(p, 0.42f, 0.70f)
    val nameIn = easeOut(seg(p, 0.46f, 0.72f))
    val numIn = easeOut(seg(p, 0.58f, 0.82f))
    val tagIn = easeOut(seg(p, 0.72f, 0.92f))
    val outA = 1f - seg(p, 0.94f, 1f)

    Backdrop(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { finish() } }) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = outA }) {
            val c = Offset(size.width / 2f, size.height * 0.42f)
            val maxR = size.width * 0.85f
            for (i in 0..2) {
                val t = (p * 1.7f - i * 0.2f).coerceIn(0f, 1f)
                if (t > 0f) {
                    drawCircle(Gold.copy(alpha = (1f - t) * 0.32f), maxR * t, c, style = Stroke(1.dp.toPx()))
                }
            }
            val bw = size.width * 0.9f * beam
            if (bw > 4f) {
                drawLine(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Silver.copy(alpha = 0.75f), Color.Transparent),
                        startX = c.x - bw / 2f, endX = c.x + bw / 2f
                    ),
                    Offset(c.x - bw / 2f, c.y), Offset(c.x + bw / 2f, c.y), 1.dp.toPx()
                )
            }
        }
        Column(
            Modifier.align(Alignment.Center).graphicsLayer { alpha = outA },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painterResource(R.drawable.peugeot_lion), null,
                Modifier.size(width = 150.dp, height = 150.dp * LION_RATIO)
                    .graphicsLayer {
                        val sc = 0.80f + 0.20f * lionIn
                        scaleX = sc
                        scaleY = sc
                        alpha = lionIn
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        if (sweep > 0.001f && sweep < 0.999f) {
                            val w = size.width
                            val x = -w * 0.5f + w * 2f * sweep
                            drawRect(
                                Brush.linearGradient(
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent),
                                    start = Offset(x - w * 0.25f, 0f),
                                    end = Offset(x + w * 0.25f, size.height * 0.35f)
                                ),
                                blendMode = BlendMode.SrcAtop
                            )
                        }
                    },
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "PEUGEOT", color = Silver, fontSize = 15.sp, fontWeight = FontWeight.Light,
                letterSpacing = (28f - 20f * nameIn).sp,
                modifier = Modifier.graphicsLayer { alpha = nameIn }
            )
            Spacer(Modifier.height(14.dp))
            GlxBadge(
                1.6f,
                Modifier.graphicsLayer {
                    alpha = numIn
                    translationY = (1f - numIn) * 18.dp.toPx()
                }
            )
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier.width((120f * tagIn).dp).height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent)))
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "i-CONTROL  ·  PNEUMATIC SUSPENSION", color = Steel, fontSize = 9.5.sp, letterSpacing = 3.sp,
                modifier = Modifier.graphicsLayer { alpha = tagIn }
            )
        }
    }
}
