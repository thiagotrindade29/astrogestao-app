package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        ProductAliasEntity::class,
        StockMovementEntity::class,
        MarketCheckoutEntity::class,
        HouseholdUserEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun productAliasDao(): ProductAliasDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun marketCheckoutDao(): MarketCheckoutDao
    abstract fun householdUserDao(): HouseholdUserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "astrogestao_erp.db"
                )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
