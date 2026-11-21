package com.devai.studio.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
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
        val ctx = holder.binding.root.context
        val isUser = msg.role == "user"
        val bubble = holder.binding.bubble
        val container = holder.binding.contentContainer

        bubble.background = ContextCompat.getDrawable(
            ctx,
            if (isUser) R.drawable.bg_bubble_user else R.drawable.bg_bubble_assistant
        )
        val lp = bubble.layoutParams as FrameLayout.LayoutParams
        lp.gravity = if (isUser) Gravity.END else Gravity.START
        bubble.layoutParams = lp

        val maxWidth = (ctx.resources.displayMetrics.widthPixels * 0.86f).toInt()
        container.removeAllViews()
        if (isUser) {
            container.addView(textView(ctx, msg.content, true, maxWidth))
        } else {
            renderAssistant(ctx, container, msg.content, maxWidth)
        }
    }

    private fun renderAssistant(ctx: Context, container: LinearLayout, content: String, maxWidth: Int) {
        val segments = parse(content)
        if (segments.isEmpty()) {
            container.addView(textView(ctx, content, false, maxWidth))
            return
        }
        segments.forEach { seg ->
            if (seg.code) container.addView(codeBlock(ctx, seg.lang, seg.text, maxWidth))
            else container.addView(textView(ctx, seg.text, false, maxWidth))
        }
    }

    private fun textView(ctx: Context, text: String, isUser: Boolean, maxWidth: Int): TextView =
        TextView(ctx).apply {
            this.text = text
            setTextIsSelectable(true)
            setTextColor(
                MaterialColors.getColor(
                    this,
                    if (isUser) com.google.android.material.R.attr.colorOnPrimary
                    else com.google.android.material.R.attr.colorOnSurfaceVariant
                )
            )
            textSize = 15f
            setLineSpacing(0f, 1.1f)
            this.maxWidth = maxWidth
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

    private fun codeBlock(ctx: Context, lang: String, code: String, maxWidth: Int): View {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1E1E1E.toInt())
            setPadding(dp(ctx, 10), dp(ctx, 6), dp(ctx, 10), dp(ctx, 8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(ctx, 6)
                bottomMargin = dp(ctx, 6)
            }
        }
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val label = TextView(ctx).apply {
            text = if (lang.isBlank()) "code" else lang
            setTextColor(0xFF9CDCFE.toInt())
            textSize = 11f
            typeface = Typeface.MONOSPACE
        }
        header.addView(label, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val copy = TextView(ctx).apply {
            text = ctx.getString(R.string.chat_copy)
            setTextColor(0xFF7FD1FF.toInt())
            textSize = 11f
            setPadding(dp(ctx, 8), dp(ctx, 4), dp(ctx, 8), dp(ctx, 4))
            setOnClickListener {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("code", code))
                text = ctx.getString(R.string.chat_copied)
            }
        }
        header.addView(copy)
        box.addView(header)

        val body = TextView(ctx).apply {
            text = code
            setTextColor(0xFFE6E6E6.toInt())
            textSize = 12.5f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setHorizontallyScrolling(true)
            this.maxWidth = (maxWidth - dp(ctx, 24)).coerceAtLeast(dp(ctx, 120))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        box.addView(body)
        return box
    }

    private data class Segment(val code: Boolean, val lang: String, val text: String)

    private fun parse(content: String): List<Segment> {
        val out = mutableListOf<Segment>()
        val parts = content.split("```")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 0) {
                if (part.isNotBlank()) out.add(Segment(false, "", part.trim('\n')))
            } else {
                val nl = part.indexOf('\n')
                val lang = if (nl >= 0) part.substring(0, nl).trim() else ""
                val code = if (nl >= 0) part.substring(nl + 1) else part
                out.add(Segment(true, lang, code.trimEnd('\n')))
            }
        }
        return out
    }

    private fun dp(ctx: Context, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    class VH(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)
}
