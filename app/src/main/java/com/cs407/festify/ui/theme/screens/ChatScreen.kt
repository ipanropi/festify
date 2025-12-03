@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cs407.festify.ui.theme.screens.ChatMessage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(eventName: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var newMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(eventName) {
        user?.uid?.let { uid ->
            val chatRef = db.collection("chats").document(eventName)
            chatRef.get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    chatRef.update(
                        mapOf(
                            "participantIds" to FieldValue.arrayUnion(uid),
                            "participantCount" to FieldValue.increment(1)
                        )
                    )
                } else {
                    // Create chat if not exists
                    chatRef.set(
                        mapOf(
                            "eventId" to eventName,
                            "eventName" to eventName.replace("_", " ").replaceFirstChar { it.uppercase() },
                            "participantIds" to listOf(uid),
                            "participantCount" to 1,
                            "lastMessage" to "Welcome to $eventName!",
                            "lastMessageSender" to "System",
                            "lastMessageTime" to Timestamp.now()
                        )
                    )
                }
            }
        }
    }

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
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newMessage.isNotBlank()) {
                            coroutineScope.launch {
                                sendMessage(
                                    db = db,
                                    chatId = eventName,
                                    senderId = user?.uid ?: "anonymous",
                                    senderName = user?.email ?: "Unknown",
                                    text = newMessage.trim()
                                )
                                newMessage = ""
                            }
                        }
                    }
                ) {
                    Text("Send")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            items(messages) { msg ->
                MessageBubble(
                    message = msg,
                    isCurrentUser = msg.senderId == user?.uid
                )
            }
        }
    }
}

/** Adds new message + updates chat metadata + participant list */
suspend fun sendMessage(
    db: FirebaseFirestore,
    chatId: String,
    senderId: String,
    senderName: String,
    text: String
) {
    val message = ChatMessage(
        senderId = senderId,
        senderName = senderName,
        text = text,
        timestamp = Timestamp.now()
    )

    val chatRef = db.collection("chats").document(chatId)

    chatRef.collection("messages").add(message)
        .addOnSuccessListener {
            chatRef.update(
                mapOf(
                    "lastMessage" to text,
                    "lastMessageSender" to senderName,
                    "lastMessageTime" to Timestamp.now(),
                    "participantIds" to FieldValue.arrayUnion(senderId)
                )
            )
        }
        .addOnFailureListener { e -> e.printStackTrace() }
}

/** Message bubble UI */
@Composable
fun MessageBubble(message: ChatMessage, isCurrentUser: Boolean) {
    val bgColor = if (isCurrentUser)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.secondaryContainer

    val textColor = if (isCurrentUser)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = bgColor,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (!isCurrentUser) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}
