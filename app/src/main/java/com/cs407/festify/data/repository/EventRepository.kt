package com.cs407.festify.data.repository

import com.cs407.festify.data.model.Attendee
import com.cs407.festify.data.model.Event
import com.cs407.festify.data.remote.FirestoreCollections
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling event operations
 */
@Singleton
class EventRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val currentUserName: String?
        get() = auth.currentUser?.displayName

    /**
     * Real-time stream of upcoming events
     * Shows ALL PUBLIC events, regardless of startDateTime
     */
    fun getUpcomingEvents(): Flow<Result<List<Event>>> = callbackFlow {
        val listener = firestore.collection(FirestoreCollections.EVENTS)
            .orderBy(FirestoreCollections.Fields.CREATED_AT, Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull {
                    it.toObject(Event::class.java)
                } ?: emptyList()

                trySend(Result.success(events))
            }

        awaitClose { listener.remove() }
    }


    /**
     * Get events hosted by current user
     */
    fun getHostedEvents(): Flow<Result<List<Event>>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(Result.failure(Exception("No user signed in")))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(FirestoreCollections.EVENTS)
            .whereEqualTo(FirestoreCollections.Fields.HOST_ID, userId)
            .orderBy(FirestoreCollections.Fields.START_DATE_TIME, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull {
                    it.toObject(Event::class.java)
                } ?: emptyList()

                trySend(Result.success(events))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get events user is attending
     */
    fun getAttendingEvents(userId: String? = currentUserId): Flow<Result<List<String>>> = callbackFlow {
        val uid = userId ?: run {
            trySend(Result.failure(Exception("No user ID provided")))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(FirestoreCollections.USERS)
            .document(uid)
            .collection(FirestoreCollections.User.RSVPS)
            .whereEqualTo(FirestoreCollections.Fields.RSVP_STATUS, FirestoreCollections.RsvpStatus.ATTENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val eventIds = snapshot?.documents?.mapNotNull {
                    it.getString(FirestoreCollections.Fields.EVENT_ID)
                } ?: emptyList()

                trySend(Result.success(eventIds))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get a single event by ID
     */
    suspend fun getEvent(eventId: String): Result<Event> {
        return try {
            val snapshot = firestore.collection(FirestoreCollections.EVENTS)
                .document(eventId)
                .get()
                .await()

            val event = snapshot.toObject(Event::class.java)
            if (event != null) {
                Result.success(event)
            } else {
                Result.failure(Exception("Event not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new event
     */
    suspend fun createEvent(event: Event): Result<String> {
        return try {
            //  TESTING CODE
            val userId = currentUserId ?: "TESTING_USER_ID" // Use a test ID for testing

            val userName = currentUserName ?: "Unknown"

            val eventData = hashMapOf(
                FirestoreCollections.Fields.TITLE to event.title,
                FirestoreCollections.Fields.DESCRIPTION to event.description,
                FirestoreCollections.Fields.IMAGE_URL to event.imageUrl,
                FirestoreCollections.Fields.DATE to event.date,
                FirestoreCollections.Fields.TIME to event.time,
                FirestoreCollections.Fields.LOCATION to event.location,
                FirestoreCollections.Fields.ATTENDEES to 0,
                FirestoreCollections.Fields.MAX_ATTENDEES to event.maxAttendees,
                FirestoreCollections.Fields.STATUS to FirestoreCollections.Status.UPCOMING,
                FirestoreCollections.Fields.HOST_ID to userId,
                FirestoreCollections.Fields.HOST_NAME to userName,
                FirestoreCollections.Fields.CREATED_AT to FieldValue.serverTimestamp(),
                FirestoreCollections.Fields.UPDATED_AT to FieldValue.serverTimestamp(),
                FirestoreCollections.Fields.START_DATE_TIME to event.startDateTime,
                FirestoreCollections.Fields.END_DATE_TIME to event.endDateTime,
                FirestoreCollections.Fields.CATEGORY to event.category,
                FirestoreCollections.Fields.IS_PUBLIC to event.isPublic,
                FirestoreCollections.Fields.TAGS to event.tags
            )

            val docRef = firestore.collection(FirestoreCollections.EVENTS)
                .add(eventData)
                .await()

            // Add to user's hosted events
            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection(FirestoreCollections.User.HOSTED_EVENTS)
                .document(docRef.id)
                .set(
                    hashMapOf(
                        FirestoreCollections.Fields.EVENT_ID to docRef.id,
                        "addedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing event
     */
    suspend fun updateEvent(eventId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val event = getEvent(eventId).getOrNull()
                ?: return Result.failure(Exception("Event not found"))

            // Check if current user is the host
            if (event.hostId != currentUserId) {
                return Result.failure(Exception("Only the host can update the event"))
            }

            val updateMap = updates.toMutableMap()
            updateMap[FirestoreCollections.Fields.UPDATED_AT] = FieldValue.serverTimestamp()

            firestore.collection(FirestoreCollections.EVENTS)
                .document(eventId)
                .update(updateMap)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete an event
     */
    suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            val event = getEvent(eventId).getOrNull()
                ?: return Result.failure(Exception("Event not found"))

            // Check if current user is the host
            if (event.hostId != currentUserId) {
                return Result.failure(Exception("Only the host can delete the event"))
            }

            // Delete event document
            firestore.collection(FirestoreCollections.EVENTS)
                .document(eventId)
                .delete()
                .await()

            // Remove from user's hosted events
            currentUserId?.let { userId ->
                firestore.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .collection(FirestoreCollections.User.HOSTED_EVENTS)
                    .document(eventId)
                    .delete()
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * RSVP to an event
     */
    suspend fun rsvpToEvent(eventId: String, status: String): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            val userName = currentUserName ?: "Unknown"

            // Add/update attendee in event
            val attendeeData = hashMapOf(
                FirestoreCollections.Fields.USER_ID to userId,
                "userName" to userName,
                FirestoreCollections.Fields.RSVP_STATUS to status,
                "joinedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(FirestoreCollections.EVENTS)
                .document(eventId)
                .collection(FirestoreCollections.Event.ATTENDEES)
                .document(userId)
                .set(attendeeData)
                .await()

            // Update attendee count if status is "attending"
            if (status == FirestoreCollections.RsvpStatus.ATTENDING) {
                firestore.collection(FirestoreCollections.EVENTS)
                    .document(eventId)
                    .update(FirestoreCollections.Fields.ATTENDEES, FieldValue.increment(1))
                    .await()
            }

            // Add RSVP to user's profile
            val rsvpData = hashMapOf(
                FirestoreCollections.Fields.EVENT_ID to eventId,
                FirestoreCollections.Fields.RSVP_STATUS to status,
                FirestoreCollections.Fields.RSVP_DATE to FieldValue.serverTimestamp()
            )

            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection(FirestoreCollections.User.RSVPS)
                .document(eventId)
                .set(rsvpData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancel RSVP to an event
     */
    suspend fun cancelRsvp(eventId: String): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            // Get current RSVP status
            val rsvpDoc = firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection(FirestoreCollections.User.RSVPS)
                .document(eventId)
                .get()
                .await()

            val currentStatus = rsvpDoc.getString(FirestoreCollections.Fields.RSVP_STATUS)

            // Remove attendee from event
            firestore.collection(FirestoreCollections.EVENTS)
                .document(eventId)
                .collection(FirestoreCollections.Event.ATTENDEES)
                .document(userId)
                .delete()
                .await()

            // Decrement attendee count if was attending
            if (currentStatus == FirestoreCollections.RsvpStatus.ATTENDING) {
                firestore.collection(FirestoreCollections.EVENTS)
                    .document(eventId)
                    .update(FirestoreCollections.Fields.ATTENDEES, FieldValue.increment(-1))
                    .await()
            }

            // Remove RSVP from user's profile
            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection(FirestoreCollections.User.RSVPS)
                .document(eventId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get event attendees
     */
    fun getEventAttendees(eventId: String): Flow<Result<List<Attendee>>> = callbackFlow {
        val listener = firestore.collection(FirestoreCollections.EVENTS)
            .document(eventId)
            .collection(FirestoreCollections.Event.ATTENDEES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val attendees = snapshot?.documents?.mapNotNull {
                    it.toObject(Attendee::class.java)
                } ?: emptyList()

                trySend(Result.success(attendees))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Search events by title or tags
     */
    suspend fun searchEvents(query: String): Result<List<Event>> {
        return try {
            val lowercaseQuery = query.lowercase()

            // Search by tags (this works well with array-contains)
            val snapshot = firestore.collection(FirestoreCollections.EVENTS)
                .whereEqualTo(FirestoreCollections.Fields.IS_PUBLIC, true)
                .whereArrayContains(FirestoreCollections.Fields.TAGS, lowercaseQuery)
                .limit(20)
                .get()
                .await()

            val events = snapshot.documents.mapNotNull {
                it.toObject(Event::class.java)
            }

            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
