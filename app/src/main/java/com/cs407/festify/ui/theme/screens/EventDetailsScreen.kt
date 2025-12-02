package com.cs407.festify.ui.theme.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cs407.festify.data.model.Event
import com.cs407.festify.ui.theme.screens.components.ReportDialog
import com.cs407.festify.ui.theme.screens.components.StatusTag
import com.cs407.festify.ui.theme.viewmodels.EventDetailsViewModel

@Composable
fun EventDetailsScreen(
    eventId: String,
    navController: NavController,
    viewModel: EventDetailsViewModel = hiltViewModel()
) {
    val event by viewModel.event.collectAsState()
    val rsvpStatus by viewModel.userRsvpStatus.collectAsState()

    // CHANGE 1: Observe nullable Boolean
    val isVouched by viewModel.isVouched.collectAsState()

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    if (event == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold { innerPadding ->
            Column(
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                EventDetailsContent(event!!, navController, viewModel, rsvpStatus, isVouched)
            }
        }
    }
}

@Composable
fun EventDetailsContent(
    event: Event,
    navController: NavController,
    viewModel: EventDetailsViewModel,
    rsvpStatus: String,
    isVouched: Boolean?
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chat", "Map", "Photos")
    val context = LocalContext.current

    // Dialog States
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    Column {
        // --- HEADER IMAGE & BUTTONS ---
        Box {
            // Image
            if (event.imageUrl.isNullOrEmpty()) {
                Box(Modifier.fillMaxWidth().height(260.dp).background(Color.Gray))
            } else {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Back Button
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.White, CircleShape)
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Black)
            }

            // More Options Menu
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(40.dp).background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, "More", tint = Color.Black)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Share Event") },
                        onClick = { showMenu = false; shareEvent(context, event) },
                        leadingIcon = { Icon(Icons.Default.Share, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Report", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; showReportDialog = true },
                        leadingIcon = { Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }

        // --- DETAILS ---
        Column(Modifier.padding(16.dp)) {
            StatusTag(event.status)
            Spacer(Modifier.height(8.dp))

            Text(event.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            // CHANGE 3: Logic to handle Loading State (null)
            val starIcon = if (isVouched == true) Icons.Filled.Star else Icons.Outlined.StarBorder
            val starColor = if (isVouched == true) Color(0xFFFFC107) else Color.Gray
            // Dim the icon if we are still loading
            val finalTint = if (isVouched == null) Color.LightGray else starColor

            // Interactive Vouch Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    // Since isVouched is Boolean?, we use '== true' to check
                    .clickable { viewModel.toggleVouch(event.id) }
            ) {
                Icon(
                    // Check explicitly for true
                    imageVector = if (isVouched == true) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Vouch",
                    // Check explicitly for true
                    tint = if (isVouched == true) Color(0xFFFFC107) else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${event.vouchCount} Vouches",
                    style = MaterialTheme.typography.titleMedium,
                    // Check explicitly for true
                    fontWeight = if (isVouched == true) FontWeight.Bold else FontWeight.Normal,
                    // Use standard black color
                    color = Color.Black
                )
            }

            Spacer(Modifier.height(8.dp))
            InfoRow(Icons.Default.DateRange, "${event.date}")
            InfoRow(Icons.Default.LocationOn, event.location)
            InfoRow(Icons.Default.People, "${event.attendees}/${event.maxAttendees}")

            Spacer(Modifier.height(12.dp))
            Text(event.description)
            Spacer(Modifier.height(16.dp))

            // RSVP Button
            val isAttending = rsvpStatus == "attending"
            Button(
                onClick = { viewModel.toggleRsvp(event.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAttending) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isAttending) "Not Going (Cancel)" else "Going")
            }

            Spacer(Modifier.height(16.dp))

            // Tabs
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = index == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.LightGray.copy(alpha = 0.3f))
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(title, color = if (isSelected) Color.Black else Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            when (selectedTab) {
                0 -> PlaceholderTab("Chat Coming Soon")
                1 -> PlaceholderTab("Map Coming Soon")
                2 -> PlaceholderTab("Photos Coming Soon")
            }
        }
    }

    if (showReportDialog) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { reason ->
                 viewModel.reportEvent(event.id, reason, context)
                showReportDialog = false
            }
        )
    }
}

@Composable
fun PlaceholderTab(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.Gray)
    }
}

@Composable
fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.Gray)
    }
}

fun shareEvent(context: Context, event: Event) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Check out ${event.title} at ${event.location}!")
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}