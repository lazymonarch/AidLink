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
import androidx.compose.material.ripple.rememberRipple
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
                        HorizontalDivider(color = Color.DarkGray)
                        ActionItem(
                            icon = Icons.Default.Settings,
                            text = "Settings",
                            onClick = onNavigateToSettings
                        )
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
            .clip(CircleShape)
            .background(Color.DarkGray),
        contentScale = ContentScale.Crop
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
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(bounded = false),
            onClick = onNavigateToEdit
        )
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

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
private fun StatsSection(helps: Int, requests: Int) {
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
        }
        HorizontalDivider(color = Color.DarkGray)
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
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
            color = Color.White,
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
                    // Custom BadgeChip composable
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF1C1C1E),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = badgeInfo.icon,
                                contentDescription = badgeInfo.text,
                                tint = MaterialTheme.colorScheme.primary, // Use accent color
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = badgeInfo.text,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "x$count",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ActionItem(icon: ImageVector, text: String, color: Color = Color.White, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = color)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = color, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    AidLinkTheme(darkTheme = true) {
        ProfileScreen(
            profileViewModel = viewModel(),
            onNavigateToEdit = {},
            onNavigateToSettings = {}
        )
    }
}