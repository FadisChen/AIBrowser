package com.example.aireader

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aireader.adapter.ReferenceAdapter
import com.example.aireader.model.Reference
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.ext.tables.TablePlugin

/**
 * 搜尋結果對話框
 */
class SearchResultDialog(
    context: Context,
    private var content: String,
    private var references: List<Reference>,
    private val query: String,
    private val onDismiss: () -> Unit,
    private val onBrowseReference: (String) -> Unit
) : Dialog(context) {
    
    private lateinit var contentTextView: TextView
    private lateinit var queryTextView: TextView
    private lateinit var closeButton: ImageButton
    private lateinit var referenceRecyclerView: RecyclerView
    private lateinit var referenceAdapter: ReferenceAdapter
    private lateinit var markwon: Markwon
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 設置對話框屬性
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_search_result)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setCancelable(true)
        
        markwon = Markwon.builder(context)
            .usePlugin(ImagesPlugin.create())
            .usePlugin(HtmlPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .build()
        
        // 初始化視圖
        contentTextView = findViewById(R.id.contentTextView)
        queryTextView = findViewById(R.id.queryTextView)
        closeButton = findViewById(R.id.closeButton)
        referenceRecyclerView = findViewById(R.id.referencesRecyclerView)
        
        // 設置搜尋查詢文字
        queryTextView.text = query
        
        // 設置內容並使用Markdown渲染
        renderMarkdown(content)
        
        // 設置參考資料列表
        referenceAdapter = ReferenceAdapter(references) { url ->
            dismiss()
            onBrowseReference(url)
        }
        
        referenceRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = referenceAdapter
        }
        
        // 設置關閉按鈕點擊事件
        closeButton.setOnClickListener {
            dismiss()
        }
    }
    
    /**
     * 渲染Markdown文本
     */
    private fun renderMarkdown(markdownText: String) {
        markwon.setMarkdown(contentTextView, markdownText)
    }
    
    override fun dismiss() {
        super.dismiss()
        onDismiss()
    }
    
    /**
     * 更新搜尋結果內容
     */
    fun updateContent(newContent: String, newReferences: List<Reference>) {
        this.content = newContent
        this.references = newReferences
        
        if (::contentTextView.isInitialized && ::markwon.isInitialized) {
            renderMarkdown(newContent)
        }
        
        if (::referenceAdapter.isInitialized) {
            referenceAdapter.updateReferences(newReferences)
        }
    }
} 