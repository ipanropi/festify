package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cs407.festify.data.model.Event
import com.cs407.festify.ui.theme.viewmodels.HomeScreenViewModel

@Composable
fun HomeScreen(viewModel: HomeScreenViewModel = viewModel()) {
    var query by remember { mutableStateOf("") }
    val events by viewModel.events.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Section
        Column(modifier = Modifier.padding(16.dp)) {
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
        }

        // Event List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            itemsIndexed(events) { index, event ->
                EventCard(event)

                // Load more when reaching bottom
                if (index == events.lastIndex) {
                    LaunchedEffect(Unit) {
                        viewModel.loadMoreEvents()
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
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
