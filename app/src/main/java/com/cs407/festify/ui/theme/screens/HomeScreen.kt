package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cs407.festify.data.model.Event
import com.cs407.festify.ui.theme.viewmodels.HomeScreenViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeScreenViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState()
    var query by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Discover Events", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search events…") }
            )

            Spacer(Modifier.height(16.dp))
        }

        if (events.isEmpty()) {
            item { Text("No events found.") }
        } else {
            items(events) { event ->
                EventCard(event) { id ->
                    navController.navigate("event/$id")
                }
            }
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
            if (event.imageUrl.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.Gray)
                )
            } else {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = event.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            }


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
