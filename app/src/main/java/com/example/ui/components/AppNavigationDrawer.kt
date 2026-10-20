package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.isAppInDarkTheme

@Composable
fun AppNavigationDrawer(
    currentRole: UserRole,
    activeUserId: String,
    adminName: String,
    staff1Name: String,
    staff2Name: String,
    onNavigateToDashboard: () -> Unit,
    onNavigateToDueLedger: () -> Unit,
    onOpenWholesaleGenerator: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val isAdmin = currentRole == UserRole.ADMIN

    val activeUserName = when (activeUserId) {
        "ADMIN" -> adminName.ifBlank { "Jasim" }
        "STAFF_1" -> staff1Name.ifBlank { "Sumon" }
        "STAFF_2" -> staff2Name.ifBlank { "Deedat" }
        else -> if (isAdmin) adminName.ifBlank { "Jasim" } else staff1Name.ifBlank { "Sumon" }
    }

    val headerGradient = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0F172A),
                Color(0xFF1E293B),
                Color(0xFF0F172A)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0F172A),
                Color(0xFF1E3A8A),
                Color(0xFF1E293B)
            )
        )
    }

    ModalDrawerSheet(
        drawerContainerColor = if (isDark) Color(0xFF0F172A) else Color.White,
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // ==========================================
            // DRAWER HEADER: BRANDING & ACTIVE USER
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient)
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 20.dp)
            ) {
                Column {
                    // Logo & App Name ("Zoha Trading" without ERP)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_app_logo),
                                    contentDescription = "Zoha Trading",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Zoha Trading",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                            Text(
                                text = "Trading Worldwide Since 2012",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Active User Pill (Clean name only, no extra hint/access text)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isAdmin) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = activeUserName,
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // DRAWER NAVIGATION ITEMS (Clean titles only, no subtitles/hints/badges)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Dashboard
                CleanDrawerItem(
                    title = "Dashboard",
                    icon = Icons.Default.Dashboard,
                    accentColor = Color(0xFF2563EB),
                    testTag = "drawer_item_dashboard",
                    onClick = onNavigateToDashboard
                )

                // 2. Due Ledger
                CleanDrawerItem(
                    title = "Due Ledger",
                    icon = Icons.Default.ReceiptLong,
                    accentColor = Color(0xFFEA580C),
                    testTag = "drawer_item_due_ledger",
                    onClick = onNavigateToDueLedger
                )

                // 3. Wholesale Price List (Admin Only - Hidden for Non-Admin / Staff)
                if (isAdmin) {
                    CleanDrawerItem(
                        title = "Wholesale Price List",
                        icon = Icons.Default.PictureAsPdf,
                        accentColor = Color(0xFF059669),
                        testTag = "drawer_item_wholesale_pdf",
                        onClick = onOpenWholesaleGenerator
                    )
                }

                // 4. About & Updates
                CleanDrawerItem(
                    title = "About & Updates",
                    icon = Icons.Default.Info,
                    accentColor = Color(0xFF0284C7),
                    testTag = "drawer_item_about_updates",
                    onClick = onOpenAbout
                )

                // 5. Settings (Admin Only - Hidden for Non-Admin / Staff)
                if (isAdmin) {
                    CleanDrawerItem(
                        title = "Settings",
                        icon = Icons.Default.Settings,
                        accentColor = Color(0xFF4F46E5),
                        testTag = "drawer_item_settings",
                        onClick = onOpenSettings
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(4.dp))

                // 6. Log Out
                CleanDrawerItem(
                    title = "Log Out",
                    icon = Icons.Default.ExitToApp,
                    accentColor = Color(0xFFDC2626),
                    testTag = "drawer_item_logout",
                    onClick = onLogout
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ==========================================
            // DRAWER FOOTER: APP DETAILS
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Zoha Trading",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFF94A3B8) else Slate700
                )
                Text(
                    text = "Version 1.0.3",
                    fontSize = 10.5.sp,
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Design & Develop By ")
                        withStyle(
                            style = SpanStyle(
                                color = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C),
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("Jasim Uddin")
                        }
                    },
                    fontSize = 10.5.sp,
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
            }
        }
    }
}

/**
 * Clean Drawer Navigation Item with icon, single clean title, and right chevron.
 * Strictly without subtitles, hints, or badge tags.
 */
@Composable
private fun CleanDrawerItem(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String = "",
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.15f),
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

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isDark) Color(0xFF64748B) else Color(0xFFCBD5E1),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
