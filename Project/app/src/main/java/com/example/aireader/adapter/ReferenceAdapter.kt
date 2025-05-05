package com.example.aireader.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aireader.R
import com.example.aireader.model.Reference

/**
 * 參考連結適配器
 */
class ReferenceAdapter(
    private var references: List<Reference>,
    private val onItemClickListener: (String) -> Unit
) : RecyclerView.Adapter<ReferenceAdapter.ReferenceViewHolder>() {

    /**
     * 參考連結的 ViewHolder
     */
    class ReferenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.referenceTitle)
        val url: TextView = itemView.findViewById(R.id.referenceUrl)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReferenceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reference, parent, false)
        return ReferenceViewHolder(view)
    }    override fun onBindViewHolder(holder: ReferenceViewHolder, position: Int) {
        val reference = references[position]
        // 設定標題，如果沒有標題則顯示網域名稱或預設文字
        val displayTitle = when {
            reference.title.isNotEmpty() -> reference.title
            reference.url.isNotEmpty() -> extractDomainFromUrl(reference.url)
            else -> "參考連結 ${position + 1}"
        }
        
        holder.title.text = displayTitle
        // 將URL設為隱藏
        holder.url.visibility = View.GONE
        
        // 點擊整個項目時觸發連結
        holder.itemView.setOnClickListener {
            onItemClickListener(reference.url)
        }
    }
    
    /**
     * 從URL中提取網域名稱
     */
    private fun extractDomainFromUrl(url: String): String {
        return try {
            val urlPattern = "https?://([^/]+).*".toRegex()
            val result = urlPattern.find(url)
            result?.groupValues?.get(1) ?: url
        } catch (e: Exception) {
            url
        }
    }

    override fun getItemCount(): Int = references.size
    
    /**
     * 更新參考連結列表
     * @param newReferences 新的參考連結列表
     */
    fun updateReferences(newReferences: List<Reference>) {
        this.references = newReferences
        notifyDataSetChanged()
    }
} 