package com.example.aireader

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aireader.adapter.ReferenceAdapter
import com.example.aireader.adapter.SearchHistoryAdapter
import com.example.aireader.databinding.ActivitySearchBinding
import com.example.aireader.model.Reference
import com.example.aireader.model.SearchHistoryItem
import com.example.aireader.service.StreamingGeminiSearchService
import com.example.aireader.utils.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 應用的搜索頁面
 */
class SearchActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySearchBinding
    private lateinit var searchEditText: EditText
    private lateinit var voiceSearchButton: ImageButton
    private lateinit var searchButton: ImageButton
    private lateinit var settingsButton: FloatingActionButton
    private lateinit var preferenceManager: PreferenceManager
    private var searchResultsShowing = false
    private val TAG = "SearchActivity"
    
    // 追蹤上次搜索結果資訊
    private var lastQuery: String = ""
    private var lastContent: String = ""
    private var lastReferences: List<Reference> = emptyList()
    private var shouldRestoreDialog = false
    
    // 搜尋歷史相關
    private lateinit var searchHistoryAdapter: SearchHistoryAdapter
    private var historyItems: MutableList<SearchHistoryItem> = mutableListOf()

    // 語音辨識結果處理
    private val speechRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.get(0)?.let { recognizedText ->
                searchEditText.setText(recognizedText)
                performSearch(recognizedText)
            }
        }
    }
    
    companion object {
        private const val PREFERENCES_NAME = "AIReaderPrefs"
        private const val API_KEY = "api_key"
        private const val MODEL_NAME = "model_name"
        private const val DEFAULT_MODEL = "gemini-2.5-flash-preview-04-17"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化 PreferenceManager
        preferenceManager = PreferenceManager(this)
        
        // 初始化 ViewBinding
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化視圖
        searchEditText = binding.searchEditText
        voiceSearchButton = binding.voiceSearchButton
        searchButton = binding.searchButton
        settingsButton = binding.settingsButton
        
        // 設置搜索輸入框監聽器
        setupSearchInputListeners()
        
        // 設置語音搜索按鈕點擊事件
        voiceSearchButton.setOnClickListener {
            startSpeechRecognition()
        }
        
        // 設置搜索按鈕點擊事件
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            }
        }
        
        // 設置設定按鈕點擊事件
        settingsButton.setOnClickListener {
            showSettingsDialog()
        }
        
        // 初始化搜尋歷史列表
        setupSearchHistoryRecyclerView()
        
        // 載入搜尋歷史
        loadSearchHistory()
    }
    
    override fun onResume() {
        super.onResume()
        
        // 如果應該恢復對話框，且有上次的搜尋結果
        if (shouldRestoreDialog && lastQuery.isNotEmpty() && lastContent.isNotEmpty()) {
            showSearchResultDialog(lastContent, lastReferences, lastQuery)
            shouldRestoreDialog = false
        } else {
            // 如果不需要恢復對話框，則重置搜索結果顯示狀態
            searchResultsShowing = false
            searchResultDialog = null
        }
    }
    
    private fun setupSearchInputListeners() {
        // 處理Enter鍵事件
        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        
        // 處理文本變化
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val isUrlPattern = s.toString().trim().startsWith("http://") || s.toString().trim().startsWith("https://")
                searchButton.setImageResource(if (isUrlPattern) R.drawable.ic_globe else R.drawable.ic_search)
                
                // 當有文字輸入時隱藏歷史記錄
                val text = s?.toString() ?: ""
                if (text.isNotEmpty()) {
                    updateSearchHistoryVisibility(false)
                } else if (searchEditText.hasFocus()) {
                    // 當文字為空且輸入框有焦點時顯示歷史記錄
                    updateSearchHistoryVisibility(true)
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // 當搜尋框獲得焦點且為空時顯示歷史記錄
        searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                updateSearchHistoryVisibility(searchEditText.text.isEmpty())
            } else {
                updateSearchHistoryVisibility(false)
            }
        }
    }
    
    private fun setupSearchHistoryRecyclerView() {
        // 初始化搜尋歷史適配器
        searchHistoryAdapter = SearchHistoryAdapter(
            this,
            historyItems,
            onItemClick = { historyItem ->
                // 點擊歷史項目時顯示相應結果
                showSearchResultFromHistory(historyItem)
            },
            onDeleteClick = { historyItem ->
                // 刪除歷史項目
                SearchHistoryItem.deleteSearchHistoryItem(this, historyItem.id)
                // 重新載入歷史列表
                loadSearchHistory()
            }
        )
        
        // 設置RecyclerView
        binding.searchHistoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = searchHistoryAdapter
        }
    }
    
    private fun loadSearchHistory() {
        // 從SharedPreferences加載搜尋歷史
        val history = SearchHistoryItem.getAllSearchHistory(this)
        historyItems.clear()
        historyItems.addAll(history)
        searchHistoryAdapter.notifyDataSetChanged()
        
        // 根據當前輸入框狀態決定是否顯示歷史記錄
        val shouldShowHistory = searchEditText.hasFocus() && searchEditText.text.isEmpty()
        updateSearchHistoryVisibility(shouldShowHistory)
    }
    
    private fun updateSearchHistoryVisibility(show: Boolean) {
        // 只有當有歷史記錄時才顯示
        binding.historyCardView.visibility = if (show && historyItems.isNotEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun showSearchResultFromHistory(historyItem: SearchHistoryItem) {
        // 保存當前結果信息
        lastQuery = historyItem.query
        lastContent = historyItem.content
        lastReferences = historyItem.references
        
        // 將查詢文字填入搜尋框
        searchEditText.setText(historyItem.query)
        
        // 顯示結果對話框
        showSearchResultDialog(historyItem.content, historyItem.references, historyItem.query)
        
        // 隱藏歷史記錄卡片
        updateSearchHistoryVisibility(false)
    }
    
    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "請說出您要搜索的內容")
        }
        
        try {
            speechRecognitionLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "您的設備不支援語音辨識", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun performSearch(query: String) {
        // 檢查是否為URL
        if (query.startsWith("http://") || query.startsWith("https://")) {
            // 直接打開網頁
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("url", query)
            }
            startActivity(intent)
            return
        }
        
        // 檢查API金鑰設置
        val apiKey = preferenceManager.apiKey
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "請先設置API金鑰", Toast.LENGTH_LONG).show()
            showSettingsDialog()
            return
        }
        
        // 確保重置搜索結果狀態
        searchResultsShowing = false
        searchResultDialog?.dismiss()
        searchResultDialog = null
        
        val loadingDialog = LoadingDialog(this, "正在搜索...")
        loadingDialog.show()
        
        // 使用流式搜索服務
        val streamingSearchService = StreamingGeminiSearchService(
            apiKey = preferenceManager.apiKey,
            modelName = preferenceManager.modelName
        )
        
        lifecycleScope.launch {
            try {
                streamingSearchService.streamingSearch(query)
                    .onEach { response ->
                        // 一旦有初始回應，就關閉加載對話框並顯示搜索結果對話框
                        if (!searchResultsShowing) {
                            searchResultsShowing = true
                            loadingDialog.dismiss()
                            showSearchResultDialog(response.content, response.references, query)
                        } else {
                            // 更新現有對話框的內容
                            updateSearchResultDialog(response.content, response.references)
                        }
                    }
                    .catch { e ->
                        loadingDialog.dismiss()
                        searchResultsShowing = false
                        Log.e(TAG, "搜索時發生錯誤", e)
                        Toast.makeText(this@SearchActivity, "搜索失敗: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    .collect()
            } catch (e: Exception) {
                loadingDialog.dismiss()
                searchResultsShowing = false
                Log.e(TAG, "執行搜索時出錯", e)
                Toast.makeText(this@SearchActivity, "搜索失敗: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private var searchResultDialog: SearchResultDialog? = null
    
    private fun showSearchResultDialog(content: String, references: List<Reference>, query: String) {
        // 保存搜尋結果的資料
        lastQuery = query
        lastContent = content
        lastReferences = references
        
        searchResultDialog = SearchResultDialog(
            context = this,
            content = content,
            references = references,
            query = query,
            onDismiss = { 
                searchResultsShowing = false 
                searchResultDialog = null
            },
            onBrowseReference = { url ->
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("url", url)
                    putExtra("from_search_result", true)
                }
                startActivity(intent)
                shouldRestoreDialog = true
            }
        )
        
        searchResultDialog?.show()
        
        // 將搜尋結果儲存到歷史記錄中
        val historyItem = SearchHistoryItem(
            query = query,
            content = content,
            references = references
        )
        
        // 保存到SearchHistoryItem中
        SearchHistoryItem.saveSearchHistoryItem(this, historyItem)
        
        // 重新載入搜尋歷史
        loadSearchHistory()
    }
    
    private fun updateSearchResultDialog(content: String, references: List<Reference>) {
        searchResultDialog?.updateContent(content, references)
        
        // 更新最後的搜尋結果資料
        lastContent = content
        lastReferences = references
        
        // 更新歷史記錄中的項目
        val updatedHistoryItem = SearchHistoryItem(
            query = lastQuery,
            content = content,
            references = references
        )
        
        // 保存更新後的搜尋歷史
        SearchHistoryItem.saveSearchHistoryItem(this, updatedHistoryItem)
        
        // 重新載入搜尋歷史
        loadSearchHistory()
    }
    
    private fun showSettingsDialog() {
        val settingsDialog = SettingsDialog(this, preferenceManager)
        settingsDialog.show()
    }
}