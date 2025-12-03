package com.cs407.festify.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Check-in data model for event attendance tracking
 */
data class CheckIn(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val eventId: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val deviceInfo: String = ""
)

/**
 * User's check-in summary for a specific event
 */
data class CheckInSummary(
    val eventId: String = "",
    val checkInCount: Int = 0,
    @ServerTimestamp
    val lastCheckInAt: Timestamp? = null,
    val allCheckIns: List<Timestamp> = emptyList()
)
