package dev.aether.manager.ads

/**
 * Central place for all AdMob unit IDs.
 * Ganti TEST_* ke ID production sebelum publish.
 */
object AdManager {

    // ── Production IDs ────────────────────────────────────────────────────────
    private const val BANNER_ID_PROD = "ca-app-pub-5043818314955328/4052266582"

    // ── Test IDs (Google resmi) — aktif saat debug ────────────────────────────
    private const val BANNER_ID_TEST = "ca-app-pub-3940256099942544/6300978111"

    /**
     * true  = pakai production ID (release build)
     * false = pakai test ID (debug build)
     */
    private val isRelease: Boolean
        get() = !dev.aether.manager.BuildConfig.DEBUG

    val bannerId: String
        get() = if (isRelease) BANNER_ID_PROD else BANNER_ID_TEST
}
