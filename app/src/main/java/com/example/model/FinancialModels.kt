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

    // Standard Curated Expense Categories (14 comprehensive categories) - All Black
    val expenseCategories = listOf(
        CategoryItem(
            id = "housing",
            nameAr = "السكن والإيجار",
            nameEn = "Housing & Rent",
            icon = Icons.Default.Home,
            iconKey = "home",
            color = 0xFFC35742,
            type = "EXPENSE",
            descriptionAr = "إيجار المنزل، مصاريف الإقامة، وتكاليف العقار",
            descriptionEn = "Rent, housing costs, and property fees"
        ),
        CategoryItem(
            id = "groceries",
            nameAr = "البقالة والسوبرماركت",
            nameEn = "Groceries & Market",
            icon = Icons.Default.ShoppingBag,
            iconKey = "grocery",
            color = 0xFF487A5B,
            type = "EXPENSE",
            descriptionAr = "مشتريات البيت، الخضار، واللوازم التموينية",
            descriptionEn = "Household groceries, supermarket, and fresh food"
        ),
        CategoryItem(
            id = "dining",
            nameAr = "المطاعم والمقاهي",
            nameEn = "Dining & Cafes",
            icon = Icons.Default.Fastfood,
            iconKey = "restaurant",
            color = 0xFFD97757,
            type = "EXPENSE",
            descriptionAr = "وجبات جاهزة، مطاعم، كافيهات، وقهوة",
            descriptionEn = "Restaurants, dining out, fast food, and coffee"
        ),
        CategoryItem(
            id = "transport",
            nameAr = "المواصلات والوقود",
            nameEn = "Transport & Fuel",
            icon = Icons.Default.DirectionsCar,
            iconKey = "car",
            color = 0xFFC97A3E,
            type = "EXPENSE",
            descriptionAr = "بنزين، تاكسي، مواصلات عامة، وصيانة سيارات",
            descriptionEn = "Gas, fuel, taxi, transit fares, and vehicle costs"
        ),
        CategoryItem(
            id = "bills",
            nameAr = "الفواتير والخدمات",
            nameEn = "Bills & Utilities",
            icon = Icons.Default.Bolt,
            iconKey = "electricity",
            color = 0xFF3D5A80,
            type = "EXPENSE",
            descriptionAr = "كهرباء، مياه، غاز، إنترنت، وفاتورة الهاتف",
            descriptionEn = "Electricity, water, gas, internet, and phone"
        ),
        CategoryItem(
            id = "shopping",
            nameAr = "التسوق والملابس",
            nameEn = "Shopping & Apparel",
            icon = Icons.Default.Checkroom,
            iconKey = "clothes",
            color = 0xFFA25968,
            type = "EXPENSE",
            descriptionAr = "ملابس، أحذية، إكسسوارات، ومشتريات شخصية",
            descriptionEn = "Clothing, shoes, accessories, and personal goods"
        ),
        CategoryItem(
            id = "health",
            nameAr = "الصحة والعلاج",
            nameEn = "Health & Medical",
            icon = Icons.Default.LocalHospital,
            iconKey = "hospital",
            color = 0xFF3A7D84,
            type = "EXPENSE",
            descriptionAr = "أدوية، كشف طبي، عيادات، وتأمين صحي",
            descriptionEn = "Pharmacy, doctor visits, hospital, and medicine"
        ),
        CategoryItem(
            id = "education",
            nameAr = "التعليم والدراسة",
            nameEn = "Education & Study",
            icon = Icons.Default.School,
            iconKey = "school",
            color = 0xFF5B6B46,
            type = "EXPENSE",
            descriptionAr = "رسوم دراسية، كورسات، كتب، وأدوات مكتبية",
            descriptionEn = "Tuition fees, courses, books, and stationeries"
        ),
        CategoryItem(
            id = "entertainment",
            nameAr = "الترفيه والأنشطة",
            nameEn = "Entertainment & Leisure",
            icon = Icons.Default.SportsEsports,
            iconKey = "games",
            color = 0xFF8D5B7B,
            type = "EXPENSE",
            descriptionAr = "سينما، ألعاب، رحلات ترفيهية، وهوايات",
            descriptionEn = "Cinema, video games, outings, and hobbies"
        ),
        CategoryItem(
            id = "travel",
            nameAr = "السفر والرحلات",
            nameEn = "Travel & Tourism",
            icon = Icons.Default.Flight,
            iconKey = "flight",
            color = 0xFF2E6F7E,
            type = "EXPENSE",
            descriptionAr = "تذاكر طيران، فنادق، وتكاليف السياحة",
            descriptionEn = "Flight tickets, hotels, and holiday expenses"
        ),
        CategoryItem(
            id = "maintenance",
            nameAr = "الصيانة والمنزل",
            nameEn = "Home & Maintenance",
            icon = Icons.Default.Build,
            iconKey = "build",
            color = 0xFFBD5338,
            type = "EXPENSE",
            descriptionAr = "أعمال صيانة، سباكة، أجهزة منزلية، وتصليحات",
            descriptionEn = "Repairs, home maintenance, and furniture"
        ),
        CategoryItem(
            id = "subscriptions",
            nameAr = "اشتراكات وخدمات رقمية",
            nameEn = "Digital Subscriptions",
            icon = Icons.Default.Tv,
            iconKey = "tv",
            color = 0xFF4A5568,
            type = "EXPENSE",
            descriptionAr = "اشتراكات نتفلكس، سبوتيفاي، برامج، وتطبيقات",
            descriptionEn = "Streaming services, software, and cloud tools"
        ),
        CategoryItem(
            id = "gifts",
            nameAr = "هدايا وتبرعات",
            nameEn = "Gifts & Charity",
            icon = Icons.Default.CardGiftcard,
            iconKey = "gift",
            color = 0xFFD46A55,
            type = "EXPENSE",
            descriptionAr = "هدايا المناسبات، صدقات، وتبرعات خيرية",
            descriptionEn = "Gifts, charitable donations, and contributions"
        ),
        CategoryItem(
            id = "other_expense",
            nameAr = "مصروفات أخرى",
            nameEn = "Other Expenses",
            icon = Icons.Default.MoreHoriz,
            iconKey = "more",
            color = 0xFF7A7568,
            type = "EXPENSE",
            descriptionAr = "نفقات متنوعة أخرى غير مصنفة",
            descriptionEn = "Miscellaneous and unclassified expenses"
        )
    )

    // Standard Curated Income Categories (8 comprehensive categories)
    val incomeCategories = listOf(
        CategoryItem(
            id = "salary",
            nameAr = "الراتب الشهري",
            nameEn = "Monthly Salary",
            icon = Icons.Default.AccountBalanceWallet,
            iconKey = "wallet",
            color = 0xFF437A53,
            type = "INCOME",
            descriptionAr = "الدخل الأساسي والراتب الوظيفي الشهري",
            descriptionEn = "Primary monthly job salary"
        ),
        CategoryItem(
            id = "freelance",
            nameAr = "العمل الحر والمشاريع",
            nameEn = "Freelance & Projects",
            icon = Icons.Default.Laptop,
            iconKey = "laptop",
            color = 0xFF2E7D68,
            type = "INCOME",
            descriptionAr = "دخل العمل الحر، الاستشارات، والمشاريع الجانبية",
            descriptionEn = "Freelance gigs, consulting, and side projects"
        ),
        CategoryItem(
            id = "investments",
            nameAr = "الاستثمارات والأرباح",
            nameEn = "Investments & Dividends",
            icon = Icons.Default.TrendingUp,
            iconKey = "trending_up",
            color = 0xFF5C7A38,
            type = "INCOME",
            descriptionAr = "عوائد الأسهم، صناديق الاستثمار، والتوزيعات",
            descriptionEn = "Stock gains, investment funds, and dividends"
        ),
        CategoryItem(
            id = "business",
            nameAr = "التجارة والمبيعات",
            nameEn = "Business & Sales",
            icon = Icons.Default.Store,
            iconKey = "store",
            color = 0xFFB8860B,
            type = "INCOME",
            descriptionAr = "أرباح التجارة، مبيعات المنتجات، والأعمال الخاصة",
            descriptionEn = "Trade profits, product sales, and commercial business"
        ),
        CategoryItem(
            id = "bonus",
            nameAr = "مكافآت وحوافز",
            nameEn = "Bonuses & Incentives",
            icon = Icons.Default.MonetizationOn,
            iconKey = "coins",
            color = 0xFF3B8A5A,
            type = "INCOME",
            descriptionAr = "مكافأة نهاية العام، عمولات، وحوافز التميز",
            descriptionEn = "Performance bonus, incentives, and commissions"
        ),
        CategoryItem(
            id = "gift_income",
            nameAr = "هدايا وعطايا",
            nameEn = "Gifts & Grants",
            icon = Icons.Default.CardGiftcard,
            iconKey = "gift",
            color = 0xFFD97757,
            type = "INCOME",
            descriptionAr = "هدايا نقدية، إكراميات، وعطايا أسرية",
            descriptionEn = "Cash gifts, family allowances, and grants"
        ),
        CategoryItem(
            id = "rental_income",
            nameAr = "عوائد وإيجارات",
            nameEn = "Rental Income",
            icon = Icons.Default.Apartment,
            iconKey = "apartment",
            color = 0xFF4D724D,
            type = "INCOME",
            descriptionAr = "إيجارات العقارات، السيارات، أو الأصول المؤجرة",
            descriptionEn = "Real estate rental income and lease earnings"
        ),
        CategoryItem(
            id = "other_income",
            nameAr = "إيرادات أخرى",
            nameEn = "Other Income",
            icon = Icons.Default.MoreHoriz,
            iconKey = "more",
            color = 0xFF607274,
            type = "INCOME",
            descriptionAr = "أي عوائد أو مداخيل أخرى متنوعة",
            descriptionEn = "Miscellaneous and other financial inflows"
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
        "مصروفات المعيشة" to "السكن والإيجار",
        "living" to "السكن والإيجار",
        "living expenses" to "السكن والإيجار",
        "طعام ومشروبات" to "المطاعم والمقاهي",
        "food" to "المطاعم والمقاهي",
        "food & drinks" to "المطاعم والمقاهي",
        "مواصلات" to "المواصلات والوقود",
        "transport" to "المواصلات والوقود",
        "فواتير وخدمات" to "الفواتير والخدمات",
        "bills" to "الفواتير والخدمات",
        "bills & utilities" to "الفواتير والخدمات",
        "تسوق" to "التسوق والملابس",
        "shopping" to "التسوق والملابس",
        "صحة وطب" to "الصحة والعلاج",
        "health" to "الصحة والعلاج",
        "health & medical" to "الصحة والعلاج",
        "مصروفات الدراسة" to "التعليم والدراسة",
        "study" to "التعليم والدراسة",
        "education" to "التعليم والدراسة",
        "ترفيه" to "الترفيه والأنشطة",
        "entertainment" to "الترفيه والأنشطة",
        "راتب شهري" to "الراتب الشهري",
        "salary" to "الراتب الشهري",
        "عمل حر" to "العمل الحر والمشاريع",
        "freelance" to "العمل الحر والمشاريع",
        "استثمار وأرباح" to "الاستثمارات والأرباح",
        "investment" to "الاستثمارات والأرباح",
        "مكافأة" to "مكافآت وحوافز",
        "bonus" to "مكافآت وحوافز",
        "هدية" to "هدايا وتبرعات",
        "gift" to "هدايا وتبرعات",
        "أخرى" to "مصروفات أخرى",
        "other" to "مصروفات أخرى",
        "other_expense" to "مصروفات أخرى",
        "other_income" to "إيرادات أخرى"
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
