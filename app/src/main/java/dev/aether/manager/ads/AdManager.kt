package dev.aether.manager.ads

/**
 * Central Unity Ads configuration.
 * Game ID: 6091240 (production)
 */
object AdManager {

    // ── Credentials ───────────────────────────────────────────
    const val GAME_ID                  = "6091240"
    const val BANNER_PLACEMENT_ID      = "Banner_Android"
    const val INTERSTITIAL_PLACEMENT_ID = "Interstitial_Android"

    // ── Anti-spam policy ──────────────────────────────────────
    /** Minimum gap (ms) between two interstitial shows. Default: 10 minutes. */
    const val INTERSTITIAL_COOLDOWN_MS = 2L * 60_000L   // 10 min

    /** Minimum app session time (ms) before the first interstitial can appear. */
    const val FIRST_SHOW_DELAY_MS      = 1L  * 60_000L   // 3 min after launch

    // ── Mode ──────────────────────────────────────────────────
    /** true = test mode (debug build), false = production */
    val isTestMode: Boolean
        get() = dev.aether.manager.BuildConfig.DEBUG
}
