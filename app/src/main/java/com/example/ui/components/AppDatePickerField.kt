package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerField(
    label: String = "Date",
    selectedDate: String, // "YYYY-MM-DD" or formatted
    onDateSelected: (String) -> Unit,
    testTag: String = "date_picker_field",
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    var showDatePicker by remember { mutableStateOf(false) }
    val displayDate = remember(selectedDate) {
        DateUtils.formatDisplayDate(selectedDate)
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayDate,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            label = {
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
            },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Select Date from Calendar",
                        tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = appTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag(testTag)
        )
        // Clickable transparent overlay so clicking anywhere on the textfield opens calendar
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showDatePicker = true
                }
        )
    }

    if (showDatePicker) {
        val initialMillis = remember(selectedDate) {
            DateUtils.parseDateToMillis(selectedDate)
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat(DateUtils.STORAGE_DATE_PATTERN, Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            onDateSelected(sdf.format(Date(millis)))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select", fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = if (isDark) Color(0xFF2563EB) else PrimaryNavy,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = if (isDark) Color(0xFF38BDF8) else PrimaryNavy
                )
            )
        }
    }
}
