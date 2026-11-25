package com.devai.studio.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

object PremiumStore {
    private const val PREFS = "devai_premium"
    private const val KEY_PLAN = "plan"
    private const val KEY_EXPIRES = "expires_at"
    private const val KEY_CODE = "code"
    private const val KEY_LABEL = "label"
    private const val KEY_LEGACY_PREMIUM = "premium"
    private const val DAY_MS = 24L * 60L * 60L * 1000L

    private val PLAN_CODES: Map<String, PremiumPlan> = mapOf(
        "DEVAI-WEEK" to PremiumPlan.WEEK,
        "DEVAI-WEEK-2026" to PremiumPlan.WEEK,
        "STARS-WEEK" to PremiumPlan.WEEK,
        "WEEK" to PremiumPlan.WEEK,
        "DEVAI-MONTH" to PremiumPlan.MONTH,
        "DEVAI-MONTH-2026" to PremiumPlan.MONTH,
        "STARS-MONTH" to PremiumPlan.MONTH,
        "MONTH" to PremiumPlan.MONTH,
        "DEVAI-YEAR" to PremiumPlan.YEAR,
        "DEVAI-YEAR-2026" to PremiumPlan.YEAR,
        "STARS-YEAR" to PremiumPlan.YEAR,
        "YEAR" to PremiumPlan.YEAR,
        "DEVAI-LIFETIME" to PremiumPlan.LIFETIME,
        "DEVAI-LIFETIME-2026" to PremiumPlan.LIFETIME,
        "DEVAI-FOREVER" to PremiumPlan.LIFETIME,
        "STARS-LIFETIME" to PremiumPlan.LIFETIME,
        "LIFETIME" to PremiumPlan.LIFETIME,
    )

    private val LEGACY_CODES = setOf(
        "DEVAI-PRO",
        "DEVAI-2025",
        "DEVAI-PRO-2025",
        "PREMIUM",
        "DEVAI-2026",
        "DEVAI2026",
        "DEVAI26",
        "DEVAI-TEST",
        "DEVAI",
        "TEST",
        "PROMO",
    )

    fun plan(ctx: Context): PremiumPlan {
        val stored = p(ctx).getString(KEY_PLAN, null)
        if (stored != null) {
            val plan = PremiumPlan.entries.firstOrNull { it.id == stored } ?: PremiumPlan.FREE
            if (plan == PremiumPlan.FREE) return PremiumPlan.FREE
            val expires = p(ctx).getLong(KEY_EXPIRES, 0L)
            if (plan.isTemporary && expires > 0L && System.currentTimeMillis() >= expires) {
                clear(ctx)
                return PremiumPlan.FREE
            }
            return plan
        }
        if (p(ctx).getBoolean(KEY_LEGACY_PREMIUM, false)) {
            activatePlan(ctx, PremiumPlan.LIFETIME, "LEGACY")
            return PremiumPlan.LIFETIME
        }
        return PremiumPlan.FREE
    }

    fun isPremium(ctx: Context): Boolean = plan(ctx).isPaid

    fun hasPlan(ctx: Context, required: PremiumPlan): Boolean =
        plan(ctx).rank >= required.rank

    fun hasFeature(ctx: Context, feature: PremiumFeature): Boolean =
        hasPlan(ctx, feature.minPlan)

    fun expiresAt(ctx: Context): Long {
        val plan = plan(ctx)
        return if (plan.isTemporary) p(ctx).getLong(KEY_EXPIRES, 0L) else 0L
    }

    fun daysLeft(ctx: Context): Int {
        val expires = expiresAt(ctx)
        if (expires <= 0L) return 0
        return ceil((expires - System.currentTimeMillis()).coerceAtLeast(0L) / DAY_MS.toDouble())
            .toInt()
    }

    fun expiresText(ctx: Context): String {
        val expires = expiresAt(ctx)
        if (expires <= 0L) return ""
        return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(expires))
    }

    fun activate(ctx: Context, raw: String): PremiumPlan? {
        val code = normalize(raw)
        val plan = PLAN_CODES[code]
            ?: if (code in LEGACY_CODES) PremiumPlan.LIFETIME else null
            ?: return null
        activatePlan(ctx, plan, code)
        return plan
    }

    fun activatePlan(
        ctx: Context,
        plan: PremiumPlan,
        code: String = "",
        days: Int? = null,
        label: String = "",
    ) {
        if (!plan.isPaid) {
            clear(ctx)
            return
        }
        val duration = if (plan.isTemporary) {
            days?.takeIf { it > 0 } ?: plan.days
        } else {
            0
        }
        val expires = if (plan.isTemporary) {
            System.currentTimeMillis() + TimeUnit.DAYS.toMillis(duration.toLong())
        } else {
            0L
        }
        p(ctx).edit()
            .putString(KEY_PLAN, plan.id)
            .putLong(KEY_EXPIRES, expires)
            .putString(KEY_CODE, code)
            .putString(KEY_LABEL, label.ifBlank { plan.title })
            .putBoolean(KEY_LEGACY_PREMIUM, true)
            .apply()
    }

    fun planLabel(ctx: Context): String {
        val plan = plan(ctx)
        if (!plan.isPaid) return plan.title
        return p(ctx).getString(KEY_LABEL, plan.title).orEmpty().ifBlank { plan.title }
    }

    fun deactivate(ctx: Context) = clear(ctx)

    private fun clear(ctx: Context) {
        p(ctx).edit()
            .remove(KEY_PLAN)
            .remove(KEY_EXPIRES)
            .remove(KEY_CODE)
            .remove(KEY_LABEL)
            .remove(KEY_LEGACY_PREMIUM)
            .apply()
    }

    fun code(ctx: Context): String = p(ctx).getString(KEY_CODE, "").orEmpty()

    private fun normalize(raw: String): String =
        raw.trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9]+"), "-")
            .trim('-')

    private fun p(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
