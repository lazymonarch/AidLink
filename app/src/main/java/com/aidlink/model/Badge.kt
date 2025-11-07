package com.aidlink.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A central, shared data class for badge information.
 * Accessible throughout the app module.
 */
data class BadgeInfo(
    val id: String,
    val icon: ImageVector,
    val text: String,
    val description: String
)

val helperBadges = listOf(
    BadgeInfo("skilled", Icons.Default.CheckCircle, "Skilled", "Knew what they were doing."),
    BadgeInfo("punctual", Icons.Default.Schedule, "Punctual", "Arrived on time."),
    BadgeInfo("friendly", Icons.Default.Face, "Friendly", "A pleasant interaction."),
    BadgeInfo("communicator", Icons.AutoMirrored.Filled.Chat, "Great Communicator", "Kept me updated."),
    BadgeInfo("above_beyond", Icons.Default.Star, "Above & Beyond", "Exceeded expectations.")
)

val requesterBadges = listOf(
    BadgeInfo("clear_instructions", Icons.AutoMirrored.Filled.ListAlt, "Clear Instructions", "The request was accurate."),
    BadgeInfo("respectful", Icons.Default.ThumbUp, "Respectful", "Was courteous and polite."),
    BadgeInfo("good_communicator", Icons.Default.ChatBubble, "Good Communicator", "Easy to coordinate with."),
    BadgeInfo("prepared", Icons.Default.Home, "Prepared", "The environment was ready.")
)

val allBadges = helperBadges + requesterBadges