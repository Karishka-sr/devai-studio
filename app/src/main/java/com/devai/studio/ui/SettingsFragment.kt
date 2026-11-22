package com.devai.studio.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.devai.studio.R
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.PremiumStore
import com.devai.studio.databinding.FragmentSettingsBinding
import com.devai.studio.util.AndroidUtils
import com.devai.studio.util.PremiumDialogs
import com.devai.studio.util.ThemeManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()

        runCatching {
            binding.authorArt.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            binding.authorArt.setContent { AuthorArtBanner() }
        }.onFailure {
            binding.authorArt.visibility = View.GONE
        }

        binding.temperatureBar.max = 20
        binding.temperatureBar.progress = (AppPrefs.temperature(ctx) * 20f).toInt().coerceIn(0, 20)
        updateTemperatureLabel()
        binding.temperatureBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                AppPrefs.setTemperature(ctx, progress / 20f)
                updateTemperatureLabel()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.maxTokensInput.setText(AppPrefs.maxTokens(ctx).toString())
        binding.maxTokensInput.doAfterTextChanged { editable ->
            val value = editable?.toString()?.toIntOrNull()
            if (value != null && value in 1..100000) AppPrefs.setMaxTokens(ctx, value)
        }

        binding.customBaseInput.setText(AppPrefs.customBase(ctx))
        binding.customBaseInput.doAfterTextChanged {
            AppPrefs.setCustomBase(ctx, it?.toString().orEmpty())
        }

        binding.customModelInput.setText(AppPrefs.customModel(ctx))
        binding.customModelInput.doAfterTextChanged {
            AppPrefs.setCustomModel(ctx, it?.toString().orEmpty())
        }

        binding.systemPromptInput.setText(AppPrefs.systemPrompt(ctx))
        binding.systemPromptInput.doAfterTextChanged {
            AppPrefs.setSystemPrompt(ctx, it?.toString().orEmpty())
        }

        binding.btnTheme.text = getString(
            R.string.settings_theme_current,
            getString(ThemeManager.labelRes(AppPrefs.theme(ctx)))
        )
        binding.btnTheme.setOnClickListener { showThemeDialog() }
        binding.btnAuthorTelegram.setOnClickListener {
            openUrl(getString(R.string.author_telegram_url))
        }
        binding.btnAuthorSite.setOnClickListener {
            openUrl(getString(R.string.author_site_url))
        }
        binding.authorCredit?.setOnClickListener {
            openUrl(getString(R.string.author_vexel_url))
        }
        binding.btnPremium.setOnClickListener {
            startActivity(Intent(requireContext(), PremiumActivity::class.java))
        }
        refreshPremium()

        binding.btnClearKeys.setOnClickListener {
            AlertDialog.Builder(ctx)
                .setTitle("Сбросить все ключи?")
                .setMessage("API-ключи будут удалены с устройства.")
                .setPositiveButton("Да") { _, _ ->
                    ApiKeyStore.clearAll(ctx)
                    AndroidUtils.toast(ctx, getString(R.string.settings_clear_confirmed))
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun showThemeDialog() {
        val ctx = requireContext()
        val ids = ThemeManager.all
        val labels = ids.map { getString(ThemeManager.labelRes(it)) }.toTypedArray()
        val current = ids.indexOf(AppPrefs.theme(ctx)).coerceAtLeast(0)
        AlertDialog.Builder(ctx)
            .setTitle(R.string.settings_appearance)
            .setSingleChoiceItems(labels, current) { dialog, which ->
                val id = ids[which]
                if (ThemeManager.isPremium(id) && !PremiumStore.isPremium(ctx)) {
                    dialog.dismiss()
                    PremiumDialogs.show(ctx, getString(R.string.premium_feature_themes))
                    return@setSingleChoiceItems
                }
                AppPrefs.setTheme(ctx, id)
                dialog.dismiss()
                requireActivity().recreate()
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        refreshPremium()
    }

    private fun refreshPremium() {
        val b = _binding ?: return
        b.btnPremium.text = getString(
            if (PremiumStore.isPremium(requireContext())) R.string.premium_status_button_active
            else R.string.premium_status_button_free
        )
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            AndroidUtils.toast(requireContext(), getString(R.string.settings_author_link_error))
        }
    }

    private fun updateTemperatureLabel() {
        val v = binding.temperatureBar.progress / 20f
        binding.temperatureLabel.text = getString(R.string.settings_temperature) + " " + "%.2f".format(v)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}