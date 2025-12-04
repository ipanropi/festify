package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

data class ChatMessage(
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    eventId: String,
    eventName: String,
    navController: NavController
) {
    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    val messages = remember { mutableStateListOf<ChatMessage>() }
    var newMessage by remember { mutableStateOf(TextFieldValue("")) }

    // --- Listen for messages ---
    LaunchedEffect(eventId) {
        db.collection("chats")
            .document(eventId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Error fetching messages: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    messages.clear()
                    messages.addAll(snapshot.toObjects(ChatMessage::class.java))
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(eventName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        val text = newMessage.text.trim()
                        if (text.isNotEmpty() && user != null) {
                            sendMessage(db, eventId, user.uid, user.email ?: "Unknown", text)
                            newMessage = TextFieldValue("")
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(8.dp),
            reverseLayout = false
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

// --- Helper to send message ---
fun sendMessage(
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
    val messagesRef = chatRef.collection("messages")

    db.runBatch { batch ->
        val newMsgRef = messagesRef.document()
        batch.set(newMsgRef, message)
        batch.update(chatRef, mapOf(
            "lastMessage" to text,
            "lastMessageSender" to senderName,
            "lastMessageTime" to Timestamp.now()
        ))
    }.addOnSuccessListener {
        println("Message sent to chat $chatId")
    }.addOnFailureListener {
        println("Failed to send message: ${it.message}")
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isCurrentUser: Boolean) {
    val bubbleColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (!isCurrentUser) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
