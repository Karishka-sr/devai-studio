package com.devai.studio.util

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.devai.studio.R
import com.devai.studio.ui.PremiumActivity

object PremiumDialogs {
    fun show(ctx: Context, feature: String) {
        AlertDialog.Builder(ctx)
            .setTitle(R.string.premium_title)
            .setMessage(ctx.getString(R.string.premium_locked_message, feature))
            .setPositiveButton(R.string.premium_open) { _, _ ->
                ctx.startActivity(Intent(ctx, PremiumActivity::class.java))
            }
            .setNegativeButton(R.string.premium_later, null)
            .show()
    }
}
