package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products")
    suspend fun getAllOnce(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}

@Dao
interface ProductAliasDao {
    @Query("SELECT * FROM product_aliases ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ProductAliasEntity>>

    @Query("SELECT * FROM product_aliases")
    suspend fun getAllOnce(): List<ProductAliasEntity>

    @Query("SELECT * FROM product_aliases WHERE LOWER(externalCode) = LOWER(:code) LIMIT 1")
    suspend fun findByCode(code: String): ProductAliasEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alias: ProductAliasEntity): Long

    @Query("DELETE FROM product_aliases")
    suspend fun deleteAll()
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY timestampMillis DESC, id DESC")
    fun observeAll(): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: StockMovementEntity): Long

    @Query("DELETE FROM stock_movements")
    suspend fun deleteAll()
}

@Dao
interface MarketCheckoutDao {
    @Query("SELECT * FROM market_checkouts ORDER BY timestampMillis DESC, id DESC")
    fun observeAll(): Flow<List<MarketCheckoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checkout: MarketCheckoutEntity): Long

    @Query("DELETE FROM market_checkouts")
    suspend fun deleteAll()
}

@Dao
interface HouseholdUserDao {
    @Query("SELECT * FROM household_users ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<HouseholdUserEntity>>

    @Query("SELECT * FROM household_users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getByEmail(email: String): HouseholdUserEntity?

    @Query("SELECT COUNT(*) FROM household_users")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: HouseholdUserEntity): Long

    @Update
    suspend fun update(user: HouseholdUserEntity)

    @Query("DELETE FROM household_users WHERE id = :id")
    suspend fun deleteById(id: Long)
}
