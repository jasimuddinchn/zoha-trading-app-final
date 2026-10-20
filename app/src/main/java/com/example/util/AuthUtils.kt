package com.example.util

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity

/**
 * Universal Authentication Utility for Admin & User Security.
 * Supports Biometric Authentication (Fingerprint / Face Unlock) with seamless fallback to PIN.
 */
object AuthUtils {
    private const val TAG = "AuthUtils"

    /**
     * Checks if the device has biometric hardware and enrolled credentials.
     */
    fun isBiometricSupported(context: Context): Boolean {
        return BiometricAuthManager.isBiometricAvailable(context)
    }

    /**
     * Launches BiometricPrompt API for Admin verification with clean & minimalist UI.
     * Subtitle and description are omitted completely so only the title appears.
     * If authentication succeeds, [onSuccess] is invoked immediately.
     * If user cancels or taps "Use PIN", [onFallbackToPin] is invoked.
     */
    fun authenticateAdmin(
        activity: FragmentActivity,
        actionTitle: String = "Admin Verification",
        onSuccess: () -> Unit,
        onFallbackToPin: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!isBiometricSupported(activity)) {
            onFallbackToPin()
            return
        }

        try {
            BiometricAuthManager.showBiometricPrompt(
                activity = activity,
                title = actionTitle.ifBlank { "Admin Verification" },
                subtitle = null,
                description = null,
                negativeButtonText = "Use PIN",
                onSuccess = {
                    Log.d(TAG, "Biometric authentication succeeded for $actionTitle")
                    onSuccess()
                },
                onFallbackToPin = {
                    Log.d(TAG, "Fallback to PIN requested for $actionTitle")
                    onFallbackToPin()
                },
                onError = { errorMsg ->
                    Log.w(TAG, "Biometric auth error: $errorMsg")
                    onError(errorMsg)
                    onFallbackToPin()
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Exception launching biometric prompt: ${e.message}")
            onFallbackToPin()
        }
    }

    /**
     * Validates entered Admin PIN.
     */
    fun verifyAdminPin(inputPin: String, adminPin: String): Boolean {
        val cleanInput = inputPin.trim()
        val cleanStored = adminPin.trim().ifBlank { "1111" }
        return cleanInput == cleanStored
    }
}
