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
fun ChatScreen(eventName: String) {
    var newMessage by remember { mutableStateOf("") }
    val messages = remember { sampleMessagesForEvent(eventName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat — $eventName") },
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
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    ChatBubble(message)
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // Message input bar (UI only)
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
                    placeholder = { Text("Message about $eventName...") },
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
                IconButton(onClick = { /* Placeholder only */ }) {
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
    val alignment = if (message.isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

/* --- Temporary sample data per event (UI only) --- */
fun sampleMessagesForEvent(eventName: String): List<Message> {
    return when (eventName.lowercase()) {
        "networking dinner" -> listOf(
            Message("Afif", "Who’s printing the name tags?", "7:30 PM", false),
            Message("Adi", "Already done this morning!", "7:31 PM", true),
            Message("Irfan", "Let’s meet at 6:45 near the hall.", "7:32 PM", false)
        )
        "music fest" -> listOf(
            Message("Ishak", "Stage setup looks amazing 🔥", "8:10 PM", false),
            Message("Adi", "Yeah! Can’t wait for the first act.", "8:12 PM", true)
        )
        else -> listOf(
            Message("Festify", "Welcome to your event chat!", "Now", false)
        )
    }
}

/* --- UI previews --- */
@Preview(showBackground = true)
@Composable
fun ChatScreenLightPreview() {
    FestifyTheme(darkTheme = false) {
        ChatScreen("Networking Dinner")
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenDarkPreview() {
    FestifyTheme(darkTheme = true) {
        ChatScreen("Music Fest")
    }
}

