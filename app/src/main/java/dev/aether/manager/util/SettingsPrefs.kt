package dev.aether.manager.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Centralized SharedPreferences helper for app settings.
 * Uses the same "aether_prefs" file as SetupActivity/SplashActivity.
 */
object SettingsPrefs {

    private const val PREFS_NAME = "aether_prefs"

    private const val KEY_DARK_MODE     = "dark_mode"
    private const val KEY_DARK_OVERRIDE = "dark_mode_override"  // true = manual, false = follow system
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    private const val KEY_AUTO_BACKUP   = "auto_backup"
    private const val KEY_APPLY_ON_BOOT = "apply_on_boot"
    private const val KEY_NOTIFICATIONS = "notifications"
    private const val KEY_DEBUG_LOG     = "debug_log"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Dark mode ────────────────────────────────────────────────────────────
    /** true = dark, false = light — only meaningful when [isDarkModeOverride] is true */
    fun getDarkMode(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_DARK_MODE, false)

    /** true = user has manually set dark/light, false = follow system */
    fun isDarkModeOverride(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_DARK_OVERRIDE, false)

    fun setDarkMode(ctx: Context, dark: Boolean) {
        prefs(ctx).edit()
            .putBoolean(KEY_DARK_MODE, dark)
            .putBoolean(KEY_DARK_OVERRIDE, true)
            .apply()
    }

    fun clearDarkModeOverride(ctx: Context) {
        prefs(ctx).edit()
            .putBoolean(KEY_DARK_OVERRIDE, false)
            .apply()
    }

    // ── Dynamic color ────────────────────────────────────────────────────────
    fun getDynamicColor(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_DYNAMIC_COLOR, true)

    fun setDynamicColor(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    // ── Auto backup ──────────────────────────────────────────────────────────
    fun getAutoBackup(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_AUTO_BACKUP, false)

    fun setAutoBackup(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply()
    }

    // ── Apply on boot ────────────────────────────────────────────────────────
    fun getApplyOnBoot(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_APPLY_ON_BOOT, true)

    fun setApplyOnBoot(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_APPLY_ON_BOOT, enabled).apply()
    }

    // ── Notifications ────────────────────────────────────────────────────────
    fun getNotifications(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_NOTIFICATIONS, true)

    fun setNotifications(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    // ── Debug log ────────────────────────────────────────────────────────────
    fun getDebugLog(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_DEBUG_LOG, false)

    fun setDebugLog(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_DEBUG_LOG, enabled).apply()
    }
}
