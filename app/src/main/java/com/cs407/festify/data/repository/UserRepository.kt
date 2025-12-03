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

    // ============================================
    // FRIEND SYSTEM FUNCTIONS
    // ============================================

    /**
     * Send a friend request to another user
     */
    suspend fun sendFriendRequest(receiverId: String): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            if (userId == receiverId) {
                return Result.failure(Exception("Cannot send friend request to yourself"))
            }

            // Get current user's profile
            val senderProfile = getUserProfile(userId).getOrNull()
                ?: return Result.failure(Exception("Sender profile not found"))

            val requestId = "${userId}_${receiverId}"

            val friendRequest = hashMapOf(
                "id" to requestId,
                "senderId" to userId,
                "senderName" to senderProfile.name,
                "senderAvatarUrl" to senderProfile.profile.avatarUrl,
                "receiverId" to receiverId,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Add to receiver's friend requests
            firestore.collection(FirestoreCollections.USERS)
                .document(receiverId)
                .collection("friendRequests")
                .document(requestId)
                .set(friendRequest)
                .await()

            // Add to sender's sent requests
            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection("sentRequests")
                .document(requestId)
                .set(friendRequest)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Accept a friend request
     */
    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            // Get the friend request
            val requestDoc = firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection("friendRequests")
                .document(requestId)
                .get()
                .await()

            val senderId = requestDoc.getString("senderId")
                ?: return Result.failure(Exception("Invalid friend request"))

            // Update request status to accepted
            val updates = mapOf(
                "status" to "accepted",
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection("friendRequests")
                .document(requestId)
                .update(updates)
                .await()

            firestore.collection(FirestoreCollections.USERS)
                .document(senderId)
                .collection("sentRequests")
                .document(requestId)
                .update(updates)
                .await()

            // Add to both users' friends list
            val friendData = mapOf(
                "userId" to senderId,
                "addedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection("friends")
                .document(senderId)
                .set(friendData)
                .await()

            val currentUserFriendData = mapOf(
                "userId" to userId,
                "addedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(FirestoreCollections.USERS)
                .document(senderId)
                .collection("friends")
                .document(userId)
                .set(currentUserFriendData)
                .await()

            // Increment connections count for both users
            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .update(FirestoreCollections.Fields.CONNECTIONS, FieldValue.increment(1))
                .await()

            firestore.collection(FirestoreCollections.USERS)
                .document(senderId)
                .update(FirestoreCollections.Fields.CONNECTIONS, FieldValue.increment(1))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Decline a friend request
     */
    suspend fun declineFriendRequest(requestId: String): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            // Get the friend request
            val requestDoc = firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection("friendRequests")
                .document(requestId)
                .get()
                .await()

            val senderId = requestDoc.getString("senderId")
                ?: return Result.failure(Exception("Invalid friend request"))

            // Delete the request from both users
            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection("friendRequests")
                .document(requestId)
                .delete()
                .await()

            firestore.collection(FirestoreCollections.USERS)
                .document(senderId)
                .collection("sentRequests")
                .document(requestId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get friend requests (received)
     */
    fun getFriendRequests(): Flow<Result<List<com.cs407.festify.data.model.FriendRequest>>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(Result.failure(Exception("No user signed in")))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection("friendRequests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull {
                    it.toObject(com.cs407.festify.data.model.FriendRequest::class.java)
                } ?: emptyList()

                trySend(Result.success(requests))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Check friendship status with another user
     */
    suspend fun checkFriendshipStatus(otherUserId: String): Result<String> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            if (userId == otherUserId) {
                return Result.success("self")
            }

            // Check if already friends
            val friendDoc = firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection("friends")
                .document(otherUserId)
                .get()
                .await()

            if (friendDoc.exists()) {
                return Result.success("friends")
            }

            // Check if request already sent
            val sentRequestId = "${userId}_${otherUserId}"
            val sentRequestDoc = firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection("sentRequests")
                .document(sentRequestId)
                .get()
                .await()

            if (sentRequestDoc.exists()) {
                return Result.success("request_sent")
            }

            // Check if received request from this user
            val receivedRequestId = "${otherUserId}_${userId}"
            val receivedRequestDoc = firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection("friendRequests")
                .document(receivedRequestId)
                .get()
                .await()

            if (receivedRequestDoc.exists()) {
                return Result.success("request_received")
            }

            Result.success("none")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove a friend
     */
    suspend fun removeFriend(friendId: String): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            // Remove from both users' friends list
            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .collection("friends")
                .document(friendId)
                .delete()
                .await()

            firestore.collection(FirestoreCollections.USERS)
                .document(friendId)
                .collection("friends")
                .document(userId)
                .delete()
                .await()

            // Decrement connections count for both users
            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .update(FirestoreCollections.Fields.CONNECTIONS, FieldValue.increment(-1))
                .await()

            firestore.collection(FirestoreCollections.USERS)
                .document(friendId)
                .update(FirestoreCollections.Fields.CONNECTIONS, FieldValue.increment(-1))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
