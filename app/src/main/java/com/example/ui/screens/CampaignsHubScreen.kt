package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Campaign
import com.example.model.CampaignStatus
import com.example.model.UiState
import com.example.ui.components.DemoDataBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SegmaOutlinedCard
import com.example.ui.theme.SegmaBackground
import com.example.ui.theme.SegmaBorder
import com.example.ui.theme.SegmaDangerRed
import com.example.ui.theme.SegmaLime
import com.example.ui.theme.SegmaNavy
import com.example.ui.theme.SegmaSuccessGreen
import com.example.ui.theme.SegmaSurface
import com.example.ui.theme.SegmaTextSecondary
import java.util.Locale

@Composable
fun CampaignsHubScreen(
    campaignsState: UiState<List<Campaign>>,
    onOpenCampaignDetail: (String) -> Unit,
    onOpenCampaignBuilder: () -> Unit,
    onOpenAiEvaluation: () -> Unit,
    onRestoreDemoCampaign: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<CampaignStatus?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SegmaBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Campaigns",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = SegmaNavy
                )
                Text(
                    text = "Manage drafts, active runs & analytics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SegmaTextSecondary
                )
            }
            Button(
                onClick = onOpenCampaignBuilder,
                colors = ButtonDefaults.buttonColors(containerColor = SegmaNavy, contentColor = Color.White),
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("campaigns_new_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Builder", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters: List<Pair<CampaignStatus?, String>> = listOf(
                null to "All",
                CampaignStatus.ONGOING to "Ongoing",
                CampaignStatus.SCHEDULED to "Scheduled",
                CampaignStatus.DRAFT to "Draft",
                CampaignStatus.COMPLETED to "Completed"
            )
            filters.forEach { (status, label) ->
                val selected = selectedFilter == status
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) SegmaLime else SegmaSurface,
                    border = BorderStroke(1.dp, SegmaBorder),
                    modifier = Modifier.clickable { selectedFilter = status }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = SegmaNavy,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (campaignsState) {
            is UiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = SegmaNavy)
                }
            }
            is UiState.Error -> {
                EmptyStateCard(
                    title = "Couldn't load campaigns",
                    message = campaignsState.message,
                    actionLabel = "Create Campaign",
                    onAction = onOpenCampaignBuilder
                )
            }
            is UiState.Success -> {
                val filtered = remember(campaignsState.data, selectedFilter) {
                    if (selectedFilter == null) campaignsState.data
                    else campaignsState.data.filter { it.statusEnum == selectedFilter }
                }

                if (filtered.isEmpty()) {
                    EmptyStateCard(
                        title = "No campaigns in this view",
                        message = "Use the AI Campaign Builder to generate a full strategy, or restore a demo campaign to explore analytics.",
                        actionLabel = "Open Campaign Builder",
                        onAction = onOpenCampaignBuilder
                    )
                    if (campaignsState.data.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRestoreDemoCampaign,
                            colors = ButtonDefaults.buttonColors(containerColor = SegmaLime, contentColor = SegmaNavy),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Restore Sample Demo Campaign", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { campaign ->
                            CampaignListCard(
                                campaign = campaign,
                                onClick = { onOpenCampaignDetail(campaign.id) },
                                onEvaluate = onOpenAiEvaluation
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CampaignListCard(
    campaign: Campaign,
    onClick: () -> Unit,
    onEvaluate: () -> Unit
) {
    SegmaOutlinedCard(
        onClick = onClick,
        modifier = Modifier.testTag("campaign_card_${campaign.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = campaign.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SegmaNavy
                )
                Text(
                    text = "${campaign.brandName.ifBlank { "Workspace" }} • ${campaign.platforms.joinToString(", ").ifBlank { "Multi-platform" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SegmaTextSecondary
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = when (campaign.statusEnum) {
                    CampaignStatus.ONGOING -> SegmaLime
                    CampaignStatus.COMPLETED -> SegmaNavy
                    CampaignStatus.SCHEDULED -> Color(0xFFE4E8FB)
                    CampaignStatus.DRAFT -> Color(0xFFECEEF4)
                }
            ) {
                Text(
                    text = campaign.statusEnum.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (campaign.statusEnum == CampaignStatus.COMPLETED) Color.White else SegmaNavy,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = campaign.objective,
            style = MaterialTheme.typography.bodyMedium,
            color = SegmaTextSecondary,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (campaign.reach > 0L) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f%%", campaign.computedEngagementRate)} ER",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = SegmaSuccessGreen
                    )
                    Text(
                        text = "• ${campaign.reach} Reach",
                        style = MaterialTheme.typography.labelSmall,
                        color = SegmaTextSecondary
                    )
                } else {
                    Text(
                        text = "No metrics yet",
                        style = MaterialTheme.typography.labelSmall,
                        color = SegmaTextSecondary
                    )
                }
                if (campaign.demoData) {
                    DemoDataBadge("Demo")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (campaign.statusEnum == CampaignStatus.COMPLETED) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SegmaLime,
                        modifier = Modifier
                            .clickable(onClick = onEvaluate)
                            .padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.FactCheck, null, tint = SegmaNavy, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Evaluate", style = MaterialTheme.typography.labelSmall, color = SegmaNavy)
                        }
                    }
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View details", tint = SegmaNavy)
            }
        }
    }
}
