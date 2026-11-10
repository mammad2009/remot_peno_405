package com.mamad.peugeot405

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// ======================================================================
//  «Maison 405» : سیستم طراحی اختصاصی
//  گرافیت و پلاتین ، با یک رنگ اصلی یخی و یک تأکید شامپاینی
//  (نام‌های قدیمی Cyan / Violet / Gold / Mint حفظ شده‌اند؛ فقط رنگشان باوقارتر شده)
// ======================================================================
val Bg = Color(0xFF050607)
val Panel = Color(0xFF0E1013)
val Surface2 = Color(0xFF14171B)
val Surface3 = Color(0xFF1B1F25)
val Surface4 = Color(0xFF292E37)
val Silver = Color(0xFFE9EAEC)
val Steel = Color(0xFF8D929C)
val Cyan = Color(0xFF86BFE8)        // یخی (رنگ اصلی)
val Violet = Color(0xFFAFA6D8)      // پلاتین بنفش‌فام
val Gold = Color(0xFFD9B777)        // شامپاین
val Mint = Color(0xFF8CCBB4)        // مریم‌گلی
val Ok = Color(0xFF6FCF97)
val Danger = Color(0xFFE5726E)
val Hair = Color(0x1FFFFFFF)        // خط موئی کم‌رنگ
val Hair2 = Color(0x33FFFFFF)       // خط موئی پررنگ‌تر

// نسبت ارتفاع به عرض عکس شیر (peugeot_lion.png)
const val LION_RATIO = 1.0f

// پنل مات و گرافیتی با لبه‌ی موئی ؛ بالای لبه کمی روشن‌تر (مثل نور روی فلز)
fun Modifier.luxPanel(shape: Shape, accent: Color = Silver, active: Boolean = false): Modifier =
    this.clip(shape)
        .background(Brush.verticalGradient(listOf(Surface2, Panel)))
        .border(
            1.dp,
            Brush.verticalGradient(listOf(accent.copy(alpha = if (active) 0.9f else 0.36f), Hair)),
            shape
        )

// پس‌زمینه‌ی کل برنامه: ابسیدین با دو هاله‌ی بسیار نرم (یخی بالا ، شامپاینی پایین)
@Composable
fun Backdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.background(Bg).drawBehind {
            drawRect(
                Brush.radialGradient(
                    listOf(Cyan.copy(alpha = 0.085f), Color.Transparent),
                    center = Offset(size.width * 0.88f, 0f), radius = size.width * 1.05f
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Gold.copy(alpha = 0.055f), Color.Transparent),
                    center = Offset(0f, size.height), radius = size.width * 1.1f
                )
            )
            drawRect(
                Brush.verticalGradient(
                    listOf(Color(0x66000000), Color.Transparent, Color.Transparent, Color(0x99000000))
                )
            )
        },
        content = content
    )
}
