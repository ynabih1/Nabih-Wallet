package com.nabih.wallet.ui.theme

import androidx.compose.ui.graphics.Color

data class CategoryPaletteTone(
    val key: String,
    val lightAccent: Color,
    val darkAccent: Color,
    val pdfArgb: Int
)

object CategoryColors {

    val all: List<CategoryPaletteTone> = listOf(
        CategoryPaletteTone("amber", Color(0xFFB45309), Color(0xFFFBBF24), 0xFFB45309.toInt()),
        CategoryPaletteTone("orange", Color(0xFFC2410C), Color(0xFFFB923C), 0xFFC2410C.toInt()),
        CategoryPaletteTone("teal", Color(0xFF0F766E), Color(0xFF2DD4BF), 0xFF0F766E.toInt()),
        CategoryPaletteTone("indigo", Color(0xFF3730A3), Color(0xFF818CF8), 0xFF3730A3.toInt()),
        CategoryPaletteTone("brown", Color(0xFF6D4C41), Color(0xFFBCAAA4), 0xFF6D4C41.toInt()),
        CategoryPaletteTone("blue", Color(0xFF1D4ED8), Color(0xFF60A5FA), 0xFF1D4ED8.toInt()),
        CategoryPaletteTone("purple", Color(0xFF7E22CE), Color(0xFFC084FC), 0xFF7E22CE.toInt()),
        CategoryPaletteTone("emerald", Color(0xFF047857), Color(0xFF34D399), 0xFF047857.toInt()),
        CategoryPaletteTone("pink", Color(0xFFBE185D), Color(0xFFF472B6), 0xFFBE185D.toInt()),
        CategoryPaletteTone("violet", Color(0xFF6D28D9), Color(0xFFA78BFA), 0xFF6D28D9.toInt()),
        CategoryPaletteTone("cyan", Color(0xFF0E7490), Color(0xFF22D3EE), 0xFF0E7490.toInt()),
        CategoryPaletteTone("rose", Color(0xFFE11D48), Color(0xFFFB7185), 0xFFE11D48.toInt()),
        CategoryPaletteTone("lime", Color(0xFF4D7C0F), Color(0xFFA3E635), 0xFF4D7C0F.toInt()),
        CategoryPaletteTone("red", Color(0xFFB91C1C), Color(0xFFF87171), 0xFFB91C1C.toInt()),
        CategoryPaletteTone("slate", Color(0xFF475569), Color(0xFF94A3B8), 0xFF475569.toInt()),
        CategoryPaletteTone("gray", Color(0xFF52525B), Color(0xFFA1A1AA), 0xFF52525B.toInt())
    )

    private val byKey: Map<String, CategoryPaletteTone> = all.associateBy { it.key }

    fun accentColor(colorKey: String, isDark: Boolean): Color {
        val tone = byKey[colorKey] ?: byKey.getValue("blue")
        return if (isDark) tone.darkAccent else tone.lightAccent
    }

    fun containerColor(colorKey: String, isDark: Boolean): Color {
        val accent = accentColor(colorKey, isDark)
        return accent.copy(alpha = if (isDark) 0.24f else 0.15f)
    }

    fun pdfArgb(colorKey: String): Int {
        return (byKey[colorKey] ?: byKey.getValue("blue")).pdfArgb
    }
}
