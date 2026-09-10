package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
        IconItem("wallet", "محفظة", "Wallet", IconCategoryGroup.FINANCE, Icons.Default.AccountBalanceWallet),
        IconItem("money", "نقود", "Money", IconCategoryGroup.FINANCE, Icons.Default.AttachMoney),
        IconItem("coins", "عملات", "Coins", IconCategoryGroup.FINANCE, Icons.Default.MonetizationOn),
        IconItem("bank", "بنك", "Bank", IconCategoryGroup.FINANCE, Icons.Default.AccountBalance),
        IconItem("payments", "مدفوعات", "Payments", IconCategoryGroup.FINANCE, Icons.Default.Payments),
        IconItem("savings", "ادخار وتوفير", "Savings", IconCategoryGroup.FINANCE, Icons.Default.Savings),
        IconItem("trending_up", "أرباح ونمو", "Profit", IconCategoryGroup.FINANCE, Icons.Default.TrendingUp),
        IconItem("trending_down", "خسائر", "Loss", IconCategoryGroup.FINANCE, Icons.Default.TrendingDown),
        IconItem("paid", "تم الدفع", "Paid", IconCategoryGroup.FINANCE, Icons.Default.Paid),
        IconItem("quote", "فاتورة مستحقة", "Quote", IconCategoryGroup.FINANCE, Icons.Default.RequestQuote),

        // Food & Drinks
        IconItem("fastfood", "وجبات سريعة", "Fast Food", IconCategoryGroup.FOOD, Icons.Default.Fastfood),
        IconItem("restaurant", "مطعم", "Restaurant", IconCategoryGroup.FOOD, Icons.Default.Restaurant),
        IconItem("cafe", "قهوة ومقهى", "Coffee & Cafe", IconCategoryGroup.FOOD, Icons.Default.LocalCafe),
        IconItem("pizza", "بيتزا", "Pizza", IconCategoryGroup.FOOD, Icons.Default.LocalPizza),
        IconItem("bakery", "مخبوزات وحلويات", "Bakery", IconCategoryGroup.FOOD, Icons.Default.BakeryDining),
        IconItem("lunch", "غداء", "Lunch", IconCategoryGroup.FOOD, Icons.Default.LunchDining),
        IconItem("dinner", "عشاء فاخر", "Dinner", IconCategoryGroup.FOOD, Icons.Default.DinnerDining),
        IconItem("grocery", "بقالة وسوبرماركت", "Groceries", IconCategoryGroup.FOOD, Icons.Default.LocalGroceryStore),
        IconItem("ice_cream", "آيس كريم ومثلجات", "Ice Cream", IconCategoryGroup.FOOD, Icons.Default.Icecream),
        IconItem("bar", "مشروبات", "Beverages", IconCategoryGroup.FOOD, Icons.Default.LocalBar),

        // Living & Home
        IconItem("home", "منزل", "Home", IconCategoryGroup.LIVING, Icons.Default.Home),
        IconItem("apartment", "شقة سكنية", "Apartment", IconCategoryGroup.LIVING, Icons.Default.Apartment),
        IconItem("cottage", "شاليه أو فيلا", "Cottage", IconCategoryGroup.LIVING, Icons.Default.Cottage),
        IconItem("couch", "أثاث ومفروشات", "Furniture", IconCategoryGroup.LIVING, Icons.Default.Weekend),
        IconItem("bed", "غرفة نوم / فندق", "Bed / Hotel", IconCategoryGroup.LIVING, Icons.Default.Bed),
        IconItem("kitchen", "أجهزة منزلية", "Appliances", IconCategoryGroup.LIVING, Icons.Default.Kitchen),
        IconItem("lightbulb", "إضاءة وكهرباء", "Lighting", IconCategoryGroup.LIVING, Icons.Default.Lightbulb),
        IconItem("build", "صيانة وإصلاحات", "Maintenance", IconCategoryGroup.LIVING, Icons.Default.Build),
        IconItem("plumbing", "سباكة ومياه", "Plumbing", IconCategoryGroup.LIVING, Icons.Default.Plumbing),
        IconItem("cleaning", "نظافة وتنظيف", "Cleaning", IconCategoryGroup.LIVING, Icons.Default.CleaningServices),
        IconItem("yard", "حديقة وزراعة", "Garden", IconCategoryGroup.LIVING, Icons.Default.Yard),

        // Transport & Travel
        IconItem("car", "سيارة خاصة", "Car", IconCategoryGroup.TRANSPORT, Icons.Default.DirectionsCar),
        IconItem("fuel", "بنزين ووقود", "Gas Station", IconCategoryGroup.TRANSPORT, Icons.Default.LocalGasStation),
        IconItem("bus", "حافلة وباص", "Bus", IconCategoryGroup.TRANSPORT, Icons.Default.DirectionsBus),
        IconItem("taxi", "تاكسي وتوصيل", "Taxi", IconCategoryGroup.TRANSPORT, Icons.Default.LocalTaxi),
        IconItem("commute", "تنقلات يومية", "Commute", IconCategoryGroup.TRANSPORT, Icons.Default.Commute),
        IconItem("flight", "طيران وسفر", "Flight", IconCategoryGroup.TRANSPORT, Icons.Default.Flight),
        IconItem("train", "قطار ومترو", "Train", IconCategoryGroup.TRANSPORT, Icons.Default.Train),
        IconItem("motorcycle", "دراجة نارية", "Motorcycle", IconCategoryGroup.TRANSPORT, Icons.Default.TwoWheeler),
        IconItem("bike", "دراجة هوائية", "Bicycle", IconCategoryGroup.TRANSPORT, Icons.Default.DirectionsBike),
        IconItem("boat", "قارب وبحرية", "Boat", IconCategoryGroup.TRANSPORT, Icons.Default.DirectionsBoat),
        IconItem("car_repair", "صيانة سيارات", "Car Repair", IconCategoryGroup.TRANSPORT, Icons.Default.CarRepair),

        // Shopping & Personal
        IconItem("shopping_bag", "حقيبة تسوق", "Shopping Bag", IconCategoryGroup.SHOPPING, Icons.Default.ShoppingBag),
        IconItem("shopping_cart", "عربة تسوق", "Shopping Cart", IconCategoryGroup.SHOPPING, Icons.Default.ShoppingCart),
        IconItem("clothes", "ملابس وأزياء", "Clothing", IconCategoryGroup.SHOPPING, Icons.Default.Checkroom),
        IconItem("store", "متاجر ومولات", "Store", IconCategoryGroup.SHOPPING, Icons.Default.Store),
        IconItem("jewelry", "مجوهرات وذهب", "Jewelry", IconCategoryGroup.SHOPPING, Icons.Default.Diamond),
        IconItem("gift", "هدايا ومناسبات", "Gifts", IconCategoryGroup.SHOPPING, Icons.Default.CardGiftcard),
        IconItem("watch", "ساعات وإكسسوارات", "Watch", IconCategoryGroup.SHOPPING, Icons.Default.Watch),
        IconItem("salon", "صالون وحلاقة", "Barber / Salon", IconCategoryGroup.SHOPPING, Icons.Default.ContentCut),
        IconItem("spa", "عناية وسبا", "Spa & Care", IconCategoryGroup.SHOPPING, Icons.Default.Spa),

        // Bills & Tech
        IconItem("receipt", "فاتورة", "Receipt", IconCategoryGroup.BILLS, Icons.Default.Receipt),
        IconItem("receipt_long", "فواتير مفصلة", "Bill Details", IconCategoryGroup.BILLS, Icons.Default.ReceiptLong),
        IconItem("electricity", "فاتورة كهرباء", "Electricity", IconCategoryGroup.BILLS, Icons.Default.Bolt),
        IconItem("water", "فاتورة مياه", "Water Bill", IconCategoryGroup.BILLS, Icons.Default.WaterDrop),
        IconItem("wifi", "إنترنت وشبكات", "Internet & Wifi", IconCategoryGroup.BILLS, Icons.Default.Wifi),
        IconItem("phone", "هاتف ورصيد", "Phone & Telecom", IconCategoryGroup.BILLS, Icons.Default.PhoneAndroid),
        IconItem("tv", "تلفزيون واشتراكات", "TV & Streaming", IconCategoryGroup.BILLS, Icons.Default.Tv),
        IconItem("credit_card", "بطاقة بنكية", "Card & Subscriptions", IconCategoryGroup.BILLS, Icons.Default.CreditCard),
        IconItem("gadgets", "أجهزة وإلكترونيات", "Devices", IconCategoryGroup.BILLS, Icons.Default.Devices),
        IconItem("pos", "نقاط بيع", "POS", IconCategoryGroup.BILLS, Icons.Default.PointOfSale),

        // Health & Medical
        IconItem("hospital", "مستشفى وعيادة", "Hospital", IconCategoryGroup.HEALTH, Icons.Default.LocalHospital),
        IconItem("doctor", "كشف واستشارة طبية", "Doctor", IconCategoryGroup.HEALTH, Icons.Default.MedicalServices),
        IconItem("medicine", "أدوية وصيدلية", "Pharmacy", IconCategoryGroup.HEALTH, Icons.Default.Medication),
        IconItem("healing", "علاج وتأهيل", "Healing", IconCategoryGroup.HEALTH, Icons.Default.Healing),
        IconItem("fitness", "جيم ولياقة بدنية", "Gym & Fitness", IconCategoryGroup.HEALTH, Icons.Default.FitnessCenter),
        IconItem("safety", "تأمين صحي", "Health Insurance", IconCategoryGroup.HEALTH, Icons.Default.HealthAndSafety),
        IconItem("vaccine", "تحاليل وتطعيمات", "Vaccine & Labs", IconCategoryGroup.HEALTH, Icons.Default.Vaccines),
        IconItem("psychology", "صحة نفسية", "Therapy", IconCategoryGroup.HEALTH, Icons.Default.Psychology),

        // Education & Work
        IconItem("school", "مدارس وجامعات", "School & College", IconCategoryGroup.EDUCATION_WORK, Icons.Default.School),
        IconItem("book", "كتب ومراجع", "Books & Study", IconCategoryGroup.EDUCATION_WORK, Icons.Default.MenuBook),
        IconItem("work", "وظيفة وراتب", "Job / Work", IconCategoryGroup.EDUCATION_WORK, Icons.Default.Work),
        IconItem("business", "أعمال واستشارات", "Business", IconCategoryGroup.EDUCATION_WORK, Icons.Default.BusinessCenter),
        IconItem("laptop", "عمل عن بعد وحاسوب", "Laptop / Remote", IconCategoryGroup.EDUCATION_WORK, Icons.Default.Laptop),
        IconItem("calc", "حسابات وضرائب", "Calculations", IconCategoryGroup.EDUCATION_WORK, Icons.Default.Calculate),
        IconItem("stories", "دورات تدريبية", "Courses", IconCategoryGroup.EDUCATION_WORK, Icons.Default.AutoStories),
        IconItem("science", "بحوث وتجارب", "Research", IconCategoryGroup.EDUCATION_WORK, Icons.Default.Science),
        IconItem("task", "مهام ومشاريع", "Projects", IconCategoryGroup.EDUCATION_WORK, Icons.Default.Assignment),

        // Leisure & Sports
        IconItem("games", "ألعاب وفيديو جيمز", "Video Games", IconCategoryGroup.LEISURE, Icons.Default.SportsEsports),
        IconItem("movie", "سينما وأفلام", "Cinema & Movies", IconCategoryGroup.LEISURE, Icons.Default.Movie),
        IconItem("theater", "مسرح وفعاليات", "Theater & Events", IconCategoryGroup.LEISURE, Icons.Default.TheaterComedy),
        IconItem("music", "موسيقى وحفلات", "Music", IconCategoryGroup.LEISURE, Icons.Default.MusicNote),
        IconItem("art", "فنون ورسم", "Art & Painting", IconCategoryGroup.LEISURE, Icons.Default.Palette),
        IconItem("camera", "تصوير ومعدات", "Photography", IconCategoryGroup.LEISURE, Icons.Default.CameraAlt),
        IconItem("soccer", "كرة قدم ورياضة", "Soccer & Sports", IconCategoryGroup.LEISURE, Icons.Default.SportsSoccer),
        IconItem("basketball", "كرة سلة", "Basketball", IconCategoryGroup.LEISURE, Icons.Default.SportsBasketball),
        IconItem("trophy", "جوائز وبطولات", "Competitions", IconCategoryGroup.LEISURE, Icons.Default.EmojiEvents),

        // Family & Kids
        IconItem("baby", "رعاية أطفال ومواليد", "Baby Care", IconCategoryGroup.FAMILY, Icons.Default.ChildCare),
        IconItem("family", "مصروفات عائلية", "Family", IconCategoryGroup.FAMILY, Icons.Default.FamilyRestroom),
        IconItem("pets", "حيوانات أليفة وبيطرة", "Pets", IconCategoryGroup.FAMILY, Icons.Default.Pets),
        IconItem("toys", "ألعاب أطفال", "Toys", IconCategoryGroup.FAMILY, Icons.Default.Toys),

        // Other & General
        IconItem("category", "تصنيف عام", "General Category", IconCategoryGroup.OTHER, Icons.Default.Category),
        IconItem("star", "مميز ومهم", "Star / Priority", IconCategoryGroup.OTHER, Icons.Default.Star),
        IconItem("heart", "تبرعات وخير", "Charity / Donations", IconCategoryGroup.OTHER, Icons.Default.Favorite),
        IconItem("flag", "أهداف وخطط", "Goals", IconCategoryGroup.OTHER, Icons.Default.Flag),
        IconItem("tag", "وسم خاص", "Custom Tag", IconCategoryGroup.OTHER, Icons.Default.Tag),
        IconItem("pin", "مثبت", "Pinned", IconCategoryGroup.OTHER, Icons.Default.PushPin),
        IconItem("more", "أخرى ومتنوع", "Other", IconCategoryGroup.OTHER, Icons.Default.MoreHoriz)
    )

    private val iconMap: Map<String, IconItem> by lazy {
        icons.associateBy { it.key }
    }

    fun getIconByKey(key: String): ImageVector {
        return iconMap[key]?.icon ?: Icons.Default.MoreHoriz
    }

    fun getIconItemByKey(key: String): IconItem? {
        return iconMap[key]
    }

    fun findKeyByIcon(icon: ImageVector): String {
        return icons.firstOrNull { it.icon == icon }?.key ?: "more"
    }
}
