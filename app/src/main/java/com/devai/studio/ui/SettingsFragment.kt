package com.devai.studio.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.devai.studio.R
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.AppPrefs
import com.devai.studio.databinding.FragmentSettingsBinding
import com.devai.studio.util.AndroidUtils

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

    private fun updateTemperatureLabel() {
        val v = binding.temperatureBar.progress / 20f
        binding.temperatureLabel.text = getString(R.string.settings_temperature) + " " + "%.2f".format(v)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}