package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val rmbToBdtRate: Double = 17.50, // 1 RMB = 17.50 BDT default
    val ownerPin: String = "1111", // Default PIN for Admin (Jasim)
    val staff1Pin: String = "2222", // Default PIN for Sumon
    val staff2Pin: String = "3333", // Default PIN for Deedat
    val isAdminPinSetupCompleted: Boolean = true, // PINs ready with defaults
    val adminName: String = "Jasim",
    val adminNameBn: String = "Jasim",
    val adminNameEn: String = "Jasim",
    val staff1NameBn: String = "Sumon",
    val staff1NameEn: String = "Sumon",
    val staff2NameBn: String = "Deedat",
    val staff2NameEn: String = "Deedat",
    val shopName: String = "Zoha Trading",
    val shopSubtitle: String = "Trading Worldwide Since 2012",
    val shopAddress: String = "Shop #204, Level 3, Eastern Plaza, Dhaka",
    val shopPhone: String = "01755-987654",
    val staff1Name: String = "Sumon",
    val staff2Name: String = "Deedat",
    val defaultWarrantyText: String = "10 days replacement warranty and 2 years free service warranty.",
    val ownerNotificationEmail: String = "jasimuddin.chn@gmail.com",
    val appLanguage: String = "en", // Pure English
    val themeMode: String = "LIGHT", // "LIGHT", "DARK", "SYSTEM"
    val dynamicColor: Boolean = true // Material 3 dynamic color support
) {
    fun getAdminName(isBn: Boolean = false): String = adminName.ifBlank { adminNameEn.ifBlank { "Jasim" } }
    fun getStaff1Name(isBn: Boolean = false): String = staff1Name.ifBlank { staff1NameEn.ifBlank { "Sumon" } }
    fun getStaff2Name(isBn: Boolean = false): String = staff2Name.ifBlank { staff2NameEn.ifBlank { "Deedat" } }
}
