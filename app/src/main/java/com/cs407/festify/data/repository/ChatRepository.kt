package com.cs407.festify.data.repository

import com.cs407.festify.data.model.EventChat
import com.cs407.festify.data.model.Message
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
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling chat and messaging operations
 */
@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val currentUserName: String?
        get() = auth.currentUser?.displayName

    /**
     * Get all event chats for current user
     * Returns chats for events the user is attending
     */
    fun getUserEventChats(): Flow<Result<List<EventChat>>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(Result.failure(Exception("No user signed in")))
            close()
            return@callbackFlow
        }

        // First, get the events user is attending
        val rsvpListener = firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.User.RSVPS)
            .whereEqualTo(FirestoreCollections.Fields.RSVP_STATUS, FirestoreCollections.RsvpStatus.ATTENDING)
            .addSnapshotListener { rsvpSnapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val eventIds = rsvpSnapshot?.documents?.mapNotNull {
                    it.getString(FirestoreCollections.Fields.EVENT_ID)
                } ?: emptyList()

                if (eventIds.isEmpty()) {
                    trySend(Result.success(emptyList()))
                    return@addSnapshotListener
                }

                // Get chat data for these events
                firestore.collection(FirestoreCollections.CHATS)
                    .whereIn(FirestoreCollections.Fields.EVENT_ID, eventIds.take(10)) // Firestore limit
                    .orderBy(FirestoreCollections.Fields.LAST_MESSAGE_TIME, Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { chatSnapshot ->
                        val chats = chatSnapshot.documents.mapNotNull {
                            val chat = it.toObject(EventChat::class.java)
                            // Format time for UI
                            chat?.copy(
                                time = formatTime(chat.lastMessageTime)
                            )
                        }
                        trySend(Result.success(chats))
                    }
                    .addOnFailureListener { exception ->
                        trySend(Result.failure(exception))
                    }
            }

        awaitClose { rsvpListener.remove() }
    }

    /**
     * Get messages for a specific event chat
     */
    fun getEventMessages(eventId: String): Flow<Result<List<Message>>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(Result.failure(Exception("No user signed in")))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(FirestoreCollections.EVENTS)
            .document(eventId)
            .collection(FirestoreCollections.Event.MESSAGES)
            .orderBy(FirestoreCollections.Fields.TIMESTAMP, Query.Direction.ASCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull {
                    val message = it.toObject(Message::class.java)
                    message?.copy(isCurrentUser = it.getString(FirestoreCollections.Fields.SENDER_ID) == userId)
                } ?: emptyList()

                trySend(Result.success(messages))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Send a text message to an event chat
     */
    suspend fun sendMessage(eventId: String, text: String): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            val userName = currentUserName ?: "Unknown"

            // Add message to event's messages subcollection
            val messageData = hashMapOf(
                FirestoreCollections.Fields.SENDER_ID to userId,
                FirestoreCollections.Fields.SENDER_NAME to userName,
                FirestoreCollections.Fields.TEXT to text,
                FirestoreCollections.Fields.TIMESTAMP to FieldValue.serverTimestamp(),
                FirestoreCollections.Fields.MESSAGE_TYPE to FirestoreCollections.MessageType.TEXT
            )

            firestore.collection(FirestoreCollections.EVENTS)
                .document(eventId)
                .collection(FirestoreCollections.Event.MESSAGES)
                .add(messageData)
                .await()

            // Update chat preview
            updateChatPreview(eventId, text, userName)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send an image message to an event chat
     */
    suspend fun sendImageMessage(eventId: String, imageUrl: String, caption: String = ""): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            val userName = currentUserName ?: "Unknown"

            val messageData = hashMapOf(
                FirestoreCollections.Fields.SENDER_ID to userId,
                FirestoreCollections.Fields.SENDER_NAME to userName,
                FirestoreCollections.Fields.TEXT to caption,
                FirestoreCollections.Fields.TIMESTAMP to FieldValue.serverTimestamp(),
                FirestoreCollections.Fields.MESSAGE_TYPE to FirestoreCollections.MessageType.IMAGE,
                "imageUrl" to imageUrl
            )

            firestore.collection(FirestoreCollections.EVENTS)
                .document(eventId)
                .collection(FirestoreCollections.Event.MESSAGES)
                .add(messageData)
                .await()

            // Update chat preview
            val previewText = if (caption.isNotEmpty()) caption else "Sent an image"
            updateChatPreview(eventId, previewText, userName)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a system message (e.g., "User joined the event")
     */
    suspend fun sendSystemMessage(eventId: String, text: String): Result<Unit> {
        return try {
            val messageData = hashMapOf(
                FirestoreCollections.Fields.SENDER_ID to "system",
                FirestoreCollections.Fields.SENDER_NAME to "System",
                FirestoreCollections.Fields.TEXT to text,
                FirestoreCollections.Fields.TIMESTAMP to FieldValue.serverTimestamp(),
                FirestoreCollections.Fields.MESSAGE_TYPE to FirestoreCollections.MessageType.SYSTEM
            )

            firestore.collection(FirestoreCollections.EVENTS)
                .document(eventId)
                .collection(FirestoreCollections.Event.MESSAGES)
                .add(messageData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a message (only sender or event host can delete)
     */
    suspend fun deleteMessage(eventId: String, messageId: String): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            // Get the message to check sender
            val messageDoc = firestore.collection(FirestoreCollections.EVENTS)
                .document(eventId)
                .collection(FirestoreCollections.Event.MESSAGES)
                .document(messageId)
                .get()
                .await()

            val senderId = messageDoc.getString(FirestoreCollections.Fields.SENDER_ID)

            // Get event to check host
            val eventDoc = firestore.collection(FirestoreCollections.EVENTS)
                .document(eventId)
                .get()
                .await()

            val hostId = eventDoc.getString(FirestoreCollections.Fields.HOST_ID)

            // Check if user is sender or host
            if (senderId == userId || hostId == userId) {
                firestore.collection(FirestoreCollections.EVENTS)
                    .document(eventId)
                    .collection(FirestoreCollections.Event.MESSAGES)
                    .document(messageId)
                    .delete()
                    .await()

                Result.success(Unit)
            } else {
                Result.failure(Exception("You don't have permission to delete this message"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Initialize chat for an event
     * Should be called when an event is created
     */
    suspend fun initializeEventChat(eventId: String, eventName: String): Result<Unit> {
        return try {
            val chatData = hashMapOf(
                FirestoreCollections.Fields.EVENT_ID to eventId,
                FirestoreCollections.Fields.EVENT_NAME to eventName,
                FirestoreCollections.Fields.LAST_MESSAGE to "Chat started",
                FirestoreCollections.Fields.LAST_MESSAGE_TIME to FieldValue.serverTimestamp(),
                FirestoreCollections.Fields.LAST_MESSAGE_SENDER to "System",
                FirestoreCollections.Fields.PARTICIPANT_COUNT to 0,
                FirestoreCollections.Fields.PARTICIPANT_IDS to emptyList<String>()
            )

            firestore.collection(FirestoreCollections.CHATS)
                .document(eventId)
                .set(chatData)
                .await()

            // Send initial system message
            sendSystemMessage(eventId, "Welcome to the event chat!")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update chat preview with last message
     * This is called internally after sending a message
     */
    private suspend fun updateChatPreview(eventId: String, lastMessage: String, senderName: String) {
        try {
            // Get event name
            val eventDoc = firestore.collection(FirestoreCollections.EVENTS)
                .document(eventId)
                .get()
                .await()

            val eventName = eventDoc.getString(FirestoreCollections.Fields.TITLE) ?: "Unknown Event"

            // Update or create chat document
            val chatData = hashMapOf(
                FirestoreCollections.Fields.EVENT_ID to eventId,
                FirestoreCollections.Fields.EVENT_NAME to eventName,
                FirestoreCollections.Fields.LAST_MESSAGE to lastMessage.take(100), // Limit preview length
                FirestoreCollections.Fields.LAST_MESSAGE_TIME to FieldValue.serverTimestamp(),
                FirestoreCollections.Fields.LAST_MESSAGE_SENDER to senderName
            )

            firestore.collection(FirestoreCollections.CHATS)
                .document(eventId)
                .set(chatData, com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (e: Exception) {
            // Log error but don't fail the send operation
            e.printStackTrace()
        }
    }

    /**
     * Format timestamp for display
     */
    private fun formatTime(timestamp: Timestamp?): String {
        if (timestamp == null) return ""

        val date = timestamp.toDate()
        val now = System.currentTimeMillis()
        val diff = now - date.time

        return when {
            diff < 60 * 1000 -> "Just now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        }
    }

    /**
     * Mark messages as read (future feature)
     */
    suspend fun markMessagesAsRead(eventId: String): Result<Unit> {
        // This can be implemented later for read receipts
        return Result.success(Unit)
    }
}
