package com.example.aireader.model

/**
 * 文檔的抽象基類，代表可讀取的內容源
 */
abstract class Document {
    /**
     * 文檔的標題
     */
    abstract val title: String

    /**
     * 獲取文檔的純文本內容
     * @return 文檔的文本內容
     */
    abstract suspend fun getTextContent(): String

    /**
     * 文檔的類型
     */
    abstract val type: DocumentType
}

/**
 * 文檔類型枚舉
 */
enum class DocumentType {
    WEB
} 