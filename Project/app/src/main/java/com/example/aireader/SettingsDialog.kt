package com.example.aireader

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.aireader.utils.PreferenceManager

/**
 * 設定對話框
 * 提供基本設定功能，包括API金鑰和模型名稱設定
 */
class SettingsDialog(
    context: Context,
    private val preferenceManager: PreferenceManager
) : Dialog(context) {
    
    private lateinit var apiKeyEditText: EditText
    private lateinit var modelNameEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 移除標題欄
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        // 設置對話框內容
        setContentView(R.layout.dialog_settings)
        
        // 設置對話框寬度為屏幕寬度的80%
        window?.let {
            val width = (context.resources.displayMetrics.widthPixels * 0.8).toInt()
            it.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        
        // 初始化視圖
        apiKeyEditText = findViewById(R.id.apiKeyEditText)
        modelNameEditText = findViewById(R.id.modelNameEditText)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)
        
        // 載入當前設定
        loadCurrentSettings()
        
        // 設置保存按鈕點擊事件
        saveButton.setOnClickListener {
            saveSettings()
            dismiss()
        }
        
        // 設置取消按鈕點擊事件
        cancelButton.setOnClickListener {
            dismiss()
        }
    }
    
    /**
     * 載入當前設定
     */
    private fun loadCurrentSettings() {
        // 設定API金鑰
        apiKeyEditText.setText(preferenceManager.apiKey)
        
        // 設定模型名稱
        modelNameEditText.setText(preferenceManager.modelName)
    }
    
    /**
     * 保存設定
     */
    private fun saveSettings() {
        // 獲取並儲存API金鑰
        val apiKey = apiKeyEditText.text.toString().trim()
        preferenceManager.apiKey = apiKey
        
        // 獲取並儲存模型名稱
        val modelName = modelNameEditText.text.toString().trim()
        if (modelName.isNotEmpty()) {
            preferenceManager.modelName = modelName
        } else {
            preferenceManager.modelName = PreferenceManager.DEFAULT_MODEL
        }
        
        // 顯示保存成功提示
        Toast.makeText(context, "設定已保存", Toast.LENGTH_SHORT).show()
    }
} 