package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.FragmentActivity
import com.example.util.AuthUtils
import com.example.util.BiometricAuthManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBg
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme

@Composable
fun PinDialog(
    userName: String = "Jasim (Admin)",
    userRoleType: String = "ADMIN", // "ADMIN", "STAFF_1", "STAFF_2"
    defaultPinHint: String = "1234",
    isBengali: Boolean = false,
    onBiometricClick: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onPinSuccess: () -> Unit,
    onVerifyPin: (String) -> Boolean
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val isDark = isAppInDarkTheme()
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    val isAdmin = userRoleType == "ADMIN"
    val isBioSupported = remember(context) { BiometricAuthManager.isBiometricAvailable(context) }
    var biometricLaunched by remember { mutableStateOf(false) }

    fun triggerBiometrics() {
        if (onBiometricClick != null) {
            onBiometricClick()
        } else if (activity != null && isBioSupported) {
            AuthUtils.authenticateAdmin(
                activity = activity,
                actionTitle = "$userName Verification",
                onSuccess = onPinSuccess,
                onFallbackToPin = {}
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!biometricLaunched && (onBiometricClick != null || (isAdmin && isBioSupported))) {
            biometricLaunched = true
            triggerBiometrics()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "Zoha Trading",
                        modifier = Modifier
                            .size(28.dp)
                            .padding(end = 4.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDark) Color.White else Slate900
                    )
                }
                if (onBiometricClick != null || isBioSupported) {
                    IconButton(
                        onClick = { triggerBiometrics() },
                        modifier = Modifier.testTag("pin_dialog_biometric_btn")
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Authenticate with Biometrics",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "Enter Your PIN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFE2E8F0) else Slate700
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 8) {
                            pinText = it
                            errorMessage = null
                        }
                    },
                    label = { Text("PIN") },
                    placeholder = { Text("Enter PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle PIN visibility",
                                tint = if (isDark) Color(0xFF94A3B8) else Slate700
                            )
                        }
                    },
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_pin_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (onVerifyPin(pinText)) {
                        onPinSuccess()
                    } else {
                        errorMessage = "Incorrect PIN! Please enter the correct PIN."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isAdmin) (if (isDark) Color(0xFF2563EB) else PrimaryNavy) else Color(0xFF0284C7)),
                modifier = Modifier.testTag("owner_pin_submit_btn")
            ) {
                Text("Login / Unlock", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("pin_cancel_btn")
            ) {
                Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Slate700)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isDark) Color(0xFF1E293B) else CardBg
    )
}
