package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SharedPrefsHelper(private val context: Context) {
    private val masterKey = runCatching {
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }.getOrNull()

    private val prefs: SharedPreferences = try {
        if (masterKey != null) {
            EncryptedSharedPreferences.create(
                context.applicationContext,
                "secure_sanam_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } else {
            context.applicationContext.getSharedPreferences("secure_sanam_prefs", Context.MODE_PRIVATE)
        }
    } catch (e: Exception) {
        context.applicationContext.getSharedPreferences("secure_sanam_prefs", Context.MODE_PRIVATE)
    }

    fun saveGeminiKey(key: String) {
        prefs.edit().putString("gemini_key", key.trim()).apply()
        SecureApiKeyManager.saveUserApiKey(context.applicationContext, key.trim())
    }

    fun getGeminiKey(): String {
        val stored = prefs.getString("gemini_key", "")?.trim() ?: ""
        if (stored.isNotEmpty()) return stored
        return SecureApiKeyManager.getEffectiveApiKey(context.applicationContext)
    }

    fun saveGroqKey(key: String) {
        prefs.edit().putString("groq_key", key.trim()).apply()
    }

    fun getGroqKey(): String {
        return prefs.getString("groq_key", "")?.trim() ?: ""
    }

    fun saveAdzunaAppId(id: String) {
        prefs.edit().putString("adzuna_app_id", id.trim()).apply()
    }

    fun getAdzunaAppId(): String {
        return prefs.getString("adzuna_app_id", "")?.trim() ?: ""
    }

    fun saveAdzunaKey(key: String) {
        prefs.edit().putString("adzuna_key", key.trim()).apply()
    }

    fun getAdzunaKey(): String {
        return prefs.getString("adzuna_key", "")?.trim() ?: ""
    }

    fun saveGmailToken(token: String) {
        prefs.edit().putString("gmail_token", token.trim()).apply()
    }

    fun getGmailToken(): String {
        return prefs.getString("gmail_token", "")?.trim() ?: ""
    }

    fun setFreeModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("free_mode_enabled", enabled).apply()
    }

    fun isFreeModeEnabled(): Boolean {
        return prefs.getBoolean("free_mode_enabled", true)
    }

    fun setAutoTriggerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_trigger_enabled", enabled).apply()
    }

    fun isAutoTriggerEnabled(): Boolean {
        return prefs.getBoolean("auto_trigger_enabled", true)
    }

    fun savePreferredCountry(country: String) {
        prefs.edit().putString("preferred_country", country).apply()
    }

    fun getPreferredCountry(): String {
        return prefs.getString("preferred_country", "Canada") ?: "Canada"
    }

    fun saveUserProfile(name: String, experience: String, skills: String, education: String, location: String) {
        prefs.edit()
            .putString("user_profile_name", name.trim())
            .putString("user_profile_exp", experience.trim())
            .putString("user_profile_skills", skills.trim())
            .putString("user_profile_edu", education.trim())
            .putString("user_profile_location", location.trim())
            .putBoolean("user_profile_filled", name.isNotBlank() && skills.isNotBlank())
            .apply()
    }

    fun getUserProfile(): UserProfile {
        return UserProfile(
            name = prefs.getString("user_profile_name", "") ?: "",
            experience = prefs.getString("user_profile_exp", "") ?: "",
            skills = prefs.getString("user_profile_skills", "") ?: "",
            education = prefs.getString("user_profile_edu", "") ?: "",
            location = prefs.getString("user_profile_location", "") ?: ""
        )
    }

    fun isProfileFilled(): Boolean {
        val profile = getUserProfile()
        return profile.name.isNotBlank() && profile.skills.isNotBlank()
    }
}

data class UserProfile(
    val name: String = "",
    val experience: String = "",
    val skills: String = "",
    val education: String = "",
    val location: String = ""
)
