package com.cs407.festify.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Event data model compatible with Firestore
 * All fields have default values for Firestore deserialization
 */
data class Event(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val attendees: Int = 0,
    val maxAttendees: Int = 0,
    val status: String = "upcoming", // "upcoming", "past", "cancelled"
    val userRsvp: String = "not_attending", // "attending", "maybe", "not_attending"
    val hostId: String = "",
    val hostName: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    val startDateTime: Timestamp? = null,
    val endDateTime: Timestamp? = null,
    val category: String = "",
    val isPublic: Boolean = true,
    val tags: List<String> = emptyList()
)

/**
 * User profile data model
 */
data class User(
    @DocumentId
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val initials: String = "",
    val eventsAttended: Int = 0,
    val eventsHosted: Int = 0,
    val rating: Double = 0.0,
    val upcomingEvents: Int = 0,
    val connections: Int = 0,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    val settings: UserSettings = UserSettings(),
    val profile: UserProfile = UserProfile()
)

/**
 * User settings
 */
data class UserSettings(
    val darkMode: Boolean = false,
    val pushNotifications: Boolean = true,
    val notifications: Boolean = true,
    val locationServices: Boolean = false
)

/**
 * User profile details
 */
data class UserProfile(
    val bio: String = "",
    val avatarUrl: String = "",
    val phoneNumber: String = ""
)

/**
 * Achievement data model
 */
data class Achievement(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val isNew: Boolean = false,
    val icon: String = "",
    val criteria: String = "",
    val points: Int = 0,
    val rarity: String = "common", // "common", "rare", "epic", "legendary"
    val unlockedAt: Timestamp? = null
)

/**
 * Message data model for event chats
 */
data class Message(
    @DocumentId
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val type: String = "text", // "text", "image", "system"
    val imageUrl: String = "",
    val isCurrentUser: Boolean = false
)

/**
 * Event chat preview model
 */
data class EventChat(
    @DocumentId
    val eventId: String = "",
    val eventName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null,
    val lastMessageSender: String = "",
    val participantCount: Int = 0,
    val participantIds: List<String> = emptyList(),
    val time: String = "" // Formatted time for UI
)

/**
 * RSVP data model
 */
data class Rsvp(
    @DocumentId
    val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val status: String = "not_attending", // "attending", "maybe", "not_attending"
    @ServerTimestamp
    val rsvpDate: Timestamp? = null
)

/**
 * Event attendee data model
 */
data class Attendee(
    @DocumentId
    val userId: String = "",
    val userName: String = "",
    val status: String = "attending", // "attending", "maybe"
    @ServerTimestamp
    val joinedAt: Timestamp? = null
)