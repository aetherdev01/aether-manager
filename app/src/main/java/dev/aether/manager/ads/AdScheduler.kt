package dev.aether.manager.ads

import android.app.Activity
import android.util.Log
import kotlinx.coroutines.*

/**
 * Scheduler iklan interstitial otomatis.
 *
 * Behaviour:
 *  - Iklan pertama muncul setelah [startDelayMs] (default 30 detik)
 *  - Iklan berikutnya muncul setiap [intervalMs] (default 90 detik)
 *  - [tryShow] bisa dipanggil dari mana saja (tab switch, scroll, dsb)
 *    tapi hanya akan show jika sudah lewat [minIntervalMs] sejak iklan terakhir
 *  - start() idempotent — aman dipanggil berkali-kali
 */
object AdScheduler {

    private const val TAG = "AdScheduler"

    /** Delay pertama setelah app dibuka sebelum iklan pertama */
    var startDelayMs: Long  = 30 * 1_000L   // 30 detik

    /** Interval loop scheduler */
    var intervalMs: Long    = 90 * 1_000L   // 90 detik

    /**
     * Minimum jeda antar iklan — dipakai oleh tryShow() supaya
     * trigger dari tab/scroll tidak spam melebihi ini.
     */
    var minIntervalMs: Long = 90 * 1_000L   // 90 detik

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /** Timestamp terakhir iklan ditampilkan (atau dicoba) */
    @Volatile private var lastShownMs: Long = 0L

    // ── Internal activity ref ─────────────────────────────────────────────
    @Volatile private var activityProvider: (() -> Activity?)? = null

    /**
     * Mulai scheduler otomatis. Aman dipanggil berkali-kali.
     */
    fun start(provider: () -> Activity?) {
        activityProvider = provider
        if (job?.isActive == true) {
            Log.d(TAG, "Scheduler already running")
            return
        }
        Log.d(TAG, "Scheduler started — first ad in ${startDelayMs / 1000}s, loop every ${intervalMs / 1000}s")
        job = scope.launch {
            delay(startDelayMs)
            while (isActive) {
                showNow()
                delay(intervalMs)
            }
        }
    }

    /** Hentikan scheduler (onPause) */
    fun stop() {
        job?.cancel()
        job = null
        activityProvider = null
        Log.d(TAG, "Scheduler stopped")
    }

    /**
     * Coba tampilkan iklan sekarang dari trigger eksternal (tab switch, scroll, dsb).
     * Akan di-skip jika belum lewat [minIntervalMs] sejak iklan terakhir.
     */
    fun tryShow() {
        val now = System.currentTimeMillis()
        if (now - lastShownMs < minIntervalMs) {
            Log.d(TAG, "tryShow: throttled, ${(minIntervalMs - (now - lastShownMs)) / 1000}s remaining")
            return
        }
        showNow()
    }

    /** Tampilkan iklan sekarang tanpa cek throttle (dipanggil dari scheduler loop) */
    private fun showNow() {
        val activity = activityProvider?.invoke()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Log.d(TAG, "showNow: no valid activity")
            return
        }
        Log.d(TAG, "⏰ Ad trigger")
        lastShownMs = System.currentTimeMillis()
        InterstitialAdManager.showIfReady(activity)
    }
}
