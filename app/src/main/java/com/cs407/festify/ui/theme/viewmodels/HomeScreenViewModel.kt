package com.cs407.festify.ui.theme.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.festify.data.model.Event
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeScreenViewModel : ViewModel() {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    private var currentPage = 0
    private val pageSize = 5

    init {
        loadMoreEvents()
    }

    fun loadMoreEvents() {
        viewModelScope.launch {
            delay(1000) // simulate network delay

            val newEvents = (1..pageSize).map { index ->
                val id = (currentPage * pageSize + index).toString()
                Event(
                    id = id,
                    title = "Community Art Workshop #$id",
                    description = "Explore creative expression and learn art techniques with local artists. Open to all skill levels!",
                    imageUrl = "https://images.unsplash.com/photo-1503264116251-35a269479413",
                    date = "Oct ${10 + index}, 2024",
                    time = "10:00 AM - 3:00 PM",
                    location = "Community Center Hall ${index}",
                    attendees = 25 + index,
                    maxAttendees = 50,
                    status = if (index % 2 == 0) "upcoming" else "maybe",
                    userRsvp = if (index % 3 == 0) "attending" else "none"
                )
            }

            _events.value = _events.value + newEvents
            currentPage++
        }
    }
}
