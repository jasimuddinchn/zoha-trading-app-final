package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Global Admin Security Dialog supporting dual-mode authentication:
 * Automatically initiates BiometricPrompt (Fingerprint / Face Unlock) on launch,
 * with seamless fallback to Admin PIN verification.
 */
@Composable
fun AdminSecurityConfirmDialog(
    title: String = "Admin Authentication",
    actionTitle: String = "Protected Admin Action",
    actionDescription: String = "Enter Admin PIN or use fingerprint to confirm.",
    actionIcon: ImageVector = Icons.Default.Fingerprint,
    confirmButtonText: String = "Confirm & Proceed",
    isDangerAction: Boolean = false,
    autoLaunchBiometrics: Boolean = true,
    onDismiss: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onAuthorized: () -> Unit
) {
    AdminPinDialog(
        title = title,
        actionTitle = actionTitle,
        actionDescription = actionDescription,
        actionIcon = actionIcon,
        confirmButtonText = confirmButtonText,
        isDangerAction = isDangerAction,
        autoLaunchBiometrics = autoLaunchBiometrics,
        onDismiss = onDismiss,
        onVerifyPin = onVerifyPin,
        onAuthorized = onAuthorized
    )
}

