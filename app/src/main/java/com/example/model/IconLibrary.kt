package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class IconCategoryGroup(val titleAr: String, val titleEn: String) {
    ALL("الكل", "All"),
    FINANCE("مال ودخل", "Finance"),
    FOOD("طعام ومشروبات", "Food & Drink"),
    LIVING("سكن ومعيشة", "Living & Home"),
    TRANSPORT("مواصلات وسفر", "Transport"),
    SHOPPING("تسوق وشخصي", "Shopping"),
    BILLS("فواتير وتقنية", "Bills & Tech"),
    HEALTH("صحة ورعاية", "Health"),
    EDUCATION_WORK("تعليم وعمل", "Work & Study"),
    LEISURE("ترفيه ورياضة", "Leisure"),
    FAMILY("عائلة وأطفال", "Family"),
    OTHER("أيقونات عامة", "General")
}

data class IconItem(
    val key: String,
    val nameAr: String,
    val nameEn: String,
    val group: IconCategoryGroup,
    val icon: ImageVector
)

object IconLibrary {
    val icons: List<IconItem> = listOf(
        // Finance & Income
        IconItem("wallet", "محفظة", "Wallet", IconCategoryGroup.FINANCE, Icons.Outlined.AccountBalanceWallet),
        IconItem("money", "نقود", "Money", IconCategoryGroup.FINANCE, Icons.Outlined.AttachMoney),
        IconItem("coins", "عملات", "Coins", IconCategoryGroup.FINANCE, Icons.Outlined.MonetizationOn),
        IconItem("bank", "بنك", "Bank", IconCategoryGroup.FINANCE, Icons.Outlined.AccountBalance),
        IconItem("payments", "مدفوعات", "Payments", IconCategoryGroup.FINANCE, Icons.Outlined.Payments),
        IconItem("savings", "ادخار وتوفير", "Savings", IconCategoryGroup.FINANCE, Icons.Outlined.Savings),
        IconItem("trending_up", "أرباح ونمو", "Profit", IconCategoryGroup.FINANCE, Icons.Outlined.TrendingUp),
        IconItem("trending_down", "خسائر", "Loss", IconCategoryGroup.FINANCE, Icons.Outlined.TrendingDown),
        IconItem("paid", "تم الدفع", "Paid", IconCategoryGroup.FINANCE, Icons.Outlined.Paid),
        IconItem("quote", "فاتورة مستحقة", "Quote", IconCategoryGroup.FINANCE, Icons.Outlined.RequestQuote),

        // Food & Drinks
        IconItem("fastfood", "وجبات سريعة", "Fast Food", IconCategoryGroup.FOOD, Icons.Outlined.Fastfood),
        IconItem("restaurant", "مطعم", "Restaurant", IconCategoryGroup.FOOD, Icons.Outlined.Restaurant),
        IconItem("cafe", "قهوة ومقهى", "Coffee & Cafe", IconCategoryGroup.FOOD, Icons.Outlined.LocalCafe),
        IconItem("pizza", "بيتزا", "Pizza", IconCategoryGroup.FOOD, Icons.Outlined.LocalPizza),
        IconItem("bakery", "مخبوزات وحلويات", "Bakery", IconCategoryGroup.FOOD, Icons.Outlined.BakeryDining),
        IconItem("lunch", "غداء", "Lunch", IconCategoryGroup.FOOD, Icons.Outlined.LunchDining),
        IconItem("dinner", "عشاء فاخر", "Dinner", IconCategoryGroup.FOOD, Icons.Outlined.DinnerDining),
        IconItem("grocery", "بقالة", "Groceries", IconCategoryGroup.FOOD, Icons.Outlined.LocalGroceryStore),
        IconItem("ice_cream", "آيس كريم ومثلجات", "Ice Cream", IconCategoryGroup.FOOD, Icons.Outlined.Icecream),
        IconItem("bar", "مشروبات", "Beverages", IconCategoryGroup.FOOD, Icons.Outlined.LocalBar),

        // Living & Home
        IconItem("home", "منزل", "Home", IconCategoryGroup.LIVING, Icons.Outlined.Home),
        IconItem("apartment", "شقة سكنية", "Apartment", IconCategoryGroup.LIVING, Icons.Outlined.Apartment),
        IconItem("cottage", "شاليه أو فيلا", "Cottage", IconCategoryGroup.LIVING, Icons.Outlined.Cottage),
        IconItem("couch", "أثاث ومفروشات", "Furniture", IconCategoryGroup.LIVING, Icons.Outlined.Weekend),
        IconItem("bed", "غرفة نوم / فندق", "Bed / Hotel", IconCategoryGroup.LIVING, Icons.Outlined.Bed),
        IconItem("kitchen", "أجهزة منزلية", "Appliances", IconCategoryGroup.LIVING, Icons.Outlined.Kitchen),
        IconItem("lightbulb", "إضاءة وكهرباء", "Lighting", IconCategoryGroup.LIVING, Icons.Outlined.Lightbulb),
        IconItem("build", "صيانة وإصلاحات", "Maintenance", IconCategoryGroup.LIVING, Icons.Outlined.Build),
        IconItem("plumbing", "سباكة ومياه", "Plumbing", IconCategoryGroup.LIVING, Icons.Outlined.Plumbing),
        IconItem("cleaning", "نظافة وتنظيف", "Cleaning", IconCategoryGroup.LIVING, Icons.Outlined.CleaningServices),
        IconItem("yard", "حديقة وزراعة", "Garden", IconCategoryGroup.LIVING, Icons.Outlined.Yard),

        // Transport & Travel
        IconItem("car", "سيارة خاصة", "Car", IconCategoryGroup.TRANSPORT, Icons.Outlined.DirectionsCar),
        IconItem("fuel", "بنزين ووقود", "Gas Station", IconCategoryGroup.TRANSPORT, Icons.Outlined.LocalGasStation),
        IconItem("bus", "حافلة وباص", "Bus", IconCategoryGroup.TRANSPORT, Icons.Outlined.DirectionsBus),
        IconItem("taxi", "تاكسي وتوصيل", "Taxi", IconCategoryGroup.TRANSPORT, Icons.Outlined.LocalTaxi),
        IconItem("commute", "تنقلات يومية", "Commute", IconCategoryGroup.TRANSPORT, Icons.Outlined.Commute),
        IconItem("flight", "طيران وسفر", "Flight", IconCategoryGroup.TRANSPORT, Icons.Outlined.Flight),
        IconItem("train", "قطار ومترو", "Train", IconCategoryGroup.TRANSPORT, Icons.Outlined.Train),
        IconItem("motorcycle", "دراجة نارية", "Motorcycle", IconCategoryGroup.TRANSPORT, Icons.Outlined.TwoWheeler),
        IconItem("bike", "دراجة هوائية", "Bicycle", IconCategoryGroup.TRANSPORT, Icons.Outlined.DirectionsBike),
        IconItem("boat", "قارب وبحرية", "Boat", IconCategoryGroup.TRANSPORT, Icons.Outlined.DirectionsBoat),
        IconItem("car_repair", "صيانة سيارات", "Car Repair", IconCategoryGroup.TRANSPORT, Icons.Outlined.CarRepair),

        // Shopping & Personal
        IconItem("shopping_bag", "حقيبة تسوق", "Shopping Bag", IconCategoryGroup.SHOPPING, Icons.Outlined.ShoppingBag),
        IconItem("shopping_cart", "عربة تسوق", "Shopping Cart", IconCategoryGroup.SHOPPING, Icons.Outlined.ShoppingCart),
        IconItem("clothes", "ملابس وأزياء", "Clothing", IconCategoryGroup.SHOPPING, Icons.Outlined.Checkroom),
        IconItem("store", "متاجر ومولات", "Store", IconCategoryGroup.SHOPPING, Icons.Outlined.Store),
        IconItem("jewelry", "مجوهرات وذهب", "Jewelry", IconCategoryGroup.SHOPPING, Icons.Outlined.Diamond),
        IconItem("gift", "هدايا ومناسبات", "Gifts", IconCategoryGroup.SHOPPING, Icons.Outlined.CardGiftcard),
        IconItem("watch", "ساعات وإكسسوارات", "Watch", IconCategoryGroup.SHOPPING, Icons.Outlined.Watch),
        IconItem("salon", "صالون وحلاقة", "Barber / Salon", IconCategoryGroup.SHOPPING, Icons.Outlined.ContentCut),
        IconItem("spa", "عناية وسبا", "Spa & Care", IconCategoryGroup.SHOPPING, Icons.Outlined.Spa),

        // Bills & Tech
        IconItem("receipt", "فاتورة", "Receipt", IconCategoryGroup.BILLS, Icons.Outlined.Receipt),
        IconItem("receipt_long", "فواتير مفصلة", "Bill Details", IconCategoryGroup.BILLS, Icons.Outlined.ReceiptLong),
        IconItem("electricity", "فاتورة كهرباء", "Electricity", IconCategoryGroup.BILLS, Icons.Outlined.Bolt),
        IconItem("water", "فاتورة مياه", "Water Bill", IconCategoryGroup.BILLS, Icons.Outlined.WaterDrop),
        IconItem("wifi", "إنترنت وشبكات", "Internet & Wifi", IconCategoryGroup.BILLS, Icons.Outlined.Wifi),
        IconItem("phone", "هاتف ورصيد", "Phone & Telecom", IconCategoryGroup.BILLS, Icons.Outlined.PhoneAndroid),
        IconItem("tv", "تلفزيون واشتراكات", "TV & Streaming", IconCategoryGroup.BILLS, Icons.Outlined.Tv),
        IconItem("credit_card", "بطاقة بنكية", "Card & Subscriptions", IconCategoryGroup.BILLS, Icons.Outlined.CreditCard),
        IconItem("gadgets", "أجهزة وإلكترونيات", "Devices", IconCategoryGroup.BILLS, Icons.Outlined.Devices),
        IconItem("pos", "نقاط بيع", "POS", IconCategoryGroup.BILLS, Icons.Outlined.PointOfSale),

        // Health & Medical
        IconItem("hospital", "مستشفى وعيادة", "Hospital", IconCategoryGroup.HEALTH, Icons.Outlined.LocalHospital),
        IconItem("doctor", "كشف واستشارة طبية", "Doctor", IconCategoryGroup.HEALTH, Icons.Outlined.MedicalServices),
        IconItem("medicine", "أدوية وصيدلية", "Pharmacy", IconCategoryGroup.HEALTH, Icons.Outlined.Medication),
        IconItem("healing", "علاج وتأهيل", "Healing", IconCategoryGroup.HEALTH, Icons.Outlined.Healing),
        IconItem("fitness", "جيم ولياقة بدنية", "Gym & Fitness", IconCategoryGroup.HEALTH, Icons.Outlined.FitnessCenter),
        IconItem("safety", "تأمين صحي", "Health Insurance", IconCategoryGroup.HEALTH, Icons.Outlined.HealthAndSafety),
        IconItem("vaccine", "تحاليل وتطعيمات", "Vaccine & Labs", IconCategoryGroup.HEALTH, Icons.Outlined.Vaccines),
        IconItem("psychology", "صحة نفسية", "Therapy", IconCategoryGroup.HEALTH, Icons.Outlined.Psychology),

        // Education & Work
        IconItem("school", "مدارس وجامعات", "School & College", IconCategoryGroup.EDUCATION_WORK, Icons.Outlined.School),
        IconItem("book", "كتب ومراجع", "Books & Study", IconCategoryGroup.EDUCATION_WORK, Icons.Outlined.MenuBook),
        IconItem("work", "حقيبة عمل", "Work Briefcase", IconCategoryGroup.EDUCATION_WORK, Icons.Outlined.Work),
        IconItem("business", "أعمال واستشارات", "Business", IconCategoryGroup.EDUCATION_WORK, Icons.Outlined.BusinessCenter),
        IconItem("laptop", "عمل عن بعد وحاسوب", "Laptop / Remote", IconCategoryGroup.EDUCATION_WORK, Icons.Outlined.Laptop),
        IconItem("calc", "حسابات وضرائب", "Calculations", IconCategoryGroup.EDUCATION_WORK, Icons.Outlined.Calculate),
        IconItem("stories", "دورات تدريبية", "Courses", IconCategoryGroup.EDUCATION_WORK, Icons.Outlined.AutoStories),
        IconItem("science", "بحوث وتجارب", "Research", IconCategoryGroup.EDUCATION_WORK, Icons.Outlined.Science),
        IconItem("task", "مهام ومشاريع", "Projects", IconCategoryGroup.EDUCATION_WORK, Icons.Outlined.Assignment),

        // Leisure & Sports
        IconItem("games", "ألعاب وفيديو جيمز", "Video Games", IconCategoryGroup.LEISURE, Icons.Outlined.SportsEsports),
        IconItem("movie", "سينما وأفلام", "Cinema & Movies", IconCategoryGroup.LEISURE, Icons.Outlined.Movie),
        IconItem("theater", "مسرح وفعاليات", "Theater & Events", IconCategoryGroup.LEISURE, Icons.Outlined.TheaterComedy),
        IconItem("music", "موسيقى وحفلات", "Music", IconCategoryGroup.LEISURE, Icons.Outlined.MusicNote),
        IconItem("art", "فنون ورسم", "Art & Painting", IconCategoryGroup.LEISURE, Icons.Outlined.Palette),
        IconItem("camera", "تصوير ومعدات", "Photography", IconCategoryGroup.LEISURE, Icons.Outlined.CameraAlt),
        IconItem("soccer", "كرة قدم ورياضة", "Soccer & Sports", IconCategoryGroup.LEISURE, Icons.Outlined.SportsSoccer),
        IconItem("basketball", "كرة سلة", "Basketball", IconCategoryGroup.LEISURE, Icons.Outlined.SportsBasketball),
        IconItem("trophy", "جوائز وبطولات", "Competitions", IconCategoryGroup.LEISURE, Icons.Outlined.EmojiEvents),

        // Family & Kids
        IconItem("baby", "رعاية أطفال ومواليد", "Baby Care", IconCategoryGroup.FAMILY, Icons.Outlined.ChildCare),
        IconItem("family", "مصروفات عائلية", "Family", IconCategoryGroup.FAMILY, Icons.Outlined.FamilyRestroom),
        IconItem("pets", "حيوانات أليفة وبيطرة", "Pets", IconCategoryGroup.FAMILY, Icons.Outlined.Pets),
        IconItem("toys", "ألعاب أطفال", "Toys", IconCategoryGroup.FAMILY, Icons.Outlined.Toys),

        // Other & General
        IconItem("category", "تصنيف عام", "General Category", IconCategoryGroup.OTHER, Icons.Outlined.Category),
        IconItem("star", "مميز ومهم", "Star / Priority", IconCategoryGroup.OTHER, Icons.Outlined.Star),
        IconItem("heart", "تبرعات وخير", "Charity / Donations", IconCategoryGroup.OTHER, Icons.Outlined.FavoriteBorder),
        IconItem("flag", "أهداف وخطط", "Goals", IconCategoryGroup.OTHER, Icons.Outlined.Flag),
        IconItem("tag", "وسم خاص", "Custom Tag", IconCategoryGroup.OTHER, Icons.Outlined.Tag),
        IconItem("pin", "مثبت", "Pinned", IconCategoryGroup.OTHER, Icons.Outlined.PushPin),
        IconItem("more", "أخرى ومتنوع", "Other", IconCategoryGroup.OTHER, Icons.Outlined.MoreHoriz)
    )

    private val iconMap: Map<String, IconItem> by lazy {
        icons.associateBy { it.key }
    }

    fun getIconByKey(key: String): ImageVector {
        return iconMap[key]?.icon ?: Icons.Outlined.MoreHoriz
    }

    fun getIconItemByKey(key: String): IconItem? {
        return iconMap[key]
    }

    fun findKeyByIcon(icon: ImageVector): String {
        return icons.firstOrNull { it.icon == icon }?.key ?: "more"
    }
}
