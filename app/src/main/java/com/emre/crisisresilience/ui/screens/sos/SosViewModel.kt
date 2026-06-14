package com.emre.crisisresilience.ui.screens.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emre.crisisresilience.data.hardware.SosFlashController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SosViewModel @Inject constructor(
    private val sosFlashController: SosFlashController
) : ViewModel() {

    private val _isFlashing = MutableStateFlow(false)
    val isFlashing: StateFlow<Boolean> = _isFlashing.asStateFlow()

    private val _customText = MutableStateFlow("SOS")
    val customText: StateFlow<String> = _customText.asStateFlow()

    private var sosJob: Job? = null

    fun updateCustomText(text: String) {
        _customText.value = text
    }

    fun toggleSos() {
        if (_isFlashing.value) {
            stopSos()
        } else {
            startSos()
        }
    }

    private fun startSos() {
        _isFlashing.value = true
        // Eski job varsa iptal et
        sosJob?.cancel()

        val text = _customText.value.ifBlank { "SOS" }
        sosJob = viewModelScope.launch {
            sosFlashController.startSosSignal(text)
        }
    }

    fun stopSos() {
        _isFlashing.value = false
        sosJob?.cancel()
        sosJob = null
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel ölürken donanımı mutlaka serbest bırak
        stopSos()
    }
}
