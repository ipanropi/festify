package com.cs407.festify.data.repository

import com.cs407.festify.data.model.Attendee
import com.cs407.festify.data.model.CheckIn
import com.cs407.festify.data.model.Event
import com.cs407.festify.data.remote.FirestoreCollections
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
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

                println(">>> HOSTED EVENTS LISTENER FIRED: Found ${snapshot?.size()} documents")

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
            // FIX: Use 'it.id' to get the document name (which is the Event ID)
            it.id
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
            val userId = currentUserId
                ?: return Result.failure(Exception("Cannot create event: User not signed in."))

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
                // FirestoreCollections.Fields.EVENT_ID to eventId,
                FirestoreCollections.Fields.RSVP_STATUS to status,
                FirestoreCollections.Fields.RSVP_DATE to FieldValue.serverTimestamp()
            )

            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection(FirestoreCollections.User.RSVPS)
                .document(eventId)
                .set(rsvpData)
                .await()

            // Update user's upcomingEvents count if status is "attending"
            if (status == FirestoreCollections.RsvpStatus.ATTENDING) {
                firestore.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .update(FirestoreCollections.Fields.UPCOMING_EVENTS, FieldValue.increment(1))
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserRsvpStatus(eventId: String): Flow<String?> = callbackFlow {
        val userId = currentUserId ?: run {
            trySend(null)
            close()
            return@callbackFlow
        }

        // Go to: Users -> [MyID] -> RSVPs -> [EventID]
        val listener = firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.User.RSVPS)
            .document(eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // If we found a document, send the status (e.g. "attending")
                    val status = snapshot.getString(FirestoreCollections.Fields.RSVP_STATUS)
                    trySend(status)
                } else {
                    // If no document exists, I am not attending
                    trySend("not_attending")
                }
            }

        awaitClose { listener.remove() }
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

            // Decrement user's upcomingEvents count if was attending
            if (currentStatus == FirestoreCollections.RsvpStatus.ATTENDING) {
                firestore.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .update(FirestoreCollections.Fields.UPCOMING_EVENTS, FieldValue.increment(-1))
                    .await()
            }

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
     * Get the full Event objects for events the user is attending.
     * This function CHAINS together two operations:
     * 1. It calls `getAttendingEvents()` to get the list of event IDs.
     * 2. It then uses those IDs to fetch the full Event details.
     */
    fun getAttendingEventsDetails(): Flow<Result<List<Event>>> {
        return getAttendingEvents().flatMapLatest { idResult ->

            if (idResult.isFailure) {
                return@flatMapLatest callbackFlow {
                    trySend(Result.failure(idResult.exceptionOrNull()!!))
                    close()
                }
            }

            val eventIds = idResult.getOrNull()

            if (eventIds.isNullOrEmpty()) {
                return@flatMapLatest callbackFlow {
                    trySend(Result.success(emptyList()))
                    close()
                }
            }

            callbackFlow {
                val listener = firestore.collection(FirestoreCollections.EVENTS)
                    .whereIn(FieldPath.documentId(), eventIds)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(Result.failure(error))
                            return@addSnapshotListener
                        }

                        // Convert the documents to Event objects, same as in your other functions.
                        val events = snapshot?.documents?.mapNotNull {
                            it.toObject(Event::class.java)
                        } ?: emptyList()

                        trySend(Result.success(events))
                    }

                // Clean up the listener when the flow is cancelled.
                awaitClose { listener.remove() }
            }
        }
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


    // Check if current user has vouched for this event
    fun hasUserVouched(eventId: String): Flow<Boolean> = callbackFlow {
        val userId = currentUserId ?: run { trySend(false); close(); return@callbackFlow }

        val docRef = firestore.collection(FirestoreCollections.EVENTS)
            .document(eventId)
            .collection("vouches") // Sub-collection to track who vouched
            .document(userId)

        val listener = docRef.addSnapshotListener { snapshot, _ ->
            trySend(snapshot != null && snapshot.exists())
        }
        awaitClose { listener.remove() }
    }

    // Toggle Vouch (Like/Unlike)
    suspend fun toggleVouch(eventId: String) {
        val userId = currentUserId ?: return
        val eventRef = firestore.collection(FirestoreCollections.EVENTS).document(eventId)
        val vouchRef = eventRef.collection("vouches").document(userId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(vouchRef)

            if (snapshot.exists()) {
                // User already vouched -> Remove vouch (Unlike)
                transaction.delete(vouchRef)
                transaction.update(eventRef, "vouchCount", FieldValue.increment(-1))
            } else {
                // New vouch -> Add vouch (Like)
                transaction.set(vouchRef, mapOf("timestamp" to FieldValue.serverTimestamp()))
                transaction.update(eventRef, "vouchCount", FieldValue.increment(1))
            }
        }.await()
    }

    /**
     * Check in to an event - allows multiple check-ins
     * @param eventId The event ID to check in to
     * @return Result indicating success or failure
     */
    suspend fun checkInToEvent(eventId: String): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("No user signed in"))
            val userName = currentUserName ?: "Unknown"

            // Check if last check-in was less than 30 seconds ago (rate limiting)
            val userCheckInRef = firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection(FirestoreCollections.User.CHECK_INS)
                .document(eventId)
                .get()
                .await()

            if (userCheckInRef.exists()) {
                val lastCheckIn = userCheckInRef.getTimestamp(FirestoreCollections.Fields.LAST_CHECK_IN_AT)
                if (lastCheckIn != null) {
                    val timeSinceLastCheckIn = System.currentTimeMillis() - lastCheckIn.toDate().time
                    if (timeSinceLastCheckIn < 30_000) { // 30 seconds
                        return Result.failure(Exception("Please wait before checking in again"))
                    }
                }
            }

            // Get device info
            val deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

            // Create check-in document
            val checkInData = hashMapOf(
                FirestoreCollections.Fields.USER_ID to userId,
                "userName" to userName,
                FirestoreCollections.Fields.EVENT_ID to eventId,
                FirestoreCollections.Fields.TIMESTAMP to FieldValue.serverTimestamp(),
                FirestoreCollections.Fields.DEVICE_INFO to deviceInfo
            )

            // Add to events/{eventId}/checkIns collection
            firestore.collection(FirestoreCollections.EVENTS)
                .document(eventId)
                .collection(FirestoreCollections.Event.CHECK_INS)
                .add(checkInData)
                .await()

            // Update event's total check-in count
            firestore.collection(FirestoreCollections.EVENTS)
                .document(eventId)
                .update(FirestoreCollections.Fields.TOTAL_CHECK_INS, FieldValue.increment(1))
                .await()

            // Update user's check-in tracking
            val isFirstCheckIn = updateUserCheckInTracking(userId, eventId)

            // Increment eventsAttended on first check-in only
            if (isFirstCheckIn) {
                firestore.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .update(FirestoreCollections.Fields.EVENTS_ATTENDED, FieldValue.increment(1))
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Report an event for review
     * @param eventId The event ID to report
     * @param reason The reason for reporting
     * @return Result indicating success or failure
     */
    suspend fun reportEvent(eventId: String, reason: String): Result<Unit> {
        return try {
            val report = hashMapOf(
                "eventId" to eventId,
                "reporterId" to (auth.currentUser?.uid ?: "anonymous"),
                "reason" to reason,
                "timestamp" to FieldValue.serverTimestamp(),
                "status" to "pending" // Admins can filter by this later
            )

            // Create a new collection called "reports" automatically
            firestore.collection("reports").add(report).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user's check-in tracking for an event
     * @return true if this was the first check-in, false otherwise
     */
    private suspend fun updateUserCheckInTracking(userId: String, eventId: String): Boolean {
        val userCheckInRef = firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.User.CHECK_INS)
            .document(eventId)

        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userCheckInRef)

            if (snapshot.exists()) {
                // Increment count
                transaction.update(
                    userCheckInRef,
                    FirestoreCollections.Fields.CHECK_IN_COUNT, FieldValue.increment(1),
                    FirestoreCollections.Fields.LAST_CHECK_IN_AT, FieldValue.serverTimestamp()
                )
                false // Not first check-in
            } else {
                // Create first check-in
                transaction.set(userCheckInRef, hashMapOf(
                    FirestoreCollections.Fields.EVENT_ID to eventId,
                    FirestoreCollections.Fields.CHECK_IN_COUNT to 1,
                    FirestoreCollections.Fields.LAST_CHECK_IN_AT to FieldValue.serverTimestamp()
                ))
                true // First check-in
            }
        }.await()
    }

    /**
     * Get all check-ins for an event (for hosts)
     * @param eventId The event ID
     * @return Flow of check-ins list
     */
    fun getEventCheckIns(eventId: String): Flow<Result<List<CheckIn>>> = callbackFlow {
        val listener = firestore.collection(FirestoreCollections.EVENTS)
            .document(eventId)
            .collection(FirestoreCollections.Event.CHECK_INS)
            .orderBy(FirestoreCollections.Fields.TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val checkIns = snapshot?.documents?.mapNotNull {
                    it.toObject(CheckIn::class.java)
                } ?: emptyList()

                trySend(Result.success(checkIns))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get user's check-in status for an event
     * @param eventId The event ID
     * @return Flow of check-in status
     */
    fun getUserCheckInStatus(eventId: String): Flow<CheckInStatus> = callbackFlow {
        val userId = currentUserId ?: run {
            trySend(CheckInStatus.NotCheckedIn)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.User.CHECK_INS)
            .document(eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(CheckInStatus.NotCheckedIn)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val count = snapshot.getLong(FirestoreCollections.Fields.CHECK_IN_COUNT)?.toInt() ?: 0
                    val lastCheckIn = snapshot.getTimestamp(FirestoreCollections.Fields.LAST_CHECK_IN_AT)
                    trySend(CheckInStatus.CheckedIn(count, lastCheckIn))
                } else {
                    trySend(CheckInStatus.NotCheckedIn)
                }
            }

        awaitClose { listener.remove() }
    }

}

/**
 * Check-in status for a user and event
 */
sealed class CheckInStatus {
    object NotCheckedIn : CheckInStatus()
    data class CheckedIn(val count: Int, val lastCheckInAt: Timestamp?) : CheckInStatus()
}


