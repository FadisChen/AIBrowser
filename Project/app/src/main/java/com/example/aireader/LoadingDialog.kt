package com.example.aireader

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import com.example.aireader.databinding.DialogLoadingBinding

/**
 * 加載對話框
 * @param context 上下文
 * @param loadingText 加載提示文字
 */
class LoadingDialog(
    context: Context,
    private val loadingText: String
) : Dialog(context) {
    
    private lateinit var binding: DialogLoadingBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 設置對話框樣式
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(false)
        
        // 初始化ViewBinding
        binding = DialogLoadingBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        
        // 設置加載文字
        binding.loadingText.text = loadingText
        
        // 設置對話框尺寸和位置
        window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
} 