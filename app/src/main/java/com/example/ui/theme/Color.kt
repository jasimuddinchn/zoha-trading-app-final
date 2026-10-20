package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Primary Material 3 Brand Colors (#455A64 Slate Blue-Gray)
val PrimaryNavy = Color(0xFF455A64) // Primary Header & Main Button
val PrimaryNavyDark = Color(0xFF37474F)
val PrimaryBlue = Color(0xFF455A64)
val PrimaryBlueLight = Color(0xFFECEFF1)
val PrimaryDark = Color(0xFF263238)
val AccentIndigo = Color(0xFF546E7A)

// Secondary & Highlight Colors (#FFC107 Gold)
val GoldAccent = Color(0xFFFFC107)
val GoldLight = Color(0xFFFFF8E1)
val TransitAmber = Color(0xFFFF8F00)
val TransitAmberLight = Color(0xFFFFF8E1)

// Accent & Profit Colors (Material Green)
val ProfitGreen = Color(0xFF2E7D32)
val ProfitGreenLight = Color(0xFFE8F5E9)
val ProfitGreenDark = Color(0xFF1B5E20)

// Stock Colors
val StockGreen = Color(0xFF2E7D32)
val StockGreenLight = Color(0xFFE8F5E9)

// Expense & Loss Colors (Material Red)
val ExpenseRed = Color(0xFFD32F2F)
val ExpenseRedLight = Color(0xFFFFEBEE)

// Currency Badges
val RmbRed = Color(0xFFD32F2F)
val RmbRedLight = Color(0xFFFFEBEE)
val BdtGreen = Color(0xFF2E7D32)
val BdtGreenLight = Color(0xFFE8F5E9)

// Material 3 Typography & Neutral Tones (#212121 Text, #F5F7FA Background, #FFFFFF Surface)
val Slate950: Color
    @Composable
    get() = if (isAppInDarkTheme()) Color(0xFFFFFFFF) else Color(0xFF212121)

val Slate900: Color
    @Composable
    get() = if (isAppInDarkTheme()) Color(0xFFFFFFFF) else Color(0xFF212121) // Primary Text

val Slate800: Color
    @Composable
    get() = if (isAppInDarkTheme()) Color(0xFFF1F5F9) else Color(0xFF37474F)

val Slate700: Color
    @Composable
    get() = if (isAppInDarkTheme()) Color(0xFFE2E8F0) else Color(0xFF455A64) // Secondary Text

val Slate600: Color
    @Composable
    get() = if (isAppInDarkTheme()) Color(0xFFCBD5E1) else Color(0xFF607D8B)

val Slate500: Color
    @Composable
    get() = if (isAppInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF78909C) // Hint / Disabled Text

val Slate400: Color
    @Composable
    get() = if (isAppInDarkTheme()) Color(0xFF64748B) else Color(0xFF90A4AE)

val Slate300: Color
    @Composable
    get() = if (isAppInDarkTheme()) Color(0xFF374151) else Color(0xFFB0BEC5) // Border Subtle

val Slate200: Color
    @Composable
    get() = if (isAppInDarkTheme()) Color(0xFF374151) else Color(0xFFCFD8DC) // Dividers

val Slate100: Color
    @Composable
    get() = if (isAppInDarkTheme()) Color(0xFF1E293B) else Color(0xFFECEFF1) // Light containers

val Slate50: Color
    @Composable
    get() = if (isDynamicColorActive()) MaterialTheme.colorScheme.background else if (isAppInDarkTheme()) Color(0xFF121824) else Color(0xFFF5F7FA)  // App Background

// Surface & Card colors
val CardBg: Color
    @Composable
    get() = if (isDynamicColorActive()) MaterialTheme.colorScheme.surface else if (isAppInDarkTheme()) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)     // Dark Card #1E1E1E / Pure White Cards

val CardBorder: Color
    @Composable
    get() = if (isDynamicColorActive()) MaterialTheme.colorScheme.outlineVariant else if (isAppInDarkTheme()) Color(0xFF374151) else Color(0xFFE0E0E0) // Subtle Card Border

val CardBorderDark: Color
    @Composable
    get() = if (isAppInDarkTheme()) Color(0xFF475569) else Color(0xFFB0BEC5)

// Theme-aware helper functions
@Composable
fun appScreenBackgroundColor(): Color = if (isDynamicColorActive()) MaterialTheme.colorScheme.background else if (isAppInDarkTheme()) Color(0xFF121824) else Color(0xFFF5F7FA)

@Composable
fun appCardBackgroundColor(): Color = if (isDynamicColorActive()) MaterialTheme.colorScheme.surface else if (isAppInDarkTheme()) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

@Composable
fun appContainerBackgroundColor(): Color = if (isDynamicColorActive()) MaterialTheme.colorScheme.surfaceVariant else if (isAppInDarkTheme()) Color(0xFF2D3748) else Color(0xFFF8FAFC)

@Composable
fun appItemBorderColor(): Color = if (isDynamicColorActive()) MaterialTheme.colorScheme.outlineVariant else if (isAppInDarkTheme()) Color(0xFF374151) else Color(0xFFE2E8F0)

/**
 * Standard high-contrast OutlinedTextField colors across the entire app
 * to ensure text, labels, and borders are always 100% visible and sharp in both Light and Dark mode.
 */
@Composable
fun appTextFieldColors(): TextFieldColors {
    val isDark = isAppInDarkTheme()
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF212121),
        unfocusedTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF212121),
        disabledTextColor = if (isDark) Color(0xFFA0AEC0) else Color(0xFF78909C),
        errorTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF212121),
        focusedContainerColor = if (isDark) Color(0xFF2D3748) else Color.White,
        unfocusedContainerColor = if (isDark) Color(0xFF2D3748) else Color(0xFFFBFDFF),
        disabledContainerColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFECEFF1),
        errorContainerColor = if (isDark) Color(0xFF2D3748) else Color.White,
        focusedBorderColor = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
        unfocusedBorderColor = if (isDark) Color(0xFF4A5568) else Color(0xFFB0BEC5),
        disabledBorderColor = if (isDark) Color(0xFF374151) else Color(0xFFCFD8DC),
        errorBorderColor = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626),
        focusedLabelColor = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
        unfocusedLabelColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF455A64),
        disabledLabelColor = if (isDark) Color(0xFFA0AEC0) else Color(0xFF90A4AE),
        errorLabelColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
        focusedPlaceholderColor = if (isDark) Color(0xFFA0AEC0) else Color(0xFF90A4AE),
        unfocusedPlaceholderColor = if (isDark) Color(0xFFA0AEC0) else Color(0xFF90A4AE),
        disabledPlaceholderColor = if (isDark) Color(0xFF718096) else Color(0xFFB0BEC5),
        errorPlaceholderColor = if (isDark) Color(0xFFA0AEC0) else Color(0xFF90A4AE),
        focusedLeadingIconColor = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
        unfocusedLeadingIconColor = if (isDark) Color(0xFF94A3B8) else Slate500,
        focusedTrailingIconColor = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
        unfocusedTrailingIconColor = if (isDark) Color(0xFF94A3B8) else Slate500,
        cursorColor = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
        errorCursorColor = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
    )
}
