package com.example.aireader.service

import android.util.Log
import com.example.aireader.model.GeminiSearchResponse
import com.example.aireader.model.Reference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gemini 串流搜尋服務
 * 使用流式返回搜索結果
 * @param apiKey API金鑰
 * @param modelName 模型名稱
 */
class StreamingGeminiSearchService(
    private val apiKey: String,
    private val modelName: String
) {
    private val TAG = "StreamingGeminiSearch"

    /**
     * 流式執行搜尋查詢
     * @param query 搜尋查詢
     * @return 包含部分回應的流
     */
    fun streamingSearch(query: String): Flow<GeminiSearchResponse> = flow {
        try {
            // 使用動態模型名稱構建API URL
            val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:streamGenerateContent"
            val urlWithKey = "$apiUrl?alt=sse&key=$apiKey"
            
            // 創建URL實例
            val url = URL(urlWithKey)
            
            // 創建連接
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            
            // 準備請求體
            val requestBody = """
                {
                    "contents": [
                        {
                            "parts": [
                                {"text": "$query"}
                            ]
                        }
                    ],
                    "tools": [
                        {
                            "google_search": {}
                        }
                    ]
                }
            """.trimIndent()
            
            // 寫入請求體
            val outputStream = connection.outputStream
            outputStream.write(requestBody.toByteArray())
            outputStream.flush()
            outputStream.close()
            
            // 獲取回應
            val responseCode = connection.responseCode
            Log.d(TAG, "API 回應碼: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 讀取流式回應
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                
                var line: String?
                val references = mutableListOf<Reference>()
                var contentBuilder = StringBuilder()
                
                // 逐行處理流式回應
                while (reader.readLine().also { line = it } != null) {
                    line?.let { currentLine ->
                        if (currentLine.startsWith("data: ")) {
                            val jsonData = currentLine.substring(6) // 移除 "data: " 前綴
                            
                            // 忽略結束標記行
                            if (jsonData == "[DONE]") {
                                return@let
                            }
                            
                            try {
                                // 解析JSON使用org.json庫
                                val jsonObject = JSONObject(jsonData)
                                
                                // 處理回應內容
                                processChunk(jsonObject, contentBuilder, references)
                                
                                // 發送當前部分回應
                                emit(GeminiSearchResponse(contentBuilder.toString(), references.toList()))
                            } catch (e: JSONException) {
                                Log.e(TAG, "解析JSON出錯: $e", e)
                            }
                        }
                    }
                }
                
                reader.close()
                
                // 發送最終回應
                if (contentBuilder.isEmpty()) {
                    emit(GeminiSearchResponse("無法獲取搜尋結果", emptyList()))
                }
            } else {
                // 處理錯誤
                val errorStream = connection.errorStream
                val reader = BufferedReader(InputStreamReader(errorStream))
                val errorResponse = StringBuilder()
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    errorResponse.append(line)
                }
                
                reader.close()
                
                Log.e(TAG, "API錯誤: $errorResponse")
                emit(GeminiSearchResponse("搜尋失敗: HTTP $responseCode - $errorResponse", emptyList()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "搜尋時出錯: ${e.message}", e)
            emit(GeminiSearchResponse("搜尋時出錯: ${e.message}", emptyList()))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 處理流式回應的單個數據塊
     */
    private fun processChunk(
        jsonObject: JSONObject, 
        contentBuilder: StringBuilder,
        references: MutableList<Reference>
    ) {
        try {
            // 檢查是否有候選內容
            if (jsonObject.has("candidates") && !jsonObject.getJSONArray("candidates").isNull(0)) {
                val candidate = jsonObject.getJSONArray("candidates").getJSONObject(0)
                
                // 提取基於網絡的參考資料
                if (candidate.has("groundingMetadata")) {
                    val groundingMetadata = candidate.getJSONObject("groundingMetadata")
                    
                    if (groundingMetadata.has("groundingChunks")) {
                        val groundingChunks = groundingMetadata.getJSONArray("groundingChunks")
                        
                        for (i in 0 until groundingChunks.length()) {
                            val chunk = groundingChunks.getJSONObject(i)
                            
                            if (chunk.has("web")) {
                                val web = chunk.getJSONObject("web")
                                val title = if (web.has("title")) web.getString("title") else ""
                                val uri = if (web.has("uri")) web.getString("uri") else ""
                                
                                // 檢查是否已存在相同URI的參考
                                if (references.none { it.url == uri } && uri.isNotEmpty()) {
                                    references.add(Reference(title, uri))
                                }
                            }
                        }
                    }
                }
                
                // 提取文本內容
                if (candidate.has("content") && candidate.getJSONObject("content").has("parts")) {
                    val parts = candidate.getJSONObject("content").getJSONArray("parts")
                    
                    if (!parts.isNull(0)) {
                        val part = parts.getJSONObject(0)
                        if (part.has("text")) {
                            val text = part.getString("text")
                            contentBuilder.append(text)
                        }
                    }
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, "處理JSON區塊出錯", e)
        }
    }
} 