package com.example.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemDao {
    @Query("SELECT * FROM checklist_items ORDER BY id ASC")
    fun getAllItems(): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllItemsList(): List<ChecklistItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChecklistItem>)

    @Update
    suspend fun updateItem(item: ChecklistItem)

    @Query("SELECT COUNT(*) FROM checklist_items")
    suspend fun getItemCount(): Int
}
