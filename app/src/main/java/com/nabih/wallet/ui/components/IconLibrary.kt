package com.nabih.wallet.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

object IconLibrary {

    val all: List<CategoryIconOption> = listOf(
        CategoryIconOption("restaurant", "طعام", Icons.Rounded.Restaurant),
        CategoryIconOption("shopping_cart", "بقالة", Icons.Rounded.ShoppingCart),
        CategoryIconOption("directions_car", "مواصلات", Icons.Rounded.DirectionsCar),
        CategoryIconOption("receipt_long", "فواتير", Icons.Rounded.ReceiptLong),
        CategoryIconOption("home", "سكن", Icons.Rounded.Home),
        CategoryIconOption("smartphone", "هاتف", Icons.Rounded.Smartphone),
        CategoryIconOption("school", "تعليم", Icons.Rounded.School),
        CategoryIconOption("medical_services", "صحة", Icons.Rounded.MedicalServices),
        CategoryIconOption("shopping_bag", "تسوق", Icons.Rounded.ShoppingBag),
        CategoryIconOption("subscriptions", "اشتراكات", Icons.Rounded.Subscriptions),
        CategoryIconOption("sports_esports", "ترفيه", Icons.Rounded.SportsEsports),
        CategoryIconOption("family_restroom", "أسرة", Icons.Rounded.FamilyRestroom),
        CategoryIconOption("spa", "عناية", Icons.Rounded.Spa),
        CategoryIconOption("card_giftcard", "هدايا", Icons.Rounded.CardGiftcard),
        CategoryIconOption("build", "صيانة", Icons.Rounded.Build),
        CategoryIconOption("more_horiz", "أخرى", Icons.Rounded.MoreHoriz),
        CategoryIconOption("payments", "راتب", Icons.Rounded.Payments),
        CategoryIconOption("laptop_mac", "عمل حر", Icons.Rounded.LaptopMac),
        CategoryIconOption("trending_up", "استثمار", Icons.Rounded.TrendingUp),
        CategoryIconOption("apartment", "إيجار", Icons.Rounded.Apartment),
        CategoryIconOption("emoji_events", "مكافأة", Icons.Rounded.EmojiEvents),
        CategoryIconOption("volunteer_activism", "دعم", Icons.Rounded.VolunteerActivism),
        CategoryIconOption("account_balance_wallet", "محفظة", Icons.Rounded.AccountBalanceWallet),
        CategoryIconOption("menu_book", "كتب", Icons.Rounded.MenuBook),
        CategoryIconOption("coffee", "قهوة", Icons.Rounded.Coffee),
        CategoryIconOption("local_gas_station", "وقود", Icons.Rounded.LocalGasStation),
        CategoryIconOption("directions_bus", "نقل", Icons.Rounded.DirectionsBus),
        CategoryIconOption("wifi", "إنترنت", Icons.Rounded.Wifi),
        CategoryIconOption("fitness_center", "رياضة", Icons.Rounded.FitnessCenter),
        CategoryIconOption("flight", "سفر", Icons.Rounded.Flight),
        CategoryIconOption("movie", "سينما", Icons.Rounded.Movie),
        CategoryIconOption("pets", "حيوانات", Icons.Rounded.Pets),
        CategoryIconOption("savings", "ادخار", Icons.Rounded.Savings),
        CategoryIconOption("work", "وظيفة", Icons.Rounded.Work)
    )

    private val byKey: Map<String, ImageVector> = all.associate { it.key to it.icon }

    fun iconFor(key: String): ImageVector = byKey[key] ?: Icons.Rounded.Category
}
