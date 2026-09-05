package com.example.myexpenditureapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myexpenditureapp.data.dao.AccountDao
import com.example.myexpenditureapp.data.dao.BudgetDao
import com.example.myexpenditureapp.data.dao.CategoryDao
import com.example.myexpenditureapp.data.dao.TransactionDao
import com.example.myexpenditureapp.data.entity.Account
import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Account::class, Category::class, Transaction::class, Budget::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expenditure_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                
                // Seeding categories on a background thread
                CoroutineScope(Dispatchers.IO).launch {
                    DataSeeder.seedData(instance)
                }
                
                instance
            }
        }
    }
}
