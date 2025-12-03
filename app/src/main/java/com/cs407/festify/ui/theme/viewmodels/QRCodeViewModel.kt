package com.cs407.festify.ui.theme.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.festify.data.model.Event
import com.cs407.festify.data.repository.EventRepository
import com.cs407.festify.data.service.QRCodeService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for host's QR code display screen
 */
@HiltViewModel
class QRCodeViewModel @Inject constructor(
    private val repository: EventRepository,
    private val qrCodeService: QRCodeService
) : ViewModel() {

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap.asStateFlow()

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _checkInCount = MutableStateFlow(0)
    val checkInCount: StateFlow<Int> = _checkInCount.asStateFlow()

    private var refreshJob: Job? = null
    private var currentEventId: String? = null

    /**
     * Load event and generate QR code
     */
    fun loadEventAndGenerateQR(eventId: String) {
        currentEventId = eventId
        viewModelScope.launch {
            // Load event
            repository.getEvent(eventId).getOrNull()?.let { event ->
                _event.value = event
                _checkInCount.value = event.totalCheckIns
            }

            // Generate QR code
            generateQR(eventId)

            // Listen to check-ins in real-time to update count
            launch {
                repository.getEventCheckIns(eventId).collect { result ->
                    result.getOrNull()?.let { checkIns ->
                        _checkInCount.value = checkIns.size
                    }
                }
            }
        }
    }

    /**
     * Start auto-refresh of QR code every 2 minutes
     */
    fun startAutoRefresh() {
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(120_000) // 2 minutes
                currentEventId?.let { generateQR(it) }
            }
        }
    }

    /**
     * Stop auto-refresh
     */
    fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    /**
     * Generate QR code for event
     */
    private fun generateQR(eventId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = qrCodeService.generateEventCheckInQR(eventId)
            _qrBitmap.value = bitmap
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
