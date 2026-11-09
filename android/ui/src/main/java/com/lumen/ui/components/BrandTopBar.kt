package com.lumen.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.ui.screens.LocalStrings
import com.lumen.ui.theme.expressivePalette

@Composable
fun BrandTopBar(
    onAddClick: (() -> Unit)? = null, modifier: Modifier = Modifier,
    title: String = "Lumen", horizontalPadding: Dp = 16.dp,
    actions: (@Composable () -> Unit)? = null
) {
    val art = expressivePalette()
    // رنگ‌های سایبرپانکی هماهنگ با تم برنامه
    val NeonGreen = Color(0xFF00FF66)
    val CyberPurple = Color(0xFFB026FF)
    val CardDark = Color(0xFF15151E)

    Row(modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = if (title == "Lumen") 44.sp else 36.sp,
                letterSpacing = if (LocalStrings.current.localeCode == "fa") 0.sp else (-1.6).sp, lineHeight = 44.sp),
            fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            
        if (actions != null) {
            actions()
        } else if (onAddClick != null) {
            
            // سیستم تشخیص لمس دکمه برای انیمیشن‌ها
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            
            // انیمیشن چرخش (۱۸۰ درجه موقع لمس)
            val rotation by animateFloatAsState(
                targetValue = if (isPressed) 180f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "rotation"
            )
            
            // انیمیشن تغییر شدت نور (روشن شدن هاله بنفش موقع لمس)
            val glowAlpha by animateFloatAsState(
                targetValue = if (isPressed) 0.85f else 0.25f,
                animationSpec = tween(durationMillis = 150),
                label = "glow"
            )
            
            // انیمیشن کوچک شدن دکمه (حالت فشردن فیزیکی)
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.88f else 1f,
                animationSpec = tween(durationMillis = 100),
                label = "scale"
            )

            Box(
                Modifier
                    .expressiveButtonSemantics(LocalStrings.current.add, onAddClick)
                    .size(50.dp)
                    // اعمال چرخش و تغییر سایز
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    }
                    // رسم هاله نور ملایم زیر دکمه
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            val paint = androidx.compose.ui.graphics.Paint().apply {
                                color = CyberPurple.copy(alpha = glowAlpha)
                                asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(35f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                            }
                            canvas.drawCircle(center, size.width / 2, paint)
                        }
                    }
                    .clip(CircleShape)
                    .background(CardDark)
                    .border(1.5.dp, Brush.sweepGradient(listOf(NeonGreen, CyberPurple, NeonGreen)), CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null, // حذف افکت کلیک پیش‌فرض اندروید برای دیدن بهتر نور
                        role = Role.Button, 
                        onClick = onAddClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                // کشیدن علامت + با ضخامت بیشتر به جای آیکون نازک قبلی
                androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
                    val strokeW = 3.5.dp.toPx() // ضخامت خطوط
                    
                    // خط افقی
                    drawLine(
                        color = NeonGreen,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    // خط عمودی
                    drawLine(
                        color = NeonGreen,
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }
        } else {
            ExpressiveMotif(Motif.Sun, art.stage, Modifier.size(34.dp))
        }
    }
}
