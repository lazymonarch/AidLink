
package com.aidlink.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aidlink.model.BadgeInfo
import com.aidlink.model.UserProfile
import com.aidlink.model.allBadges
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onNavigateToEdit: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val userProfile by profileViewModel.userProfile.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
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
                item { UserInfoSection(userProfile = userProfile!!, onNavigateToEdit = onNavigateToEdit) }

                item {
                    StatsSection(
                        helps = userProfile!!.helpsCompleted,
                        requests = userProfile!!.requestsPosted
                    )
                }

                if (userProfile!!.trustBadges.isNotEmpty()) {
                    item {
                        TrustBadgesSection(badges = userProfile!!.trustBadges)
                    }
                }

                item {
                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        HorizontalDivider()
                        ActionItem(
                            icon = Icons.Default.Settings,
                            text = "Settings",
                            onClick = onNavigateToSettings
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ActionItem(icon = Icons.AutoMirrored.Filled.HelpOutline, text = "Help & Support", onClick = { /* TODO */ })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ActionItem(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            text = "Log Out",
                            color = MaterialTheme.colorScheme.error,
                            onClick = { profileViewModel.onLogoutClicked() }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserInfoSection(userProfile: UserProfile, onNavigateToEdit: () -> Unit) {
    Spacer(modifier = Modifier.height(16.dp))
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(userProfile.photoUrl)
            .crossfade(true)
            .build(),
        contentDescription = "Profile Picture",
        modifier = Modifier
            .size(90.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = userProfile.name,
        style = MaterialTheme.typography.headlineSmall
    )
    Spacer(modifier = Modifier.height(4.dp))
    TextButton(onClick = onNavigateToEdit) {
        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "Edit Profile")
    }
    Spacer(modifier = Modifier.height(16.dp))
    if (userProfile.bio.isNotBlank()) {
        Text(
            text = userProfile.bio,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        userProfile.skills.forEach { skill ->
            SuggestionChip(onClick = {}, label = { Text(skill) })
        }
    }
}

@Composable
private fun StatsSection(helps: Int, requests: Int) {
    Column(Modifier.padding(top = 24.dp)) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(value = helps.toString(), label = "Helps")
            StatItem(value = requests.toString(), label = "Requests")
        }
        HorizontalDivider()
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge)
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrustBadgesSection(badges: Map<String, Int>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "Trust Badges",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 12.dp)
        )

        val sortedBadges = badges.toList().sortedByDescending { it.second }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            sortedBadges.forEach { (badgeId, count) ->
                val badgeInfo = allBadges.find { it.id == badgeId }
                if (badgeInfo != null) {
                    BadgeChip(badgeInfo = badgeInfo, count = count)
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(badgeInfo: BadgeInfo, count: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = badgeInfo.icon,
                contentDescription = badgeInfo.text,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = badgeInfo.text,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "x$count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionItem(icon: ImageVector, text: String, color: Color = Color.Unspecified, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = color)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = color, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    AidLinkTheme {
        ProfileScreen(
            profileViewModel = viewModel(),
            onNavigateToEdit = {},
            onNavigateToSettings = {}
        )
    }
}
