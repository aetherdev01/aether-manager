package dev.aether.manager.update

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UpdateUiState {
    object Idle    : UpdateUiState()
    object Checking : UpdateUiState()
    data class UpdateAvailable(val info: ReleaseInfo) : UpdateUiState()
    object UpToDate : UpdateUiState()
    data class CheckError(val message: String) : UpdateUiState()
}

class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    // Dismiss hanya untuk soft update — untuk force update tidak dipakai
    private val _dismissed = MutableStateFlow(false)
    val dismissed: StateFlow<Boolean> = _dismissed.asStateFlow()

    init {
        checkForUpdate()
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _state.value = UpdateUiState.Checking
            _dismissed.value = false

            val currentCode = getCurrentVersionCode()
            when (val result = UpdateChecker.check(currentCode)) {
                is UpdateResult.UpdateAvailable -> {
                    _state.value = UpdateUiState.UpdateAvailable(result.info)
                }
                is UpdateResult.UpToDate -> {
                    _state.value = UpdateUiState.UpToDate
                }
                is UpdateResult.Error -> {
                    _state.value = UpdateUiState.CheckError(result.message)
                }
            }
        }
    }

    fun dismiss() {
        // Hanya boleh dismiss jika bukan force update
        val current = _state.value
        if (current is UpdateUiState.UpdateAvailable && !current.info.isForceUpdate) {
            _dismissed.value = true
        }
    }

    private fun getCurrentVersionCode(): Int {
        return try {
            val ctx = getApplication<Application>()
            val pm  = ctx.packageManager
            val pi  = pm.getPackageInfo(ctx.packageName, 0)
            @Suppress("DEPRECATION")
            pi.versionCode
        } catch (_: PackageManager.NameNotFoundException) {
            0
        }
    }
}
