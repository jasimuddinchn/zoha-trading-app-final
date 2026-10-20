package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val LocalDarkTheme = compositionLocalOf { false }
val LocalDynamicTheme = compositionLocalOf { true }

@Composable
fun isAppInDarkTheme(): Boolean = LocalDarkTheme.current

@Composable
fun isDynamicColorActive(): Boolean = LocalDynamicTheme.current

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryNavy,           // #455A64 Slate Blue-Gray
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFF1F5F9),
    secondary = GoldAccent,          // #FFC107 Gold Highlights
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = ProfitGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = Color(0xFF121824),  // Dark background #121824
    onBackground = Color(0xFFFFFFFF), // High-contrast Primary Text #FFFFFF
    surface = Color(0xFF1E1E1E),     // Dark container #1E1E1E
    onSurface = Color(0xFFFFFFFF),   // High-contrast Primary Text #FFFFFF
    surfaceVariant = Color(0xFF2D3748), // Dark container #2D3748
    onSurfaceVariant = Color(0xFFE2E8F0), // Secondary Text #E2E8F0
    outline = Color(0xFF374151),     // Distinct #374151 borders
    outlineVariant = Color(0xFF374151),
    error = ExpenseRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryNavy,           // #455A64 Slate Blue-Gray
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryNavyDark,
    secondary = GoldAccent,          // #FFC107 Gold Highlights
    onSecondary = Color(0xFF212121),
    secondaryContainer = GoldLight,
    onSecondaryContainer = Color(0xFFB45309),
    tertiary = ProfitGreen,
    onTertiary = Color.White,
    tertiaryContainer = ProfitGreenLight,
    onTertiaryContainer = ProfitGreenDark,
    background = Color(0xFFF5F7FA),  // #F5F7FA Screen Background
    onBackground = Color(0xFF212121), // #212121 Text Typography
    surface = Color(0xFFFFFFFF),     // #FFFFFF Pure White Surface
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF455A64),
    outline = Color(0xFFB0BEC5),
    outlineVariant = Color(0xFFE0E0E0),
    error = ExpenseRed,
    onError = Color.White
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp), // 12dp for cards
    large = RoundedCornerShape(24.dp),  // 24dp for buttons
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDynamicActive = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        isDynamicActive -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = if (darkTheme) Color(0xFF1E293B).toArgb() else PrimaryNavy.toArgb()
                window.navigationBarColor = if (darkTheme) Color(0xFF1E1E1E).toArgb() else Color.White.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false // White text on navy or dark header
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalDynamicTheme provides isDynamicActive
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AppShapes,
            typography = Typography,
            content = content
        )
    }
}
