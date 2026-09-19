package com.example.myexpenditureapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myexpenditureapp.data.dao.*
import com.example.myexpenditureapp.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `subscriptions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `amount` TEXT NOT NULL,
                `billingCycle` TEXT NOT NULL,
                `dueDayOfMonth` INTEGER NOT NULL,
                `categoryId` INTEGER,
                `accountId` INTEGER,
                `autoDetectEnabled` INTEGER NOT NULL,
                `lastPaidDate` INTEGER,
                `notes` TEXT,
                `isActive` INTEGER NOT NULL,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_categoryId` ON `subscriptions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_accountId` ON `subscriptions` (`accountId`)")
    }
}

@Database(
    entities = [
        Account::class,
        Category::class,
        Transaction::class,
        Budget::class,
        AutoCategoryRule::class,
        SavingGoal::class,
        Subscription::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun autoCategoryRuleDao(): AutoCategoryRuleDao
    abstract fun savingGoalDao(): SavingGoalDao
    abstract fun subscriptionDao(): SubscriptionDao

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
                    .addMigrations(MIGRATION_7_8)
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance
                
                // Seeding default categories only if empty on a background thread
                CoroutineScope(Dispatchers.IO).launch {
                    DataSeeder.seedData(instance)
                }
                
                instance
            }
        }
    }
}
