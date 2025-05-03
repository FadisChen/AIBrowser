package com.example.aireader.model

/**
 * Gemini 搜尋回應數據模型
 * @param content 搜尋結果內容
 * @param references 參考連結列表
 */
data class GeminiSearchResponse(
    val content: String,
    val references: List<Reference>
) 