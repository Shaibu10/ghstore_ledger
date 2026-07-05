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

    var canLogProducts: Boolean
        get() = prefs.getBoolean("can_log_products", true)
        set(value) = prefs.edit().putBoolean("can_log_products", value).apply()

    var canProcessPurchases: Boolean
        get() = prefs.getBoolean("can_process_purchases", true)
        set(value) = prefs.edit().putBoolean("can_process_purchases", value).apply()

    var canAddClients: Boolean
        get() = prefs.getBoolean("can_add_clients", true)
        set(value) = prefs.edit().putBoolean("can_add_clients", value).apply()

    var canManageExpenses: Boolean
        get() = prefs.getBoolean("can_manage_expenses", true)
        set(value) = prefs.edit().putBoolean("can_manage_expenses", value).apply()

    var canViewReports: Boolean
        get() = prefs.getBoolean("can_view_reports", true)
        set(value) = prefs.edit().putBoolean("can_view_reports", value).apply()

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
            // Default permission flags for legacy calls
            putBoolean("can_log_products", true)
            putBoolean("can_process_purchases", true)
            putBoolean("can_add_clients", true)
            putBoolean("can_manage_expenses", true)
            putBoolean("can_view_reports", true)
            if (rememberMe) {
                putString(KEY_SAVED_USERNAME, username)
            } else {
                remove(KEY_SAVED_USERNAME)
            }
        }.apply()
    }

    fun createSession(user: com.example.data.entity.UserEntity) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, user.id)
            putString(KEY_USERNAME, user.username)
            putString(KEY_USER_ROLE, user.role)
            putBoolean("can_log_products", user.canLogProducts)
            putBoolean("can_process_purchases", user.canProcessPurchases)
            putBoolean("can_add_clients", user.canAddClients)
            putBoolean("can_manage_expenses", user.canManageExpenses)
            putBoolean("can_view_reports", user.canViewReports)
            if (rememberMe) {
                putString(KEY_SAVED_USERNAME, user.username)
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
            putBoolean("can_log_products", false)
            putBoolean("can_process_purchases", false)
            putBoolean("can_add_clients", false)
            putBoolean("can_manage_expenses", false)
            putBoolean("can_view_reports", false)
        }.apply()
    }

    var syncMode: String
        get() = prefs.getString("sync_mode", "STANDALONE") ?: "STANDALONE"
        set(value) = prefs.edit().putString("sync_mode", value).apply()

    var clientHostIp: String
        get() = prefs.getString("client_host_ip", "192.168.1.100") ?: "192.168.1.100"
        set(value) = prefs.edit().putString("client_host_ip", value).apply()

    var clientHostPort: Int
        get() = prefs.getInt("client_host_port", 8080)
        set(value) = prefs.edit().putInt("client_host_port", value).apply()

    var serverPort: Int
        get() = prefs.getInt("server_port", 8080)
        set(value) = prefs.edit().putInt("server_port", value).apply()

    var storeName: String
        get() = prefs.getString("store_name", "GH POS & RETAILS LTD") ?: "GH POS & RETAILS LTD"
        set(value) = prefs.edit().putString("store_name", value).apply()

    var storePhone: String
        get() = prefs.getString("store_phone", "+233 (0) 244-123456") ?: "+233 (0) 244-123456"
        set(value) = prefs.edit().putString("store_phone", value).apply()

    var storeLocation: String
        get() = prefs.getString("store_location", "Accra Mall Road, Accra-Ghana") ?: "Accra Mall Road, Accra-Ghana"
        set(value) = prefs.edit().putString("store_location", value).apply()

    var storeFooter: String
        get() = prefs.getString("store_footer", "THANK YOU FOR YOUR PATRONAGE!") ?: "THANK YOU FOR YOUR PATRONAGE!"
        set(value) = prefs.edit().putString("store_footer", value).apply()

    var storeTaxId: String
        get() = prefs.getString("store_tax_id", "") ?: ""
        set(value) = prefs.edit().putString("store_tax_id", value).apply()
}
