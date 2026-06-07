package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// --- ENTITIES ---

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val phone: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int?, // Nullable for Walk-in
    val customerName: String, // Denormalized for historic speed
    val amount: Double,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isCredit: Boolean = false,
    val creditPaid: Boolean = false
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val category: String, // e.g. "Rent", "Utilities", "Salary", "Inventory", "Marketing", "Other"
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val customerName: String,
    val amount: Double,
    val interestRate: Double = 0.0, // interest rate in percent, e.g. 5.0 is 5%
    val dueDate: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isRepaid: Boolean = false,
    val repaidAmount: Double = 0.0
)


// --- DAOs ---

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Int)

    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Query("UPDATE sales SET creditPaid = :paid WHERE id = :id")
    suspend fun updateCreditPaymentStatus(id: Int, paid: Boolean)

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteSaleById(id: Int)

    @Query("DELETE FROM sales")
    suspend fun clearAllSales()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Int)

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY timestamp DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan): Long

    @Query("UPDATE loans SET repaidAmount = :repaidAmount, isRepaid = :isRepaid WHERE id = :id")
    suspend fun updateRepayment(id: Int, repaidAmount: Double, isRepaid: Boolean)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoanById(id: Int)

    @Query("DELETE FROM loans")
    suspend fun clearAllLoans()
}


// --- DATABASE ---

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // e.g., "Electronics", "Groceries", "Clothing", "Other"
    val sku: String, // Stock keeping unit / code
    val price: Double, // Selling price
    val costPrice: Double, // Purchasing/Production cost
    val stockQuantity: Int, // Number remaining in inventory
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Int)

    @Query("UPDATE products SET stockQuantity = stockQuantity - :quantity WHERE id = :id")
    suspend fun reduceStock(id: Int, quantity: Int)

    @Query("UPDATE products SET stockQuantity = :quantity WHERE id = :id")
    suspend fun updateStock(id: Int, quantity: Int)

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()
}

@Database(entities = [Customer::class, Sale::class, Expense::class, Product::class, Loan::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun productDao(): ProductDao
    abstract fun loanDao(): LoanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "store_ledger_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


// --- REPOSITORIES ---

class CustomerRepository(private val customerDao: CustomerDao) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    suspend fun insert(customer: Customer) = customerDao.insertCustomer(customer)
    suspend fun delete(id: Int) = customerDao.deleteCustomerById(id)
    suspend fun clear() = customerDao.clearAllCustomers()
}

class SaleRepository(private val saleDao: SaleDao) {
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()

    suspend fun insert(sale: Sale) = saleDao.insertSale(sale)
    suspend fun updateCreditPaymentStatus(id: Int, paid: Boolean) = saleDao.updateCreditPaymentStatus(id, paid)
    suspend fun delete(id: Int) = saleDao.deleteSaleById(id)
    suspend fun clear() = saleDao.clearAllSales()
}

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun insert(expense: Expense) = expenseDao.insertExpense(expense)
    suspend fun delete(id: Int) = expenseDao.deleteExpenseById(id)
    suspend fun clear() = expenseDao.clearAllExpenses()
}

class ProductRepository(private val productDao: ProductDao) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    suspend fun insert(product: Product) = productDao.insertProduct(product)
    suspend fun delete(id: Int) = productDao.deleteProductById(id)
    suspend fun reduceStock(id: Int, quantity: Int) = productDao.reduceStock(id, quantity)
    suspend fun updateStock(id: Int, quantity: Int) = productDao.updateStock(id, quantity)
    suspend fun clear() = productDao.clearAllProducts()
}

class LoanRepository(private val loanDao: LoanDao) {
    val allLoans: Flow<List<Loan>> = loanDao.getAllLoans()

    suspend fun insert(loan: Loan) = loanDao.insertLoan(loan)
    suspend fun updateRepayment(id: Int, repaidAmount: Double, isRepaid: Boolean) = loanDao.updateRepayment(id, repaidAmount, isRepaid)
    suspend fun delete(id: Int) = loanDao.deleteLoanById(id)
    suspend fun clear() = loanDao.clearAllLoans()
}
