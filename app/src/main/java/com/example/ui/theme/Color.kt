package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Modern Corporate Blue (Light Mode Palette)
val LightBackground = Color(0xFFF8FAFC)        // Off-white 60%
val LightSurface = Color(0xFFFFFFFF)           // Pure white cards
val LightSurfaceVariant = Color(0xFFF1F5F9)    // Subtle slate tint
val LightOutline = Color(0xFFE2E8F0)           // Clean card border
val LightOutlineSubtle = Color(0xFFF1F5F9)     // Soft dividers

val LightPrimary = Color(0xFF2563EB)           // Royal Blue brand
val LightPrimaryContainer = Color(0xFFDBEAFE)  // Soft sky blue container
val LightOnPrimaryContainer = Color(0xFF1E40AF)
val LightSecondary = Color(0xFF06B6D4)         // Cyan accent
val LightSecondaryContainer = Color(0xFFCFFAFE)
val LightOnSecondaryContainer = Color(0xFF0E7490)

val LightTextPrimary = Color(0xFF0F172A)       // Dark slate
val LightTextSecondary = Color(0xFF64748B)     // Muted gray
val LightTextMuted = Color(0xFF94A3B8)         // Cool gray

val LightSuccess = Color(0xFF16A34A)           // Semantic Success
val LightWarning = Color(0xFFCA8A04)           // Semantic Warning
val LightError = Color(0xFFDC2626)             // Semantic Error

// Sleek Midnight Dark Mode Palette
val DarkBackground = Color(0xFF0B0F19)         // Deep navy/black 60%
val DarkSurface = Color(0xFF1E293B)            // Dark charcoal cards
val DarkSurfaceVariant = Color(0xFF27354A)     // Elevated charcoal
val DarkOutline = Color(0xFF334155)            // Sleek card border
val DarkOutlineSubtle = Color(0xFF1E293B)

val DarkPrimary = Color(0xFF6366F1)            // Electric Indigo
val DarkPrimaryContainer = Color(0xFF312E81)
val DarkOnPrimaryContainer = Color(0xFFE0E7FF)
val DarkSecondary = Color(0xFF10B981)          // Neon Teal
val DarkSecondaryContainer = Color(0xFF064E3B)
val DarkOnSecondaryContainer = Color(0xFFA7F3D0)

val DarkTextPrimary = Color(0xFFF1F5F9)        // Off-white primary text
val DarkTextSecondary = Color(0xFF94A3B8)      // Cool gray secondary text & icons
val DarkTextMuted = Color(0xFF64748B)          // Muted label gray

val DarkSuccess = Color(0xFF22C55E)            // Semantic Success
val DarkWarning = Color(0xFFF59E0B)            // Semantic Warning
val DarkError = Color(0xFFEF4444)              // Semantic Error

// Semantic State & Chart Accents
val MintEmerald = Color(0xFF10B981)
val SunsetAmber = Color(0xFFF59E0B)
val RoseRed = Color(0xFFEF4444)
val ElectricCyan = Color(0xFF06B6D4)
val VibrantIndigo = Color(0xFF6366F1)
val RoyalBlue = Color(0xFF2563EB)
val NeonTeal = Color(0xFF10B981)

// Backward compatible tokens mapped to current theme mode
val PolishBackground get() = LightBackground
val PolishSurface get() = LightSurface
val PolishSurfaceVariant get() = LightSurfaceVariant
val PolishOutline get() = LightOutline
val PolishOutlineSubtle get() = LightOutlineSubtle
val PolishPrimary get() = LightPrimary
val PolishPrimaryContainer get() = LightPrimaryContainer
val PolishOnPrimaryContainer get() = LightOnPrimaryContainer
val PolishMediumRose get() = LightSecondary
val PolishLightRose get() = LightSecondaryContainer
val PolishAiCallout get() = LightPrimaryContainer
val PolishRecoContainer get() = LightPrimaryContainer
val PolishOnReco get() = LightOnPrimaryContainer
val PolishRecoActive get() = LightSecondary
val PolishTextPrimary get() = LightTextPrimary
val PolishTextSecondary get() = LightTextSecondary
val PolishTextMuted get() = LightTextSecondary
val PolishWineDark get() = LightTextPrimary

// Category Specific Colors
val ColorSocial = Color(0xFF6366F1)          // Electric Indigo
val ColorEntertainment = Color(0xFF8B5CF6)   // Purple
val ColorProductivity = Color(0xFF10B981)    // Neon Teal
val ColorShopping = Color(0xFFF59E0B)        // Amber
val ColorFinance = Color(0xFF06B6D4)         // Cyan
val ColorCommunication = Color(0xFF2563EB)   // Royal Blue
val ColorUtilities = Color(0xFF64748B)       // Slate Gray
val ColorHealth = Color(0xFF14B8A6)          // Teal
val ColorGames = Color(0xFFEF4444)           // Crimson
