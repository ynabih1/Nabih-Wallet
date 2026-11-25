package com.nabih.wallet.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.nabih.wallet.data.local.dao.CategoryDao
import com.nabih.wallet.data.local.dao.DebtDao
import com.nabih.wallet.data.local.dao.DebtPaymentDao
import com.nabih.wallet.data.local.dao.TransactionDao
import com.nabih.wallet.data.local.entity.CategoryEntity
import com.nabih.wallet.data.local.entity.DebtEntity
import com.nabih.wallet.data.local.entity.DebtPaymentEntity
import com.nabih.wallet.data.local.entity.TransactionEntity
import com.nabih.wallet.data.local.entity.TransactionType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun debtDao(): DebtDao
    abstract fun debtPaymentDao(): DebtPaymentDao

    companion object {
        private const val DATABASE_NAME = "nabih_wallet.db"
        private val seedMutex = Mutex()

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .build()
        }

        /**
         * Single synchronized entry point for seeding default categories AND cleaning up
         * any previously duplicated categories safely inside a Room transaction.
         */
        suspend fun seedAndDeduplicateCategories(database: AppDatabase) {
            seedMutex.withLock {
                database.withTransaction {
                    val categoryDao = database.categoryDao()
                    val transactionDao = database.transactionDao()
                    val existing = categoryDao.getAll()
                    if (existing.isEmpty()) {
                        categoryDao.insertAll(defaultCategories())
                        return@withTransaction
                    }

                    // Deduplicate any categories sharing the same (type, normalized name)
                    val grouped = existing.groupBy { it.type to it.name.trim() }
                    grouped.values.forEach { duplicates ->
                        if (duplicates.size > 1) {
                            val sorted = duplicates.sortedBy { it.id }
                            val keeper = sorted.first()
                            sorted.drop(1).forEach { duplicate ->
                                transactionDao.reassignCategory(
                                    oldCategoryId = duplicate.id,
                                    newCategoryId = keeper.id
                                )
                                categoryDao.delete(duplicate)
                            }
                        }
                    }
                }
            }
        }

        fun defaultCategories(): List<CategoryEntity> = listOf(
            // Expense Categories (16)
            CategoryEntity(name = "طعام", type = TransactionType.EXPENSE, iconKey = "restaurant", colorKey = "amber", isDefault = true, sortOrder = 1),
            CategoryEntity(name = "بقالة", type = TransactionType.EXPENSE, iconKey = "shopping_cart", colorKey = "orange", isDefault = true, sortOrder = 2),
            CategoryEntity(name = "مواصلات", type = TransactionType.EXPENSE, iconKey = "directions_car", colorKey = "teal", isDefault = true, sortOrder = 3),
            CategoryEntity(name = "فواتير", type = TransactionType.EXPENSE, iconKey = "receipt_long", colorKey = "indigo", isDefault = true, sortOrder = 4),
            CategoryEntity(name = "سكن", type = TransactionType.EXPENSE, iconKey = "home", colorKey = "brown", isDefault = true, sortOrder = 5),
            CategoryEntity(name = "شحن رصيد", type = TransactionType.EXPENSE, iconKey = "smartphone", colorKey = "blue", isDefault = true, sortOrder = 6),
            CategoryEntity(name = "تعليم", type = TransactionType.EXPENSE, iconKey = "school", colorKey = "purple", isDefault = true, sortOrder = 7),
            CategoryEntity(name = "صحة", type = TransactionType.EXPENSE, iconKey = "medical_services", colorKey = "emerald", isDefault = true, sortOrder = 8),
            CategoryEntity(name = "تسوق", type = TransactionType.EXPENSE, iconKey = "shopping_bag", colorKey = "pink", isDefault = true, sortOrder = 9),
            CategoryEntity(name = "اشتراكات", type = TransactionType.EXPENSE, iconKey = "subscriptions", colorKey = "violet", isDefault = true, sortOrder = 10),
            CategoryEntity(name = "ترفيه", type = TransactionType.EXPENSE, iconKey = "sports_esports", colorKey = "cyan", isDefault = true, sortOrder = 11),
            CategoryEntity(name = "أسرة", type = TransactionType.EXPENSE, iconKey = "family_restroom", colorKey = "rose", isDefault = true, sortOrder = 12),
            CategoryEntity(name = "عناية", type = TransactionType.EXPENSE, iconKey = "spa", colorKey = "lime", isDefault = true, sortOrder = 13),
            CategoryEntity(name = "هدايا", type = TransactionType.EXPENSE, iconKey = "card_giftcard", colorKey = "red", isDefault = true, sortOrder = 14),
            CategoryEntity(name = "صيانة", type = TransactionType.EXPENSE, iconKey = "build", colorKey = "slate", isDefault = true, sortOrder = 15),
            CategoryEntity(name = "أخرى", type = TransactionType.EXPENSE, iconKey = "more_horiz", colorKey = "gray", isDefault = true, sortOrder = 16),

            // Income Categories (7)
            CategoryEntity(name = "راتب", type = TransactionType.INCOME, iconKey = "payments", colorKey = "emerald", isDefault = true, sortOrder = 101),
            CategoryEntity(name = "عمل حر", type = TransactionType.INCOME, iconKey = "laptop_mac", colorKey = "indigo", isDefault = true, sortOrder = 102),
            CategoryEntity(name = "استثمار", type = TransactionType.INCOME, iconKey = "trending_up", colorKey = "teal", isDefault = true, sortOrder = 103),
            CategoryEntity(name = "إيجار", type = TransactionType.INCOME, iconKey = "apartment", colorKey = "blue", isDefault = true, sortOrder = 104),
            CategoryEntity(name = "مكافأة", type = TransactionType.INCOME, iconKey = "emoji_events", colorKey = "amber", isDefault = true, sortOrder = 105),
            CategoryEntity(name = "دعم", type = TransactionType.INCOME, iconKey = "volunteer_activism", colorKey = "pink", isDefault = true, sortOrder = 106),
            CategoryEntity(name = "أخرى", type = TransactionType.INCOME, iconKey = "account_balance_wallet", colorKey = "slate", isDefault = true, sortOrder = 107)
        )
    }
}
