package com.cs407.festify.data.repository

import com.cs407.festify.data.model.Achievement
import com.cs407.festify.data.model.Rsvp
import com.cs407.festify.data.model.User
import com.cs407.festify.data.remote.FirestoreCollections
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling user profile operations
 */
@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Get current user profile as a Flow (real-time updates)
     */
    fun getCurrentUserProfile(): Flow<Result<User>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(Result.failure(Exception("No user signed in")))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val user = snapshot?.toObject(User::class.java)
                if (user != null) {
                    trySend(Result.success(user))
                } else {
                    trySend(Result.failure(Exception("User profile not found")))
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get user profile by ID (one-time fetch)
     */
    suspend fun getUserProfile(userId: String): Result<User> {
        return try {
            val snapshot = firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .get()
                .await()

            val user = snapshot.toObject(User::class.java)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user profile information
     */
    suspend fun updateUserProfile(
        name: String? = null,
        bio: String? = null,
        phoneNumber: String? = null
    ): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            val updates = mutableMapOf<String, Any>(
                FirestoreCollections.Fields.UPDATED_AT to FieldValue.serverTimestamp()
            )

            name?.let {
                updates[FirestoreCollections.Fields.NAME] = it
                val initials = it.split(" ")
                    .mapNotNull { word -> word.firstOrNull()?.uppercaseChar() }
                    .joinToString("")
                    .take(2)
                updates[FirestoreCollections.Fields.INITIALS] = initials
            }

            bio?.let { updates["profile.bio"] = it }
            phoneNumber?.let { updates["profile.phoneNumber"] = it }

            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user settings
     */
    suspend fun updateUserSettings(
        darkMode: Boolean? = null,
        pushNotifications: Boolean? = null,
        notifications: Boolean? = null,
        locationServices: Boolean? = null
    ): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            val updates = mutableMapOf<String, Any>(
                FirestoreCollections.Fields.UPDATED_AT to FieldValue.serverTimestamp()
            )

            darkMode?.let { updates["settings.darkMode"] = it }
            pushNotifications?.let { updates["settings.pushNotifications"] = it }
            notifications?.let { updates["settings.notifications"] = it }
            locationServices?.let { updates["settings.locationServices"] = it }

            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user's achievements
     */
    fun getUserAchievements(): Flow<Result<List<Achievement>>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(Result.failure(Exception("No user signed in")))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.User.ACHIEVEMENTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val achievements = snapshot?.documents?.mapNotNull {
                    it.toObject(Achievement::class.java)
                } ?: emptyList()

                trySend(Result.success(achievements))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get user's RSVPs
     */
    fun getUserRsvps(): Flow<Result<List<Rsvp>>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(Result.failure(Exception("No user signed in")))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.User.RSVPS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val rsvps = snapshot?.documents?.mapNotNull {
                    it.toObject(Rsvp::class.java)
                } ?: emptyList()

                trySend(Result.success(rsvps))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Add or update an RSVP
     */
    suspend fun addRsvp(eventId: String, status: String): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            val rsvp = hashMapOf(
                FirestoreCollections.Fields.EVENT_ID to eventId,
                FirestoreCollections.Fields.RSVP_STATUS to status,
                FirestoreCollections.Fields.RSVP_DATE to FieldValue.serverTimestamp()
            )

            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection(FirestoreCollections.User.RSVPS)
                .document(eventId)
                .set(rsvp)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove an RSVP
     */
    suspend fun removeRsvp(eventId: String): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

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
     * Increment events attended count
     */
    suspend fun incrementEventsAttended(): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .update(
                    FirestoreCollections.Fields.EVENTS_ATTENDED, FieldValue.increment(1),
                    FirestoreCollections.Fields.UPDATED_AT, FieldValue.serverTimestamp()
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Increment events hosted count
     */
    suspend fun incrementEventsHosted(): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .update(
                    FirestoreCollections.Fields.EVENTS_HOSTED, FieldValue.increment(1),
                    FirestoreCollections.Fields.UPDATED_AT, FieldValue.serverTimestamp()
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update avatar URL
     */
    suspend fun updateAvatarUrl(avatarUrl: String): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .update(
                    "profile.avatarUrl", avatarUrl,
                    FirestoreCollections.Fields.UPDATED_AT, FieldValue.serverTimestamp()
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
