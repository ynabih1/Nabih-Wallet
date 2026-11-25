package com.nabih.wallet

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nabih.wallet.data.backup.BackupManager
import com.nabih.wallet.data.backup.BackupRestoreResult
import com.nabih.wallet.data.local.database.AppDatabase
import com.nabih.wallet.data.local.entity.DebtType
import com.nabih.wallet.data.local.entity.TransactionType
import com.nabih.wallet.data.model.CategoryExpenseSummary
import com.nabih.wallet.data.repository.WalletRepository
import com.nabih.wallet.pdf.PdfExpenseReportData
import com.nabih.wallet.pdf.PdfReportGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupAndPdfTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: WalletRepository
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WalletRepository(database)
        backupManager = BackupManager(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `json backup exports and atomically restores all entities intact`() = runBlocking {
        repository.ensureDefaultCategories()
        val categories = repository.categories.first()
        val foodCat = categories.first { it.name == "طعام" }
        val salaryCat = categories.first { it.name == "راتب" }

        repository.addTransaction(TransactionType.INCOME, 15000.0, salaryCat.id, 1_760_000_000_000L, "راتب نوفمبر")
        repository.addTransaction(TransactionType.EXPENSE, 650.0, foodCat.id, 1_760_100_000_000L, "غداء")
        val debtId = repository.addDebt("سارة أحمد", DebtType.OWED_TO_ME, 1200.0, 1_760_200_000_000L)
        repository.addDebtPayment(debtId, 400.0, 1_760_300_000_000L)

        val backupJson = backupManager.createBackupJson()
        assertTrue(backupJson.contains("\"schemaVersion\""))
        assertTrue(backupJson.contains("سارة أحمد"))

        // Mutate DB completely before restoring
        repository.addTransaction(TransactionType.EXPENSE, 9999.0, foodCat.id, 1_760_400_000_000L, "مؤقت")
        assertEquals(3, repository.transactions.first().size)

        // Restore atomically
        val restoreResult = backupManager.restoreFromJson(backupJson)
        assertTrue(restoreResult is BackupRestoreResult.Success)
        val success = restoreResult as BackupRestoreResult.Success
        assertEquals(2, success.transactionsCount)
        assertEquals(1, success.debtsCount)
        assertEquals(1, success.paymentsCount)

        // Verify restored DB contents
        val restoredTxs = repository.transactions.first()
        assertEquals(2, restoredTxs.size)
        val restoredDebts = repository.debtsWithPayments.first()
        assertEquals(1, restoredDebts.size)
        assertEquals("سارة أحمد", restoredDebts.first().debt.personName)
        assertEquals(800.0, restoredDebts.first().remainingAmount, 0.001)
    }

    @Test
    fun `corrupt json backup fails validation and leaves existing database untouched`() = runBlocking {
        repository.ensureDefaultCategories()
        val cat = repository.categories.first().first()
        repository.addTransaction(TransactionType.EXPENSE, 250.0, cat.id, 1_760_000_000_000L)

        val badResult = backupManager.restoreFromJson("""{"schemaVersion": 99, "categories": []}""")
        assertTrue(badResult is BackupRestoreResult.Error)

        // Original transaction must remain untouched
        assertEquals(1, repository.transactions.first().size)
        assertEquals(250.0, repository.transactions.first().first().amount, 0.001)
    }

    @Test
    fun `pdf report generator renders arabic rtl pages with logo table and pagination`() {
        val categories = (1..20).map { idx ->
            CategoryExpenseSummary(
                categoryId = idx.toLong(),
                categoryName = "فئة مصروف $idx",
                iconKey = "restaurant",
                colorKey = "amber",
                totalAmount = idx * 100.0,
                percentage = 5.0,
                transactionCount = 2
            )
        }
        val report = PdfExpenseReportData(
            periodLabel = "نوفمبر 2026",
            exportedAtMillis = System.currentTimeMillis(),
            totalExpense = categories.sumOf { it.totalAmount },
            categories = categories
        )

        val renderedPages = mutableListOf<android.graphics.Bitmap>()
        val pageCount = PdfReportGenerator.renderPages(context, report) { _, width, height, draw ->
            val bitmap = android.graphics.Bitmap.createBitmap(
                width,
                height,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            draw(canvas)
            renderedPages.add(bitmap)
        }

        assertEquals(2, pageCount)
        assertEquals(2, renderedPages.size)
        renderedPages.forEach { bmp ->
            assertEquals(595, bmp.width)
            assertEquals(842, bmp.height)
            bmp.recycle()
        }

        // Also verify empty report produces 1 clean page without error
        val emptyReport = PdfExpenseReportData(
            periodLabel = "كل الفترات",
            exportedAtMillis = System.currentTimeMillis(),
            totalExpense = 0.0,
            categories = emptyList()
        )
        val emptyPages = PdfReportGenerator.renderPages(context, emptyReport) { _, width, height, draw ->
            val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            draw(android.graphics.Canvas(bmp))
            bmp.recycle()
        }
        assertEquals(1, emptyPages)
    }
}
