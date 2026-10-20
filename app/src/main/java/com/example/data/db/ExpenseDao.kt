package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ExpenseItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC, id DESC")
    fun getAllExpenses(): Flow<List<ExpenseItem>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC, id DESC")
    suspend fun getAllExpensesDirect(): List<ExpenseItem>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): ExpenseItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseItem>)

    @Update
    suspend fun updateExpense(expense: ExpenseItem)

    @Delete
    suspend fun deleteExpense(expense: ExpenseItem)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getExpenseCount(): Int
}
