package com.example.data.model

/**
 * Defines the access roles available in the Gh POS System.
 */
enum class UserRole {
    ADMINISTRATOR,
    MANAGER,
    CASHIER;

    companion object {
        fun fromString(value: String): UserRole {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                CASHIER // Default safe constraint fallback
            }
        }
    }
}
