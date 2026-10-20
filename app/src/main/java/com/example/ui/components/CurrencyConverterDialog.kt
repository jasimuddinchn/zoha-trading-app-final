package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppStrings
import com.example.ui.theme.BdtGreen
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.RmbRed
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme

@Composable
fun CurrencyConverterDialog(
    currentRate: Double,
    isBengali: Boolean,
    onDismiss: () -> Unit
) {
    var rmbInput by remember { mutableStateOf("2500") }
    var bdtInput by remember { mutableStateOf("") }
    var isRmbToBdt by remember { mutableStateOf(true) }

    val calculatedBdt = (rmbInput.toDoubleOrNull() ?: 0.0) * currentRate
    val calculatedRmb = if (currentRate > 0) (bdtInput.toDoubleOrNull() ?: 0.0) / currentRate else 0.0
    val isDark = isAppInDarkTheme()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CurrencyExchange,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = if (isBengali) "মুদ্রা রূপান্তর ক্যালকুলেটর (¥ ⇄ ৳)" else "Currency Converter (¥ ⇄ ৳)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isDark) Color.White else Slate900
                )
            }
        },
        text = {
            Column {
                // Rate notice
                Surface(
                    color = if (isDark) Color(0xFF1E293B) else Slate100,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isBengali) {
                            "বর্তমান এক্সচেঞ্জ রেট: ১ RMB (¥) = ${String.format("%.2f", currentRate)} টাকা (৳)"
                        } else {
                            "Exchange Rate: 1 RMB (¥) = ${String.format("%.2f", currentRate)} BDT (৳)"
                        },
                        fontSize = 12.sp,
                        color = Slate700,
                        modifier = Modifier.padding(8.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isRmbToBdt) {
                    OutlinedTextField(
                        value = rmbInput,
                        onValueChange = { rmbInput = it },
                        label = { Text(if (isBengali) "চায়না দাম (¥ RMB)" else "China Price (¥ RMB)") },
                        placeholder = { Text("3500") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rmb_calc_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFECFDF5)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if (isDark) Color(0xFF059669) else Color(0xFFA7F3D0))),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isBengali) "বাংলাদেশি টাকায় মোট (BDT):" else "Equivalent in BDT:",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF34D399) else BdtGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "৳ ${String.format("%,.2f", calculatedBdt)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFF34D399) else BdtGreen
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = bdtInput,
                        onValueChange = { bdtInput = it },
                        label = { Text(if (isBengali) "বাংলাদেশি টাকা (৳ BDT)" else "BDT Amount (৳)") },
                        placeholder = { Text("50000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bdt_calc_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF450A0A).copy(alpha = 0.5f) else Color(0xFFFFECEB)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if (isDark) Color(0xFFDC2626) else Color(0xFFFECDD3))),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isBengali) "চায়না ইউয়ানে পরিমাণ (RMB):" else "Equivalent in RMB (¥):",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFFF87171) else RmbRed,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "¥ ${String.format("%,.2f", calculatedRmb)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFFF87171) else RmbRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { isRmbToBdt = !isRmbToBdt },
                        modifier = Modifier.testTag("switch_calc_direction_btn")
                    ) {
                        Icon(imageVector = Icons.Default.SwapVert, contentDescription = "Swap", tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy)
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text(
                            text = if (isRmbToBdt) {
                                if (isBengali) "টাকা থেকে আরএমবি করুন" else "Switch to BDT ➔ RMB"
                            } else {
                                if (isBengali) "আরএমবি থেকে টাকা করুন" else "Switch to RMB ➔ BDT"
                            },
                            color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2563EB) else PrimaryNavy),
                modifier = Modifier.testTag("close_calc_btn")
            ) {
                Text(AppStrings.close(isBengali), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isDark) Color(0xFF1E293B) else CardBg
    )
}
