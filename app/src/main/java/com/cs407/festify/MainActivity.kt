package com.cs407.festify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.filled.Event
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.navArgument

import com.cs407.festify.ui.screens.ProfileScreen
import com.cs407.festify.ui.theme.screens.MyEventsScreen
import com.cs407.festify.ui.theme.screens.HomeScreen
import com.cs407.festify.ui.theme.screens.ChatListScreen
import com.cs407.festify.ui.theme.screens.ChatScreen
import com.cs407.festify.ui.theme.FestifyTheme
import com.cs407.festify.ui.theme.LocalDarkMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Chat : Screen("chat", "Chat")
    object Profile : Screen("profile", "Profile")
    object MyEvents : Screen("myevents", "My Events")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkModeState = remember { mutableStateOf(false) }
<<<<<<< Updated upstream
=======
            var isLoggedIn by remember { mutableStateOf(firebaseAuth.currentUser != null) }

            // Listen to auth state changes
            DisposableEffect(Unit) {
                val authStateListener = FirebaseAuth.AuthStateListener { auth ->
                    isLoggedIn = auth.currentUser != null
                }
                firebaseAuth.addAuthStateListener(authStateListener)

                onDispose {
                    firebaseAuth.removeAuthStateListener(authStateListener)
                }
            }

>>>>>>> Stashed changes
            CompositionLocalProvider(LocalDarkMode provides darkModeState) {
                FestifyTheme {
                    FestifyApp()
                }
            }
        }
    }
}

@Composable
fun FestifyApp() {
    val navController = rememberNavController()

    val items = listOf(
        Screen.Home,
        Screen.MyEvents,
        Screen.Chat,
        Screen.Profile
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    val icon = when (screen) {
                        Screen.Home -> Icons.Default.Home
                        Screen.MyEvents -> Icons.Default.Event
                        Screen.Chat -> Icons.AutoMirrored.Filled.Chat
                        Screen.Profile -> Icons.Default.Person
                    }

                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Chat.route) {
                ChatListScreen(navController)
            }
            composable(
                route = "chat/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                ChatScreen(eventName = eventId.replace("_", " ").replaceFirstChar { it.uppercase() })
            }
            composable(Screen.MyEvents.route) { MyEventsScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
        }
    }
}