package com.devai.studio.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.devai.studio.R
import com.devai.studio.data.Project
import com.devai.studio.databinding.ItemProjectBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProjectAdapter(
    private val onOpen: (Project) -> Unit,
    private val onExport: (Project) -> Unit,
    private val onDelete: (Project) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.VH>() {

    private val items = mutableListOf<Project>()
    private val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    fun submit(list: List<Project>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        val ctx = holder.binding.root.context
        holder.binding.projectName.text = p.name
        holder.binding.projectMeta.text = ctx.getString(
            R.string.project_meta,
            typeLabel(p.type),
            fmt.format(Date(p.updatedAt))
        )
        holder.binding.btnOpen.setOnClickListener { onOpen(p) }
        holder.binding.btnExport.setOnClickListener { onExport(p) }
        holder.binding.btnDelete.setOnClickListener { onDelete(p) }
        holder.binding.root.setOnClickListener { onOpen(p) }
    }

    private fun typeLabel(type: String): String = when (type) {
        "android" -> "Android"
        "web" -> "Web"
        "python" -> "Python"
        "kotlin" -> "Kotlin"
        else -> type
    }

    class VH(val binding: ItemProjectBinding) : RecyclerView.ViewHolder(binding.root)
}