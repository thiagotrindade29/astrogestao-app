package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryEntity
import com.example.data.local.HouseholdUserEntity
import com.example.data.local.MovementType
import com.example.data.local.ProductEntity
import com.example.data.repository.AstroRepository
import com.example.data.repository.AuthOutcome
import com.example.data.repository.PosCheckoutEntry
import com.example.data.repository.VersionComparator
import com.example.notifications.LowStockReminderPolicy
import com.example.ui.viewmodel.AstroUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AstroRepositoryRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AstroRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AstroRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `defaults seed categories only without personal hardcoded user`() = runTest {
        repository.ensureDefaultsSeeded()
        val categories = repository.categories.first()
        val users = repository.users.first()

        assertTrue(categories.isNotEmpty())
        assertTrue(categories.any { it.name == "Consumo Único" && it.singleConsumption })
        assertEquals(0, users.size)
    }

    @Test
    fun `authentication hashes passwords securely and rejects empty hash bypass`() = runTest {
        val regResult = repository.registerUser(
            name = "Maria Silva",
            email = "Maria@Casa.com",
            password = "segredo123",
            role = "Administrador"
        )
        assertTrue(regResult.isSuccess)
        val user = regResult.getOrThrow()
        assertTrue(user.passwordHash.startsWith("pbkdf2$"))

        val duplicate = repository.registerUser("Outra", "maria@casa.com", "outra123", "Morador")
        assertTrue(duplicate.isFailure)

        assertTrue(repository.authenticateUser("maria@casa.com", "segredo123") is AuthOutcome.Success)
        assertEquals(AuthOutcome.WrongPassword, repository.authenticateUser("maria@casa.com", "errada123"))
        assertEquals(AuthOutcome.UserNotFound, repository.authenticateUser("naoexiste@casa.com", "123456"))

        repository.saveUser(HouseholdUserEntity(name = "Visitante", email = "visitante@casa.com", passwordHash = "", role = "Morador"))
        assertEquals(
            AuthOutcome.NoPasswordConfigured,
            repository.authenticateUser("visitante@casa.com", "123456")
        )
    }

    @Test
    fun `movements recalculate balance and prevent negative stock`() = runTest {
        val catId = repository.saveCategory(CategoryEntity(name = "Despensa", colorHex = 0xFF5B3DF5))
        val prodId = repository.saveProduct(
            ProductEntity(
                name = "Café 500g",
                categoryId = catId,
                unit = "un",
                quantity = 2.0,
                minStock = 1.0,
                unitPrice = 18.0
            ),
            authorName = "Maria"
        )

        repository.registerMovement(prodId, MovementType.ENTRADA, 3.0, 20.0, "Compra semanal", "Maria")
        repository.registerMovement(prodId, MovementType.SAIDA, 10.0, null, "Consumo excessivo", "Maria")

        val updated = repository.products.first().first { it.id == prodId }
        assertEquals("AST-0001", updated.uniqueCode)
        assertEquals(0.0, updated.quantity, 0.001)
        assertEquals(20.0, updated.unitPrice, 0.001)

        val movements = repository.movements.first()
        assertEquals(3, movements.size)
    }

    @Test
    fun `pos checkout deduplicates by name across market codes and backup roundtrips cleanly`() = runTest {
        val catId = repository.saveCategory(CategoryEntity(name = "Geladeira", colorHex = 0xFF0284C7))
        repository.saveProduct(
            ProductEntity(
                code = "7890001112223",
                name = "Leite Integral 1L",
                categoryId = catId,
                unit = "un",
                quantity = 2.0,
                unitPrice = 4.50
            ),
            authorName = "Maria"
        )

        repository.finalizeMarketPosCheckout(
            storeName = "Supermercado Sul",
            notes = "Compra do mês",
            entries = listOf(
                PosCheckoutEntry(
                    canonicalProductId = null,
                    scannedCode = "COD-INTERNO-SUL-99",
                    name = "  LEITE INTEGRAL 1L ",
                    unit = "un",
                    quantity = 4.0,
                    unitPrice = 4.99,
                    categoryId = catId
                ),
                PosCheckoutEntry(
                    canonicalProductId = null,
                    scannedCode = "7895556667778",
                    name = "Manteiga 200g",
                    unit = "un",
                    quantity = 1.0,
                    unitPrice = 9.90,
                    categoryId = catId
                )
            ),
            saveToStock = true,
            authorName = "Maria"
        )

        val products = repository.products.first()
        assertEquals(2, products.size)
        val leite = products.first { it.name.contains("Leite", ignoreCase = true) }
        assertEquals(6.0, leite.quantity, 0.001)
        assertEquals(4.99, leite.unitPrice, 0.001)
        assertEquals(4.50, leite.lastPaidPrice ?: 0.0, 0.001)

        val aliases = repository.aliases.first()
        assertTrue(aliases.any { it.productId == leite.id && it.externalCode == "COD-INTERNO-SUL-99" })

        val backupJson = repository.exportBackupJson()
        db.categoryDao().deleteAll()
        db.productDao().deleteAll()
        assertEquals(0, repository.products.first().size)

        val restoreResult = repository.importBackupJson(backupJson)
        assertTrue(restoreResult.isSuccess)
        val summary = restoreResult.getOrThrow()
        assertEquals(1, summary.categoriesRestored)
        assertEquals(2, summary.productsRestored)
        assertEquals(2, repository.products.first().size)
        assertEquals(6.0, repository.products.first().first { it.uniqueCode == leite.uniqueCode }.quantity, 0.001)
    }

    @Test
    fun `single consumption category suppresses low stock alerts and preserves backup flag`() = runTest {
        val singleCatId = repository.saveCategory(
            CategoryEntity(
                name = "Consumo Único",
                colorHex = 0xFFDB2777,
                sectorNote = "Lanches imediatos",
                singleConsumption = true
            )
        )
        val regularCatId = repository.saveCategory(
            CategoryEntity(
                name = "Despensa",
                colorHex = 0xFF5B3DF5,
                singleConsumption = false
            )
        )

        val coxinhaId = repository.saveProduct(
            ProductEntity(
                name = "Coxinha 60gr",
                categoryId = singleCatId,
                unit = "un",
                quantity = 0.0,
                minStock = 1.0,
                unitPrice = 4.59
            ),
            authorName = "Maria"
        )
        val arrozId = repository.saveProduct(
            ProductEntity(
                name = "Arroz 5kg",
                categoryId = regularCatId,
                unit = "pct",
                quantity = 0.0,
                minStock = 1.0,
                unitPrice = 25.90
            ),
            authorName = "Maria"
        )

        val categories = repository.categories.first()
        val products = repository.products.first()
        val state = AstroUiState(
            isLoading = false,
            categories = categories,
            products = products,
            onlyLowStock = true
        )

        val coxinha = products.first { it.id == coxinhaId }
        val arroz = products.first { it.id == arrozId }

        assertFalse(state.isProductLowStock(coxinha))
        assertTrue(state.isProductLowStock(arroz))
        assertEquals(1, state.restockAlertsCount)
        assertEquals(listOf(arrozId), state.lowStockProducts.map { it.id })
        assertEquals(listOf(arrozId), state.filteredProducts.map { it.id })

        val backupJson = repository.exportBackupJson()
        db.categoryDao().deleteAll()
        db.productDao().deleteAll()
        assertTrue(repository.importBackupJson(backupJson).isSuccess)

        val restoredSingle = repository.categories.first().first { it.name == "Consumo Único" }
        assertTrue(restoredSingle.singleConsumption)
    }

    @Test
    fun `version comparator handles semantic version updates accurately`() {
        assertTrue(VersionComparator.isUpdateAvailable("1.1.0", "1.2.0"))
        assertTrue(VersionComparator.isUpdateAvailable("1.1.0", "1.10.0"))
        assertTrue(VersionComparator.isUpdateAvailable("1.1.0", "v2.0.0"))
        assertFalse(VersionComparator.isUpdateAvailable("1.1.0", "1.1.0"))
        assertFalse(VersionComparator.isUpdateAvailable("1.2.0", "1.1.9"))
        assertFalse(VersionComparator.isUpdateAvailable("1.1.0", ""))
    }

    @Test
    fun `low stock reminder policy notifies once per item set and respects cooldown`() {
        val arroz = ProductEntity(id = 10L, name = "Arroz 5kg", quantity = 0.0, minStock = 1.0)
        val cafe = ProductEntity(id = 20L, name = "Café 500g", quantity = 1.0, minStock = 2.0)
        val t0 = 1_700_000_000_000L

        assertFalse(
            LowStockReminderPolicy.shouldSendReminder(
                lowStockProducts = emptyList(),
                notificationsEnabled = true,
                lastNotifiedSignature = "",
                lastNotifiedAtMillis = 0L,
                nowMillis = t0
            )
        )
        assertFalse(
            LowStockReminderPolicy.shouldSendReminder(
                lowStockProducts = listOf(arroz),
                notificationsEnabled = false,
                lastNotifiedSignature = "",
                lastNotifiedAtMillis = 0L,
                nowMillis = t0
            )
        )
        assertTrue(
            LowStockReminderPolicy.shouldSendReminder(
                lowStockProducts = listOf(arroz),
                notificationsEnabled = true,
                lastNotifiedSignature = "",
                lastNotifiedAtMillis = 0L,
                nowMillis = t0
            )
        )
        val sig1 = LowStockReminderPolicy.buildSignature(listOf(arroz))
        assertFalse(
            LowStockReminderPolicy.shouldSendReminder(
                lowStockProducts = listOf(arroz),
                notificationsEnabled = true,
                lastNotifiedSignature = sig1,
                lastNotifiedAtMillis = t0,
                nowMillis = t0 + 60_000L
            )
        )
        // New item enters low stock -> remind even before 12h cooldown
        assertTrue(
            LowStockReminderPolicy.shouldSendReminder(
                lowStockProducts = listOf(arroz, cafe),
                notificationsEnabled = true,
                lastNotifiedSignature = sig1,
                lastNotifiedAtMillis = t0,
                nowMillis = t0 + 60_000L
            )
        )
        // After 12h cooldown -> gentle reminder for still-unrestocked items
        assertTrue(
            LowStockReminderPolicy.shouldSendReminder(
                lowStockProducts = listOf(arroz),
                notificationsEnabled = true,
                lastNotifiedSignature = sig1,
                lastNotifiedAtMillis = t0,
                nowMillis = t0 + LowStockReminderPolicy.REMINDER_COOLDOWN_MS + 1_000L
            )
        )
    }
}
