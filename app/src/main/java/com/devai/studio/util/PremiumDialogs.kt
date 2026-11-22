package com.devai.studio.util

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.devai.studio.R
import com.devai.studio.data.PremiumPlan
import com.devai.studio.ui.PremiumActivity

object PremiumDialogs {
    fun show(ctx: Context, feature: String, plan: PremiumPlan? = null) {
        val message = if (plan != null && plan.isPaid) {
            ctx.getString(R.string.premium_locked_plan, feature, plan.title, plan.stars)
        } else {
            ctx.getString(R.string.premium_locked_message, feature)
        }
        AlertDialog.Builder(ctx)
            .setTitle(R.string.premium_title)
            .setMessage(message)
            .setPositiveButton(R.string.premium_open) { _, _ ->
                ctx.startActivity(Intent(ctx, PremiumActivity::class.java))
            }
            .setNegativeButton(R.string.premium_later, null)
            .show()
    }
}
