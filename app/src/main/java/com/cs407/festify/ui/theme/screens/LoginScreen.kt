package com.cs407.festify.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.cs407.festify.auth.EmailResult
import com.cs407.festify.auth.PasswordResult
import com.cs407.festify.auth.validateEmail
import com.cs407.festify.auth.validatePassword
import com.cs407.festify.auth.signIn

@Composable
fun ErrorText(error: String?, modifier: Modifier = Modifier) {
    if (!error.isNullOrEmpty()) {
        Text(
            text = error,
            color = Color.Red,
            textAlign = TextAlign.Center,
            modifier = modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Festify Login", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            ErrorText(error = errorMessage)
            Spacer(modifier = Modifier.height(12.dp))

            // Email field
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password field
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sign-in / Sign-up button
            Button(
                onClick = {
                    when (validateEmail(email)) {
                        EmailResult.Empty -> {
                            errorMessage = "Please enter an email address."
                            return@Button
                        }
                        EmailResult.Invalid -> {
                            errorMessage = "Invalid email format."
                            return@Button
                        }
                        else -> Unit
                    }

                    when (validatePassword(password)) {
                        PasswordResult.Empty -> {
                            errorMessage = "Please enter a password."
                            return@Button
                        }
                        PasswordResult.Short -> {
                            errorMessage = "Password must be at least 5 characters."
                            return@Button
                        }
                        PasswordResult.Invalid -> {
                            errorMessage = "Password must include uppercase, lowercase, and a number."
                            return@Button
                        }
                        else -> Unit
                    }

                    // Firebase sign-in or create-account
                    signIn(
                        email = email,
                        password = password,
                        onSuccess = {
                            errorMessage = null
                            onLoginSuccess()
                        },
                        onFailure = { error ->
                            errorMessage = error
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Login / Sign Up")
            }
        }
    }
}
