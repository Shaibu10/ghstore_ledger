package com.example.data.repository

import com.example.data.dao.UserDao
import com.example.data.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository class that coordinates database access for User Profiles in Gh POS.
 * Integrates local Room storage and supports sync operations.
 */
class UserRepository(private val userDao: UserDao) {

    /**
     * Exposes a Flow of all users sorted alphabetically for reactive UI updates.
     */
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()

    /**
     * Inserts or replaces a user profile locally.
     */
    suspend fun saveUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    /**
     * Saves a list of user profiles (usually downloaded from cloud storage or sync).
     */
    suspend fun saveUsers(users: List<UserEntity>) = withContext(Dispatchers.IO) {
        userDao.insertUsers(users)
    }

    /**
     * Retrieves a user by their unique local/remote identifier.
     */
    suspend fun getUserById(id: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserById(id)
    }

    /**
     * Retrieves a user by their username (useful for local credentials checks).
     */
    suspend fun getUserByUsername(username: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserByUsername(username)
    }

    /**
     * Deletes a user profile locally.
     */
    suspend fun deleteUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.deleteUser(user)
    }

    /**
     * Deletes a user profile by ID.
     */
    suspend fun deleteUserById(id: String) = withContext(Dispatchers.IO) {
        userDao.deleteUserById(id)
    }

    /**
     * Fetches all user profiles that have changes unsaved to Firebase.
     */
    suspend fun getUnsyncedUsers(): List<UserEntity> = withContext(Dispatchers.IO) {
        userDao.getUnsyncedUsers()
    }
}
