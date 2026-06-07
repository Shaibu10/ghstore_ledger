package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database Entity representing a completed Sale Transaction in Gh POS.
 */
@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey
    val id: String,
    val invoiceNumber: String,
    val customerId: String?, // Nullable for general customer checkout
    val subtotal: Double,
    val discount: Double,
    val tax: Double,
    val total: Double,
    val paymentMethod: String, // CASH, MOBILE_MONEY, CARD
    val cashierId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = 0L
)
