package dev.aether.manager.data

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aether.manager.ads.InterstitialAdManager
import dev.aether.manager.util.BackupManager
import dev.aether.manager.util.DeviceInfo
import dev.aether.manager.util.RootManager
import dev.aether.manager.util.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TweaksState(
    val schedboost: Boolean = false,
    val cpuBoost: Boolean = false,
    val gpuThrottleOff: Boolean = false,
    val cpusetOpt: Boolean = false,
    val mtkBoost: Boolean = false,
    val lmkAggressive: Boolean = false,
    val zram: Boolean = false,
    val zramSize: String = "1073741824",
    val zramAlgo: String = "lz4",
    val vmDirtyOpt: Boolean = false,
    val ioScheduler: String = "",
    val ioLatencyOpt: Boolean = false,
    val tcpBbr: Boolean = false,
    val doh: Boolean = false,
    val netBuffer: Boolean = false,
    val doze: Boolean = false,
    val fastAnim: Boolean = false,
    val entropyBoost: Boolean = false,
    val clearCache: Boolean = false,
)

data class MonitorState(
    val cpuUsage: Int = 0,
    val cpuFreq: String = "",
    val gpuUsage: Int = 0,
    val gpuFreq: String = "",
    val ramUsedMb: Long = 0L,
    val ramTotalMb: Long = 0L,
    val cpuTemp: Float = 0f,
    val batTemp: Float = 0f,
    val storageUsedGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    val uptime: String = "",
    val batLevel: Int = 0,
    val cpuGovernor: String = "",
    val swapUsedMb: Long = 0L,
    val swapTotalMb: Long = 0L,
)

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val msg: String) : UiState<Nothing>()
}

class MainViewModel : ViewModel() {

    private val _rootGranted = MutableStateFlow<Boolean?>(null)
    val rootGranted: StateFlow<Boolean?> = _rootGranted.asStateFlow()

    private val _deviceInfo = MutableStateFlow<UiState<DeviceInfo>>(UiState.Loading)
    val deviceInfo: StateFlow<UiState<DeviceInfo>> = _deviceInfo.asStateFlow()

    private val _tweaks = MutableStateFlow(TweaksState())
    val tweaks: StateFlow<TweaksState> = _tweaks.asStateFlow()

    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    private val _applyingTweak = MutableStateFlow(false)
    val applyingTweak: StateFlow<Boolean> = _applyingTweak.asStateFlow()

    private val _monitorState = MutableStateFlow(MonitorState())
    val monitorState: StateFlow<MonitorState> = _monitorState.asStateFlow()

    private var monitorStarted = false

    init {
        // PENTING: Di sini TIDAK boleh request root!
        // RootManager sudah di-markGranted() oleh SetupActivity sebelum
        // MainActivity dibuka. Kita hanya pakai cachedRoot yang sudah ada.
        viewModelScope.launch {
            initFromCachedRoot()
        }
    }

    /**
     * Inisialisasi dari cache RootManager — TANPA memunculkan dialog baru.
     * Setup sudah selesai sebelum MainActivity, jadi root pasti sudah granted
     * atau prefs setup_done tidak akan pernah true.
     */
    private suspend fun initFromCachedRoot() {
        // Pakai cached state dari RootManager (hasil SetupActivity)
        val hasRoot = if (RootManager.isRootGranted) {
            true
        } else {
            // Fallback: silent check tanpa dialog baru
            RootUtils.hasRoot()
        }

        _rootGranted.value = hasRoot
        if (hasRoot) {
            loadAll()
            startMonitorLoop()
        } else {
            _deviceInfo.value = UiState.Error(
                "Root access denied.\nAether Manager requires root."
            )
        }
    }

    /**
     * refresh() — dipakai user tap "Coba Lagi".
     * Melakukan silent check, TIDAK memunculkan dialog grant baru.
     */
    fun refresh() = viewModelScope.launch {
        _deviceInfo.value = UiState.Loading
        val hasRoot = RootUtils.hasRoot()
        _rootGranted.value = hasRoot
        if (hasRoot) {
            loadAll()
            if (!monitorStarted) startMonitorLoop()
        } else {
            _deviceInfo.value = UiState.Error(
                "Root access denied.\nAether Manager requires root."
            )
        }
    }

    fun refreshMonitor() = viewModelScope.launch {
        _monitorState.value = RootUtils.getMonitorState()
    }

    private fun startMonitorLoop() = viewModelScope.launch(Dispatchers.IO) {
        if (monitorStarted) return@launch
        monitorStarted = true
        while (true) {
            try {
                val state = RootUtils.getMonitorState()
                _monitorState.value = state
            } catch (_: Exception) {}
            delay(3000)
        }
    }

    private suspend fun loadAll() {
        try {
            val info = RootUtils.getDeviceInfo()
            _deviceInfo.value = UiState.Success(info)
            loadTweaks()
        } catch (e: Exception) {
            _deviceInfo.value = UiState.Error(e.message ?: "Failed to load device info")
        }
    }

    private suspend fun loadTweaks() {
        val map = RootUtils.readTweaksConf()
        _tweaks.value = mapToTweaksState(map)
    }

    private fun mapToTweaksState(map: Map<String, String>) = TweaksState(
        schedboost     = map["schedboost"] == "1",
        cpuBoost       = map["cpu_boost"] == "1",
        gpuThrottleOff = map["gpu_throttle_off"] == "1",
        cpusetOpt      = map["cpuset_opt"] == "1",
        mtkBoost       = map["obb_noop"] == "1",
        lmkAggressive  = map["lmk_aggressive"] == "1",
        zram           = map["zram"] == "1",
        zramSize       = map["zram_size"] ?: "1073741824",
        zramAlgo       = map["zram_algo"] ?: "lz4",
        vmDirtyOpt     = map["vm_dirty_opt"] == "1",
        ioScheduler    = map["io_scheduler"] ?: "",
        ioLatencyOpt   = map["io_latency_opt"] == "1",
        tcpBbr         = map["tcp_bbr"] == "1",
        doh            = map["doh"] == "1",
        netBuffer      = map["net_buffer"] == "1",
        doze           = map["doze"] == "1",
        fastAnim       = map["fast_anim"] == "1",
        entropyBoost   = map["entropy_boost"] == "1",
        clearCache     = map["clear_cache"] == "1",
    )

    /** Optimistic update — UI berubah langsung, apply background */
    fun setTweak(key: String, value: Boolean) {
        val current = _tweaks.value
        _tweaks.value = applyTweakToState(current, key, value)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                RootUtils.writeTweakConf(key, if (value) "1" else "0")
                val map = RootUtils.readTweaksConf()
                RootUtils.applyTweaksDirect(map)
                _tweaks.value = mapToTweaksState(map)
            } catch (e: Exception) {
                _tweaks.value = current
                snack("Gagal apply: ${e.message}")
            }
        }
    }

    fun setTweakStr(key: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            RootUtils.writeTweakConf(key, value)
            val map = RootUtils.readTweaksConf()
            _tweaks.value = mapToTweaksState(map)
        }
    }

    private fun applyTweakToState(state: TweaksState, key: String, value: Boolean): TweaksState =
        when (key) {
            "schedboost"       -> state.copy(schedboost = value)
            "cpu_boost"        -> state.copy(cpuBoost = value)
            "gpu_throttle_off" -> state.copy(gpuThrottleOff = value)
            "cpuset_opt"       -> state.copy(cpusetOpt = value)
            "obb_noop"         -> state.copy(mtkBoost = value)
            "lmk_aggressive"   -> state.copy(lmkAggressive = value)
            "zram"             -> state.copy(zram = value)
            "vm_dirty_opt"     -> state.copy(vmDirtyOpt = value)
            "io_latency_opt"   -> state.copy(ioLatencyOpt = value)
            "tcp_bbr"          -> state.copy(tcpBbr = value)
            "doh"              -> state.copy(doh = value)
            "net_buffer"       -> state.copy(netBuffer = value)
            "doze"             -> state.copy(doze = value)
            "fast_anim"        -> state.copy(fastAnim = value)
            "entropy_boost"    -> state.copy(entropyBoost = value)
            "clear_cache"      -> state.copy(clearCache = value)
            else               -> state
        }

    fun setProfile(profile: String) = viewModelScope.launch {
        _applyingTweak.value = true
        val ok = RootUtils.setProfileDirect(profile)
        if (ok) {
            delay(300)
            val info = RootUtils.getDeviceInfo()
            _deviceInfo.value = UiState.Success(info)
            _monitorState.value = RootUtils.getMonitorState()
            snack("Profile → ${profile.replaceFirstChar { it.uppercaseChar() }}")
        }
        _applyingTweak.value = false
    }

    fun applyAll(activity: Activity? = null) = viewModelScope.launch {
        _applyingTweak.value = true
        val map = RootUtils.readTweaksConf()
        RootUtils.applyTweaksDirect(map)
        delay(400)
        _applyingTweak.value = false
        snack("Tweaks applied ✓")
        // Show interstitial after apply — respects cooldown, skippable
        if (activity != null) {
            InterstitialAdManager.showIfReady(activity)
        }
    }

    fun toggleSafeMode(enable: Boolean) = viewModelScope.launch {
        RootUtils.toggleSafeMode(enable)
        refresh()
    }

    fun reboot(mode: RootUtils.RebootMode) = viewModelScope.launch {
        RootUtils.reboot(mode)
    }

    // ── Backup & Reset ────────────────────────────────────────────────────

    private val _backupList = MutableStateFlow<List<BackupManager.BackupEntry>>(emptyList())
    val backupList: StateFlow<List<BackupManager.BackupEntry>> = _backupList.asStateFlow()

    private val _backupWorking = MutableStateFlow(false)
    val backupWorking: StateFlow<Boolean> = _backupWorking.asStateFlow()

    fun loadBackups() = viewModelScope.launch(Dispatchers.IO) {
        _backupList.value = BackupManager.listBackups()
    }

    fun createBackup() = viewModelScope.launch {
        _backupWorking.value = true
        val name = BackupManager.createBackup()
        if (name != null) {
            snack("Backup tersimpan: $name")
            loadBackups()
        } else {
            snack("Gagal membuat backup")
        }
        _backupWorking.value = false
    }

    fun restoreBackup(filename: String) = viewModelScope.launch {
        _backupWorking.value = true
        val ok = BackupManager.restoreBackup(filename)
        if (ok) {
            // Reload tweaks dari conf yang baru di-restore
            val map = RootUtils.readTweaksConf()
            _tweaks.value = mapToTweaksState(map)
            RootUtils.applyTweaksDirect(map)
            snack("Restore berhasil ✓")
        } else {
            snack("Gagal restore backup")
        }
        _backupWorking.value = false
    }

    fun deleteBackup(filename: String) = viewModelScope.launch(Dispatchers.IO) {
        BackupManager.deleteBackup(filename)
        _backupList.value = BackupManager.listBackups()
    }

    fun resetToDefaults() = viewModelScope.launch {
        _backupWorking.value = true
        val ok = BackupManager.resetToDefaults()
        if (ok) {
            // Reload state setelah reset
            _tweaks.value = TweaksState()  // semua default
            val info = RootUtils.getDeviceInfo()
            _deviceInfo.value = UiState.Success(info)
            snack("Reset ke default berhasil ✓")
        } else {
            snack("Gagal reset ke default")
        }
        _backupWorking.value = false
    }

    fun snack(msg: String) { _snackMessage.value = msg }
    fun clearSnack() { _snackMessage.value = null }
}
