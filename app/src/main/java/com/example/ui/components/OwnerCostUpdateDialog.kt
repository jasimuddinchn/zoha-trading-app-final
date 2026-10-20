package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PhoneItem
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme

@Composable
fun OwnerCostUpdateDialog(
    phone: PhoneItem,
    rmbRate: Double,
    isBengali: Boolean = false,
    onDismiss: () -> Unit,
    onUpdateCosts: (
        buyingPriceRmb: Double,
        buyingPriceBdt: Double,
        shippingCostBdt: Double,
        otherCostBdt: Double,
        desiredProfitBdt: Double
    ) -> Unit
) {
    var rmbText by remember {
        mutableStateOf(if (phone.buyingPriceRmb > 0) phone.buyingPriceRmb.toInt().toString() else "")
    }
    var bdtText by remember {
        mutableStateOf(if (phone.buyingPriceBdt > 0) phone.buyingPriceBdt.toInt().toString() else "")
    }
    var shippingRmbText by remember {
        mutableStateOf(
            if (phone.shippingCostBdt > 0 && rmbRate > 0) {
                (phone.shippingCostBdt / rmbRate).toInt().toString()
            } else ""
        )
    }
    var otherRmbText by remember {
        mutableStateOf(
            if (phone.otherCostBdt > 0 && rmbRate > 0) {
                (phone.otherCostBdt / rmbRate).toInt().toString()
            } else if (phone.otherCostBdt > 0) {
                phone.otherCostBdt.toInt().toString()
            } else ""
        )
    }
    var desiredProfitText by remember {
        mutableStateOf(if (phone.desiredProfitBdt > 0) phone.desiredProfitBdt.toInt().toString() else "")
    }

    val rmbVal = rmbText.toDoubleOrNull() ?: 0.0
    val bdtVal = bdtText.toDoubleOrNull() ?: 0.0
    val effectiveBdt = if (bdtVal > 0) bdtVal else (rmbVal * rmbRate)
    val shippingRmbVal = shippingRmbText.toDoubleOrNull() ?: 0.0
    val effectiveShippingBdt = shippingRmbVal * rmbRate
    val otherRmbVal = otherRmbText.toDoubleOrNull() ?: 0.0
    val effectiveOtherBdt = if (rmbRate > 0) otherRmbVal * rmbRate else otherRmbVal
    val totalCost = effectiveBdt + effectiveShippingBdt + effectiveOtherBdt
    val manualProfitVal = desiredProfitText.toDoubleOrNull()
    val defaultAdminProfit = if (totalCost < 50000.0) 7000.0 else 10000.0
    val effectiveDesiredProfit = manualProfitVal ?: defaultAdminProfit
    val desiredProfitVal = effectiveDesiredProfit
    val calculatedAskingPrice = if (totalCost > 0.0 || manualProfitVal != null) {
        totalCost + effectiveDesiredProfit
    } else {
        0.0
    }
    val isDark = isAppInDarkTheme()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Edit Landing Costs (Admin)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isDark) Color.White else Slate900
                    )
                    Text(
                        text = "${phone.brand} ${phone.model}",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFA0AEC0) else Slate700
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Update the China purchase price and cargo costs for this device:",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Slate700
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = rmbText,
                        onValueChange = {
                            rmbText = it
                            val r = it.toDoubleOrNull() ?: 0.0
                            bdtText = if (r > 0) (r * rmbRate).toInt().toString() else ""
                        },
                        label = { Text("China Price (¥ RMB)") },
                        placeholder = { Text("2800") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cost_rmb_input")
                    )

                    OutlinedTextField(
                        value = bdtText,
                        onValueChange = { bdtText = it },
                        label = { Text("Buy Price (৳ BDT)") },
                        placeholder = { Text("49000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cost_bdt_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = shippingRmbText,
                        onValueChange = { shippingRmbText = it },
                        label = { Text("Air Cargo (¥)") },
                        placeholder = { Text("150") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cost_shipping_input")
                    )

                    OutlinedTextField(
                        value = otherRmbText,
                        onValueChange = { otherRmbText = it },
                        label = { Text("Other Cost (¥ RMB)") },
                        placeholder = { Text("50") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cost_other_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = desiredProfitText,
                    onValueChange = { desiredProfitText = it },
                    label = { Text("Desired Profit / Margin (৳ BDT)") },
                    placeholder = {
                        val autoHint = if (totalCost < 50000.0) "Auto: 7,000 (<50k)" else "Auto: 10,000 (≥50k)"
                        Text(autoHint)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cost_desired_profit_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Base Cost:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700
                            )
                            Text(
                                text = "৳ ${String.format("%,.0f", totalCost)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
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
                                    text = if (desiredProfitText.isBlank()) "Desired Profit (Auto):" else "Desired Profit:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                                )
                                Text(
                                    text = "+৳ ${String.format("%,.0f", desiredProfitVal)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.material3.HorizontalDivider(color = if (isDark) Color(0xFF374151) else Color(0xFFCBD5E1))
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
                                    color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy
                                )
                                Text(
                                    text = "(Base Cost + Desired Profit)",
                                    fontSize = 9.sp,
                                    color = Slate700
                                )
                            }
                            Text(
                                text = "৳ ${String.format("%,.0f", calculatedAskingPrice)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalBuyingBdt = if (bdtVal > 0) bdtVal else (rmbVal * rmbRate)
                    val finalDesiredProfit = when {
                        desiredProfitText.isNotBlank() -> desiredProfitText.toDoubleOrNull() ?: 0.0
                        else -> if (totalCost < 50000.0) 7000.0 else 10000.0
                    }
                    onUpdateCosts(rmbVal, finalBuyingBdt, effectiveShippingBdt, effectiveOtherBdt, finalDesiredProfit)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2563EB) else PrimaryNavy),
                modifier = Modifier.testTag("save_owner_cost_btn")
            ) {
                Text("Update Cost", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_owner_cost_btn")
            ) {
                Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Medium)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isDark) Color(0xFF1E293B) else CardBg
    )
}
