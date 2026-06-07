package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database Entity representing dynamic business overhead expenses.
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val amount: Double,
    val description: String,
    val date: Long, // Epoch timestamp representing transaction date
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = 0L
)
