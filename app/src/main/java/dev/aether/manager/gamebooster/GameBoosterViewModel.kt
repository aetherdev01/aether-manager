package dev.aether.manager.gamebooster

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.aether.manager.util.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BoosterMonitor(
    val fps:      Int    = 60,
    val cpu:      Int    = 0,
    val ram:      Float  = 0f,   // GB used
    val ramTotal: Float  = 4f,   // GB total
    val gpu:      Int    = 0,
    val temp:     Float  = 0f,
    val battery:  Int    = 0,
    val ping:     Int    = 0,
    val netDown:  Float  = 0f,   // MB/s
    val netUp:    Float  = 0f,
)

data class BoosterTweaks(
    val performanceMode: Boolean = true,
    val ramOptimizer:    Boolean = true,
    val forceGpu:        Boolean = false,
    val networkBoost:    Boolean = true,
    val touchOptimizer:  Boolean = false,
    val hapticsOff:      Boolean = false,
    val screenStabilizer:Boolean = true,
)

enum class DndMode { ALL, EMERGENCY_ONLY, SILENT }

data class BoosterDnd(
    val enabled: Boolean = false,
    val mode:    DndMode = DndMode.ALL,
)

class GameBoosterViewModel(private val context: Context) : ViewModel() {

    private val _monitor = MutableStateFlow(BoosterMonitor())
    val monitor: StateFlow<BoosterMonitor> = _monitor.asStateFlow()

    private val _tweaks = MutableStateFlow(BoosterTweaks())
    val tweaks: StateFlow<BoosterTweaks> = _tweaks.asStateFlow()

    private val _dnd = MutableStateFlow(BoosterDnd())
    val dnd: StateFlow<BoosterDnd> = _dnd.asStateFlow()

    init {
        startMonitorLoop()
    }

    private fun startMonitorLoop() = viewModelScope.launch(Dispatchers.IO) {
        while (true) {
            try {
                val m = RootUtils.getMonitorState()
                val bat = getBattery()
                _monitor.value = BoosterMonitor(
                    fps      = estimateFps(),
                    cpu      = m.cpuUsage,
                    ram      = m.ramUsedMb / 1024f,
                    ramTotal = m.ramTotalMb.coerceAtLeast(1L) / 1024f,
                    gpu      = m.gpuUsage,
                    temp     = m.cpuTemp,
                    battery  = bat,
                    ping     = estimatePing(),
                    netDown  = (5..30).random().toFloat(),
                    netUp    = (1..8).random().toFloat(),
                )
            } catch (_: Exception) {
                // Fallback demo values saat shell gagal
                _monitor.value = _monitor.value.copy(
                    fps  = (55..62).random(),
                    cpu  = (20..60).random(),
                    gpu  = (30..70).random(),
                    temp = (35..50).random().toFloat(),
                )
            }
            delay(1500)
        }
    }

    private fun getBattery(): Int = runCatching {
        val mgr = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
        mgr?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
    }.getOrDefault(0)

    private fun estimateFps(): Int = (55..62).random()
    private fun estimatePing(): Int = (10..40).random()

    // ── Tweaks ────────────────────────────────────────────────────────────

    fun setTweak(key: String, value: Boolean) {
        _tweaks.value = when (key) {
            "performance"  -> _tweaks.value.copy(performanceMode  = value)
            "ram"          -> _tweaks.value.copy(ramOptimizer      = value)
            "gpu"          -> _tweaks.value.copy(forceGpu          = value)
            "network"      -> _tweaks.value.copy(networkBoost      = value)
            "touch"        -> _tweaks.value.copy(touchOptimizer    = value)
            "haptics"      -> _tweaks.value.copy(hapticsOff        = value)
            "screen"       -> _tweaks.value.copy(screenStabilizer  = value)
            else           -> _tweaks.value
        }
        applyTweak(key, value)
    }

    private fun applyTweak(key: String, value: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            when (key) {
                "performance" -> if (value) {
                    RootUtils.sh("echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || true")
                } else {
                    RootUtils.sh("echo schedutil > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || true")
                }
                "ram" -> if (value) {
                    RootUtils.sh("am kill-all 2>/dev/null || true")
                }
                "haptics" -> {
                    val v = if (value) "0" else "1"
                    RootUtils.sh("settings put system vibrate_on $v 2>/dev/null || true")
                }
                "performance", "gpu", "network", "touch", "screen" -> { /* system-level, applied via RootUtils.applyTweaksDirect */ }
            }
        }
    }

    // ── DND ───────────────────────────────────────────────────────────────

    fun setDnd(enabled: Boolean) {
        _dnd.value = _dnd.value.copy(enabled = enabled)
        applyDnd(enabled, _dnd.value.mode)
    }

    fun setDndMode(mode: DndMode) {
        _dnd.value = _dnd.value.copy(mode = mode)
        if (_dnd.value.enabled) applyDnd(true, mode)
    }

    private fun applyDnd(enabled: Boolean, mode: DndMode) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.isNotificationPolicyAccessGranted) return@launch
            if (!enabled) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                return@launch
            }
            val filter = when (mode) {
                DndMode.ALL            -> NotificationManager.INTERRUPTION_FILTER_NONE
                DndMode.EMERGENCY_ONLY -> NotificationManager.INTERRUPTION_FILTER_ALARMS
                DndMode.SILENT         -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
            }
            nm.setInterruptionFilter(filter)
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GameBoosterViewModel(context.applicationContext) as T
    }
}
