package com.ani.map

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)

    // Save login state
    fun setLogin(isLoggedIn: Boolean, userType: String) {
        val editor = preferences.edit()
        editor.putBoolean("isLoggedIn", isLoggedIn)
        editor.putString("userType", userType)
        editor.apply()
    }

    // Get login status
    fun isLoggedIn(): Boolean {
        return preferences.getBoolean("isLoggedIn", false)
    }

    // Get user type
    fun getUserType(): String? {
        return preferences.getString("userType", null)
    }

    // Clear session (for logout)
    fun clearSession() {
        val editor = preferences.edit()
        editor.clear()
        editor.apply()
    }
}
