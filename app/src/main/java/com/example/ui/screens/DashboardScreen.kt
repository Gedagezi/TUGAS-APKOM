package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SegmaDemoCatalog
import com.example.model.Campaign
import com.example.model.CampaignStatus
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.DemoDataBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.SegmaBackground
import com.example.ui.theme.SegmaBorder
import com.example.ui.theme.SegmaLime
import com.example.ui.theme.SegmaLimeBright
import com.example.ui.theme.SegmaNavy
import com.example.ui.theme.SegmaSuccessGreen
import com.example.ui.theme.SegmaSurface
import com.example.ui.theme.SegmaTextPrimary
import com.example.ui.theme.SegmaTextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userProfileState: UiState<UserProfile?>,
    campaignsState: UiState<List<Campaign>>,
    onPromptToAgent: (String) -> Unit,
    onOpenCampaignDetail: (String) -> Unit,
    onOpenCampaignBuilder: () -> Unit,
    onOpenOngoingCampaigns: () -> Unit,
    onOpenAudienceProfiling: () -> Unit,
    onOpenAiEvaluation: () -> Unit,
    onOpenCompetitorResearch: () -> Unit,
    onOpenWhatsTrending: (String) -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (userProfileState as? UiState.Success)?.data
    val firstName = profile?.displayName?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Creator"
    val campaigns = (campaignsState as? UiState.Success)?.data.orEmpty()
    val featuredCampaigns = remember(campaigns) {
        val ongoing = campaigns.filter { it.statusEnum == CampaignStatus.ONGOING }
        if (ongoing.isNotEmpty()) ongoing else campaigns
    }

    var agentIdeaInput by remember { mutableStateOf("") }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showQuickMenuSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SegmaBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Lime -> Off-White Gradient Header matching PDF Page 2
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SegmaLime, SegmaLime.copy(alpha = 0.45f), SegmaBackground)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column {
                // Top Bar: Avatar + Greeting + Bell + Hamburger Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onOpenProfile() }
                            .padding(end = 8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_user_avatar),
                            contentDescription = "User profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, SegmaNavy, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Hi, $firstName",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SegmaNavy
                            )
                            Text(
                                text = profile?.activeHandle?.ifBlank { "@segmacreator" } ?: "@segmacreator",
                                style = MaterialTheme.typography.labelSmall,
                                color = SegmaTextSecondary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showNotificationsSheet = true },
                            modifier = Modifier.testTag("dashboard_notifications_btn")
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = SegmaNavy, contentColor = Color.White) {
                                        Text("${SegmaDemoCatalog.notifications.size}")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = "Notifications",
                                    tint = SegmaNavy
                                )
                            }
                        }
                        IconButton(
                            onClick = { showQuickMenuSheet = true },
                            modifier = Modifier.testTag("dashboard_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Quick Menu",
                                tint = SegmaNavy
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Start your Campaign")
                        }
                        append(" with Agent assistance!")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = SegmaNavy,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Agent Prompt Input Pill matching PDF Page 2
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SegmaSurface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (agentIdeaInput.isEmpty()) {
                                Text(
                                    text = "Turn your idea into a strategy...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SegmaTextSecondary
                                )
                            }
                            BasicTextField(
                                value = agentIdeaInput,
                                onValueChange = { agentIdeaInput = it },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = SegmaTextPrimary),
                                singleLine = true,
                                cursorBrush = SolidColor(SegmaNavy),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dashboard_agent_input")
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SegmaLime)
                                .clickable {
                                    val query = agentIdeaInput.trim().ifBlank {
                                        "Help me plan an engaging social media campaign for my brand."
                                    }
                                    agentIdeaInput = ""
                                    onPromptToAgent(query)
                                }
                                .testTag("dashboard_agent_send_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Send to Segma Agent",
                                tint = SegmaNavy,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // Ongoing Campaigns Stacked Carousel Card
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            if (campaignsState is UiState.Loading) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SegmaSurface,
                    border = BorderStroke(1.dp, SegmaBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Loading campaigns...", color = SegmaTextSecondary)
                    }
                }
            } else if (featuredCampaigns.isEmpty()) {
                EmptyStateCard(
                    title = "No Active Campaigns Yet",
                    message = "Create a campaign with Segma AI or use the Campaign Builder to launch your first strategy.",
                    actionLabel = "Create Campaign",
                    onAction = onOpenCampaignBuilder
                )
            } else {
                val pagerState = rememberPagerState(pageCount = { featuredCampaigns.size })
                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 12.dp,
                    contentPadding = PaddingValues(0.dp)
                ) { page ->
                    val campaign = featuredCampaigns[page]
                    DashboardCampaignHeroCard(
                        campaign = campaign,
                        onCheckDetails = { onOpenCampaignDetail(campaign.id) }
                    )
                }
                if (featuredCampaigns.size > 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    PagerDotsRow(
                        count = featuredCampaigns.size,
                        currentIndex = pagerState.currentPage,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // WHAT'S TRENDING Section
            Text(
                text = "WHAT'S TRENDING",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                ),
                color = SegmaNavy
            )
            Spacer(modifier = Modifier.height(10.dp))

            val editorials = SegmaDemoCatalog.trendingEditorials
            val trendingPager = rememberPagerState(pageCount = { editorials.size })
            HorizontalPager(
                state = trendingPager,
                pageSpacing = 12.dp
            ) { idx ->
                val item = editorials[idx]
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SegmaNavy,
                    border = BorderStroke(2.dp, SegmaLime),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clickable { onOpenWhatsTrending(item.id) }
                        .testTag("trending_banner_${item.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.badgeTitle,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 22.sp
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Demo Snapshot • Tap to read →",
                                style = MaterialTheme.typography.labelSmall,
                                color = SegmaLime
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Image(
                            painter = painterResource(id = R.drawable.img_music_viral_collage),
                            contentDescription = item.headline,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(136.dp)
                                .height(98.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            PagerDotsRow(
                count = editorials.size,
                currentIndex = trendingPager.currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(22.dp))

            // EXPLORE MORE FEATURES 2x2 Grid matching PDF Page 2
            Text(
                text = "EXPLORE MORE FEATURES",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                ),
                color = SegmaNavy
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureGridCard(
                    title = "Audience\nProfiling",
                    icon = Icons.Filled.Groups,
                    isLime = true,
                    onClick = onOpenAudienceProfiling,
                    testTag = "feature_audience_profiling",
                    modifier = Modifier.weight(1f)
                )
                FeatureGridCard(
                    title = "Ongoing\nCampaigns",
                    icon = Icons.Filled.TrackChanges,
                    isLime = false,
                    onClick = onOpenOngoingCampaigns,
                    testTag = "feature_ongoing_campaigns",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureGridCard(
                    title = "AI\nEvaluation",
                    icon = Icons.AutoMirrored.Filled.FactCheck,
                    isLime = false,
                    onClick = onOpenAiEvaluation,
                    testTag = "feature_ai_evaluation",
                    modifier = Modifier.weight(1f)
                )
                FeatureGridCard(
                    title = "AI Competitor\nResearch",
                    icon = Icons.Filled.Search,
                    isLime = true,
                    onClick = onOpenCompetitorResearch,
                    testTag = "feature_competitor_research",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showNotificationsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            containerColor = SegmaSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Notifications & Early Warnings",
                    style = MaterialTheme.typography.titleLarge,
                    color = SegmaNavy
                )
                SegmaDemoCatalog.notifications.forEach { n ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (n.severity == "OPPORTUNITY") SegmaLime.copy(alpha = 0.35f) else Color(0xFFF3F4FA),
                        border = BorderStroke(1.dp, SegmaBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = n.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SegmaNavy,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = n.timestampLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SegmaTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = n.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SegmaTextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showQuickMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickMenuSheet = false },
            containerColor = SegmaSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SEGMA.AI Workspace Tools",
                    style = MaterialTheme.typography.titleLarge,
                    color = SegmaNavy
                )
                QuickMenuRow("Campaign Builder (AI Planner)", Icons.Filled.Add) {
                    showQuickMenuSheet = false
                    onOpenCampaignBuilder()
                }
                QuickMenuRow("Ongoing & Saved Campaigns", Icons.Filled.TrackChanges) {
                    showQuickMenuSheet = false
                    onOpenOngoingCampaigns()
                }
                QuickMenuRow("AI Campaign Evaluation", Icons.AutoMirrored.Filled.FactCheck) {
                    showQuickMenuSheet = false
                    onOpenAiEvaluation()
                }
                QuickMenuRow("AI Competitor Research", Icons.Filled.Analytics) {
                    showQuickMenuSheet = false
                    onOpenCompetitorResearch()
                }
                QuickMenuRow("Audience Persona Profiling", Icons.Filled.Groups) {
                    showQuickMenuSheet = false
                    onOpenAudienceProfiling()
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun DashboardCampaignHeroCard(
    campaign: Campaign,
    onCheckDetails: () -> Unit
) {
    // Stacked card look matching PDF Page 2
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SegmaSurface,
            border = BorderStroke(1.dp, SegmaBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .padding(top = 12.dp)
                .height(176.dp)
        ) {}
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SegmaSurface,
            border = BorderStroke(1.2.dp, SegmaBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.15f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = campaign.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SegmaNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            color = SegmaLime,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (campaign.statusEnum == CampaignStatus.ONGOING) "Live" else campaign.statusEnum.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = SegmaNavy,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = campaign.handle.ifBlank { "@segma" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = SegmaTextSecondary
                        )
                        if (campaign.demoData) {
                            DemoDataBadge("Demo")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val erFormatted = String.format(Locale.US, "%.1f%%", campaign.computedEngagementRate)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$erFormatted ↑",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = SegmaSuccessGreen
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.ThumbUp,
                            contentDescription = "High Engagement",
                            tint = Color(0xFFF5B731),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = if (campaign.computedEngagementRate >= 4.0) "HIGH ENGAGEMENT" else "ACTIVE TRACKING",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SegmaNavy
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SegmaLime,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onCheckDetails)
                            .testTag("dashboard_check_detail_${campaign.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Check Details",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = SegmaNavy
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = SegmaNavy,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Image(
                    painter = painterResource(id = R.drawable.img_campaign_cover_eco),
                    contentDescription = campaign.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(114.dp)
                        .height(152.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }
        }
    }
}

@Composable
private fun FeatureGridCard(
    title: String,
    icon: ImageVector,
    isLime: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val bg = if (isLime) SegmaLimeBright else SegmaNavy
    val fg = if (isLime) SegmaNavy else Color.White
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bg,
        modifier = modifier
            .height(106.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp
                ),
                color = fg,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = fg,
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun PagerDotsRow(
    count: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { idx ->
            Box(
                modifier = Modifier
                    .size(if (idx == currentIndex) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (idx == currentIndex) SegmaNavy else Color(0xFFBDC2D6))
            )
        }
    }
}

@Composable
private fun QuickMenuRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF4F5FA),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = SegmaNavy)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = SegmaNavy
            )
        }
    }
}
