package com.example.data.repository

import com.example.data.dao.ProductDao
import com.example.data.entity.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository coordinating Product item records and catalog management in Gh POS.
 */
class ProductRepository(private val productDao: ProductDao) {

    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = productDao.getLowStockProducts()

    suspend fun saveProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
    }

    suspend fun saveProducts(products: List<ProductEntity>) = withContext(Dispatchers.IO) {
        productDao.insertProducts(products)
    }

    suspend fun getProductById(id: String): ProductEntity? = withContext(Dispatchers.IO) {
        productDao.getProductById(id)
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? = withContext(Dispatchers.IO) {
        productDao.getProductByBarcode(barcode)
    }

    fun getProductsByCategory(categoryId: String): Flow<List<ProductEntity>> {
        return productDao.getProductsByCategory(categoryId)
    }

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return productDao.searchProducts(query)
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    suspend fun deleteProductById(id: String) = withContext(Dispatchers.IO) {
        productDao.deleteProductById(id)
    }

    suspend fun getUnsyncedProducts(): List<ProductEntity> = withContext(Dispatchers.IO) {
        productDao.getUnsyncedProducts()
    }
}
