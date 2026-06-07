package com.example

import com.example.data.dao.UserDao
import com.example.data.entity.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the Gh POS User Profile data models, roles, and repository flows.
 */
class UserRepositoryTest {

    // A simple, light-weight Mock/Fake of UserDao to keep host unit tests extremely fast & isolated.
    private class FakeUserDao : UserDao {
        val users = mutableMapOf<String, UserEntity>()

        override fun getAllUsers(): Flow<List<UserEntity>> {
            return flowOf(users.values.toList().sortedBy { it.name })
        }

        override suspend fun getUserById(id: String): UserEntity? {
            return users[id]
        }

        override suspend fun getUserByUsername(username: String): UserEntity? {
            return users.values.find { it.username == username }
        }

        override suspend fun insertUser(user: UserEntity) {
            users[user.id] = user
        }

        override suspend fun insertUsers(usersList: List<UserEntity>) {
            usersList.forEach { users[it.id] = it }
        }

        override suspend fun updateUser(user: UserEntity) {
            users[user.id] = user
        }

        override suspend fun deleteUser(user: UserEntity) {
            users.remove(user.id)
        }

        override suspend fun deleteUserById(id: String) {
            users.remove(id)
        }

        override suspend fun getUnsyncedUsers(): List<UserEntity> {
            return users.values.filter { it.updatedAt > it.lastSyncedAt }
        }
    }

    @Test
    fun testUserRoleParsing() {
        assertEquals(UserRole.ADMINISTRATOR, UserRole.fromString("administrator"))
        assertEquals(UserRole.MANAGER, UserRole.fromString("Manager"))
        assertEquals(UserRole.CASHIER, UserRole.fromString("cashier"))
        assertEquals(UserRole.CASHIER, UserRole.fromString("nonexistent_role")) // Default safe fallback
    }

    @Test
    fun testUserEntityCreation() {
        val user = UserEntity(
            id = "user_001",
            name = "Shaibu Ali",
            username = "shaibu_admin",
            passwordHash = "argon2_or_bcrypt_hash_placeholder",
            role = UserRole.ADMINISTRATOR.name,
            createdAt = 1000000L,
            updatedAt = 1000000L,
            lastSyncedAt = 0L
        )

        assertEquals("user_001", user.id)
        assertEquals("Shaibu Ali", user.name)
        assertEquals("shaibu_admin", user.username)
        assertEquals("ADMINISTRATOR", user.role)
    }

    @Test
    fun testUserRepositoryOperations() = runBlocking {
        val fakeDao = FakeUserDao()
        val repository = UserRepository(fakeDao)

        val user1 = UserEntity(
            id = "uid_001",
            name = "Kofi Mensah",
            username = "kofi_cashier",
            passwordHash = "hash1",
            role = UserRole.CASHIER.name,
            createdAt = 1000L,
            updatedAt = 1000L,
            lastSyncedAt = 0L
        )

        // Test insertion
        repository.saveUser(user1)
        val retrieved = repository.getUserById("uid_001")
        assertNotNull(retrieved)
        assertEquals("Kofi Mensah", retrieved?.name)

        // Test username query
        val queryByUsername = repository.getUserByUsername("kofi_cashier")
        assertNotNull(queryByUsername)
        assertEquals("uid_001", queryByUsername?.id)

        // Test synchronization sync detection (updatedAt 1000 > lastSyncedAt 0)
        val unsynced = repository.getUnsyncedUsers()
        assertEquals(1, unsynced.size)
        assertEquals("uid_001", unsynced[0].id)

        // Test update to synchronized state
        val syncedUser = user1.copy(lastSyncedAt = 1200L, updatedAt = 1100L) // lastSyncedAt > updatedAt
        repository.saveUser(syncedUser)
        val unsyncedAfter = repository.getUnsyncedUsers()
        assertEquals(0, unsyncedAfter.size) // No unsynced records remaining

        // Test delete
        repository.deleteUserById("uid_001")
        assertNull(fakeDao.getUserById("uid_001"))
    }
}
