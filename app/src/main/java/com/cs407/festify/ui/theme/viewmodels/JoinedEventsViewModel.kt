package com.cs407.festify.ui.theme.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.festify.data.model.Event
import com.cs407.festify.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinedEventsViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _joinedEvents = MutableStateFlow<List<Event>>(emptyList())
    val joinedEvents: StateFlow<List<Event>> = _joinedEvents

    init {
        observeJoinedEvents()
    }

    private fun observeJoinedEvents() {
        viewModelScope.launch {
            // Call the new function from your repository
            eventRepository.getAttendingEventsDetails()
                .catch { e ->
                    // Handle any errors in the flow
                    println("Error observing joined events: ${e.message}")
                }
                .collect { result ->
                    if (result.isSuccess) {
                        _joinedEvents.value = result.getOrNull() ?: emptyList()
                    } else {
                        println("Failed to get joined events: ${result.exceptionOrNull()?.message}")
                    }
                }
        }
    }
}