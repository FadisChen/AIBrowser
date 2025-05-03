package com.example.aireader.gemini

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.aireader.utils.PreferenceManager

/**
 * Gemini服務類，簡化與GeminiClient的交互
 */
class GeminiService(
    private val preferenceManager: PreferenceManager,
    private val context: Context
) {
    private val geminiClient = GeminiClient(preferenceManager)
    private val TAG = "GeminiService"
    
    /**
     * 生成內容摘要
     * @param content 原始內容
     * @return 生成的摘要
     */
    suspend fun generateSummary(content: String): String {
        return try {
            val result = geminiClient.generateSummary(content)
            
            if (result.isSuccess) {
                result.getOrNull() ?: "無法生成摘要"
            } else {
                val exception = result.exceptionOrNull()
                Log.e(TAG, "生成摘要失敗", exception)
                "生成摘要時出錯: ${exception?.message ?: "未知錯誤"}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "生成摘要時出現異常", e)
            "生成摘要時出現異常: ${e.message ?: "未知錯誤"}"
        }
    }
    
    /**
     * 進行基於文章內容的問答對話
     * @param content 文章內容
     * @param userQuestion 用戶問題
     * @param history 對話歷史
     * @return 生成的回答
     */
    suspend fun chat(
        content: String,
        userQuestion: String,
        history: List<ChatMessage> = emptyList()
    ): String {
        return try {
            val result = geminiClient.chat(content, userQuestion, history)
            
            if (result.isSuccess) {
                result.getOrNull() ?: "無法獲得回答"
            } else {
                val exception = result.exceptionOrNull()
                Log.e(TAG, "對話失敗", exception)
                "對話時出錯: ${exception?.message ?: "未知錯誤"}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "對話時出現異常", e)
            "對話時出現異常: ${e.message ?: "未知錯誤"}"
        }
    }
} 