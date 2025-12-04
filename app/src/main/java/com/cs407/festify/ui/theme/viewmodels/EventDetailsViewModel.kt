package com.cs407.festify.ui.theme.viewmodels

import android.content.Context
import android.widget.Toast
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

    private val _userRsvpStatus = MutableStateFlow("not_attending")
    val userRsvpStatus: StateFlow<String> = _userRsvpStatus.asStateFlow()

    private val _isVouched = MutableStateFlow<Boolean?>(null)
    val isVouched: StateFlow<Boolean?> = _isVouched.asStateFlow()

    private val _checkInStatus =
        MutableStateFlow<CheckInStatus>(CheckInStatus.NotCheckedIn)
    val checkInStatus: StateFlow<CheckInStatus> = _checkInStatus.asStateFlow()

    /**
     * Load initial event data + listen to live updates.
     */
    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            repository.observeEvent(eventId).collect { updatedEvent ->
                _event.value = updatedEvent
            }
        }

        // 2. Listen to RSVP status updates
        viewModelScope.launch {
            repository.getUserRsvpStatus(eventId).collect { status ->
                _userRsvpStatus.value = status ?: "not_attending"
            }
        }

        // 3. Listen to vouch status
        viewModelScope.launch {
            repository.hasUserVouched(eventId).collect { vouched ->
                if (_isVouched.value != vouched) {
                    _isVouched.value = vouched
                }
            }
        }

        // 4. Listen to user check-in status
        viewModelScope.launch {
            repository.getUserCheckInStatus(eventId).collect { status ->
                _checkInStatus.value = status
            }
        }
    }

    /**
     * Toggle RSVP between attending / not attending
     */
    fun toggleRsvp(eventId: String) {
        viewModelScope.launch {
            val current = _userRsvpStatus.value

            if (current == FirestoreCollections.RsvpStatus.ATTENDING) {
                repository.cancelRsvp(eventId)
            } else {
                repository.rsvpToEvent(eventId, FirestoreCollections.RsvpStatus.ATTENDING)
            }
        }
    }

    /**
     * Toggle vouching for an event (optimistic UI update)
     */
    fun toggleVouch(eventId: String) {
        val wasVouched = _isVouched.value ?: return
        val eventData = _event.value ?: return
        val currentCount = eventData.vouchCount

        val newState = !wasVouched
        val newCount = if (newState) currentCount + 1 else maxOf(0, currentCount - 1)

        // Optimistic update
        _isVouched.value = newState
        _event.value = eventData.copy(vouchCount = newCount)

        viewModelScope.launch {
            try {
                repository.toggleVouch(eventId)
            } catch (e: Exception) {
                // revert on error
                _isVouched.value = wasVouched
                _event.value = eventData.copy(vouchCount = currentCount)
            }
        }
    }

    /**
     * Report event to Firestore
     */
    fun reportEvent(eventId: String, reason: String, context: Context) {
        viewModelScope.launch {
            val result = repository.reportEvent(eventId, reason)
            if (result.isSuccess) {
                Toast.makeText(context, "Report submitted successfully", Toast.LENGTH_SHORT).show()
            } else {
                println("Failed to submit report: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}
