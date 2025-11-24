package com.cs407.festify.ui.theme.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.festify.data.model.Event
import com.cs407.festify.data.remote.FirestoreCollections
import com.cs407.festify.data.repository.EventRepository
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

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            // 1. Load Event Data
            val result = repository.getEvent(eventId)
            if (result.isSuccess) {
                _event.value = result.getOrNull()
            }

            // 2. Start listening to the RSVP status
            repository.getUserRsvpStatus(eventId).collect { status ->
                _userRsvpStatus.value = status ?: "not_attending"
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
}