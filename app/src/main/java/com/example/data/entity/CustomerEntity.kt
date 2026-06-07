package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database Entity representing a Customer profile in Gh POS.
 */
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val address: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = 0L
)
