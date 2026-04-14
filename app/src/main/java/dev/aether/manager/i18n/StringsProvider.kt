package dev.aether.manager.i18n

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.*
import java.util.Locale

/**
 * Returns the AppStrings matching the current system locale.
 * Falls back to English for unsupported locales.
 */
fun getStringsForLocale(locale: Locale): AppStrings {
    return when (locale.language) {
        "in", "id" -> StringsId   // Bahasa Indonesia ("in" = legacy ISO, "id" = modern)
        else        -> StringsEn
    }
}

/**
 * Composable that provides AppStrings and re-composes automatically
 * when the system locale changes (ACTION_LOCALE_CHANGED broadcast).
 *
 * Usage — wrap your root composable:
 *
 *   ProvideStrings {
 *       AetherTheme { ... }
 *   }
 */
@Composable
fun ProvideStrings(content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Start with current locale
    var locale by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                context.resources.configuration.locales[0]
            else
                @Suppress("DEPRECATION") context.resources.configuration.locale
        )
    }

    // Listen for locale changes broadcast (real-time)
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_LOCALE_CHANGED) {
                    locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        ctx!!.resources.configuration.locales[0]
                    else
                        @Suppress("DEPRECATION") ctx!!.resources.configuration.locale
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_LOCALE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    val strings = remember(locale) { getStringsForLocale(locale) }

    CompositionLocalProvider(LocalStrings provides strings) {
        content()
    }
}
