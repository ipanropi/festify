package com.cs407.festify.ui.theme.viewmodels

import androidx.lifecycle.ViewModel
import com.cs407.festify.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for LoginScreen to provide access to AuthRepository
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    val authRepository: AuthRepository
) : ViewModel()