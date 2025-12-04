package com.cs407.festify.ui.theme.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.festify.data.model.Event
import com.cs407.festify.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredEvents: StateFlow<List<Event>> = combine(_events, _searchQuery) { events, query ->
        if (query.isBlank()) events
        else events.filter { it.title.contains(query, ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        observeEvents()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            eventRepository.getUpcomingEvents()
                .collect { result ->
                    if (result.isSuccess) {
                        _events.value = result.getOrNull() ?: emptyList()
                    }
                }
        }
    }



    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }


    /**
     * Process a report and delete the event
     */
    fun processReportAndDelete(eventId: String) {
        viewModelScope.launch {
            // 1. Remove the event
            val deleteResult = eventRepository.adminDeleteEvent(eventId)

            if (deleteResult.isSuccess) {

            }
        }
    }
}



