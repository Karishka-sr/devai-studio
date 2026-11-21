package com.devai.studio.ui

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.devai.studio.R
import com.devai.studio.ai.AiMessage
import com.devai.studio.databinding.ItemMessageBinding

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.VH>() {

    private val items = mutableListOf<AiMessage>()

    fun submit(list: List<AiMessage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun append(message: AiMessage): Int {
        items.add(message)
        notifyItemInserted(items.size - 1)
        return items.size - 1
    }

    fun updateAt(index: Int, content: String) {
        if (index in items.indices) {
            items[index] = items[index].copy(content = content)
            notifyItemChanged(index)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = items[position]
        val tv = holder.binding.msgText
        val ctx = tv.context
        val isUser = msg.role == "user"

        tv.text = msg.content
        tv.background = ContextCompat.getDrawable(
            ctx,
            if (isUser) R.drawable.bg_bubble_user else R.drawable.bg_bubble_assistant
        )
        tv.setTextColor(
            ContextCompat.getColor(ctx, if (isUser) R.color.white else R.color.on_surface)
        )
        tv.maxWidth = (ctx.resources.displayMetrics.widthPixels * 0.82f).toInt()

        val lp = tv.layoutParams as FrameLayout.LayoutParams
        lp.gravity = if (isUser) Gravity.END else Gravity.START
        tv.layoutParams = lp
    }

    class VH(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)
}