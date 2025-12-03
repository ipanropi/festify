@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class EventChat(
    val eventId: String = "",
    val eventName: String = "",
    val lastMessage: String = "",
    val time: String = "",
    val participantIds: List<String> = emptyList()
)

@Composable
fun ChatListScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val chats = remember { mutableStateListOf<EventChat>() }

    // Load only chats the user joined
    LaunchedEffect(user) {
        db.collection("chats")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val joined = snapshot.documents
                        .mapNotNull { doc -> doc.toObject(EventChat::class.java) }
                        .filter { it.participantIds.contains(user?.uid) }
                    chats.clear()
                    chats.addAll(joined)
                }
            }
    }

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
            items(chats) { chat ->
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
