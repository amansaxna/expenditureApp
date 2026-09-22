package com.example.myexpenditureapp.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Obsidian & Slate Backgrounds (Tailored Premium OLED Dark Mode - Anti-Slop)
val ObsidianDark = Color(0xFF05080F)       // Deep pitch black base
val ObsidianSurface = Color(0xFF0A101D)    // Primary container surface
val ObsidianCard = Color(0xFF0F1728)       // Elevated card surface
val ObsidianCardElevated = Color(0xFF172238)// Higher elevation / modal sheet
val ObsidianBorder = Color(0xFF1E2C44)     // Crisp 1px hairline border
val ObsidianBorderSubtle = Color(0xFF131D2E)// Muted secondary border

// Alpine Light Backgrounds
val AlpineBackground = Color(0xFFF8FAFC)
val AlpineSurface = Color(0xFFFFFFFF)
val AlpineCard = Color(0xFFF1F5F9)
val AlpineCardElevated = Color(0xFFE2E8F0)
val AlpineBorder = Color(0xFFCBD5E1)

// Primary Indigo & Violet Accents
val IndigoPrimary = Color(0xFF6366F1)
val VioletPrimary = Color(0xFF8B5CF6)
val BlueAccent = Color(0xFF38BDF8)
val CyanAccent = Color(0xFF22D3EE)

// Financial Semantic Accents
val IncomeGreen = Color(0xFF10B981)
val IncomeGreenLight = Color(0xFF34D399)
val IncomeGreenDark = Color(0xFF065F46)

val ExpenseRed = Color(0xFFF43F5E)
val ExpenseRedLight = Color(0xFFFB7185)
val ExpenseRedDark = Color(0xFFBE123C)

val AmberWarning = Color(0xFFF59E0B)
val GoldAccent = Color(0xFFFBBF24)
val PurpleAccent = Color(0xFFA855F7)

// Text & Neutral Colors
val TextWhite = Color(0xFFF8FAFC)
val TextGrayLight = Color(0xFF94A3B8)
val TextGrayMuted = Color(0xFF64748B)
val TextDark = Color(0xFF0F172A)
val TextDarkMuted = Color(0xFF475569)

// Gradients
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(IndigoPrimary, VioletPrimary)
)

val HeroCardGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0E1726), Color(0xFF070C15), Color(0xFF0A192F))
)

val HeroCardDarkLuminousGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF131F37), Color(0xFF0B1220), Color(0xFF061A23))
)

val IncomeGradient = Brush.horizontalGradient(
    colors = listOf(IncomeGreen, IncomeGreenLight)
)

val ExpenseGradient = Brush.horizontalGradient(
    colors = listOf(ExpenseRed, ExpenseRedLight)
)

val GoldGradient = Brush.horizontalGradient(
    colors = listOf(GoldAccent, AmberWarning)
)

// Legacy alias mappings for backward compatibility
val NavyDeep = ObsidianDark
val NavyLight = ObsidianSurface
val NavyLighter = ObsidianCard
val NavyAccent = ObsidianCardElevated
val AccentVibrant = IndigoPrimary
val AccentVibrantGradient = VioletPrimary
val AccentSuccess = IncomeGreen
val AccentWarning = AmberWarning
val AccentError = ExpenseRed
val Gold = GoldAccent
val GoldDark = AmberWarning
val Slate = TextGrayLight
val SlateLight = AlpineBorder
val OffWhite = AlpineBackground
val Purple80 = IndigoPrimary
val PurpleGrey80 = Slate
val Pink80 = GoldAccent
val Purple40 = ObsidianDark
val PurpleGrey40 = Slate
val Pink40 = AmberWarning

