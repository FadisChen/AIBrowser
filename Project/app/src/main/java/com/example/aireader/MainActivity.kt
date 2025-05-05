package com.example.aireader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.aireader.databinding.ActivityMainBinding
import com.example.aireader.gemini.GeminiService
import com.example.aireader.model.Document
import com.example.aireader.model.WebDocument
import com.example.aireader.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var geminiService: GeminiService
    private var currentDocument: Document? = null
    private val TAG = "MainActivity"
    private var isFabMenuOpen = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化PreferenceManager
        preferenceManager = PreferenceManager(this)

        // 初始化GeminiService
        geminiService = GeminiService(preferenceManager, this)
        
        // 初始化ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化WebView相關UI元素
        webView = binding.webView
        setupWebView()

        // 設置懸浮按鈕點擊事件
        setupFloatingActionButtons()
        
        // 設置返回按鈕點擊事件
        binding.backButton.setOnClickListener {
            finish()
        }
        
        // 處理傳入的Intent，載入相應內容
        handleIntent(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }
    
    /**
     * 處理返回按鍵邏輯
     */
    override fun onBackPressed() {
        // 如果可以返回網頁歷史
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            // 檢查是否是從搜尋結果點擊進入
            val fromSearchResult = intent.getBooleanExtra("from_search_result", false)
            
            // 如果是從搜尋結果點擊進入，則正常結束這個 Activity，返回到上一個 Activity
            // 這樣 SearchActivity 的 onResume 會被呼叫，並恢復對話框
            if (fromSearchResult) {
                finish()
            } else {
                // 如果不是從搜尋結果點擊進入，則顯示搜尋畫面
                val intent = Intent(this, SearchActivity::class.java)
                // 清除所有之前的 Activity
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
    
    /**
     * 處理傳入的Intent，根據內容類型顯示相應內容
     */
    private fun handleIntent(intent: Intent) {
        try {
            // 檢查是否從SearchActivity傳來URL
            val url = intent.getStringExtra("url")
            if (url != null && url.isNotEmpty()) {
                loadUrl(url)
                currentDocument = WebDocument(url, this)
                return
    }
    
            // 如果不是從SearchActivity傳來的，則處理舊的格式
            val documentType = intent.getStringExtra(EXTRA_DOCUMENT_TYPE) ?: return
            when (documentType) {
                "web" -> {
                    val webUrl = intent.getStringExtra(EXTRA_WEB_URL) ?: throw IllegalArgumentException("未提供網址")
                    loadUrl(webUrl)
                    currentDocument = WebDocument(webUrl, this)
                }
                else -> throw IllegalArgumentException("不支援的文件類型: $documentType")
            }
        } catch (e: Exception) {
            Log.e(TAG, "處理Intent時出錯", e)
            Toast.makeText(this, "載入失敗: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    /**
     * 設置WebView
     */
    private fun setupWebView() {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 頁面載入完成後的操作
                binding.progressBar.visibility = View.GONE
                
                // 更新當前文檔
                url?.let {
                    currentDocument = WebDocument(it, this@MainActivity)
                }
            }
            }
            
        // 配置WebView設置
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
        }
    }
    
    /**
     * 載入URL到WebView
     */
    private fun loadUrl(url: String) {
        binding.progressBar.visibility = View.VISIBLE
        
        // 確保URL有正確的格式
        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        
        webView.visibility = View.VISIBLE
        webView.loadUrl(formattedUrl)
    }
    
    /**
     * 設置浮動按鈕
     */
    private fun setupFloatingActionButtons() {
        // 主按鈕點擊事件
        binding.mainFab.setOnClickListener {
            toggleFabMenu()
        }
        
        // 摘要按鈕
        binding.summaryFab.setOnClickListener {
            closeFabMenu()
            generateSummary()
        }
        
        // 問答按鈕
        binding.chatFab.setOnClickListener {
            closeFabMenu()
            showChatDialog()
        }
    }
    
    /**
     * 展開/收合懸浮按鈕菜單
     */
    private fun toggleFabMenu() {
        if (isFabMenuOpen) {
            closeFabMenu()
        } else {
            openFabMenu()
        }
    }
    
    /**
     * 展開懸浮按鈕菜單
     */
    private fun openFabMenu() {
        isFabMenuOpen = true
        binding.summaryFab.visibility = View.VISIBLE
        binding.chatFab.visibility = View.VISIBLE
        binding.mainFab.setImageResource(R.drawable.ic_close)
    }
    
    /**
     * 收合懸浮按鈕菜單
     */
    private fun closeFabMenu() {
        isFabMenuOpen = false
        binding.summaryFab.visibility = View.GONE
        binding.chatFab.visibility = View.GONE
        binding.mainFab.setImageResource(R.drawable.ic_settings)
    }
    
    /**
     * 生成摘要
     */
    private fun generateSummary() {
        val loadingDialog = LoadingDialog(this, "正在提取並生成摘要...")
        loadingDialog.show()
        
        lifecycleScope.launch {
            try {
                // 首先提取WebView中的內容
                val webDoc = currentDocument as? WebDocument
                if (webDoc == null) {
                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        Toast.makeText(this@MainActivity, "無法獲取當前文檔", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                // 從WebView提取內容
                val content = webDoc.extractContentFromWebView(webView)
                
                if (content.isBlank()) {
                    withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                        Toast.makeText(this@MainActivity, "無法提取內容，請確保頁面已完全載入", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                // 生成摘要
                val summary = geminiService.generateSummary(content)
                withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        showSummaryDialog(summary)
                    }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "生成摘要時出錯", e)
                Toast.makeText(this@MainActivity, "生成摘要失敗: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    /**
     * 顯示摘要對話框
     */
    private fun showSummaryDialog(summary: String) {
        val summaryDialog = SummaryDialog(this, summary)
        summaryDialog.show()
    }
    
    /**
     * 顯示聊天對話框
     */
    private fun showChatDialog() {
        val loadingDialog = LoadingDialog(this, "正在提取網頁內容...")
        loadingDialog.show()
        
        lifecycleScope.launch {
            try {
                // 首先提取WebView中的內容
                val webDoc = currentDocument as? WebDocument
                if (webDoc == null) {
                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        Toast.makeText(this@MainActivity, "無法獲取當前文檔", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                // 從WebView提取內容
                val content = webDoc.extractContentFromWebView(webView)
                val title = webDoc.title
                
                if (content.isBlank()) {
                    withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                        Toast.makeText(this@MainActivity, "無法提取內容，請確保頁面已完全載入", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                withContext(Dispatchers.Main) {
                loadingDialog.dismiss()
                    val chatDialog = ChatDialog(this@MainActivity, geminiService, content, title)
                    chatDialog.show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "準備聊天對話框時出錯", e)
                    Toast.makeText(this@MainActivity, "準備聊天失敗: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    }
    
    companion object {
        const val EXTRA_DOCUMENT_TYPE = "document_type"
        const val EXTRA_WEB_URL = "web_url"
    }
}