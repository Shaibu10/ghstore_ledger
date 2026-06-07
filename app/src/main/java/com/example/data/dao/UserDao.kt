package com.example.data.dao

import androidx.room.*
import com.example.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local User table operations in Gh POS.
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)

    // For background online sync tracking: retrieve any users updated after their last sync
    @Query("SELECT * FROM users WHERE updatedAt > lastSyncedAt")
    suspend fun getUnsyncedUsers(): List<UserEntity>
}
