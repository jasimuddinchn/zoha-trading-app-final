package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.PhoneItem
import com.example.data.model.formatGradeLetter
import com.example.data.model.getDisplayNameWithGrade
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.DateUtils

@Composable
fun CashMemoDialog(
    phone: PhoneItem,
    settings: AppSettings,
    isBengali: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val rawDate = phone.saleDate.ifBlank { phone.entryDate }
    val displayFormattedDate = DateUtils.formatDisplayDate(rawDate)

    val memoText = buildString {
        appendLine("===============================")
        appendLine("       ${settings.shopName}")
        appendLine("   ${settings.shopAddress}")
        appendLine("       Phone: ${settings.shopPhone}")
        appendLine("===============================")
        appendLine("      SALES CASH MEMO")
        appendLine("Date: $displayFormattedDate")
        appendLine("Invoice #: PH-${phone.id}")
        appendLine("-------------------------------")
        appendLine("Customer: ${phone.customerName.ifBlank { "Valued Customer" }}")
        if (phone.customerPhone.isNotBlank()) appendLine("Phone: ${phone.customerPhone}")
        appendLine("-------------------------------")
        appendLine("Model: ${phone.brand} ${phone.model}")
        appendLine("IMEI 1: ${phone.imei}")
        if (phone.imei2.isNotBlank()) appendLine("IMEI 2: ${phone.imei2}")
        appendLine("Color & RAM/ROM: ${phone.color} / ${phone.ramRom}")
        appendLine("Condition: ${phone.conditionGrade}")
        appendLine("-------------------------------")
        appendLine("Total Paid: ৳ ${String.format("%,.0f", phone.sellingPriceBdt)}")
        appendLine("Payment Method: ${phone.paymentMethod.ifBlank { "Cash" }}")
        appendLine("Sold By: ${phone.soldByStaff.ifBlank { "Staff" }}")
        appendLine("Warranty: ${phone.warrantyDays} Days Replacement")
        appendLine("-------------------------------")
        appendLine(settings.defaultWarrantyText)
        appendLine("  Thank you for your purchase!")
        appendLine("===============================")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Digital Invoice / Memo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isDark) Color.White else Slate900
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                // Receipt Card Container
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Zoha Trading Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = settings.shopName.ifBlank { "ZOHA TRADING" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = settings.shopAddress,
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Slate700,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Hotline: ${settings.shopPhone}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFCBD5E1) else Slate700
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = CardBorder)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Invoice & Date Key-Value Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Invoice #:", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "#${phone.id}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Date:", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = displayFormattedDate,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Slate900,
                                    maxLines = 1
                                )
                            }
                        }

                        if (phone.customerName.isNotBlank() || phone.customerPhone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Customer:", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = phone.customerName.trim().uppercase().ifBlank { "VALUED CUSTOMER" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else Slate900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (phone.customerPhone.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Phone:", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Medium)
                                            Text(
                                                text = phone.customerPhone,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDark) Color.White else Slate900,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = CardBorder)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Device Details Structured Key-Value Section
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = phone.getDisplayNameWithGrade(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "৳ ${String.format("%,.0f", phone.sellingPriceBdt)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Structured Item Details Rows
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("IMEI 1:", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Medium)
                                Text(
                                    text = phone.imei,
                                    fontSize = 11.sp,
                                    color = if (isDark) Color.White else Slate900,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (phone.imei2.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("IMEI 2:", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = phone.imei2,
                                        fontSize = 11.sp,
                                        color = if (isDark) Color.White else Slate900,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Color / Storage:", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${phone.color} / ${phone.ramRom}",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color.White else Slate900,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Condition:", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Medium)
                                Text(
                                    text = formatGradeLetter(phone.conditionGrade),
                                    fontSize = 11.sp,
                                    color = if (isDark) Color.White else Slate900,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = CardBorder)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Total Price Highlight Box
                        Surface(
                            color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFECFDF5),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Paid:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF065F46),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "৳ ${String.format("%,.0f", phone.sellingPriceBdt)}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF065F46),
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Payment Method & Staff Key-Value Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Method:", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = phone.paymentMethod.ifBlank { "Cash" },
                                    fontSize = 11.sp,
                                    color = if (isDark) Color.White else Slate900,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text("Seller:", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = phone.soldByStaff.ifBlank { "Staff" },
                                    fontSize = 11.sp,
                                    color = if (isDark) Color.White else Slate900,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Warranty: ${phone.warrantyDays} Days replacement. ${settings.defaultWarrantyText}",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Slate700,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Copy Memo and Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppOutlinedActionButton(
                        text = "Copy Memo",
                        icon = Icons.Default.ContentCopy,
                        borderColor = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                        contentColor = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_memo_btn"),
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Cash Memo", memoText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Memo copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    AppActionButton(
                        text = "Share",
                        icon = Icons.Default.Share,
                        gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_memo_btn"),
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Invoice - ${phone.brand} ${phone.model}")
                                putExtra(Intent.EXTRA_TEXT, memoText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Memo"))
                        }
                    )
                }
            }
        },
        confirmButton = {
            AppDialogDismissButton(
                text = "Close",
                testTag = "close_memo_dialog_btn",
                onClick = onDismiss
            )
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isDark) Color(0xFF1E293B) else CardBg
    )
}
