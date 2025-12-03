package com.cs407.festify.ui.theme.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.festify.data.model.Event
import com.cs407.festify.data.model.User
import com.cs407.festify.data.repository.EventRepository
import com.cs407.festify.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _friendshipStatus = MutableStateFlow<String>("none")
    val friendshipStatus: StateFlow<String> = _friendshipStatus

    private val _hostedEvents = MutableStateFlow<List<Event>>(emptyList())
    val hostedEvents: StateFlow<List<Event>> = _hostedEvents

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            // Load user profile
            val userResult = userRepository.getUserProfile(userId)
            if (userResult.isSuccess) {
                _user.value = userResult.getOrNull()
            }

            // Check friendship status
            val statusResult = userRepository.checkFriendshipStatus(userId)
            if (statusResult.isSuccess) {
                _friendshipStatus.value = statusResult.getOrNull() ?: "none"
            }

            _isLoading.value = false

            // Load hosted events in a separate coroutine (continuous Flow)
            viewModelScope.launch {
                eventRepository.getEventsByHostId(userId).collect { result ->
                    result.onSuccess { events ->
                        _hostedEvents.value = events
                    }
                }
            }
        }
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            val result = userRepository.sendFriendRequest(userId)
            if (result.isSuccess) {
                _friendshipStatus.value = "request_sent"
            }
        }
    }

    fun acceptFriendRequest(userId: String) {
        viewModelScope.launch {
            val requestId = "${userId}_${userRepository.getUserProfile(userId).getOrNull()?.id}"
            val result = userRepository.acceptFriendRequest(requestId)
            if (result.isSuccess) {
                _friendshipStatus.value = "friends"
            }
        }
    }

    fun declineFriendRequest(userId: String) {
        viewModelScope.launch {
            val requestId = "${userId}_${userRepository.getUserProfile(userId).getOrNull()?.id}"
            val result = userRepository.declineFriendRequest(requestId)
            if (result.isSuccess) {
                _friendshipStatus.value = "none"
            }
        }
    }

    fun removeFriend(userId: String) {
        viewModelScope.launch {
            val result = userRepository.removeFriend(userId)
            if (result.isSuccess) {
                _friendshipStatus.value = "none"
            }
        }
    }
}
