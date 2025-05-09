package com.example.aireader.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.aireader.R
import com.example.aireader.model.SearchHistoryItem

/**
 * 搜尋歷史項目的適配器
 */
class SearchHistoryAdapter(
    private val context: Context,
    private var historyItems: MutableList<SearchHistoryItem>,
    private val onItemClick: (SearchHistoryItem) -> Unit,
    private val onDeleteClick: (SearchHistoryItem) -> Unit
) : RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_search_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = historyItems.size

    /**
     * 更新歷史項目列表
     */
    fun updateItems(newItems: List<SearchHistoryItem>) {
        historyItems.clear()
        historyItems.addAll(newItems)
        notifyDataSetChanged()
    }

    /**
     * 移除指定位置的項目
     */
    fun removeItem(position: Int) {
        if (position in 0 until historyItems.size) {
            historyItems.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, historyItems.size - position)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val historyText: TextView = itemView.findViewById(R.id.historyText)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(item: SearchHistoryItem) {
            historyText.text = item.query

            // 設置項目點擊事件
            itemView.setOnClickListener {
                onItemClick(item)
            }

            // 設置刪除按鈕點擊事件
            deleteButton.setOnClickListener {
                showDeleteConfirmDialog(item)
            }
        }

        /**
         * 顯示刪除確認對話框
         */
        private fun showDeleteConfirmDialog(item: SearchHistoryItem) {
            AlertDialog.Builder(context)
                .setTitle("刪除搜尋歷史")
                .setMessage("確定要刪除「${item.query}」的搜尋記錄嗎？")
                .setPositiveButton("確定") { _, _ ->
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onDeleteClick(item)
                        removeItem(position)
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
} 