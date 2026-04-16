package dev.aether.manager.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AppsUiState {
    object Loading : AppsUiState()
    data class Ready(
        val apps: List<AppInfo>,
        val profiles: Map<String, AppProfile>,
        val monitorRunning: Boolean,
    ) : AppsUiState()
    data class Error(val msg: String) : AppsUiState()
}

class AppProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<AppsUiState>(AppsUiState.Loading)
    val state: StateFlow<AppsUiState> = _state.asStateFlow()

    private val _snack = MutableStateFlow<String?>(null)
    val snack: StateFlow<String?> = _snack.asStateFlow()

    private val _editingProfile = MutableStateFlow<AppProfile?>(null)
    val editingProfile: StateFlow<AppProfile?> = _editingProfile.asStateFlow()

    private val _savingPkg = MutableStateFlow<String?>(null)
    val savingPkg: StateFlow<String?> = _savingPkg.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch(Dispatchers.IO) {
        _state.value = AppsUiState.Loading
        try {
            val apps     = AppProfileRepository.loadUserApps(getApplication())
            val profiles = AppProfileRepository.loadAllProfiles()
            val running  = AppProfileRepository.isMonitorRunning()
            _state.value = AppsUiState.Ready(apps, profiles, running)
        } catch (e: Exception) {
            _state.value = AppsUiState.Error(e.message ?: "Gagal memuat aplikasi")
        }
    }

    fun openEditor(app: AppInfo) = viewModelScope.launch(Dispatchers.IO) {
        val current = AppProfileRepository.loadProfile(app.packageName)
        _editingProfile.value = current
    }

    fun closeEditor() { _editingProfile.value = null }

    fun saveProfile(profile: AppProfile) = viewModelScope.launch(Dispatchers.IO) {
        _savingPkg.value = profile.packageName
        try {
            AppProfileRepository.saveProfile(profile)
            val s = _state.value
            if (s is AppsUiState.Ready) {
                val updated = s.profiles.toMutableMap()
                updated[profile.packageName] = profile
                _state.value = s.copy(profiles = updated)
            }
            _editingProfile.value = null
            snack("Profile disimpan ✓")
        } catch (e: Exception) {
            snack("Gagal simpan: ${e.message}")
        }
        _savingPkg.value = null
    }

    fun toggleMonitor(enable: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        if (enable) {
            AppProfileRepository.startMonitor()
            snack("Monitor aktif — profile akan diapply otomatis")
        } else {
            AppProfileRepository.stopMonitor()
            snack("Monitor dimatikan")
        }
        val s = _state.value
        if (s is AppsUiState.Ready) {
            _state.value = s.copy(monitorRunning = enable)
        }
    }

    fun deleteProfile(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
        AppProfileRepository.deleteProfile(packageName)
        val s = _state.value
        if (s is AppsUiState.Ready) {
            val updated = s.profiles.toMutableMap()
            updated.remove(packageName)
            _state.value = s.copy(profiles = updated)
        }
        snack("Profile dihapus")
    }

    fun snack(msg: String) { _snack.value = msg }
    fun clearSnack() { _snack.value = null }
}
