package com.devai.studio.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.devai.studio.R
import com.devai.studio.data.LocalModel
import com.devai.studio.data.LocalModelStore
import com.devai.studio.databinding.ItemLocalModelBinding

class LocalModelAdapter(
    private val ctx: Context,
    private val onDownload: (LocalModel) -> Unit,
    private val onCancel: (LocalModel) -> Unit,
    private val onUse: (LocalModel) -> Unit,
    private val onDelete: (LocalModel) -> Unit
) : RecyclerView.Adapter<LocalModelAdapter.VH>() {

    private var items: List<LocalModel> = emptyList()
    private var activeId: String = ""
    private val progress = HashMap<String, Pair<Long, Long>>()
    private val downloading = HashSet<String>()
    private val lastNotify = HashMap<String, Long>()

    fun submit(list: List<LocalModel>, active: String) {
        items = list
        activeId = active
        notifyDataSetChanged()
    }

    fun setDownloading(id: String, value: Boolean) {
        if (value) downloading.add(id) else downloading.remove(id)
        if (!value) progress.remove(id)
        notifyItemChangedSafe(id)
    }

    fun setProgress(id: String, done: Long, total: Long) {
        progress[id] = done to total
        val now = System.currentTimeMillis()
        val last = lastNotify[id] ?: 0L
        if (done >= total || now - last > 180) {
            lastNotify[id] = now
            notifyItemChangedSafe(id)
        }
    }

    private fun notifyItemChangedSafe(id: String) {
        val pos = items.indexOfFirst { it.id == id }
        if (pos >= 0) notifyItemChanged(pos)
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemLocalModelBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        val b = holder.binding
        val installed = LocalModelStore.isInstalled(ctx, m)
        val isDownloading = downloading.contains(m.id)
        val isActive = m.id == activeId && installed

        b.modelName.text = m.name
        val extras = buildList {
            if (!m.builtIn) add(ctx.getString(R.string.local_models_custom_mark))
            if (m.premium) add("PRO")
        }
        b.modelMeta.text = ctx.getString(R.string.local_models_meta, sizeText(m), m.license) +
            if (extras.isEmpty()) "" else " · " + extras.joinToString(" · ")
        b.modelDesc.text = m.description

        when {
            isDownloading -> {
                val p = progress[m.id]
                b.modelProgress.visibility = View.VISIBLE
                if (p != null && p.second > 0) {
                    b.modelProgress.isIndeterminate = false
                    val percent = ((p.first * 100) / p.second).toInt().coerceIn(0, 100)
                    b.modelProgress.progress = percent
                    b.modelStatus.text = ctx.getString(R.string.local_models_downloading, percent)
                } else {
                    b.modelProgress.isIndeterminate = true
                    b.modelStatus.text = ctx.getString(R.string.local_models_downloading, 0)
                }
                b.btnDownload.visibility = View.VISIBLE
                b.btnDownload.text = ctx.getString(R.string.local_models_cancel)
                b.btnUse.visibility = View.GONE
                b.btnDelete.visibility = View.GONE
            }
            installed -> {
                b.modelProgress.visibility = View.GONE
                b.modelStatus.text = ctx.getString(R.string.local_models_installed) +
                    if (isActive) " · " + ctx.getString(R.string.local_models_active) else ""
                b.btnDownload.visibility = View.GONE
                b.btnUse.visibility = View.VISIBLE
                b.btnUse.isEnabled = !isActive
                b.btnUse.text = ctx.getString(
                    if (isActive) R.string.local_models_active else R.string.local_models_use
                )
                b.btnDelete.visibility = View.VISIBLE
            }
            else -> {
                b.modelProgress.visibility = View.GONE
                val part = LocalModelStore.partFile(ctx, m)
                b.modelStatus.text =
                    if (part.exists() && part.length() > 0)
                        ctx.getString(R.string.local_models_partial, human(part.length()))
                    else ctx.getString(R.string.local_models_not_downloaded)
                b.btnDownload.visibility = if (m.url.isBlank()) View.GONE else View.VISIBLE
                b.btnDownload.text = ctx.getString(R.string.local_models_download)
                b.btnUse.visibility = View.GONE
                b.btnDelete.visibility = if (m.builtIn) View.GONE else View.VISIBLE
            }
        }

        b.btnDownload.setOnClickListener {
            if (downloading.contains(m.id)) onCancel(m) else onDownload(m)
        }
        b.btnUse.setOnClickListener { onUse(m) }
        b.btnDelete.setOnClickListener { onDelete(m) }
    }

    private fun sizeText(m: LocalModel): String =
        if (m.sizeBytes > 0) human(m.sizeBytes) else ctx.getString(R.string.local_models_size_unknown)

    private fun human(bytes: Long): String = when {
        bytes >= 1_000_000_000L -> String.format("%.2f ГБ", bytes / 1e9)
        bytes >= 1_000_000L -> String.format("%.0f МБ", bytes / 1e6)
        else -> String.format("%.0f КБ", bytes / 1e3)
    }

    class VH(val binding: ItemLocalModelBinding) : RecyclerView.ViewHolder(binding.root)
}
