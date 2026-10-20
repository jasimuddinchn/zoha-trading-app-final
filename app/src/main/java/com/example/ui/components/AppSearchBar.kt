package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate900
import com.example.ui.theme.isAppInDarkTheme

@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search brand, model, IMEI, or date...",
    testTag: String = "app_search_bar",
    onClearClick: () -> Unit = { onQueryChange("") },
    onSearchAction: (() -> Unit)? = null,
    onScanImeiClick: (() -> Unit)? = null
) {
    val isDark = isAppInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = if (isFocused) {
        if (isDark) Color(0xFF38BDF8) else PrimaryNavy
    } else {
        if (isDark) Color(0xFF4A5568) else Color(0xFFE2E8F0)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF2D3748) else Color.White,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isFocused) 2.dp else 0.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Icon
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (isFocused) {
                    if (isDark) Color(0xFF38BDF8) else PrimaryNavy
                } else {
                    if (isDark) Color(0xFFA0AEC0) else Color(0xFF64748B)
                },
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Text input container with placeholder
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFFA0AEC0) else Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White else Slate900
                    ),
                    cursorBrush = SolidColor(if (isDark) Color(0xFF38BDF8) else PrimaryNavy),
                    interactionSource = interactionSource,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (onSearchAction != null) ImeAction.Search else ImeAction.Default
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSearchAction?.invoke() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag)
                )
            }

            // Scanner icon button
            if (onScanImeiClick != null) {
                IconButton(
                    onClick = onScanImeiClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("${testTag}_scan_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan IMEI",
                        tint = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Clear 'X' button when query is not empty
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClearClick,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("${testTag}_clear")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = if (isDark) Color(0xFFA0AEC0) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
