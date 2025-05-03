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
    }

    override fun onBindViewHolder(holder: ReferenceViewHolder, position: Int) {
        val reference = references[position]
        holder.title.text = reference.title.ifEmpty { "參考連結 ${position + 1}" }
        holder.url.text = reference.url
        
        holder.itemView.setOnClickListener {
            onItemClickListener(reference.url)
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