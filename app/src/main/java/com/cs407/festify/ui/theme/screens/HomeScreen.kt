package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cs407.festify.ui.theme.viewmodels.HomeScreenViewModel
import com.cs407.festify.ui.theme.screens.components.SmartEventList
import com.cs407.festify.ui.theme.viewmodels.EventDetailsViewModel
import androidx.compose.ui.platform.LocalContext



@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    detailsViewModel: EventDetailsViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState()
    var query by remember { mutableStateOf("") }
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
                        onValueChange = {
                            query = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search events…") },
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