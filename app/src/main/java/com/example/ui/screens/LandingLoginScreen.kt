package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.AppSettings

/**
 * LandingLoginScreen forwards directly to LoginScreen to maintain unified architecture
 * and complete backwards compatibility.
 */
@Composable
fun LandingLoginScreen(
    settings: AppSettings,
    isCloudConnected: Boolean,
    isInitialLoading: Boolean = false,
    isPermissionDenied: Boolean = false,
    isDarkTheme: Boolean = false,
    isBiometricAvailable: Boolean = false,
    isBiometricEnabled: Boolean = false,
    onTriggerBiometric: (targetProfile: String) -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onSelectProfile: (userId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LoginScreen(
        settings = settings,
        isCloudConnected = isCloudConnected,
        isInitialLoading = isInitialLoading,
        isPermissionDenied = isPermissionDenied,
        isDarkTheme = isDarkTheme,
        isBiometricAvailable = isBiometricAvailable,
        isBiometricEnabled = isBiometricEnabled,
        onTriggerBiometric = onTriggerBiometric,
        onToggleTheme = onToggleTheme,
        onSelectProfile = onSelectProfile,
        modifier = modifier
    )
}
