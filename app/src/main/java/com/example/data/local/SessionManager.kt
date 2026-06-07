package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Handles persistent offline login preference sessions, user role caches,
 * and remember-me variables securely.
 */
class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "gh_pos_session_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "logged_user_id"
        private const val KEY_USERNAME = "logged_username"
        private const val KEY_USER_ROLE = "logged_user_role"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SAVED_USERNAME = "saved_username"
    }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var loggedInUserId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var loggedInUsername: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var loggedInUserRole: String?
        get() = prefs.getString(KEY_USER_ROLE, null)
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    var rememberMe: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_ME, false)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER_ME, value).apply()

    var savedUsername: String?
        get() = prefs.getString(KEY_SAVED_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_SAVED_USERNAME, value).apply()

    fun createSession(userId: String, username: String, role: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_USER_ROLE, role)
            if (rememberMe) {
                putString(KEY_SAVED_USERNAME, username)
            } else {
                remove(KEY_SAVED_USERNAME)
            }
        }.apply()
    }

    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            putString(KEY_USER_ID, null)
            putString(KEY_USERNAME, null)
            putString(KEY_USER_ROLE, null)
        }.apply()
    }
}
