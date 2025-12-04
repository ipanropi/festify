package com.cs407.festify.ui.theme.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.cs407.festify.data.repository.CheckInStatus
import com.cs407.festify.ui.theme.screens.components.ReportDialog
import com.cs407.festify.ui.theme.screens.components.StatusTag
import com.cs407.festify.ui.theme.viewmodels.EventDetailsViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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
    val context = LocalContext.current
    val checkInStatus by viewModel.checkInStatus.collectAsState()

    // Get current user ID to check if user is host
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

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
            StatusTag(event.computedStatus)
            Spacer(Modifier.height(8.dp))

            Text(event.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            val starIcon = if (isVouched == true) Icons.Filled.Star else Icons.Outlined.StarBorder
            val starColor = if (isVouched == true) Color(0xFFFFC107) else Color.Gray
            val finalTint = if (isVouched == null) Color.LightGray else starColor

            // Interactive Vouch Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable(enabled = isVouched != null) {
                        viewModel.toggleVouch(event.id)
                    }
            ) {

                val starColor = when (isVouched) {
                    true -> Color(0xFFFFC107) // Gold (Vouched)
                    false -> Color.Gray       // Gray (Not Vouched)
                    null -> Color.LightGray.copy(alpha = 0.3f) // Faint (Loading)
                }

                val starIcon = if (isVouched == true) Icons.Filled.Star else Icons.Outlined.StarBorder

                Icon(
                    imageVector = starIcon,
                    contentDescription = "Vouch",
                    tint = starColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${event.vouchCount} Vouches",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isVouched == true) FontWeight.Bold else FontWeight.Normal,
                    color = if (isVouched == true) Color.Black else Color.Gray
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
            val db = FirebaseFirestore.getInstance()
            val user = FirebaseAuth.getInstance().currentUser
            val coroutineScope = rememberCoroutineScope()

            Button(
                onClick = {
                    coroutineScope.launch {
                        val wasAttending = (rsvpStatus == "attending")

                        // Toggle RSVP in the ViewModel
                        viewModel.toggleRsvp(event.id)

                        // Wait briefly to allow state to update (optional delay if Flow is async)
                        delay(400)

                        if (!wasAttending && user != null) {
                            // User just joined the event → add them to chat
                            val chatRef = db.collection("chats").document(event.id)
                            chatRef.get().addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    chatRef.update(
                                        mapOf(
                                            "participantIds" to FieldValue.arrayUnion(user.uid),
                                            "participantCount" to FieldValue.increment(1)
                                        )
                                    )
                                } else {
                                    // Create chat if missing
                                    chatRef.set(
                                        mapOf(
                                            "eventId" to event.id,
                                            "eventName" to event.title,
                                            "participantIds" to listOf(user.uid),
                                            "participantCount" to 1,
                                            "lastMessage" to "Welcome to ${event.title}! 🎉",
                                            "lastMessageSender" to "System",
                                            "lastMessageTime" to com.google.firebase.Timestamp.now()
                                        )
                                    )
                                }
                            }
                        } else if (wasAttending && user != null) {
                            // User cancelled → remove from chat participants
                            db.collection("chats").document(event.id)
                                .update("participantIds", FieldValue.arrayRemove(user.uid))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (rsvpStatus == "attending")
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (rsvpStatus == "attending") "Not Going (Cancel)" else "Going")
            }

            Spacer(Modifier.height(12.dp))

            // QR Code Check-In Section
            val isHost = currentUserId == event.hostId

            if (isHost) {
                // Host view: Show QR Code button
                Button(
                    onClick = { navController.navigate("qr-display/${event.id}") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Show Check-In QR Code")
                }

                Spacer(Modifier.height(12.dp))

                // Check-in stats card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Check-Ins", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "${event.totalCheckIns}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Unique Attendees", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "${event.checkInCount}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (isAttending) {
                // Attendee view: Scan QR button
                OutlinedButton(
                    onClick = { navController.navigate("qr-scanner") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan to Check In")
                }

                Spacer(Modifier.height(12.dp))

                // Check-in status display
                when (val status = checkInStatus) {
                    is CheckInStatus.CheckedIn -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Checked In (${status.count}x)",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                    status.lastCheckInAt?.let {
                                        val timeFormat = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                                        Text(
                                            "Last: ${timeFormat.format(it.toDate())}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is CheckInStatus.NotCheckedIn -> {
                        // Don't show anything if not checked in
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            EventMapTab(event)
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
fun EventMapTab(event: Event) {
    val context = LocalContext.current
    val eventLatLng = remember(event.latitude, event.longitude) {
        if (event.latitude != null && event.longitude != null) {
            LatLng(event.latitude, event.longitude)
        } else null
    }

    if (eventLatLng == null) {
        PlaceholderTab("Host has not dropped a map pin yet.")
        return
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(eventLatLng, 15f)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = MarkerState(position = eventLatLng),
                    title = event.title,
                    snippet = event.location
                )
            }
        }
        Text(
            text = event.location,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Button(
            onClick = { openLocationInMaps(context, eventLatLng, event.title) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Directions, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Navigate with Google Maps")
        }
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

fun openLocationInMaps(context: Context, latLng: LatLng, label: String) {
    val encodedLabel = Uri.encode(label.ifBlank { "Event Location" })
    val geoUri = Uri.parse("geo:${latLng.latitude},${latLng.longitude}?q=${latLng.latitude},${latLng.longitude}($encodedLabel)")
    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
    }
}
