package com.cs407.festify.ui.theme.viewmodels

import androidx.lifecycle.ViewModel
import com.cs407.festify.data.model.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HomeScreenViewModel : ViewModel() {

    // StateFlow holding one or more hardcoded events
    private val _events = MutableStateFlow<List<Event>>(listOf(
        Event(
            id = "1",
            title = "Tech Startup Networking",
            description = "Connect with fellow entrepreneurs and innovators. Great opportunity to share ideas, find mentors, and build your startup network!",
            imageUrl = "https://images.unsplash.com/photo-1551836022-4c4c79ecde51",
            date = "Nov 5, 2025",
            time = "6:00 PM - 9:00 PM",
            location = "Innovation Hub, Downtown",
            attendees = 42,
            maxAttendees = 80,
            status = "upcoming",
            userRsvp = "attending"
        )
    ))

    // Expose read-only version to UI
    val events: StateFlow<List<Event>> = _events
}

