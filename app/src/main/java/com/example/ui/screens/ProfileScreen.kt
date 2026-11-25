package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ai.AiUsageSnapshot
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.signOutUser
import com.example.data.SegmaDemoCatalog
import com.example.model.AudiencePersona
import com.example.model.Campaign
import com.example.model.CampaignStatus
import com.example.model.TrendBookmark
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.theme.SegmaBackground
import com.example.ui.theme.SegmaBorder
import com.example.ui.theme.SegmaDangerRed
import com.example.ui.theme.SegmaLime
import com.example.ui.theme.SegmaNavy
import com.example.ui.theme.SegmaSurface
import com.example.ui.theme.SegmaTextPrimary
import com.example.ui.theme.SegmaTextSecondary
import java.util.Locale

@Composable
fun ProfileScreen(
    userProfileState: UiState<UserProfile?>,
    campaigns: List<Campaign>,
    personas: List<AudiencePersona>,
    bookmarks: List<TrendBookmark>,
    onUpdateProfile: (displayName: String, roleTitle: String, activeHandle: String, secondaryHandle: String) -> Unit,
    onUpdateSocialConnection: (igConnected: Boolean, igHandle: String, ttConnected: Boolean, ttHandle: String) -> Unit,
    onOpenPersonaArchive: () -> Unit,
    onOpenEvaluationHistory: () -> Unit,
    onOpenWhatsTrending: (String) -> Unit,
    modifier: Modifier = Modifier,
    aiUsageSnapshot: AiUsageSnapshot = AiUsageSnapshot()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val profile = (userProfileState as? UiState.Success)?.data ?: UserProfile()
    val completedCount = remember(campaigns) {
        val count = campaigns.count { it.statusEnum == CampaignStatus.COMPLETED }
        if (count > 0) count else 0
    }
    val avgEr = remember(campaigns) {
        val active = campaigns.filter { it.reach > 0L }
        if (active.isEmpty()) 0.0 else active.map { it.computedEngagementRate }.average()
    }

    var handlesExpanded by remember { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf(false) }
    var oauthDialogPlatform by remember { mutableStateOf<String?>(null) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SegmaBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Lime -> Off-White Gradient Header matching PDF Page 6
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SegmaLime, SegmaLime.copy(alpha = 0.4f), SegmaBackground)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_user_avatar),
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.dp, SegmaNavy, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = SegmaNavy
                        )
                        Text(
                            text = profile.roleTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SegmaTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(shape = RoundedCornerShape(50), color = SegmaNavy) {
                            Text(
                                text = "${profile.plan} plan",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.testTag("profile_edit_btn")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Profile", tint = SegmaNavy)
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // Account Handle Switcher Card matching PDF Page 6
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SegmaSurface,
                border = BorderStroke(1.2.dp, SegmaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { handlesExpanded = !handlesExpanded }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.activeHandle.ifBlank { "@eitssampah" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SegmaNavy
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(shape = RoundedCornerShape(50), color = SegmaLime) {
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SegmaNavy,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SegmaLime),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ExpandMore, contentDescription = "Expand handles", tint = SegmaNavy)
                        }
                    }

                    if (handlesExpanded && profile.secondaryHandle.isNotBlank()) {
                        HorizontalDivider(color = SegmaBorder)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateProfile(
                                        profile.displayName,
                                        profile.roleTitle,
                                        profile.secondaryHandle,
                                        profile.activeHandle
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = profile.secondaryHandle,
                                style = MaterialTheme.typography.titleMedium,
                                color = SegmaNavy
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(shape = RoundedCornerShape(50), color = SegmaLime.copy(alpha = 0.6f)) {
                                Text(
                                    text = "Switch",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SegmaNavy,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dark Navy KPI Stats Card matching PDF Page 6
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SegmaNavy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileStatColumn(
                        label = "Completed\nCampaigns",
                        value = "$completedCount",
                        sub = "Archived",
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.width(1.dp).height(58.dp).background(Color.White.copy(alpha = 0.35f)))
                    ProfileStatColumn(
                        label = "Average ER",
                        value = String.format(Locale.US, "%.1f%%", avgEr),
                        sub = if (avgEr >= 4.0) "Status: High" else "Status: Tracking",
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.width(1.dp).height(58.dp).background(Color.White.copy(alpha = 0.35f)))
                    ProfileStatColumn(
                        label = "Saved\nSegments",
                        value = "${personas.size}",
                        sub = "Profiles",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // App-side Local AI Request Budget card (App Usage Estimate only)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SegmaSurface,
                border = BorderStroke(
                    1.2.dp,
                    when {
                        aiUsageSnapshot.isLimitReached -> SegmaDangerRed
                        aiUsageSnapshot.isApproachingLimit -> SegmaNavy
                        else -> SegmaBorder
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_ai_budget_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "App Usage Estimate • AI Budget",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SegmaNavy
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (aiUsageSnapshot.isLimitReached) Color(0xFFFFECEC) else SegmaLime
                        ) {
                            Text(
                                text = "${aiUsageSnapshot.usedIn24h} / ${aiUsageSnapshot.dailyLimit} in 24h",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (aiUsageSnapshot.isLimitReached) SegmaDangerRed else SegmaNavy,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val progress = if (aiUsageSnapshot.dailyLimit > 0) {
                        (aiUsageSnapshot.usedIn24h.toFloat() / aiUsageSnapshot.dailyLimit.toFloat()).coerceIn(0f, 1f)
                    } else 1f
                    LinearProgressIndicator(
                        progress = { progress },
                        color = if (aiUsageSnapshot.isLimitReached) SegmaDangerRed else SegmaNavy,
                        trackColor = Color(0xFFE3E6F2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val statusCopy = when {
                        aiUsageSnapshot.isLimitReached ->
                            "Local 24h AI budget reached. Live AI generation is paused to protect Free Tier quota; saved campaigns, cached insights, and demo data remain available."
                        aiUsageSnapshot.isApproachingLimit ->
                            "Approaching local 24h AI request limit (${aiUsageSnapshot.remaining} remaining). Cached results and saved reports do not use requests."
                        else ->
                            "Local safety guard counts live AI dispatches on this installation (cached/saved results use 0 requests). Note: This is an app estimate, not the official Google API quota."
                    }
                    Text(
                        text = statusCopy,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (aiUsageSnapshot.isLimitReached) SegmaDangerRed else SegmaTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Social Media Connections",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SegmaNavy
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SocialAccountCard(
                    platformName = "Instagram",
                    handle = if (profile.instagramConnected) profile.instagramHandle.ifBlank { "@eitssampah" } else "Not connected",
                    isConnected = profile.instagramConnected,
                    icon = Icons.Filled.CameraAlt,
                    onClick = { oauthDialogPlatform = "Instagram" },
                    testTag = "connect_instagram_btn",
                    modifier = Modifier.weight(1f)
                )
                SocialAccountCard(
                    platformName = "TikTok",
                    handle = if (profile.tiktokConnected) profile.tiktokHandle.ifBlank { "@creator" } else "@username",
                    isConnected = profile.tiktokConnected,
                    icon = Icons.Filled.MusicNote,
                    onClick = { oauthDialogPlatform = "TikTok" },
                    testTag = "connect_tiktok_btn",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Profile Archive & Settings Rows matching PDF Page 6
            ProfileMenuRow("Audience Persona Archive", onClick = onOpenPersonaArchive)
            ProfileMenuRow("AI Evaluation Report History", onClick = onOpenEvaluationHistory)
            ProfileMenuRow("Notifications & Early Warnings", onClick = { showNotificationsDialog = true })
            ProfileMenuRow("Bookmarked Trend Insights (${bookmarks.size})", onClick = { showBookmarksDialog = true })
            ProfileMenuRow("SEGMA.AI Help & User Guide", onClick = { showHelpDialog = true })

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    signOutUser(credentialManager = credentialManager, scope = scope)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SegmaDangerRed, contentColor = Color.White),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(48.dp)
                    .align(Alignment.CenterHorizontally)
                    .testTag("profile_logout_btn")
            ) {
                Text("Log Out", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, role, h1, h2 ->
                onUpdateProfile(name, role, h1, h2)
                showEditDialog = false
            }
        )
    }

    oauthDialogPlatform?.let { plat ->
        val isIg = plat == "Instagram"
        val currentlyConnected = if (isIg) profile.instagramConnected else profile.tiktokConnected
        val currentHandle = if (isIg) profile.instagramHandle else profile.tiktokHandle
        var handleInput by remember(plat) { mutableStateOf(currentHandle.ifBlank { profile.activeHandle }) }

        AlertDialog(
            onDismissRequest = { oauthDialogPlatform = null },
            containerColor = SegmaSurface,
            title = { Text("$plat Official OAuth Integration", color = SegmaNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SEGMA.AI never asks for your social media password. Official OAuth grants read-only analytics permissions (reach, views, engagement) with your consent.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SegmaTextSecondary
                    )
                    if (!currentlyConnected) {
                        OutlinedTextField(
                            value = handleInput,
                            onValueChange = { handleInput = it },
                            label = { Text("$plat Handle (Demo OAuth Link)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "Connected as $currentHandle (Demo OAuth Mode)",
                            style = MaterialTheme.typography.labelLarge,
                            color = SegmaNavy
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isIg) {
                            onUpdateSocialConnection(
                                !currentlyConnected,
                                if (!currentlyConnected) handleInput else "",
                                profile.tiktokConnected,
                                profile.tiktokHandle
                            )
                        } else {
                            onUpdateSocialConnection(
                                profile.instagramConnected,
                                profile.instagramHandle,
                                !currentlyConnected,
                                if (!currentlyConnected) handleInput else ""
                            )
                        }
                        oauthDialogPlatform = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentlyConnected) SegmaDangerRed else SegmaNavy
                    )
                ) {
                    Text(if (currentlyConnected) "Disconnect" else "Authorize & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { oauthDialogPlatform = null }) {
                    Text("Cancel", color = SegmaTextSecondary)
                }
            }
        )
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            containerColor = SegmaSurface,
            title = { Text("Notifications & Early Warnings", color = SegmaNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SegmaDemoCatalog.notifications.forEach { n ->
                        Text("• ${n.title}", style = MaterialTheme.typography.labelLarge, color = SegmaNavy)
                        Text(n.body, style = MaterialTheme.typography.bodyMedium, color = SegmaTextSecondary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) { Text("Close", color = SegmaNavy) }
            }
        )
    }

    if (showBookmarksDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarksDialog = false },
            containerColor = SegmaSurface,
            title = { Text("Bookmarked Trend Insights", color = SegmaNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bookmarks.isEmpty()) {
                        Text("No bookmarked insights yet. Tap the bookmark icon on Trend Radar or What's Trending.", color = SegmaTextSecondary)
                    } else {
                        bookmarks.forEach { bm ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF3F5FC),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showBookmarksDialog = false
                                        onOpenWhatsTrending("editorial_top3_audio")
                                    }
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Bookmark, null, tint = SegmaNavy, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(bm.title, style = MaterialTheme.typography.labelLarge, color = SegmaNavy)
                                        Text(bm.category, style = MaterialTheme.typography.labelSmall, color = SegmaTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarksDialog = false }) { Text("Close", color = SegmaNavy) }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = SegmaSurface,
            title = { Text("SEGMA.AI User Guide", color = SegmaNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Dashboard: Monitor live/demo campaigns and start a strategy chat in one tap.", style = MaterialTheme.typography.bodyMedium)
                    Text("2. Segma Agent: Chat naturally with Gemini AI, edit structured strategy cards, and save campaigns.", style = MaterialTheme.typography.bodyMedium)
                    Text("3. Trend Radar: Filter trends and click 'Create Strategy from This Trend' to launch an AI plan.", style = MaterialTheme.typography.bodyMedium)
                    Text("4. Evaluation & Competitors: Evaluate completed campaigns and differentiate against competitors transparently.", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Got it", color = SegmaNavy) }
            }
        )
    }
}

@Composable
private fun ProfileStatColumn(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
            color = SegmaLime
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFDCE1F5)
        )
    }
}

@Composable
private fun SocialAccountCard(
    platformName: String,
    handle: String,
    isConnected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SegmaSurface,
        border = BorderStroke(1.2.dp, SegmaBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) SegmaLime else SegmaNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = platformName,
                        tint = if (isConnected) SegmaNavy else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(platformName, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = SegmaNavy)
                    Text(handle, style = MaterialTheme.typography.labelSmall, color = SegmaTextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (isConnected) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SegmaLime,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onClick)
                        .testTag(testTag)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = SegmaNavy, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connected", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SegmaNavy)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    border = BorderStroke(1.dp, SegmaNavy),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag(testTag)
                ) {
                    Text("Connect +", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SegmaNavy)
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(title: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        HorizontalDivider(color = Color(0xFFBDC2D6))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = SegmaNavy,
            modifier = Modifier.padding(vertical = 14.dp)
        )
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(profile.displayName) }
    var role by remember { mutableStateOf(profile.roleTitle) }
    var h1 by remember { mutableStateOf(profile.activeHandle) }
    var h2 by remember { mutableStateOf(profile.secondaryHandle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SegmaSurface,
        title = { Text("Edit Profile", color = SegmaNavy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") })
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role / Organization") })
                OutlinedTextField(value = h1, onValueChange = { h1 = it }, label = { Text("Primary Handle") })
                OutlinedTextField(value = h2, onValueChange = { h2 = it }, label = { Text("Secondary Handle") })
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.ifBlank { profile.displayName }, role, h1, h2) },
                colors = ButtonDefaults.buttonColors(containerColor = SegmaNavy)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SegmaTextSecondary) }
        }
    )
}
