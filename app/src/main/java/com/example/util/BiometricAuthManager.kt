package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Manages device Biometric Authentication (Fingerprint & Face Unlock)
 * with graceful fallback to PIN authentication.
 *
 * Uses SharedPreferences for preference persistence to avoid modifying
 * Room Database schemas or DAOs.
 */
object BiometricAuthManager {
    private const val PREFS_NAME = "biometric_auth_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
    private const val KEY_LAST_PROFILE = "key_last_profile"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if biometric hardware is present and user has enrolled biometric credentials.
     */
    fun canAuthenticate(context: Context): Int {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK
        return biometricManager.canAuthenticate(authenticators)
    }

    fun isBiometricAvailable(context: Context): Boolean {
        return canAuthenticate(context) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isHardwareAvailable(context: Context): Boolean {
        val status = canAuthenticate(context)
        return status != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
               status != BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
    }

    /**
     * User-controlled setting: whether biometric authentication is enabled in Settings.
     * Defaults to true if biometric hardware is available on device.
     */
    fun isBiometricEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        return if (prefs.contains(KEY_BIOMETRIC_ENABLED)) {
            prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
        } else {
            // Default enabled if device supports biometrics
            isBiometricAvailable(context)
        }
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    /**
     * Last logged-in profile ID (defaults to "ADMIN").
     */
    fun getLastProfile(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_PROFILE, "ADMIN") ?: "ADMIN"
    }

    fun setLastProfile(context: Context, profileId: String) {
        getPrefs(context).edit().putString(KEY_LAST_PROFILE, profileId).apply()
    }

    @Volatile
    private var isPromptActive = false
    @Volatile
    private var lastPromptTimestamp = 0L

    fun isPromptShowing(): Boolean = isPromptActive

    fun cancelPrompt() {
        isPromptActive = false
    }

    /**
     * Launches the system BiometricPrompt dialog for fingerprint or face recognition.
     * Enforces single active instance to guarantee prompt never appears twice.
     * Clean & minimalist UI: Subtitle and description are omitted completely so only the title appears.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Admin Verification",
        subtitle: String? = null,
        description: String? = null,
        negativeButtonText: String = "Use PIN",
        onSuccess: () -> Unit,
        onFallbackToPin: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val now = System.currentTimeMillis()
        if (isPromptActive || (now - lastPromptTimestamp < 1000L)) {
            android.util.Log.w("BiometricAuthManager", "Biometric prompt is already active or recently launched. Ignoring duplicate prompt request.")
            return
        }
        isPromptActive = true
        lastPromptTimestamp = now

        val executor = ContextCompat.getMainExecutor(activity)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)

        if (!subtitle.isNullOrBlank()) {
            promptInfoBuilder.setSubtitle(subtitle)
        }
        if (!description.isNullOrBlank()) {
            promptInfoBuilder.setDescription(description)
        }

        val promptInfo = promptInfoBuilder.build()

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isPromptActive = false
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isPromptActive = false
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        // User explicitly clicked "Use PIN Instead"
                        onFallbackToPin()
                    }
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> {
                        // User dismissed or clicked outside - cancel quietly without launching duplicate/fallback dialog
                    }
                    else -> {
                        onError(errString.toString())
                    }
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Biometric was not recognized, prompt stays open for retry
            }
        })

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            isPromptActive = false
            onError(e.message ?: "Authentication failed to launch")
            onFallbackToPin()
        }
    }
}
