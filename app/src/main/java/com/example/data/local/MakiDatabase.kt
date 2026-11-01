package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AppSettingsDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.CelebrationDao
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.InventoryMovementDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.PreviousGiftDao
import com.example.data.local.dao.SaleDao
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CelebrationEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.InventoryMovementEntity
import com.example.data.local.entity.PreviousGiftEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.room.migration.Migration

@Database(
    entities = [
        ProductEntity::class,
        CategoryEntity::class,
        SaleEntity::class,
        InventoryMovementEntity::class,
        CustomerEntity::class,
        CelebrationEntity::class,
        PreviousGiftEntity::class,
        ExpenseEntity::class,
        AppSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MakiDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun saleDao(): SaleDao
    abstract fun inventoryMovementDao(): InventoryMovementDao
    abstract fun customerDao(): CustomerDao
    abstract fun celebrationDao(): CelebrationDao
    abstract fun previousGiftDao(): PreviousGiftDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: MakiDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN photoUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE customers ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_isArchived ON customers(isArchived)")
            }
        }

        fun getDatabase(context: Context): MakiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MakiDatabase::class.java,
                    "maki_brands.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(false)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate initial default categories & settings
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    val defaultCategories = listOf(
                                        CategoryEntity(name = "Custom Gifts", iconName = "gift"),
                                        CategoryEntity(name = "Chocolates & Sweets", iconName = "cake"),
                                        CategoryEntity(name = "Flowers & Bouquets", iconName = "local_florist"),
                                        CategoryEntity(name = "Perfumes & Scents", iconName = "spa"),
                                        CategoryEntity(name = "Greeting Cards", iconName = "card"),
                                        CategoryEntity(name = "Gift Boxes & Hampers", iconName = "inventory_2"),
                                        CategoryEntity(name = "Jewelry & Keepsakes", iconName = "diamond")
                                    )
                                    database.categoryDao().insertAll(defaultCategories)
                                    database.appSettingsDao().setSetting(
                                        AppSettingsEntity(key = "app_language", value = "en")
                                    )
                                    database.appSettingsDao().setSetting(
                                        AppSettingsEntity(key = "currency", value = "ETB")
                                    )
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
