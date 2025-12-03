package com.cs407.festify.ui.theme.screens

import com.google.firebase.Timestamp

data class ChatMessage(
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null
)

