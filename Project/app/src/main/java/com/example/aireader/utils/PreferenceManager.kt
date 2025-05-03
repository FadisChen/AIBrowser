package com.example.aireader.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * 偏好設定管理器，用於存儲和獲取應用設置
 */
class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 獲取或設置Gemini API金鑰
     */
    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()
    
    /**
     * 獲取或設置Gemini模型名稱
     */
    var modelName: String
        get() = prefs.getString(KEY_MODEL_NAME, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_MODEL_NAME, value).apply()    
    companion object {
        private const val PREFS_NAME = "AIReader_Preferences"
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_MODEL_NAME = "gemini_model_name"
        const val DEFAULT_MODEL = "gemini-2.5-flash-preview-04-17"
    }
} 