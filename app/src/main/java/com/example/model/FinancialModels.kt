package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Work
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
    val color: Long = 0xFF1DB954,
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

    // 6-color chart & category palette in exact specified order:
    // #1DB954, #1D9BF0, #7856FF, #FF7A00, #F91880, #FFD400
    val expenseCategories: List<CategoryItem> = listOf(
        CategoryItem("food", "طعام", "Food & Dining", Icons.Outlined.Fastfood, "fastfood", 0xFF1DB954, "EXPENSE", "وجبات ومطاعم"),
        CategoryItem("grocery", "بقالة", "Groceries", Icons.Outlined.ShoppingCart, "shopping_cart", 0xFF1D9BF0, "EXPENSE", "تموين ومستلزمات منزلية"),
        CategoryItem("housing", "سكن", "Housing & Rent", Icons.Outlined.Home, "home", 0xFF7856FF, "EXPENSE", "إيجار وصيانة"),
        CategoryItem("bills", "فواتير", "Bills & Utilities", Icons.Outlined.Receipt, "receipt", 0xFFFF7A00, "EXPENSE", "كهرباء ومياه وإنترنت"),
        CategoryItem("transport", "مواصلات", "Transportation", Icons.Outlined.DirectionsCar, "car", 0xFFF91880, "EXPENSE", "وقود وبنزين ومواصلات"),
        CategoryItem("shopping", "تسوق", "Shopping", Icons.Outlined.ShoppingBag, "shopping_bag", 0xFFFFD400, "EXPENSE", "ملابس ومقتنيات"),
        CategoryItem("health", "صحة", "Healthcare", Icons.Outlined.LocalHospital, "hospital", 0xFF1DB954, "EXPENSE", "علاج وأدوية"),
        CategoryItem("education", "تعليم", "Education", Icons.Outlined.School, "school", 0xFF1D9BF0, "EXPENSE", "دراسة ودورات"),
        CategoryItem("entertainment", "ترفيه", "Entertainment", Icons.Outlined.SportsEsports, "games", 0xFF7856FF, "EXPENSE", "نزهات ورحلات"),
        CategoryItem("family", "أسرة", "Family & Kids", Icons.Outlined.ChildCare, "baby", 0xFFFF7A00, "EXPENSE", "مصاريف العائلة"),
        CategoryItem("personal_care", "عناية", "Personal Care", Icons.Outlined.FitnessCenter, "fitness", 0xFFF91880, "EXPENSE", "لياقة ومظهر"),
        CategoryItem("charity", "هدايا", "Charity & Gifts", Icons.Outlined.CardGiftcard, "gift", 0xFFFFD400, "EXPENSE", "تبرعات وهدايا"),
        CategoryItem("maintenance", "صيانة", "Maintenance", Icons.Outlined.Build, "build", 0xFF1DB954, "EXPENSE", "تصليحات"),
        CategoryItem("subscriptions", "اشتراكات", "Subscriptions", Icons.Outlined.Devices, "gadgets", 0xFF1D9BF0, "EXPENSE", "خدمات رقمية"),
        CategoryItem("other_expense", "أخرى", "Other Expenses", Icons.Outlined.MoreHoriz, "more", 0xFF7856FF, "EXPENSE", "نفقات متنوعة")
    )

    val incomeCategories: List<CategoryItem> = listOf(
        CategoryItem("salary", "راتب", "Salary", Icons.Outlined.Work, "work", 0xFF1DB954, "INCOME", "راتب أساسي"),
        CategoryItem("freelance", "عمل حر", "Freelance & Projects", Icons.Outlined.Laptop, "laptop", 0xFF1D9BF0, "INCOME", "مشروعات مستقلة"),
        CategoryItem("investment", "استثمار", "Investments", Icons.Outlined.TrendingUp, "trending_up", 0xFF7856FF, "INCOME", "أرباح وعوائد"),
        CategoryItem("rental", "إيجار", "Rental Income", Icons.Outlined.Apartment, "apartment", 0xFFFF7A00, "INCOME", "عوائد عقارية"),
        CategoryItem("bonus", "مكافأة", "Bonuses & Rewards", Icons.Outlined.EmojiEvents, "trophy", 0xFFF91880, "INCOME", "حوافز وبدلات"),
        CategoryItem("support", "دعم", "Gifts & Support", Icons.Outlined.CardGiftcard, "gift", 0xFFFFD400, "INCOME", "مساعدات وهدايا"),
        CategoryItem("other_income", "أخرى", "Other Income", Icons.Outlined.MonetizationOn, "coins", 0xFF1DB954, "INCOME", "إيرادات متفرقة")
    )

    val paymentMethods: List<PaymentMethodItem> = listOf(
        PaymentMethodItem("cash", "كاش", "Cash", Icons.Outlined.MonetizationOn),
        PaymentMethodItem("card", "بطاقة", "Card", Icons.Outlined.CreditCard),
        PaymentMethodItem("transfer", "تحويل", "Bank Transfer", Icons.Outlined.AccountBalance),
        PaymentMethodItem("wallet", "محفظة", "E-Wallet", Icons.Outlined.Devices)
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

        val mappedLegacy = legacyMapping[trimmed] ?: legacyMapping[lower]
        if (mappedLegacy != null) return mappedLegacy

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
        return found?.icon ?: Icons.Outlined.MoreHoriz
    }

    fun getCategoryColor(categoryIdOrName: String): Color {
        val normalized = normalizeCategoryName(categoryIdOrName)
        val all = expenseCategories + incomeCategories
        val item = all.firstOrNull {
            it.nameAr.equals(normalized, ignoreCase = true) ||
            it.id.equals(categoryIdOrName, ignoreCase = true) ||
            it.nameEn.equals(categoryIdOrName, ignoreCase = true)
        }
        return if (item != null) Color(item.color) else Color(0xFF1DB954)
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
