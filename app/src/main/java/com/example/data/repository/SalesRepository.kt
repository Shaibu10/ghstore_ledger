package com.example.data.repository

import com.example.data.dao.ProductDao
import com.example.data.dao.SaleDao
import com.example.data.dao.SaleItemDao
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Cohesive Repository coordinating checkout processes, receipts, items,
 * and automatic real-time product stock deductions in Gh POS.
 */
class SalesRepository(
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val productDao: ProductDao
) {

    val allSales: Flow<List<SaleEntity>> = saleDao.getAllSales()
    val allSaleItems: Flow<List<SaleItemEntity>> = saleItemDao.getAllSaleItems()

    suspend fun getSaleById(id: String): SaleEntity? = withContext(Dispatchers.IO) {
        saleDao.getSaleById(id)
    }

    suspend fun getSaleByInvoiceNumber(invoiceNumber: String): SaleEntity? = withContext(Dispatchers.IO) {
        saleDao.getSaleByInvoiceNumber(invoiceNumber)
    }

    fun getSaleItemsBySaleId(saleId: String): Flow<List<SaleItemEntity>> {
        return saleItemDao.getSaleItemsBySaleId(saleId)
    }

    suspend fun getSaleItemsBySaleIdSync(saleId: String): List<SaleItemEntity> = withContext(Dispatchers.IO) {
        saleItemDao.getSaleItemsBySaleIdSync(saleId)
    }

    fun getSalesByCashier(cashierId: String): Flow<List<SaleEntity>> {
        return saleDao.getSalesByCashier(cashierId)
    }

    fun getSalesByCustomer(customerId: String): Flow<List<SaleEntity>> {
        return saleDao.getSalesByCustomer(customerId)
    }

    /**
     * Executes atomic business logic of checking out an order.
     * Inserts the master SaleEntity, inserts all detailed SaleItemEntities,
     * and automatically deducts the quantity purchased from the associated products in stock.
     */
    suspend fun checkout(sale: SaleEntity, items: List<SaleItemEntity>): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Insert master invoice record
            saleDao.insertSale(sale)

            // 2. Insert item line lists
            saleItemDao.insertSaleItems(items)

            // 3. Subtract stock quantities for each checked-out item to prevent discrepancy
            items.forEach { item ->
                val product = productDao.getProductById(item.productId)
                if (product != null) {
                    val newQty = (product.quantity - item.quantity).coerceAtLeast(0.0)
                    productDao.insertProduct(product.copy(
                        quantity = newQty,
                        updatedAt = System.currentTimeMillis()
                    ))
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteSale(sale: SaleEntity) = withContext(Dispatchers.IO) {
        saleDao.deleteSale(sale)
        saleItemDao.deleteSaleItemsBySaleId(sale.id)
    }

    suspend fun getUnsyncedSales(): List<SaleEntity> = withContext(Dispatchers.IO) {
        saleDao.getUnsyncedSales()
    }

    suspend fun getUnsyncedSaleItems(): List<SaleItemEntity> = withContext(Dispatchers.IO) {
        saleItemDao.getUnsyncedSaleItems()
    }
}
