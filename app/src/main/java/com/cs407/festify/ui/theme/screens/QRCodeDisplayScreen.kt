package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cs407.festify.ui.theme.viewmodels.QRCodeViewModel

/**
 * Screen for displaying QR code for event check-in (host view)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCodeDisplayScreen(
    eventId: String,
    navController: NavController,
    viewModel: QRCodeViewModel = hiltViewModel()
) {
    val qrBitmap by viewModel.qrBitmap.collectAsState()
    val checkInCount by viewModel.checkInCount.collectAsState()
    val event by viewModel.event.collectAsState()

    LaunchedEffect(eventId) {
        viewModel.loadEventAndGenerateQR(eventId)
        viewModel.startAutoRefresh() // Refresh QR every 2 min
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAutoRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check-In QR Code") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Event title
            Text(
                text = event?.title ?: "",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Scan to Check In",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // QR Code Display
            Card(
                modifier = Modifier.size(300.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Live check-in counter
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Total Check-Ins:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "$checkInCount",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Info text
            Text(
                text = "QR code refreshes every 2 minutes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
