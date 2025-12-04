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
                onProfileClick = { userId -> navController.navigate("user/$userId") },
                headerContent = {
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Events Hosted", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                showEditDelete = true,
                onEdit = { event ->
                    eventToEdit = event
                    showCreateEventDialog = true
                },
                onDelete = { event ->
                    eventToDelete = event
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
        onProfileClick = { userId -> navController.navigate("user/$userId") },
        headerContent = {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Events Joined", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
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
    // --- 1. SETUP FORMATTERS ---
    // The DatePicker returns UTC. We must format it using UTC or it shifts by one day visually.
    val dateFormatter = remember {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
    }
    // Time picker is always local
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.US) }

    // --- 2. CALCULATE INITIAL STATE FOR EDIT MODE ---
    // We need to convert the saved Event Time (Local) back to a UTC Date for the picker initialization
    val initialUtcMillis = remember(eventToEdit) {
        eventToEdit?.startDateTime?.toDate()?.let { date ->
            val localCal = Calendar.getInstance().apply { time = date }

            // Create a UTC calendar with the same Year/Month/Day
            val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            utcCal.clear()
            utcCal.set(Calendar.YEAR, localCal.get(Calendar.YEAR))
            utcCal.set(Calendar.MONTH, localCal.get(Calendar.MONTH))
            utcCal.set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH))
            utcCal.timeInMillis
        }
    }

    // --- 3. FORM STATES (Reset when eventToEdit changes) ---
    var title by remember(eventToEdit) { mutableStateOf(eventToEdit?.title ?: "") }
    var description by remember(eventToEdit) { mutableStateOf(eventToEdit?.description ?: "") }
    var location by remember(eventToEdit) { mutableStateOf(eventToEdit?.location ?: "") }
    var latitude by remember(eventToEdit) { mutableStateOf(eventToEdit?.latitude) }
    var longitude by remember(eventToEdit) { mutableStateOf(eventToEdit?.longitude) }
    var maxAttendees by remember(eventToEdit) { mutableStateOf(eventToEdit?.maxAttendees?.toString() ?: "") }
    var tags by remember(eventToEdit) { mutableStateOf(eventToEdit?.tags?.joinToString(", ") ?: "") }
    var imageUri by remember(eventToEdit) { mutableStateOf<Uri?>(null) } // Reset image on edit open
    var showLocationPicker by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia(),
        onResult = { uri -> imageUri = uri }
    )

    // --- 4. PICKER STATES ---
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialUtcMillis
    )
    // Track selected date manually to update UI instantly
    var selectedDateMillis by remember(eventToEdit) { mutableStateOf(initialUtcMillis) }

    // Calculate Initial Time (Local)
    val initialTimeCal = remember(eventToEdit) {
        if (eventToEdit != null) {
            Calendar.getInstance().apply { time = eventToEdit.startDateTime!!.toDate() }
        } else null
    }
    var selectedTimeHour by remember(eventToEdit) { mutableStateOf(initialTimeCal?.get(Calendar.HOUR_OF_DAY)) }
    var selectedTimeMinute by remember(eventToEdit) { mutableStateOf(initialTimeCal?.get(Calendar.MINUTE)) }

    // Dialog Visibility States
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val timePickerState = rememberTimePickerState(
        initialHour = selectedTimeHour ?: 0,
        initialMinute = selectedTimeMinute ?: 0,
        is24Hour = false
    )

    // Location Summary String
    val selectedLatLng = latitude?.let { lat ->
        longitude?.let { lng -> LatLng(lat, lng) }
    }
    val locationSummary = selectedLatLng?.let {
        "Lat: ${"%.4f".format(it.latitude)}, Lng: ${"%.4f".format(it.longitude)}"
    } ?: "Tap \"Pick on Map\" to drop a pin"


    // --- DIALOG UI ---
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        TimePickerDialog(
            title = "Select Time",
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTimeHour = timePickerState.hour
                    selectedTimeMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        ) { TimePicker(state = timePickerState) }
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
        title = { Text(if (eventToEdit == null) "Create New Event" else "Edit Event") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Title
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Description
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Date & Time Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Date Button
                        Button(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.DateRange, "Date")
                            Spacer(Modifier.width(8.dp))
                            // Use the UTC formatter so "Dec 19 UTC" displays as "Dec 19"
                            val buttonText = selectedDateMillis?.let { dateFormatter.format(Date(it)) } ?: "Select Date"
                            Text(buttonText)
                        }
                        Spacer(Modifier.width(16.dp))
                        // Time Button
                        Button(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                            val buttonText = if (selectedTimeHour != null) {
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, selectedTimeHour!!)
                                    set(Calendar.MINUTE, selectedTimeMinute!!)
                                }
                                timeFormatter.format(cal.time)
                            } else "Select Time"
                            Text(buttonText)
                        }
                    }
                }

                // Location & Map Picker
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

                // Max Attendees
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

                // Image Upload
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Image, "Upload Image", modifier = Modifier.size(40.dp), tint = Color.Gray)
                        Text(
                            if (eventToEdit != null && imageUri == null) "Current image will be kept" else "Upload event image",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }) {
                            Text(if (imageUri == null) "Select Image" else "Change Image")
                        }
                        imageUri?.let { Text("Selected: ${it.path?.substringAfterLast('/')}", style = MaterialTheme.typography.bodySmall) }
                    }
                }

                // Tags
                item {
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Add Tags") },
                        placeholder = { Text("e.g., networking, party") },
                        leadingIcon = { Icon(Icons.Outlined.Style, "Tags") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Guidelines
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
            ) { Text(if (eventToEdit == null) "Create Event" else "Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun combineDateAndTime(
    dateMillis: Long?,
    hour: Int?,
    minute: Int?
): Timestamp? {
    if (dateMillis == null || hour == null || minute == null) {
        return null
    }

    val utcCalendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    utcCalendar.timeInMillis = dateMillis

    val localCalendar = Calendar.getInstance() // System default timezone (e.g., CST)
    localCalendar.clear()

    localCalendar.set(Calendar.YEAR, utcCalendar.get(Calendar.YEAR))
    localCalendar.set(Calendar.MONTH, utcCalendar.get(Calendar.MONTH))
    localCalendar.set(Calendar.DAY_OF_MONTH, utcCalendar.get(Calendar.DAY_OF_MONTH))

    localCalendar.set(Calendar.HOUR_OF_DAY, hour)
    localCalendar.set(Calendar.MINUTE, minute)
    localCalendar.set(Calendar.SECOND, 0)
    localCalendar.set(Calendar.MILLISECOND, 0)

    return Timestamp(localCalendar.time)
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
