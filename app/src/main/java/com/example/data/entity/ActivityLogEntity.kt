package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room persistence model representing an audit trail log entry.
 */
@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,       // The user executing this activity
    val actionType: String,     // e.g., "AUTH", "CATALOG", "SALES", "CLIENTS", "EXPENSES", "ACCESS"
    val description: String,    // Detailed description of the action taken
    val timestamp: Long = System.currentTimeMillis()
)
