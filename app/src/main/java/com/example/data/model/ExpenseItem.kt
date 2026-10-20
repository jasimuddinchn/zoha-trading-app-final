package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,
    val amountBdt: Double,
    val date: String, // e.g. "2026-08-22"
    val notes: String = "",
    val addedBy: String = "মালিক",
    val receiptUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Original creation timestamp preserved across edits for chronological sorting (newest first).
     */
    val entryDate: Long
        get() = timestamp

    val createdAt: Long
        get() = timestamp
}
