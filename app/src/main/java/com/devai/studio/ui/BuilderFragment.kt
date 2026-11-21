package com.devai.studio.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.devai.studio.R
import com.devai.studio.builder.AiProjectGenerator
import com.devai.studio.builder.ZipSaver
import com.devai.studio.data.ProjectStore
import com.devai.studio.databinding.FragmentBuilderBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BuilderFragment : Fragment() {

    private var _binding: FragmentBuilderBinding? = null
    private val binding get() = _binding!!
    private var busy = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBuilderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnZip.setOnClickListener { build(zipOnly = true) }
        binding.btnApk.setOnClickListener { build(zipOnly = false) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun build(zipOnly: Boolean) {
        if (busy) return
        val ctx = requireContext()
        val desc = binding.builderDesc.text.toString().trim()
        val kind = when (binding.typeGroup.checkedRadioButtonId) {
            R.id.type_web -> "web"
            R.id.type_python -> "python"
            R.id.type_kotlin -> "kotlin"
            else -> "android"
        }

        setBusy(true, getString(R.string.builder_busy))
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = AiProjectGenerator.generate(ctx, kind, desc)
                val projectName = desc.ifBlank { defaultName(kind) }.take(40)
                val project = withContext(Dispatchers.IO) {
                    ProjectStore.create(ctx, projectName, kind).also {
                        ProjectStore.import(ctx, it.id, result.files)
                    }
                }
                val bytes = withContext(Dispatchers.IO) { ZipSaver.buildZip(result.files) }
                val safe = desc.ifBlank { "project" }.take(20).replace(Regex("[^A-Za-z0-9_-]"), "_")
                val name = safe + "_" + System.currentTimeMillis() + ".zip"
                val uri = withContext(Dispatchers.IO) { ZipSaver.save(ctx, name, bytes) }

                val source = getString(
                    if (result.usedAi) R.string.builder_ai_used else R.string.builder_template_used
                )
                val status = getString(R.string.builder_saved_project) + " · " + source
                setBusy(false, status)

                val details = "Файлов: " + result.files.size + "\n" + status + "\n" + name +
                    "\n\nПроект: " + project.name
                if (zipOnly) {
                    CommonDialogs.resultDialog(ctx, "ZIP готов", details, uri, "application/zip")
                } else {
                    CommonDialogs.resultDialog(
                        ctx,
                        "APK-заготовка",
                        getString(R.string.builder_apk_instructions) + "\n\n" + details,
                        uri,
                        "application/zip"
                    )
                }
            } catch (e: Exception) {
                setBusy(false, "Ошибка: " + (e.message ?: e.javaClass.simpleName))
            }
        }
    }

    private fun defaultName(kind: String): String = when (kind) {
        "web" -> "WebApp"
        "python" -> "PythonApp"
        "kotlin" -> "KotlinCLI"
        else -> "AndroidApp"
    }

    private fun setBusy(b: Boolean, status: String) {
        busy = b
        val bd = _binding ?: return
        bd.btnZip.isEnabled = !b
        bd.btnApk.isEnabled = !b
        bd.builderStatus.text = status
        bd.builderStatus.visibility = View.VISIBLE
    }
}
