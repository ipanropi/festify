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
import kotlinx.coroutines.flow.first
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

            try {
                // Sync connections count first
                userRepository.syncConnectionsCount(userId)

                // Load user profile
                val userResult = userRepository.getUserProfile(userId)
                if (userResult.isSuccess) {
                    _user.value = userResult.getOrNull()
                } else {
                    println("Error loading user profile: ${userResult.exceptionOrNull()?.message}")
                }

                _isLoading.value = false

                // Listen to friendship status changes in real-time
                viewModelScope.launch {
                    try {
                        userRepository.getFriendshipStatusFlow(userId).collect { result ->
                            result.onSuccess { status ->
                                _friendshipStatus.value = status
                            }.onFailure { error ->
                                println("Error checking friendship status: ${error.message}")
                                _friendshipStatus.value = "none"
                            }
                        }
                    } catch (e: Exception) {
                        println("Exception checking friendship status: ${e.message}")
                        _friendshipStatus.value = "none"
                    }
                }

                // Load hosted events in a separate coroutine (continuous Flow)
                viewModelScope.launch {
                    try {
                        eventRepository.getEventsByHostId(userId).collect { result ->
                            result.onSuccess { events ->
                                _hostedEvents.value = events
                            }.onFailure { error ->
                                println("Error loading hosted events: ${error.message}")
                                _hostedEvents.value = emptyList()
                            }
                        }
                    } catch (e: Exception) {
                        println("Exception loading hosted events: ${e.message}")
                        _hostedEvents.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                println("Exception in loadUserProfile: ${e.message}")
                _isLoading.value = false
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

    fun cancelFriendRequest(userId: String) {
        viewModelScope.launch {
            val result = userRepository.cancelFriendRequest(userId)
            if (result.isSuccess) {
                _friendshipStatus.value = "none"
            }
        }
    }

    fun acceptFriendRequest(senderId: String) {
        viewModelScope.launch {
            // Get current user ID first
            val currentUserResult = userRepository.getCurrentUserProfile().first()
            if (currentUserResult.isSuccess) {
                val currentUserId = currentUserResult.getOrNull()?.id ?: return@launch
                // Request ID format: senderId_receiverId
                val requestId = "${senderId}_${currentUserId}"
                val result = userRepository.acceptFriendRequest(requestId)
                if (result.isSuccess) {
                    _friendshipStatus.value = "friends"
                }
            }
        }
    }

    fun declineFriendRequest(senderId: String) {
        viewModelScope.launch {
            // Get current user ID first
            val currentUserResult = userRepository.getCurrentUserProfile().first()
            if (currentUserResult.isSuccess) {
                val currentUserId = currentUserResult.getOrNull()?.id ?: return@launch
                // Request ID format: senderId_receiverId
                val requestId = "${senderId}_${currentUserId}"
                val result = userRepository.declineFriendRequest(requestId)
                if (result.isSuccess) {
                    _friendshipStatus.value = "none"
                }
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
