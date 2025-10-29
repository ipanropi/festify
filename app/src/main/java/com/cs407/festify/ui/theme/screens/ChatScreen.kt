package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.cs407.festify.ui.theme.FestifyTheme
data class Message(
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isCurrentUser: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<Message>,
    onSendMessage: (String) -> Unit
) {
    var newMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Chat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Message list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    ChatBubble(message)
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // Message input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                IconButton(
                    onClick = {
                        if (newMessage.isNotBlank()) {
                            onSendMessage(newMessage.trim())
                            newMessage = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    val alignment = if (message.isCurrentUser)
        Alignment.CenterEnd
    else
        Alignment.CenterStart
    val bubbleColor = if (message.isCurrentUser)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 6.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .background(bubbleColor, shape = MaterialTheme.shapes.medium)
                .padding(10.dp)
                .widthIn(max = 280.dp)
        ) {
            if (!message.isCurrentUser) {
                Text(
                    text = message.senderName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Text(
                text = message.text,
                color = textColor,
                fontSize = 14.sp
            )
            Text(
                text = message.timestamp,
                color = MaterialTheme.colorScheme.outline,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenLightPreview() {
    FestifyTheme(darkTheme = false) {
        ChatScreen(
            messages = sampleMessages(),
            onSendMessage = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenDarkPreview() {
    FestifyTheme(darkTheme = true) {
        ChatScreen(
            messages = sampleMessages(),
            onSendMessage = {}
        )
    }
}

private fun sampleMessages() = listOf(
    Message("Irfan", "Hey everyone, ready for tonight?", "7:45 PM", false),
    Message("Adi", "Yep, just printed the QR codes!", "7:46 PM", true),
    Message("Afif", "Don’t forget the name tags 😆", "7:47 PM", false),
    Message("Irfan Ishak", "On my way to the venue!", "7:49 PM", false)
)
