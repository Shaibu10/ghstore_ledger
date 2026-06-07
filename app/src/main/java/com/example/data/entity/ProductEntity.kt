package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database Entity representing a Product in Gh POS.
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val id: String, // UUID or centralized sync key
    val barcode: String,
    val name: String,
    val description: String,
    val categoryId: String, // foreign key/indexed reference
    val buyPrice: Double,
    val sellPrice: Double,
    val quantity: Double, // supports both integer and fractional measures e.g. kg
    val reorderLevel: Double,
    val imageUrl: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = 0L
)
