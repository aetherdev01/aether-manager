package dev.aether.manager.ads

import android.app.Activity
import android.util.Log
import kotlinx.coroutines.*

/**
 * Scheduler iklan interstitial otomatis.
 *
 * Behaviour:
 *  - Iklan pertama muncul setelah [startDelayMs] (default 60 detik)
 *  - Iklan berikutnya muncul setiap [intervalMs] (default 2 menit)
 *  - Tidak spam: hanya show jika ad sudah loaded
 *  - start() aman dipanggil berkali-kali (idempotent)
 *  - stop() saat onPause, start() lagi saat onResume
 */
object AdScheduler {

    private const val TAG = "AdScheduler"

    /** Delay pertama setelah app dibuka (ms) */
    var startDelayMs: Long = 60 * 1_000L       // 1 menit

    /** Interval antar iklan (ms) */
    var intervalMs: Long   = 2 * 60 * 1_000L  // 2 menit

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Mulai scheduler. Aman dipanggil berkali-kali — hanya satu job aktif.
     * [activityProvider] harus return Activity yang sedang foreground, atau null jika tidak ada.
     */
    fun start(activityProvider: () -> Activity?) {
        if (job?.isActive == true) {
            Log.d(TAG, "Scheduler already running, skip start()")
            return
        }

        Log.d(TAG, "Scheduler started — first ad in ${startDelayMs / 1000}s, interval ${intervalMs / 1000}s")
        job = scope.launch {
            delay(startDelayMs)
            while (isActive) {
                val activity = activityProvider()
                if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                    Log.d(TAG, "⏰ Scheduled ad trigger")
                    InterstitialAdManager.showIfReady(activity)
                } else {
                    Log.d(TAG, "Skipping — no valid activity")
                }
                delay(intervalMs)
            }
        }
    }

    /** Hentikan scheduler (panggil saat onPause / onDestroy) */
    fun stop() {
        job?.cancel()
        job = null
        Log.d(TAG, "Scheduler stopped")
    }

    /** Reset timer — iklan berikutnya mundur ke [intervalMs] dari sekarang */
    fun resetTimer(activityProvider: () -> Activity?) {
        stop()
        start(activityProvider)
    }
}
