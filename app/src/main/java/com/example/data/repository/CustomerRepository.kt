package com.example.data.repository

import com.example.data.dao.CustomerDao
import com.example.data.entity.CustomerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository coordinating Customer relationship records in Gh POS.
 */
class CustomerRepository(private val customerDao: CustomerDao) {

    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    suspend fun saveCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        customerDao.insertCustomer(customer)
    }

    suspend fun saveCustomers(customers: List<CustomerEntity>) = withContext(Dispatchers.IO) {
        customerDao.insertCustomers(customers)
    }

    suspend fun getCustomerById(id: String): CustomerEntity? = withContext(Dispatchers.IO) {
        customerDao.getCustomerById(id)
    }

    fun searchCustomers(query: String): Flow<List<CustomerEntity>> {
        return customerDao.searchCustomers(query)
    }

    suspend fun deleteCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        customerDao.deleteCustomer(customer)
    }

    suspend fun deleteCustomerById(id: String) = withContext(Dispatchers.IO) {
        customerDao.deleteCustomerById(id)
    }

    suspend fun getUnsyncedCustomers(): List<CustomerEntity> = withContext(Dispatchers.IO) {
        customerDao.getUnsyncedCustomers()
    }
}
