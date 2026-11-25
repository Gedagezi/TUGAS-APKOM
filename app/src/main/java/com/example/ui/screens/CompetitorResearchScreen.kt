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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.model.CompetitorResearch
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
fun CompetitorResearchScreen(
    savedResearchState: UiState<List<CompetitorResearch>>,
    draftState: UiState<CompetitorResearch?>,
    onBack: () -> Unit,
    onRunResearch: (brand: String, username: String, platform: String, observations: String, brandContext: String, forceRefresh: Boolean) -> Unit,
    onSaveResearch: (CompetitorResearch) -> Unit,
    modifier: Modifier = Modifier,
    cacheNotice: String? = null
) {
    var brandName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("Instagram") }
    var manualObservations by remember { mutableStateOf("") }
    var yourBrandContext by remember { mutableStateOf("") }

    val platforms = listOf("Instagram", "TikTok", "YouTube Shorts", "X")

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
                text = "AI Competitor Research",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SegmaNavy
            )
        }

        // Data transparency card per Section 5.D & Workflow 4
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SegmaLime.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, SegmaBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = SegmaNavy, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SEGMA.AI respects platform privacy and does not scrape private accounts. Add any publicly observed stats below (optional) for deeper Gemini AI differentiation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SegmaNavy
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SegmaOutlinedCard {
            OutlinedTextField(
                value = brandName,
                onValueChange = { brandName = it },
                label = { Text("Competitor or Brand Name *") },
                modifier = Modifier.fillMaxWidth().testTag("competitor_brand_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Social Media Handle (e.g. @competitor)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text("Platform", style = MaterialTheme.typography.labelLarge, color = SegmaNavy)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                platforms.forEach { p ->
                    val selected = platform == p
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) SegmaNavy else SegmaSurface,
                        border = BorderStroke(1.dp, SegmaBorder),
                        modifier = Modifier.clickable { platform = p }
                    ) {
                        Text(
                            text = p,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) Color.White else SegmaNavy,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = manualObservations,
                onValueChange = { manualObservations = it },
                label = { Text("Observed Public Metrics / Notes (Optional)") },
                placeholder = { Text("e.g. Posts 4x/week, avg 300 likes on carousels, formal tone") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = yourBrandContext,
                onValueChange = { yourBrandContext = it },
                label = { Text("Your Brand Positioning (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    onRunResearch(
                        brandName.trim().ifBlank { "Competitor Brand" },
                        username.trim().ifBlank { "@competitor" },
                        platform,
                        manualObservations.trim(),
                        yourBrandContext.trim(),
                        false
                    )
                },
                enabled = draftState !is UiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = SegmaNavy, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("competitor_analyze_btn")
            ) {
                if (draftState is UiState.Loading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing Positioning...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze Competitor with AI", fontWeight = FontWeight.Bold)
                }
            }

            if (draftState is UiState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(draftState.message, color = SegmaDangerRed, style = MaterialTheme.typography.bodyMedium)
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
                    text = cacheNotice ?: "AI competitor analysis ready",
                    style = MaterialTheme.typography.labelSmall,
                    color = SegmaTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        onRunResearch(
                            brandName.trim().ifBlank { generated.brandName.ifBlank { "Competitor Brand" } },
                            username.trim().ifBlank { generated.username.ifBlank { "@competitor" } },
                            platform,
                            manualObservations.trim(),
                            yourBrandContext.trim(),
                            true
                        )
                    },
                    enabled = draftState !is UiState.Loading,
                    border = BorderStroke(1.dp, SegmaNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("competitor_refresh_btn")
                ) {
                    Icon(Icons.Filled.Refresh, null, tint = SegmaNavy, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh", style = MaterialTheme.typography.labelSmall, color = SegmaNavy)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            CompetitorCard(
                research = generated,
                showSaveButton = true,
                onSave = { onSaveResearch(generated) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Saved Competitor Research",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = SegmaNavy
        )
        Spacer(modifier = Modifier.height(10.dp))

        when (savedResearchState) {
            is UiState.Loading -> CircularProgressIndicator(color = SegmaNavy)
            is UiState.Error -> Text(savedResearchState.message, color = SegmaDangerRed)
            is UiState.Success -> {
                if (savedResearchState.data.isEmpty()) {
                    EmptyStateCard(
                        title = "No saved competitor analyses yet",
                        message = "Run an analysis above and save it to compare differentiation opportunities."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        savedResearchState.data.forEach { item ->
                            CompetitorCard(research = item, showSaveButton = false, onSave = {})
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CompetitorCard(
    research: CompetitorResearch,
    showSaveButton: Boolean,
    onSave: () -> Unit
) {
    SegmaOutlinedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${research.brandName} (${research.username})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SegmaNavy
                )
                Text(
                    text = "${research.platform} • ${research.dataMode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SegmaTextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        ResearchLine("Content Themes", research.contentThemes)
        ResearchLine("Publishing Frequency", research.publishingFrequency)
        ResearchLine("Dominant Formats", research.dominantFormats)
        ResearchLine("Observed Metrics / Transparency", research.observedMetricsSummary)
        ResearchLine("Communication Style", research.communicationStyle)
        ResearchLine("Strengths", research.strengths)
        ResearchLine("Untapped Opportunities", research.opportunities)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SegmaLime.copy(alpha = 0.65f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("AI Differentiation Strategy", style = MaterialTheme.typography.labelLarge, color = SegmaNavy)
                Text(research.differentiationStrategy, style = MaterialTheme.typography.bodyMedium, color = SegmaNavy)
            }
        }

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
                Text("Save Competitor Insight", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResearchLine(label: String, value: String) {
    if (value.isBlank()) return
    Column(modifier = Modifier.padding(bottom = 5.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = SegmaNavy)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = SegmaTextPrimary)
    }
}
