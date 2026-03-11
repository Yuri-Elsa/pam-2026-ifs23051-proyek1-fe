package org.delcom.watchlist.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AuthTokenPref(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("watchlist_auth_prefs", Context.MODE_PRIVATE)

    private val AUTH_TOKEN_KEY    = "AUTH_TOKEN_KEY"
    private val REFRESH_TOKEN_KEY = "REFRESH_TOKEN_KEY"

    fun saveAuthToken(token: String)    { sharedPreferences.edit { putString(AUTH_TOKEN_KEY, token) } }
    fun getAuthToken(): String?         = sharedPreferences.getString(AUTH_TOKEN_KEY, null)
    fun clearAuthToken()                { sharedPreferences.edit { remove(AUTH_TOKEN_KEY) } }

    fun saveRefreshToken(token: String) { sharedPreferences.edit { putString(REFRESH_TOKEN_KEY, token) } }
    fun getRefreshToken(): String?      = sharedPreferences.getString(REFRESH_TOKEN_KEY, null)
    fun clearRefreshToken()             { sharedPreferences.edit { remove(REFRESH_TOKEN_KEY) } }
}
