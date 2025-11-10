// In: app/src/main/java/com/aidlink/ui/theme/Color.kt
package com.aidlink.ui.theme

import androidx.compose.ui.graphics.Color

// --- Base Light Theme ---
// A clean, slightly off-white for the main background (less harsh than pure white)
val LightBackground = Color(0xFFFCFCFF)
// Pure white for cards, sheets, and primary surfaces
val LightSurface = Color(0xFFFFFFFF)
// A very light gray for subtle divisions or variant surfaces
val LightSurfaceVariant = Color(0xFFF0F3F7)
// A light gray for outlines and borders
val LightOutline = Color(0xFFDDE1E6)

// --- Primary Color (Same as before, works great on light) ---
// A vibrant, trustworthy blue for primary buttons and actions
val PrimaryBlue = Color(0xFF3399FF)
// A very light, subtle blue for containers (e.g., info boxes)
val PrimaryBlueContainer = Color(0xFFDCEBFF)
// A dark, contrasting text color for elements on a primary container
val OnPrimaryBlueContainer = Color(0xFF001D3C)

// --- Text Colors (Light Theme) ---
// Near-black for primary text (better than pure black)
val TextPrimaryLight = Color(0xFF191C20)
// A neutral gray for secondary text and icons
val TextSecondaryLight = Color(0xFF5A6068)
// A lighter gray for placeholders and disabled text
val TextPlaceholderLight = Color(0xFF9CA2AB)

// --- Functional Colors ---
val ErrorRed = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF410002)

val SuccessGreen = Color(0xFF00C853)
val VolunteerBlue = Color(0xFF00658E) // A slightly deeper blue for readability
val FeeGreen = Color(0xFF006D3E) // A darker green for readability