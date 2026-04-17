package dev.aether.manager.i18n

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import java.util.Locale

// ── Language enum ─────────────────────────────────────────────────────────────

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val langIcon: String,   // 2-letter ISO abbrev shown in UI
) {
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia", "ID"),
    ENGLISH("en", "English", "English", "EN");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code }
                ?: fromSystemLocale(Locale.getDefault())

        fun fromSystemLocale(locale: Locale): AppLanguage =
            when (locale.language) {
                "in", "id" -> INDONESIAN
                else       -> ENGLISH
            }
    }
}

// ── Strings resolver ──────────────────────────────────────────────────────────

fun getStringsForLanguage(language: AppLanguage): AppStrings =
    when (language) {
        AppLanguage.INDONESIAN -> StringsId
        AppLanguage.ENGLISH    -> StringsEn
    }

/** Legacy helper — kept for backward compatibility */
fun getStringsForLocale(locale: Locale): AppStrings =
    getStringsForLanguage(AppLanguage.fromSystemLocale(locale))

// ── Preference helper ─────────────────────────────────────────────────────────

private const val PREFS_NAME = "aether_prefs"
private const val KEY_LANGUAGE = "selected_language"

private fun getPrefs(context: Context): SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

fun loadSavedLanguage(context: Context): AppLanguage? {
    val code = getPrefs(context).getString(KEY_LANGUAGE, null) ?: return null
    return AppLanguage.fromCode(code)
}

fun saveLanguage(context: Context, language: AppLanguage) {
    getPrefs(context).edit().putString(KEY_LANGUAGE, language.code).apply()
}

// ── CompositionLocal ──────────────────────────────────────────────────────────

/** Exposes the current AppLanguage so any composable can read or change it. */
val LocalLanguage = staticCompositionLocalOf<AppLanguage> {
    error("AppLanguage not provided")
}

/** Callback type to change language from any composable. */
val LocalSetLanguage = staticCompositionLocalOf<(AppLanguage) -> Unit> {
    error("SetLanguage not provided")
}

// ── ProvideStrings ────────────────────────────────────────────────────────────

/**
 * Provides [AppStrings], [AppLanguage], and a language-change callback
 * to the entire composable tree.
 *
 * Priority:
 *  1. User-saved preference (SharedPreferences)
 *  2. System locale
 */
@Composable
fun ProvideStrings(content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val initial = remember {
        loadSavedLanguage(context)
            ?: AppLanguage.fromSystemLocale(
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
                    context.resources.configuration.locales[0]
                else
                    @Suppress("DEPRECATION") context.resources.configuration.locale
            )
    }

    var currentLanguage by remember { mutableStateOf(initial) }

    val setLanguage: (AppLanguage) -> Unit = { lang ->
        currentLanguage = lang
        saveLanguage(context, lang)
    }

    val strings = remember(currentLanguage) { getStringsForLanguage(currentLanguage) }

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalLanguage provides currentLanguage,
        LocalSetLanguage provides setLanguage,
    ) {
        content()
    }
}
