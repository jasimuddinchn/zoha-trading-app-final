package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BATTERY_HEALTH_OPTIONS
import com.example.data.model.BRAND_OPTIONS
import com.example.data.model.COLOR_OPTIONS
import com.example.data.model.COMMON_PHONE_MODELS
import com.example.data.model.ConditionGrade
import com.example.data.model.ITEM_CONDITION_OPTIONS
import com.example.data.model.PACKAGING_OPTIONS
import com.example.data.model.PHONE_CONDITION_STATUS_OPTIONS
import com.example.data.model.PHONE_GRADE_OPTIONS
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.example.data.model.STORAGE_OPTIONS
import com.example.data.model.UserRole
import com.example.data.model.VARIANT_OPTIONS
import com.example.data.model.cleanModelForBrand
import com.example.data.model.getDefaultStorageForBrand
import com.example.data.model.getModelsForBrand
import com.example.data.model.getStorageOptionsForBrand
import com.example.ui.components.CustomInputPopupDialog
import com.example.ui.components.ImeiCameraScannerDialog
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.StockGreen
import com.example.ui.theme.TransitAmber
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme
import com.example.viewmodel.PhoneHisabViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPhoneDialog(
    currentRole: UserRole,
    rmbRate: Double,
    isBengali: Boolean = false,
    existingPhones: List<PhoneItem> = emptyList(),
    onDismiss: () -> Unit,
    onAddPhone: (
        brand: String,
        model: String,
        imei: String,
        imei2: String,
        color: String,
        ramRom: String,
        variant: String,
        batteryHealth: String,
        conditionGrade: String,
        itemCondition: String,
        packaging: String,
        entryDate: String,
        status: String,
        notes: String,
        buyingPriceRmb: Double,
        buyingPriceBdt: Double,
        shippingCostBdt: Double,
        otherCostBdt: Double,
        desiredProfitBdt: Double
    ) -> Unit
) {
    val isAdmin = currentRole == UserRole.ADMIN

    var dynamicBrands by remember { mutableStateOf(BRAND_OPTIONS) }
    var brand by remember { mutableStateOf("Apple") }
    var brandExpanded by remember { mutableStateOf(false) }
    var showCustomBrandDialog by remember { mutableStateOf(false) }

    var model by remember { mutableStateOf("") }
    var modelExpanded by remember { mutableStateOf(false) }
    var showCustomModelDialog by remember { mutableStateOf(false) }

    var imei by remember { mutableStateOf("") }
    var imei2 by remember { mutableStateOf("") }
    var showCameraScanner by remember { mutableStateOf(false) }

    var dynamicColors by remember { mutableStateOf(COLOR_OPTIONS) }
    var color by remember { mutableStateOf("Black") }
    var colorExpanded by remember { mutableStateOf(false) }
    var showCustomColorDialog by remember { mutableStateOf(false) }

    var dynamicStorage by remember { mutableStateOf(getStorageOptionsForBrand("Apple")) }
    var ramRom by remember { mutableStateOf(getDefaultStorageForBrand("Apple")) }
    var ramRomExpanded by remember { mutableStateOf(false) }
    var showCustomStorageDialog by remember { mutableStateOf(false) }

    var dynamicVariants by remember { mutableStateOf(VARIANT_OPTIONS) }
    var variant by remember { mutableStateOf("Global") }
    var variantExpanded by remember { mutableStateOf(false) }
    var showCustomVariantDialog by remember { mutableStateOf(false) }

    var dynamicBattery by remember { mutableStateOf(BATTERY_HEALTH_OPTIONS) }
    var batteryHealth by remember { mutableStateOf("100%") }
    var batteryExpanded by remember { mutableStateOf(false) }
    var showCustomBatteryDialog by remember { mutableStateOf(false) }

    var dynamicItemConditions: List<String> by remember { mutableStateOf(ITEM_CONDITION_OPTIONS) }
    var itemCondition by remember { mutableStateOf("Used") }
    var conditionExpanded by remember { mutableStateOf(false) }
    var showCustomConditionDialog by remember { mutableStateOf(false) }

    var dynamicGrades: List<String> by remember { mutableStateOf(PHONE_GRADE_OPTIONS) }
    var conditionGrade by remember { mutableStateOf("Grade A") }
    var gradeExpanded by remember { mutableStateOf(false) }
    var showCustomGradeDialog by remember { mutableStateOf(false) }

    var dynamicPackaging by remember { mutableStateOf(PACKAGING_OPTIONS) }
    var packaging by remember { mutableStateOf("Full Box") }
    var packagingExpanded by remember { mutableStateOf(false) }
    var showCustomPackagingDialog by remember { mutableStateOf(false) }

    var entryDate by remember { mutableStateOf(PhoneHisabViewModel.getTodayDate()) }
    var status by remember { mutableStateOf(PhoneStatus.IN_STOCK.code) }
    var notes by remember { mutableStateOf("") }

    // Admin only fields
    var buyingPriceRmbText by remember { mutableStateOf("") }
    var buyingPriceBdtText by remember { mutableStateOf("") }
    var shippingCostRmbText by remember { mutableStateOf("") }
    var otherCostRmbText by remember { mutableStateOf("") }
    var desiredProfitBdtText by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Calculation helper
    val rmbVal = buyingPriceRmbText.toDoubleOrNull() ?: 0.0
    val bdtDirect = buyingPriceBdtText.toDoubleOrNull() ?: 0.0
    val effectiveBuyingBdt = if (bdtDirect > 0) bdtDirect else (rmbVal * rmbRate)
    val shippingRmbVal = shippingCostRmbText.toDoubleOrNull() ?: 0.0
    val effectiveShippingBdt = shippingRmbVal * rmbRate
    val otherRmbVal = otherCostRmbText.toDoubleOrNull() ?: 0.0
    val otherBdt = if (rmbRate > 0) otherRmbVal * rmbRate else otherRmbVal
    val calculatedTotalCost = effectiveBuyingBdt + effectiveShippingBdt + otherBdt
    val manualProfitVal = desiredProfitBdtText.toDoubleOrNull()
    val defaultAdminProfit = if (calculatedTotalCost < 50000.0) 7000.0 else 10000.0
    val effectiveDesiredProfit = when {
        manualProfitVal != null -> manualProfitVal
        isAdmin -> defaultAdminProfit
        else -> 0.0
    }
    val desiredProfitVal = effectiveDesiredProfit
    val calculatedAskingPrice = if (calculatedTotalCost > 0.0 || manualProfitVal != null) {
        calculatedTotalCost + effectiveDesiredProfit
    } else {
        0.0
    }
    val isDark = isAppInDarkTheme()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Add New Phone",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDark) Color.White else Slate900,
                        maxLines = 1
                    )
                    Text(
                        text = if (isAdmin) "Admin Sourcing Mode (With Costs)" else "Staff Mode (Stock Entry)",
                        fontSize = 11.sp,
                        color = if (isAdmin) (if (isDark) Color(0xFF60A5FA) else PrimaryBlue) else (if (isDark) Color(0xFFA0AEC0) else Slate700),
                        maxLines = 1
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                // Stock Location Status Selection - Stacked Vertically (Up & Down)
                Text(
                    text = "Phone Status & Location:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Slate900
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: In Stock (Shop)
                    val isInStockSelected = status == PhoneStatus.IN_STOCK.code
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isInStockSelected) (if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7)) else (if (isDark) Color(0xFF2D3748) else Color(0xFFF8FAFC))
                        ),
                        border = BorderStroke(
                            width = if (isInStockSelected) 1.5.dp else 1.dp,
                            color = if (isInStockSelected) StockGreen else (if (isDark) Color(0xFF4A5568) else Color(0xFFE2E8F0))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { status = PhoneStatus.IN_STOCK.code }
                            .testTag("status_in_stock_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = if (isInStockSelected) (if (isDark) Color(0xFF4ADE80) else StockGreen) else (if (isDark) Color(0xFFA0AEC0) else Slate700),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "In Stock (Shop)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isInStockSelected) (if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)) else (if (isDark) Color.White else Slate900),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Available physically in shop for sale",
                                        fontSize = 10.sp,
                                        color = if (isDark) Color(0xFFA0AEC0) else Slate700,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (isInStockSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (isInStockSelected) (if (isDark) Color(0xFF4ADE80) else StockGreen) else (if (isDark) Color(0xFF4A5568) else Color(0xFFCBD5E1)),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Option 2: In Transit (China)
                    val isInTransitSelected = status == PhoneStatus.IN_TRANSIT.code
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isInTransitSelected) (if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7)) else (if (isDark) Color(0xFF2D3748) else Color(0xFFF8FAFC))
                        ),
                        border = BorderStroke(
                            width = if (isInTransitSelected) 1.5.dp else 1.dp,
                            color = if (isInTransitSelected) TransitAmber else (if (isDark) Color(0xFF4A5568) else Color(0xFFE2E8F0))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { status = PhoneStatus.IN_TRANSIT.code }
                            .testTag("status_in_transit_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlightTakeoff,
                                    contentDescription = null,
                                    tint = if (isInTransitSelected) (if (isDark) Color(0xFFFBBF24) else TransitAmber) else (if (isDark) Color(0xFFA0AEC0) else Slate700),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "In Transit (China)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isInTransitSelected) (if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)) else (if (isDark) Color.White else Slate900),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Shipped from China / Air cargo in transit",
                                        fontSize = 10.sp,
                                        color = if (isDark) Color(0xFFA0AEC0) else Slate700,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (isInTransitSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (isInTransitSelected) (if (isDark) Color(0xFFFBBF24) else TransitAmber) else (if (isDark) Color(0xFF4A5568) else Color(0xFFCBD5E1)),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Brand Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = brandExpanded,
                    onExpandedChange = { brandExpanded = !brandExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Brand Name *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("phone_brand_select")
                    )
                    ExposedDropdownMenu(
                        expanded = brandExpanded,
                        onDismissRequest = { brandExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                    ) {
                        dynamicBrands.forEach { opt ->
                            val isCustom = opt.startsWith("Custom") || opt == "Other"
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = opt,
                                        color = if (isCustom) (if (isDark) Color(0xFF38BDF8) else PrimaryNavy) else (if (isDark) Color.White else Color(0xFF212121)),
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else Color(0xFF212121)
                                ),
                                onClick = {
                                    if (isCustom) {
                                        showCustomBrandDialog = true
                                    } else {
                                        brand = opt
                                        dynamicStorage = getStorageOptionsForBrand(opt)
                                        ramRom = getDefaultStorageForBrand(opt)
                                    }
                                    brandExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Model Name with Dropdown & Custom Popup Support
                val brandModels = remember(brand, model) {
                    val list = getModelsForBrand(brand)
                    val cleanedQuery = cleanModelForBrand(brand, model).trim()
                    val matching = if (model.isBlank()) list else list.filter {
                        it.contains(model.trim(), ignoreCase = true) || (cleanedQuery.isNotBlank() && it.contains(cleanedQuery, ignoreCase = true))
                    }
                    matching + listOf("Custom...")
                }

                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = !modelExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { 
                            model = it
                            modelExpanded = true
                            errorMessage = null 
                        },
                        label = { Text("Phone Model *") },
                        placeholder = { Text("e.g. 15 Pro Max or S24 Ultra") },
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("phone_model_input")
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                    ) {
                        brandModels.forEach { opt ->
                            val isCustom = opt.startsWith("Custom") || opt == "Other"
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = opt,
                                        color = if (isCustom) (if (isDark) Color(0xFF38BDF8) else PrimaryNavy) else (if (isDark) Color.White else Color(0xFF212121)),
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else Color(0xFF212121)
                                ),
                                onClick = {
                                    if (isCustom) {
                                        showCustomModelDialog = true
                                    } else {
                                        model = cleanModelForBrand(brand, opt)
                                        errorMessage = null
                                    }
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // IMEI Number with Strict 15-Digit Validation and QR Scanner Icon inside input field
                val isImeiLengthInvalid = imei.isNotEmpty() && imei.length != 15
                OutlinedTextField(
                    value = imei,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        if (digitsOnly.length <= 15) {
                            imei = digitsOnly
                            errorMessage = null
                        }
                    },
                    label = { Text("IMEI Number *") },
                    placeholder = { Text("15-digit IMEI Code") },
                    singleLine = true,
                    isError = isImeiLengthInvalid,
                    supportingText = {
                        if (isImeiLengthInvalid) {
                            Text(
                                text = "IMEI must be exactly 15 digits long (${imei.length}/15)",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        } else if (imei.length == 15) {
                            Text(
                                text = if (imei2.isNotBlank()) "✓ Valid IMEI: $imei | IMEI 2: $imei2" else "✓ Valid 15-digit IMEI",
                                color = Color(0xFF16A34A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { showCameraScanner = true },
                            modifier = Modifier.testTag("phone_imei_scanner_trailing_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode / IMEI",
                                tint = PrimaryNavy,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("phone_imei_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2x2 GRID FOR SPECIFICATIONS: Color, Storage, Variant, Battery Health
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF2D3748) else Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF4A5568) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Specifications & Hardware Details:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA0AEC0) else Slate700
                        )

                        // Row 1: Color & Storage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Color Dropdown
                            ExposedDropdownMenuBox(
                                expanded = colorExpanded,
                                onExpandedChange = { colorExpanded = !colorExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = color,
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    label = { Text("Color", maxLines = 1) },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorExpanded) },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("phone_color_select")
                                )
                                ExposedDropdownMenu(
                                    expanded = colorExpanded,
                                    onDismissRequest = { colorExpanded = false },
                                    modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                                ) {
                                    dynamicColors.forEach { opt ->
                                        val isCustom = opt.startsWith("Custom") || opt == "Other"
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = opt,
                                                    color = if (isCustom) (if (isDark) Color(0xFF38BDF8) else PrimaryNavy) else (if (isDark) Color.White else Color(0xFF212121)),
                                                    fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp,
                                                    maxLines = 1
                                                )
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = if (isDark) Color.White else Color(0xFF212121)
                                            ),
                                            onClick = {
                                                if (isCustom) {
                                                    showCustomColorDialog = true
                                                } else {
                                                    color = opt
                                                }
                                                colorExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 2. Storage Dropdown
                            ExposedDropdownMenuBox(
                                expanded = ramRomExpanded,
                                onExpandedChange = { ramRomExpanded = !ramRomExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = ramRom,
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    label = { Text("Storage", maxLines = 1) },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ramRomExpanded) },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("phone_ramrom_select")
                                )
                                ExposedDropdownMenu(
                                    expanded = ramRomExpanded,
                                    onDismissRequest = { ramRomExpanded = false },
                                    modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                                ) {
                                    dynamicStorage.forEach { opt ->
                                        val isCustom = opt.startsWith("Custom") || opt == "Other"
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = opt,
                                                    color = if (isCustom) (if (isDark) Color(0xFF38BDF8) else PrimaryNavy) else (if (isDark) Color.White else Color(0xFF212121)),
                                                    fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp,
                                                    maxLines = 1
                                                )
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = if (isDark) Color.White else Color(0xFF212121)
                                            ),
                                            onClick = {
                                                if (isCustom) {
                                                    showCustomStorageDialog = true
                                                } else {
                                                    ramRom = opt
                                                }
                                                ramRomExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Row 2: Region/Variant, Battery Health & Item Condition
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 3. Variant Dropdown
                            ExposedDropdownMenuBox(
                                expanded = variantExpanded,
                                onExpandedChange = { variantExpanded = !variantExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = variant,
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    label = { Text("Variant", maxLines = 1, fontSize = 11.sp, overflow = TextOverflow.Ellipsis) },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = variantExpanded) },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("phone_variant_select")
                                )
                                ExposedDropdownMenu(
                                    expanded = variantExpanded,
                                    onDismissRequest = { variantExpanded = false },
                                    modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                                ) {
                                    dynamicVariants.forEach { opt ->
                                        val isCustom = opt.startsWith("Custom") || opt == "Other"
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = opt,
                                                    color = if (isCustom) (if (isDark) Color(0xFF38BDF8) else PrimaryNavy) else (if (isDark) Color.White else Color(0xFF212121)),
                                                    fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp,
                                                    maxLines = 1
                                                )
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = if (isDark) Color.White else Color(0xFF212121)
                                            ),
                                            onClick = {
                                                if (isCustom) {
                                                    showCustomVariantDialog = true
                                                } else {
                                                    variant = opt
                                                }
                                                variantExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 4. Battery Health Dropdown
                            ExposedDropdownMenuBox(
                                expanded = batteryExpanded,
                                onExpandedChange = { batteryExpanded = !batteryExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = batteryHealth,
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    label = { Text("Battery %", maxLines = 1, fontSize = 11.sp, overflow = TextOverflow.Ellipsis) },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = batteryExpanded) },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("phone_battery_select")
                                )
                                ExposedDropdownMenu(
                                    expanded = batteryExpanded,
                                    onDismissRequest = { batteryExpanded = false },
                                    modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                                ) {
                                    dynamicBattery.forEach { opt ->
                                        val isCustom = opt.startsWith("Custom") || opt == "Other"
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = opt,
                                                    color = if (isCustom) (if (isDark) Color(0xFF38BDF8) else PrimaryNavy) else (if (isDark) Color.White else Color(0xFF212121)),
                                                    fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp,
                                                    maxLines = 1
                                                )
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = if (isDark) Color.White else Color(0xFF212121)
                                            ),
                                            onClick = {
                                                if (isCustom) {
                                                    showCustomBatteryDialog = true
                                                } else {
                                                    batteryHealth = opt
                                                }
                                                batteryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Entry Date (Calendar Date Picker)
                AppDatePickerField(
                    label = "Entry Date (YYYY-MM-DD)",
                    selectedDate = entryDate.ifBlank { PhoneHisabViewModel.getTodayDate() },
                    onDateSelected = { entryDate = it },
                    testTag = "phone_entry_date_input",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Condition Dropdown (Directly UNDERNEATH Entry Date)
                ExposedDropdownMenuBox(
                    expanded = conditionExpanded,
                    onExpandedChange = { conditionExpanded = !conditionExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = itemCondition,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Condition *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("phone_condition_select")
                    )
                    ExposedDropdownMenu(
                        expanded = conditionExpanded,
                        onDismissRequest = { conditionExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                    ) {
                        dynamicItemConditions.forEach { opt ->
                            val isCustom = opt.startsWith("Custom") || opt == "Other"
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = opt,
                                        color = if (isCustom) (if (isDark) Color(0xFF38BDF8) else PrimaryNavy) else (if (isDark) Color.White else Color(0xFF212121)),
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else Color(0xFF212121)
                                ),
                                onClick = {
                                    if (isCustom) {
                                        showCustomConditionDialog = true
                                    } else {
                                        itemCondition = opt
                                    }
                                    conditionExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Phone Grade Dropdown (Directly UNDERNEATH Condition Dropdown)
                ExposedDropdownMenuBox(
                    expanded = gradeExpanded,
                    onExpandedChange = { gradeExpanded = !gradeExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = conditionGrade,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Phone Grade *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("phone_grade_select")
                    )
                    ExposedDropdownMenu(
                        expanded = gradeExpanded,
                        onDismissRequest = { gradeExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                    ) {
                        dynamicGrades.forEach { opt ->
                            val isCustom = opt.startsWith("Custom") || opt == "Other"
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = opt,
                                        color = if (isCustom) (if (isDark) Color(0xFF38BDF8) else PrimaryNavy) else (if (isDark) Color.White else Color(0xFF212121)),
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else Color(0xFF212121)
                                ),
                                onClick = {
                                    if (isCustom) {
                                        showCustomGradeDialog = true
                                    } else {
                                        conditionGrade = opt
                                    }
                                    gradeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Included Accessories / Packaging Dropdown (Directly ABOVE Phone Status)
                ExposedDropdownMenuBox(
                    expanded = packagingExpanded,
                    onExpandedChange = { packagingExpanded = !packagingExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = packaging,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Included Accessories / Packaging", maxLines = 1) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = packagingExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("phone_packaging_select")
                    )
                    ExposedDropdownMenu(
                        expanded = packagingExpanded,
                        onDismissRequest = { packagingExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                    ) {
                        dynamicPackaging.forEach { opt ->
                            val isCustom = opt.startsWith("Custom") || opt == "Other"
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = opt,
                                        color = if (isCustom) (if (isDark) Color(0xFF38BDF8) else PrimaryNavy) else (if (isDark) Color.White else Color(0xFF212121)),
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else Color(0xFF212121)
                                ),
                                onClick = {
                                    if (isCustom) {
                                        showCustomPackagingDialog = true
                                    } else {
                                        packaging = opt
                                    }
                                    packagingExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes / Details (Compact Size)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Extra Specs", maxLines = 1, fontSize = 12.sp) },
                    placeholder = { Text("Accessories, cable, scratches, etc.", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("phone_notes_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ADMIN-ONLY COST SECTION
                if (isAdmin) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF2D3748) else Color(0xFFF1F5F9)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if (isDark) Color(0xFF4A5568) else CardBorder)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Admin Landing Costs (Confidential)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Buying price in RMB or BDT
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = buyingPriceRmbText,
                                    onValueChange = {
                                        buyingPriceRmbText = it
                                        val rmb = it.toDoubleOrNull() ?: 0.0
                                        buyingPriceBdtText = if (rmb > 0) (rmb * rmbRate).toInt().toString() else ""
                                    },
                                    label = { Text("China Price (¥ RMB)", maxLines = 1) },
                                    placeholder = { Text("2800") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("phone_rmb_input")
                                )

                                OutlinedTextField(
                                    value = buyingPriceBdtText,
                                    onValueChange = { buyingPriceBdtText = it },
                                    label = { Text("Buy Price (৳ BDT)", maxLines = 1) },
                                    placeholder = { Text("49000") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("phone_buying_bdt_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Shipping cost & Other cost
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = shippingCostRmbText,
                                    onValueChange = { shippingCostRmbText = it },
                                    label = { Text("Air Cargo (¥)", maxLines = 1) },
                                    placeholder = { Text("150") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("phone_shipping_input")
                                )

                                OutlinedTextField(
                                    value = otherCostRmbText,
                                    onValueChange = { otherCostRmbText = it },
                                    label = { Text("Other Cost (¥ RMB)", maxLines = 1) },
                                    placeholder = { Text("50") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("phone_other_cost_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Desired Profit Input
                            OutlinedTextField(
                                value = desiredProfitBdtText,
                                onValueChange = { desiredProfitBdtText = it },
                                label = { Text("Desired Profit / Margin (৳ BDT)", maxLines = 1) },
                                placeholder = {
                                    val autoHint = if (isAdmin) {
                                        if (calculatedTotalCost < 50000.0) "Auto: 7,000 (<50k)" else "Auto: 10,000 (≥50k)"
                                    } else "e.g. 5000"
                                    Text(autoHint)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = appTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("phone_desired_profit_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Cost, Desired Profit & Asking Price Summary Card
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Total Base Cost:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFFA0AEC0) else Slate700,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "৳ ${String.format("%,.0f", calculatedTotalCost)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else Slate900,
                                            maxLines = 1
                                        )
                                    }

                                    if (desiredProfitVal > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (desiredProfitBdtText.isBlank() && isAdmin) "Desired Profit (Auto):" else "Desired Profit:",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534),
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "+৳ ${String.format("%,.0f", desiredProfitVal)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Asking Price:",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "(Base Cost + Desired Profit)",
                                                fontSize = 9.sp,
                                                color = if (isDark) Color(0xFFA0AEC0) else Slate700,
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            text = "৳ ${String.format("%,.0f", calculatedAskingPrice)}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Privacy notice for staff
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = TransitAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🔒 Purchase and cargo costs are securely managed by Admin.",
                                fontSize = 11.sp,
                                color = Color(0xFF92400E),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
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
            AppDialogConfirmButton(
                text = "Save Phone",
                gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)),
                testTag = "submit_add_phone_btn",
                onClick = {
                    if (model.isBlank()) {
                        errorMessage = "Please enter phone model!"
                        return@AppDialogConfirmButton
                    }
                    if (imei.isBlank() || imei.length != 15 || !imei.all { it.isDigit() }) {
                        errorMessage = "IMEI must be exactly 15 digits long"
                        return@AppDialogConfirmButton
                    }

                    // Strict Duplicate IMEI Prevention
                    val cleanImei = imei.trim()
                    val duplicate = existingPhones.firstOrNull {
                        it.imei.trim().equals(cleanImei, ignoreCase = true) ||
                        (!it.imei2.isNullOrBlank() && it.imei2.trim().equals(cleanImei, ignoreCase = true))
                    }
                    if (duplicate != null) {
                        errorMessage = "Duplicate IMEI! This device is already registered in stock/transit."
                        return@AppDialogConfirmButton
                    }

                    if (imei2.isNotBlank()) {
                        val cleanImei2 = imei2.trim()
                        val duplicate2 = existingPhones.firstOrNull {
                            it.imei.trim().equals(cleanImei2, ignoreCase = true) ||
                            (!it.imei2.isNullOrBlank() && it.imei2.trim().equals(cleanImei2, ignoreCase = true))
                        }
                        if (duplicate2 != null) {
                            errorMessage = "Duplicate IMEI! This device is already registered in stock/transit."
                            return@AppDialogConfirmButton
                        }
                    }

                    val rmb = buyingPriceRmbText.toDoubleOrNull() ?: 0.0
                    val bdtDirectCost = buyingPriceBdtText.toDoubleOrNull() ?: 0.0
                    val finalBuyingBdt = if (bdtDirectCost > 0) bdtDirectCost else (rmb * rmbRate)
                    val shippingRmb = shippingCostRmbText.toDoubleOrNull() ?: 0.0
                    val finalShippingBdt = shippingRmb * rmbRate
                    val other = if (rmbRate > 0) (otherCostRmbText.toDoubleOrNull() ?: 0.0) * rmbRate else (otherCostRmbText.toDoubleOrNull() ?: 0.0)
                    val desiredProfit = when {
                        desiredProfitBdtText.isNotBlank() -> desiredProfitBdtText.toDoubleOrNull() ?: 0.0
                        isAdmin -> if (calculatedTotalCost < 50000.0) 7000.0 else 10000.0
                        else -> 0.0
                    }

                    val resolvedBatteryHealth = batteryHealth.trim().ifBlank { "100%" }

                    onAddPhone(
                        brand,
                        cleanModelForBrand(brand, model),
                        imei,
                        "",
                        color,
                        ramRom,
                        variant,
                        resolvedBatteryHealth,
                        conditionGrade,
                        itemCondition,
                        packaging,
                        entryDate,
                        status,
                        notes,
                        rmb,
                        finalBuyingBdt,
                        finalShippingBdt,
                        other,
                        desiredProfit
                    )
                    onDismiss()
                }
            )
        },
        dismissButton = {
            AppDialogDismissButton(
                text = "Cancel",
                testTag = "cancel_add_phone_btn",
                onClick = onDismiss
            )
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isDark) Color(0xFF1E293B) else CardBg
    )

    // Custom Brand Dialog
    if (showCustomBrandDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Brand Name",
            label = "Brand Name",
            placeholder = "e.g. Asus ROG, ZTE, Sony",
            initialValue = if (brand in BRAND_OPTIONS) "" else brand,
            onDismiss = { showCustomBrandDialog = false },
            onConfirm = { customValue ->
                brand = customValue
                dynamicStorage = getStorageOptionsForBrand(customValue)
                ramRom = getDefaultStorageForBrand(customValue)
                if (!dynamicBrands.contains(customValue)) {
                    val customIdx = dynamicBrands.indexOfFirst { it.startsWith("Custom") }
                    dynamicBrands = if (customIdx >= 0) {
                        dynamicBrands.toMutableList().apply { add(customIdx, customValue) }
                    } else {
                        dynamicBrands + customValue
                    }
                }
                showCustomBrandDialog = false
            }
        )
    }

    // Custom Model Dialog
    if (showCustomModelDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Phone Model",
            label = "Phone Model",
            placeholder = "e.g. ROG Phone 8 Pro, Xperia 1 V",
            initialValue = model,
            onDismiss = { showCustomModelDialog = false },
            onConfirm = { customValue ->
                model = cleanModelForBrand(brand, customValue)
                errorMessage = null
                showCustomModelDialog = false
            }
        )
    }

    // Custom Color Dialog
    if (showCustomColorDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Phone Color",
            label = "Color Finish",
            placeholder = "e.g. Alpine Green, Cosmic Orange",
            initialValue = if (color in COLOR_OPTIONS) "" else color,
            onDismiss = { showCustomColorDialog = false },
            onConfirm = { customValue ->
                color = customValue
                if (!dynamicColors.contains(customValue)) {
                    val customIdx = dynamicColors.indexOfFirst { it.startsWith("Custom") || it == "Other" }
                    dynamicColors = if (customIdx >= 0) {
                        dynamicColors.toMutableList().apply { add(customIdx, customValue) }
                    } else {
                        dynamicColors + customValue
                    }
                }
                showCustomColorDialog = false
            }
        )
    }

    // Custom Storage Dialog
    if (showCustomStorageDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Storage / RAM",
            label = "Storage Capacity / RAM",
            placeholder = "e.g. 18GB / 1TB, 24GB / 1TB",
            initialValue = if (ramRom in dynamicStorage) "" else ramRom,
            onDismiss = { showCustomStorageDialog = false },
            onConfirm = { customValue ->
                ramRom = customValue
                if (!dynamicStorage.contains(customValue)) {
                    val customIdx = dynamicStorage.indexOfFirst { it.startsWith("Custom") || it == "Other" }
                    dynamicStorage = if (customIdx >= 0) {
                        dynamicStorage.toMutableList().apply { add(customIdx, customValue) }
                    } else {
                        dynamicStorage + customValue
                    }
                }
                showCustomStorageDialog = false
            }
        )
    }

    // Custom Regional Variant Dialog
    if (showCustomVariantDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Regional Variant",
            label = "Regional Variant",
            placeholder = "e.g. Canada (VC/A), Singapore (ZA/A)",
            initialValue = if (variant in VARIANT_OPTIONS) "" else variant,
            onDismiss = { showCustomVariantDialog = false },
            onConfirm = { customValue ->
                variant = customValue
                if (!dynamicVariants.contains(customValue)) {
                    val customIdx = dynamicVariants.indexOfFirst { it.startsWith("Custom") || it == "Other" }
                    dynamicVariants = if (customIdx >= 0) {
                        dynamicVariants.toMutableList().apply { add(customIdx, customValue) }
                    } else {
                        dynamicVariants + customValue
                    }
                }
                showCustomVariantDialog = false
            }
        )
    }

    // Custom Battery Health Dialog
    if (showCustomBatteryDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Battery % / Detail",
            label = "Battery Health % / Replacement Note",
            placeholder = "e.g. 87% (Replaced), Boosted 100%",
            initialValue = if (batteryHealth in BATTERY_HEALTH_OPTIONS) "" else batteryHealth,
            onDismiss = { showCustomBatteryDialog = false },
            onConfirm = { customValue ->
                batteryHealth = customValue
                if (!dynamicBattery.contains(customValue)) {
                    val customIdx = dynamicBattery.indexOfFirst { it.startsWith("Custom") || it == "Other" }
                    dynamicBattery = if (customIdx >= 0) {
                        dynamicBattery.toMutableList().apply { add(customIdx, customValue) }
                    } else {
                        dynamicBattery + customValue
                    }
                }
                showCustomBatteryDialog = false
            }
        )
    }

    // Custom Packaging / Accessories Dialog
    if (showCustomPackagingDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Packaging / Accessories",
            label = "Packaging & Accessories",
            placeholder = "e.g. Box & Cable, Only Phone, Charger",
            initialValue = if (packaging in PACKAGING_OPTIONS) "" else packaging,
            onDismiss = { showCustomPackagingDialog = false },
            onConfirm = { customValue ->
                packaging = customValue
                if (!dynamicPackaging.contains(customValue)) {
                    val customIdx = dynamicPackaging.indexOfFirst { it.startsWith("Custom") || it == "Other" }
                    dynamicPackaging = if (customIdx >= 0) {
                        dynamicPackaging.toMutableList().apply { add(customIdx, customValue) }
                    } else {
                        dynamicPackaging + customValue
                    }
                }
                showCustomPackagingDialog = false
            }
        )
    }

    // Custom Item Condition Dialog
    if (showCustomConditionDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Item Condition",
            label = "Item Condition",
            placeholder = "e.g. Refurbished, Open Box, Demo Unit",
            initialValue = if (itemCondition in ITEM_CONDITION_OPTIONS) "" else itemCondition,
            onDismiss = { showCustomConditionDialog = false },
            onConfirm = { customValue ->
                itemCondition = customValue
                if (!dynamicItemConditions.contains(customValue)) {
                    val customIdx = dynamicItemConditions.indexOfFirst { it.startsWith("Custom") || it == "Other" }
                    dynamicItemConditions = if (customIdx >= 0) {
                        dynamicItemConditions.toMutableList().apply { add(customIdx, customValue) }
                    } else {
                        dynamicItemConditions + customValue
                    }
                }
                showCustomConditionDialog = false
            }
        )
    }

    // Custom Phone Grade Dialog
    if (showCustomGradeDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Phone Grade",
            label = "Phone Grade",
            placeholder = "e.g. Grade A+, Grade D, Pristine",
            initialValue = if (conditionGrade in PHONE_GRADE_OPTIONS) "" else conditionGrade,
            onDismiss = { showCustomGradeDialog = false },
            onConfirm = { customValue ->
                conditionGrade = customValue
                if (!dynamicGrades.contains(customValue)) {
                    val customIdx = dynamicGrades.indexOfFirst { it.startsWith("Custom") || it == "Other" }
                    dynamicGrades = if (customIdx >= 0) {
                        dynamicGrades.toMutableList().apply { add(customIdx, customValue) }
                    } else {
                        dynamicGrades + customValue
                    }
                }
                showCustomGradeDialog = false
            }
        )
    }

    // Camera Barcode & IMEI Scanner Dialog
    if (showCameraScanner) {
        ImeiCameraScannerDialog(
            onDismiss = { showCameraScanner = false },
            onImeiScanned = { scannedImei1, scannedImei2 ->
                imei = scannedImei1
                if (!scannedImei2.isNullOrBlank()) {
                    imei2 = scannedImei2
                }
                errorMessage = null
                showCameraScanner = false
            }
        )
    }
}

