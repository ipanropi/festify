package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.cs407.festify.data.model.Event
import com.cs407.festify.ui.viewmodels.MyEventsViewModel
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Style
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Composable
fun MyEventsScreen(
    // Get the ViewModel that's automatically created
    viewModel: MyEventsViewModel = hiltViewModel()
) {

    var showCreateEventDialog by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(16.dp))
    // Observe the list of events from the ViewModel
    val myEvents by viewModel.myEvents.collectAsState()


    val event = Event(
        id = "1",
        title = "Tech Startup Networking",
        description = "Connect with fellow entrepreneurs and innovators. Great opportunity to share ideas, find mentors, and build your startup network!",
        imageUrl = "https://images.unsplash.com/photo-1551836022-4c4c79ecde51",
        date = "Nov 5, 2025",
        time = "6:00 PM - 9:00 PM",
        location = "Innovation Hub, Downtown",
        attendees = 42,
        maxAttendees = 80,
        status = "upcoming",
        userRsvp = "attending"
    )


    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showCreateEventDialog = true
                },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Event")
            }
        }
    ) { innerPadding ->


        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("My Events", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            EventCard(event = event)
            Spacer(modifier = Modifier.height(16.dp))
        }


        if (showCreateEventDialog) {
            CreateEventDialog(
                onDismiss = { showCreateEventDialog = false },

                onCreateEvent = { title, description, location, timestamp, max, tags ->

                    viewModel.createEvent(title, description, location, timestamp, max, tags)
                    showCreateEventDialog = false
                }
            )
        }
    }
}

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
        tags: List<String>
    ) -> Unit
) {
    // --- Form States ---
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var maxAttendees by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    // --- Date/Time Picker States ---
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedTimeHour by remember { mutableStateOf<Int?>(null) }
    var selectedTimeMinute by remember { mutableStateOf<Int?>(null) }

    // --- Date/Time Formatter for the Button Text ---
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.US) }


    // - The DatePickerDialog Composable ---
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
            //  LazyColumn to make the form scrollable
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

                // --- 8. Date and Time Pickers ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.DateRange, "Date")
                            Spacer(Modifier.width(8.dp))
                            val buttonText = selectedDateMillis?.let {
                                dateFormatter.format(Date(it))
                            } ?: "Select Date"
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
                        onValueChange = { maxAttendees = it.filter { it.isDigit() } }, // Only allow numbers
                        label = { Text("Max Attendees") },
                        leadingIcon = { Icon(Icons.Outlined.PeopleOutline, "Max Attendees") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Image Upload Placeholder
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Image, "Upload Image", modifier = Modifier.size(40.dp), tint = Color.Gray)
                        Text("Upload event image or select from gallery", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { /* TODO: Launch image picker */ }) {
                            Text("Choose Image")
                        }
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

                // Community Guidelines ---
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
                    val finalTimestamp = combineDateAndTime(
                        selectedDateMillis,
                        selectedTimeHour,
                        selectedTimeMinute
                    )
                    val maxAttendeesInt = maxAttendees.toIntOrNull() ?: 100 // Default to 100
                    val tagsList = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    if (finalTimestamp != null) {
                        onCreateEvent(
                            title,
                            description,
                            location,
                            finalTimestamp,
                            maxAttendeesInt,
                            tagsList
                        )
                    }
                    onDismiss()
                },
                enabled = title.isNotBlank() && location.isNotBlank() && selectedDateMillis != null && selectedTimeHour != null
            ) {
                Text("Create Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// --- Helper Function to combine results ---
private fun combineDateAndTime(
    dateMillis: Long?,
    hour: Int?,
    minute: Int?
): Timestamp? {
    if (dateMillis == null || hour == null || minute == null) {
        return null
    }

    val calendar = Calendar.getInstance().apply {
        timeInMillis = dateMillis

        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)

        // Add the selected hour and minute
        add(Calendar.HOUR_OF_DAY, hour)
        add(Calendar.MINUTE, minute)
    }
    return Timestamp(calendar.time)
}

//  Helper Composable for Time Picker
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
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                content()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

