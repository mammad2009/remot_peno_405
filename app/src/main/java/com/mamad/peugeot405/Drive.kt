package com.mamad.peugeot405

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ======================================================================
//  «Drive» : صفحه‌ی تمام‌صفحه‌ی رانندگی با گوشی
//   ۱) صفحه کامل تاریک میشه   ۲) جلوپنجره‌ی پژو ۴۰۵ با چراغ‌های روشن از تاریکی میاد وسط
//   ۳) گوشی رو به هر سمت کج کنی یه فلش همون سمت ظاهر میشه و سرووها با سرعت ثابت حرکت میکنن
//   ۴) گوشی صاف شد → فلش میره و سرووها همون‌جا که بودن میایستن
//  جهت‌ها:  بالا = هر دو بالا ، پایین = هر دو پایین ، راست = جلو بالا ، چپ = عقب بالا
// ======================================================================
@Composable
fun DriveOverlay(vm: MainViewModel) {
    val g = vm.game
    val S = LocalStrings.current
    val en = S.lang == "en"
    fun tr(fa: String, e: String) = if (en) e else fa
    val haptic = LocalHapticFeedback.current

    BackHandler(enabled = true) { g.closeDrive() }
    LaunchedEffect(vm.foreground) { if (!vm.foreground) g.closeDrive() }

    // ورود: اول تاریکی ، بعد جلوپنجره
    val dark = remember { Animatable(0f) }
    val grille = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        dark.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        grille.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
    }

    val dir = g.driveDir
    LaunchedEffect(dir) {
        if (dir != 0) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    val aUp by animateFloatAsState(if (dir == 1) 1f else 0f, tween(170), label = "dUp")
    val aDn by animateFloatAsState(if (dir == 2) 1f else 0f, tween(170), label = "dDn")
    val aLf by animateFloatAsState(if (dir == 3) 1f else 0f, tween(170), label = "dLf")
    val aRt by animateFloatAsState(if (dir == 4) 1f else 0f, tween(170), label = "dRt")
    val inf = rememberInfiniteTransition(label = "drive")
    val pulse by inf.animateFloat(
        0f, 1f, infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dPulse"
    )
    val breathe by inf.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dBreathe"
    )

    // فلش‌ها فیزیکی‌اند: چپ همیشه چپ (مستقل از فارسی/انگلیسی)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = dark.value }.background(Color.Black)
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            // هاله‌ی وسط + نور لبه‌ی جهت فعال
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height * 0.46f)
                drawCircle(
                    Brush.radialGradient(
                        listOf(Cyan.copy(alpha = (0.10f + 0.04f * breathe) * grille.value), Color.Transparent),
                        center = c, radius = size.width * 0.85f
                    ),
                    radius = size.width * 0.85f, center = c
                )
                val e = size.minDimension * 0.34f
                if (aUp > 0.01f) drawRect(
                    Brush.verticalGradient(listOf(Cyan.copy(alpha = 0.30f * aUp), Color.Transparent), 0f, e),
                    Offset.Zero, Size(size.width, e)
                )
                if (aDn > 0.01f) drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, Cyan.copy(alpha = 0.30f * aDn)), size.height - e, size.height),
                    Offset(0f, size.height - e), Size(size.width, e)
                )
                if (aLf > 0.01f) drawRect(
                    Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.30f * aLf), Color.Transparent), 0f, e),
                    Offset.Zero, Size(e, size.height)
                )
                if (aRt > 0.01f) drawRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, Gold.copy(alpha = 0.30f * aRt)), size.width - e, size.width),
                    Offset(size.width - e, 0f), Size(e, size.height)
                )
            }

            // جلوپنجره‌ی پژو ۴۰۵ (وسط صفحه)
            Column(
                Modifier.align(Alignment.Center).offset(y = (-6).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(width = 300.dp, height = 176.dp).graphicsLayer {
                        val s = 0.72f + 0.28f * grille.value
                        scaleX = s
                        scaleY = s
                        alpha = grille.value
                        translationY = (1f - grille.value) * 46.dp.toPx()
                    }
                ) {
                    val on = ((grille.value - 0.55f) / 0.45f).coerceIn(0f, 1f)
                    Canvas(Modifier.fillMaxSize()) { drawFront405(on, breathe) }
                    Image(
                        painterResource(R.drawable.peugeot_lion), null,
                        Modifier.align(Alignment.TopCenter).offset(y = 18.dp)
                            .size(width = 46.dp, height = 46.dp * LION_RATIO),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(Modifier.height(14.dp))
                GlxBadge(1.1f, Modifier.graphicsLayer { alpha = grille.value })
            }

            // چهار فلش جهت
            DriveArrow(aUp, pulse, 0f, -1f, tr("بدنه بالا", "BODY UP"), Cyan, Modifier.align(Alignment.TopCenter).padding(top = 96.dp))
            DriveArrow(aDn, pulse, 0f, 1f, tr("بدنه پایین", "BODY DOWN"), Cyan, Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp))
            DriveArrow(aLf, pulse, -1f, 0f, tr("عقب بالا", "TAIL UP"), Gold, Modifier.align(Alignment.CenterStart).padding(start = 10.dp))
            DriveArrow(aRt, pulse, 1f, 0f, tr("جلو بالا", "NOSE UP"), Gold, Modifier.align(Alignment.CenterEnd).padding(end = 10.dp))

            // نوار بالا: عنوان و خروج
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)
                    .graphicsLayer { alpha = grille.value },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("DRIVE", color = Gold, fontSize = 11.sp, letterSpacing = 5.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "H ${g.h}%   ·   ${if (g.d > 0) "+" else ""}${g.d}",
                        color = Steel, fontSize = 11.sp, letterSpacing = 1.sp
                    )
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Surface2)
                        .border(1.dp, Hair2, CircleShape).clickable { g.closeDrive() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Silver, modifier = Modifier.size(22.dp))
                }
            }

            // پایین: راهنما و دکمه‌ها
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)
                    .graphicsLayer { alpha = grille.value },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!g.tilt.available) {
                    Text(
                        tr("این گوشی سنسور کج شدن ندارد", "No tilt sensor on this phone"),
                        color = Danger, fontSize = 13.sp, textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        tr(
                            "گوشی رو به هر سمت کج کن ؛ صاف که کنی ماکت همون‌جا می‌ایسته",
                            "Tilt the phone any way — level it and the car stops where it is"
                        ),
                        color = Steel.copy(alpha = 1f - 0.7f * (aUp + aDn + aLf + aRt).coerceIn(0f, 1f)),
                        fontSize = 12.sp, lineHeight = 17.sp, textAlign = TextAlign.Center
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(
                        tr("صفر کن", "ZERO"), Cyan, false, true, Modifier.weight(1f), 50.dp,
                        onClick = { g.driveCalibrate() }
                    )
                    PillButton(
                        tr("برعکس", "FLIP"), Violet, g.invert, true, Modifier.weight(1f), 50.dp,
                        onClick = { g.updateInvert(!g.invert) }
                    )
                }
            }
        }
    }
}

// یک فلش سه‌تایی که فقط وقتی جهتش فعاله دیده میشه ؛ کمی در جهت خودش میتپه
@Composable
private fun DriveArrow(
    a: Float, pulse: Float, dx: Float, dy: Float, label: String, tint: Color, modifier: Modifier
) {
    if (a < 0.01f) return
    // فلش پایه رو به بالاست ؛ برای بقیه‌ی جهت‌ها میچرخه
    val rot = when {
        dy < 0f -> 0f
        dy > 0f -> 180f
        dx > 0f -> 90f
        else -> -90f
    }
    Column(
        modifier.graphicsLayer {
            alpha = a
            translationX = dx * pulse * 12.dp.toPx()
            translationY = dy * pulse * 12.dp.toPx()
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.graphicsLayer { rotationZ = rot }) {
            Chevrons(true, 3, tint, 92.dp)
        }
        Text(label, color = tint, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
    }
}

// نمای روبه‌روی جلوی ۴۰۵ با طراحی اختصاصی (خطی و ساده): بدنه ، دو چراغ ، جلوپنجره ، سپر
private fun DrawScope.drawFront405(on: Float, breathe: Float) {
    val w = size.width
    val h = size.height
    val sw = 1.6.dp.toPx()

    // انعکاس نور روی زمین
    drawOval(
        Brush.radialGradient(
            listOf(Cyan.copy(alpha = 0.20f * on), Color.Transparent),
            center = Offset(w / 2f, h * 0.97f), radius = w * 0.55f
        ),
        topLeft = Offset(w * 0.04f, h * 0.90f), size = Size(w * 0.92f, h * 0.20f)
    )

    // بدنه: کاپوت و گلگیرها
    val body = Path().apply {
        moveTo(0f, h * 0.86f)
        lineTo(w * 0.02f, h * 0.50f)
        quadraticBezierTo(w * 0.07f, h * 0.22f, w * 0.26f, h * 0.16f)
        lineTo(w * 0.74f, h * 0.16f)
        quadraticBezierTo(w * 0.93f, h * 0.22f, w * 0.98f, h * 0.50f)
        lineTo(w, h * 0.86f)
        close()
    }
    drawPath(body, Brush.verticalGradient(listOf(Surface3, Panel, Bg)))
    drawPath(body, Silver.copy(alpha = 0.38f), style = Stroke(sw))
    // خط وسط کاپوت
    drawLine(Hair, Offset(w * 0.34f, h * 0.17f), Offset(w * 0.30f, h * 0.36f), sw)
    drawLine(Hair, Offset(w * 0.66f, h * 0.17f), Offset(w * 0.70f, h * 0.36f), sw)

    // چراغ‌ها
    for (m in 0..1) {
        fun x(v: Float): Float = if (m == 0) w * v else w * (1f - v)
        val lamp = Path().apply {
            moveTo(x(0.06f), h * 0.42f)
            lineTo(x(0.31f), h * 0.35f)
            lineTo(x(0.31f), h * 0.62f)
            lineTo(x(0.07f), h * 0.64f)
            close()
        }
        drawPath(lamp, Brush.verticalGradient(listOf(Surface4, Bg)))
        val cx = x(0.19f)
        val cy = h * 0.50f
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.85f * on), Cyan.copy(alpha = 0.35f * on * (0.85f + 0.15f * breathe)), Color.Transparent),
                center = Offset(cx, cy), radius = w * 0.20f
            ),
            radius = w * 0.20f, center = Offset(cx, cy)
        )
        drawCircle(Silver.copy(alpha = 0.55f + 0.35f * on), h * 0.065f, Offset(cx, cy), style = Stroke(sw))
        drawCircle(Color.White.copy(alpha = 0.3f + 0.7f * on), h * 0.03f, Offset(cx, cy))
        drawPath(lamp, Silver.copy(alpha = 0.7f), style = Stroke(sw))
    }

    // جلوپنجره
    val gx = w * 0.36f
    val gy = h * 0.40f
    val gw = w * 0.28f
    val gh = h * 0.25f
    drawRoundRect(Panel, Offset(gx, gy), Size(gw, gh), CornerRadius(w * 0.03f))
    for (i in 1..4) {
        val y = gy + gh * i / 5f
        drawLine(Silver.copy(alpha = 0.32f), Offset(gx + w * 0.02f, y), Offset(gx + gw - w * 0.02f, y), sw)
    }
    drawRoundRect(Silver.copy(alpha = 0.85f), Offset(gx, gy), Size(gw, gh), CornerRadius(w * 0.03f), style = Stroke(sw))
    drawLine(Silver.copy(alpha = 0.7f), Offset(w * 0.34f, h * 0.37f), Offset(w * 0.66f, h * 0.37f), sw * 1.5f)

    // سپر
    drawRoundRect(
        Brush.verticalGradient(listOf(Surface3, Bg)), Offset(w * 0.02f, h * 0.76f),
        Size(w * 0.96f, h * 0.13f), CornerRadius(w * 0.03f)
    )
    drawRoundRect(Silver.copy(alpha = 0.3f), Offset(w * 0.02f, h * 0.76f), Size(w * 0.96f, h * 0.13f), CornerRadius(w * 0.03f), style = Stroke(sw))
    drawRoundRect(Color.Black, Offset(w * 0.30f, h * 0.805f), Size(w * 0.40f, h * 0.045f), CornerRadius(w * 0.02f))
}
