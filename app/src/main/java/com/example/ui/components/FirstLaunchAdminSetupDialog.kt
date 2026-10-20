package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
fun FirstLaunchAdminSetupDialog(
    adminName: String = "Jasim",
    onSetupComplete: (adminPin: String) -> Unit,
    onRestoreCloudBackup: (() -> Unit)? = null
) {
    var adminPinText by remember { mutableStateOf("") }
    var confirmPinText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isDark = isAppInDarkTheme()

    AlertDialog(
        onDismissRequest = { /* Prevent dismiss on first launch setup */ },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF1E3A8A) else PrimaryNavy,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Setup Security",
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Admin Security Setup",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = if (isDark) Color.White else Slate900
                    )
                    Text(
                        text = "First Launch PIN Configuration",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Set a secure 4-digit PIN for Admin ($adminName). This PIN protects buying costs, profit reports, and settings.",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else Slate700,
                            lineHeight = 16.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = adminPinText,
                    onValueChange = {
                        if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                            adminPinText = it
                            errorMessage = null
                        }
                    },
                    label = { Text("New Admin PIN (4-8 digits)") },
                    placeholder = { Text("e.g. 1234") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle visibility",
                                tint = if (isDark) Color(0xFF94A3B8) else Slate700
                            )
                        }
                    },
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("first_launch_admin_pin_input")
                )

                OutlinedTextField(
                    value = confirmPinText,
                    onValueChange = {
                        if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                            confirmPinText = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Confirm Admin PIN") },
                    placeholder = { Text("Re-enter Admin PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("first_launch_confirm_pin_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        when {
                            adminPinText.length < 4 -> {
                                errorMessage = "PIN must be at least 4 digits."
                            }
                            adminPinText != confirmPinText -> {
                                errorMessage = "PINs do not match. Please verify."
                            }
                            else -> {
                                onSetupComplete(adminPinText)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2563EB) else PrimaryNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("first_launch_save_pin_btn")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set PIN & Unlock Admin", color = Color.White, fontWeight = FontWeight.Bold)
                }

                if (onRestoreCloudBackup != null) {
                    OutlinedButton(
                        onClick = onRestoreCloudBackup,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("first_launch_restore_cloud_btn")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Existing Cloud Backup", color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = if (isDark) Color(0xFF1E293B) else CardBg
    )
}
