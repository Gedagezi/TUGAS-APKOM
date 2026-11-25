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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.model.GeneratedStrategyDraft
import com.example.model.UiState
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
fun CampaignBuilderScreen(
    existingCampaign: Campaign?,
    draftState: UiState<GeneratedStrategyDraft?>,
    onBack: () -> Unit,
    onGenerateWithAi: (
        name: String,
        brand: String,
        objective: String,
        description: String,
        audience: String,
        platforms: List<String>,
        duration: String,
        budget: String,
        forceRefresh: Boolean
    ) -> Unit,
    onSaveCampaign: (Campaign) -> Unit,
    modifier: Modifier = Modifier,
    cacheNotice: String? = null
) {
    var name by remember(existingCampaign) { mutableStateOf(existingCampaign?.name.orEmpty()) }
    var brandName by remember(existingCampaign) { mutableStateOf(existingCampaign?.brandName.orEmpty()) }
    var handle by remember(existingCampaign) { mutableStateOf(existingCampaign?.handle.orEmpty().ifBlank { "@mybrand" }) }
    var objective by remember(existingCampaign) { mutableStateOf(existingCampaign?.objective.orEmpty()) }
    var description by remember(existingCampaign) { mutableStateOf(existingCampaign?.description.orEmpty()) }
    var targetAudience by remember(existingCampaign) { mutableStateOf(existingCampaign?.targetAudience.orEmpty()) }
    var duration by remember(existingCampaign) { mutableStateOf(existingCampaign?.duration.orEmpty().ifBlank { "14 Days" }) }
    var budget by remember(existingCampaign) { mutableStateOf(existingCampaign?.budget.orEmpty()) }
    var selectedPlatforms by remember(existingCampaign) {
        mutableStateOf(existingCampaign?.platforms?.ifEmpty { listOf("Instagram", "TikTok") } ?: listOf("Instagram", "TikTok"))
    }
    var selectedStatus by remember(existingCampaign) {
        mutableStateOf(existingCampaign?.statusEnum ?: CampaignStatus.DRAFT)
    }

    // Editable AI-generated plan fields
    var aiOverview by remember(existingCampaign) { mutableStateOf(existingCampaign?.aiOverview.orEmpty()) }
    var aiCommStrategy by remember(existingCampaign) { mutableStateOf(existingCampaign?.aiCommunicationStrategy.orEmpty()) }
    var aiPillars by remember(existingCampaign) { mutableStateOf(existingCampaign?.aiContentPillars?.joinToString(", ").orEmpty()) }
    var aiUpload by remember(existingCampaign) { mutableStateOf(existingCampaign?.aiUploadStrategy.orEmpty()) }
    var aiCalendar by remember(existingCampaign) { mutableStateOf(existingCampaign?.aiContentCalendar.orEmpty()) }
    var aiCaptions by remember(existingCampaign) { mutableStateOf(existingCampaign?.aiCaptions.orEmpty()) }
    var aiHashtags by remember(existingCampaign) { mutableStateOf(existingCampaign?.aiHashtags?.joinToString(" ").orEmpty()) }
    var aiAudio by remember(existingCampaign) { mutableStateOf(existingCampaign?.aiAudioRecommendation.orEmpty()) }

    val availablePlatforms = listOf("Instagram", "TikTok", "YouTube Shorts", "LinkedIn", "X")

    LaunchedEffect(draftState) {
        val draft = (draftState as? UiState.Success)?.data
        if (draft != null) {
            if (name.isBlank()) name = draft.campaignName
            if (brandName.isBlank()) brandName = draft.brandName
            aiOverview = draft.communicationAngle
            aiCommStrategy = "${draft.communicationAngle} | Formats: ${draft.recommendedFormats.joinToString(", ")}"
            aiPillars = draft.contentPillars.joinToString(", ")
            aiUpload = draft.uploadStrategy
            aiCalendar = draft.contentCalendar
            aiCaptions = draft.captionsAndHooks
            aiHashtags = draft.hashtags.joinToString(" ")
            aiAudio = draft.audioRecommendation
        }
    }

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
                text = if (existingCampaign == null) "Campaign Builder" else "Edit Campaign Strategy",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SegmaNavy
            )
        }

        SegmaOutlinedCard {
            Text(
                text = "Campaign Brief",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SegmaNavy
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Campaign Name *") },
                modifier = Modifier.fillMaxWidth().testTag("builder_name_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = brandName,
                onValueChange = { brandName = it },
                label = { Text("Brand or Product Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = handle,
                onValueChange = { handle = it },
                label = { Text("Account Handle (e.g. @brand)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = objective,
                onValueChange = { objective = it },
                label = { Text("Campaign Objective *") },
                modifier = Modifier.fillMaxWidth().testTag("builder_objective_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Campaign Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = targetAudience,
                onValueChange = { targetAudience = it },
                label = { Text("Target Audience") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text("Social Media Platforms", style = MaterialTheme.typography.labelLarge, color = SegmaNavy)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availablePlatforms.forEach { plat ->
                    val chosen = selectedPlatforms.contains(plat)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (chosen) SegmaLime else SegmaSurface,
                        border = BorderStroke(1.dp, SegmaBorder),
                        modifier = Modifier.clickable {
                            selectedPlatforms = if (chosen && selectedPlatforms.size > 1) {
                                selectedPlatforms - plat
                            } else if (!chosen) {
                                selectedPlatforms + plat
                            } else selectedPlatforms
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (chosen) {
                                Icon(Icons.Filled.Check, null, tint = SegmaNavy, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(plat, style = MaterialTheme.typography.labelSmall, color = SegmaNavy)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    label = { Text("Budget (Optional)") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    onGenerateWithAi(
                        name.ifBlank { "New Campaign" },
                        brandName.ifBlank { "My Brand" },
                        objective.ifBlank { "Increase brand awareness and engagement" },
                        description,
                        targetAudience.ifBlank { "Gen Z & young digital consumers" },
                        selectedPlatforms,
                        duration,
                        budget,
                        false
                    )
                },
                enabled = draftState !is UiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = SegmaLime, contentColor = SegmaNavy),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("builder_generate_ai_btn")
            ) {
                if (draftState is UiState.Loading) {
                    CircularProgressIndicator(color = SegmaNavy, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating Strategy with Gemini...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Strategy with Segma AI", fontWeight = FontWeight.Bold)
                }
            }

            if ((draftState as? UiState.Success)?.data != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cacheNotice ?: "Generated strategy applied below.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SegmaTextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            onGenerateWithAi(
                                name.ifBlank { "New Campaign" },
                                brandName.ifBlank { "My Brand" },
                                objective.ifBlank { "Increase brand awareness and engagement" },
                                description,
                                targetAudience.ifBlank { "Gen Z & young digital consumers" },
                                selectedPlatforms,
                                duration,
                                budget,
                                true
                            )
                        },
                        enabled = draftState !is UiState.Loading,
                        border = BorderStroke(1.dp, SegmaNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("builder_regenerate_ai_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, null, tint = SegmaNavy, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Regenerate", style = MaterialTheme.typography.labelSmall, color = SegmaNavy)
                    }
                }
            }

            if (draftState is UiState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(draftState.message, color = SegmaDangerRed, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Editable Strategy Output Section
        SegmaOutlinedCard {
            Text(
                text = "Strategy & Content Plan (Editable)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SegmaNavy
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = aiOverview,
                onValueChange = { aiOverview = it },
                label = { Text("Campaign Overview") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = aiCommStrategy,
                onValueChange = { aiCommStrategy = it },
                label = { Text("Communication Strategy & Formats") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = aiPillars,
                onValueChange = { aiPillars = it },
                label = { Text("Content Pillars (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = aiUpload,
                onValueChange = { aiUpload = it },
                label = { Text("Upload Strategy & Best Times") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = aiCalendar,
                onValueChange = { aiCalendar = it },
                label = { Text("Content Calendar") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = aiCaptions,
                onValueChange = { aiCaptions = it },
                label = { Text("Copywriting, Hooks & Captions") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = aiHashtags,
                onValueChange = { aiHashtags = it },
                label = { Text("Recommended Hashtags") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = aiAudio,
                onValueChange = { aiAudio = it },
                label = { Text("Music & Audio Direction") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("Campaign Status", style = MaterialTheme.typography.labelLarge, color = SegmaNavy)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CampaignStatus.entries.forEach { st ->
                    val active = selectedStatus == st
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (active) SegmaNavy else SegmaSurface,
                        border = BorderStroke(1.dp, SegmaBorder),
                        modifier = Modifier.clickable { selectedStatus = st }
                    ) {
                        Text(
                            text = st.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (active) Color.White else SegmaNavy,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val toSave = (existingCampaign ?: Campaign()).copy(
                        name = name.trim().ifBlank { "Untitled Campaign" },
                        brandName = brandName.trim(),
                        handle = handle.trim(),
                        objective = objective.trim().ifBlank { "Increase audience engagement" },
                        description = description.trim(),
                        targetAudience = targetAudience.trim(),
                        audienceTags = targetAudience.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(4),
                        platforms = selectedPlatforms,
                        duration = duration.trim(),
                        budget = budget.trim(),
                        status = selectedStatus.name,
                        aiOverview = aiOverview.trim(),
                        aiCommunicationStrategy = aiCommStrategy.trim(),
                        aiContentPillars = aiPillars.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(6),
                        aiUploadStrategy = aiUpload.trim(),
                        aiContentCalendar = aiCalendar.trim(),
                        aiCaptions = aiCaptions.trim(),
                        aiHashtags = aiHashtags.split(" ").map { it.trim() }.filter { it.isNotBlank() }.take(15),
                        aiAudioRecommendation = aiAudio.trim(),
                        dataSourceLabel = existingCampaign?.dataSourceLabel?.ifBlank { "Created in Campaign Builder" }
                            ?: "Created in Campaign Builder"
                    )
                    onSaveCampaign(toSave)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SegmaNavy, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("builder_save_campaign_btn")
            ) {
                Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Campaign (${selectedStatus.label})", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
