package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppSettings
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.example.data.model.formatDeviceDisplayName
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.DateUtils
import com.example.util.WholesalePdfGenerator
import com.example.util.WholesalePdfItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Filter out due collection records that are not actual devices
 */
private fun isDueTransaction(phone: PhoneItem): Boolean {
    val searchBlob = "${phone.brand} ${phone.model} ${phone.customerName} ${phone.notes} ${phone.customerPhone} ${phone.paymentMethod} ${phone.soldByStaff} ${phone.variant} ${phone.imei}".lowercase()
    return searchBlob.contains("razu") ||
            searchBlob.contains("due collection") ||
            searchBlob.contains("due payment") ||
            phone.brand.trim().equals("Due Collection", ignoreCase = true) ||
            phone.model.contains("Due Collection", ignoreCase = true) ||
            phone.notes.contains("Due Collection", ignoreCase = true)
}

/**
 * Internal state holder for each device in the wholesale selection dialog
 */
private class WholesaleDeviceRowState(
    val phone: PhoneItem,
    initialChecked: Boolean = true,
    initialPrice: Double,
    initialBoxStatus: String
) {
    var isChecked by mutableStateOf(initialChecked)
    var boxStatus by mutableStateOf(initialBoxStatus)

    val baseCost: Double
        get() = if (phone.totalCostBdt > 0.0) phone.totalCostBdt else if (phone.buyingPriceBdt > 0.0) phone.buyingPriceBdt else 0.0

    val defaultMargin: Double
        get() = WholesalePdfGenerator.calculateDefaultMargin(baseCost)

    var profitText by mutableStateOf(defaultMargin.toLong().toString())
    var priceText by mutableStateOf(initialPrice.toLong().toString())

    val currentPrice: Double
        get() = priceText.toDoubleOrNull() ?: 0.0

    val addedProfit: Double
        get() = profitText.toDoubleOrNull() ?: 0.0

    val calculatedAutoPrice: Double
        get() = WholesalePdfGenerator.calculateDefaultWholesalePrice(phone)

    fun onProfitChanged(newProfitStr: String) {
        val digitsOnly = newProfitStr.filter { it.isDigit() }
        profitText = digitsOnly
        val p = digitsOnly.toDoubleOrNull() ?: 0.0
        val target = WholesalePdfGenerator.roundToNearest500(baseCost + p)
        priceText = target.toLong().toString()
    }

    fun onPriceChanged(newPriceStr: String) {
        val digitsOnly = newPriceStr.filter { it.isDigit() }
        priceText = digitsOnly
        if (digitsOnly.isNotEmpty()) {
            val p = digitsOnly.toDoubleOrNull() ?: 0.0
            val diff = (p - baseCost).coerceAtLeast(0.0)
            profitText = diff.toLong().toString()
        }
    }

    fun resetToDefaultProfit() {
        profitText = defaultMargin.toLong().toString()
        priceText = calculatedAutoPrice.toLong().toString()
    }
}

// Canonical priority sorting for common mobile phone brands
private val canonicalBrandOrder = listOf(
    "Apple", "iPhone", "Samsung", "Vivo", "Xiaomi", "Redmi", "OnePlus",
    "Google Pixel", "Google", "Oppo", "Realme", "Motorola", "Nothing", "Honor", "Huawei", "Sony"
)

@Composable
fun WholesalePriceListDialog(
    phones: List<PhoneItem>,
    settings: AppSettings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val scope = rememberCoroutineScope()

    // 1. Filter in-stock devices
    val stockPhones = remember(phones) {
        val strictlyStock = phones.filter {
            (it.status == PhoneStatus.IN_STOCK.code || it.status.equals("STOCK", ignoreCase = true)) && !isDueTransaction(it)
        }
        val candidateList = if (strictlyStock.isNotEmpty()) strictlyStock else phones.filter {
            it.status != PhoneStatus.SOLD.code && !isDueTransaction(it)
        }
        // 2. Smart Duplicate Item Filtering:
        // If multiple identical devices exist in stock, display ONLY ONE entry by default.
        // Exception: Differences in Memory/Storage, Box Status, or Price are retained.
        WholesalePdfGenerator.filterSmartDuplicates(candidateList)
    }

    // 3. Build row states for each stock item
    val rowStates = remember(stockPhones) {
        stockPhones.map { phone ->
            val defaultPrice = WholesalePdfGenerator.calculateDefaultWholesalePrice(phone)
            val defaultBox = WholesalePdfGenerator.getBoxStatusString(phone)
            WholesaleDeviceRowState(
                phone = phone,
                initialChecked = true,
                initialPrice = defaultPrice,
                initialBoxStatus = defaultBox
            )
        }
    }

    // Search query
    var searchQuery by remember { mutableStateOf("") }

    // PDF generation state
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Filtered rows by search
    val filteredRowStates = remember(rowStates, searchQuery) {
        if (searchQuery.isBlank()) {
            rowStates
        } else {
            val q = searchQuery.trim().lowercase()
            rowStates.filter { state ->
                val name = "${state.phone.brand} ${state.phone.model}".lowercase()
                val color = state.phone.color.lowercase()
                val storage = state.phone.ramRom.lowercase()
                val imei = state.phone.imei.lowercase()
                val box = state.boxStatus.lowercase()
                name.contains(q) || color.contains(q) || storage.contains(q) || imei.contains(q) || box.contains(q)
            }
        }
    }

    // 4. Automatic Brand Categorization / Grouping:
    // Group and sort the stock items by Brand Name (e.g., Vivo, Xiaomi, iPhone, Samsung).
    val groupedByBrand = remember(filteredRowStates) {
        filteredRowStates
            .groupBy { it.phone.brand.trim().ifBlank { "Other" } }
            .toList()
            .sortedWith(Comparator { (b1, _), (b2, _) ->
                val idx1 = canonicalBrandOrder.indexOfFirst { it.equals(b1, ignoreCase = true) }
                val idx2 = canonicalBrandOrder.indexOfFirst { it.equals(b2, ignoreCase = true) }
                when {
                    idx1 != -1 && idx2 != -1 -> idx1.compareTo(idx2)
                    idx1 != -1 -> -1
                    idx2 != -1 -> 1
                    else -> b1.compareTo(b2, ignoreCase = true)
                }
            })
    }

    val selectedCount by remember {
        derivedStateOf { rowStates.count { it.isChecked } }
    }
    val totalCount = rowStates.size

    val allSelected by remember {
        derivedStateOf { rowStates.isNotEmpty() && rowStates.all { it.isChecked } }
    }

    // Success PDF Result Dialog
    if (showSuccessDialog && generatedPdfFile != null) {
        val pdfFile = generatedPdfFile!!
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onDismiss()
            },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Wholesale Price List PDF Ready!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    color = if (isDark) Color.White else Slate900
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Store: ${settings.shopName.ifBlank { "Zoha Trading" }}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Updated: ${DateUtils.getTodayDisplayDate()} • $selectedCount of $totalCount item",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Slate700
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "File: ${pdfFile.name}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFE2E8F0) else Slate900,
                            modifier = Modifier.padding(10.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "PDF has been saved to your device. You can now view or share it instantly with wholesalers.",
                        fontSize = 11.5.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Slate500,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        WholesalePdfGenerator.sharePdf(context, pdfFile)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("share_pdf_btn")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share PDF", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        WholesalePdfGenerator.viewPdf(context, pdfFile)
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("view_pdf_btn")
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View PDF")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = if (isDark) Color(0xFF1E293B) else CardBg
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF0F172A) else CardBg,
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 1. DIALOG HEADER (No "auto margin" subtitle line)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0284C7).copy(alpha = if (isDark) 0.3f else 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Wholesale Price List",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Wholesale Price List",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isDark) Color.White else Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("wholesale_dialog_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFF94A3B8) else Slate700
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. SEARCH BOX (Vertically centered text and "Search" placeholder)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isDark) Color(0xFF94A3B8) else Slate500,
                            modifier = Modifier.size(19.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search",
                                    fontSize = 13.5.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Slate500,
                                    maxLines = 1
                                )
                            }

                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isDark) Color.White else Slate900
                                ),
                                cursorBrush = SolidColor(if (isDark) Color(0xFF38BDF8) else PrimaryNavy),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wholesale_search_input")
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = if (isDark) Color(0xFF94A3B8) else Slate500,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. SELECTION & ACTION BAR (Select All / Reset Margins)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Select All Checkbox
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    val target = !allSelected
                                    rowStates.forEach { it.isChecked = target }
                                }
                                .padding(end = 6.dp)
                        ) {
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { isChecked ->
                                    rowStates.forEach { it.isChecked = isChecked }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF0284C7)
                                ),
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Select All ($selectedCount/$totalCount)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (isDark) Color.White else Slate900
                            )
                        }

                        // Reset All Prices to Auto Margins
                        TextButton(
                            onClick = {
                                rowStates.forEach { state ->
                                    state.resetToDefaultProfit()
                                }
                                Toast.makeText(context, "Prices reset to default margin formula!", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset Margins",
                                fontSize = 11.5.sp,
                                color = Color(0xFF0284C7),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 4. DEVICE LIST WITH BRAND HEADERS
                if (filteredRowStates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No devices match \"$searchQuery\"" else "No in-stock devices available in inventory",
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Slate500,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 6.dp)
                    ) {
                        groupedByBrand.forEach { (brandName, brandItems) ->
                            // Bold Brand Section Headline (e.g., "Vivo:", "Xiaomi:") strictly without count numbers
                            item(key = "brand_header_$brandName") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 2.dp, start = 4.dp)
                                ) {
                                    Text(
                                        text = "$brandName:",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy
                                    )
                                }
                            }

                            items(brandItems, key = { state -> state.phone.id }) { state ->
                                WholesaleItemCard(
                                    state = state,
                                    isDark = isDark
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(modifier = Modifier.height(8.dp))

                // 5. FOOTER ACTIONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Strictly displayed as "X of Y item" with NO unchecked items line below
                    Text(
                        text = "$selectedCount of $totalCount item",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Slate900
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Slate700)
                        }

                        Button(
                            onClick = {
                                val selectedItems = rowStates.filter { it.isChecked }
                                if (selectedItems.isEmpty()) {
                                    Toast.makeText(context, "Please select at least 1 device for the PDF list", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isGeneratingPdf = true
                                scope.launch {
                                    val pdfItems = selectedItems.map { state ->
                                        val mobileName = "${state.phone.brand} ${state.phone.model}".trim()
                                        WholesalePdfItem(
                                            id = state.phone.id,
                                            brand = state.phone.brand.trim().ifBlank { "Other" },
                                            mobileName = mobileName,
                                            color = state.phone.color.ifBlank { "Standard" },
                                            grade = WholesalePdfGenerator.formatGradeDisplay(state.phone.conditionGrade),
                                            storageRam = state.phone.ramRom.ifBlank { "Standard" },
                                            boxStatus = state.boxStatus,
                                            wholesalePrice = state.currentPrice
                                        )
                                    }

                                    val generatedFile = withContext(Dispatchers.IO) {
                                        WholesalePdfGenerator.generateWholesalePdf(
                                            context = context,
                                            items = pdfItems,
                                            settings = settings
                                        )
                                    }

                                    isGeneratingPdf = false

                                    if (generatedFile != null) {
                                        generatedPdfFile = generatedFile
                                        showSuccessDialog = true
                                    } else {
                                        Toast.makeText(context, "Error generating wholesale PDF document", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isGeneratingPdf && selectedCount > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("generate_wholesale_pdf_btn")
                        ) {
                            if (isGeneratingPdf) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Building PDF...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WholesaleItemCard(
    state: WholesaleDeviceRowState,
    isDark: Boolean
) {
    val phone = state.phone

    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isChecked) {
                if (isDark) Color(0xFF1E293B) else Color.White
            } else {
                if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFF8FAFC)
            }
        ),
        border = BorderStroke(
            1.dp,
            if (state.isChecked) (if (isDark) Color(0xFF334155) else CardBorder) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                state.isChecked = !state.isChecked
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            // Top Row: Checkbox on left, Mobile Name Headline, and Middle Spec Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = state.isChecked,
                    onCheckedChange = { state.isChecked = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0284C7)),
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("item_check_${phone.id}")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Mobile Name Headline without sequential numbering
                    val displayName = formatDeviceDisplayName(phone.brand, phone.model)
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = if (isDark) Color.White else Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Middle Specification Line: Storage: [RAM-ROM] . Grade: [Grade] . [Box Status]
                    val storageText = phone.ramRom.trim().replace("/", "-").ifBlank { "Standard" }
                    val gradeText = WholesalePdfGenerator.formatGradeDisplay(phone.conditionGrade)
                    val boxStatusText = WholesalePdfGenerator.formatBoxStatus(state.boxStatus)
                    val specLine = "Storage: $storageText  .  Grade: $gradeText  .  $boxStatusText"
                    Text(
                        text = specLine,
                        fontSize = 11.5.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Slate700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Row: Base Cost Button + Editable Profit Button/Field + Final Wholesale Price Field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                val baseInt = state.baseCost.toLong()

                // 1. Base Cost Button: Displays strictly the calculated Landing Cost (e.g., 10000 or 38000)
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .height(34.dp)
                        .clickable {
                            state.resetToDefaultProfit()
                            Toast.makeText(context, "Landing Base Cost: ৳$baseInt (Reset applied)", Toast.LENGTH_SHORT).show()
                        }
                        .testTag("wholesale_base_cost_btn_${phone.id}")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$baseInt",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Slate700,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(5.dp))

                // 2. Plus Symbol (+) neatly positioned between Base Cost and Profit buttons
                Text(
                    text = "+",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.width(5.dp))

                // 3. Editable Profit Button/Field: Pre-fills 5000/7000 with direct interactive editing
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFA7F3D0)),
                    modifier = Modifier
                        .width(62.dp)
                        .height(34.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BasicTextField(
                            value = state.profitText,
                            onValueChange = { input ->
                                state.onProfitChanged(input)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center,
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                            ),
                            cursorBrush = SolidColor(if (isDark) Color(0xFF34D399) else Color(0xFF059669)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wholesale_profit_input_${phone.id}")
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 4. Final Wholesale Price Edit Field (clean Surface container with no digit clipping)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF0F172A) else Color.White,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .width(112.dp)
                        .height(34.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "৳",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        BasicTextField(
                            value = state.priceText,
                            onValueChange = { input ->
                                state.onPriceChanged(input)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.End,
                                color = if (isDark) Color.White else Slate900
                            ),
                            cursorBrush = SolidColor(if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wholesale_price_input_${phone.id}")
                        )
                    }
                }
            }
        }
    }
}
