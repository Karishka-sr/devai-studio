package com.devai.studio.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.devai.studio.R
import com.devai.studio.ai.AiFormat
import com.devai.studio.ai.ModelFetcher
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.Category
import com.devai.studio.data.LocalModelStore
import com.devai.studio.data.ProviderCatalog
import com.devai.studio.data.ProviderInfo
import com.devai.studio.databinding.FragmentProvidersBinding
import com.devai.studio.util.AndroidUtils
import kotlinx.coroutines.launch

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
        binding.btnLocalModels.setOnClickListener { openLocalModels() }
        refresh()
    }

    private fun openLocalModels() {
        startActivity(Intent(requireContext(), LocalModelsActivity::class.java))
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
        if (p.format == AiFormat.LOCAL) {
            openLocalModels()
            return
        }
        CommonDialogs.keyDialog(requireContext(), p) { refresh() }
    }

    override fun onModel(p: ProviderInfo) {
        if (p.format == AiFormat.LOCAL) {
            openLocalModels()
            return
        }
        openModelDialog(p)
    }

    private fun openModelDialog(p: ProviderInfo) {
        val ctx = requireContext()
        val current = AppPrefs.modelFor(ctx, p.id) ?: p.models.firstOrNull().orEmpty()
        val extra = AppPrefs.modelsFor(ctx, p.id)
        CommonDialogs.modelDialog(
            ctx,
            p,
            current,
            extra,
            onFetch = { fetchModels(p) }
        ) { model ->
            AppPrefs.setModelFor(ctx, p.id, model)
            AndroidUtils.toast(ctx, "Модель: " + model)
            refresh()
        }
    }

    private fun fetchModels(p: ProviderInfo) {
        val ctx = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val key = ApiKeyStore.get(ctx, p.id).orEmpty()
                val base = if (p.id == "custom") AppPrefs.customBase(ctx) else p.baseUrl
                if (base.isBlank()) {
                    AndroidUtils.toast(ctx, getString(R.string.model_fetch_error, "base URL пуст"))
                    return@launch
                }
                val models = ModelFetcher.fetch(p, key, base)
                if (models.isEmpty()) {
                    AndroidUtils.toast(ctx, getString(R.string.model_fetch_empty))
                } else {
                    AppPrefs.setModelsFor(ctx, p.id, models)
                    AndroidUtils.toast(ctx, getString(R.string.model_fetched, models.size))
                    if (isAdded) openModelDialog(p)
                }
            } catch (e: Exception) {
                AndroidUtils.toast(
                    ctx,
                    getString(R.string.model_fetch_error, e.message ?: "network")
                )
            }
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

        if (p.format == AiFormat.LOCAL) {
            if (LocalModelStore.active(ctx) == null) openLocalModels()
            return
        }

        val needsKey = p.id != "custom" &&
            (p.category != Category.FULLY_FREE || p.id == "openrouter-free") &&
            !ApiKeyStore.has(ctx, p.id)
        if (needsKey) {
            CommonDialogs.keyDialog(ctx, p) { refresh() }
        }
    }
}