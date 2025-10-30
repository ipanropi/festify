package com.cs407.festify.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.cs407.festify.data.model.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MyEventsViewModel : ViewModel() {

    // This holds the list of events for the UI
    private val _myEvents = MutableStateFlow<List<Event>>(emptyList())
    val myEvents: StateFlow<List<Event>> = _myEvents

    init {
        // Load some placeholder data when the ViewModel is created
        loadMyEvents()
    }

    private fun loadMyEvents() {
        // mock value, for now
        _myEvents.value = listOf(
            Event(
                id = "1",
                title = "Tech Startup Networking",
                description = "Connect with fellow entrepreneurs and tech enthusiasts. Great opportunity to share ideas, find co-founders, and discover new opportunities in the startup ecosystem.",
                imageUrl = "https://example.com", // placeholder for now, will find light picture later
                date = "Today, Oct 7",
                time = "6:00 PM - 9:00 PM",
                location = "Innovation Hub, Downtown",
                attendees = 42,
                maxAttendees = 60,
                status = "upcoming",
                userRsvp = "attending"
            )

        )
    }
}