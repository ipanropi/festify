package com.cs407.festify.ui.screens

import android.graphics.drawable.Icon
import android.media.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // === PROFILE CARD ===
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Avatar (Initials)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEAEAEA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(uiState.initials, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(uiState.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(uiState.email, color = Color.Gray, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(16.dp))

                // Profile Stats
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProfileStat(uiState.eventsAttended.toString(), "Events Attended")
                    ProfileStat(uiState.eventsHosted.toString(), "Events Hosted")
                    ProfileStat(uiState.rating.toString(), "Rating")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { /* TODO: Navigate to Edit Profile Screen */ },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF9F9F9),
                        contentColor = Color(0xFF2E2E2E)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF2E2E2E).copy(alpha = 0.1f)),
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Profile")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === INFO CARDS ===
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(uiState.upcomingEvents.toString(), "Upcoming Events", Modifier.weight(1f), icon = Icons.Default.Event, contentdesc = "Events", color = 0xFFA7C7E7)
            InfoCard(uiState.connections.toString(), "Connections", Modifier.weight(1f), icon = Icons.Outlined.Person, contentdesc = "Person", color = 0xFFB8E0D2)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === RECENT ACHIEVEMENTS ===
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Recent Achievements", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                uiState.achievements.forEach {
                    AchievementItem(it.title, it.description, it.isNew)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === SETTINGS ===
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Settings", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                SettingSwitch(
                    title = "Dark Mode",
                    subtitle = "Toggle dark theme",
                    checked = uiState.darkMode,
                    onCheckedChange = { viewModel.toggleDarkMode() },
                    icon = Icons.Outlined.DarkMode,
                    contentdesc = "Dark Mode"
                )
                SettingSwitch(
                    title = "Push Notifications",
                    subtitle = "Get event updates",
                    checked = uiState.pushNotifications,
                    onCheckedChange = { viewModel.toggleNotifications() },
                    icon = Icons.Outlined.Notifications,
                    contentdesc = "Notifications"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "More Settings",
                        fontWeight = FontWeight.Medium
                    )
                }

            }
        }
    }
}

@Composable
fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Color.Gray, fontSize = 13.sp)
    }
}

@Composable
fun InfoCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentdesc: String,
    color: Long
    ) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(vertical = 8.dp)
                .padding(top = 4.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentdesc, modifier = Modifier.size(36.dp), tint = Color(color))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(label, color = Color.Gray, fontSize = 13.sp)
        }
    }
}

@Composable
fun AchievementItem(title: String, subtitle: String, isNew: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Medium, lineHeight = 12.sp)
            Text(subtitle, color = Color.Gray, fontSize = 13.sp, lineHeight = 12.sp)
        }
        if (isNew) {
            Text(
                "New",
                color = Color(0xFF2E2E2E),
                fontSize = 12.sp,
                modifier = Modifier
                    .background(Color(0xFFB8E0D2), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    contentdesc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = contentdesc)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(title, fontWeight = FontWeight.Medium, lineHeight = 12.sp)
                Text(subtitle, color = Color.Gray, fontSize = 13.sp, lineHeight = 12.sp)
            }

        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
