package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cs407.festify.data.model.Event
import com.cs407.festify.ui.theme.viewmodels.EventDetailsViewModel

@Composable
fun EventDetailsScreen(
    eventId: String,
    navController: NavController,
    viewModel: EventDetailsViewModel = hiltViewModel()
) {
    val event by viewModel.event.collectAsState()
    val rsvpStatus by viewModel.userRsvpStatus.collectAsState()

    // load only once
    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    if (event == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold { innerPadding ->
        Column(Modifier
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())) {
            EventDetailsContent(event!!, navController, viewModel, rsvpStatus)
        }
    }
}




@Composable
fun EventDetailsContent(
    event: Event,
    navController: NavController,
    viewModel: EventDetailsViewModel,
    rsvpStatus: String
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Chat", "Map", "Photos")
    Column {

    // ===== PARENT STACK FOR IMAGE + BACK BUTTON =====
    Box {
        // IMAGE OR GRAY PLACEHOLDER
        if (event.imageUrl.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color.Gray)
            )
        } else {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentScale = ContentScale.Crop
            )
        }

        // BACK BUTTON
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .padding(16.dp)
                .size(40.dp)
                .background(Color.White, CircleShape)
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black
            )
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {

        Spacer(Modifier.height(12.dp))

        // ===== EVENT INFO =====
        StatusTag(event.status)
        Spacer(Modifier.height(8.dp))

        Text(
            event.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(Modifier.height(8.dp))

        InfoRow(Icons.Default.DateRange, "${event.date}")
        InfoRow(Icons.Default.LocationOn, event.location)
        InfoRow(Icons.Default.People, "${event.attendees}/${event.maxAttendees}")

        Spacer(Modifier.height(12.dp))

        Text(event.description)
        Spacer(Modifier.height(16.dp))

        val isAttending = rsvpStatus == "attending"

        Button(
            onClick = { viewModel.toggleRsvp(event.id) }, // Call toggle instead of rsvpToEvent
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAttending) Color(0xFFFF6B6B) else Color(0xFF2196F3)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isAttending) "Not Going (Cancel)" else "Going")
        }


        Spacer(Modifier.height(16.dp))

        // ===== TABS =====
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            tabs.forEachIndexed { index, title ->
                val isSelected = index == selectedTab

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isSelected) Color(0xFFE9EEF9)
                            else Color(0xFFF3F3F3)
                        )
                        .clickable { selectedTab = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.Black else Color.Gray
                    )
                }
            }
        }


        Spacer(Modifier.height(16.dp))

        // ===== TAB CONTENTS =====
        when (selectedTab) {
            0 -> ChatTabUI()
            1 -> MapTabUI()
            2 -> PhotosTabUI()
        }
    }
    }
}

@Composable
fun PhotosTabUI() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text("Photos Coming Soon")
    }
}

@Composable
fun MapTabUI() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text("Map Coming Soon")
    }
}
@Composable
fun ChatTabUI() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text("Chat Coming Soon")
    }
}


@Composable
fun InfoRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.Gray)
    }
}