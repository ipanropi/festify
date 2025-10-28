package com.cs407.festify.data.model

// This class defines what an "Event" is
data class Event(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String, // URL to the image
    val date: String,
    val time: String,
    val location: String,
    val attendees: Int,
    val maxAttendees: Int,
    val status: String,
    val userRsvp: String
)