package com.example.data.repository

import com.example.data.dao.CategoryDao
import com.example.data.entity.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository coordinating Category data flow between local db and online sources.
 */
class CategoryRepository(private val categoryDao: CategoryDao) {

    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun saveCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(category)
    }

    suspend fun saveCategories(categories: List<CategoryEntity>) = withContext(Dispatchers.IO) {
        categoryDao.insertCategories(categories)
    }

    suspend fun getCategoryById(id: String): CategoryEntity? = withContext(Dispatchers.IO) {
        categoryDao.getCategoryById(id)
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategory(category)
    }

    suspend fun deleteCategoryById(id: String) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategoryById(id)
    }

    suspend fun getUnsyncedCategories(): List<CategoryEntity> = withContext(Dispatchers.IO) {
        categoryDao.getUnsyncedCategories()
    }
}
