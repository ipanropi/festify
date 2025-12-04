package com.cs407.festify.ui.theme.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cs407.festify.data.model.Event
import com.cs407.festify.ui.theme.screens.components.SmartEventList
import com.cs407.festify.ui.theme.viewmodels.EventDetailsViewModel
import com.cs407.festify.ui.viewmodels.MyEventsViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.cs407.festify.ui.theme.viewmodels.JoinedEventsViewModel
import java.util.*
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

// ==========================================
// 1. HOSTED EVENTS SCREEN (Logic + Buttons)
// ==========================================
@Composable
fun MyEventsScreen(
    viewModel: MyEventsViewModel = hiltViewModel(),
    navController: NavController,
    detailsViewModel: EventDetailsViewModel = hiltViewModel()
) {
    // --- STATE ---
    var showCreateEventDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<Event?>(null) }

    var eventToEdit by remember { mutableStateOf<Event?>(null) }

    val myEvents by viewModel.myEvents.collectAsState()
    val context = LocalContext.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                eventToEdit = null // Ensure we are in "Create" mode
                showCreateEventDialog = true
            }) {
                Icon(Icons.Filled.Add, "Add")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            SmartEventList(
                events = myEvents,
                onEventClick = { id -> navController.navigate("event/$id") },
                headerContent = {
                    item { Text("Events Hosted", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                },
                // Custom Overlay with EDIT and DELETE buttons
                cardOverlay = { event ->
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        // --- EDIT BUTTON ---
                        IconButton(
                            onClick = {
                                eventToEdit = event // Set the event to pre-fill
                                showCreateEventDialog = true
                            },
                            modifier = Modifier
                                .padding(end = 8.dp) // Space between buttons
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, "Edit", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // --- DELETE BUTTON ---
                        IconButton(
                            onClick = { eventToDelete = event },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                onReportSubmit = { eventId, reason ->
                    detailsViewModel.reportEvent(eventId, reason, context)
                }
            )
        }

        // --- SMART DELETE DIALOG ---
        if (eventToDelete != null) {
            val hasAttendees = eventToDelete!!.attendees > 0

            AlertDialog(
                onDismissRequest = { eventToDelete = null },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                title = {
                    Text(
                        text = if (hasAttendees) "Active Event Warning" else "Confirm Deletion",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (hasAttendees) {
                            Text("This event has ${eventToDelete!!.attendees} attendee(s).")
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Deleting it will remove it from their app without notice.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text("Are you sure you want to permanently delete '${eventToDelete!!.title}'?")
                        }
                    }
                },
                // We use a Column to stack buttons neatly in the middle
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        if (hasAttendees) {
                            FilledTonalButton(
                                onClick = {
                                    eventToDelete?.let { viewModel.cancelEvent(it) }
                                    eventToDelete = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Mark as 'Cancelled'")
                            }
                        }


                        Button(
                            onClick = {
                                eventToDelete?.let { viewModel.deleteEvent(it) }
                                eventToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (hasAttendees) "Delete Anyway" else "Delete Event")
                        }


                        TextButton(
                            onClick = { eventToDelete = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close")
                        }
                    }
                }

            )
        }

        if (showCreateEventDialog) {
            CreateEventDialog(
                onDismiss = {
                    showCreateEventDialog = false
                    eventToEdit = null // Reset so next time it opens empty
                },
                eventToEdit = eventToEdit, // Pass the event to pre-fill the form!
                onCreateEvent = { title, desc, loc, lat, lng, time, max, tags, uri ->
                    if (eventToEdit == null) {
                        // CREATE MODE
                        viewModel.createEvent(title, desc, loc, lat, lng, time, max, tags, uri)
                    } else {
                        // EDIT MODE
                        viewModel.updateEvent(
                            eventToEdit!!.id,
                            title,
                            desc,
                            loc,
                            lat,
                            lng,
                            time,
                            max,
                            tags,
                            uri
                        )
                    }
                    showCreateEventDialog = false
                    eventToEdit = null
                }
            )
        }
    }
}

// ==========================================
// 2. JOINED EVENTS SCREEN (Simple List)
// ==========================================
@Composable
fun JoinedEventsScreen(
    navController: NavController,
    viewModel: JoinedEventsViewModel = hiltViewModel(),
    detailsViewModel: EventDetailsViewModel = hiltViewModel()
) {
    val joinedEvents by viewModel.joinedEvents.collectAsState()
    val context = LocalContext.current


    SmartEventList(
        events = joinedEvents,
        onEventClick = { id -> navController.navigate("event/$id") },
        headerContent = {
            item {
                Text("Events Joined", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        },
        onReportSubmit = { eventId, reason ->
            detailsViewModel.reportEvent(eventId, reason, context)
        }
    )
}

// ==========================================
// 3. HELPER DIALOGS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventDialog(
    onDismiss: () -> Unit,
    onCreateEvent: (
        title: String,
        description: String,
        location: String,
        latitude: Double?,
        longitude: Double?,
        startDateTime: Timestamp,
        maxAttendees: Int,
        tags: List<String>,
        imageUri: Uri?,

    ) -> Unit,
    eventToEdit: Event? = null,
) {
    // --- Form States ---
    var title by remember { mutableStateOf(eventToEdit?.title ?: "") }
    var description by remember { mutableStateOf(eventToEdit?.description ?: "") }
    var location by remember { mutableStateOf(eventToEdit?.location ?: "") }
    var latitude by remember { mutableStateOf(eventToEdit?.latitude) }
    var longitude by remember { mutableStateOf(eventToEdit?.longitude) }
    var maxAttendees by remember { mutableStateOf(eventToEdit?.maxAttendees?.toString() ?: "") }
    var tags by remember { mutableStateOf(eventToEdit?.tags?.joinToString(", ") ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showLocationPicker by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia(),
        onResult = { uri -> imageUri = uri }
    )

    // --- Date/Time Logic (Critical Fix) ---
    // 1. Get initial timestamp from event, or null
    val initialTimestamp = eventToEdit?.startDateTime?.toDate()

    // 2. Initialize DatePicker state
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialTimestamp?.time
    )
    var selectedDateMillis by remember { mutableStateOf(initialTimestamp?.time) }

    // 3. Initialize Time state
    // We need to extract hour/minute from the timestamp if it exists
    val initialCalendar = remember(initialTimestamp) {
        if (initialTimestamp != null) {
            Calendar.getInstance().apply { time = initialTimestamp }
        } else {
            null
        }
    }

    var selectedTimeHour by remember { mutableStateOf(initialCalendar?.get(Calendar.HOUR_OF_DAY)) }
    var selectedTimeMinute by remember { mutableStateOf(initialCalendar?.get(Calendar.MINUTE)) }

    // Dialog States
    var showDatePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTimeHour ?: 0,
        initialMinute = selectedTimeMinute ?: 0,
        is24Hour = false
    )
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.US) }
    val selectedLatLng = latitude?.let { lat ->
        longitude?.let { lng -> LatLng(lat, lng) }
    }
    val locationSummary = selectedLatLng?.let {
        "Lat: ${"%.4f".format(it.latitude)}, Lng: ${"%.4f".format(it.longitude)}"
    } ?: "Tap \"Pick on Map\" to drop a pin"


    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }


    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTimeHour = timePickerState.hour
                    selectedTimeMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            initialLocation = selectedLatLng,
            onDismissRequest = { showLocationPicker = false },
            onLocationSelected = { latLng ->
                latitude = latLng.latitude
                longitude = latLng.longitude
                showLocationPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Event") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.DateRange, "Date")
                            Spacer(Modifier.width(8.dp))
                            val buttonText = selectedDateMillis?.let { dateFormatter.format(Date(it)) } ?: "Select Date"
                            Text(buttonText)
                        }
                        Spacer(Modifier.width(16.dp))
                        Button(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                            val buttonText = if (selectedTimeHour != null && selectedTimeMinute != null) {
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, selectedTimeHour!!)
                                    set(Calendar.MINUTE, selectedTimeMinute!!)
                                }
                                timeFormatter.format(cal.time)
                            } else {
                                "Select Time"
                            }
                            Text(buttonText)
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location Description") },
                            leadingIcon = { Icon(Icons.Outlined.LocationOn, "Location") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = { showLocationPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Place, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (selectedLatLng == null) "Pick on Map" else "Update Map Pin")
                        }
                        Text(
                            text = locationSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = maxAttendees,
                        onValueChange = { maxAttendees = it.filter { it.isDigit() } },
                        label = { Text("Max Attendees") },
                        leadingIcon = { Icon(Icons.Outlined.PeopleOutline, "Max Attendees") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Image, "Upload Image", modifier = Modifier.size(40.dp), tint = Color.Gray)
                        Text("Upload event image or select from gallery", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }) {
                            Text(if (imageUri == null) "Select Image" else "Change Image")
                        }
                        imageUri?.let { Text("Selected: ${it.path?.substringAfterLast('/')}", style = MaterialTheme.typography.bodySmall) }
                    }
                }
                item {
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Add Tags") },
                        placeholder = { Text("e.g., networking, party, studygroup") },
                        leadingIcon = { Icon(Icons.Outlined.Style, "Tags") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text(
                        text = "By creating this event, you agree to our Community Guidelines",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTimestamp = combineDateAndTime(selectedDateMillis, selectedTimeHour, selectedTimeMinute)
                    val maxAttendeesInt = maxAttendees.toIntOrNull() ?: 100
                    val tagsList = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    if (finalTimestamp != null) {
                        onCreateEvent(
                            title,
                            description,
                            location,
                            latitude,
                            longitude,
                            finalTimestamp,
                            maxAttendeesInt,
                            tagsList,
                            imageUri
                        )
                    }
                    onDismiss()
                },
                enabled = title.isNotBlank() && location.isNotBlank() && selectedDateMillis != null && selectedTimeHour != null
            ) { Text("Create Event") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun combineDateAndTime(dateMillis: Long?, hour: Int?, minute: Int?): Timestamp? {
    if (dateMillis == null || hour == null || minute == null) return null
    val calendar = Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.HOUR_OF_DAY, hour)
        add(Calendar.MINUTE, minute)
    }
    return Timestamp(calendar.time)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    onDismissRequest: () -> Unit,
    confirmButton: @Composable (() -> Unit),
    dismissButton: @Composable (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = containerColor,
            modifier = Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 20.dp))
                content()
                Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.End) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

@Composable
fun LocationPickerDialog(
    initialLocation: LatLng?,
    onDismissRequest: () -> Unit,
    onLocationSelected: (LatLng) -> Unit,
) {
    val defaultLocation = LatLng(43.0731, -89.4012)
    var pendingLocation by remember(initialLocation) { mutableStateOf(initialLocation) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation ?: defaultLocation, 11f)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Drop a Pin",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap anywhere on the map to select the event location.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        onMapClick = { latLng ->
                            pendingLocation = latLng
                        }
                    ) {
                        pendingLocation?.let {
                            Marker(
                                state = MarkerState(position = it),
                                title = "Selected Location"
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                pendingLocation?.let {
                    Text(
                        text = "Selected: ${"%.4f".format(it.latitude)}, ${"%.4f".format(it.longitude)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                } ?: Text(
                    text = "No pin selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            pendingLocation?.let(onLocationSelected)
                        },
                        enabled = pendingLocation != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Use Pin")
                    }
                }
            }
        }
    }
}
