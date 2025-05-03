package com.example.aireader

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aireader.databinding.DialogChatBinding
import com.example.aireader.databinding.ItemChatMessageBinding
import com.example.aireader.gemini.ChatMessage
import com.example.aireader.gemini.GeminiService
import kotlinx.coroutines.launch

/**
 * 聊天對話框
 * @param context 上下文
 * @param geminiService Gemini服務
 * @param documentContent 文檔內容
 * @param documentTitle 文檔標題
 */
class ChatDialog(
    private val context: Context,
    private val geminiService: GeminiService,
    private val documentContent: String,
    private val documentTitle: String
) : Dialog(context) {
    
    private lateinit var binding: DialogChatBinding
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private var isProcessing = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 設置對話框樣式
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // 初始化ViewBinding
        binding = DialogChatBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        
        // 設置對話框尺寸
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        
        // 設置對話框標題
        binding.titleText.text = "關於：$documentTitle"
        
        // 初始化RecyclerView和Adapter
        adapter = ChatAdapter(chatMessages)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.chatRecyclerView.adapter = adapter
        
        // 設置發送按鈕點擊事件
        binding.sendButton.setOnClickListener {
            val question = binding.messageInput.text.toString().trim()
            if (question.isNotEmpty() && !isProcessing) {
                sendMessage(question)
            }
        }
        
        // 設置關閉按鈕點擊事件
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        // 添加歡迎訊息
        addBotMessage("您好！我已讀取完文章內容，請問有什麼問題？")
    }
    
    /**
     * 發送用戶訊息並獲取回應
     */
    private fun sendMessage(question: String) {
        // 添加用戶訊息
        val userMessage = ChatMessage(question, true)
        chatMessages.add(userMessage)
        adapter.notifyItemInserted(chatMessages.size - 1)
        binding.chatRecyclerView.scrollToPosition(chatMessages.size - 1)
        
        // 清空輸入框
        binding.messageInput.text.clear()
        
        // 顯示處理指示器
        isProcessing = true
        binding.progressBar.visibility = View.VISIBLE
        binding.sendButton.isEnabled = false
        
        // 使用Gemini服務獲取回應
        (context as MainActivity).lifecycleScope.launch {
            try {
                val response = geminiService.chat(documentContent, question, chatMessages.filter { it.isUser })
                // 添加機器人回應
                addBotMessage(response)
            } catch (e: Exception) {
                // 顯示異常錯誤訊息
                addBotMessage("很抱歉，發生了未知錯誤：${e.message}")
            } finally {
                // 隱藏處理指示器
                isProcessing = false
                binding.progressBar.visibility = View.GONE
                binding.sendButton.isEnabled = true
            }
        }
    }
    
    /**
     * 添加機器人訊息
     */
    private fun addBotMessage(message: String) {
        val botMessage = ChatMessage(message, false)
        chatMessages.add(botMessage)
        adapter.notifyItemInserted(chatMessages.size - 1)
        binding.chatRecyclerView.scrollToPosition(chatMessages.size - 1)
    }
    
    /**
     * 聊天訊息Adapter
     */
    private inner class ChatAdapter(
        private val messages: List<ChatMessage>
    ) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val binding = ItemChatMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ChatViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val message = messages[position]
            holder.bind(message)
        }
        
        override fun getItemCount(): Int = messages.size
        
        inner class ChatViewHolder(
            private val binding: ItemChatMessageBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            
            fun bind(message: ChatMessage) {
                if (message.isUser) {
                    // 用戶訊息
                    binding.userMessage.visibility = View.VISIBLE
                    binding.botMessage.visibility = View.GONE
                    binding.userMessageText.text = message.content
                } else {
                    // 機器人訊息
                    binding.userMessage.visibility = View.GONE
                    binding.botMessage.visibility = View.VISIBLE
                    binding.botMessageText.text = message.content
                }
            }
        }
    }
} 