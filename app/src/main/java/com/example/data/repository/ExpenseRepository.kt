package com.example.data.repository

import com.example.data.dao.ExpenseDao
import com.example.data.entity.ExpenseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository coordinating operating cost records for shop management audits.
 */
class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    suspend fun saveExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.insertExpense(expense)
    }

    suspend fun saveExpenses(expenses: List<ExpenseEntity>) = withContext(Dispatchers.IO) {
        expenseDao.insertExpenses(expenses)
    }

    suspend fun getExpenseById(id: String): ExpenseEntity? = withContext(Dispatchers.IO) {
        expenseDao.getExpenseById(id)
    }

    fun getExpensesInRange(startTimestamp: Long, endTimestamp: Long): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesInRange(startTimestamp, endTimestamp)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteExpenseById(id: String) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpenseById(id)
    }

    suspend fun getUnsyncedExpenses(): List<ExpenseEntity> = withContext(Dispatchers.IO) {
        expenseDao.getUnsyncedExpenses()
    }
}
