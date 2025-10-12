package com.aidlink.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aidlink.model.Review
import com.aidlink.model.UserProfile
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.ProfileViewModel
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel
) {
    val userProfile by profileViewModel.userProfile.collectAsState()

    val reviews = listOf(
        Review(reviewerId = "1", reviewerName = "Sophia Bennett", rating = 5, comment = "Ethan was incredibly helpful..."),
        Review(reviewerId = "2", reviewerName = "Liam Harper", rating = 4, comment = "Ethan helped me with some minor home repairs...")
    )

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (userProfile == null) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                item { UserInfoSection(userProfile = userProfile!!) }

                // FIXED: The StatsSection has been removed to resolve errors
                // item { StatsSection(helps = helpsCount, requests = requestsCount, rating = 4.8) }

                item {
                    Text(
                        text = "Reviews",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                    )
                }
                items(reviews) { review ->
                    ReviewCard(review = review, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                item {
                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        HorizontalDivider(color = Color.DarkGray)
                        ActionItem(icon = Icons.Default.Settings, text = "Settings", onClick = { /* TODO */ })
                        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(horizontal = 16.dp))
                        ActionItem(icon = Icons.AutoMirrored.Filled.HelpOutline, text = "Help & Support", onClick = { /* TODO */ })
                        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(horizontal = 16.dp))
                        ActionItem(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            text = "Log Out",
                            color = Color.Red,
                            onClick = { profileViewModel.onLogoutClicked() }
                        )
                        HorizontalDivider(color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserInfoSection(userProfile: UserProfile) {
    Spacer(modifier = Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .size(90.dp)
            .clip(CircleShape)
            .background(Color.DarkGray)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = userProfile.name,
        color = Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { /* TODO: Navigate to Edit Profile */ }
    ) {
        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "Edit Profile", color = Color.Gray)
    }
    Spacer(modifier = Modifier.height(16.dp))
    if (userProfile.bio.isNotBlank()) {
        Text(
            text = userProfile.bio,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        userProfile.skills.forEach { skill ->
            Surface(shape = RoundedCornerShape(50), color = Color.DarkGray) {
                Text(
                    text = skill,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
@Composable
private fun StatsSection(helps: Int, requests: Int, rating: Double) {
    Column(Modifier.padding(top = 24.dp)) {
        HorizontalDivider(color = Color.DarkGray)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(value = helps.toString(), label = "Helps")
            StatItem(value = requests.toString(), label = "Requests")
            StatItem(value = rating.toString(), label = "Rating", hasStar = true)
        }
        HorizontalDivider(color = Color.DarkGray)
    }
}

@Composable
private fun StatItem(value: String, label: String, hasStar: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (hasStar) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color.Yellow, modifier = Modifier.size(18.dp))
            }
        }
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
    }
}

// --- THIS IS THE CORRECTED REVIEW CARD ---
@Composable
private fun ReviewCard(review: Review, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.DarkGray))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = review.reviewerName, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(text = formatReviewTimestamp(review.createdAt), color = Color.Gray, fontSize = 12.sp)
                }
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (index < review.rating) Color.Yellow else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = review.comment, color = Color.LightGray)
        }
    }
}
// --- END CORRECTION ---

@Composable
private fun ActionItem(icon: ImageVector, text: String, color: Color = Color.White, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = color)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = color, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

// --- NEW HELPER FUNCTION TO FORMAT TIME ---
private fun formatReviewTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return "Just now" // Fallback for preview or fresh reviews
    val diff = Timestamp.now().seconds - timestamp.seconds
    val days = TimeUnit.SECONDS.toDays(diff)
    if (days > 365) return "${days / 365}y ago"
    if (days > 30) return "${days / 30}m ago"
    if (days > 7) return "${days / 7}w ago"
    if (days > 0) return "${days}d ago"
    val hours = TimeUnit.SECONDS.toHours(diff)
    if (hours > 0) return "${hours}h ago"
    val minutes = TimeUnit.SECONDS.toMinutes(diff)
    if (minutes > 0) return "${minutes}min ago"
    return "Just now"
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    AidLinkTheme(darkTheme = true) {
        ProfileScreen(profileViewModel = viewModel())
    }
}