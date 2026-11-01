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
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
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

object FinancialConstants {
    private val _customIconOverrides = mutableMapOf<String, String>()

    // Comprehensive pre-configured categories with rich icons and modern colors
    val expenseCategories: List<CategoryItem> = listOf(
        CategoryItem("food", "طعام", "Food & Dining", Icons.Default.Fastfood, "fastfood", 0xFFE07A5F, "EXPENSE", "وجبات ومطاعم"),
        CategoryItem("grocery", "بقالة", "Groceries", Icons.Default.ShoppingCart, "shopping_cart", 0xFF4A7C59, "EXPENSE", "تموين ومستلزمات منزلية"),
        CategoryItem("housing", "سكن", "Housing & Rent", Icons.Default.Home, "home", 0xFF457B9D, "EXPENSE", "إيجار وصيانة"),
        CategoryItem("bills", "فواتير", "Bills & Utilities", Icons.Default.Receipt, "receipt", 0xFF81B29A, "EXPENSE", "كهرباء ومياه وإنترنت"),
        CategoryItem("transport", "مواصلات", "Transportation", Icons.Default.DirectionsCar, "car", 0xFF3D405B, "EXPENSE", "وقود وبنزين ومواصلات"),
        CategoryItem("shopping", "تسوق", "Shopping", Icons.Default.ShoppingBag, "shopping_bag", 0xFFD4A373, "EXPENSE", "ملابس ومقتنيات"),
        CategoryItem("health", "صحة", "Healthcare", Icons.Default.LocalHospital, "hospital", 0xFFE63946, "EXPENSE", "علاج وأدوية"),
        CategoryItem("education", "تعليم", "Education", Icons.Default.School, "school", 0xFF588157, "EXPENSE", "دراسة ودورات"),
        CategoryItem("entertainment", "ترفيه", "Entertainment", Icons.Default.SportsEsports, "games", 0xFF6D597A, "EXPENSE", "نزهات ورحلات"),
        CategoryItem("family", "أسرة", "Family & Kids", Icons.Default.ChildCare, "baby", 0xFFF4A261, "EXPENSE", "مصاريف العائلة"),
        CategoryItem("personal_care", "عناية", "Personal Care", Icons.Default.FitnessCenter, "fitness", 0xFF2A9D8F, "EXPENSE", "لياقة ومظهر"),
        CategoryItem("charity", "هدايا", "Charity & Gifts", Icons.Default.CardGiftcard, "gift", 0xFF38B000, "EXPENSE", "تبرعات وهدايا"),
        CategoryItem("maintenance", "صيانة", "Maintenance", Icons.Default.Build, "build", 0xFF7F5539, "EXPENSE", "تصليحات"),
        CategoryItem("subscriptions", "اشتراكات", "Subscriptions", Icons.Default.Devices, "gadgets", 0xFF3A86FF, "EXPENSE", "خدمات رقمية"),
        CategoryItem("other_expense", "أخرى", "Other Expenses", Icons.Default.MoreHoriz, "more", 0xFF6C757D, "EXPENSE", "نفقات متنوعة")
    )

    val incomeCategories: List<CategoryItem> = listOf(
        CategoryItem("salary", "راتب", "Salary", Icons.Default.Work, "work", 0xFF2A9D8F, "INCOME", "راتب أساسي"),
        CategoryItem("freelance", "عمل حر", "Freelance & Projects", Icons.Default.Laptop, "laptop", 0xFF3B82F6, "INCOME", "مشروعات مستقلة"),
        CategoryItem("investment", "استثمار", "Investments", Icons.Default.TrendingUp, "trending_up", 0xFF264653, "INCOME", "أرباح وعوائد"),
        CategoryItem("rental", "إيجار", "Rental Income", Icons.Default.Apartment, "apartment", 0xFF8B5CF6, "INCOME", "عوائد عقارية"),
        CategoryItem("bonus", "مكافأة", "Bonuses & Rewards", Icons.Default.EmojiEvents, "trophy", 0xFFF59E0B, "INCOME", "حوافز وبدلات"),
        CategoryItem("support", "دعم", "Gifts & Support", Icons.Default.CardGiftcard, "gift", 0xFFEC4899, "INCOME", "مساعدات وهدايا"),
        CategoryItem("other_income", "أخرى", "Other Income", Icons.Default.MonetizationOn, "coins", 0xFFE9C46A, "INCOME", "إيرادات متفرقة")
    )

    val paymentMethods: List<PaymentMethodItem> = listOf(
        PaymentMethodItem("cash", "كاش", "Cash", Icons.Default.MonetizationOn),
        PaymentMethodItem("card", "بطاقة", "Card", Icons.Default.CreditCard),
        PaymentMethodItem("transfer", "تحويل", "Bank Transfer", Icons.Default.AccountBalance),
        PaymentMethodItem("wallet", "محفظة", "E-Wallet", Icons.Default.Devices)
    )

    private val legacyMapping: Map<String, String> = mapOf(
        "طعام ومشروبات" to "طعام",
        "سوبرماركت وبقالة" to "بقالة",
        "سكن وإيجار" to "سكن",
        "فواتير ومرافق" to "فواتير",
        "مواصلات ووقود" to "مواصلات",
        "مواصلات وبنزين" to "مواصلات",
        "تسوق ومستلزمات" to "تسوق",
        "تسوق ومشتريات" to "تسوق",
        "صحة وعلاج" to "صحة",
        "تعليم ودراسة" to "تعليم",
        "ترفيه وسياحة" to "ترفيه",
        "ترفيه وخروجات" to "ترفيه",
        "أسرة وأطفال" to "أسرة",
        "عناية شخصية ولياقة" to "عناية",
        "هدايا وصدقات" to "هدايا",
        "صيانة وإصلاحات" to "صيانة",
        "اشتراكات وخدمات" to "اشتراكات",
        "مصروفات أخرى" to "أخرى",
        "راتب شهري" to "راتب",
        "عمل حر ومشروعات" to "عمل حر",
        "استثمارات وأرباح" to "استثمار",
        "استثمارات وعوائد" to "استثمار",
        "عوائد عقار وإيجار" to "إيجار",
        "مكافآت وحوافز" to "مكافأة",
        "هدايا ودعم مالي" to "دعم",
        "دخل إضافي" to "أخرى",
        "نقد / كاش" to "كاش",
        "بطاقة بنكية" to "بطاقة",
        "تحويل بنكي" to "تحويل",
        "محفظة إلكترونية" to "محفظة"
    )

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
