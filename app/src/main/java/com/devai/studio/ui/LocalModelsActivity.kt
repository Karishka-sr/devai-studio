package com.devai.studio.ui

import android.app.ActivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.devai.studio.R
import com.devai.studio.ai.LocalLlmEngine
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.LocalModel
import com.devai.studio.data.LocalModelStore
import com.devai.studio.data.ModelDownloader
import com.devai.studio.data.PremiumStore
import com.devai.studio.databinding.ActivityLocalModelsBinding
import com.devai.studio.util.AndroidUtils
import com.devai.studio.util.PremiumDialogs
import com.devai.studio.util.ThemeManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LocalModelsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocalModelsBinding
    private lateinit var adapter: LocalModelAdapter
    private val jobs = HashMap<String, Job>()

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val (name, size) = fileMeta(uri)
            confirmImport(uri, name, size)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        ThemeManager.applyDynamicIfAvailable(this)
        binding = ActivityLocalModelsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = LocalModelAdapter(
            this,
            onDownload = { startDownload(it) },
            onCancel = { jobs[it.id]?.cancel() },
            onUse = { use(it) },
            onDelete = { confirmDelete(it) }
        )
        binding.modelsList.layoutManager = LinearLayoutManager(this)
        binding.modelsList.adapter = adapter
        binding.btnAddUrl.setOnClickListener { addUrlDialog() }
        binding.btnImport.setOnClickListener { pickFile.launch(arrayOf("*/*")) }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        adapter.submit(LocalModelStore.catalog(this), LocalModelStore.activeId(this))
        binding.storageInfo.text = getString(
            R.string.local_models_storage,
            human(LocalModelStore.freeSpace(this))
        )
    }

    private fun startDownload(m: LocalModel) {
        if (!PremiumStore.hasPlan(this, m.requiredPlan)) {
            PremiumDialogs.show(this, m.name, m.requiredPlan)
            return
        }
        if (m.url.isBlank()) {
            AndroidUtils.toast(this, getString(R.string.local_models_no_url))
            return
        }
        if (!Build.SUPPORTED_ABIS.contains("arm64-v8a")) {
            AndroidUtils.toast(this, getString(R.string.local_models_abi))
            return
        }
        val info = ActivityManager.MemoryInfo()
        getSystemService(ActivityManager::class.java)?.getMemoryInfo(info)
        val totalRamGb = (info.totalMem / 1_000_000_000L).toInt()
        if (totalRamGb < m.minRamGb) {
            AlertDialog.Builder(this)
                .setTitle(R.string.local_models_title)
                .setMessage(getString(R.string.local_models_ram_warning, totalRamGb, m.minRamGb))
                .setPositiveButton(R.string.common_ok) { _, _ -> launchDownload(m) }
                .setNegativeButton(R.string.common_cancel, null)
                .show()
        } else {
            launchDownload(m)
        }
    }

    private fun launchDownload(m: LocalModel) {
        adapter.setDownloading(m.id, true)
        jobs[m.id] = lifecycleScope.launch {
            try {
                ModelDownloader.download(this@LocalModelsActivity, m) { done, total ->
                    runOnUiThread { adapter.setProgress(m.id, done, total) }
                }
                LocalModelStore.setActive(this@LocalModelsActivity, m.id)
                AppPrefs.setActiveProvider(this@LocalModelsActivity, "local")
                AndroidUtils.toast(
                    this@LocalModelsActivity,
                    getString(R.string.local_models_done, m.name)
                )
            } catch (e: CancellationException) {
                AndroidUtils.toast(this@LocalModelsActivity, getString(R.string.local_models_paused))
            } catch (e: Exception) {
                AndroidUtils.toast(
                    this@LocalModelsActivity,
                    getString(R.string.local_models_error, e.message ?: "download")
                )
            } finally {
                jobs.remove(m.id)
                adapter.setDownloading(m.id, false)
                refresh()
            }
        }
    }

    private fun use(m: LocalModel) {
        if (!PremiumStore.hasPlan(this, m.requiredPlan)) {
            PremiumDialogs.show(this, m.name, m.requiredPlan)
            return
        }
        LocalModelStore.setActive(this, m.id)
        AppPrefs.setActiveProvider(this, "local")
        AndroidUtils.toast(this, getString(R.string.local_models_selected, m.name))
        refresh()
    }

    private fun confirmDelete(m: LocalModel) {
        AlertDialog.Builder(this)
            .setTitle(R.string.local_models_delete)
            .setMessage(getString(R.string.local_models_delete_confirm, m.name))
            .setPositiveButton(R.string.local_models_delete) { _, _ ->
                jobs[m.id]?.cancel()
                LocalLlmEngine.close()
                LocalModelStore.delete(this, m)
                refresh()
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun addUrlDialog() {
        val name = EditText(this).apply { hint = getString(R.string.local_models_name) }
        val url = EditText(this).apply {
            hint = getString(R.string.local_models_url)
            inputType = InputType.TYPE_TEXT_VARIATION_URI
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
            addView(name)
            addView(url)
            addView(TextView(this@LocalModelsActivity).apply {
                text = getString(R.string.local_models_license_note)
                textSize = 11f
            })
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.local_models_add_title)
            .setView(ScrollView(this).apply { addView(container) })
            .setPositiveButton(R.string.local_models_add) { _, _ ->
                val n = name.text.toString().trim().ifBlank { getString(R.string.local_models_custom_name) }
                val u = url.text.toString().trim()
                if (u.isBlank()) {
                    AndroidUtils.toast(this, getString(R.string.local_models_no_url))
                    return@setPositiveButton
                }
                val model = LocalModelStore.addCustom(this, n, u, 0L, "auto")
                refresh()
                launchDownload(model)
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun confirmImport(uri: Uri, name: String, size: Long) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
            addView(TextView(this@LocalModelsActivity).apply {
                text = name + " · " + human(size)
                textSize = 13f
            })
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.local_models_import_title)
            .setView(ScrollView(this).apply { addView(container) })
            .setPositiveButton(R.string.local_models_import) { _, _ ->
                importFile(uri, name, "auto")
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun importFile(uri: Uri, name: String, format: String) {
        val progress = AlertDialog.Builder(this)
            .setMessage(R.string.local_models_copying)
            .setCancelable(false)
            .create()
        progress.show()
        lifecycleScope.launch {
            try {
                val dir = LocalModelStore.modelsDir(this@LocalModelsActivity)
                val extension = name.substringAfterLast('.', "task")
                    .lowercase()
                    .takeIf { it == "task" || it == "litertlm" }
                    ?: "task"
                val fileName = "imported_" + System.currentTimeMillis() + "." + extension
                val target = File(dir, fileName)
                withContext(Dispatchers.IO) {
                    val input = contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Не удалось открыть файл")
                    input.use { ins ->
                        FileOutputStream(target).use { out -> ins.copyTo(out, 256 * 1024) }
                    }
                }
                val model = LocalModelStore.addImported(
                    this@LocalModelsActivity, name, fileName, target.length(), format
                )
                LocalModelStore.setActive(this@LocalModelsActivity, model.id)
                AppPrefs.setActiveProvider(this@LocalModelsActivity, "local")
                AndroidUtils.toast(
                    this@LocalModelsActivity,
                    getString(R.string.local_models_imported, model.name)
                )
            } catch (e: Exception) {
                AndroidUtils.toast(
                    this@LocalModelsActivity,
                    getString(R.string.local_models_error, e.message ?: "import")
                )
            } finally {
                progress.dismiss()
                refresh()
            }
        }
    }

    private fun fileMeta(uri: Uri): Pair<String, Long> {
        var name = "model.task"
        var size = 0L
        runCatching {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = c.getColumnIndex(OpenableColumns.SIZE)
                if (c.moveToFirst()) {
                    if (nameIndex >= 0) c.getString(nameIndex)?.let { name = it }
                    if (sizeIndex >= 0 && !c.isNull(sizeIndex)) size = c.getLong(sizeIndex)
                }
            }
        }
        return name to size
    }

    private fun human(bytes: Long): String = when {
        bytes >= 1_000_000_000L -> String.format("%.2f ГБ", bytes / 1e9)
        bytes >= 1_000_000L -> String.format("%.0f МБ", bytes / 1e6)
        else -> String.format("%.0f КБ", bytes / 1e3)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
