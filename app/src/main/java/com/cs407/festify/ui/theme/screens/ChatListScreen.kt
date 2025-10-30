package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cs407.festify.ui.theme.FestifyTheme

data class EventChat(
    val eventId: String,
    val eventName: String,
    val lastMessage: String,
    val time: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(navController: NavController) {
    val chatList = listOf(
        EventChat("networking_dinner", "Networking Dinner 2025", "Let’s meet at 6:45 near the hall.", "Yesterday"),
        EventChat("music_fest", "Music Fest 2025", "Yeah! Can’t wait for the first act.", "2h ago"),
        EventChat("startup_mixer", "Startup Mixer", "Welcome to your event chat!", "10m ago")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Event Chats") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(chatList) { chat ->
                ChatListItem(chat) {
                    navController.navigate("chat/${chat.eventId}")
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
fun ChatListItem(chat: EventChat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(chat.eventName, style = MaterialTheme.typography.titleMedium)
            Text(
                chat.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            chat.time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatListScreenPreview() {
    FestifyTheme {
        ChatListScreen(navController = androidx.navigation.compose.rememberNavController())
    }
}
