package dev.aether.manager.ads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel yang mengelola lifecycle deteksi ad-blocker.
 *
 * - Deteksi dijalankan sekali saat app start (setelah delay singkat)
 * - Pengguna bisa dismiss; hasil dismiss disimpan dalam session agar
 *   dialog tidak muncul terus-menerus (respek terhadap pilihan user)
 */
class AdBlockViewModel(app: Application) : AndroidViewModel(app) {

    private val _detectionResult = MutableStateFlow<AdBlockDetector.DetectionResult?>(null)
    val detectionResult: StateFlow<AdBlockDetector.DetectionResult?> = _detectionResult

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog

    // Berapa kali pengguna sudah dismiss dalam session ini
    private var dismissCount = 0
    private val MAX_PROMPTS_PER_SESSION = 2

    init {
        runDetection()
    }

    private fun runDetection() {
        viewModelScope.launch {
            // Tunggu sebentar agar UI sudah siap
            delay(2_000)
            val result = AdBlockDetector.detect(getApplication())
            _detectionResult.value = result
            if (result.isBlocking && dismissCount < MAX_PROMPTS_PER_SESSION) {
                _showDialog.value = true
            }
        }
    }

    /** Dipanggil saat user klik "Tetap Lanjutkan" */
    fun onUserDismissed() {
        dismissCount++
        _showDialog.value = false
    }

    /** Dipanggil saat user klik "Dukung Kami" — tutup dialog, anggap resolved */
    fun onUserAcknowledged() {
        dismissCount = MAX_PROMPTS_PER_SESSION // jangan tanya lagi session ini
        _showDialog.value = false
    }

    /** Re-check manual (opsional, bisa dipanggil dari Settings) */
    fun recheck() {
        dismissCount = 0
        runDetection()
    }
}
