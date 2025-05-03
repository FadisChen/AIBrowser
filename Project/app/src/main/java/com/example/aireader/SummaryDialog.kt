package com.example.aireader

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import android.widget.Toast
import com.example.aireader.databinding.DialogSummaryBinding

/**
 * 摘要顯示對話框
 * @param context 上下文
 * @param summary 摘要內容
 */
class SummaryDialog(
    context: Context,
    private val summary: String
) : Dialog(context) {
    
    private lateinit var binding: DialogSummaryBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 設置對話框樣式
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // 初始化ViewBinding
        binding = DialogSummaryBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        
        // 設置對話框尺寸
        window?.setLayout(MATCH_PARENT, WRAP_CONTENT)
        
        // 設置摘要內容
        binding.summaryContent.text = summary
        
        // 設置複製按鈕點擊事件
        binding.copyButton.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("摘要內容", summary)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "已複製摘要內容", Toast.LENGTH_SHORT).show()
        }
        
        // 設置關閉按鈕點擊事件
        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }
} 