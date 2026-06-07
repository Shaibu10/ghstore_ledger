package com.example.data.dao

import androidx.room.*
import com.example.data.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local Product operations in Gh POS.
 */
@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getProductsByCategory(categoryId: String): Flow<List<ProductEntity>>

    // Fetch products with quantities strictly less than or equal to their reorderThreshold
    @Query("SELECT * FROM products WHERE quantity <= reorderLevel ORDER BY name ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    // Case-insensitive name search
    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    @Query("SELECT * FROM products WHERE updatedAt > lastSyncedAt")
    suspend fun getUnsyncedProducts(): List<ProductEntity>
}
