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
    object Idle       : UpdateUiState()
    object Checking   : UpdateUiState()
    data class UpdateAvailable(val info: ReleaseInfo) : UpdateUiState()
    object UpToDate   : UpdateUiState()
    data class CheckError(val message: String) : UpdateUiState()
}

class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private val _dismissed = MutableStateFlow(false)
    val dismissed: StateFlow<Boolean> = _dismissed.asStateFlow()

    /** Versi app yang sedang berjalan, e.g. "v1.2" */
    val currentVersionName: String by lazy {
        try {
            val ctx = getApplication<Application>()
            @Suppress("DEPRECATION")
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "v?"
        } catch (_: PackageManager.NameNotFoundException) { "v?" }
    }

    init { checkForUpdate() }

    fun checkForUpdate() {
        viewModelScope.launch {
            _state.value   = UpdateUiState.Checking
            _dismissed.value = false

            val currentCode = getCurrentVersionCode()
            _state.value = when (val result = UpdateChecker.check(currentCode)) {
                is UpdateResult.UpdateAvailable -> UpdateUiState.UpdateAvailable(result.info)
                is UpdateResult.UpToDate        -> UpdateUiState.UpToDate
                is UpdateResult.Error           -> UpdateUiState.CheckError(result.message)
            }
        }
    }

    /** User tap "Nanti" — sembunyikan dialog sampai app restart. */
    fun dismiss() {
        if (_state.value is UpdateUiState.UpdateAvailable) {
            _dismissed.value = true
        }
    }

    private fun getCurrentVersionCode(): Int = try {
        val ctx = getApplication<Application>()
        @Suppress("DEPRECATION")
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode
    } catch (_: PackageManager.NameNotFoundException) { 0 }
}
