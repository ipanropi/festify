package com.cs407.festify.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.festify.data.model.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.cs407.festify.data.repository.EventRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MyEventsViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _myEvents = MutableStateFlow<List<Event>>(emptyList())
    val myEvents: StateFlow<List<Event>> = _myEvents

    init {
        loadMyEvents()
    }

    private fun loadMyEvents() {

        viewModelScope.launch {
            eventRepository.getHostedEvents().collect { result ->
                if (result.isSuccess) {
                    _myEvents.value = result.getOrNull() ?: emptyList()
                } else {
                    println("Error loading events: ${result.exceptionOrNull()?.message}")
                    _myEvents.value = emptyList()
                }
            }
        }
    }

    fun createEvent(
        title: String,
        description: String,
        location: String,
        startDateTime: Timestamp,
        maxAttendees: Int,
        tags: List<String>
    ) {
        viewModelScope.launch {
            try {

                val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                val timeFormatter = SimpleDateFormat("h:mm a", Locale.US)
                val date = dateFormatter.format(startDateTime.toDate())
                val time = timeFormatter.format(startDateTime.toDate())

                val newEvent = Event(
                    id = "",
                    title = title,
                    description = description,
                    location = location,
                    date = date,
                    time = time,
                    startDateTime = startDateTime,
                    endDateTime = null,
                    imageUrl = "", // TODO: Get this from image upload
                    attendees = 0,
                    maxAttendees = maxAttendees,
                    status = "upcoming",
                    userRsvp = "hosting",
                    isPublic = true,
                    category = "General",
                    tags = tags
                )

                val result = eventRepository.createEvent(newEvent)

                if (result.isSuccess) {
                    println("Event created with ID: ${result.getOrNull()}")
                } else {
                    println("Error creating event: ${result.exceptionOrNull()?.message}")
                }

            } catch (e: Exception) {
                println("Error creating event: ${e.message}")
            }
        }
    }
}