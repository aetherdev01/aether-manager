package dev.aether.manager.ads

import android.app.Activity
import android.util.Log
import kotlinx.coroutines.*

/**
 * Menjalankan iklan interstitial secara otomatis setiap [intervalMs] milidetik.
 * Dimulai dengan [startDelayMs] agar user sempat melihat konten terlebih dahulu.
 *
 * Cara pakai:
 *   - Panggil [start] di onResume / LaunchedEffect(Unit)
 *   - Panggil [stop] di onPause / DisposableEffect onDispose
 */
object AdScheduler {

    private const val TAG = "AdScheduler"

    /** Interval antar iklan (default: 5 menit) */
    var intervalMs: Long = 2 * 60 * 1_000L

    /** Delay pertama setelah app dibuka sebelum iklan pertama tampil */
    var startDelayMs: Long = 60 * 1_000L  // 1 menit

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Mulai jadwal iklan otomatis.
     * Aman dipanggil berkali-kali — hanya satu job yang jalan.
     */
    fun start(activityProvider: () -> Activity?) {
        if (job?.isActive == true) return
        Log.d(TAG, "Scheduler started — first ad in ${startDelayMs / 1000}s, then every ${intervalMs / 1000}s")
        job = scope.launch {
            delay(startDelayMs)
            while (isActive) {
                val activity = activityProvider()
                if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                    Log.d(TAG, "Scheduled ad trigger")
                    InterstitialAdManager.showIfReady(activity)
                }
                delay(intervalMs)
            }
        }
    }

    /** Hentikan jadwal (saat app di-pause atau destroy) */
    fun stop() {
        job?.cancel()
        job = null
        Log.d(TAG, "Scheduler stopped")
    }
}
