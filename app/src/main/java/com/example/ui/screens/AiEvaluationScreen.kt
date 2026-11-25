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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.model.CampaignEvaluation
import com.example.model.CampaignStatus
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SegmaOutlinedCard
import com.example.ui.theme.SegmaBackground
import com.example.ui.theme.SegmaBorder
import com.example.ui.theme.SegmaDangerRed
import com.example.ui.theme.SegmaLime
import com.example.ui.theme.SegmaNavy
import com.example.ui.theme.SegmaSurface
import com.example.ui.theme.SegmaTextPrimary
import com.example.ui.theme.SegmaTextSecondary

@Composable
fun AiEvaluationScreen(
    campaigns: List<Campaign>,
    evaluationsState: UiState<List<CampaignEvaluation>>,
    draftState: UiState<CampaignEvaluation?>,
    onBack: () -> Unit,
    onEvaluateCampaign: (Campaign, Boolean) -> Unit,
    onSaveEvaluation: (CampaignEvaluation) -> Unit,
    modifier: Modifier = Modifier,
    cacheNotice: String? = null
) {
    val completedFirst = remember(campaigns) {
        campaigns.sortedByDescending { it.statusEnum == CampaignStatus.COMPLETED }
    }
    var selectedCampaignId by remember(completedFirst) {
        mutableStateOf(completedFirst.firstOrNull()?.id.orEmpty())
    }
    val selectedCampaign = completedFirst.firstOrNull { it.id == selectedCampaignId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SegmaBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SegmaNavy)
            }
            Text(
                text = "AI Campaign Evaluation",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SegmaNavy
            )
        }

        if (completedFirst.isEmpty()) {
            EmptyStateCard(
                title = "No campaigns available to evaluate",
                message = "Create or complete a campaign first, then run an AI evaluation report here."
            )
        } else {
            SegmaOutlinedCard {
                Text(
                    text = "Select Campaign to Evaluate",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SegmaNavy
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    completedFirst.forEach { camp ->
                        val active = camp.id == selectedCampaignId
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (active) SegmaNavy else SegmaSurface,
                            border = BorderStroke(1.dp, SegmaBorder),
                            modifier = Modifier.clickable { selectedCampaignId = camp.id }
                        ) {
                            Text(
                                text = "${camp.name} (${camp.statusEnum.label})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (active) Color.White else SegmaNavy,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { selectedCampaign?.let { onEvaluateCampaign(it, false) } },
                    enabled = selectedCampaign != null && draftState !is UiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = SegmaLime, contentColor = SegmaNavy),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("run_ai_evaluation_btn")
                ) {
                    if (draftState is UiState.Loading) {
                        CircularProgressIndicator(color = SegmaNavy, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Evaluating Performance...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Evaluation Report", fontWeight = FontWeight.Bold)
                    }
                }

                if (draftState is UiState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(draftState.message, color = SegmaDangerRed, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        val generated = (draftState as? UiState.Success)?.data
        if (generated != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cacheNotice ?: "Generated evaluation report",
                    style = MaterialTheme.typography.labelSmall,
                    color = SegmaTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                if (selectedCampaign != null) {
                    OutlinedButton(
                        onClick = { onEvaluateCampaign(selectedCampaign, true) },
                        enabled = draftState !is UiState.Loading,
                        border = BorderStroke(1.dp, SegmaNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("eval_regenerate_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, null, tint = SegmaNavy, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Regenerate", style = MaterialTheme.typography.labelSmall, color = SegmaNavy)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            EvaluationReportCard(
                report = generated,
                showSaveButton = true,
                onSave = { onSaveEvaluation(generated) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Evaluation Report History",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = SegmaNavy
        )
        Spacer(modifier = Modifier.height(10.dp))

        when (evaluationsState) {
            is UiState.Loading -> CircularProgressIndicator(color = SegmaNavy)
            is UiState.Error -> Text(evaluationsState.message, color = SegmaDangerRed)
            is UiState.Success -> {
                if (evaluationsState.data.isEmpty()) {
                    EmptyStateCard(
                        title = "No saved evaluation reports yet",
                        message = "Generate an AI evaluation above and save it to build your campaign archive."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        evaluationsState.data.forEach { report ->
                            EvaluationReportCard(report = report, showSaveButton = false, onSave = {})
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EvaluationReportCard(
    report: CampaignEvaluation,
    showSaveButton: Boolean,
    onSave: () -> Unit
) {
    SegmaOutlinedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = report.campaignName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SegmaNavy,
                modifier = Modifier.weight(1f)
            )
            Surface(shape = RoundedCornerShape(50), color = SegmaLime) {
                Text(
                    text = "Score: ${report.overallScore}/100",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = SegmaNavy,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        ReportSection("Execution Summary", report.executionSummary)
        ReportSection("Objective Achievement", report.objectiveAchievement)
        ReportSection("Target vs Actual", report.targetVsActual)
        ReportSection("Engagement Analysis", report.engagementAnalysis)
        ReportSection("Content Effectiveness", report.contentEffectiveness)
        ReportSection("Strengths", report.strengths)
        ReportSection("Weaknesses", report.weaknesses)
        ReportSection("Influencing Factors", report.influencingFactors)
        ReportSection("Future Recommendations", report.futureRecommendations)

        if (showSaveButton) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = SegmaNavy, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Evaluation Report", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReportSection(title: String, body: String) {
    if (body.isBlank()) return
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = SegmaNavy)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = SegmaTextPrimary)
    }
}
