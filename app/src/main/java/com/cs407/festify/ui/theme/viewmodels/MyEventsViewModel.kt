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
import com.cs407.festify.data.repository.StorageRepository


@HiltViewModel
class MyEventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _myEvents = MutableStateFlow<List<Event>>(emptyList())
    val myEvents: StateFlow<List<Event>> = _myEvents

    private val _joinedEvents = MutableStateFlow<List<Event>>(emptyList())
    val joinedEvents: StateFlow<List<Event>> = _joinedEvents

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





    fun deleteEvent(event: Event) {
        viewModelScope.launch {

            _myEvents.value = _myEvents.value.filter { it.id != event.id }
            val result = eventRepository.deleteEvent(event.id)
            if (result.isFailure) {
                println("Error deleting event, rolling back UI change: ${result.exceptionOrNull()?.message}")
                _myEvents.value = (_myEvents.value + event).sortedByDescending { it.startDateTime }
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
                        val uploadResult = storageRepository.uploadEventImage(imageUri)
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

    fun updateEvent(
        eventId: String,
        title: String,
        description: String,
        location: String,
        startDateTime: Timestamp,
        maxAttendees: Int,
        tags: List<String>,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            println("--- STARTING UPDATE ---")
            try {
                // 1. Prepare the base updates (text fields)
                val updates = mutableMapOf<String, Any>(
                    "title" to title,
                    "description" to description,
                    "location" to location,
                    "startDateTime" to startDateTime,
                    "date" to SimpleDateFormat("MMM dd, yyyy", Locale.US).format(startDateTime.toDate()),
                    "time" to SimpleDateFormat("h:mm a", Locale.US).format(startDateTime.toDate()),
                    "maxAttendees" to maxAttendees,
                    "tags" to tags
                )

                // 2. Handle Image Upload (only if a NEW image was selected)
                if (imageUri != null) {
                    println("--- UPLOADING IMAGE... (This might take time) ---")
                    val uploadResult = storageRepository.uploadEventImage(imageUri)
                    if (uploadResult.isSuccess) {
                        val newImageUrl = uploadResult.getOrNull()
                        println("--- IMAGE UPLOAD SUCCESS! URL: $newImageUrl ---")
                        if (newImageUrl != null) {
                            updates["imageUrl"] = newImageUrl
                        }
                    } else {
                        println("Failed to upload new image: ${uploadResult.exceptionOrNull()?.message}")


                    }
                }

                println("--- UPDATING FIRESTORE DATABASE... ---")

                // 3. Send updates to Firestore
                val result = eventRepository.updateEvent(eventId, updates)

                if (result.isSuccess) {
                    println("Event updated successfully")
                    // Refresh the list to reflect changes
                    observeMyEvents()
                } else {
                    println("Update failed: ${result.exceptionOrNull()?.message}")
                    println("--- DATABASE ERROR: ${result.exceptionOrNull()?.message} ---")
                }
            } catch (e: Exception) {
                println("Exception updating event: ${e.message}")
            }
        }
    }

    fun cancelEvent(event: Event) {
        // Optimistic Update
        val updatedEvent = event.copy(status = "cancelled")
        _myEvents.value = _myEvents.value.map {
            if (it.id == event.id) updatedEvent else it
        }

        viewModelScope.launch {
            // We reuse your existing update logic!
            val updates = mapOf("status" to "cancelled")
            val result = eventRepository.updateEvent(event.id, updates)

            if (result.isFailure) {
                // Revert on failure
                observeMyEvents()
            }
        }
    }
    }
