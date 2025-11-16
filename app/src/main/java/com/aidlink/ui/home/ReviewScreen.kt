
package com.aidlink.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aidlink.model.BadgeInfo
import com.aidlink.model.Review
import com.aidlink.model.helperBadges
import com.aidlink.model.requesterBadges
import com.aidlink.viewmodel.ReviewUiState
import com.aidlink.viewmodel.ReviewViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReviewScreen(
    requestId: String,
    revieweeId: String,
    isHelperReviewing: Boolean,
    onReviewSubmitted: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    var overallExperience by remember { mutableStateOf<String?>(null) }
    var selectedBadges by remember { mutableStateOf(setOf<String>()) }
    val uiState by viewModel.uiState.collectAsState()
    val currentUser = Firebase.auth.currentUser

    // Fetch reviewee profile and request details
    LaunchedEffect(revieweeId, requestId) {
        viewModel.fetchRevieweeProfile(revieweeId)
        viewModel.fetchRequest(requestId)
    }
    val revieweeProfile by viewModel.revieweeProfile.collectAsState()
    val currentRequest by viewModel.currentRequest.collectAsState()


    val badgesToShow = if (isHelperReviewing) requesterBadges else helperBadges

    // Handle successful submission
    LaunchedEffect(uiState) {
        if (uiState is ReviewUiState.Success) {
            onReviewSubmitted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leave Feedback", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onReviewSubmitted) { // Navigates back if review is cancelled
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        val review = Review(
                            requestId = requestId,
                            reviewerId = currentUser?.uid ?: "",
                            revieweeId = revieweeId,
                            overall = overallExperience!!,
                            badges = selectedBadges.toList()
                        )
                        viewModel.submitReview(review)
                    },
                    enabled = overallExperience != null && uiState !is ReviewUiState.Submitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState is ReviewUiState.Submitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Submit Feedback", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // --- Context Header (User Profile & Request) ---
            revieweeProfile?.let { profile ->
                currentRequest?.let { request ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(profile.photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Reviewee Profile Picture",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Feedback for",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                profile.name,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Request: ${request.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp)) // More space after header

            // --- Private Feedback Section ---
            Text(
                "Private Feedback",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "This will not be shared publicly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                "How was your experience?",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp) // Space between buttons
            ) {
                FeedbackButton(
                    icon = Icons.Default.ThumbUp,
                    text = "Good",
                    isSelected = overallExperience == "positive",
                    onClick = { overallExperience = "positive" },
                    modifier = Modifier.weight(1f)
                )
                FeedbackButton(
                    icon = Icons.Default.ThumbDown,
                    text = "Bad",
                    isSelected = overallExperience == "negative",
                    onClick = { overallExperience = "negative" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(32.dp))

            // --- Public "Trust Badges" Section ---
            Text(
                "Public \"Trust Badges\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Recognize their strengths. (Optional, select up to 3)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                badgesToShow.forEach { badge ->
                    BadgeSelectionChip(
                        badgeInfo = badge,
                        isSelected = badge.id in selectedBadges,
                        onToggleSelection = {
                            val newBadges = selectedBadges.toMutableSet()
                            if (it.id in newBadges) {
                                newBadges.remove(it.id)
                            } else if (newBadges.size < 3) {
                                newBadges.add(it.id)
                            }
                            selectedBadges = newBadges
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FeedbackButton(
    icon: ImageVector,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Use rememberRipple()
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(text = text, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        }
    }
}

@Composable
fun BadgeSelectionChip(
    badgeInfo: BadgeInfo,
    isSelected: Boolean,
    onToggleSelection: (BadgeInfo) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, // Use rememberRipple()
            onClick = { onToggleSelection(badgeInfo) }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = badgeInfo.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = badgeInfo.text,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}
