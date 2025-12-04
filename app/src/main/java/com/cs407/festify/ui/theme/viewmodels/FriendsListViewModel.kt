package com.cs407.festify.ui.theme.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.festify.data.model.User
import com.cs407.festify.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsListViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends: StateFlow<List<User>> = _friends

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadFriends(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            userRepository.getFriendsWithDetails(userId).collect { result ->
                result.onSuccess { friendsList ->
                    _friends.value = friendsList
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load friends"
                    _isLoading.value = false
                }
            }
        }
    }
}
