package com.cs407.festify.ui.theme.viewmodels

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
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
    //val context = LocalContext.current



    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _userRsvpStatus = MutableStateFlow<String>("not_attending")
    val userRsvpStatus: StateFlow<String> = _userRsvpStatus.asStateFlow()

    private val _isVouched = MutableStateFlow<Boolean?>(null)
    val isVouched: StateFlow<Boolean?> = _isVouched.asStateFlow()

    fun loadEvent(eventId: String) {

        viewModelScope.launch {
            val result = repository.getEvent(eventId)
            if (result.isSuccess) _event.value = result.getOrNull()
        }


        viewModelScope.launch {
            repository.getUserRsvpStatus(eventId).collect { status ->
                _userRsvpStatus.value = status ?: "not_attending"
            }
        }


        viewModelScope.launch {
            repository.hasUserVouched(eventId).collect { vouched ->
                _isVouched.value = vouched
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
        val wasVouched = _isVouched.value ?: return

        val currentEvent = _event.value ?: return
        val currentCount = currentEvent.vouchCount

        val newVouchedState = !wasVouched
        val newCount = if (newVouchedState) currentCount + 1 else maxOf(0, currentCount - 1)

        // 4. Optimistic Update
        _isVouched.value = newVouchedState
        _event.value = currentEvent.copy(vouchCount = newCount)

        viewModelScope.launch {
            try {
                repository.toggleVouch(eventId)
            } catch (e: Exception) {
                // Revert on error
                _isVouched.value = wasVouched
                _event.value = currentEvent.copy(vouchCount = currentCount)
            }
        }
    }

    fun reportEvent(eventId: String, reason: String, context: Context) {
        viewModelScope.launch {
            val result = repository.reportEvent(eventId, reason)
            if (result.isSuccess) {
                println("Report submitted successfully")
                Toast.makeText(context, "Report submitted successfully", Toast.LENGTH_SHORT).show()
            } else {
                println("Failed to submit report: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    }
