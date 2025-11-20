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
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.launch
import androidx.compose.ui.graphics.vector.path
import android.provider.MediaStore.Audio.Media
import androidx.navigation.NavController

/**
 * MyEventsScreen is a stateful composable that displays the events created by the current user.
 * It allows users to see their list of events and provides a way to create new ones.
 *
 * @param navController The NavController used for navigating to other screens, like event details.
 * @param viewModel The MyEventsViewModel instance that provides the state (list of events) and business logic.
 */
@Composable
fun MyEventsScreen(
    viewModel: MyEventsViewModel = hiltViewModel(),
    navController : NavController
) {

    // --- STATE MANAGEMENT ---
    var showCreateEventDialog by remember { mutableStateOf(false) }

    // Observe the list of events from the ViewModel
    val myEvents by viewModel.myEvents.collectAsState()


    // --- UI ---
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
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {

            // LazyColumn to display the list of events
            LazyColumn(
                modifier = Modifier.fillMaxSize(), // It fills the Box
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("My Events", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                // if event empty show messages to add event
                if (myEvents.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "No events yet.",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Tap the '+' button to create your first event!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    // Display the list of events
                    items(myEvents) { event ->
                        EventCard(event) { id ->
                            // Navigate to the event details screen when a card is clicked
                            navController.navigate("event/$id")
                        }
                    }
                }
            }

            // Show the CreateEventDialog if showCreateEventDialog is true
            if (showCreateEventDialog) {
                CreateEventDialog(
                    onDismiss = { showCreateEventDialog = false },
                    onCreateEvent = { title, description, location, timestamp, max, tags, imageUri ->
                        viewModel.createEvent(title, description, location, timestamp, max, tags, imageUri)
                        showCreateEventDialog = false
                    }
                )
            }
        }
    }
}

/**
 * A dialog composable for creating a new event. It contains a form with various fields.
 * This is a stateless composable, where all state is hoisted to the caller.
 *
 * @param onDismiss A lambda function to be invoked when the dialog is dismissed.
 * @param onCreateEvent A lambda function to be invoked when the user confirms event creation, passing all form data.
 */
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
    // handle image upload, receives URI of the selected image
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia(),
        onResult = { uri ->
            // When the user selects an image, the result URI is stored here
            imageUri = uri
        }
    )

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



    // the main AlertDialog that contains the entire scrollable form.
    // this will be triggered when we click create new event
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Event") },
        text = {
            //  LazyColumn to make the form scrollable
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Event title
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // --- Description ---
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Date and Time Pickers ---
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
                // location
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        leadingIcon = { Icon(Icons.Outlined.LocationOn, "Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // max attendees
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

                // Image Upload, if no image uploaded use Placeholder
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Image, "Upload Image", modifier = Modifier.size(40.dp), tint = Color.Gray)
                        Text("Upload event image or select from gallery", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(PickVisualMedia.ImageOnly) // Limit to images
                            )
                        }) {
                            // Show different text depending on whether an image is selected
                            Text(if (imageUri == null) "Select Image" else "Change Image")
                        }

                        imageUri?.let {
                            Text("Selected: ${it.path?.substringAfterLast('/')}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                }
                // tags, for finding relatable events
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

                    // Combine the selected date and time into a single Timestamp
                    val finalTimestamp = combineDateAndTime(
                        selectedDateMillis,
                        selectedTimeHour,
                        selectedTimeMinute
                    )
                    // if no max attendess, default to 100
                    val maxAttendeesInt = maxAttendees.toIntOrNull() ?: 100 // Default to 100
                    val tagsList = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    // Call the onCreateEvent callback with the collected data
                    if (finalTimestamp != null) {
                        onCreateEvent(
                            title,
                            description,
                            location,
                            finalTimestamp,
                            maxAttendeesInt,
                            tagsList,
                            imageUri
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
/**
 * A private helper function to combine separate date and time values into a single Firebase Timestamp.
 * This is crucial for accurately storing event start times.
 */
private fun combineDateAndTime(
    dateMillis: Long?,
    hour: Int?,
    minute: Int?
): Timestamp? {
    if (dateMillis == null || hour == null || minute == null) {
        return null
    }

    // Use a Calendar instance to correctly assemble the date and time.
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

/**
 * A custom date picker dialog.
 */
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

