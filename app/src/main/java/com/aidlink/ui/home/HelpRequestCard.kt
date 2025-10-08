// File: com/aidlink/ui/home/HelpRequestCard.kt
package com.aidlink.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aidlink.model.HelpRequest
import com.aidlink.model.RequestType
import com.aidlink.ui.theme.AidLinkTheme
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.VolunteerActivism

// Recreating colors from your HTML file
private val surfaceDark = Color(0xFF1E1E1E)
private val onSurfaceVariantDark = Color(0xFFC4C7C5)
private val primaryColor = Color(0xFF4DABF7)
private val secondaryColor = Color(0xFFFF8A65)

@Composable
fun HelpRequestCard(request: HelpRequest) {
    Card(
        modifier = Modifier
            .fillMaxWidth() // Sets the width
            .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp)), // Adds a border
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceDark)
    ){
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Tag
            Surface(
                shape = RoundedCornerShape(50),
                color = primaryColor.copy(alpha = 0.2f),
            ) {
                Text(
                    text = request.category,
                    color = primaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Title
            Text(
                text = request.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Description
            Text(
                text = request.description,
                fontSize = 14.sp,
                color = onSurfaceVariantDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Footer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = onSurfaceVariantDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = request.location,
                    fontSize = 14.sp,
                    color = onSurfaceVariantDark
                )
                Text(
                    text = "·",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = onSurfaceVariantDark
                )

                val (icon, text, color) = when(request.type) {
                    RequestType.VOLUNTEER -> Triple(Icons.Default.VolunteerActivism, "Volunteer", primaryColor)
                    RequestType.FEE -> Triple(Icons.Default.Payments, "Fee Range", secondaryColor)
                }
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = text,
                    fontSize = 14.sp,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Preview to check your component
@Preview
@Composable
fun HelpRequestCardPreview() {
    AidLinkTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HelpRequestCard(
                request = HelpRequest("1", "Need help setting up my new router", "The manual is confusing and I can't get the Wi-Fi to connect properly on my laptop.", "Tech Support", "Near Central Park", RequestType.VOLUNTEER)
            )
            HelpRequestCard(
                request = HelpRequest("2", "Leaky faucet needs fixing", "My kitchen sink has been dripping for days and it's starting to get on my nerves.", "Home Repair", "Near Downtown", RequestType.FEE)
            )
        }
    }
}