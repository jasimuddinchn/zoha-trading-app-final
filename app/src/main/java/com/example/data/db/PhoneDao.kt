package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PhoneItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneDao {
    @Query("SELECT * FROM phones ORDER BY entryDate DESC, createdAt DESC, id DESC")
    fun getAllPhones(): Flow<List<PhoneItem>>

    @Query("SELECT * FROM phones ORDER BY entryDate DESC, createdAt DESC, id DESC")
    suspend fun getAllPhonesDirect(): List<PhoneItem>

    @Query("SELECT * FROM phones WHERE status = :status ORDER BY CASE WHEN :status = 'SOLD' THEN saleDate ELSE entryDate END DESC, updatedAt DESC, createdAt DESC, id DESC")
    fun getPhonesByStatus(status: String): Flow<List<PhoneItem>>

    @Query("SELECT * FROM phones WHERE id = :id")
    suspend fun getPhoneById(id: Long): PhoneItem?

    @Query("SELECT * FROM phones WHERE imei LIKE '%' || :query || '%' OR model LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%'")
    fun searchPhones(query: String): Flow<List<PhoneItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhone(phone: PhoneItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(phones: List<PhoneItem>)

    @Update
    suspend fun updatePhone(phone: PhoneItem)

    @Delete
    suspend fun deletePhone(phone: PhoneItem)

    @Query("DELETE FROM phones WHERE id = :id")
    suspend fun deletePhoneById(id: Long)

    @Query("DELETE FROM phones")
    suspend fun deleteAllPhones()

    @Query("SELECT COUNT(*) FROM phones")
    suspend fun getPhoneCount(): Int
}
