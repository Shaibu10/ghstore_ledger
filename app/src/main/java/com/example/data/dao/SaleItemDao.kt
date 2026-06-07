package com.example.data.dao

import androidx.room.*
import com.example.data.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for purchase line items in Gh POS.
 */
@Dao
interface SaleItemDao {
    @Query("SELECT * FROM sale_items")
    fun getAllSaleItems(): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getSaleItemsBySaleId(saleId: String): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItemsBySaleIdSync(saleId: String): List<SaleItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItem(saleItem: SaleItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(saleItems: List<SaleItemEntity>)

    @Update
    suspend fun updateSaleItem(saleItem: SaleItemEntity)

    @Delete
    suspend fun deleteSaleItem(saleItem: SaleItemEntity)

    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    suspend fun deleteSaleItemsBySaleId(saleId: String)

    @Query("SELECT * FROM sale_items WHERE lastSyncedAt = 0")
    suspend fun getUnsyncedSaleItems(): List<SaleItemEntity>
}
