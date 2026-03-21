package org.delcom.watchlist.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Wrapper SharedPreferences untuk menyimpan token autentikasi secara lokal.
 *
 * Disimpan di file `watchlist_auth_prefs` dengan mode privat.
 */
class AuthTokenPref(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    // ── Auth Token ────────────────────────────────────────────────────────────

    fun saveAuthToken(token: String) = prefs.edit { putString(KEY_AUTH_TOKEN, token) }
    fun getAuthToken(): String?      = prefs.getString(KEY_AUTH_TOKEN, null)
    fun clearAuthToken()             = prefs.edit { remove(KEY_AUTH_TOKEN) }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    fun saveRefreshToken(token: String) = prefs.edit { putString(KEY_REFRESH_TOKEN, token) }
    fun getRefreshToken(): String?      = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun clearRefreshToken()             = prefs.edit { remove(KEY_REFRESH_TOKEN) }

    // ── Clear All ─────────────────────────────────────────────────────────────

    /** Menghapus semua token sekaligus (logout). */
    fun clearAll() = prefs.edit { clear() }

    companion object {
        private const val PREF_FILE        = "watchlist_auth_prefs"
        private const val KEY_AUTH_TOKEN   = "AUTH_TOKEN_KEY"
        private const val KEY_REFRESH_TOKEN = "REFRESH_TOKEN_KEY"
    }
}