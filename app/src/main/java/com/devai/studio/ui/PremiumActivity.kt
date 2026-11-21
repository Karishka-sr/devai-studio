package com.devai.studio.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.devai.studio.R
import com.devai.studio.data.PremiumStore
import com.devai.studio.databinding.ActivityPremiumBinding
import com.devai.studio.util.AndroidUtils
import com.devai.studio.util.ThemeManager

class PremiumActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        ThemeManager.applyDynamicIfAvailable(this)
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnActivate.setOnClickListener { activate() }
        binding.btnBuy.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.premium_buy)
                .setMessage(R.string.premium_buy_message)
                .setPositiveButton(R.string.common_ok, null)
                .show()
        }
        refresh()
    }

    private fun activate() {
        val code = binding.promoInput.text.toString()
        if (PremiumStore.activate(this, code)) {
            binding.promoInput.setText("")
            AndroidUtils.toast(this, getString(R.string.premium_activated))
            refresh()
        } else {
            AndroidUtils.toast(this, getString(R.string.premium_wrong_code))
        }
    }

    private fun refresh() {
        val premium = PremiumStore.isPremium(this)
        binding.premiumStatus.text = getString(
            if (premium) R.string.premium_status_active else R.string.premium_status_free
        )
        binding.premiumStatusDetail.text =
            if (premium) getString(R.string.premium_status_active_detail, PremiumStore.code(this))
            else getString(R.string.premium_status_free_detail)
        val free = if (premium) View.GONE else View.VISIBLE
        binding.promoInput.visibility = free
        binding.btnActivate.visibility = free
        binding.btnBuy.visibility = free
    }
}
