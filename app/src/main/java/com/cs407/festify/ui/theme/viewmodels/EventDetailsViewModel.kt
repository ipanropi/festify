package com.cs407.festify.ui.theme.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.festify.data.model.Event
import com.cs407.festify.data.remote.FirestoreCollections
import com.cs407.festify.data.repository.EventRepository
import com.cs407.festify.data.repository.CheckInStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailsViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _userRsvpStatus = MutableStateFlow<String>("not_attending")
    val userRsvpStatus: StateFlow<String> = _userRsvpStatus.asStateFlow()

    private val _isVouched = MutableStateFlow(false)
    val isVouched: StateFlow<Boolean> = _isVouched.asStateFlow()

    private val _checkInStatus = MutableStateFlow<CheckInStatus>(
        CheckInStatus.NotCheckedIn
    )
    val checkInStatus: StateFlow<CheckInStatus> = _checkInStatus.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            // 1. Load Event Data
            val result = repository.getEvent(eventId)
            if (result.isSuccess) {
                _event.value = result.getOrNull()
            }

            // 2. Start listening to the RSVP status
            launch {
                repository.getUserRsvpStatus(eventId).collect { status ->
                    _userRsvpStatus.value = status ?: "not_attending"
                }
            }

            // 3. Check if I vouched
            launch {
                repository.hasUserVouched(eventId).collect { vouched ->
                    // ONLY update if the UI isn't already "ahead" of the server
                    // This prevents the "reset to 0" flicker
                    if (_isVouched.value != vouched) {
                        _isVouched.value = vouched
                    }
                }
            }

            // 4. Load check-in status
            launch {
                repository.getUserCheckInStatus(eventId).collect { status ->
                    _checkInStatus.value = status
                }
            }
        }
    }



        fun toggleRsvp(eventId: String) {
            viewModelScope.launch {
                val currentStatus = _userRsvpStatus.value

                if (currentStatus == FirestoreCollections.RsvpStatus.ATTENDING) {
                    // If already attending, cancel it
                    repository.cancelRsvp(eventId)
                } else {
                    // If not attending, join
                    repository.rsvpToEvent(eventId, FirestoreCollections.RsvpStatus.ATTENDING)
                }
            }
        }

        fun toggleVouch(eventId: String) {
            // 1. Get current snapshot
            val currentEvent = _event.value ?: return
            val wasVouched = _isVouched.value
            val currentCount = currentEvent.vouchCount

            // 2. Calculate target state logic
            val newVouchedState = !wasVouched
            val newCount = if (newVouchedState) {
                currentCount + 1
            } else {
                maxOf(0, currentCount - 1)
            }

            // 3. INSTANT UI UPDATE (Optimistic)
            // This makes the star turn Gold instantly and the number change instantly.
            _isVouched.value = newVouchedState
            _event.value = currentEvent.copy(vouchCount = newCount)

            // 4. Send to Server in Background
            viewModelScope.launch {
                try {
                    repository.toggleVouch(eventId)
                } catch (e: Exception) {
                    _isVouched.value = wasVouched
                    _event.value = currentEvent.copy(vouchCount = currentCount)
                }
            }
        }
    }
