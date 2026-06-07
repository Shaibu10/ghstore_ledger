package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database Entity representing an entry item within a specific invoice transaction.
 */
@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey
    val id: String,
    val saleId: String, // foreign key relation linking to SaleEntity id
    val productId: String, // foreign key relation linking to ProductEntity id
    val quantity: Double,
    val price: Double, // unit price recorded at checkout time
    val subtotal: Double,
    val lastSyncedAt: Long = 0L
)
