package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBg
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme
import kotlinx.coroutines.delay

@Composable
fun CustomInputPopupDialog(
    title: String,
    label: String = "Custom Value",
    placeholder: String = "",
    initialValue: String = "",
    confirmButtonText: String = "Add / Save",
    dismissButtonText: String = "Cancel",
    testTagPrefix: String = "custom_popup",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    var isBlankError by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val isDark = isAppInDarkTheme()

    LaunchedEffect(Unit) {
        delay(150)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Slate900
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        if (it.isNotBlank()) isBlankError = false
                    },
                    label = { Text(label) },
                    placeholder = { if (placeholder.isNotBlank()) Text(placeholder) },
                    singleLine = true,
                    isError = isBlankError,
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("${testTagPrefix}_input")
                )
                if (isBlankError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Value cannot be empty",
                        color = Color(0xFFDC2626),
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = text.trim()
                    if (trimmed.isBlank()) {
                        isBlankError = true
                    } else {
                        onConfirm(trimmed)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2563EB) else PrimaryNavy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("${testTagPrefix}_save_btn")
            ) {
                Text(
                    text = confirmButtonText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("${testTagPrefix}_cancel_btn")
            ) {
                Text(
                    text = dismissButtonText,
                    color = if (isDark) Color(0xFF94A3B8) else Slate700,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isDark) Color(0xFF1E293B) else CardBg
    )
}
