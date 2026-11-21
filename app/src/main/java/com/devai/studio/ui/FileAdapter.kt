package com.devai.studio.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.devai.studio.databinding.ItemFileBinding

class FileAdapter(
    private val onOpen: (String) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<FileAdapter.VH>() {

    private val items = mutableListOf<String>()

    fun submit(list: List<String>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val path = items[position]
        holder.binding.filePath.text = path
        holder.binding.root.setOnClickListener { onOpen(path) }
        holder.binding.btnFileDelete.setOnClickListener { onDelete(path) }
        holder.binding.root.setOnLongClickListener {
            onDelete(path)
            true
        }
    }

    class VH(val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root)
}