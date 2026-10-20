package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.isAppInDarkTheme

/**
 * Modern Admin-Only Edit Button for Phone Detail Cards.
 * Features an elevated glassmorphic container with amber/gold accent border and icon badge.
 * Provides crisp responsiveness and premium styling.
 */
@Composable
fun AdminCardEditButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "admin_card_edit_btn"
) {
    val isDark = isAppInDarkTheme()

    val containerBg = if (isDark) {
        Color(0xFF1E293B).copy(alpha = 0.88f)
    } else {
        Color.White.copy(alpha = 0.94f)
    }

    val borderBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFF59E0B).copy(alpha = if (isDark) 0.85f else 0.70f),
            Color(0xFFD97706).copy(alpha = if (isDark) 0.95f else 0.85f)
        )
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerBg,
        border = BorderStroke(1.2.dp, borderBrush),
        shadowElevation = 3.dp,
        modifier = modifier
            .testTag(testTag)
            .height(34.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFF59E0B),
                                Color(0xFFD97706)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit (Admin Only)",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "Edit",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                letterSpacing = 0.3.sp
            )
        }
    }
}
