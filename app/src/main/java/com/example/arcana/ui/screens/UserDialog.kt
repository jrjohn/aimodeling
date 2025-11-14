package com.example.arcana.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.arcana.data.model.User
import com.example.arcana.domain.model.EmailAddress
import com.example.arcana.domain.validation.UserValidator

// Predefined avatar options
private val AVATAR_OPTIONS = listOf(
    "https://reqres.in/img/faces/1-image.jpg",
    "https://reqres.in/img/faces/2-image.jpg",
    "https://reqres.in/img/faces/3-image.jpg",
    "https://reqres.in/img/faces/4-image.jpg",
    "https://reqres.in/img/faces/5-image.jpg",
    "https://reqres.in/img/faces/6-image.jpg",
    "https://reqres.in/img/faces/7-image.jpg",
    "https://reqres.in/img/faces/8-image.jpg"
)

@Composable
fun UserDialog(
    user: User? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    // Input state
    var firstName by remember { mutableStateOf(user?.firstName ?: "") }
    var lastName by remember { mutableStateOf(user?.lastName ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var avatar by remember { mutableStateOf(user?.avatar ?: AVATAR_OPTIONS[0]) }

    // Touched state - track if user has interacted with each field
    var firstNameTouched by remember { mutableStateOf(false) }
    var lastNameTouched by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }

    // Validation errors - computed as user types using derivedStateOf
    val firstNameError by remember {
        derivedStateOf {
            when {
                !firstNameTouched -> null
                firstName.isBlank() -> "First name is required"
                !UserValidator.isValidName(firstName) -> "First name is too long (max 100 characters)"
                else -> null
            }
        }
    }

    val lastNameError by remember {
        derivedStateOf {
            when {
                !lastNameTouched -> null
                lastName.isBlank() -> "Last name is required"
                !UserValidator.isValidName(lastName) -> "Last name is too long (max 100 characters)"
                else -> null
            }
        }
    }

    val emailError by remember {
        derivedStateOf {
            when {
                !emailTouched -> null
                email.isBlank() -> "Email is required"
                else -> {
                    EmailAddress.create(email).fold(
                        onSuccess = { null },
                        onFailure = { it.message }
                    )
                }
            }
        }
    }

    // Form is valid if all fields have no errors and at least one name is provided
    val isFormValid by remember {
        derivedStateOf {
            firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            email.isNotBlank() &&
            avatar.isNotBlank() &&
            firstNameError == null &&
            lastNameError == null &&
            emailError == null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (user == null) "Create New User" else "Update User",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (user != null) {
                    Text(
                        text = "User ID: ${user.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                        firstNameTouched = true
                    },
                    label = { Text("First Name *") },
                    placeholder = { Text("Enter first name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = firstNameError != null,
                    supportingText = firstNameError?.let {
                        { Text(it) }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = {
                        lastName = it
                        lastNameTouched = true
                    },
                    label = { Text("Last Name *") },
                    placeholder = { Text("Enter last name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = lastNameError != null,
                    supportingText = lastNameError?.let {
                        { Text(it) }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailTouched = true
                    },
                    label = { Text("Email *") },
                    placeholder = { Text("user@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = emailError != null,
                    supportingText = emailError?.let {
                        { Text(it) }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Avatar Picker
                Text(
                    text = "Select Avatar *",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Avatar Grid
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AVATAR_OPTIONS.chunked(4).forEach { rowAvatars ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowAvatars.forEach { avatarUrl ->
                                AvatarOption(
                                    avatarUrl = avatarUrl,
                                    isSelected = avatar == avatarUrl,
                                    onClick = { avatar = avatarUrl }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(firstName, lastName, email, avatar) },
                enabled = isFormValid
            ) {
                Text(if (user == null) "Create" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AvatarOption(
    avatarUrl: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    AsyncImage(
        model = avatarUrl,
        contentDescription = "Avatar option",
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}
