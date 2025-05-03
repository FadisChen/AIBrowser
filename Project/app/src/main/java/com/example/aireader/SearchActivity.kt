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
import com.example.aireader.databinding.ActivitySearchBinding
import com.example.aireader.model.Reference
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
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
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
                        Log.e(TAG, "搜索時發生錯誤", e)
                        Toast.makeText(this@SearchActivity, "搜索失敗: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    .collect()
            } catch (e: Exception) {
                loadingDialog.dismiss()
                Log.e(TAG, "執行搜索時出錯", e)
                Toast.makeText(this@SearchActivity, "搜索失敗: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private var searchResultDialog: SearchResultDialog? = null
    
    private fun showSearchResultDialog(content: String, references: List<Reference>, query: String) {
        searchResultDialog = SearchResultDialog(
            context = this,
            content = content,
            references = references,
            query = query,
            onWebsiteClick = { url ->
                // 打開特定網頁
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("url", url)
                }
                startActivity(intent)
            }
        )
        searchResultDialog?.show()
    }
    
    private fun updateSearchResultDialog(content: String, references: List<Reference>) {
        searchResultDialog?.updateContent(content, references)
    }
    
    private fun showSettingsDialog() {
        val settingsDialog = SettingsDialog(this, preferenceManager)
        settingsDialog.show()
    }
}

/**
 * 搜索結果對話框
 */
class SearchResultDialog(
    context: AppCompatActivity,
    var content: String,
    var references: List<Reference>,
    private val query: String,
    private val onWebsiteClick: (String) -> Unit
) : Dialog(context) {
    
    private lateinit var contentTextView: TextView
    private lateinit var referencesRecyclerView: RecyclerView
    private lateinit var closeButton: ImageButton
    private lateinit var queryTextView: TextView
    private lateinit var referenceAdapter: ReferenceAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 移除標題欄
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        // 設置對話框內容
        setContentView(R.layout.dialog_search_result)
        
        // 設置對話框寬度為屏幕寬度的90%
        window?.let {
            val width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
            it.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        
        // 初始化視圖
        contentTextView = findViewById(R.id.contentTextView)
        referencesRecyclerView = findViewById(R.id.referencesRecyclerView)
        closeButton = findViewById(R.id.closeButton)
        queryTextView = findViewById(R.id.queryTextView)
        
        // 設置關閉按鈕
        closeButton.setOnClickListener {
            dismiss()
        }
        
        // 設置查詢文本
        queryTextView.text = "「$query」的搜索結果"
        
        // 設置內容
        contentTextView.text = content
        
        // 設置參考資料列表
        setupReferencesList()
    }
    
    private fun setupReferencesList() {
        referenceAdapter = ReferenceAdapter(references) { url ->
            dismiss()
            onWebsiteClick(url)
        }
        
        referencesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = referenceAdapter
        }
    }
    
    /**
     * 更新對話框內容
     */
    fun updateContent(newContent: String, newReferences: List<Reference>) {
        this.content = newContent
        this.references = newReferences
        
        // 在UI線程更新內容
        contentTextView.post {
            contentTextView.text = newContent
            
            // 更新參考資料列表
            referenceAdapter.updateReferences(newReferences)
        }
    }
} 