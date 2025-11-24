package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cs407.festify.ui.theme.viewmodels.JoinedEventsViewModel



@Composable
fun JoinedEventsScreen(
    navController: NavController,
    viewModel: JoinedEventsViewModel = hiltViewModel()
) {
    val joinedEvents by viewModel.joinedEvents.collectAsState()

    if (joinedEvents.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("You haven't joined any events yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                Text("Events Joined", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

            items(joinedEvents) { event ->

                EventCard(event = event) { eventId ->
                    navController.navigate("event/$eventId")
                }
            }
        }
    }
}