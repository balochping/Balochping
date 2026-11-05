package com.lumen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun LivePingCard(ping: String?, isConnected: Boolean, modifier: Modifier = Modifier) {
    // استخراج عدد پینگ
    val pingNum = ping?.filter { it.isDigit() }?.toIntOrNull() ?: 0
    val isActive = isConnected && pingNum > 0

    // رنگ‌های استایل سایبرپانک
    val neonCyan = Color(0xFF00FFCC) // فیروزه‌ای لیزری
    val darkBg1 = Color(0xFF0A0C14)
    val darkBg2 = Color(0xFF141824)
    
    // شکل زاویه‌دار خفن (برش در گوشه بالا-چپ و پایین-راست)
    val hudShape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)

    val accentColor = if (isActive) neonCyan else Color(0xFF4A4A4A)
    val statusText = if (isActive) "ONLINE" else "OFFLINE"

    // چپ‌چین کردن اجباری برای حفظ ظاهر انگلیسی و کدهای بازی
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(hudShape)
                .background(Brush.linearGradient(listOf(darkBg1, darkBg2)))
                .border(1.5.dp, accentColor.copy(alpha = 0.6f), hudShape)
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // بخش چپ: اطلاعات سیستم (طراحی شبیه ترمینال)
                Column {
                    Text(
                        text = "SYS_PING //",
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // نقطه وضعیت (مربع سایبرپانکی)
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STATUS: $statusText",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace // فونت دیجیتالی
                        )
                    }
                }

                // بخش راست: عدد پینگ با استایل رادار
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (isActive) pingNum.toString() else "--",
                        color = if (isActive) Color.White else Color(0xFF4A4A4A),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace // فونت ثابت برای اعداد
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ms",
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}
