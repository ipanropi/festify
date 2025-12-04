package com.cs407.festify.ui.theme.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.festify.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for LoginScreen to provide access to AuthRepository
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Sign in with Google using ID token
     */
    fun signInWithGoogle(
        idToken: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess {
                onSuccess()
            }.onFailure { error ->
                onFailure(error.message ?: "Google sign-in failed")
            }
        }
    }
}