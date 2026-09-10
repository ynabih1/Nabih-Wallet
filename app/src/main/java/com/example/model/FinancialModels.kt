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

    // Standard Curated Expense Categories (Single clear concepts with distinct modern palette)
    val expenseCategories = listOf(
        CategoryItem(
            id = "housing",
            nameAr = "إيجار",
            nameEn = "Rent",
            icon = Icons.Default.Home,
            iconKey = "home",
            color = 0xFFC25E40,
            type = "EXPENSE",
            descriptionAr = "إيجار السكن والمصاريف العقارية",
            descriptionEn = "Housing rent and residential costs"
        ),
        CategoryItem(
            id = "groceries",
            nameAr = "بقالة",
            nameEn = "Groceries",
            icon = Icons.Default.ShoppingBag,
            iconKey = "grocery",
            color = 0xFF388E3C,
            type = "EXPENSE",
            descriptionAr = "التموين والمواد الغذائية للبيت",
            descriptionEn = "Household groceries and food essentials"
        ),
        CategoryItem(
            id = "dining",
            nameAr = "مطاعم",
            nameEn = "Restaurants",
            icon = Icons.Default.Fastfood,
            iconKey = "restaurant",
            color = 0xFFE06D3B,
            type = "EXPENSE",
            descriptionAr = "الوجبات الجاهزة والمشروبات والكافيهات",
            descriptionEn = "Restaurants, dining out, and cafes"
        ),
        CategoryItem(
            id = "transport",
            nameAr = "مواصلات",
            nameEn = "Transportation",
            icon = Icons.Default.DirectionsCar,
            iconKey = "car",
            color = 0xFFC97A3E,
            type = "EXPENSE",
            descriptionAr = "الوقود والمواصلات وتكاليف التنقل",
            descriptionEn = "Fuel, transit fares, and commute"
        ),
        CategoryItem(
            id = "bills",
            nameAr = "فواتير",
            nameEn = "Bills",
            icon = Icons.Default.Bolt,
            iconKey = "electricity",
            color = 0xFF2E6B9E,
            type = "EXPENSE",
            descriptionAr = "الكهرباء والمياه والإنترنت والهاتف",
            descriptionEn = "Electricity, water, internet, and phone"
        ),
        CategoryItem(
            id = "shopping",
            nameAr = "تسوق",
            nameEn = "Shopping",
            icon = Icons.Default.Checkroom,
            iconKey = "clothes",
            color = 0xFFA6445B,
            type = "EXPENSE",
            descriptionAr = "الملابس والمقتنيات والمشتريات الشخصية",
            descriptionEn = "Clothing, accessories, and personal items"
        ),
        CategoryItem(
            id = "health",
            nameAr = "صحة",
            nameEn = "Healthcare",
            icon = Icons.Default.LocalHospital,
            iconKey = "hospital",
            color = 0xFF00897B,
            type = "EXPENSE",
            descriptionAr = "الأدوية والعيادات والرعاية الصحية",
            descriptionEn = "Pharmacy, clinics, and medical care"
        ),
        CategoryItem(
            id = "education",
            nameAr = "تعليم",
            nameEn = "Education",
            icon = Icons.Default.School,
            iconKey = "school",
            color = 0xFF5B7A3E,
            type = "EXPENSE",
            descriptionAr = "الدراسة والكتب والدورات التعليمية",
            descriptionEn = "Courses, tuition, books, and study"
        ),
        CategoryItem(
            id = "entertainment",
            nameAr = "ترفيه",
            nameEn = "Entertainment",
            icon = Icons.Default.SportsEsports,
            iconKey = "games",
            color = 0xFF7E4A82,
            type = "EXPENSE",
            descriptionAr = "الأنشطة والألعاب والرحلات الترفيهية",
            descriptionEn = "Outings, games, movies, and hobbies"
        ),
        CategoryItem(
            id = "travel",
            nameAr = "سفر",
            nameEn = "Travel",
            icon = Icons.Default.Flight,
            iconKey = "flight",
            color = 0xFF1976D2,
            type = "EXPENSE",
            descriptionAr = "تذاكر السفر والإقامة والرحلات",
            descriptionEn = "Flight tickets, hotels, and tourism"
        ),
        CategoryItem(
            id = "maintenance",
            nameAr = "صيانة",
            nameEn = "Maintenance",
            icon = Icons.Default.Build,
            iconKey = "build",
            color = 0xFFB8562E,
            type = "EXPENSE",
            descriptionAr = "التصليحات وصيانة المنزل والأجهزة",
            descriptionEn = "Home repairs and hardware maintenance"
        ),
        CategoryItem(
            id = "subscriptions",
            nameAr = "اشتراكات",
            nameEn = "Subscriptions",
            icon = Icons.Default.Tv,
            iconKey = "tv",
            color = 0xFF455A64,
            type = "EXPENSE",
            descriptionAr = "الخدمات والتطبيقات والاشتراكات الشهرية",
            descriptionEn = "Streaming, software, and digital services"
        ),
        CategoryItem(
            id = "gifts",
            nameAr = "هدايا",
            nameEn = "Gifts",
            icon = Icons.Default.CardGiftcard,
            iconKey = "gift",
            color = 0xFFD32F2F,
            type = "EXPENSE",
            descriptionAr = "الهدايا والمناسبات والتبرعات",
            descriptionEn = "Gifts, family occasions, and charity"
        ),
        CategoryItem(
            id = "other_expense",
            nameAr = "أخرى",
            nameEn = "Other",
            icon = Icons.Default.MoreHoriz,
            iconKey = "more",
            color = 0xFF6D685E,
            type = "EXPENSE",
            descriptionAr = "أي مصروفات ونفقات متنوعة أخرى",
            descriptionEn = "Miscellaneous and unclassified expenses"
        )
    )

    // Standard Curated Income Categories (Single clear concepts)
    val incomeCategories = listOf(
        CategoryItem(
            id = "salary",
            nameAr = "راتب",
            nameEn = "Salary",
            icon = Icons.Default.AccountBalanceWallet,
            iconKey = "wallet",
            color = 0xFF2E7D32,
            type = "INCOME",
            descriptionAr = "الراتب الأساسي والدخل الشهري",
            descriptionEn = "Primary monthly job salary"
        ),
        CategoryItem(
            id = "freelance",
            nameAr = "عمل حر",
            nameEn = "Freelance",
            icon = Icons.Default.Laptop,
            iconKey = "laptop",
            color = 0xFF00796B,
            type = "INCOME",
            descriptionAr = "دخل المشاريع المستقلة والاستشارات",
            descriptionEn = "Freelance gigs and consulting"
        ),
        CategoryItem(
            id = "investments",
            nameAr = "استثمار",
            nameEn = "Investments",
            icon = Icons.Default.TrendingUp,
            iconKey = "trending_up",
            color = 0xFF388E3C,
            type = "INCOME",
            descriptionAr = "عوائد الأسهم والاستثمارات والأرباح",
            descriptionEn = "Stock gains and investment dividends"
        ),
        CategoryItem(
            id = "business",
            nameAr = "تجارة",
            nameEn = "Business",
            icon = Icons.Default.Store,
            iconKey = "store",
            color = 0xFFC07D15,
            type = "INCOME",
            descriptionAr = "أرباح التجارة والمبيعات الخاصة",
            descriptionEn = "Trade profits and business sales"
        ),
        CategoryItem(
            id = "bonus",
            nameAr = "مكافأة",
            nameEn = "Bonus",
            icon = Icons.Default.MonetizationOn,
            iconKey = "coins",
            color = 0xFF00897B,
            type = "INCOME",
            descriptionAr = "الحوافز والمكافآت والعمولات",
            descriptionEn = "Performance bonus and incentives"
        ),
        CategoryItem(
            id = "gift_income",
            nameAr = "هدية",
            nameEn = "Gift",
            icon = Icons.Default.CardGiftcard,
            iconKey = "gift",
            color = 0xFFE64A19,
            type = "INCOME",
            descriptionAr = "الهدايا النقدية والإكراميات",
            descriptionEn = "Cash gifts and family grants"
        ),
        CategoryItem(
            id = "rental_income",
            nameAr = "إيجارات",
            nameEn = "Rental",
            icon = Icons.Default.Apartment,
            iconKey = "apartment",
            color = 0xFF4E6E4E,
            type = "INCOME",
            descriptionAr = "عوائد إيجار العقارات والأصول",
            descriptionEn = "Real estate and rental earnings"
        ),
        CategoryItem(
            id = "other_income",
            nameAr = "أخرى",
            nameEn = "Other",
            icon = Icons.Default.MoreHoriz,
            iconKey = "more",
            color = 0xFF546E7A,
            type = "INCOME",
            descriptionAr = "أي مداخيل أو إيرادات أخرى",
            descriptionEn = "Miscellaneous financial inflows"
        )
    )

    val paymentMethods = listOf(
        PaymentMethodItem("cash", "نقداً", "Cash", Icons.Default.MonetizationOn),
        PaymentMethodItem("card", "بطاقة بنكية", "Bank Card", Icons.Default.CreditCard),
        PaymentMethodItem("wallet", "محفظة إلكترونية", "E-Wallet", Icons.Default.AccountBalanceWallet),
        PaymentMethodItem("transfer", "تحويل بنكي", "Bank Transfer", Icons.Default.AccountBalance)
    )

    // Legacy taxonomy migration dictionary for seamless backward compatibility
    private val legacyMapping = mapOf(
        "السكن والإيجار" to "إيجار",
        "سكن" to "إيجار",
        "مصروفات المعيشة" to "إيجار",
        "living" to "إيجار",
        "living expenses" to "إيجار",
        "rent" to "إيجار",
        "housing" to "إيجار",
        "البقالة والسوبرماركت" to "بقالة",
        "سوبرماركت" to "بقالة",
        "groceries" to "بقالة",
        "market" to "بقالة",
        "المطاعم والمقاهي" to "مطاعم",
        "طعام ومشروبات" to "مطاعم",
        "كافيهات" to "مطاعم",
        "food" to "مطاعم",
        "food & drinks" to "مطاعم",
        "dining" to "مطاعم",
        "المواصلات والوقود" to "مواصلات",
        "وقود" to "مواصلات",
        "بنزين" to "مواصلات",
        "transport" to "مواصلات",
        "الفواتير والخدمات" to "فواتير",
        "خدمات" to "فواتير",
        "bills" to "فواتير",
        "bills & utilities" to "فواتير",
        "التسوق والملابس" to "تسوق",
        "ملابس" to "تسوق",
        "shopping" to "تسوق",
        "الصحة والعلاج" to "صحة",
        "علاج" to "صحة",
        "صحة وطب" to "صحة",
        "health" to "صحة",
        "health & medical" to "صحة",
        "التعليم والدراسة" to "تعليم",
        "دراسة" to "تعليم",
        "مصروفات الدراسة" to "تعليم",
        "study" to "تعليم",
        "education" to "تعليم",
        "الترفيه والأنشطة" to "ترفيه",
        "أنشطة" to "ترفيه",
        "entertainment" to "ترفيه",
        "السفر والرحلات" to "سفر",
        "رحلات" to "سفر",
        "travel" to "سفر",
        "الصيانة والمنزل" to "صيانة",
        "منزل" to "صيانة",
        "maintenance" to "صيانة",
        "اشتراكات وخدمات رقمية" to "اشتراكات",
        "subscriptions" to "اشتراكات",
        "هدايا وتبرعات" to "هدايا",
        "تبرعات" to "هدايا",
        "gifts" to "هدايا",
        "الراتب الشهري" to "راتب",
        "salary" to "راتب",
        "العمل الحر والمشاريع" to "عمل حر",
        "مشاريع" to "عمل حر",
        "freelance" to "عمل حر",
        "الاستثمارات والأرباح" to "استثمار",
        "أرباح" to "استثمار",
        "استثمار وأرباح" to "استثمار",
        "investment" to "استثمار",
        "التجارة والمبيعات" to "تجارة",
        "مبيعات" to "تجارة",
        "business" to "تجارة",
        "مكافآت وحوافز" to "مكافأة",
        "حوافز" to "مكافأة",
        "bonus" to "مكافأة",
        "هدايا وعطايا" to "هدية",
        "عطايا" to "هدية",
        "gift" to "هدية",
        "عوائد وإيجارات" to "إيجارات",
        "rental" to "إيجارات",
        "مصروفات أخرى" to "أخرى",
        "إيرادات أخرى" to "أخرى",
        "other" to "أخرى",
        "other_expense" to "أخرى",
        "other_income" to "أخرى"
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
