package com.example.aireader.model

import android.content.Context
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.coroutines.resume

/**
 * 網頁文檔類，代表從URL加載的網頁內容
 * @property url 網頁URL
 * @property context 應用上下文
 */
class WebDocument(val url: String, private val context: Context) : Document() {
    
    private var cachedContent: String? = null
    private var cachedTitle: String? = null
    private var webView: WebView? = null
    
    override val title: String
        get() = cachedTitle ?: url.substringAfterLast('/').takeIf { it.isNotEmpty() } ?: url
    
    override val type: DocumentType = DocumentType.WEB
    
    /**
     * 從URL獲取網頁內容
     * 僅在請求時提取內容，不會預先加載
     * @return 網頁的純文本內容
     */
    override suspend fun getTextContent(): String = withContext(Dispatchers.IO) {
        // 如果已有緩存內容，則直接返回
        if (cachedContent != null) {
            return@withContext cachedContent!!
        }
        
        // 由於直接從webView參數獲取，這裡僅返回缺省值
        return@withContext "網頁內容尚未提取，請先調用extractContentFromWebView方法"
    }
    
    /**
     * 從當前WebView提取內容，為了處理動態加載的網頁內容
     * 這是提取內容的主要方法，在使用摘要或問答功能時調用
     * @param webView 當前加載的WebView
     */
    suspend fun extractContentFromWebView(webView: WebView): String {
        this.webView = webView
        
        // 如果已有緩存內容，則直接返回
        if (cachedContent != null) {
            return cachedContent!!
        }
        
        return suspendCancellableCoroutine { continuation ->
            try {
                // 使用JavaScript提取頁面上的文本內容
                webView.evaluateJavascript("""
                    (function() {
                        // 提取頁面標題
                        var pageTitle = document.title;
                        
                        // 提取頁面所有文本 (包括動態加載的部分)
                        var bodyText = '';
                        var elements = document.querySelectorAll('p, h1, h2, h3, h4, h5, h6, span, div, article, section');
                        for (var i = 0; i < elements.length; i++) {
                            var element = elements[i];
                            if (element.innerText && element.innerText.trim() !== '' && isVisible(element)) {
                                bodyText += element.innerText + ' ';
                            }
                        }
                        
                        // 檢查元素是否可見
                        function isVisible(element) {
                            var style = window.getComputedStyle(element);
                            return style.display !== 'none' && 
                                   style.visibility !== 'hidden' && 
                                   element.offsetWidth > 0 && 
                                   element.offsetHeight > 0;
                        }
                        
                        return JSON.stringify({
                            title: pageTitle,
                            content: bodyText.trim()
                        });
                    })();
                """.trimIndent()) { result ->
                    try {
                        // 解析JavaScript返回的JSON
                        val jsonString = result.trim('"')
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                        
                        // 解析為JSON對象
                        val titlePattern = "\"title\":\"(.*?)\"".toRegex()
                        val contentPattern = "\"content\":\"(.*?)\"".toRegex()
                        
                        // 提取標題
                        val titleMatch = titlePattern.find(jsonString)
                        if (titleMatch != null) {
                            cachedTitle = titleMatch.groupValues[1]
                        }
                        
                        // 提取內容
                        val contentMatch = contentPattern.find(jsonString)
                        val content = if (contentMatch != null) {
                            contentMatch.groupValues[1]
                        } else {
                            "無法提取網頁內容"
                        }
                        
                        // 緩存內容
                        cachedContent = content
                        
                        // 恢復協程
                        continuation.resume(content)
                    } catch (e: Exception) {
                        val errorMsg = "解析頁面內容時出錯: ${e.message}"
                        cachedContent = errorMsg
                        continuation.resume(errorMsg)
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "提取網頁內容時出錯: ${e.message}"
                continuation.resume(errorMsg)
            }
        }
    }
    
    /**
     * 清除緩存的內容，強制重新獲取
     */
    fun clearCache() {
        cachedContent = null
    }
    
    companion object {
        /**
         * 檢查URL是否有效
         * @param url 要檢查的URL
         * @return 是否為有效URL
         */
        fun isValidUrl(url: String): Boolean {
            return try {
                URL(url)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
} 