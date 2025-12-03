package com.cs407.festify.ui.theme.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.festify.data.repository.EventRepository
import com.cs407.festify.data.service.QRCodeService
import com.cs407.festify.data.service.QRValidation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for attendee's QR scanner screen
 */
@HiltViewModel
class QRScannerViewModel @Inject constructor(
    private val repository: EventRepository,
    private val qrCodeService: QRCodeService
) : ViewModel() {

    private val _scanResult = MutableStateFlow<String?>(null)
    val scanResult: StateFlow<String?> = _scanResult.asStateFlow()

    private val _checkInSuccess = MutableStateFlow<Result<Unit>?>(null)
    val checkInSuccess: StateFlow<Result<Unit>?> = _checkInSuccess.asStateFlow()

    private var isProcessing = false

    /**
     * Process scanned QR code data
     * @param qrData The raw QR code data
     */
    fun processQRCode(qrData: String) {
        if (isProcessing) return
        isProcessing = true

        viewModelScope.launch {
            try {
                // Validate QR code
                when (val validation = qrCodeService.validateQRPayload(qrData)) {
                    is QRValidation.Valid -> {
                        // Perform check-in
                        val result = repository.checkInToEvent(validation.eventId)
                        _checkInSuccess.value = result
                    }
                    QRValidation.Expired -> {
                        _checkInSuccess.value = Result.failure(Exception("QR code has expired"))
                    }
                    QRValidation.InvalidSignature -> {
                        _checkInSuccess.value = Result.failure(Exception("Invalid QR code"))
                    }
                    QRValidation.Malformed -> {
                        _checkInSuccess.value = Result.failure(Exception("Invalid QR code format"))
                    }
                }
            } finally {
                isProcessing = false
            }
        }
    }

    /**
     * Clear check-in result (for dismissing dialogs)
     */
    fun clearCheckInResult() {
        _checkInSuccess.value = null
    }
}
