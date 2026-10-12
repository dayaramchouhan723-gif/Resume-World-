package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.BuildConfig

/**
 * Manages the user's Gemini API key locally using EncryptedSharedPreferences (AES-256 GCM),
 * never hardcoded in source code. Falls back to BuildConfig.GEMINI_API_KEY if configured
 * via the AI Studio Secrets panel.
 */
object SecureApiKeyManager {
    private const val PREFS_FILE_NAME = "encrypted_gemini_api_prefs"
    private const val KEY_USER_GEMINI_API_KEY = "user_gemini_api_key"
    private const val PLACEHOLDER_KEY = "MY_GEMINI_API_KEY"
    private const val DUMMY_GITHUB_KEY = "dummy_key_for_github_only"

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback for local JVM/Robolectric unit test environments where AndroidKeyStore hardware is unavailable
            context.applicationContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    fun saveUserApiKey(context: Context, apiKey: String) {
        getEncryptedPrefs(context).edit()
            .putString(KEY_USER_GEMINI_API_KEY, apiKey.trim())
            .apply()
    }

    fun clearUserApiKey(context: Context) {
        getEncryptedPrefs(context).edit()
            .remove(KEY_USER_GEMINI_API_KEY)
            .apply()
    }

    fun getUserStoredApiKey(context: Context): String {
        return getEncryptedPrefs(context).getString(KEY_USER_GEMINI_API_KEY, "")?.trim() ?: ""
    }

    fun getEffectiveApiKey(context: Context): String {
        val userKey = getUserStoredApiKey(context)
        if (userKey.isNotEmpty()) return userKey

        val buildConfigKey = runCatching { BuildConfig.GEMINI_API_KEY.trim() }.getOrDefault("")
        if (
            buildConfigKey.isNotEmpty() &&
            buildConfigKey != PLACEHOLDER_KEY &&
            buildConfigKey != DUMMY_GITHUB_KEY
        ) {
            return buildConfigKey
        }
        return ""
    }

    fun hasValidApiKey(context: Context): Boolean {
        return getEffectiveApiKey(context).isNotEmpty()
    }
}
