package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppStrings
import com.example.data.model.UserRole
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.PrimaryNavyDark
import com.example.ui.theme.Slate900

@Composable
fun RoleHeaderBar(
    currentRole: UserRole,
    activeUserId: String, // "ADMIN", "STAFF_1", "STAFF_2"
    adminName: String,
    staff1Name: String,
    staff2Name: String,
    isBengali: Boolean = false,
    isInitialLoading: Boolean = false,
    isCloudConnected: Boolean = false,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onSelectUser: (String) -> Unit, // "ADMIN", "STAFF_1", "STAFF_2"
    onLockAdmin: () -> Unit = {},
    onLogout: () -> Unit = {},
    pageTitle: String = "Dashboard",
    pageTitleColor: Color? = null,
    onOpenDrawer: (() -> Unit)? = null,
    unreadNotificationCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isAdmin = currentRole == UserRole.ADMIN && activeUserId == "ADMIN"
    val activeUserName = when (activeUserId) {
        "ADMIN" -> adminName.ifBlank { "Jasim" }
        "STAFF_1" -> staff1Name.ifBlank { "Sumon" }
        "STAFF_2" -> staff2Name.ifBlank { "Deedat" }
        else -> if (currentRole == UserRole.ADMIN) adminName.ifBlank { "Jasim" } else staff1Name.ifBlank { "Sumon" }
    }

    var showUserMenu by remember { mutableStateOf(false) }

    // Dynamic accent theme color matching currently visible screen
    val dynamicTitleColor = pageTitleColor ?: when (pageTitle.trim().lowercase()) {
        "dashboard" -> Color(0xFF38BDF8) // Vibrant sky blue accent for Dashboard
        "stock" -> Color(0xFF34D399)     // Vibrant emerald green for Stock
        "sales" -> Color(0xFFA78BFA)     // Vibrant violet/purple for Sales
        "reports" -> Color(0xFFFB7185)   // Vibrant rose/coral for Reports
        "settings", "profile" -> Color(0xFF818CF8) // Vibrant indigo for Settings / Profile
        else -> Color.White
    }

    // Modern, elegant gradient / deep slate color palette
    val headerGradient = if (isDarkTheme) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF0F172A),
                Color(0xFF1E293B),
                Color(0xFF0F172A)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF0F172A),
                Color(0xFF1E3A8A),
                Color(0xFF1E293B)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(headerGradient)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                // 1. Left Section: 3-line hamburger menu icon + dynamically display the active Page Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    if (onOpenDrawer != null) {
                        Surface(
                            onClick = onOpenDrawer,
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.18f),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("top_bar_hamburger_menu_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Navigation Menu",
                                    tint = dynamicTitleColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Text(
                        text = pageTitle,
                        color = dynamicTitleColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("top_bar_page_title")
                    )
                }

                // 2. Right Section: Icon-only compact user profile button placed immediately to the LEFT of the theme toggle button
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Compact Icon-Only User Switcher Button
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = if (isAdmin) {
                                Color(0xFFF59E0B)
                            } else {
                                if (isDarkTheme) Color(0xFF334155) else Color.White.copy(alpha = 0.22f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isAdmin) Color(0xFFD97706) else if (isDarkTheme) Color(0xFF64748B) else Color.White.copy(alpha = 0.40f)
                            ),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { showUserMenu = true }
                                .testTag("top_bar_user_header")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = if (isAdmin) Icons.Default.VerifiedUser else Icons.Default.Person,
                                    contentDescription = "Profile: $activeUserName",
                                    tint = if (isAdmin) Color(0xFF0F172A) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // User Switching / Locking Dropdown Menu
                        DropdownMenu(
                            expanded = showUserMenu,
                            onDismissRequest = { showUserMenu = false }
                        ) {
                            if (isAdmin) {
                                DropdownMenuItem(
                                    text = { Text("Lock Admin Session 🔒") },
                                    onClick = {
                                        showUserMenu = false
                                        onLockAdmin()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD97706))
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Switch to ${adminName.ifBlank { "Jasim" }} (Admin)") },
                                    onClick = {
                                        showUserMenu = false
                                        onSelectUser("ADMIN")
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFFD97706))
                                    }
                                )
                            }

                            if (activeUserId != "STAFF_1") {
                                DropdownMenuItem(
                                    text = { Text("Switch to ${staff1Name.ifBlank { "Sumon" }}") },
                                    onClick = {
                                        showUserMenu = false
                                        onSelectUser("STAFF_1")
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0284C7))
                                    }
                                )
                            }

                            if (activeUserId != "STAFF_2") {
                                DropdownMenuItem(
                                    text = { Text("Switch to ${staff2Name.ifBlank { "Deedat" }}") },
                                    onClick = {
                                        showUserMenu = false
                                        onSelectUser("STAFF_2")
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF7C3AED))
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Logout") },
                                onClick = {
                                    showUserMenu = false
                                    onLogout()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFEF4444))
                                }
                            )
                        }
                    }

                    // Light / Dark Mode Toggle button
                    Surface(
                        shape = CircleShape,
                        color = if (isDarkTheme) Color(0xFF334155) else Color.White.copy(alpha = 0.22f),
                        border = BorderStroke(
                            1.dp,
                            if (isDarkTheme) Color(0xFF64748B) else Color.White.copy(alpha = 0.40f)
                        ),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { onToggleTheme() }
                            .testTag("header_theme_toggle_btn")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkTheme) "Switch to Light Theme" else "Switch to Dark Theme",
                                tint = if (isDarkTheme) Color(0xFFFDE047) else Color(0xFFBAE6FD),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Notification Bell Icon Button with Count Badge (right next to theme toggle)
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkTheme) Color(0xFF334155) else Color.White.copy(alpha = 0.22f),
                            border = BorderStroke(
                                1.dp,
                                if (isDarkTheme) Color(0xFF64748B) else Color.White.copy(alpha = 0.40f)
                            ),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { onOpenNotifications() }
                                .testTag("header_notification_bell_btn")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = if (unreadNotificationCount > 0) Color(0xFF38BDF8) else (if (isDarkTheme) Color(0xFFE2E8F0) else Color.White),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        if (unreadNotificationCount > 0) {
                            val badgeText = AppStrings.formatBadgeCount(unreadNotificationCount, isBengali)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEF4444),
                                border = BorderStroke(1.5.dp, if (isDarkTheme) Color(0xFF0F172A) else Color(0xFF1E3A8A)),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-5).dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                                        .padding(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Subtle Loading / Cloud Sync Status Strip
            AnimatedVisibility(
                visible = isInitialLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .testTag("cloud_sync_loading_bar"),
                        color = Color(0xFF38BDF8),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Syncing live cloud data...",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
