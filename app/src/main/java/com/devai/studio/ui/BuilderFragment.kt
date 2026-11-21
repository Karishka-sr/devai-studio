package com.devai.studio.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.devai.studio.R
import com.devai.studio.ai.AiClientFactory
import com.devai.studio.ai.AiMessage
import com.devai.studio.builder.ProjectGenerator
import com.devai.studio.builder.ZipSaver
import com.devai.studio.databinding.FragmentBuilderBinding
import com.devai.studio.util.AiSupport
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
            R.id.type_web -> Kind.WEB
            R.id.type_python -> Kind.PYTHON
            R.id.type_kotlin -> Kind.KOTLIN
            R.id.type_ai -> Kind.AI
            else -> Kind.ANDROID
        }

        setBusy(true, getString(R.string.builder_busy))
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val files = withContext(Dispatchers.IO) { generateFiles(kind, desc) }
                val bytes = withContext(Dispatchers.IO) { ZipSaver.buildZip(files) }
                val safe = desc.ifBlank { "project" }.take(20).replace(Regex("[^A-Za-z0-9_-]"), "_")
                val name = safe + "_" + System.currentTimeMillis() + ".zip"
                val uri = withContext(Dispatchers.IO) { ZipSaver.save(ctx, name, bytes) }

                setBusy(false, getString(R.string.builder_done, name))
                if (zipOnly) {
                    CommonDialogs.resultDialog(
                        ctx, "ZIP готов",
                        "Файлов: " + files.size + "\n" + name, uri, "application/zip"
                    )
                } else {
                    CommonDialogs.resultDialog(
                        ctx, "APK-заготовка",
                        getString(R.string.builder_apk_instructions), uri, "application/zip"
                    )
                }
            } catch (e: Exception) {
                setBusy(false, "Ошибка: " + (e.message ?: e.javaClass.simpleName))
            }
        }
    }

    private suspend fun generateFiles(kind: Kind, desc: String): Map<String, String> = when (kind) {
        Kind.ANDROID -> ProjectGenerator.android("MyApp", "com.devai.app", aiCodeOrNull(desc, "android"), desc)
        Kind.WEB -> ProjectGenerator.web("MyApp", aiCodeOrNull(desc, "html"))
        Kind.PYTHON -> ProjectGenerator.python("script", aiCodeOrNull(desc, "python"))
        Kind.KOTLIN -> ProjectGenerator.kotlinCli("cli", aiCodeOrNull(desc, "kotlin"))
        Kind.AI -> ProjectGenerator.android("MyApp", "com.devai.app", aiCodeOrNull(desc, "android"), desc)
    }

    private suspend fun aiCodeOrNull(desc: String, target: String): String? {
        if (desc.isBlank()) return null
        val cfg = try {
            AiSupport.config(requireContext())
        } catch (e: Exception) {
            return null
        }
        return try {
            val client = AiClientFactory.create(cfg)
            val sys = "Ты — генератор кода. Верни ТОЛЬКО код файла, без пояснений и без markdown-разметки."
            val raw = client.chat(
                listOf(AiMessage("system", sys), AiMessage("user", buildPrompt(target, desc)))
            ) {}
            stripFences(raw)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildPrompt(target: String, desc: String): String = when (target) {
        "android" ->
            "Сгенерируй файл MainActivity.kt (Kotlin, package com.devai.app, AndroidX AppCompatActivity) для приложения: " + desc
        "html" ->
            "Сгенерируй полный файл index.html (HTML+CSS+JS в одном файле) для приложения: " + desc
        "python" ->
            "Сгенерируй файл main.py (Python 3) для задачи: " + desc
        else ->
            "Сгенерируй файл Main.kt (Kotlin, fun main) для задачи: " + desc
    }

    private fun stripFences(s: String): String {
        var t = s.trim()
        if (t.startsWith("```")) {
            val nl = t.indexOf('\n')
            if (nl >= 0) t = t.substring(nl + 1)
            if (t.endsWith("```")) t = t.substring(0, t.length - 3)
        }
        return t.trim()
    }

    private fun setBusy(b: Boolean, status: String) {
        busy = b
        val bd = _binding ?: return
        bd.btnZip.isEnabled = !b
        bd.btnApk.isEnabled = !b
        bd.builderStatus.text = status
        bd.builderStatus.visibility = View.VISIBLE
    }

    private enum class Kind { ANDROID, WEB, PYTHON, KOTLIN, AI }
}
