package com.nabih.wallet

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nabih.wallet.data.local.database.AppDatabase
import com.nabih.wallet.data.local.entity.DebtType
import com.nabih.wallet.data.local.entity.TransactionType
import com.nabih.wallet.data.model.FinancialCalculator
import com.nabih.wallet.data.repository.PaymentResult
import com.nabih.wallet.data.repository.WalletRepository
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DebtSeparationTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: WalletRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WalletRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `debts and debt payments are strictly isolated from wallet balance and expenses`() = runBlocking {
        repository.ensureDefaultCategories()
        val categories = repository.categories.first()
        val salaryCat = categories.first { it.type == TransactionType.INCOME }
        val foodCat = categories.first { it.type == TransactionType.EXPENSE }

        repository.addTransaction(TransactionType.INCOME, 10000.0, salaryCat.id, System.currentTimeMillis())
        repository.addTransaction(TransactionType.EXPENSE, 2000.0, foodCat.id, System.currentTimeMillis())

        val txBeforeDebts = repository.transactions.first()
        val balanceBefore = FinancialCalculator.calculateNetBalance(txBeforeDebts)
        val expenseBefore = FinancialCalculator.calculateTotalExpense(txBeforeDebts)
        assertEquals(8000.0, balanceBefore, 0.001)
        assertEquals(2000.0, expenseBefore, 0.001)

        // Add debts and record payments
        val owedByMeId = repository.addDebt("Omar Mostafa", DebtType.OWED_BY_ME, 2100.0)
        val owedToMeId = repository.addDebt("Karim Hassan", DebtType.OWED_TO_ME, 800.0)
        assertEquals(PaymentResult.Success, repository.addDebtPayment(owedByMeId, 1100.0))
        assertEquals(PaymentResult.Success, repository.addDebtPayment(owedToMeId, 300.0))

        // Verify transactions, net balance, and expenses did NOT change at all
        val txAfterDebts = repository.transactions.first()
        assertEquals(2, txAfterDebts.size)
        assertEquals(balanceBefore, FinancialCalculator.calculateNetBalance(txAfterDebts), 0.001)
        assertEquals(expenseBefore, FinancialCalculator.calculateTotalExpense(txAfterDebts), 0.001)

        // Verify debt summary math: owedToMe remaining (500) - owedByMe remaining (1000) = -500
        val debts = repository.debtsWithPayments.first()
        val summary = FinancialCalculator.calculateDebtSummary(debts)
        assertEquals(500.0, summary.totalOwedToMeRemaining, 0.001)
        assertEquals(1000.0, summary.totalOwedByMeRemaining, 0.001)
        assertEquals(-500.0, summary.netDebt, 0.001)
    }

    @Test
    fun `partial full repayment validation and payment deletion recalculate remaining and ratio`() = runBlocking {
        val debtId = repository.addDebt("Ahmed Ali", DebtType.OWED_BY_ME, 1000.0)

        // Reject zero and negative payments
        assertTrue(repository.addDebtPayment(debtId, 0.0) is PaymentResult.Error)
        assertTrue(repository.addDebtPayment(debtId, -50.0) is PaymentResult.Error)
        // Reject overpayment
        assertTrue(repository.addDebtPayment(debtId, 1200.0) is PaymentResult.Error)

        // Partial payment (250)
        assertEquals(PaymentResult.Success, repository.addDebtPayment(debtId, 250.0))
        var debtState = repository.debtsWithPayments.first().first { it.debt.id == debtId }
        assertEquals(250.0, debtState.totalPaid, 0.001)
        assertEquals(750.0, debtState.remainingAmount, 0.001)
        assertEquals(0.25, debtState.progressRatio, 0.001)
        assertEquals(25, debtState.progressPercent)

        // Reject payment exceeding remaining 750
        assertTrue(repository.addDebtPayment(debtId, 751.0) is PaymentResult.Error)

        // Full remaining payment (750)
        assertEquals(PaymentResult.Success, repository.addDebtPayment(debtId, 750.0))
        debtState = repository.debtsWithPayments.first().first { it.debt.id == debtId }
        assertEquals(1000.0, debtState.totalPaid, 0.001)
        assertEquals(0.0, debtState.remainingAmount, 0.001)
        assertEquals(1.0, debtState.progressRatio, 0.001)
        assertTrue(debtState.isFullyPaid)

        // Delete the 750 payment and verify immediate recalculation
        val payment750 = debtState.payments.first { it.amount == 750.0 }
        repository.deleteDebtPayment(payment750.id)

        debtState = repository.debtsWithPayments.first().first { it.debt.id == debtId }
        assertEquals(250.0, debtState.totalPaid, 0.001)
        assertEquals(750.0, debtState.remainingAmount, 0.001)
        assertEquals(0.25, debtState.progressRatio, 0.001)
    }

    @Test
    fun `default category seeding never duplicates and cleans up pre-existing duplicates`() = runBlocking {
        // Call multiple times to ensure idempotency
        repository.ensureDefaultCategories()
        repository.ensureDefaultCategories()
        repository.ensureDefaultCategories()

        val initialCategories = repository.categories.first()
        assertEquals(23, initialCategories.size)
        assertEquals(1, initialCategories.count { it.name == "طعام" && it.type == TransactionType.EXPENSE })

        // Simulate pre-existing duplicate category with a linked transaction
        val originalFood = initialCategories.first { it.name == "طعام" }
        val dupFoodId = database.categoryDao().insert(originalFood.copy(id = 0))
        repository.addTransaction(TransactionType.EXPENSE, 120.0, dupFoodId, System.currentTimeMillis())

        // Running ensureDefaultCategories must merge duplicate and preserve transaction link
        repository.ensureDefaultCategories()

        val cleanedCategories = repository.categories.first()
        assertEquals(23, cleanedCategories.size)
        assertEquals(1, cleanedCategories.count { it.name == "طعام" && it.type == TransactionType.EXPENSE })

        val tx = repository.transactions.first().single()
        assertEquals(originalFood.id, tx.categoryId)
    }
}
