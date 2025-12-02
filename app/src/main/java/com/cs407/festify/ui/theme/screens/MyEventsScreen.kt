package com.cs407.festify.ui.theme.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.cs407.festify.ui.theme.viewmodels.JoinedEventsViewModel
import java.util.*

// ==========================================
// 1. HOSTED EVENTS SCREEN (Logic + Buttons)
// ==========================================
@Composable
fun MyEventsScreen(
    viewModel: MyEventsViewModel = hiltViewModel(),
    navController: NavController,
    detailsViewModel: EventDetailsViewModel = hiltViewModel()
) {
    // --- STATE & OBSERVERS ---
    var showCreateEventDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<Event?>(null) }
    val myEvents by viewModel.myEvents.collectAsState()
    val context = LocalContext.current


    // --- UI STRUCTURE ---
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateEventDialog = true }) {
                Icon(Icons.Filled.Add, "Add")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {

            // Using SmartEventList
            SmartEventList(
                events = myEvents,
                onEventClick = { id -> navController.navigate("event/$id") },
                headerContent = {
                    item { Text("Events Hosted", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                },
                cardOverlay = { event ->
                    // Delete Button Logic
                    IconButton(
                        onClick = { eventToDelete = event },
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color.White)
                    }

                },
                onReportSubmit = { eventId, reason ->
                    detailsViewModel.reportEvent(eventId, reason, context)
                }

            )
        }

        // --- DIALOGS (Create & Delete) ---
        if (eventToDelete != null) {
            AlertDialog(
                onDismissRequest = { eventToDelete = null },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to delete '${eventToDelete?.title}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            eventToDelete?.let { viewModel.deleteEvent(it) }
                            eventToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { eventToDelete = null }) { Text("Cancel") } }
            )
        }

        if (showCreateEventDialog) {
            CreateEventDialog(
                onDismiss = { showCreateEventDialog = false },
                onCreateEvent = { title, desc, loc, time, max, tags, uri ->
                    viewModel.createEvent(title, desc, loc, time, max, tags, uri)
                    showCreateEventDialog = false
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
        startDateTime: Timestamp,
        maxAttendees: Int,
        tags: List<String>,
        imageUri: Uri?
    ) -> Unit
) {
    // --- Form States ---
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var maxAttendees by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia(),
        onResult = { uri -> imageUri = uri }
    )

    // --- Date/Time Picker States ---
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedTimeHour by remember { mutableStateOf<Int?>(null) }
    var selectedTimeMinute by remember { mutableStateOf<Int?>(null) }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.US) }


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
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        leadingIcon = { Icon(Icons.Outlined.LocationOn, "Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                        onCreateEvent(title, description, location, finalTimestamp, maxAttendeesInt, tagsList, imageUri)
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