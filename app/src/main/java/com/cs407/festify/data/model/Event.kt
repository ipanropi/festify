package com.cs407.festify.data.model

// This class defines what an "Event" is
data class Event(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String = "https://images.unsplash.com/photo-1551836022-4c4c79ecde51", // URL to the image
    val date: String,
    val time: String,
    val location: String,
    val attendees: Int,
    val maxAttendees: Int,
    val status: String,
    val userRsvp: String
)