package dev.aether.manager.ads

/**
 * Central place for all AdMob unit IDs.
 * Production ID diambil dari native layer (XOR-encrypted di binary).
 */
object AdManager {

    // ── Native library ────────────────────────────────────────────────────────
    init {
        System.loadLibrary("aether-x")
    }

    /**
     * Ambil AdMob ID dari native layer.
     * @param key 0 = Banner production ID
     */
    @JvmStatic
    private external fun nGetAdId(key: Int): String

    // ── Test IDs (Google resmi) — aktif saat debug ────────────────────────────
    private const val BANNER_ID_TEST = "ca-app-pub-3940256099942544/6300978111"

    /**
     * true  = pakai production ID (release build)
     * false = pakai test ID (debug build)
     */
    private val isRelease: Boolean
        get() = !dev.aether.manager.BuildConfig.DEBUG

    val bannerId: String
        get() = if (isRelease) nGetAdId(0) else BANNER_ID_TEST
}
