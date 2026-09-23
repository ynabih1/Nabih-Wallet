package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class TransactionType(val titleAr: String, val titleEn: String) {
    EXPENSE("مصروف", "Expense"),
    INCOME("دخل", "Income")
}

data class CategoryItem(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val icon: ImageVector,
    val iconKey: String = "tag",
    val color: Long = 0xFF1E293B,
    val type: String = "EXPENSE",
    val descriptionAr: String = "",
    val descriptionEn: String = ""
)

data class PaymentMethodItem(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val icon: ImageVector
)

data class MonthlyBudgetStatus(
    val budgetAmount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val percentage: Float = 0f,
    val isWarning: Boolean = false,
    val isExceeded: Boolean = false,
    val isSet: Boolean = false,
    val isAlertEnabled: Boolean = true
)

enum class NotificationSeverity {
    INFO,
    WARNING,
    ALERT
}

data class SmartNotificationItem(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val messageAr: String,
    val messageEn: String,
    val severity: NotificationSeverity = NotificationSeverity.INFO,
    val type: String = "GENERAL", // BUDGET, DEBT, CATEGORY, DAILY
    val timestamp: Long = System.currentTimeMillis(),
    val actionRoute: String? = null
)

object FinancialConstants {
    private val _customIconOverrides = mutableMapOf<String, String>()

    // Default pre-configured categories to ensure a smooth onboarding experience
    val expenseCategories: List<CategoryItem> = listOf(
        CategoryItem("food", "طعام ومشروبات", "Food & Dining", Icons.Default.Fastfood, "food", 0xFFE07A5F, "EXPENSE", "وجبات ومستلزمات الغذاء"),
        CategoryItem("transport", "مواصلات وبنزين", "Transportation", Icons.Default.DirectionsCar, "car", 0xFF3D405B, "EXPENSE", "وقود ومواصلات عامة"),
        CategoryItem("bills", "فواتير ومرافق", "Bills & Utilities", Icons.Default.Receipt, "receipt", 0xFF81B29A, "EXPENSE", "كهرباء ومياه وإنترنت"),
        CategoryItem("shopping", "تسوق ومشتريات", "Shopping", Icons.Default.ShoppingBag, "cart", 0xFFF2CC8F, "EXPENSE", "ملابس وأجهزة ومقتنيات"),
        CategoryItem("health", "صحة وعلاج", "Healthcare", Icons.Default.LocalHospital, "health", 0xFFE76F51, "EXPENSE", "أدوية واستشارات طبية"),
        CategoryItem("entertainment", "ترفيه وخروجات", "Entertainment", Icons.Default.SportsEsports, "game", 0xFF6D597A, "EXPENSE", "سينما ونزهات وأنشطة")
    )

    val incomeCategories: List<CategoryItem> = listOf(
        CategoryItem("salary", "راتب شهري", "Salary", Icons.Default.Work, "salary", 0xFF2A9D8F, "INCOME", "الراتب الأساسي ومكافآت العمل"),
        CategoryItem("investment", "استثمارات وعوائد", "Investments", Icons.Default.TrendingUp, "trending_up", 0xFF264653, "INCOME", "أرباح أسهم أو عقارات"),
        CategoryItem("other_income", "دخل إضافي", "Other Income", Icons.Default.MonetizationOn, "cash", 0xFFE9C46A, "INCOME", "أي إيرادات أخرى متفرقة")
    )

    val paymentMethods: List<PaymentMethodItem> = listOf(
        PaymentMethodItem("cash", "نقد / كاش", "Cash", Icons.Default.MonetizationOn),
        PaymentMethodItem("card", "بطاقة بنكية", "Card", Icons.Default.CreditCard),
        PaymentMethodItem("transfer", "تحويل بنكي", "Bank Transfer", Icons.Default.AccountBalance),
        PaymentMethodItem("wallet", "محفظة إلكترونية", "E-Wallet", Icons.Default.Devices)
    )

    private val legacyMapping: Map<String, String> = emptyMap()

    fun setCustomIconOverrides(overrides: Map<String, String>) {
        _customIconOverrides.clear()
        _customIconOverrides.putAll(overrides)
    }

    fun normalizeCategoryName(categoryIdOrName: String): String {
        val trimmed = categoryIdOrName.trim()
        val lower = trimmed.lowercase()

        // 1. Check legacy mapping first
        val mappedLegacy = legacyMapping[trimmed] ?: legacyMapping[lower]
        if (mappedLegacy != null) return mappedLegacy

        // 2. Check current categories
        val all = expenseCategories + incomeCategories
        val match = all.firstOrNull {
            it.id.equals(trimmed, ignoreCase = true) ||
            it.nameAr.equals(trimmed, ignoreCase = true) ||
            it.nameEn.equals(trimmed, ignoreCase = true)
        }
        return match?.nameAr ?: trimmed
    }

    fun getCategoryItem(categoryIdOrName: String): CategoryItem? {
        val normalized = normalizeCategoryName(categoryIdOrName)
        val all = expenseCategories + incomeCategories
        return all.firstOrNull {
            it.nameAr.equals(normalized, ignoreCase = true) ||
            it.id.equals(categoryIdOrName, ignoreCase = true) ||
            it.nameEn.equals(categoryIdOrName, ignoreCase = true)
        }
    }

    fun getCategoryIcon(categoryIdOrName: String, overrides: Map<String, String>? = null): ImageVector {
        val trimmed = categoryIdOrName.trim()
        val normalized = normalizeCategoryName(trimmed)

        val activeMap = overrides ?: _customIconOverrides
        val customKey = activeMap[trimmed]
            ?: activeMap[normalized]
            ?: activeMap[trimmed.lowercase()]

        if (customKey != null) {
            return IconLibrary.getIconByKey(customKey)
        }

        val all = expenseCategories + incomeCategories
        val found = all.firstOrNull {
            it.nameAr.equals(normalized, ignoreCase = true) ||
            it.id.equals(trimmed, ignoreCase = true) ||
            it.nameEn.equals(trimmed, ignoreCase = true)
        }
        return found?.icon ?: Icons.Default.MoreHoriz
    }

    fun getCategoryColor(categoryIdOrName: String): Color {
        val normalized = normalizeCategoryName(categoryIdOrName)
        val all = expenseCategories + incomeCategories
        val item = all.firstOrNull {
            it.nameAr.equals(normalized, ignoreCase = true) ||
            it.id.equals(categoryIdOrName, ignoreCase = true) ||
            it.nameEn.equals(categoryIdOrName, ignoreCase = true)
        }
        return if (item != null) Color(item.color) else Color(0xFF1E293B)
    }

    fun getCategoryIconKey(categoryIdOrName: String, overrides: Map<String, String>? = null): String {
        val trimmed = categoryIdOrName.trim()
        val normalized = normalizeCategoryName(trimmed)

        val activeMap = overrides ?: _customIconOverrides
        val customKey = activeMap[trimmed]
            ?: activeMap[normalized]
            ?: activeMap[trimmed.lowercase()]

        if (customKey != null) {
            return customKey
        }

        val all = expenseCategories + incomeCategories
        return all.firstOrNull {
            it.nameAr.equals(normalized, ignoreCase = true) ||
            it.id.equals(trimmed, ignoreCase = true) ||
            it.nameEn.equals(trimmed, ignoreCase = true)
        }?.iconKey ?: "tag"
    }
}
