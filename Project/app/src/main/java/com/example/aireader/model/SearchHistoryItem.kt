package com.example.aireader.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 搜尋歷史項目模型
 * @param query 查詢文字
 * @param content 搜尋結果內容
 * @param references 搜尋結果參考連結列表
 * @param timestamp 搜尋時間戳
 * @param id 唯一識別碼
 */
data class SearchHistoryItem(
    val query: String,
    val content: String,
    val references: List<Reference>,
    val timestamp: Long = System.currentTimeMillis(),
    val id: String = System.currentTimeMillis().toString()
) {
    
    /**
     * 格式化的時間文字
     */
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    
    companion object {
        private const val PREFS_NAME = "search_history_prefs"
        private const val HISTORY_KEY = "search_history_items"
        private const val MAX_HISTORY_ITEMS = 10
        
        /**
         * 保存搜尋歷史項目
         */
        fun saveSearchHistoryItem(context: Context, item: SearchHistoryItem) {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val gson = Gson()
            
            // 獲取現有的歷史紀錄
            val historyJson = sharedPreferences.getString(HISTORY_KEY, null)
            val historyItems = if (historyJson != null) {
                val type = object : TypeToken<ArrayList<SearchHistoryItem>>() {}.type
                gson.fromJson<ArrayList<SearchHistoryItem>>(historyJson, type)
            } else {
                ArrayList()
            }
            
            // 檢查是否已存在相同的查詢
            val existingIndex = historyItems.indexOfFirst { it.query == item.query }
            if (existingIndex != -1) {
                // 如果已存在，移除舊的
                historyItems.removeAt(existingIndex)
            }
            
            // 添加到列表開頭
            historyItems.add(0, item)
            
            // 如果超過最大數量，刪除最舊的
            if (historyItems.size > MAX_HISTORY_ITEMS) {
                historyItems.removeAt(historyItems.size - 1)
            }
            
            // 保存更新後的列表
            val updatedJson = gson.toJson(historyItems)
            sharedPreferences.edit().putString(HISTORY_KEY, updatedJson).apply()
        }
        
        /**
         * 獲取所有搜尋歷史項目
         */
        fun getAllSearchHistory(context: Context): List<SearchHistoryItem> {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val historyJson = sharedPreferences.getString(HISTORY_KEY, null) ?: return emptyList()
            
            val gson = Gson()
            val type = object : TypeToken<ArrayList<SearchHistoryItem>>() {}.type
            return gson.fromJson(historyJson, type)
        }
        
        /**
         * 刪除指定的搜尋歷史項目
         */
        fun deleteSearchHistoryItem(context: Context, itemId: String) {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val gson = Gson()
            
            val historyJson = sharedPreferences.getString(HISTORY_KEY, null) ?: return
            val type = object : TypeToken<ArrayList<SearchHistoryItem>>() {}.type
            val historyItems: ArrayList<SearchHistoryItem> = gson.fromJson(historyJson, type)
            
            // 找到並移除指定ID的項目
            val updatedItems = historyItems.filter { it.id != itemId }
            
            // 保存更新後的列表
            val updatedJson = gson.toJson(updatedItems)
            sharedPreferences.edit().putString(HISTORY_KEY, updatedJson).apply()
        }
        
        /**
         * 清空所有搜尋歷史
         */
        fun clearAllSearchHistory(context: Context) {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPreferences.edit().remove(HISTORY_KEY).apply()
        }
    }
} 