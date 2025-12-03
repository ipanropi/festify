package com.cs407.festify.ui.theme.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cs407.festify.data.model.Event
import com.cs407.festify.ui.theme.screens.shareEvent
import com.cs407.festify.ui.theme.viewmodels.EventDetailsViewModel

/**
 * A "Smart" List that handles Layout, Cards, Reporting, and Empty States automatically.
 */
@Composable
fun SmartEventList(
    events: List<Event>,
    onEventClick: (String) -> Unit,
    onReportSubmit: (eventId: String, reason: String) -> Unit,
    headerContent: (LazyListScope.() -> Unit)? = null,
    cardOverlay: @Composable (BoxScope.(Event) -> Unit)? = null,
    showEditDelete: Boolean = false,
    onEdit: (Event) -> Unit = {},
    onDelete: (Event) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    detailsViewModel: EventDetailsViewModel = hiltViewModel()
) {
    // --- SHARED REPORTING STATE ---
    var showReportDialog by remember { mutableStateOf(false) }
    var eventIdToReport by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 1. Render Header
        if (headerContent != null) {
            headerContent()
        }

        // 2. Render Empty State
        if (events.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .padding(16.dp)
                        .height(200.dp), // Height constraint prevents layout crash
                    contentAlignment = Alignment.Center
                ) {
                    Text("No events found.", color = Color.Gray)
                }
            }
        } else {
            // 3. Render Event Cards
            items(events) { event ->
                Box {
                    EventCard(
                        event = event,
                        onClick = { onEventClick(event.id) },
                        onReport = {
                            eventIdToReport = event.id
                            showReportDialog = true
                        },
                        showEditDelete = showEditDelete,
                        onEdit = { onEdit(event) },
                        onDelete = { onDelete(event) },
                        onProfileClick = onProfileClick
                    )

                    // Render custom overlay (like Delete button)
                    if (cardOverlay != null) {
                        cardOverlay(event)
                    }
                }
            }
        }
    }

    // --- REPORT DIALOG ---
    if (showReportDialog) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { reason ->
                if (eventIdToReport != null) {
                    detailsViewModel.reportEvent(eventIdToReport!!, reason, context)
                }
                showReportDialog = false
            }
        )
    }
}

/**
 * Standard Event Card used across Home, MyEvents, and JoinedEvents.
 */
@Composable
fun EventCard(
    event: Event,
    onClick: (String) -> Unit = {},
    onReport: () -> Unit = {},
    showEditDelete: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onProfileClick: (String) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(event.id) }
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP LAYER - Profile, Username, and Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Profile Picture and Username
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onProfileClick(event.hostId) }
                ) {
                    // Profile Picture or Initials
                    if (event.hostAvatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = event.hostAvatarUrl,
                            contentDescription = "Host Profile",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Show initials if no avatar
                        val displayName = event.hostName.ifEmpty { "Unknown" }
                        val initials = displayName
                            .split(" ")
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .joinToString("")
                            .ifEmpty { "?" }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Username
                    Text(
                        text = event.hostName.ifEmpty { "Unknown" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }

                // Vertical Menu Button
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            "Options",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Black
                        )
                    }

                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (showEditDelete) {
                            // Edit option for event host
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            // Delete option for event host
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                        // Report option (always available)
                        DropdownMenuItem(
                            text = { Text("Report", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onReport()
                            },
                            leadingIcon = { Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

        // IMAGE (Full width, no rounded corners)
        if (event.imageUrl.isNullOrEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color.LightGray)
            )
        } else {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = event.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                contentScale = ContentScale.Crop
            )
        }

        // CONTENT
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                // Status & RSVP
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatusTag(event.computedStatus)
                    if (event.userRsvp == "attending") {
                        AssistChip(
                            onClick = {},
                            label = { Text("Attending") },
                            colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(event.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                // Vouch Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${event.vouchCount} Vouches",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                }

            // Details
            Text("${event.date} · ${event.time}", fontSize = 14.sp, color = Color.Gray)
            Text(event.location, fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(event.description, fontSize = 13.sp, maxLines = 2)
        }

        // Divider at the bottom for separation
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}

/**
 * Reusable Status Tag (Upcoming, Past, etc.)
 */
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

@Composable
fun ReportDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    val options = listOf("Spam", "Inappropriate Content", "Fake Event", "Other")
    var selectedOption by remember { mutableStateOf(options[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Event") },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { selectedOption = option }.padding(8.dp)
                    ) {
                        RadioButton(selected = (option == selectedOption), onClick = { selectedOption = option })
                        Text(text = option)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedOption) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Report") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}