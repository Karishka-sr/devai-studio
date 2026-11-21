package com.devai.studio.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.Category
import com.devai.studio.data.ProviderCatalog
import com.devai.studio.data.ProviderInfo
import com.devai.studio.databinding.FragmentProvidersBinding
import com.devai.studio.util.AndroidUtils

class ProvidersFragment : Fragment(), ProviderAdapter.Listener {

    private var _binding: FragmentProvidersBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProviderAdapter
    private var filter: Category? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProvidersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ProviderAdapter(this)
        binding.providerList.layoutManager = LinearLayoutManager(requireContext())
        binding.providerList.adapter = adapter
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun refresh() {
        val rows = mutableListOf<ProviderAdapter.Row>()
        rows.add(ProviderAdapter.Row.Header)
        val categories = if (filter == null) {
            listOf(Category.FULLY_FREE, Category.FREE, Category.PAID)
        } else {
            listOf(filter!!)
        }
        categories.forEach { c ->
            val list = ProviderCatalog.byCategory(c)
            if (list.isNotEmpty()) {
                rows.add(ProviderAdapter.Row.CategoryRow(c))
                list.forEach { rows.add(ProviderAdapter.Row.ProviderRow(it)) }
            }
        }
        adapter.activeId = AppPrefs.activeProvider(requireContext())
        adapter.submit(rows)
    }

    override fun onFilter(category: Category?) {
        filter = category
        refresh()
    }

    override fun onKey(p: ProviderInfo) {
        CommonDialogs.keyDialog(requireContext(), p) { refresh() }
    }

    override fun onModel(p: ProviderInfo) {
        val ctx = requireContext()
        val current = AppPrefs.modelFor(ctx, p.id) ?: p.models.firstOrNull().orEmpty()
        CommonDialogs.modelDialog(ctx, p, current) { model ->
            AppPrefs.setModelFor(ctx, p.id, model)
            AndroidUtils.toast(ctx, "Модель: " + model)
            refresh()
        }
    }

    override fun onSite(p: ProviderInfo) {
        if (p.website.isBlank()) {
            AndroidUtils.toast(requireContext(), "Ссылка не указана")
        } else {
            AndroidUtils.openUrl(requireContext(), p.website)
        }
    }

    override fun onUse(p: ProviderInfo) {
        val ctx = requireContext()
        AppPrefs.setActiveProvider(ctx, p.id)
        adapter.activeId = p.id
        AndroidUtils.toast(ctx, "Активен: " + p.name)

        val needsKey = p.id != "custom" &&
            (p.category != Category.FULLY_FREE || p.id == "openrouter-free") &&
            !ApiKeyStore.has(ctx, p.id)
        if (needsKey) {
            CommonDialogs.keyDialog(ctx, p) { refresh() }
        }
    }
}