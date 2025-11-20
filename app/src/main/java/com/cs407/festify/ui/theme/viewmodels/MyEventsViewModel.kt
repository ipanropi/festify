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
import android.net.Uri
import kotlin.jvm.optionals.getOrNull


@HiltViewModel
class MyEventsViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _myEvents = MutableStateFlow<List<Event>>(emptyList())
    val myEvents: StateFlow<List<Event>> = _myEvents

    companion object {
        private const val DEFAULT_EVENT_IMAGE_URL = "https://firebasestorage.googleapis.com/v0/b/festify-2ab07.firebasestorage.app/o/common-corporate-events.jpg?alt=media&token=0fdd3dd4-f512-4938-8784-e6a2d8ee4dce"
    }


    init {
        observeMyEvents()
    }

    private fun observeMyEvents() {
        viewModelScope.launch {
            eventRepository.getHostedEvents().collect { result ->
                if (result.isSuccess) {
                    _myEvents.value = result.getOrNull() ?: emptyList()
                } else {
                    println("Error observing my events: ${result.exceptionOrNull()?.message}")
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
            tags: List<String>,
            imageUri: Uri?
        ) {
            viewModelScope.launch {
                try {

                    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                    val timeFormatter = SimpleDateFormat("h:mm a", Locale.US)
                    val date = dateFormatter.format(startDateTime.toDate())
                    val time = timeFormatter.format(startDateTime.toDate())

                    val imageUrl = if (imageUri != null) {
                        // An image was selected, try to upload it.
                        val uploadResult = eventRepository.uploadEventImage(imageUri)
                        if (uploadResult.isSuccess) {
                            uploadResult.getOrNull() ?: DEFAULT_EVENT_IMAGE_URL // Use placeholder on strange null success
                        } else {
                            println("Error uploading image, using placeholder: ${uploadResult.exceptionOrNull()?.message}")
                            DEFAULT_EVENT_IMAGE_URL // Use placeholder on failure
                        }
                    } else {
                        // No image was selected by the user, use the placeholder directly.
                        DEFAULT_EVENT_IMAGE_URL
                    }

                    val newEvent = Event(
                        id = "",
                        title = title,
                        description = description,
                        location = location,
                        date = date,
                        time = time,
                        startDateTime = startDateTime,
                        endDateTime = null,
                        imageUrl = imageUrl,
                        attendees = 0,
                        maxAttendees = maxAttendees,
                        status = "upcoming",
                        userRsvp = "hosting",
                        isPublic = true,
                        category = "General",
                        tags = tags,
                    )

                    val result = eventRepository.createEvent(newEvent)

                    if (result.isSuccess) {
                        val newEventId = result.getOrNull()
                        if (newEventId != null) {
                            val confirmedEvent = newEvent.copy(id = newEventId)
                            _myEvents.value = listOf(confirmedEvent) + _myEvents.value
                    }
                    }
                    else {
                        println("Error creating event: ${result.exceptionOrNull()?.message}")
                    }

                } catch (e: Exception) {
                    println("Error creating event: ${e.message}")
                }
            }
        }
    }
