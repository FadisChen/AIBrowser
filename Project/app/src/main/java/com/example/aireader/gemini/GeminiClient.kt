package com.example.aireader.gemini

import android.util.Log
import com.example.aireader.utils.PreferenceManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gemini API客戶端，處理與Gemini API的交互
 * @property preferenceManager 偏好設定管理器，用於獲取API金鑰和模型設置
 */
class GeminiClient(private val preferenceManager: PreferenceManager) {
    
    /**
     * 生成內容摘要
     * @param content 原始內容
     * @return 生成的摘要
     */
    suspend fun generateSummary(content: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = preferenceManager.apiKey
            Log.d(TAG, "使用API金鑰: ${apiKey.take(5)}...${if (apiKey.length > 10) apiKey.takeLast(5) else ""}")
            
            if (apiKey.isEmpty()) {
                return@withContext Result.failure(Exception("API金鑰未設置"))
            }
            
            val modelName = preferenceManager.modelName
            Log.d(TAG, "使用模型: $modelName")
            
            // 在 0.9.0 版本中 GenerationConfig 的建構方式改變了
            val generationConfig = GenerationConfig.builder()
                .build()

            val model = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey,
                generationConfig = generationConfig
            )
            
            val prompt = """Please read the following text and provide a concise summary.

                    Here are the requirements for the summary:
                    1.  The summary must be concise.
                    2.  The summary must be no more than 300 words.
                    3.  The summary must be provided in Traditional Chinese.
        
                    [Text to Summarize]:
                    $content"""
            val response = model.generateContent(prompt)
            
            return@withContext Result.success(response.text ?: "無法生成摘要")
        } catch (e: Exception) {
            Log.e(TAG, "生成摘要時發生錯誤", e)
            return@withContext Result.failure(e)
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
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = preferenceManager.apiKey
            Log.d(TAG, "使用API金鑰: ${apiKey.take(5)}...${if (apiKey.length > 10) apiKey.takeLast(5) else ""}")
            
            if (apiKey.isEmpty()) {
                return@withContext Result.failure(Exception("API金鑰未設置"))
            }
            
            val modelName = preferenceManager.modelName
            Log.d(TAG, "使用模型: $modelName")
            
            // 在 0.9.0 版本中使用 builder 模式創建 GenerationConfig
            val generationConfig = GenerationConfig.builder()
                .build()
                
            val model = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey,
                generationConfig = generationConfig
            )
            
            // 構建系統提示
            val systemPrompt = """You are a Knowledge Assistant designed to answer questions based on the text provided.

            Here are the rules you must follow:
            1.  **Prioritize the Text:** Always attempt to answer questions *solely* using the information found within the text provided below.
            2.  **Handle Unrelated Questions with General Knowledge:** If a question *cannot* be answered using *only* the provided text, but you *do* possess relevant general knowledge about the topic:
                *   Explicitly state that the information is **not found within the provided text**.
                *   Then, provide the answer based on your general knowledge.
            3.  **Handle Unanswerable Questions:** If a question *cannot* be answered from the provided text *and* you also *do not* have relevant general knowledge on the topic, simply state that you cannot answer the question.
            4.  **Always answer in Traditional Chinese.
            [Provided Text]:
            $content"""
            
            // 創建聊天會話
            val chat = model.startChat(
                history = listOf(
                    content {
                        role = "user"
                        text(systemPrompt)
                    },
                    content {
                        role = "model"
                        text("我已了解，我將基於文章內容以繁體中文回答問題。")
                    }
                )
            )
            
            // 發送當前問題並獲取回答
            val response = chat.sendMessage(userQuestion)
            
            return@withContext Result.success(response.text ?: "無法獲得回答")
        } catch (e: Exception) {
            Log.e(TAG, "聊天時發生錯誤", e)
            return@withContext Result.failure(e)
        }
    }
    
    companion object {
        private const val TAG = "GeminiClient"
    }
}

/**
 * 聊天消息類
 * @property content 消息內容
 * @property isUser 是否為用戶消息
 */
data class ChatMessage(
    val content: String,
    val isUser: Boolean
) 