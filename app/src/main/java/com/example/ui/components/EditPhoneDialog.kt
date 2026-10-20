package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingCartCheckout
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
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.PAYMENT_METHODS
import com.example.data.model.PAYMENT_METHODS_EN
import com.example.data.model.PHONE_CONDITION_STATUS_OPTIONS
import com.example.data.model.PHONE_GRADE_OPTIONS
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.example.data.model.STORAGE_OPTIONS
import com.example.data.model.VARIANT_OPTIONS
import com.example.data.model.cleanModelForBrand
import com.example.data.model.getModelsForBrand
import com.example.data.model.getDefaultStorageForBrand
import com.example.data.model.getStorageOptionsForBrand
import com.example.ui.components.ImeiCameraScannerDialog
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.StockGreen
import com.example.ui.theme.StockGreenLight
import com.example.ui.theme.TransitAmber
import com.example.ui.theme.TransitAmberLight
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme
import com.example.viewmodel.PhoneHisabViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPhoneDialog(
    phone: PhoneItem,
    rmbRate: Double,
    isBengali: Boolean = false,
    existingPhones: List<PhoneItem> = emptyList(),
    onDismiss: () -> Unit,
    onSaveEdit: (PhoneItem) -> Unit
) {
    var dynamicBrands by remember {
        mutableStateOf(
            if (phone.brand.isNotBlank() && phone.brand !in BRAND_OPTIONS) {
                listOf(phone.brand) + BRAND_OPTIONS
            } else {
                BRAND_OPTIONS
            }
        )
    }
    var brand by remember { mutableStateOf(phone.brand) }
    var brandExpanded by remember { mutableStateOf(false) }
    var showCustomBrandDialog by remember { mutableStateOf(false) }

    var model by remember { mutableStateOf(phone.model) }
    var modelExpanded by remember { mutableStateOf(false) }
    var showCustomModelDialog by remember { mutableStateOf(false) }

    var imei by remember { mutableStateOf(phone.imei) }
    var imei2 by remember { mutableStateOf(phone.imei2) }
    var showCameraScanner by remember { mutableStateOf(false) }

    var dynamicColors by remember {
        mutableStateOf(
            if (phone.color.isNotBlank() && phone.color !in COLOR_OPTIONS) {
                listOf(phone.color) + COLOR_OPTIONS
            } else {
                COLOR_OPTIONS
            }
        )
    }
    var color by remember { mutableStateOf(phone.color.ifBlank { "Black" }) }
    var colorExpanded by remember { mutableStateOf(false) }
    var showCustomColorDialog by remember { mutableStateOf(false) }

    val initialStorageOptions = getStorageOptionsForBrand(phone.brand)
    var dynamicStorage by remember {
        mutableStateOf(
            if (phone.ramRom.isNotBlank() && phone.ramRom !in initialStorageOptions) {
                listOf(phone.ramRom) + initialStorageOptions
            } else {
                initialStorageOptions
            }
        )
    }
    var ramRom by remember { mutableStateOf(phone.ramRom.ifBlank { getDefaultStorageForBrand(phone.brand) }) }
    var ramRomExpanded by remember { mutableStateOf(false) }
    var showCustomStorageDialog by remember { mutableStateOf(false) }

    var dynamicVariants by remember {
        mutableStateOf(
            if (phone.variant.isNotBlank() && phone.variant !in VARIANT_OPTIONS) {
                listOf(phone.variant) + VARIANT_OPTIONS
            } else {
                VARIANT_OPTIONS
            }
        )
    }
    var variant by remember { mutableStateOf(phone.variant.ifBlank { "Global / Official" }) }
    var variantExpanded by remember { mutableStateOf(false) }
    var showCustomVariantDialog by remember { mutableStateOf(false) }

    var dynamicBattery by remember {
        mutableStateOf(
            if (phone.batteryHealth.isNotBlank() && phone.batteryHealth !in BATTERY_HEALTH_OPTIONS) {
                listOf(phone.batteryHealth) + BATTERY_HEALTH_OPTIONS
            } else {
                BATTERY_HEALTH_OPTIONS
            }
        )
    }
    var batteryHealth by remember { mutableStateOf(phone.batteryHealth.ifBlank { "100%" }) }
    var batteryExpanded by remember { mutableStateOf(false) }
    var showCustomBatteryDialog by remember { mutableStateOf(false) }

    var dynamicPackaging by remember {
        mutableStateOf(
            if (phone.packaging.isNotBlank() && phone.packaging !in PACKAGING_OPTIONS) {
                listOf(phone.packaging) + PACKAGING_OPTIONS
            } else {
                PACKAGING_OPTIONS
            }
        )
    }
    var packaging by remember { mutableStateOf(phone.packaging.ifBlank { "Full Box" }) }
    var packagingExpanded by remember { mutableStateOf(false) }
    var showCustomPackagingDialog by remember { mutableStateOf(false) }

    var dynamicItemConditions: List<String> by remember {
        mutableStateOf(
            if (phone.itemCondition.isNotBlank() && phone.itemCondition !in ITEM_CONDITION_OPTIONS) {
                listOf(phone.itemCondition) + ITEM_CONDITION_OPTIONS
            } else {
                ITEM_CONDITION_OPTIONS
            }
        )
    }
    var itemCondition by remember { mutableStateOf(phone.itemCondition.ifBlank { "Used" }) }
    var itemConditionExpanded by remember { mutableStateOf(false) }
    var showCustomItemConditionDialog by remember { mutableStateOf(false) }

    var dynamicGrades: List<String> by remember {
        mutableStateOf(
            if (phone.conditionGrade.isNotBlank() && phone.conditionGrade !in PHONE_GRADE_OPTIONS) {
                listOf(phone.conditionGrade) + PHONE_GRADE_OPTIONS
            } else {
                PHONE_GRADE_OPTIONS
            }
        )
    }
    var conditionGrade by remember { mutableStateOf(phone.conditionGrade.ifBlank { "Grade A" }) }
    var gradeExpanded by remember { mutableStateOf(false) }
    var showCustomGradeDialog by remember { mutableStateOf(false) }

    var entryDate by remember { mutableStateOf(phone.entryDate) }
    var status by remember { mutableStateOf(phone.status) }
    var notes by remember { mutableStateOf(phone.notes) }

    // Landing Costs (Admin)
    var buyingPriceRmbText by remember {
        mutableStateOf(if (phone.buyingPriceRmb > 0) phone.buyingPriceRmb.toInt().toString() else "")
    }
    var buyingPriceBdtText by remember {
        mutableStateOf(if (phone.buyingPriceBdt > 0) phone.buyingPriceBdt.toInt().toString() else "")
    }
    var shippingCostRmbText by remember {
        mutableStateOf(
            if (phone.shippingCostBdt > 0 && rmbRate > 0) {
                (phone.shippingCostBdt / rmbRate).toInt().toString()
            } else ""
        )
    }
    var otherCostRmbText by remember {
        mutableStateOf(
            if (phone.otherCostBdt > 0 && rmbRate > 0) {
                (phone.otherCostBdt / rmbRate).toInt().toString()
            } else if (phone.otherCostBdt > 0) {
                phone.otherCostBdt.toInt().toString()
            } else ""
        )
    }
    var desiredProfitBdtText by remember {
        mutableStateOf(if (phone.desiredProfitBdt > 0) phone.desiredProfitBdt.toInt().toString() else "")
    }

    // Landing Cost validation error flags
    var chinaPriceError by remember { mutableStateOf(false) }
    var buyPriceError by remember { mutableStateOf(false) }
    var airCargoError by remember { mutableStateOf(false) }
    var otherCostError by remember { mutableStateOf(false) }
    var desiredProfitError by remember { mutableStateOf(false) }

    // Target / Selling Price & Sales Details
    var sellingPriceBdtText by remember {
        mutableStateOf(if (phone.sellingPriceBdt > 0) phone.sellingPriceBdt.toInt().toString() else "")
    }
    var saleDate by remember { mutableStateOf(phone.saleDate) }
    var customerName by remember { mutableStateOf(phone.customerName.uppercase()) }
    var customerPhone by remember { mutableStateOf(phone.customerPhone) }
    var customerEmail by remember { mutableStateOf(phone.customerEmail) }
    var soldByStaff by remember { mutableStateOf(phone.soldByStaff) }

    var dynamicPaymentMethods by remember { mutableStateOf(PAYMENT_METHODS_EN) }
    var paymentMethod by remember { mutableStateOf(phone.paymentMethod.ifBlank { "Cash" }) }
    var paymentExpanded by remember { mutableStateOf(false) }
    var showCustomPaymentDialog by remember { mutableStateOf(false) }
    var warrantyDaysText by remember { mutableStateOf(phone.warrantyDays.toString()) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Live Calculations
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
        else -> defaultAdminProfit
    }
    val desiredProfitVal = effectiveDesiredProfit
    val calculatedAskingPrice = if (calculatedTotalCost > 0.0 || manualProfitVal != null) {
        calculatedTotalCost + effectiveDesiredProfit
    } else {
        0.0
    }
    val sellingPriceVal = sellingPriceBdtText.toDoubleOrNull() ?: 0.0
    val isDark = isAppInDarkTheme()

    val estimatedProfit = if (sellingPriceVal > 0 && calculatedTotalCost > 0) {
        sellingPriceVal - calculatedTotalCost
    } else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Admin Edit Phone Entry",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = if (isDark) Color.White else Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "👑",
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "Full modification access (Admin Only)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                // Section 1: Device Status & Location
                Text(
                    text = "Inventory Status & Location",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // In Stock
                    val isInStock = status == PhoneStatus.IN_STOCK.code
                    Surface(
                        selected = isInStock,
                        onClick = { status = PhoneStatus.IN_STOCK.code },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isInStock) (if (isDark) Color(0xFF14532D) else StockGreenLight) else (if (isDark) Color(0xFF2D3748) else Color(0xFFF8FAFC)),
                        border = BorderStroke(
                            width = if (isInStock) 1.5.dp else 1.dp,
                            color = if (isInStock) StockGreen else (if (isDark) Color(0xFF4A5568) else Color(0xFFCBD5E1))
                        ),
                        shadowElevation = if (isInStock) 1.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("edit_status_in_stock")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "Stock",
                                fontSize = 11.sp,
                                fontWeight = if (isInStock) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isInStock) (if (isDark) Color(0xFF4ADE80) else StockGreen) else (if (isDark) Color(0xFFA0AEC0) else Slate700),
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // In Transit
                    val isInTransit = status == PhoneStatus.IN_TRANSIT.code
                    Surface(
                        selected = isInTransit,
                        onClick = { status = PhoneStatus.IN_TRANSIT.code },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isInTransit) (if (isDark) Color(0xFF78350F) else TransitAmberLight) else (if (isDark) Color(0xFF2D3748) else Color(0xFFF8FAFC)),
                        border = BorderStroke(
                            width = if (isInTransit) 1.5.dp else 1.dp,
                            color = if (isInTransit) TransitAmber else (if (isDark) Color(0xFF4A5568) else Color(0xFFCBD5E1))
                        ),
                        shadowElevation = if (isInTransit) 1.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("edit_status_in_transit")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "Transit",
                                fontSize = 11.sp,
                                fontWeight = if (isInTransit) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isInTransit) (if (isDark) Color(0xFFFBBF24) else TransitAmber) else (if (isDark) Color(0xFFA0AEC0) else Slate700),
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Sold
                    val isSold = status == PhoneStatus.SOLD.code
                    Surface(
                        selected = isSold,
                        onClick = { status = PhoneStatus.SOLD.code },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSold) (if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)) else (if (isDark) Color(0xFF2D3748) else Color(0xFFF8FAFC)),
                        border = BorderStroke(
                            width = if (isSold) 1.5.dp else 1.dp,
                            color = if (isSold) Color(0xFF2563EB) else (if (isDark) Color(0xFF4A5568) else Color(0xFFCBD5E1))
                        ),
                        shadowElevation = if (isSold) 1.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("edit_status_sold")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "Sold",
                                fontSize = 11.sp,
                                fontWeight = if (isSold) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSold) (if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFA0AEC0) else Slate700),
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Returned
                    val isReturned = status == PhoneStatus.RETURNED.code || status == "RETURN"
                    Surface(
                        selected = isReturned,
                        onClick = { status = PhoneStatus.RETURNED.code },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isReturned) (if (isDark) Color(0xFF581C87) else Color(0xFFF3E8FF)) else (if (isDark) Color(0xFF2D3748) else Color(0xFFF8FAFC)),
                        border = BorderStroke(
                            width = if (isReturned) 1.5.dp else 1.dp,
                            color = if (isReturned) Color(0xFF7E22CE) else (if (isDark) Color(0xFF4A5568) else Color(0xFFCBD5E1))
                        ),
                        shadowElevation = if (isReturned) 1.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("edit_status_returned")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "Return",
                                fontSize = 11.sp,
                                fontWeight = if (isReturned) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isReturned) (if (isDark) Color(0xFFC084FC) else Color(0xFF6B21A8)) else (if (isDark) Color(0xFFA0AEC0) else Slate700),
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(12.dp))

                // Section 2: Core Phone Information
                Text(
                    text = "Core Device Details",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))

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
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        label = { Text("Brand Name *", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("edit_phone_brand_select")
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
                                        color = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900),
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900)
                                ),
                                onClick = {
                                    if (isCustom) {
                                        showCustomBrandDialog = true
                                    } else {
                                        if (brand != opt) {
                                            brand = opt
                                            dynamicStorage = getStorageOptionsForBrand(opt)
                                            ramRom = getDefaultStorageForBrand(opt)
                                        }
                                    }
                                    brandExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Model Name with dropdown if known models exist
                val availableModels = remember(brand) {
                    val predefined = getModelsForBrand(brand)
                    if (predefined.isNotEmpty()) {
                        predefined + listOf("Custom...")
                    } else {
                        emptyList()
                    }
                }

                if (availableModels.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = modelExpanded,
                        onExpandedChange = { modelExpanded = !modelExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it; errorMessage = null },
                            label = { Text("Phone Model *", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                            placeholder = { Text("e.g. 15 Pro or S24 Ultra", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("edit_phone_model_input")
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                        ) {
                            availableModels.forEach { modelOpt ->
                                val isCustom = modelOpt.startsWith("Custom") || modelOpt == "Other"
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = modelOpt,
                                            color = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900),
                                            fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900)
                                    ),
                                    onClick = {
                                        if (isCustom) {
                                            showCustomModelDialog = true
                                        } else {
                                            model = cleanModelForBrand(brand, modelOpt)
                                        }
                                        modelExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it; errorMessage = null },
                        label = { Text("Phone Model *", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                        placeholder = { Text("e.g. iPhone 14 Pro", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("edit_phone_model_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // IMEI Number with Scanner Icon inside input field
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
                    label = { Text("IMEI Number *", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                    placeholder = { Text("15-digit code", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    isError = isImeiLengthInvalid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(
                            onClick = { showCameraScanner = true },
                            modifier = Modifier.testTag("edit_phone_imei_scanner_trailing_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode / IMEI",
                                tint = PrimaryNavy,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("edit_phone_imei_input")
                )
                if (isImeiLengthInvalid) {
                    Text(
                        text = "IMEI Number must be exactly 15 digits long (${imei.length}/15)",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                } else if (imei.length == 15 && imei2.isNotBlank()) {
                    Text(
                        text = "Dual IMEI: IMEI 1 ($imei) | IMEI 2 ($imei2)",
                        color = Color(0xFF16A34A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Specifications Box: Color, Storage, Variant, Battery Health
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Specifications & Hardware Details",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA0AEC0) else Slate700,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Row 1: Color & Storage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Color Dropdown
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
                                    label = { Text("Color", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorExpanded) },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .testTag("edit_phone_color_select")
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
                                                    color = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900),
                                                    fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900)
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

                            // Storage Dropdown
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
                                    label = { Text("Storage", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ramRomExpanded) },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .testTag("edit_phone_ramrom_select")
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
                                                    color = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900),
                                                    fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900)
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

                        // Row 2: Variant, Battery Health & Item Condition
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Variant Dropdown
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
                                    label = { Text("Variant", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = variantExpanded) },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .testTag("edit_phone_variant_select")
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
                                                    color = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900),
                                                    fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900)
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

                            // Battery Health Dropdown
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
                                    label = { Text("Battery %", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = batteryExpanded) },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .testTag("edit_phone_battery_select")
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
                                                    color = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900),
                                                    fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900)
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

                // 1. Entry Date (Calendar Date Picker)
                AppDatePickerField(
                    label = "Entry Date (YYYY-MM-DD)",
                    selectedDate = entryDate.ifBlank { PhoneHisabViewModel.getTodayDate() },
                    onDateSelected = { entryDate = it },
                    testTag = "edit_phone_entry_date",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Item Condition Dropdown (Directly UNDERNEATH Entry Date)
                ExposedDropdownMenuBox(
                    expanded = itemConditionExpanded,
                    onExpandedChange = { itemConditionExpanded = !itemConditionExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = itemCondition,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Condition *", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemConditionExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("edit_phone_condition_select")
                    )
                    ExposedDropdownMenu(
                        expanded = itemConditionExpanded,
                        onDismissRequest = { itemConditionExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                    ) {
                        dynamicItemConditions.forEach { opt ->
                            val isCustom = opt.startsWith("Custom") || opt == "Other"
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = opt,
                                        color = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900),
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900)
                                ),
                                onClick = {
                                    if (isCustom) {
                                        showCustomItemConditionDialog = true
                                    } else {
                                        itemCondition = opt
                                    }
                                    itemConditionExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Phone Grade Dropdown (Directly UNDERNEATH Condition Dropdown)
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
                        label = { Text("Phone Grade *", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("edit_phone_grade_select")
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
                                        color = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900),
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900)
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
                        label = { Text("Included Accessories / Packaging", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = packagingExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("edit_phone_packaging_select")
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
                                        color = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900),
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900)
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

                // Notes / Remarks (Compact Size)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Remarks", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                    placeholder = { Text("Accessories, condition notes...", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("edit_phone_notes")
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                // Section 3: Admin Landing Costs (Confidential)
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF2D3748) else Color(0xFFF1F5F9)),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF4A5568) else CardBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Landing Costs Breakdown 👑",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = buyingPriceRmbText,
                                onValueChange = {
                                    buyingPriceRmbText = it
                                    chinaPriceError = false
                                    val r = it.toDoubleOrNull() ?: 0.0
                                    buyingPriceBdtText = if (r > 0) (r * rmbRate).toInt().toString() else ""
                                    if (buyingPriceBdtText.isNotBlank()) buyPriceError = false
                                },
                                label = { Text("China Price (¥) *", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                placeholder = { Text("2800", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                singleLine = true,
                                isError = chinaPriceError,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = appTextFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .testTag("edit_cost_rmb_input")
                            )

                            OutlinedTextField(
                                value = buyingPriceBdtText,
                                onValueChange = {
                                    buyingPriceBdtText = it
                                    buyPriceError = false
                                },
                                label = { Text("Buy Price (৳) *", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                placeholder = { Text("49000", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                singleLine = true,
                                isError = buyPriceError,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = appTextFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .testTag("edit_cost_bdt_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = shippingCostRmbText,
                                onValueChange = {
                                    shippingCostRmbText = it
                                    airCargoError = false
                                },
                                label = { Text("Air Cargo (¥) *", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                placeholder = { Text("150", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                singleLine = true,
                                isError = airCargoError,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = appTextFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .testTag("edit_cost_shipping_input")
                            )

                            OutlinedTextField(
                                value = otherCostRmbText,
                                onValueChange = {
                                    otherCostRmbText = it
                                    otherCostError = false
                                },
                                label = { Text("Other Cost (¥ RMB) *", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                placeholder = { Text("50", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                singleLine = true,
                                isError = otherCostError,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = appTextFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .testTag("edit_cost_other_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Desired Profit input
                        OutlinedTextField(
                            value = desiredProfitBdtText,
                            onValueChange = {
                                desiredProfitBdtText = it
                                desiredProfitError = false
                            },
                            label = { Text("Desired Profit (৳ BDT)", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                            placeholder = {
                                val autoHint = if (calculatedTotalCost < 50000.0) "Auto: 7,000 (<50k)" else "Auto: 10,000 (≥50k)"
                                Text(autoHint, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                            },
                            singleLine = true,
                            isError = desiredProfitError,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("edit_desired_profit_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

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
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
                                            text = if (desiredProfitBdtText.isBlank()) "Desired Profit (Auto):" else "Desired Profit:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "(Base Cost + Desired Profit)",
                                            fontSize = 9.sp,
                                            color = if (isDark) Color(0xFFA0AEC0) else Slate700,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                // Section 4: Sales Record & Customer Info
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF0FDF4)),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFBBF7D0)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF4ADE80) else ProfitGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sales & Customer Info",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = sellingPriceBdtText,
                                onValueChange = { sellingPriceBdtText = it },
                                label = { Text("Selling Price (৳) *", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                placeholder = { Text("e.g. 58000", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = appTextFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .testTag("edit_selling_price_input")
                            )

                            AppDatePickerField(
                                label = "Sale Date",
                                selectedDate = saleDate.ifBlank { PhoneHisabViewModel.getTodayDate() },
                                onDateSelected = { saleDate = it },
                                testTag = "edit_sale_date_input",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (sellingPriceVal > 0 && calculatedTotalCost > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Estimated Margin/Profit: ৳ ${String.format("%,.0f", estimatedProfit)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (estimatedProfit >= 0) ProfitGreen else Color(0xFFDC2626),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Customer info
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it.uppercase() },
                            label = { Text("Customer Name", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                            placeholder = { Text("Customer full name", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("edit_customer_name_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customerPhone,
                                onValueChange = { customerPhone = it },
                                label = { Text("Phone Number", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                placeholder = { Text("017XXXXXXXX", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = appTextFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .testTag("edit_customer_phone_input")
                            )

                            OutlinedTextField(
                                value = customerEmail,
                                onValueChange = { customerEmail = it },
                                label = { Text("Email Address", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                placeholder = { Text("user@email.com", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = appTextFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .testTag("edit_customer_email_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = soldByStaff,
                                onValueChange = { soldByStaff = it },
                                label = { Text("Sold By Staff", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                placeholder = { Text("Staff Name", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                colors = appTextFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .testTag("edit_sold_by_input")
                            )

                            // Payment Method
                            ExposedDropdownMenuBox(
                                expanded = paymentExpanded,
                                onExpandedChange = { paymentExpanded = !paymentExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = paymentMethod,
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    label = { Text("Payment", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                                    colors = appTextFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .testTag("edit_payment_method_select")
                                )
                                ExposedDropdownMenu(
                                    expanded = paymentExpanded,
                                    onDismissRequest = { paymentExpanded = false },
                                    modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                                ) {
                                    dynamicPaymentMethods.forEach { method ->
                                        val isCustom = method.startsWith("Custom") || method == "Other"
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = method,
                                                    color = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900),
                                                    fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900)
                                            ),
                                            onClick = {
                                                if (isCustom) {
                                                    showCustomPaymentDialog = true
                                                } else {
                                                    paymentMethod = method
                                                }
                                                paymentExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = warrantyDaysText,
                            onValueChange = { warrantyDaysText = it },
                            label = { Text("Warranty (Days)", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                            placeholder = { Text("10", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("edit_warranty_days_input")
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppDialogConfirmButton(
                text = "Save Changes",
                gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)),
                testTag = "submit_edit_phone_btn",
                onClick = {
                    if (model.isBlank()) {
                        errorMessage = "Phone model cannot be empty!"
                        return@AppDialogConfirmButton
                    }
                    if (imei.isBlank() || imei.length != 15 || !imei.all { it.isDigit() }) {
                        errorMessage = "IMEI Number must be exactly 15 digits long"
                        return@AppDialogConfirmButton
                    }

                    // Strict Duplicate IMEI Prevention on Edit
                    val cleanImei = imei.trim()
                    val duplicate = existingPhones.firstOrNull {
                        it.id != phone.id && (
                            it.imei.trim().equals(cleanImei, ignoreCase = true) ||
                            (!it.imei2.isNullOrBlank() && it.imei2.trim().equals(cleanImei, ignoreCase = true))
                        )
                    }
                    if (duplicate != null) {
                        errorMessage = "Duplicate IMEI! This device is already registered in stock/transit."
                        return@AppDialogConfirmButton
                    }

                    if (imei2.isNotBlank()) {
                        val cleanImei2 = imei2.trim()
                        val duplicate2 = existingPhones.firstOrNull {
                            it.id != phone.id && (
                                it.imei.trim().equals(cleanImei2, ignoreCase = true) ||
                                (!it.imei2.isNullOrBlank() && it.imei2.trim().equals(cleanImei2, ignoreCase = true))
                            )
                        }
                        if (duplicate2 != null) {
                            errorMessage = "Duplicate IMEI! This device is already registered in stock/transit."
                            return@AppDialogConfirmButton
                        }
                    }

                    // Mandatory Landing Costs validation (China Price, Buy Price, Air Cargo, Other Cost)
                    val isChinaEmpty = buyingPriceRmbText.isBlank()
                    val isBuyEmpty = buyingPriceBdtText.isBlank()
                    val isAirCargoEmpty = shippingCostRmbText.isBlank()
                    val isOtherCostEmpty = otherCostRmbText.isBlank()

                    chinaPriceError = isChinaEmpty
                    buyPriceError = isBuyEmpty
                    airCargoError = isAirCargoEmpty
                    otherCostError = isOtherCostEmpty
                    desiredProfitError = false

                    if (isChinaEmpty || isBuyEmpty || isAirCargoEmpty || isOtherCostEmpty) {
                        errorMessage = "Please fill in all Landing Cost fields (China Price, Buy Price, Air Cargo, Other Cost)"
                        return@AppDialogConfirmButton
                    }

                    val resolvedBatteryHealth = batteryHealth.trim().ifBlank { "100%" }

                    val finalStatus = if (phone.status == PhoneStatus.SOLD.code && (status == PhoneStatus.IN_STOCK.code || status == PhoneStatus.RETURNED.code)) {
                        PhoneStatus.RETURNED.code
                    } else {
                        status
                    }

                    val isTransitToStock = (phone.status.equals(PhoneStatus.IN_TRANSIT.code, ignoreCase = true) || phone.status.equals("TRANSIT", ignoreCase = true)) &&
                        (finalStatus.equals(PhoneStatus.IN_STOCK.code, ignoreCase = true) || finalStatus.equals("IN_STOCK", ignoreCase = true) || finalStatus.equals("STOCK", ignoreCase = true))
                    val effectiveEntryDate = if (isTransitToStock) com.example.util.DateUtils.getTodayStorageDate() else entryDate.trim()
                    val now = System.currentTimeMillis()

                    val updatedPhone = phone.copy(
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
                        status = finalStatus,
                        notes = notes.trim(),
                        buyingPriceRmb = rmbVal,
                        buyingPriceBdt = effectiveBuyingBdt,
                        shippingCostBdt = effectiveShippingBdt,
                        otherCostBdt = otherBdt,
                        totalCostBdt = calculatedTotalCost,
                        desiredProfitBdt = desiredProfitVal,
                        sellingPriceBdt = sellingPriceVal,
                        saleDate = saleDate.trim(),
                        customerName = customerName.trim().uppercase(),
                        customerPhone = customerPhone.trim(),
                        customerEmail = customerEmail.trim(),
                        soldByStaff = soldByStaff.trim(),
                        paymentMethod = paymentMethod.trim(),
                        warrantyDays = warrantyDaysText.toIntOrNull() ?: 10,
                        createdAt = if (isTransitToStock) now else phone.createdAt,
                        updatedAt = now
                    )

                    onSaveEdit(updatedPhone)
                }
            )
        },
        dismissButton = {
            AppDialogDismissButton(
                text = "Cancel",
                testTag = "cancel_edit_phone_btn",
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
            placeholder = "e.g. Nothing, Sony, ASUS, Tecno",
            initialValue = if (brand in BRAND_OPTIONS) "" else brand,
            onDismiss = { showCustomBrandDialog = false },
            onConfirm = { customValue ->
                if (brand != customValue) {
                    brand = customValue
                    dynamicStorage = getStorageOptionsForBrand(customValue)
                    ramRom = getDefaultStorageForBrand(customValue)
                }
                if (!dynamicBrands.contains(customValue)) {
                    val customIdx = dynamicBrands.indexOfFirst { it.startsWith("Custom") || it == "Other" }
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
            placeholder = "e.g. Phone (2a) Plus, ROG Phone 8",
            initialValue = if (model.startsWith("Custom")) "" else model,
            onDismiss = { showCustomModelDialog = false },
            onConfirm = { customValue ->
                model = cleanModelForBrand(brand, customValue)
                showCustomModelDialog = false
            }
        )
    }

    // Custom Color Dialog
    if (showCustomColorDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Phone Color",
            label = "Color Finish",
            placeholder = "e.g. Alpine Green, Cosmic Orange, Titanium Gray",
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
            label = "Storage / RAM",
            placeholder = "e.g. 18GB / 1TB, 32GB / 2TB",
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
            label = "Variant Name",
            placeholder = "e.g. Canada VC/A, Singapore ZA/A, Korea KH/A",
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
            title = "Enter Custom Battery % / Note",
            label = "Battery % / Note",
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

    // Custom Payment Method Dialog
    if (showCustomPaymentDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Payment Method",
            label = "Payment Method",
            placeholder = "e.g. Crypto / USDT, City Bank POS",
            initialValue = if (paymentMethod in PAYMENT_METHODS_EN) "" else paymentMethod,
            onDismiss = { showCustomPaymentDialog = false },
            onConfirm = { customValue ->
                paymentMethod = customValue
                if (!dynamicPaymentMethods.contains(customValue)) {
                    val customIdx = dynamicPaymentMethods.indexOfFirst { it.startsWith("Custom") || it == "Other" }
                    dynamicPaymentMethods = if (customIdx >= 0) {
                        dynamicPaymentMethods.toMutableList().apply { add(customIdx, customValue) }
                    } else {
                        dynamicPaymentMethods + customValue
                    }
                }
                showCustomPaymentDialog = false
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
    if (showCustomItemConditionDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Item Condition",
            label = "Item Condition",
            placeholder = "e.g. Refurbished, Open Box, Demo Unit",
            initialValue = if (itemCondition in ITEM_CONDITION_OPTIONS) "" else itemCondition,
            onDismiss = { showCustomItemConditionDialog = false },
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
                showCustomItemConditionDialog = false
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
