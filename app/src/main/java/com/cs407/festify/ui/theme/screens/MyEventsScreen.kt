package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.cs407.festify.data.model.Event
import com.cs407.festify.ui.viewmodels.MyEventsViewModel
import androidx.compose.material.icons.filled.Add


@Composable
fun MyEventsScreen(
    // Get the ViewModel that's automatically created
    viewModel: MyEventsViewModel = viewModel()
) {

    Spacer(modifier = Modifier.height(16.dp))
    // Observe the list of events from the ViewModel
    val myEvents by viewModel.myEvents.collectAsState()

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


    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Placeholder: Do nothing */ },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Event")
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ){
            EventCard(event = event)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }


}



