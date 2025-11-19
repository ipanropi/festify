package com.cs407.festify.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.festify.data.model.Achievement
import com.cs407.festify.data.repository.AuthRepository
import com.cs407.festify.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val name: String = "",
    val email: String = "",
    val initials: String = "",
    val eventsAttended: Int = 0,
    val eventsHosted: Int = 0,
    val rating: Double = 0.0,
    val upcomingEvents: Int = 0,
    val connections: Int = 0,
    val achievements: List<Achievement> = emptyList(),
    val darkMode: Boolean = false,
    val pushNotifications: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        loadUserProfile()
        loadAchievements()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            userRepository.getCurrentUserProfile().collect { result ->
                result.onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            name = user.name,
                            email = user.email,
                            initials = user.initials,
                            eventsAttended = user.eventsAttended,
                            eventsHosted = user.eventsHosted,
                            rating = user.rating,
                            upcomingEvents = user.upcomingEvents,
                            connections = user.connections,
                            darkMode = user.settings.darkMode,
                            pushNotifications = user.settings.pushNotifications
                        )
                    }
                }.onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load profile"
                        )
                    }
                }
            }
        }
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            userRepository.getUserAchievements().collect { result ->
                result.onSuccess { achievements ->
                    _uiState.update {
                        it.copy(achievements = achievements)
                    }
                }.onFailure {
                    // Silently fail for achievements, not critical
                    _uiState.update {
                        it.copy(achievements = emptyList())
                    }
                }
            }
        }
    }

    fun toggleDarkMode() {
        val newValue = !_uiState.value.darkMode
        _uiState.update { it.copy(darkMode = newValue) }

        viewModelScope.launch {
            userRepository.updateUserSettings(darkMode = newValue)
        }
    }

    fun toggleNotifications() {
        val newValue = !_uiState.value.pushNotifications
        _uiState.update { it.copy(pushNotifications = newValue) }

        viewModelScope.launch {
            userRepository.updateUserSettings(pushNotifications = newValue)
        }
    }

    fun editProfile(newName: String, newEmail: String) {
        viewModelScope.launch {
            // Update in Firestore
            val result = userRepository.updateUserProfile(name = newName)

            result.onSuccess {
                // Also update email in Firebase Auth
                authRepository.updateEmail(newEmail)

                // UI will be updated automatically via the Flow
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(error = exception.message ?: "Failed to update profile")
                }
            }
        }
    }

    fun refreshProfileData() {
        loadUserProfile()
        loadAchievements()
    }

    fun signOut() {
        authRepository.signOut()
    }
}
