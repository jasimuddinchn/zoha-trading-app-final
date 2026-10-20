package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate900
import com.example.ui.theme.isAppInDarkTheme

/**
 * Standardized Screen-Level Top Header Bar with Primary #455A64 Slate Blue-Gray
 * and crisp white typography and icons.
 */
@Composable
fun AppScreenTopHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    badgeText: String? = null,
    actionContent: (@Composable () -> Unit)? = null
) {
    Surface(
        color = PrimaryNavy, // #455A64 Slate Blue-Gray
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (badgeText != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (actionContent != null) {
                actionContent()
            }
        }
    }
}

/**
 * Standardized Section & Tracker Header Component across the entire app.
 * Provides high-contrast bold typography, pill/soft badge containers,
 * aligned subtitles, and vertically centered action chips.
 */
@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = PrimaryNavy,
    iconBgColor: Color = if (isAppInDarkTheme()) Color(0xFF1E293B) else Color(0xFFEFF6FF),
    badgeText: String? = null,
    badgeColor: Color = Color(0xFF2563EB),
    badgeBgColor: Color = if (isAppInDarkTheme()) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF),
    containerBgColor: Color = if (isAppInDarkTheme()) Color(0xFF2D3748) else Color(0xFFF1F5F9),
    containerBorderColor: Color = if (isAppInDarkTheme()) Color(0xFF374151) else Color(0xFFE2E8F0),
    testTag: String = "section_header",
    actionContent: (@Composable () -> Unit)? = null
) {
    val isDark = isAppInDarkTheme()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (icon != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconBgColor,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(9.dp))
            }

            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Button / Badge style pill container
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = containerBgColor,
                        border = BorderStroke(1.dp, containerBorderColor)
                    ) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    if (badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBgColor,
                            border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }

        if (actionContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.CenterEnd
            ) {
                actionContent()
            }
        }
    }
}

/**
 * Standalone Section Title with Pill Badge Container.
 */
@Composable
fun SectionBadgeTitle(
    title: String,
    modifier: Modifier = Modifier,
    countBadge: String? = null,
    badgeColor: Color = Color(0xFF2563EB),
    badgeBgColor: Color = Color(0xFFEFF6FF),
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    actionContent: (@Composable () -> Unit)? = null
) {
    val isDark = isAppInDarkTheme()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF2D3748) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF374151) else Color(0xFFE2E8F0))
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            if (countBadge != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) badgeColor.copy(alpha = 0.2f) else badgeBgColor,
                    border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = countBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF93C5FD) else badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        if (actionText != null && onActionClick != null) {
            Surface(
                onClick = onActionClick,
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent
            ) {
                Text(
                    text = actionText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        } else if (actionContent != null) {
            actionContent()
        }
    }
}
