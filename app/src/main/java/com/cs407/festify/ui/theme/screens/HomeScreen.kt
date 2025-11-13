package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cs407.festify.data.model.Event

@Composable
fun HomeScreen(navController: NavController) {


    var query by remember { mutableStateOf("") }

    // Hardcoded event (no ViewModel)
    val event = Event(
        id = "1",
        title = "Tech Startup Networking",
        description = "Connect with fellow entrepreneurs and innovators. Great opportunity to share ideas, find mentors, and build your startup network!",
        imageUrl = "https://images.unsplash.com/photo-1551836022-4c4c79ecde51",
        date = "Nov 5, 2025",
        time = "6:00 PM - 9:00 PM",
        location = "Innovation Hub, Downtown",
        attendees = 42,
        maxAttendees = 80,
        status = "upcoming",
        userRsvp = "attending"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title and Search Bar
        Text("Discover Events", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Find and join amazing events near you", fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search events…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Only one event card
        EventCard(event) { eventId ->
            navController.navigate("event/$eventId")
        }
    }
}

@Composable
fun EventCard(event: Event, onClick: (String) -> Unit = {} ) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(event.id) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Network image (uses Coil)
            AsyncImage(
                model = event.imageUrl,
                contentDescription = event.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatusTag(event.status)
                    if (event.userRsvp == "attending") {
                        AssistChip(onClick = {}, label = { Text("Attending") })
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(event.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("${event.date} · ${event.time}", fontSize = 14.sp)
                Text(event.location, fontSize = 14.sp)
                Text(
                    "${event.attendees}/${event.maxAttendees} attending",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(event.description, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun StatusTag(status: String) {
    val color = when (status.lowercase()) {
        "upcoming" -> MaterialTheme.colorScheme.primary
        "maybe" -> MaterialTheme.colorScheme.tertiary
        "past" -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = status.uppercase(),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
