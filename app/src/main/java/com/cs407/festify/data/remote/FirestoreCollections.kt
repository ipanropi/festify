package com.cs407.festify.data.remote

/**
 * Centralized Firestore collection and field names
 * This ensures consistency across the app and makes refactoring easier
 */
object FirestoreCollections {

    // Root Collections
    const val USERS = "users"
    const val EVENTS = "events"
    const val CHATS = "chats"
    const val ACHIEVEMENTS = "achievements"

    // User Subcollections
    object User {
        const val ACHIEVEMENTS = "achievements"
        const val RSVPS = "rsvps"
        const val HOSTED_EVENTS = "hostedEvents"
    }

    // Event Subcollections
    object Event {
        const val ATTENDEES = "attendees"
        const val MESSAGES = "messages"
    }

    // Field Names
    object Fields {
        // Common
        const val ID = "id"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"

        // User Fields
        const val EMAIL = "email"
        const val NAME = "name"
        const val INITIALS = "initials"
        const val EVENTS_ATTENDED = "eventsAttended"
        const val EVENTS_HOSTED = "eventsHosted"
        const val RATING = "rating"
        const val UPCOMING_EVENTS = "upcomingEvents"
        const val CONNECTIONS = "connections"

        // Event Fields
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val IMAGE_URL = "imageUrl"
        const val DATE = "date"
        const val TIME = "time"
        const val LOCATION = "location"
        const val ATTENDEES = "attendees"
        const val MAX_ATTENDEES = "maxAttendees"
        const val STATUS = "status"
        const val HOST_ID = "hostId"
        const val HOST_NAME = "hostName"
        const val START_DATE_TIME = "startDateTime"
        const val END_DATE_TIME = "endDateTime"
        const val CATEGORY = "category"
        const val IS_PUBLIC = "isPublic"
        const val TAGS = "tags"

        // Message Fields
        const val SENDER_ID = "senderId"
        const val SENDER_NAME = "senderName"
        const val TEXT = "text"
        const val TIMESTAMP = "timestamp"
        const val MESSAGE_TYPE = "type"

        // RSVP Fields
        const val EVENT_ID = "eventId"
        const val USER_ID = "userId"
        const val RSVP_STATUS = "status"
        const val RSVP_DATE = "rsvpDate"

        // Chat Fields
        const val EVENT_NAME = "eventName"
        const val LAST_MESSAGE = "lastMessage"
        const val LAST_MESSAGE_TIME = "lastMessageTime"
        const val LAST_MESSAGE_SENDER = "lastMessageSender"
        const val PARTICIPANT_COUNT = "participantCount"
        const val PARTICIPANT_IDS = "participantIds"
    }

    // Status Values
    object Status {
        const val UPCOMING = "upcoming"
        const val PAST = "past"
        const val CANCELLED = "cancelled"
    }

    // RSVP Status Values
    object RsvpStatus {
        const val ATTENDING = "attending"
        const val MAYBE = "maybe"
        const val NOT_ATTENDING = "not_attending"
    }

    // Message Types
    object MessageType {
        const val TEXT = "text"
        const val IMAGE = "image"
        const val SYSTEM = "system"
    }
}
