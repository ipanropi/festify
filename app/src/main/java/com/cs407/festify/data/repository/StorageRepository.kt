package com.cs407.festify.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling file uploads to Firebase Storage
 */
@Singleton
class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Upload an event image
     * @param uri URI of the image to upload
     * @return Download URL of the uploaded image
     */
    suspend fun uploadEventImage(uri: Uri): Result<String> {
        return try {
            val fileName = "events/${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child(fileName)

            // Set metadata
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            // Upload file
            storageRef.putFile(uri, metadata).await()

            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await()

            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload a profile/avatar image
     * @param uri URI of the image to upload
     * @return Download URL of the uploaded image
     */
    suspend fun uploadProfileImage(uri: Uri): Result<String> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            val fileName = "profiles/$userId.jpg"
            val storageRef = storage.reference.child(fileName)

            // Set metadata
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            // Upload file (this will overwrite previous profile image)
            storageRef.putFile(uri, metadata).await()

            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await()

            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload a chat image
     * @param eventId ID of the event
     * @param uri URI of the image to upload
     * @return Download URL of the uploaded image
     */
    suspend fun uploadChatImage(eventId: String, uri: Uri): Result<String> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            val fileName = "chats/$eventId/${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child(fileName)

            // Set metadata
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("uploadedBy", userId)
                .setCustomMetadata("eventId", eventId)
                .build()

            // Upload file
            storageRef.putFile(uri, metadata).await()

            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await()

            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete an image from storage
     * @param imageUrl Full URL of the image to delete
     */
    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            val storageRef = storage.getReferenceFromUrl(imageUrl)
            storageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a user's profile image
     */
    suspend fun deleteProfileImage(): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            val fileName = "profiles/$userId.jpg"
            val storageRef = storage.reference.child(fileName)

            storageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete all images for an event (called when event is deleted)
     * This deletes event cover image and all chat images
     */
    suspend fun deleteEventImages(eventId: String): Result<Unit> {
        return try {
            val chatImagesRef = storage.reference.child("chats/$eventId")

            // List all files in the event's chat images folder
            val listResult = chatImagesRef.listAll().await()

            // Delete all files
            listResult.items.forEach { fileRef ->
                fileRef.delete().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload a file with progress tracking
     * @param uri URI of the file to upload
     * @param path Storage path (e.g., "events/image.jpg")
     * @param onProgress Callback for upload progress (0.0 to 1.0)
     * @return Download URL of the uploaded file
     */
    suspend fun uploadFileWithProgress(
        uri: Uri,
        path: String,
        onProgress: (Double) -> Unit
    ): Result<String> {
        return try {
            val storageRef = storage.reference.child(path)

            // Set metadata
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            // Upload with progress tracking
            val uploadTask = storageRef.putFile(uri, metadata)

            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                onProgress(progress / 100.0)
            }

            uploadTask.await()

            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await()

            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get file size from URI (useful for validation)
     */
    fun getFileSize(uri: Uri): Long {
        // This would require context to get content resolver
        // For now, return -1 to indicate not implemented
        return -1L
    }

    /**
     * Check if image URL is from Firebase Storage
     */
    fun isFirebaseStorageUrl(url: String): Boolean {
        return url.contains("firebasestorage.googleapis.com")
    }

    /**
     * Generate a unique filename
     */
    fun generateUniqueFilename(prefix: String, extension: String): String {
        return "$prefix/${UUID.randomUUID()}.$extension"
    }
}
