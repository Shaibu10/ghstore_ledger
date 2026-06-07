package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.UserRole

/**
 * Room database Entity representing a User in the Gh POS System.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String, // UUID or Firebase Auth UID for seamless sync
    val name: String,
    val username: String,
    val passwordHash: String, // Encrypted/hashed password for local login Fallback
    val role: String, // "ADMINISTRATOR", "MANAGER", "CASHIER"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = 0L // Timestamp of the last successful backup or sync with firestore
)
