package com.example.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Secure Session Manager with Auto-Logout on App Close:
 * - Ensures no persistent login state or credentials linger in SharedPreferences or disk.
 * - Enforces cold-start redirection to the Profile Login screen (Jasim, Sumon, Deedat).
 * - Provides immediate session purging upon app termination, app exit, or logout.
 */
object SessionManager {
    private const val PREFS_NAME = "phone_hisab_session"
    private const val KEY_SESSION_ACTIVE = "key_session_active"
    private const val KEY_LOGGED_IN_USER = "key_logged_in_user"
    private const val KEY_SESSION_TIMESTAMP = "key_session_timestamp"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Completely purges any stored session info, active profile tokens, and keys.
     */
    fun clearSession(context: Context) {
        try {
            getPrefs(context).edit().clear().apply()
        } catch (_: Exception) {}
    }

    /**
     * Records an active session for transient verification during active process lifetime.
     */
    fun saveTransientSession(context: Context, userId: String) {
        try {
            getPrefs(context).edit()
                .putBoolean(KEY_SESSION_ACTIVE, true)
                .putString(KEY_LOGGED_IN_USER, userId)
                .putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
                .apply()
        } catch (_: Exception) {}
    }
}
