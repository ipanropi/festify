package com.cs407.festify.auth

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest


// ============================================
// Email Validation
// ============================================

enum class EmailResult {
    Valid,
    Empty,
    Invalid,
}

fun validateEmail(email: String): EmailResult {
    if (email.isEmpty()){
        return EmailResult.Empty
    }
    val pattern = Regex("^[\\w.]+@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$")
    return if (pattern.matches(email)){
        EmailResult.Valid
    } else {
        EmailResult.Invalid
    }
}

// ============================================
// Password Validation
// ============================================

enum class PasswordResult {
    Valid,
    Empty,
    Short,
    Invalid
}

fun validatePassword(password: String): PasswordResult {
    if (password.isEmpty()) {
        return PasswordResult.Empty
    }
    if (password.length < 5) {
        return PasswordResult.Short
    }
    if (Regex("\\d+").containsMatchIn(password) &&
        Regex("[a-z]+").containsMatchIn(password) &&
        Regex("[A-Z]+").containsMatchIn(password)
    ) {
        return PasswordResult.Valid
    }
    return PasswordResult.Invalid
}

// ============================================
// Firebase Authentication Functions
// ============================================

/**
 * Sign in existing user with email and password
 * If sign-in fails, automatically attempts to create new account
 */
fun signIn(
    email: String,
    password: String,
    onSuccess: (String) -> Unit = {},
    onFailure: (String) -> Unit = {}
) {
    val auth = Firebase.auth

    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                onSuccess("Sign-in successful: ${user?.email ?: "Unknown user"}")
            } else {
                createAccount(
                    email = email,
                    password = password,
                    onSuccess = { user ->
                        onSuccess("Account created for: ${user?.email ?: "Unknown user"}")
                    },
                    onFailure = { errorMsg ->
                        onFailure("Sign-in failed and account creation error: $errorMsg")
                    }
                )
            }
        }
}

/**
 * Create new Firebase account with email and password
 */
fun createAccount(
    email: String,
    password: String,
    onSuccess: (FirebaseUser?) -> Unit,
    onFailure: (String) -> Unit
) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    auth.createUserWithEmailAndPassword(email, password)
        .addOnSuccessListener { result ->
            val user = auth.currentUser
            onSuccess(user)
        }
        .addOnFailureListener { exception ->
            onFailure(exception.message ?: "Error creating account")
        }
}

fun updateName(displayName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
    val user = Firebase.auth.currentUser
    if (user != null) {
        val profileUpdates = userProfileChangeRequest {
            this.displayName = displayName
        }
        user.updateProfile(profileUpdates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Failed to update name") }
    } else {
        onFailure("No authenticated user")
    }
}