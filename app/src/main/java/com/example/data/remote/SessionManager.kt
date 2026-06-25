package com.example.data.remote

import android.content.Context

data class UserSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val name: String?,
    val avatarUrl: String?
)

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("debttrack_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "name"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_BIOMETRICS_ENABLED = "biometrics_enabled"
        private const val KEY_SMS_READ_ENABLED = "sms_read_enabled"
        private const val KEY_NOTIFICATION_LISTENER_ENABLED = "notification_listener_enabled"
    }

    fun saveOfflineSession(name: String) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, "offline_access_token")
            putString(KEY_REFRESH_TOKEN, "offline_refresh_token")
            putString(KEY_USER_ID, "offline_user_id")
            putString(KEY_EMAIL, "offline@spendtracker.local")
            putString(KEY_NAME, name)
            putString(KEY_AVATAR_URL, null)
            apply()
        }
    }

    fun getSession(): UserSession? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val displayEmail = if (email.contains("debttrack")) "offline@spendtracker.local" else email
        val name = prefs.getString(KEY_NAME, null)
        val avatarUrl = prefs.getString(KEY_AVATAR_URL, null)
        return UserSession(token, refresh, userId, displayEmail, name, avatarUrl)
    }

    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_EMAIL)
            remove(KEY_NAME)
            remove(KEY_AVATAR_URL)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = getSession() != null

    // Preferences for toggles
    var isBiometricsEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRICS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRICS_ENABLED, value).apply()

    var isSmsReadEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMS_READ_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SMS_READ_ENABLED, value).apply()

    var isNotificationListenerEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_LISTENER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_LISTENER_ENABLED, value).apply()
}
