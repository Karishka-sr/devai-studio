package com.devai.studio.ui

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.devai.studio.R
import com.devai.studio.builder.AiProjectGenerator
import com.devai.studio.builder.ZipSaver
import com.devai.studio.data.Project
import com.devai.studio.data.ProjectStore
import com.devai.studio.databinding.FragmentWorkspaceBinding
import com.devai.studio.util.AndroidUtils
import kotlinx.coroutines.launch

class WorkspaceFragment : Fragment() {

    private var _binding: FragmentWorkspaceBinding? = null
    private val binding get() = _binding!!

    private lateinit var projectAdapter: ProjectAdapter
    private lateinit var fileAdapter: FileAdapter

    private var projectId: String = ""
    private var filePath: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkspaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        projectAdapter = ProjectAdapter(
            onOpen = { showFiles(it.id) },
            onExport = { export(it) },
            onDelete = { confirmDeleteProject(it) }
        )
        fileAdapter = FileAdapter(
            onOpen = { openEditor(it) },
            onDelete = { confirmDeleteFile(it) }
        )
        binding.projectList.layoutManager = LinearLayoutManager(requireContext())
        binding.projectList.adapter = projectAdapter
        binding.fileList.layoutManager = LinearLayoutManager(requireContext())
        binding.fileList.adapter = fileAdapter

        binding.btnNewProject.setOnClickListener { newProjectDialog() }
        binding.btnBack.setOnClickListener { showList() }
        binding.btnAddFile.setOnClickListener { addFileDialog() }
        binding.btnExportZip.setOnClickListener {
            ProjectStore.get(requireContext(), projectId)?.let { export(it) }
        }
        binding.btnDeleteProject.setOnClickListener {
            ProjectStore.get(requireContext(), projectId)?.let { confirmDeleteProject(it) }
        }
        binding.btnEditorBack.setOnClickListener { showFiles(projectId) }
        binding.btnSaveFile.setOnClickListener { saveFile() }
    }

    override fun onResume() {
        super.onResume()
        when {
            binding.listContainer.visibility == View.VISIBLE -> showList()
            binding.filesContainer.visibility == View.VISIBLE -> showFiles(projectId)
        }
    }

    private fun showList() {
        projectId = ""
        binding.listContainer.visibility = View.VISIBLE
        binding.filesContainer.visibility = View.GONE
        binding.editorContainer.visibility = View.GONE
        val list = ProjectStore.list(requireContext())
        projectAdapter.submit(list)
        binding.workspaceEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.projectList.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showFiles(id: String) {
        projectId = id
        val p = ProjectStore.get(requireContext(), id)
        if (p == null) {
            showList()
            return
        }
        binding.projectTitle.text = p.name
        binding.listContainer.visibility = View.GONE
        binding.filesContainer.visibility = View.VISIBLE
        binding.editorContainer.visibility = View.GONE
        val files = ProjectStore.files(requireContext(), id)
        fileAdapter.submit(files)
        binding.filesEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
        binding.fileList.visibility = if (files.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun openEditor(path: String) {
        val content = ProjectStore.read(requireContext(), projectId, path) ?: return
        filePath = path
        binding.editorPath.text = path
        binding.editorText.setText(content)
        binding.listContainer.visibility = View.GONE
        binding.filesContainer.visibility = View.GONE
        binding.editorContainer.visibility = View.VISIBLE
    }

    private fun saveFile() {
        if (projectId.isBlank() || filePath.isBlank()) return
        val ctx = requireContext()
        val ok = ProjectStore.write(ctx, projectId, filePath, binding.editorText.text.toString())
        if (ok) AndroidUtils.toast(ctx, getString(R.string.editor_saved))
        else AndroidUtils.toast(ctx, "Не удалось сохранить файл")
    }

    private fun export(p: Project) {
        val ctx = requireContext()
        try {
            val bytes = ProjectStore.exportZip(ctx, p.id)
            if (bytes.isEmpty()) {
                AndroidUtils.toast(ctx, getString(R.string.project_files_empty))
                return
            }
            val name = safeName(p.name) + "_" + System.currentTimeMillis() + ".zip"
            val uri = ZipSaver.save(ctx, name, bytes)
            CommonDialogs.resultDialog(
                ctx,
                "ZIP",
                getString(R.string.builder_done, name),
                uri,
                "application/zip"
            )
        } catch (e: Exception) {
            AndroidUtils.toast(ctx, getString(R.string.chat_error, e.message ?: "zip"))
        }
    }

    private fun safeName(s: String): String =
        s.replace(Regex("[^A-Za-z0-9._-]"), "_").take(40).ifBlank { "project" }

    private fun confirmDeleteProject(p: Project) {
        val ctx = requireContext()
        AlertDialog.Builder(ctx)
            .setTitle(R.string.project_delete)
            .setMessage(getString(R.string.project_delete_confirm, p.name))
            .setPositiveButton(R.string.project_delete) { _, _ ->
                ProjectStore.delete(ctx, p.id)
                showList()
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun confirmDeleteFile(path: String) {
        val ctx = requireContext()
        AlertDialog.Builder(ctx)
            .setTitle(R.string.project_delete)
            .setMessage(getString(R.string.file_delete_confirm, path))
            .setPositiveButton(R.string.project_delete) { _, _ ->
                ProjectStore.deleteFile(ctx, projectId, path)
                showFiles(projectId)
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun addFileDialog() {
        if (projectId.isBlank()) return
        val ctx = requireContext()
        val edit = EditText(ctx).apply {
            hint = getString(R.string.new_file_path)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        AlertDialog.Builder(ctx)
            .setTitle(R.string.new_file_title)
            .setView(padded(ctx, edit))
            .setPositiveButton(R.string.common_ok) { _, _ ->
                val path = edit.text.toString().trim()
                if (path.isBlank()) return@setPositiveButton
                if (ProjectStore.write(ctx, projectId, path, "")) showFiles(projectId)
                else AndroidUtils.toast(ctx, "Некорректный путь файла")
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun newProjectDialog() {
        val ctx = requireContext()
        val name = EditText(ctx).apply { hint = getString(R.string.new_project_name) }
        val desc = EditText(ctx).apply { hint = getString(R.string.new_project_desc) }
        val types = arrayOf("android", "web", "python", "kotlin")
        val labels = arrayOf("Android", "Web", "Python", "Kotlin")

        val group = RadioGroup(ctx).apply { orientation = RadioGroup.VERTICAL }
        labels.forEachIndexed { i, label ->
            val rb = RadioButton(ctx).apply {
                id = i + 1
                text = label
                isChecked = i == 0
            }
            group.addView(rb)
        }
        val ai = CheckBox(ctx).apply { text = getString(R.string.new_project_ai) }

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 20), dp(ctx, 8), dp(ctx, 20), dp(ctx, 8))
            addView(name)
            addView(desc)
            addView(group)
            addView(ai)
        }

        AlertDialog.Builder(ctx)
            .setTitle(R.string.new_project_title)
            .setView(ScrollView(ctx).apply { addView(container) })
            .setPositiveButton(R.string.new_project_create) { _, _ ->
                val idx = (group.checkedRadioButtonId - 1).coerceIn(0, types.size - 1)
                val kind = types[idx]
                val projectName = name.text.toString().trim().ifBlank { "Проект" }
                val projectDesc = desc.text.toString().trim()
                val project = ProjectStore.create(ctx, projectName, kind)
                if (ai.isChecked && projectDesc.isNotBlank()) {
                    generateInto(project, kind, projectDesc)
                } else {
                    ProjectStore.import(ctx, project.id, AiProjectGenerator.templates(kind, projectDesc))
                    showFiles(project.id)
                }
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun generateInto(project: Project, kind: String, desc: String) {
        val ctx = requireContext()
        showFiles(project.id)
        AndroidUtils.toast(ctx, getString(R.string.builder_busy))
        viewLifecycleOwner.lifecycleScope.launch {
            val result = AiProjectGenerator.generate(ctx, kind, desc)
            ProjectStore.import(ctx, project.id, result.files)
            if (!isAdded) return@launch
            AndroidUtils.toast(
                ctx,
                getString(if (result.usedAi) R.string.builder_ai_used else R.string.builder_template_used)
            )
            showFiles(project.id)
        }
    }

    private fun padded(ctx: Context, view: View): View =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 20), dp(ctx, 8), dp(ctx, 20), dp(ctx, 8))
            addView(view)
        }

    private fun dp(ctx: Context, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()
}
