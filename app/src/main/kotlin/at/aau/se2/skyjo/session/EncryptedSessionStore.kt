package at.aau.se2.skyjo.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedSessionStore(context: Context) : SessionStore {

    private val appContext = context.applicationContext
    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val preferences = EncryptedSharedPreferences.create(
        appContext,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun saveToken(token: String) {
        preferences.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun getToken(): String? =
        preferences.getString(KEY_TOKEN, null)

    override fun clearToken() {
        preferences.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val PREFS_NAME = "skyjo_secure_session"
        const val KEY_TOKEN = "session_token"
    }
}
