package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Campaign
import com.example.model.CampaignStatus
import com.example.model.UiState
import com.example.ui.components.DemoDataBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.EngagementFormulaDialog
import com.example.ui.components.MetricGradientCard
import com.example.ui.components.SegmaOutlinedCard
import com.example.ui.components.SegmaSparkBadge
import com.example.ui.components.TrendLineChartCanvas
import com.example.ui.theme.SegmaBackground
import com.example.ui.theme.SegmaBorder
import com.example.ui.theme.SegmaDangerRed
import com.example.ui.theme.SegmaLime
import com.example.ui.theme.SegmaNavy
import com.example.ui.theme.SegmaSuccessGreen
import com.example.ui.theme.SegmaSurface
import com.example.ui.theme.SegmaTextPrimary
import com.example.ui.theme.SegmaTextSecondary
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CampaignDetailScreen(
    campaign: Campaign?,
    aiInsightState: UiState<String?>,
    followUpState: UiState<String?>,
    onBack: () -> Unit,
    onReviseStrategy: (Campaign) -> Unit,
    onUpdateStatus: (String, CampaignStatus) -> Unit,
    onUpdateMetrics: (Campaign, Long, Long, Long, Long, Long, Long) -> Unit,
    onRefreshAiAnalysis: (Campaign, Boolean) -> Unit,
    onRequestFollowUp: (Campaign, Boolean) -> Unit,
    onClearFollowUp: () -> Unit,
    onOpenEvaluation: () -> Unit,
    onDeleteCampaign: (String) -> Unit,
    modifier: Modifier = Modifier,
    insightCacheNotice: String? = null,
    followUpCacheNotice: String? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showFormulaDialog by remember { mutableStateOf(false) }
    var showMetricsDialog by remember { mutableStateOf(false) }
    var showStrategyDetails by remember { mutableStateOf(false) }

    val nf = remember { NumberFormat.getNumberInstance(Locale.US) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SegmaBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Header matching PDF Page 5
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SegmaNavy)
            }
            Text(
                text = "Campaign Detail",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SegmaNavy
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreHoriz, contentDescription = "Campaign options", tint = SegmaNavy)
                }
                if (campaign != null) {
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        CampaignStatus.entries.forEach { st ->
                            DropdownMenuItem(
                                text = { Text("Mark as ${st.label}") },
                                onClick = {
                                    showMenu = false
                                    onUpdateStatus(campaign.id, st)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Log / Edit Metrics") },
                            onClick = {
                                showMenu = false
                                showMetricsDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open AI Evaluation") },
                            onClick = {
                                showMenu = false
                                onOpenEvaluation()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Campaign", color = SegmaDangerRed) },
                            onClick = {
                                showMenu = false
                                onDeleteCampaign(campaign.id)
                            }
                        )
                    }
                }
            }
        }

        if (campaign == null) {
            EmptyStateCard(
                title = "Campaign not found",
                message = "This campaign may have been deleted or hasn't finished syncing.",
                actionLabel = "Go Back",
                onAction = onBack
            )
            return
        }

        // Top Summary Card matching PDF Page 5
        SegmaOutlinedCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = campaign.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = SegmaNavy,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(shape = RoundedCornerShape(50), color = SegmaLime) {
                            Text(
                                text = if (campaign.statusEnum == CampaignStatus.ONGOING) "Live" else campaign.statusEnum.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SegmaNavy,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "${campaign.handle.ifBlank { "@segma" }} • ${campaign.platforms.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SegmaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val erFormatted = String.format(Locale.US, "%.1f%%", campaign.computedEngagementRate)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$erFormatted ↑",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = SegmaSuccessGreen
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.ThumbUp,
                            contentDescription = null,
                            tint = Color(0xFFF5B731),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showFormulaDialog = true }
                    ) {
                        Text(
                            text = if (campaign.computedEngagementRate >= 4.0) "HIGH ENGAGEMENT" else "ENGAGEMENT RATE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SegmaNavy
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "View ER formula",
                            tint = SegmaNavy,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Image(
                    painter = painterResource(id = R.drawable.img_campaign_cover_eco),
                    contentDescription = campaign.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(114.dp)
                        .height(142.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DemoDataBadge(campaign.dataSourceLabel.ifBlank { if (campaign.demoData) "Demo Data" else "Manual Metrics" })
                Text(
                    text = if (showStrategyDetails) "Hide Plan ▲" else "Full Plan ▼",
                    style = MaterialTheme.typography.labelLarge,
                    color = SegmaNavy,
                    modifier = Modifier.clickable { showStrategyDetails = !showStrategyDetails }
                )
            }
        }

        if (showStrategyDetails) {
            Spacer(modifier = Modifier.height(12.dp))
            SegmaOutlinedCard(containerColor = Color(0xFFF7F8FD)) {
                Text("Campaign Strategy & Copy", style = MaterialTheme.typography.titleMedium, color = SegmaNavy)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Period: ${campaign.duration.ifBlank { "Flexible" }}", style = MaterialTheme.typography.labelLarge, color = SegmaTextSecondary)
                Text("Objective: ${campaign.objective}", style = MaterialTheme.typography.bodyMedium)
                if (campaign.aiCommunicationStrategy.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Angle: ${campaign.aiCommunicationStrategy}", style = MaterialTheme.typography.bodyMedium)
                }
                if (campaign.aiUploadStrategy.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Upload Strategy: ${campaign.aiUploadStrategy}", style = MaterialTheme.typography.bodyMedium)
                }
                if (campaign.aiCaptions.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Copywriting:\n${campaign.aiCaptions}", style = MaterialTheme.typography.bodyMedium)
                }
                if (campaign.aiHashtags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(campaign.aiHashtags.joinToString(" "), style = MaterialTheme.typography.labelLarge, color = SegmaNavy)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Target Audience Card matching PDF Page 5
        SegmaOutlinedCard {
            Text(
                text = "Selected Target Audience",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SegmaNavy,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(10.dp))
            val tags = campaign.audienceTags.ifEmpty {
                listOf(campaign.targetAudience.take(28).ifBlank { "General Digital Audience" })
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEachIndexed { index, tag ->
                    val isNavyPill = index % 2 == 1
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isNavyPill) SegmaNavy else SegmaLime
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isNavyPill) Color.White else SegmaNavy,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2x2 Metric Grid matching PDF Page 5
        val targetDelta = if (campaign.targetReach > 0L && campaign.reach > 0L) {
            val pct = (((campaign.reach - campaign.targetReach).toDouble() / campaign.targetReach.toDouble()) * 100.0).toInt()
            if (pct >= 0) "(+$pct% vs target)" else "($pct% vs target)"
        } else if (campaign.targetReach > 0L) {
            "Target: ${nf.format(campaign.targetReach)}"
        } else null

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricGradientCard(
                title = "Reach",
                value = nf.format(campaign.reach),
                subtitle = targetDelta,
                isLimeStyle = true,
                modifier = Modifier.weight(1f)
            )
            MetricGradientCard(
                title = "Likes",
                value = nf.format(campaign.likes),
                subtitle = "Views: ${nf.format(campaign.views)}",
                isLimeStyle = false,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricGradientCard(
                title = "Comments",
                value = nf.format(campaign.comments),
                isLimeStyle = false,
                modifier = Modifier.weight(1f)
            )
            MetricGradientCard(
                title = "Share & Saves",
                value = nf.format(campaign.shares + campaign.saves),
                subtitle = "${nf.format(campaign.shares)} shares • ${nf.format(campaign.saves)} saves",
                isLimeStyle = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Target vs Actual Progress Bar when target exists
        if (campaign.targetReach > 0L) {
            Spacer(modifier = Modifier.height(12.dp))
            SegmaOutlinedCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Target vs Actual Reach", style = MaterialTheme.typography.labelLarge, color = SegmaNavy)
                    Text(
                        "${nf.format(campaign.reach)} / ${nf.format(campaign.targetReach)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = SegmaTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (campaign.reach.toFloat() / campaign.targetReach.toFloat()).coerceIn(0f, 1f) },
                    color = SegmaSuccessGreen,
                    trackColor = Color(0xFFE3E6F2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Performance Development Chart matching PDF Page 5
        SegmaOutlinedCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Performance Trend",
                    style = MaterialTheme.typography.titleMedium,
                    color = SegmaNavy
                )
                Text(
                    text = "Log Metrics",
                    style = MaterialTheme.typography.labelLarge,
                    color = SegmaNavy,
                    modifier = Modifier.clickable { showMetricsDialog = true }
                )
            }
            val erVal = campaign.computedEngagementRate
            val chartPts = if (erVal > 0.0) {
                listOf(0.28f, 0.42f, 0.56f, 0.72f, 0.90f)
            } else {
                listOf(0.10f, 0.10f, 0.10f, 0.10f, 0.10f)
            }
            TrendLineChartCanvas(
                points = chartPts,
                endValueLabel = String.format(Locale.US, "%.1f%%", erVal)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Neon Lime Segma AI Recommendations Card matching PDF Page 5
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SegmaLime,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SegmaSparkBadge(
                            size = 32.dp,
                            containerColor = SegmaSurface,
                            iconTint = SegmaNavy
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Segma AI Recommendations",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SegmaNavy
                        )
                    }
                    IconButton(
                        onClick = { onRefreshAiAnalysis(campaign, false) },
                        enabled = aiInsightState !is UiState.Loading,
                        modifier = Modifier.size(34.dp)
                    ) {
                        if (aiInsightState is UiState.Loading) {
                            CircularProgressIndicator(color = SegmaNavy, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh AI analysis", tint = SegmaNavy)
                        }
                    }
                }
                if (!insightCacheNotice.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = insightCacheNotice,
                            style = MaterialTheme.typography.labelSmall,
                            color = SegmaNavy.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Regenerate",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SegmaNavy,
                            modifier = Modifier
                                .clickable(enabled = aiInsightState !is UiState.Loading) {
                                    onRefreshAiAnalysis(campaign, true)
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val displayAnalysis = when (aiInsightState) {
                    is UiState.Success -> aiInsightState.data ?: campaign.aiNextActionRecommendation
                    is UiState.Error -> aiInsightState.message
                    UiState.Loading -> "Analyzing campaign performance..."
                }
                Text(
                    text = displayAnalysis.ifBlank {
                        "Tap refresh to have Segma AI analyze this campaign's metrics and recommend next steps."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SegmaNavy
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dual Action Buttons matching PDF Page 5
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onReviseStrategy(campaign) },
                border = BorderStroke(1.2.dp, SegmaNavy),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("detail_revise_strategy_btn")
            ) {
                Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp), tint = SegmaNavy)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Revise Strategy", color = SegmaNavy, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { onRequestFollowUp(campaign, false) },
                enabled = followUpState !is UiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = SegmaNavy, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("detail_followup_btn")
            ) {
                if (followUpState is UiState.Loading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text("Follow-up", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (campaign.statusEnum == CampaignStatus.COMPLETED) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onOpenEvaluation,
                colors = ButtonDefaults.buttonColors(containerColor = SegmaLime, contentColor = SegmaNavy),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.FactCheck, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Evaluate Completed Campaign with AI", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showFormulaDialog) {
        EngagementFormulaDialog(onDismiss = { showFormulaDialog = false })
    }

    if (showMetricsDialog && campaign != null) {
        UpdateMetricsDialog(
            campaign = campaign,
            onDismiss = { showMetricsDialog = false },
            onConfirm = { reach, views, likes, comments, shares, saves ->
                onUpdateMetrics(campaign, reach, views, likes, comments, shares, saves)
                showMetricsDialog = false
            }
        )
    }

    val followUpText = (followUpState as? UiState.Success)?.data
    val followUpError = (followUpState as? UiState.Error)?.message
    if (!followUpText.isNullOrBlank() || !followUpError.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = onClearFollowUp,
            containerColor = SegmaSurface,
            title = { Text("Follow-up Content Concepts", color = SegmaNavy) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (!followUpCacheNotice.isNullOrBlank()) {
                        Text(
                            text = followUpCacheNotice,
                            style = MaterialTheme.typography.labelSmall,
                            color = SegmaTextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Text(
                        text = followUpText ?: followUpError.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (followUpError != null) SegmaDangerRed else SegmaTextPrimary
                    )
                }
            },
            dismissButton = {
                if (campaign != null && followUpState !is UiState.Loading) {
                    TextButton(onClick = { onRequestFollowUp(campaign, true) }) {
                        Text("Regenerate", color = SegmaNavy)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onClearFollowUp) {
                    Text("Close", color = SegmaNavy, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun UpdateMetricsDialog(
    campaign: Campaign,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Long, Long, Long, Long) -> Unit
) {
    var reach by remember { mutableStateOf(campaign.reach.toString()) }
    var views by remember { mutableStateOf(campaign.views.toString()) }
    var likes by remember { mutableStateOf(campaign.likes.toString()) }
    var comments by remember { mutableStateOf(campaign.comments.toString()) }
    var shares by remember { mutableStateOf(campaign.shares.toString()) }
    var saves by remember { mutableStateOf(campaign.saves.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SegmaSurface,
        title = { Text("Log Campaign Metrics", color = SegmaNavy) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Enter observed metrics from your connected social analytics. ER is computed automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SegmaTextSecondary
                )
                OutlinedTextField(value = reach, onValueChange = { reach = it }, label = { Text("Reach") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = views, onValueChange = { views = it }, label = { Text("Views") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = likes, onValueChange = { likes = it }, label = { Text("Likes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = comments, onValueChange = { comments = it }, label = { Text("Comments") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = shares, onValueChange = { shares = it }, label = { Text("Shares") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = saves, onValueChange = { saves = it }, label = { Text("Saves") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        reach.toLongOrNull() ?: 0L,
                        views.toLongOrNull() ?: 0L,
                        likes.toLongOrNull() ?: 0L,
                        comments.toLongOrNull() ?: 0L,
                        shares.toLongOrNull() ?: 0L,
                        saves.toLongOrNull() ?: 0L
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SegmaNavy)
            ) {
                Text("Save Metrics")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SegmaTextSecondary) }
        }
    )
}
