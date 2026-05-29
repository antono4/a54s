package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklist_items")
data class ChecklistItem(
    @PrimaryKey val id: Int,
    val title: String,
    val isChecked: Boolean = false,
    val description: String = ""
)
