package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BATTERY_HEALTH_OPTIONS
import com.example.data.model.BRAND_OPTIONS
import com.example.data.model.COLOR_OPTIONS
import com.example.data.model.ITEM_CONDITION_OPTIONS
import com.example.data.model.PACKAGING_OPTIONS
import com.example.data.model.PHONE_GRADE_OPTIONS
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.example.data.model.UserRole
import com.example.data.model.VARIANT_OPTIONS
import com.example.data.model.cleanModelForBrand
import com.example.data.model.getDefaultStorageForBrand
import com.example.data.model.getModelsForBrand
import com.example.data.model.getStorageOptionsForBrand
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TransitAmber
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme
import com.example.viewmodel.PhoneHisabViewModel

/**
 * Calculates the automatic default Desire Profit based on Base Cost:
 * - If Base Cost < 50,000 BDT -> 7,000 BDT
 * - If Base Cost >= 50,000 BDT -> 10,000 BDT
 */
fun calculateDefaultDesiredProfit(baseCost: Double): Double {
    return if (baseCost < 50000.0) 7000.0 else 10000.0
}

/**
 * Calculates the effective Desire Profit:
 * 1. If Admin/user enters a value manually, use that entered value.
 * 2. If left empty/blank and logged in as Admin, automatically apply:
 *    - Base Cost < 50,000 BDT -> 7,000 BDT
 *    - Base Cost >= 50,000 BDT -> 10,000 BDT
 * 3. General non-admin users default to 0.0 unless already recorded.
 */
fun calculateEffectiveDesiredProfit(
    desiredProfitInput: String,
    baseCost: Double,
    isAdmin: Boolean
): Double {
    val manualVal = desiredProfitInput.toDoubleOrNull()
    return when {
        manualVal != null -> manualVal
        isAdmin -> calculateDefaultDesiredProfit(baseCost)
        else -> 0.0
    }
}

/**
 * Calculates the Asking Price = Base Cost + Desire Profit.
 */
fun calculateAskingPrice(
    baseCost: Double,
    effectiveDesiredProfit: Double,
    hasManualProfit: Boolean
): Double {
    return if (baseCost > 0.0 || hasManualProfit) {
        baseCost + effectiveDesiredProfit
    } else {
        0.0
    }
}

/**
 * Add / Edit Stock Dialog for managing phone inventory stock items.
 *
 * Implements the Desire Profit logic:
 * 1. When logged in as Admin, if "Desire Profit" is left empty/blank:
 *    - Base Cost < 50,000 BDT -> automatically set Desire Profit to 7,000 BDT.
 *    - Base Cost >= 50,000 BDT -> automatically set Desire Profit to 10,000 BDT.
 * 2. If Admin enters a value manually in the Desire Profit field, use that entered value.
 * 3. Calculate Asking Price as: Asking Price = Base Cost + Desire Profit.
 * 4. General non-admin users continue seeing only the Asking Price as before.
 * 5. Preserves all existing design, card layouts, theme colors, and database logic.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditStockDialog(
    phoneToEdit: PhoneItem? = null,
    currentRole: UserRole = UserRole.ADMIN,
    rmbRate: Double,
    isBengali: Boolean = false,
    existingPhones: List<PhoneItem> = emptyList(),
    onDismiss: () -> Unit,
    onSavePhone: (PhoneItem) -> Unit
) {
    val isEditMode = phoneToEdit != null
    val isAdmin = currentRole == UserRole.ADMIN
    val isDark = isAppInDarkTheme()

    // Brand
    var dynamicBrands by remember {
        mutableStateOf(
            if (phoneToEdit != null && phoneToEdit.brand.isNotBlank() && phoneToEdit.brand !in BRAND_OPTIONS) {
                listOf(phoneToEdit.brand) + BRAND_OPTIONS
            } else {
                BRAND_OPTIONS
            }
        )
    }
    var brand by remember { mutableStateOf(phoneToEdit?.brand ?: "Apple") }
    var brandExpanded by remember { mutableStateOf(false) }
    var showCustomBrandDialog by remember { mutableStateOf(false) }

    // Model
    var model by remember { mutableStateOf(phoneToEdit?.model ?: "") }
    var modelExpanded by remember { mutableStateOf(false) }
    var showCustomModelDialog by remember { mutableStateOf(false) }

    // IMEI & Scanner
    var imei by remember { mutableStateOf(phoneToEdit?.imei ?: "") }
    var imei2 by remember { mutableStateOf(phoneToEdit?.imei2 ?: "") }
    var showCameraScanner1 by remember { mutableStateOf(false) }
    var showCameraScanner2 by remember { mutableStateOf(false) }

    // Color
    var dynamicColors by remember {
        mutableStateOf(
            if (phoneToEdit != null && phoneToEdit.color.isNotBlank() && phoneToEdit.color !in COLOR_OPTIONS) {
                listOf(phoneToEdit.color) + COLOR_OPTIONS
            } else {
                COLOR_OPTIONS
            }
        )
    }
    var color by remember { mutableStateOf(phoneToEdit?.color ?: "Black") }
    var colorExpanded by remember { mutableStateOf(false) }
    var showCustomColorDialog by remember { mutableStateOf(false) }

    // Storage / RAM-ROM
    var dynamicStorageOptions by remember(brand) {
        mutableStateOf(getStorageOptionsForBrand(brand))
    }
    var ramRom by remember {
        mutableStateOf(phoneToEdit?.ramRom ?: getDefaultStorageForBrand("Apple"))
    }
    var ramRomExpanded by remember { mutableStateOf(false) }
    var showCustomStorageDialog by remember { mutableStateOf(false) }

    // Variant
    var dynamicVariants by remember {
        mutableStateOf(
            if (phoneToEdit != null && phoneToEdit.variant.isNotBlank() && phoneToEdit.variant !in VARIANT_OPTIONS) {
                listOf(phoneToEdit.variant) + VARIANT_OPTIONS
            } else {
                VARIANT_OPTIONS
            }
        )
    }
    var variant by remember { mutableStateOf(phoneToEdit?.variant ?: "Global") }
    var variantExpanded by remember { mutableStateOf(false) }
    var showCustomVariantDialog by remember { mutableStateOf(false) }

    // Battery Health
    var batteryHealth by remember { mutableStateOf(phoneToEdit?.batteryHealth ?: "100%") }
    var batteryExpanded by remember { mutableStateOf(false) }

    // Condition Grade
    var dynamicGrades by remember {
        mutableStateOf(
            if (phoneToEdit != null && phoneToEdit.conditionGrade.isNotBlank() && phoneToEdit.conditionGrade !in PHONE_GRADE_OPTIONS) {
                listOf(phoneToEdit.conditionGrade) + PHONE_GRADE_OPTIONS
            } else {
                PHONE_GRADE_OPTIONS
            }
        )
    }
    var conditionGrade by remember { mutableStateOf(phoneToEdit?.conditionGrade ?: "Grade A") }
    var gradeExpanded by remember { mutableStateOf(false) }
    var showCustomGradeDialog by remember { mutableStateOf(false) }

    // Item Condition
    var dynamicItemConditions by remember {
        mutableStateOf(
            if (phoneToEdit != null && phoneToEdit.itemCondition.isNotBlank() && phoneToEdit.itemCondition !in ITEM_CONDITION_OPTIONS) {
                listOf(phoneToEdit.itemCondition) + ITEM_CONDITION_OPTIONS
            } else {
                ITEM_CONDITION_OPTIONS
            }
        )
    }
    var itemCondition by remember { mutableStateOf(phoneToEdit?.itemCondition ?: "Used") }
    var itemConditionExpanded by remember { mutableStateOf(false) }
    var showCustomItemConditionDialog by remember { mutableStateOf(false) }

    // Packaging
    var dynamicPackaging by remember {
        mutableStateOf(
            if (phoneToEdit != null && phoneToEdit.packaging.isNotBlank() && phoneToEdit.packaging !in PACKAGING_OPTIONS) {
                listOf(phoneToEdit.packaging) + PACKAGING_OPTIONS
            } else {
                PACKAGING_OPTIONS
            }
        )
    }
    var packaging by remember { mutableStateOf(phoneToEdit?.packaging ?: "Full Box") }
    var packagingExpanded by remember { mutableStateOf(false) }
    var showCustomPackagingDialog by remember { mutableStateOf(false) }

    // Entry Date & Status
    var entryDate by remember {
        mutableStateOf(phoneToEdit?.entryDate ?: PhoneHisabViewModel.getTodayDate())
    }
    var status by remember {
        mutableStateOf(phoneToEdit?.status ?: PhoneStatus.IN_STOCK.code)
    }
    var notes by remember { mutableStateOf(phoneToEdit?.notes ?: "") }

    // Cost Fields (Admin Only)
    var buyingPriceRmbText by remember {
        mutableStateOf(
            if (phoneToEdit != null && phoneToEdit.buyingPriceRmb > 0) {
                phoneToEdit.buyingPriceRmb.toInt().toString()
            } else ""
        )
    }
    var buyingPriceBdtText by remember {
        mutableStateOf(
            if (phoneToEdit != null && phoneToEdit.buyingPriceBdt > 0) {
                phoneToEdit.buyingPriceBdt.toInt().toString()
            } else ""
        )
    }
    var shippingCostRmbText by remember {
        mutableStateOf(
            if (phoneToEdit != null && phoneToEdit.shippingCostBdt > 0 && rmbRate > 0) {
                (phoneToEdit.shippingCostBdt / rmbRate).toInt().toString()
            } else ""
        )
    }
    var otherCostRmbText by remember {
        mutableStateOf(
            if (phoneToEdit != null && phoneToEdit.otherCostBdt > 0 && rmbRate > 0) {
                (phoneToEdit.otherCostBdt / rmbRate).toInt().toString()
            } else if (phoneToEdit != null && phoneToEdit.otherCostBdt > 0) {
                phoneToEdit.otherCostBdt.toInt().toString()
            } else ""
        )
    }
    var desiredProfitBdtText by remember {
        mutableStateOf(
            if (phoneToEdit != null && phoneToEdit.desiredProfitBdt > 0) {
                phoneToEdit.desiredProfitBdt.toInt().toString()
            } else ""
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Calculation Helpers
    val rmbVal = buyingPriceRmbText.toDoubleOrNull() ?: 0.0
    val bdtDirect = buyingPriceBdtText.toDoubleOrNull() ?: 0.0
    val effectiveBuyingBdt = if (bdtDirect > 0) bdtDirect else (rmbVal * rmbRate)
    val shippingRmbVal = shippingCostRmbText.toDoubleOrNull() ?: 0.0
    val effectiveShippingBdt = shippingRmbVal * rmbRate
    val otherRmbVal = otherCostRmbText.toDoubleOrNull() ?: 0.0
    val otherBdt = if (rmbRate > 0) otherRmbVal * rmbRate else otherRmbVal

    // Base Cost = Buying Price + Shipping Cost + Other Cost
    val calculatedTotalCost = effectiveBuyingBdt + effectiveShippingBdt + otherBdt

    // Desire Profit Logic:
    // 1. If Admin enters value manually, use that entered value.
    // 2. If left empty/blank when Admin:
    //    - If Base Cost < 50,000 BDT -> automatically set Desire Profit to 7,000 BDT.
    //    - If Base Cost >= 50,000 BDT -> automatically set Desire Profit to 10,000 BDT.
    // 3. For general non-admin users, defaults to 0.0.
    val manualProfitVal = desiredProfitBdtText.toDoubleOrNull()
    val defaultAdminProfit = calculateDefaultDesiredProfit(calculatedTotalCost)
    val effectiveDesiredProfit = when {
        manualProfitVal != null -> manualProfitVal
        isAdmin -> defaultAdminProfit
        else -> 0.0
    }
    val desiredProfitVal = effectiveDesiredProfit

    // Asking Price = Base Cost + Desire Profit
    val calculatedAskingPrice = calculateAskingPrice(
        baseCost = calculatedTotalCost,
        effectiveDesiredProfit = effectiveDesiredProfit,
        hasManualProfit = manualProfitVal != null
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isEditMode) "Edit Phone Stock" else "Add New Phone",
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
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Stock Status selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isInStock = status == PhoneStatus.IN_STOCK.code
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isInStock) (if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)) else (if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { status = PhoneStatus.IN_STOCK.code }
                    ) {
                        Text(
                            text = "In Stock",
                            fontWeight = if (isInStock) FontWeight.Bold else FontWeight.Normal,
                            color = if (isInStock) (if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF)) else (if (isDark) Color.White else Slate700),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                    }

                    val isInTransit = status == PhoneStatus.IN_TRANSIT.code
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isInTransit) (if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7)) else (if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { status = PhoneStatus.IN_TRANSIT.code }
                    ) {
                        Text(
                            text = "In Transit (China)",
                            fontWeight = if (isInTransit) FontWeight.Bold else FontWeight.Normal,
                            color = if (isInTransit) (if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)) else (if (isDark) Color.White else Slate700),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // BRAND DROPDOWN
                ExposedDropdownMenuBox(
                    expanded = brandExpanded,
                    onExpandedChange = { brandExpanded = !brandExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Brand *", maxLines = 1) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("stock_brand_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = brandExpanded,
                        onDismissRequest = { brandExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                    ) {
                        dynamicBrands.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b, color = if (isDark) Color.White else Slate900) },
                                onClick = {
                                    brand = b
                                    brandExpanded = false
                                    dynamicStorageOptions = getStorageOptionsForBrand(b)
                                    ramRom = getDefaultStorageForBrand(b)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("+ Custom Brand...", color = PrimaryBlue, fontWeight = FontWeight.Bold) },
                            onClick = {
                                brandExpanded = false
                                showCustomBrandDialog = true
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // MODEL FIELD
                val currentSuggestions = getModelsForBrand(brand)
                if (currentSuggestions.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = modelExpanded,
                        onExpandedChange = { modelExpanded = !modelExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Model *", maxLines = 1) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("stock_model_input")
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                        ) {
                            currentSuggestions.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m, color = if (isDark) Color.White else Slate900) },
                                    onClick = {
                                        model = cleanModelForBrand(brand, m)
                                        modelExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Type Custom Model...", color = PrimaryBlue, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    modelExpanded = false
                                    showCustomModelDialog = true
                                }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model *", maxLines = 1) },
                        placeholder = { Text("e.g. Note 13 Pro+") },
                        singleLine = true,
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stock_model_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // IMEI 1 & Scanner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = imei,
                        onValueChange = { imei = it },
                        label = { Text("IMEI 1 * (15 digits)", maxLines = 1) },
                        placeholder = { Text("35...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stock_imei_input")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { showCameraScanner1 = true },
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(48.dp)
                            .testTag("stock_scan_imei_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan IMEI",
                            tint = if (isDark) Color(0xFF60A5FA) else PrimaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // IMEI 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = imei2,
                        onValueChange = { imei2 = it },
                        label = { Text("IMEI 2 (Optional)", maxLines = 1) },
                        placeholder = { Text("35...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stock_imei2_input")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { showCameraScanner2 = true },
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(48.dp)
                            .testTag("stock_scan_imei2_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan IMEI 2",
                            tint = if (isDark) Color(0xFF60A5FA) else PrimaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // COLOR & STORAGE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Color
                    ExposedDropdownMenuBox(
                        expanded = colorExpanded,
                        onExpandedChange = { colorExpanded = !colorExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = color,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Color", maxLines = 1) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorExpanded) },
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = colorExpanded,
                            onDismissRequest = { colorExpanded = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                        ) {
                            dynamicColors.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c, color = if (isDark) Color.White else Slate900) },
                                    onClick = {
                                        color = c
                                        colorExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Custom Color...", color = PrimaryBlue, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    colorExpanded = false
                                    showCustomColorDialog = true
                                }
                            )
                        }
                    }

                    // Storage
                    ExposedDropdownMenuBox(
                        expanded = ramRomExpanded,
                        onExpandedChange = { ramRomExpanded = !ramRomExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = ramRom,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Storage / RAM", maxLines = 1) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ramRomExpanded) },
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = ramRomExpanded,
                            onDismissRequest = { ramRomExpanded = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                        ) {
                            dynamicStorageOptions.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s, color = if (isDark) Color.White else Slate900) },
                                    onClick = {
                                        ramRom = s
                                        ramRomExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Custom Storage...", color = PrimaryBlue, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    ramRomExpanded = false
                                    showCustomStorageDialog = true
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // VARIANT & BATTERY HEALTH
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Variant
                    ExposedDropdownMenuBox(
                        expanded = variantExpanded,
                        onExpandedChange = { variantExpanded = !variantExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = variant,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Variant", maxLines = 1) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = variantExpanded) },
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = variantExpanded,
                            onDismissRequest = { variantExpanded = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                        ) {
                            dynamicVariants.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(v, color = if (isDark) Color.White else Slate900) },
                                    onClick = {
                                        variant = v
                                        variantExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Custom Variant...", color = PrimaryBlue, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    variantExpanded = false
                                    showCustomVariantDialog = true
                                }
                            )
                        }
                    }

                    // Battery Health
                    ExposedDropdownMenuBox(
                        expanded = batteryExpanded,
                        onExpandedChange = { batteryExpanded = !batteryExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = batteryHealth,
                            onValueChange = { batteryHealth = it },
                            label = { Text("Battery", maxLines = 1) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = batteryExpanded) },
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = batteryExpanded,
                            onDismissRequest = { batteryExpanded = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                        ) {
                            BATTERY_HEALTH_OPTIONS.forEach { bh ->
                                DropdownMenuItem(
                                    text = { Text(bh, color = if (isDark) Color.White else Slate900) },
                                    onClick = {
                                        batteryHealth = bh
                                        batteryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // CONDITION GRADE & ITEM CONDITION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Condition Grade
                    ExposedDropdownMenuBox(
                        expanded = gradeExpanded,
                        onExpandedChange = { gradeExpanded = !gradeExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = conditionGrade,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Grade", maxLines = 1) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeExpanded) },
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = gradeExpanded,
                            onDismissRequest = { gradeExpanded = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                        ) {
                            dynamicGrades.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g, color = if (isDark) Color.White else Slate900) },
                                    onClick = {
                                        conditionGrade = g
                                        gradeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Item Condition
                    ExposedDropdownMenuBox(
                        expanded = itemConditionExpanded,
                        onExpandedChange = { itemConditionExpanded = !itemConditionExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = itemCondition,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Condition", maxLines = 1) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemConditionExpanded) },
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = itemConditionExpanded,
                            onDismissRequest = { itemConditionExpanded = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                        ) {
                            dynamicItemConditions.forEach { ic ->
                                DropdownMenuItem(
                                    text = { Text(ic, color = if (isDark) Color.White else Slate900) },
                                    onClick = {
                                        itemCondition = ic
                                        itemConditionExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // PACKAGING
                ExposedDropdownMenuBox(
                    expanded = packagingExpanded,
                    onExpandedChange = { packagingExpanded = !packagingExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = packaging,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Packaging / Accessories", maxLines = 1) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = packagingExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = packagingExpanded,
                        onDismissRequest = { packagingExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                    ) {
                        dynamicPackaging.forEach { pkg ->
                            DropdownMenuItem(
                                text = { Text(pkg, color = if (isDark) Color.White else Slate900) },
                                onClick = {
                                    packaging = pkg
                                    packagingExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ENTRY DATE
                AppDatePickerField(
                    label = "Entry Date",
                    selectedDate = entryDate,
                    onDateSelected = { entryDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // NOTES
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)", maxLines = 1) },
                    placeholder = { Text("e.g. Minor scratches on bezel, battery replaced") },
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stock_notes_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ADMIN CONFIDENTIAL LANDING COSTS & DESIRE PROFIT
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
                                    text = "Admin Landing Costs & Margin (Confidential)",
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
                                    placeholder = { Text("¥") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stock_rmb_input")
                                )

                                OutlinedTextField(
                                    value = buyingPriceBdtText,
                                    onValueChange = { buyingPriceBdtText = it },
                                    label = { Text("Buy Price (৳ BDT)", maxLines = 1) },
                                    placeholder = { Text("৳") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stock_buying_bdt_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Air Cargo & Other Costs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = shippingCostRmbText,
                                    onValueChange = { shippingCostRmbText = it },
                                    label = { Text("Air Cargo (¥ RMB)", maxLines = 1) },
                                    placeholder = { Text("¥ Cargo") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stock_shipping_rmb_input")
                                )

                                OutlinedTextField(
                                    value = otherCostRmbText,
                                    onValueChange = { otherCostRmbText = it },
                                    label = { Text("Other Cost (¥ RMB)", maxLines = 1) },
                                    placeholder = { Text("¥") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stock_other_cost_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // DESIRE PROFIT FIELD (Logic: Blank -> 7,000 if <50k, 10,000 if >=50k; Manual -> Entered value)
                            OutlinedTextField(
                                value = desiredProfitBdtText,
                                onValueChange = { desiredProfitBdtText = it },
                                label = { Text("Desired Profit / Margin (৳ BDT)", maxLines = 1) },
                                placeholder = {
                                    val autoHint = if (calculatedTotalCost < 50000.0) "Auto: 7,000 (<50k)" else "Auto: 10,000 (≥50k)"
                                    Text(autoHint)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = appTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("stock_desired_profit_input")
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
                    // Non-admin view: privacy notice for staff (General non-admin users continue seeing only the Asking Price on cards as before)
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
                text = if (isEditMode) "Save Changes" else "Save Phone",
                gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)),
                testTag = "submit_stock_dialog_btn",
                onClick = {
                    if (model.isBlank()) {
                        errorMessage = "Please enter or select a model name."
                        return@AppDialogConfirmButton
                    }
                    if (imei.isBlank() || imei.length < 10) {
                        errorMessage = "Please enter a valid IMEI number (minimum 10 digits)."
                        return@AppDialogConfirmButton
                    }

                    // Duplicate IMEI verification
                    val cleanImei = imei.trim()
                    val duplicate = existingPhones.find {
                        (phoneToEdit == null || it.id != phoneToEdit.id) &&
                        (it.imei.trim().equals(cleanImei, ignoreCase = true) ||
                         (!it.imei2.isNullOrBlank() && it.imei2.trim().equals(cleanImei, ignoreCase = true)))
                    }
                    if (duplicate != null) {
                        errorMessage = "Duplicate IMEI! This device is already registered in stock/transit."
                        return@AppDialogConfirmButton
                    }

                    if (imei2.isNotBlank()) {
                        val cleanImei2 = imei2.trim()
                        val duplicate2 = existingPhones.find {
                            (phoneToEdit == null || it.id != phoneToEdit.id) &&
                            (it.imei.trim().equals(cleanImei2, ignoreCase = true) ||
                             (!it.imei2.isNullOrBlank() && it.imei2.trim().equals(cleanImei2, ignoreCase = true)))
                        }
                        if (duplicate2 != null) {
                            errorMessage = "Duplicate IMEI! This device is already registered in stock/transit."
                            return@AppDialogConfirmButton
                        }
                    }

                    val finalDesiredProfit = when {
                        desiredProfitBdtText.isNotBlank() -> desiredProfitBdtText.toDoubleOrNull() ?: 0.0
                        isAdmin -> defaultAdminProfit
                        else -> 0.0
                    }

                    val resolvedBatteryHealth = batteryHealth.trim().ifBlank { "100%" }

                    val savedItem = if (phoneToEdit != null) {
                        val isTransitToStock = (phoneToEdit.status.equals(PhoneStatus.IN_TRANSIT.code, ignoreCase = true) || phoneToEdit.status.equals("TRANSIT", ignoreCase = true)) &&
                            (status.equals(PhoneStatus.IN_STOCK.code, ignoreCase = true) || status.equals("IN_STOCK", ignoreCase = true) || status.equals("STOCK", ignoreCase = true))
                        val effectiveEntryDate = if (isTransitToStock) com.example.util.DateUtils.getTodayStorageDate() else entryDate.trim()
                        val now = System.currentTimeMillis()

                        phoneToEdit.copy(
                            brand = brand.trim(),
                            model = cleanModelForBrand(brand, model),
                            imei = imei.trim(),
                            imei2 = imei2.trim(),
                            color = color.trim(),
                            ramRom = ramRom.trim(),
                            variant = variant.trim(),
                            batteryHealth = resolvedBatteryHealth,
                            conditionGrade = conditionGrade,
                            itemCondition = itemCondition.trim().ifBlank { "Used" },
                            packaging = packaging.trim().ifBlank { "Full Box" },
                            entryDate = effectiveEntryDate,
                            status = status,
                            notes = notes.trim(),
                            buyingPriceRmb = rmbVal,
                            buyingPriceBdt = effectiveBuyingBdt,
                            shippingCostBdt = effectiveShippingBdt,
                            otherCostBdt = otherBdt,
                            totalCostBdt = calculatedTotalCost,
                            desiredProfitBdt = finalDesiredProfit,
                            createdAt = if (isTransitToStock) now else phoneToEdit.createdAt,
                            updatedAt = now
                        )
                    } else {
                        PhoneItem(
                            brand = brand.trim(),
                            model = cleanModelForBrand(brand, model),
                            imei = imei.trim(),
                            imei2 = imei2.trim(),
                            color = color.trim(),
                            ramRom = ramRom.trim(),
                            variant = variant.trim(),
                            batteryHealth = resolvedBatteryHealth,
                            conditionGrade = conditionGrade,
                            itemCondition = itemCondition.trim().ifBlank { "Used" },
                            packaging = packaging.trim().ifBlank { "Full Box" },
                            entryDate = entryDate.trim(),
                            status = status,
                            notes = notes.trim(),
                            buyingPriceRmb = rmbVal,
                            buyingPriceBdt = effectiveBuyingBdt,
                            shippingCostBdt = effectiveShippingBdt,
                            otherCostBdt = otherBdt,
                            totalCostBdt = calculatedTotalCost,
                            desiredProfitBdt = finalDesiredProfit,
                            addedByRole = if (isAdmin) "ADMIN" else "STAFF"
                        )
                    }

                    onSavePhone(savedItem)
                    onDismiss()
                }
            )
        },
        dismissButton = {
            AppDialogDismissButton(
                text = "Cancel",
                testTag = "cancel_stock_dialog_btn",
                onClick = onDismiss
            )
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isDark) Color(0xFF1E293B) else CardBg
    )

    // Camera Barcode Scanner for IMEI 1
    if (showCameraScanner1) {
        ImeiCameraScannerDialog(
            onDismiss = { showCameraScanner1 = false },
            onImeiScanned = { imei1, _ ->
                imei = imei1
                showCameraScanner1 = false
            },
            initialHelperTitle = "Scan IMEI 1"
        )
    }

    // Camera Barcode Scanner for IMEI 2
    if (showCameraScanner2) {
        ImeiCameraScannerDialog(
            onDismiss = { showCameraScanner2 = false },
            onImeiScanned = { imei1, _ ->
                imei2 = imei1
                showCameraScanner2 = false
            },
            initialHelperTitle = "Scan IMEI 2"
        )
    }

    // Custom Popups for Add-hoc options
    if (showCustomBrandDialog) {
        CustomInputPopupDialog(
            title = "Add Custom Brand",
            label = "Brand Name",
            onDismiss = { showCustomBrandDialog = false },
            onConfirm = { newBrand ->
                if (newBrand !in dynamicBrands) {
                    dynamicBrands = listOf(newBrand) + dynamicBrands
                }
                brand = newBrand
                showCustomBrandDialog = false
            }
        )
    }

    if (showCustomModelDialog) {
        CustomInputPopupDialog(
            title = "Add Custom Model",
            label = "Model Name",
            onDismiss = { showCustomModelDialog = false },
            onConfirm = { newModel ->
                model = cleanModelForBrand(brand, newModel)
                showCustomModelDialog = false
            }
        )
    }

    if (showCustomColorDialog) {
        CustomInputPopupDialog(
            title = "Add Custom Color",
            label = "Color Name",
            onDismiss = { showCustomColorDialog = false },
            onConfirm = { newColor ->
                if (newColor !in dynamicColors) {
                    dynamicColors = listOf(newColor) + dynamicColors
                }
                color = newColor
                showCustomColorDialog = false
            }
        )
    }

    if (showCustomStorageDialog) {
        CustomInputPopupDialog(
            title = "Add Custom Storage",
            label = "Storage / RAM",
            placeholder = "e.g. 12GB / 512GB",
            onDismiss = { showCustomStorageDialog = false },
            onConfirm = { newStorage ->
                if (newStorage !in dynamicStorageOptions) {
                    dynamicStorageOptions = listOf(newStorage) + dynamicStorageOptions
                }
                ramRom = newStorage
                showCustomStorageDialog = false
            }
        )
    }

    if (showCustomVariantDialog) {
        CustomInputPopupDialog(
            title = "Add Custom Variant",
            label = "Variant Name",
            placeholder = "e.g. Korea (K/A)",
            onDismiss = { showCustomVariantDialog = false },
            onConfirm = { newVariant ->
                if (newVariant !in dynamicVariants) {
                    dynamicVariants = listOf(newVariant) + dynamicVariants
                }
                variant = newVariant
                showCustomVariantDialog = false
            }
        )
    }
}
