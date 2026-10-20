package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.isAppInDarkTheme

/**
 * Standard Material 3 Elevated Card for consistent 12dp rounded corners,
 * subtle border, pure white surface, and standard 12dp internal padding.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CardBg,
    borderColor: Color = CardBorder,
    elevation: Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

/**
 * Standard Material 3 Action Button re-skinned with a modern high-contrast
 * vibrant gradient palette, 12dp rounded corners, subtle 1dp border outline,
 * ambient drop shadow / diffused glow, bold typography, and interactive press scaling.
 */
@Composable
fun AppActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = PrimaryNavy,
    contentColor: Color = Color.White,
    gradientColors: List<Color>? = null,
    enabled: Boolean = true,
    testTag: String = ""
) {
    val isDark = isAppInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        label = "btn_scale"
    )

    val resolvedColors = gradientColors ?: when {
        containerColor == PrimaryNavy -> listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
        containerColor == ExpenseRed -> listOf(Color(0xFFEF4444), Color(0xFFDC2626))
        containerColor == Color(0xFF2563EB) -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
        else -> listOf(containerColor, containerColor.copy(alpha = 0.85f))
    }

    val brush = if (enabled) {
        Brush.horizontalGradient(resolvedColors)
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1)))
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (enabled) {
                if (isDark) Color(0xFF818CF8).copy(alpha = 0.40f) else Color.White.copy(alpha = 0.35f)
            } else {
                Color.Transparent
            }
        ),
        shadowElevation = if (enabled) 3.dp else 0.dp,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) contentColor else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) contentColor else Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Standard Material 3 Outlined Action Button re-skinned with subtle elevated surface,
 * 12dp rounded corners, 1dp border outline, interactive press feedback, and bold typography.
 */
@Composable
fun AppOutlinedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    borderColor: Color = Color(0xFF6366F1),
    contentColor: Color = Color(0xFF6366F1),
    enabled: Boolean = true,
    testTag: String = ""
) {
    val isDark = isAppInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        label = "outlined_btn_scale"
    )

    val surfaceBg = if (isDark) {
        Color(0xFF1E293B).copy(alpha = if (isPressed) 0.95f else 0.75f)
    } else {
        Color(0xFF6366F1).copy(alpha = if (isPressed) 0.15f else 0.08f)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = surfaceBg,
        border = BorderStroke(
            1.dp,
            if (enabled) {
                if (isDark) Color(0xFF6366F1).copy(alpha = 0.50f) else borderColor.copy(alpha = 0.60f)
            } else {
                Color(0xFF94A3B8).copy(alpha = 0.3f)
            }
        ),
        shadowElevation = if (enabled) 1.dp else 0.dp,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) {
                            if (isDark) Color(0xFF818CF8) else contentColor
                        } else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) {
                        if (isDark) Color(0xFF818CF8) else contentColor
                    } else Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Standardized List Action IconButton with uniform touch-target (38dp),
 * 18dp icon size, 10dp rounded corners, subtle border, ambient shadow, and press feedback.
 */
@Composable
fun AppListIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF6366F1),
    backgroundColor: Color = tint.copy(alpha = 0.12f),
    size: Dp = 38.dp,
    iconSize: Dp = 18.dp,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        label = "icon_btn_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isPressed) backgroundColor.copy(alpha = 0.25f) else backgroundColor,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.25f)),
        shadowElevation = 1.dp,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Modern High-Contrast Dialog Confirm Action Button
 */
@Composable
fun AppDialogConfirmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)),
    enabled: Boolean = true,
    testTag: String = ""
) {
    val isDark = isAppInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        label = "dialog_confirm_scale"
    )
    val brush = if (enabled) {
        Brush.horizontalGradient(gradientColors)
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1)))
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0xFF818CF8).copy(alpha = 0.40f) else Color.White.copy(alpha = 0.35f)
        ),
        shadowElevation = 2.dp,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Modern Elevated Dialog Dismiss / Secondary Action Button
 */
@Composable
fun AppDialogDismissButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val isDark = isAppInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        label = "dialog_dismiss_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
        ),
        shadowElevation = 1.dp,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isDark) Color(0xFF94A3B8) else Slate700,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Standard Elevated Metric Summary Card for Dashboard and Reports screens
 * with uniform sizing, icon badge, overflow-safe labels, and primary values.
 */
@Composable
fun AppSummaryStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accentColor: Color = PrimaryNavy,
    backgroundColor: Color = CardBg,
    testTag: String = ""
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = value,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = Slate700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
