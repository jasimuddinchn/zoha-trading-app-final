package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.AuthUtils
import com.example.util.BiometricAuthManager

/**
 * Enhanced Admin PIN & Biometric Verification Dialog.
 *
 * Supports dual-mode authentication:
 * 1. Automatically initiates system BiometricPrompt (Fingerprint / Face Unlock) on launch if hardware is supported.
 * 2. If Biometric succeeds, instantly proceeds with the protected Admin action.
 * 3. Fallback to PIN: If biometric is cancelled, fails, or is unsupported, allows Admin to enter PIN manually.
 */
@Composable
fun AdminPinDialog(
    title: String = "Admin Authentication",
    actionTitle: String = "Protected Admin Action",
    actionDescription: String = "Enter Admin PIN or use fingerprint to confirm.",
    actionIcon: ImageVector = Icons.Default.Lock,
    confirmButtonText: String = "Confirm & Proceed",
    isDangerAction: Boolean = false,
    autoLaunchBiometrics: Boolean = true,
    onDismiss: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onAuthorized: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val isDark = isAppInDarkTheme()

    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    val isBioSupported = remember(context) { AuthUtils.isBiometricSupported(context) }
    var biometricAttempted by remember { mutableStateOf(false) }

    fun triggerBiometrics() {
        if (activity != null && isBioSupported) {
            AuthUtils.authenticateAdmin(
                activity = activity,
                actionTitle = actionTitle.ifBlank { "Admin Verification" },
                onSuccess = {
                    onAuthorized()
                },
                onFallbackToPin = {
                    // Stays on dialog for manual PIN input
                },
                onError = {
                    // Stays on dialog for manual PIN input
                }
            )
        }
    }

    // Automatically launch Biometric authentication when popup opens
    LaunchedEffect(Unit) {
        if (autoLaunchBiometrics && isBioSupported && !biometricAttempted) {
            biometricAttempted = true
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) {
                            if (isDangerAction) Color(0xFFEF4444).copy(alpha = 0.25f) else Color(0xFF1E3A8A).copy(alpha = 0.35f)
                        } else {
                            (if (isDangerAction) Color(0xFFEF4444) else PrimaryNavy).copy(alpha = 0.1f)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = "Admin Security",
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isDark) Color.White else Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isBioSupported) "Fingerprint or PIN Required" else "PIN Verification Required",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Quick Fingerprint Re-trigger icon button if supported
                if (isBioSupported) {
                    Surface(
                        onClick = { triggerBiometrics() },
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = if (isDark) 0.20f else 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("admin_dialog_biometric_trigger_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = "Authenticate with Biometrics",
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Operation detail badge
                Surface(
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            tint = if (isDangerAction) Color(0xFFEF4444) else (if (isDark) Color(0xFF60A5FA) else PrimaryNavy),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = actionTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = if (isDark) Color.White else Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (actionDescription.isNotBlank()) {
                                Text(
                                    text = actionDescription,
                                    fontSize = 10.5.sp,
                                    color = if (isDark) Color(0xFFCBD5E1) else Slate700,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Prompt description
                Text(
                    text = if (isBioSupported) "Scan fingerprint or enter Admin PIN:" else "Enter Admin PIN to authorize:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFFE2E8F0) else Slate700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 8) {
                            pinText = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Admin PIN") },
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
                        .testTag("admin_security_pin_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // If biometric is supported, show quick fingerprint tap button
                if (isBioSupported) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        onClick = { triggerBiometrics() },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = if (isDark) 0.15f else 0.08f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = "Biometrics",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scan Fingerprint to Unlock",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (onVerifyPin(pinText)) {
                        onAuthorized()
                    } else {
                        errorMessage = "Incorrect Admin PIN! Authorization denied."
                        Toast.makeText(context, "Incorrect Admin PIN", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDangerAction) Color(0xFFEF4444) else (if (isDark) Color(0xFF2563EB) else PrimaryNavy)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("admin_security_confirm_btn")
            ) {
                Text(
                    text = confirmButtonText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("admin_security_cancel_btn")
            ) {
                Text(
                    text = "Cancel",
                    color = if (isDark) Color(0xFF94A3B8) else Slate700,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        containerColor = if (isDark) Color(0xFF1E293B) else CardBg
    )
}
