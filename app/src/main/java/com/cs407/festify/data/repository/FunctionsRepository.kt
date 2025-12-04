package com.cs407.festify.data.repository

import com.cs407.festify.data.model.Event
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for calling Firebase Cloud Functions
 */
@Singleton
class FunctionsRepository @Inject constructor(
    private val functions: FirebaseFunctions
) {

    /**
     * Search events using Cloud Function
     * More efficient than client-side filtering
     */
    suspend fun searchEvents(query: String, limit: Int = 20): Result<List<Event>> {
        return try {
            val data = hashMapOf(
                "query" to query,
                "limit" to limit
            )

            val result = functions
                .getHttpsCallable("searchEvents")
                .call(data)
                .await()

            // Parse result - use getData() instead of data property
            @Suppress("UNCHECKED_CAST")
            val resultData = result.getData() as? Map<String, Any>
            val events = resultData?.get("events") as? List<Map<String, Any>> ?: emptyList()

            // Convert to Event objects
            // Note: You may need to implement proper parsing based on your Event model
            val eventsList = events.mapNotNull { eventMap ->
                try {
                    parseEventFromMap(eventMap)
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(eventsList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get detailed event statistics
     */
    suspend fun getEventStats(eventId: String): Result<EventStats> {
        return try {
            val data = hashMapOf("eventId" to eventId)

            val result = functions
                .getHttpsCallable("getEventStats")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val resultData = result.getData() as? Map<String, Any>
                ?: return Result.failure(Exception("Invalid response"))

            val stats = EventStats(
                totalAttendees = (resultData["totalAttendees"] as? Number)?.toInt() ?: 0,
                attending = (resultData["attending"] as? Number)?.toInt() ?: 0,
                maybe = (resultData["maybe"] as? Number)?.toInt() ?: 0,
                totalMessages = (resultData["totalMessages"] as? Number)?.toInt() ?: 0
            )

            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a test notification (example callable function)
     * You can add this to your Cloud Functions
     */
    suspend fun sendTestNotification(userId: String, message: String): Result<Unit> {
        return try {
            val data = hashMapOf(
                "userId" to userId,
                "message" to message
            )

            functions
                .getHttpsCallable("sendTestNotification")
                .call(data)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Use emulator for local testing
     * Call this in debug builds before using functions
     */
    fun useEmulator(host: String = "10.0.2.2", port: Int = 5001) {
        functions.useEmulator(host, port)
    }

    /**
     * Helper function to parse Event from Map
     * Adjust based on your actual Event model structure
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseEventFromMap(map: Map<String, Any>): Event {
        return Event(
            id = map["id"] as? String ?: "",
            title = map["title"] as? String ?: "",
            description = map["description"] as? String ?: "",
            imageUrl = map["imageUrl"] as? String ?: "",
            date = map["date"] as? String ?: "",
            time = map["time"] as? String ?: "",
            location = map["location"] as? String ?: "",
            latitude = (map["latitude"] as? Number)?.toDouble(),
            longitude = (map["longitude"] as? Number)?.toDouble(),
            attendees = (map["attendees"] as? Number)?.toInt() ?: 0,
            maxAttendees = (map["maxAttendees"] as? Number)?.toInt() ?: 0,
            status = map["status"] as? String ?: "upcoming",
            userRsvp = map["userRsvp"] as? String ?: "not_attending",
            hostId = map["hostId"] as? String ?: "",
            hostName = map["hostName"] as? String ?: "",
            createdAt = null,
            updatedAt = null,
            startDateTime = null,
            endDateTime = null,
            category = map["category"] as? String ?: "",
            isPublic = map["isPublic"] as? Boolean ?: true,
            tags = (map["tags"] as? List<String>) ?: emptyList()
        )
    }
}

/**
 * Data class for event statistics
 */
data class EventStats(
    val totalAttendees: Int,
    val attending: Int,
    val maybe: Int,
    val totalMessages: Int
)
