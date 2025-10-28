package com.cs407.festify.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "John Doe",
    val email: String = "john.doe@email.com",
    val initials: String = "JD",
    val eventsAttended: Int = 24,
    val eventsHosted: Int = 8,
    val rating: Double = 4.8,
    val upcomingEvents: Int = 3,
    val connections: Int = 156,
    val achievements: List<Achievement> = listOf(
        Achievement("Super Host", "Hosted 5 successful events", true),
        Achievement("Network Builder", "Connected with 100+ people", false)
    ),
    val darkMode: Boolean = false,
    val pushNotifications: Boolean = true
)

data class Achievement(
    val title: String,
    val description: String,
    val isNew: Boolean
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    fun toggleDarkMode() {
        _uiState.update { it.copy(darkMode = !it.darkMode) }
    }

    fun toggleNotifications() {
        _uiState.update { it.copy(pushNotifications = !it.pushNotifications) }
    }

    fun editProfile(newName: String, newEmail: String) {
        _uiState.update {
            it.copy(
                name = newName,
                email = newEmail,
                initials = newName.split(" ")
                    .take(2)
                    .joinToString("") { n -> n.first().uppercaseChar().toString() }
            )
        }
    }

    fun refreshProfileData() {
        // Example of async loading later:
        viewModelScope.launch {
            // simulate network call or database fetch
        }
    }
}
