package com.mamad.peugeot405

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box

/**
 * آرم «۴۰۵ | GLX» با رنگ پلاتین ؛ یک خط باریک شامپاینی میان دو بخش
 * (برای صفحه‌ی شروع و زیر شیر صفحه‌ی کنترل). scale = اندازه‌ی نسبی (1 = کوچک)
 */
@Composable
fun GlxBadge(scale: Float = 1f, modifier: Modifier = Modifier) {
    val chrome = Brush.verticalGradient(
        listOf(Color(0xFFF4F5F7), Color(0xFF9AA0AB), Color(0xFFE6E8EC), Color(0xFF626976))
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "405",
            style = TextStyle(
                brush = chrome, fontSize = (24f * scale).sp, fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic, letterSpacing = (3f * scale).sp
            )
        )
        Spacer(Modifier.width((10f * scale).dp))
        Box(Modifier.width(1.dp).height((16f * scale).dp).background(Gold.copy(alpha = 0.75f)))
        Spacer(Modifier.width((10f * scale).dp))
        Text(
            "GLX",
            style = TextStyle(
                brush = chrome, fontSize = (13f * scale).sp, fontWeight = FontWeight.Medium,
                letterSpacing = (4f * scale).sp
            )
        )
    }
}
