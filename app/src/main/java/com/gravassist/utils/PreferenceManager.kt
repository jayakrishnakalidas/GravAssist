package com.gravassist.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("grav_assist_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_SELECTED_MODEL = "selected_model"
        private const val KEY_ADB_ENABLED = "adb_enabled"
        private const val KEY_REQUEST_COUNT = "request_count"
        private const val KEY_PROMPT_TOKENS = "prompt_tokens"
        private const val KEY_COMPLETION_TOKENS = "completion_tokens"
        private const val KEY_CACHED_TUNNEL_URL = "cached_tunnel_url"
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun hasApiKey(): Boolean {
        return getApiKey().isNotEmpty()
    }

    fun saveSelectedModel(model: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL, model).apply()
    }

    fun getSelectedModel(): String {
        return prefs.getString(KEY_SELECTED_MODEL, "default") ?: "default"
    }

    fun setAdbEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ADB_ENABLED, enabled).apply()
    }

    fun isAdbEnabled(): Boolean {
        return prefs.getBoolean(KEY_ADB_ENABLED, false)
    }

    fun saveCachedTunnelUrl(url: String) {
        prefs.edit().putString(KEY_CACHED_TUNNEL_URL, url).apply()
    }

    fun getCachedTunnelUrl(): String {
        return prefs.getString(KEY_CACHED_TUNNEL_URL, "") ?: ""
    }

    fun recordUsage(promptTokens: Int, completionTokens: Int) {
        val requests = getRequestCount() + 1
        val pTokens = getPromptTokens() + promptTokens
        val cTokens = getCompletionTokens() + completionTokens

        prefs.edit()
            .putInt(KEY_REQUEST_COUNT, requests)
            .putInt(KEY_PROMPT_TOKENS, pTokens)
            .putInt(KEY_COMPLETION_TOKENS, cTokens)
            .apply()
    }

    fun getRequestCount(): Int = prefs.getInt(KEY_REQUEST_COUNT, 0)
    fun getPromptTokens(): Int = prefs.getInt(KEY_PROMPT_TOKENS, 0)
    fun getCompletionTokens(): Int = prefs.getInt(KEY_COMPLETION_TOKENS, 0)

    fun resetUsage() {
        prefs.edit()
            .putInt(KEY_REQUEST_COUNT, 0)
            .putInt(KEY_PROMPT_TOKENS, 0)
            .putInt(KEY_COMPLETION_TOKENS, 0)
            .apply()
    }
}
