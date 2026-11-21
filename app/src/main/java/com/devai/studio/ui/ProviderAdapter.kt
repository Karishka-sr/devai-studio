package com.devai.studio.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.devai.studio.R
import com.devai.studio.data.Category
import com.devai.studio.data.ProviderInfo
import com.devai.studio.databinding.ItemProviderBinding
import com.devai.studio.databinding.ItemProviderCategoryBinding
import com.devai.studio.databinding.ItemProviderHeaderBinding
import com.google.android.material.chip.Chip

class ProviderAdapter(private val listener: Listener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class Row {
        object Header : Row()
        data class CategoryRow(val category: Category) : Row()
        data class ProviderRow(val provider: ProviderInfo) : Row()
    }

    interface Listener {
        fun onFilter(category: Category?)
        fun onKey(p: ProviderInfo)
        fun onModel(p: ProviderInfo)
        fun onSite(p: ProviderInfo)
        fun onUse(p: ProviderInfo)
    }

    var activeId: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val rows = mutableListOf<Row>()

    fun submit(list: List<Row>) {
        rows.clear()
        rows.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = rows.size

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.Header -> TYPE_HEADER
        is Row.CategoryRow -> TYPE_CATEGORY
        is Row.ProviderRow -> TYPE_PROVIDER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(ItemProviderHeaderBinding.inflate(inflater, parent, false))
            TYPE_CATEGORY -> CategoryVH(ItemProviderCategoryBinding.inflate(inflater, parent, false))
            else -> ProviderVH(ItemProviderBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as HeaderVH).bind()
            is Row.CategoryRow -> (holder as CategoryVH).bind(row.category)
            is Row.ProviderRow -> (holder as ProviderVH).bind(row.provider)
        }
    }

    inner class HeaderVH(val b: ItemProviderHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind() {
            b.filterAll.setOnClickListener { select(b.filterAll); listener.onFilter(null) }
            b.filterFree.setOnClickListener { select(b.filterFree); listener.onFilter(Category.FREE) }
            b.filterPaid.setOnClickListener { select(b.filterPaid); listener.onFilter(Category.PAID) }
            b.filterFullyFree.setOnClickListener {
                select(b.filterFullyFree); listener.onFilter(Category.FULLY_FREE)
            }
        }

        private fun select(chip: Chip) {
            b.filterAll.isChecked = false
            b.filterFree.isChecked = false
            b.filterPaid.isChecked = false
            b.filterFullyFree.isChecked = false
            chip.isChecked = true
        }
    }

    inner class CategoryVH(val b: ItemProviderCategoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(category: Category) {
            when (category) {
                Category.FULLY_FREE -> {
                    b.catTitle.setText(R.string.category_fully_free)
                    b.catSub.setText(R.string.cat_fully_free_desc)
                }
                Category.FREE -> {
                    b.catTitle.setText(R.string.category_free)
                    b.catSub.setText(R.string.cat_free_desc)
                }
                Category.PAID -> {
                    b.catTitle.setText(R.string.category_paid)
                    b.catSub.setText(R.string.cat_paid_desc)
                }
            }
        }
    }

    inner class ProviderVH(val b: ItemProviderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(p: ProviderInfo) {
            val ctx = b.root.context
            b.providerName.text = p.name
            b.providerDesc.text = p.description

            b.providerModels.text = if (p.models.isEmpty()) "" else {
                val head = p.models.take(3).joinToString(", ")
                if (p.models.size > 3) "Модели: " + head + " +" + (p.models.size - 3) + " ещё"
                else "Модели: " + head
            }

            when (p.category) {
                Category.FULLY_FREE -> {
                    b.providerBadge.text = "Полностью бесплатный"
                    b.providerBadge.setBackgroundResource(R.drawable.bg_badge_fully_free)
                }
                Category.FREE -> {
                    b.providerBadge.text = "Бесплатный тариф"
                    b.providerBadge.setBackgroundResource(R.drawable.bg_badge_free)
                }
                Category.PAID -> {
                    b.providerBadge.text = "Платный"
                    b.providerBadge.setBackgroundResource(R.drawable.bg_badge_paid)
                }
            }
            b.providerBadge.setTextColor(ContextCompat.getColor(ctx, R.color.white))

            b.providerActive.visibility = if (p.id == activeId) View.VISIBLE else View.GONE

            b.btnKey.setOnClickListener { listener.onKey(p) }
            b.btnModel.setOnClickListener { listener.onModel(p) }
            b.btnSite.setOnClickListener { listener.onSite(p) }
            b.btnUse.setOnClickListener { listener.onUse(p) }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CATEGORY = 1
        private const val TYPE_PROVIDER = 2
    }
}
