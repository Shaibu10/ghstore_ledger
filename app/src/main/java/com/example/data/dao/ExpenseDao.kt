package com.example.data.dao

import androidx.room.*
import com.example.data.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for recorded operating Expense overheads.
 */
@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE date >= :startTimestamp AND date <= :endTimestamp ORDER BY date DESC")
    fun getExpensesInRange(startTimestamp: Long, endTimestamp: Long): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)

    @Query("SELECT * FROM expenses WHERE updatedAt > lastSyncedAt")
    suspend fun getUnsyncedExpenses(): List<ExpenseEntity>
}
