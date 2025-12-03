package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cs407.festify.ui.theme.screens.components.SmartEventList
import com.cs407.festify.ui.theme.viewmodels.EventDetailsViewModel
import com.cs407.festify.ui.theme.viewmodels.HomeScreenViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    detailsViewModel: EventDetailsViewModel = hiltViewModel()
) {
    val events by viewModel.filteredEvents.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current

    SmartEventList(
        events = events,
        onEventClick = { id -> navController.navigate("event/$id") },
        onProfileClick = { userId -> navController.navigate("user/$userId") },
        headerContent = {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Discover Events", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search events") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )

                    Spacer(Modifier.height(16.dp))
                }
            }
        },
        onReportSubmit = { eventId, reason ->
            detailsViewModel.reportEvent(eventId, reason, context)
        }
    )
}
