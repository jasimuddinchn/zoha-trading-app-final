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
import androidx.compose.material.icons.filled.Description
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
fun InvoiceDialog(
    phone: PhoneItem,
    settings: AppSettings,
    isBengali: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val rawDate = phone.saleDate.ifBlank { phone.entryDate }
    val displayFormattedDate = DateUtils.formatDisplayDate(rawDate)

    val invoiceText = buildString {
        appendLine("==================================================")
        appendLine("                OFFICIAL SALES INVOICE            ")
        appendLine("==================================================")
        appendLine("Shop: ${settings.shopName}")
        appendLine("Tagline: ${settings.shopSubtitle}")
        appendLine("Address: ${settings.shopAddress}")
        appendLine("Hotline: ${settings.shopPhone}")
        appendLine("--------------------------------------------------")
        appendLine("INVOICE NO: INV-ZT-${phone.id}")
        appendLine("DATE: $displayFormattedDate")
        appendLine("BILL TO:")
        appendLine("Customer: ${phone.customerName.ifBlank { "Valued Customer" }}")
        if (phone.customerPhone.isNotBlank()) appendLine("Phone: ${phone.customerPhone}")
        appendLine("--------------------------------------------------")
        appendLine("ITEM DESCRIPTION: ${phone.brand} ${phone.model}")
        appendLine("IMEI: ${phone.imei}")
        appendLine("Color: ${phone.color} | Storage: ${phone.ramRom}")
        appendLine("Grade: ${phone.conditionGrade}")
        appendLine("Warranty: ${phone.warrantyDays} Days Replacement Warranty")
        appendLine("--------------------------------------------------")
        appendLine("TOTAL AMOUNT: ৳ ${String.format("%,.0f", phone.sellingPriceBdt)}")
        appendLine("PAYMENT METHOD: ${phone.paymentMethod.ifBlank { "Cash" }}")
        appendLine("PAYMENT STATUS: PAID (FULL)")
        appendLine("SERVED BY: ${phone.soldByStaff.ifBlank { "Authorized Staff" }}")
        appendLine("--------------------------------------------------")
        appendLine("TERMS & CONDITIONS:")
        appendLine(settings.defaultWarrantyText)
        appendLine("==================================================")
        appendLine("Thank you for choosing ${settings.shopName}!")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Official Sales Invoice",
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
                // Official Invoice Container
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
                        // Shop Header
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Zoha Trading Logo",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = settings.shopName.ifBlank { "ZOHA TRADING" },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                            textAlign = TextAlign.Center
                        )
                        if (settings.shopSubtitle.isNotBlank()) {
                            Text(
                                text = settings.shopSubtitle,
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Slate700,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = settings.shopAddress,
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Slate700,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Hotline: ${settings.shopPhone}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = CardBorder)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Invoice metadata row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("INVOICE NO:", fontSize = 10.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "INV-ZT-${phone.id}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("DATE:", fontSize = 10.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = displayFormattedDate,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Slate900,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Customer Details Box
                        Surface(
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("BILL TO:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF94A3B8) else Slate700)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Customer Name:", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Medium)
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
                                        Text("Mobile Number:", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = phone.customerPhone,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color.White else Slate900,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = CardBorder)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Table Breakdown Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ITEM DESCRIPTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF94A3B8) else Slate700)
                            Text("AMOUNT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF94A3B8) else Slate700)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = phone.getDisplayNameWithGrade(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color.White else Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("IMEI: ${phone.imei}", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (phone.imei2.isNotBlank()) {
                                    Text("IMEI 2: ${phone.imei2}", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text("Color: ${phone.color} | Storage: ${phone.ramRom}", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (phone.conditionGrade.isNotBlank()) {
                                    Text("Grade: ${formatGradeLetter(phone.conditionGrade)}", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Text(
                                text = "৳ ${String.format("%,.0f", phone.sellingPriceBdt)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Slate900,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = CardBorder)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Total Price Box
                        Surface(
                            color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFF0FDF4),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Grand Total (Paid):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF34D399) else Color(0xFF166534))
                                    Text("Paid via ${phone.paymentMethod.ifBlank { "Cash" }}", fontSize = 10.sp, color = if (isDark) Color(0xFF34D399) else Color(0xFF166534), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(
                                    text = "৳ ${String.format("%,.0f", phone.sellingPriceBdt)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF166534),
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Representative:", fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = phone.soldByStaff.ifBlank { "Staff" },
                                    fontSize = 11.sp,
                                    color = if (isDark) Color.White else Slate900,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            Text("Status: PAID", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF34D399) else Color(0xFF166534))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Warranty: ${phone.warrantyDays} Days Replacement Warranty. ${settings.defaultWarrantyText}",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Slate700,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Copy & Share Invoice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Sales Invoice", invoiceText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Invoice copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_invoice_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp, color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Invoice - ${phone.brand} ${phone.model}")
                                putExtra(Intent.EXTRA_TEXT, invoiceText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Invoice"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2563EB) else PrimaryNavy),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_invoice_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share Invoice", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_invoice_dialog_btn")
            ) {
                Text("Close", color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isDark) Color(0xFF1E293B) else CardBg
    )
}
