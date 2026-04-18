package dev.aether.manager.gamebooster

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.core.app.NotificationCompat
import dev.aether.manager.ui.AetherTheme
import dev.aether.manager.i18n.ProvideStrings

class GameBoosterService : Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    // ── Lifecycle boilerplate untuk ComposeView di luar Activity ─────────
    private val _lifecycle        = LifecycleRegistry(this)
    private val _viewModelStore   = ViewModelStore()
    private val _savedStateCtrl   = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle                 get() = _lifecycle
    override val viewModelStore: ViewModelStore       get() = _viewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = _savedStateCtrl.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null

    companion object {
        const val CHANNEL_ID  = "game_booster_overlay"
        const val NOTIF_ID    = 2001
        const val ACTION_STOP = "dev.aether.manager.STOP_BOOSTER"

        // State bersama — diupdate dari overlay, dibaca ViewModel
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        _savedStateCtrl.performAttach()
        _savedStateCtrl.performRestore(null)
        _lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotification())
        _lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        _lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        isRunning = true
        showOverlay()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        _lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        _lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        _lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        removeOverlay()
        _viewModelStore.clear()
        super.onDestroy()
    }

    // ── Overlay ───────────────────────────────────────────────────────────

    private fun showOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@GameBoosterService)
            setViewTreeViewModelStoreOwner(this@GameBoosterService)
            setViewTreeSavedStateRegistryOwner(this@GameBoosterService)
            setContent {
                AetherTheme {
                    ProvideStrings {
                        GameBoosterOverlay(
                            windowManager = windowManager,
                            layoutParams  = params,
                            composeView   = this@apply,
                            onStop        = { stopSelf() }
                        )
                    }
                }
            }
        }

        windowManager.addView(composeView, params)
    }

    private fun removeOverlay() {
        composeView?.let { v ->
            runCatching { windowManager.removeView(v) }
        }
        composeView = null
    }

    // ── Notification ──────────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Game Booster", NotificationManager.IMPORTANCE_MIN)
                    .apply { setShowBadge(false) }
            )
        }
        val stopIntent = android.app.PendingIntent.getService(
            this, 0,
            Intent(this, GameBoosterService::class.java).apply { action = ACTION_STOP },
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Game Booster Aktif")
            .setContentText("Overlay sidebar aktif di atas layar")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
    }
}
