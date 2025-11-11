package com.cs407.festify.data.repository

import com.cs407.festify.data.model.User
import com.cs407.festify.data.model.UserProfile
import com.cs407.festify.data.model.UserSettings
import com.cs407.festify.data.remote.FirestoreCollections
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling authentication operations
 * Manages Firebase Authentication and user profile creation
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /**
     * Get the currently signed-in user
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Check if a user is currently signed in
     */
    val isUserSignedIn: Boolean
        get() = currentUser != null

    /**
     * Get current user ID
     */
    val currentUserId: String?
        get() = currentUser?.uid

    /**
     * Sign up a new user with email and password
     * Also creates a user profile in Firestore
     */
    suspend fun signUp(email: String, password: String, name: String): Result<FirebaseUser> {
        return try {
            // Create user account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("User creation failed"))

            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // Create user profile in Firestore
            val initials = name.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .take(2)

            val userProfile = hashMapOf(
                FirestoreCollections.Fields.EMAIL to email,
                FirestoreCollections.Fields.NAME to name,
                FirestoreCollections.Fields.INITIALS to initials,
                FirestoreCollections.Fields.EVENTS_ATTENDED to 0,
                FirestoreCollections.Fields.EVENTS_HOSTED to 0,
                FirestoreCollections.Fields.RATING to 0.0,
                FirestoreCollections.Fields.UPCOMING_EVENTS to 0,
                FirestoreCollections.Fields.CONNECTIONS to 0,
                FirestoreCollections.Fields.CREATED_AT to FieldValue.serverTimestamp(),
                FirestoreCollections.Fields.UPDATED_AT to FieldValue.serverTimestamp(),
                "settings" to hashMapOf(
                    "darkMode" to false,
                    "pushNotifications" to true,
                    "notifications" to true,
                    "locationServices" to false
                ),
                "profile" to hashMapOf(
                    "bio" to "",
                    "avatarUrl" to "",
                    "phoneNumber" to ""
                )
            )

            firestore.collection(FirestoreCollections.USERS)
                .document(firebaseUser.uid)
                .set(userProfile)
                .await()

            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in an existing user with email and password
     */
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Sign in failed"))

            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out the current user
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Send a password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user email
     */
    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            currentUser?.updateEmail(newEmail)?.await()
                ?: return Result.failure(Exception("No user signed in"))

            // Update email in Firestore
            currentUserId?.let { userId ->
                firestore.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .update(
                        FirestoreCollections.Fields.EMAIL, newEmail,
                        FirestoreCollections.Fields.UPDATED_AT, FieldValue.serverTimestamp()
                    )
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user password
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            currentUser?.updatePassword(newPassword)?.await()
                ?: return Result.failure(Exception("No user signed in"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete user account
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user signed in"))

            // Delete user profile from Firestore
            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .delete()
                .await()

            // Delete user account
            currentUser?.delete()?.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reload current user data
     */
    suspend fun reloadUser(): Result<Unit> {
        return try {
            currentUser?.reload()?.await()
                ?: return Result.failure(Exception("No user signed in"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
