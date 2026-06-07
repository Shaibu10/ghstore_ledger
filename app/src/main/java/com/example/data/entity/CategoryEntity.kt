package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database Entity representing a Product Category in Gh POS.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = 0L
)
