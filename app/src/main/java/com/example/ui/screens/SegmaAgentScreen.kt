package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ai.AiUsageSnapshot
import com.example.model.AgentMessage
import com.example.model.CampaignStatus
import com.example.model.GeneratedStrategyDraft
import com.example.ui.components.SegmaSparkBadge
import com.example.ui.theme.SegmaBackground
import com.example.ui.theme.SegmaBorder
import com.example.ui.theme.SegmaDangerRed
import com.example.ui.theme.SegmaLime
import com.example.ui.theme.SegmaNavy
import com.example.ui.theme.SegmaSurface
import com.example.ui.theme.SegmaTextPrimary
import com.example.ui.theme.SegmaTextSecondary

@Composable
fun SegmaAgentScreen(
    userName: String,
    messages: List<AgentMessage>,
    isGenerating: Boolean,
    errorMessage: String?,
    onSendPrompt: (String) -> Unit,
    onRequestStrategyCard: () -> Unit,
    onEditDraft: (String, GeneratedStrategyDraft) -> Unit,
    onSaveStrategyDraft: (GeneratedStrategyDraft, CampaignStatus) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
    canRetry: Boolean = false,
    onRetry: () -> Unit = {},
    usageSnapshot: AiUsageSnapshot = AiUsageSnapshot()
) {
    var inputText by remember { mutableStateOf("") }
    var editingPair by remember { mutableStateOf<Pair<String, GeneratedStrategyDraft>?>(null) }
    val listState = rememberLazyListState()

    val quickPrompts = remember {
        listOf(
            "Help me create a campaign to increase awareness of a new product.",
            "I want an environmental awareness campaign targeting university students.",
            "Recommend a content strategy to boost Instagram engagement.",
            "Generate TikTok content ideas for the next 7 days."
        )
    }

    val lastMessageLength = messages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(messages.size, lastMessageLength, errorMessage) {
        val totalItems = listState.layoutInfo.totalItemsCount
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        } else if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SegmaBackground)
            .statusBarsPadding()
    ) {
        // Subtle header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SegmaSparkBadge(size = 34.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Segma Agent",
                        style = MaterialTheme.typography.titleLarge,
                        color = SegmaNavy
                    )
                    Text(
                        text = "AI Campaign Strategist • Gemini",
                        style = MaterialTheme.typography.labelSmall,
                        color = SegmaTextSecondary
                    )
                }
            }

            if (messages.any { !it.isUser && it.id != "welcome_agent" && it.strategyDraft == null } && !isGenerating) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SegmaLime,
                    modifier = Modifier.clickable { onRequestStrategyCard() }
                ) {
                    Text(
                        text = "Build Card",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SegmaNavy,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Chat Stream matching PDF Page 3
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (messages.size <= 1) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Try a campaign prompt:",
                            style = MaterialTheme.typography.labelLarge,
                            color = SegmaNavy
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickPrompts.forEach { sample ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SegmaSurface,
                                    border = BorderStroke(1.dp, SegmaBorder),
                                    modifier = Modifier
                                        .width(240.dp)
                                        .clickable(enabled = !isGenerating) { onSendPrompt(sample) }
                                ) {
                                    Text(
                                        text = sample,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SegmaNavy,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                if (msg.isUser) {
                    UserPromptBubble(userName = userName, text = msg.text)
                } else {
                    AgentReplyBlock(
                        message = msg,
                        onEditStrategy = { draft -> editingPair = msg.id to draft },
                        onSaveAsDraft = { draft ->
                            onSaveStrategyDraft(draft, CampaignStatus.DRAFT)
                        },
                        onCreateCampaign = { draft ->
                            onSaveStrategyDraft(draft, CampaignStatus.ONGOING)
                        }
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFECEC),
                        border = BorderStroke(1.dp, SegmaDangerRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("agent_error_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SegmaDangerRed
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = onClearError,
                                    modifier = Modifier.testTag("agent_dismiss_error_btn")
                                ) {
                                    Text("Dismiss", color = SegmaNavy)
                                }
                                if (canRetry) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = onRetry,
                                        enabled = !isGenerating,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SegmaNavy,
                                            contentColor = SegmaLime
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("agent_retry_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        if (usageSnapshot.isLimitReached || usageSnapshot.isApproachingLimit) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (usageSnapshot.isLimitReached) Color(0xFFFFECEC) else SegmaLime.copy(alpha = 0.45f),
                border = BorderStroke(
                    1.dp,
                    if (usageSnapshot.isLimitReached) SegmaDangerRed else SegmaBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp)
                    .testTag("agent_budget_banner")
            ) {
                Text(
                    text = if (usageSnapshot.isLimitReached) {
                        "App Usage Estimate: Local 24h AI budget reached (${usageSnapshot.usedIn24h}/${usageSnapshot.dailyLimit}). Saved strategy cards & chat history remain accessible."
                    } else {
                        "App Usage Estimate: ${usageSnapshot.usedIn24h}/${usageSnapshot.dailyLimit} local AI requests used in 24h (${usageSnapshot.remaining} left)."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (usageSnapshot.isLimitReached) SegmaDangerRed else SegmaNavy,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Bottom Input Bar matching PDF Page 3
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SegmaSurface,
            border = BorderStroke(1.2.dp, SegmaBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Ask Segma Agent",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SegmaTextSecondary
                        )
                    }
                    val canSend = !isGenerating && inputText.isNotBlank()
                    val triggerSend = {
                        if (canSend) {
                            val toSend = inputText
                            inputText = ""
                            onSendPrompt(toSend)
                        }
                    }
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = SegmaTextPrimary),
                        maxLines = 4,
                        cursorBrush = SolidColor(SegmaNavy),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { triggerSend() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("agent_chat_input")
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                val isSendEnabled = !isGenerating && inputText.isNotBlank()
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isGenerating -> Color(0xFFD6DAE8)
                                inputText.isNotBlank() -> SegmaLime
                                else -> SegmaLime.copy(alpha = 0.55f)
                            }
                        )
                        .clickable(
                            enabled = isSendEnabled,
                            role = Role.Button
                        ) {
                            val toSend = inputText
                            inputText = ""
                            onSendPrompt(toSend)
                        }
                        .testTag("agent_send_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = SegmaNavy,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(20.dp)
                                .testTag("agent_loading_indicator")
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Send prompt to Segma Agent",
                            tint = if (isSendEnabled) SegmaNavy else SegmaNavy.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    editingPair?.let { (msgId, draft) ->
        EditStrategyDialog(
            draft = draft,
            onDismiss = { editingPair = null },
            onConfirm = { updated ->
                onEditDraft(msgId, updated)
                editingPair = null
            }
        )
    }
}

@Composable
private fun UserPromptBubble(userName: String, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = userName.split(" ").firstOrNull().orEmpty().ifBlank { "You" },
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = SegmaNavy,
            modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SegmaSurface,
            border = BorderStroke(1.2.dp, SegmaBorder),
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = SegmaNavy,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgentReplyBlock(
    message: AgentMessage,
    onEditStrategy: (GeneratedStrategyDraft) -> Unit,
    onSaveAsDraft: (GeneratedStrategyDraft) -> Unit,
    onCreateCampaign: (GeneratedStrategyDraft) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            SegmaSparkBadge(size = 30.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (message.text.isBlank() && message.isStreaming) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = SegmaNavy,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mapping audience & strategy...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SegmaTextSecondary
                        )
                    }
                } else {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = SegmaTextPrimary
                    )
                    if (message.isStreaming) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = SegmaNavy,
                                strokeWidth = 1.8.dp,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Generating strategy...",
                                style = MaterialTheme.typography.labelSmall,
                                color = SegmaTextSecondary
                            )
                        }
                    }
                }
            }
        }

        message.strategyDraft?.let { draft ->
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SegmaSurface,
                border = BorderStroke(1.3.dp, SegmaNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STRUCTURED STRATEGY CARD",
                                style = MaterialTheme.typography.labelSmall,
                                color = SegmaTextSecondary
                            )
                            Text(
                                text = draft.campaignName,
                                style = MaterialTheme.typography.titleLarge,
                                color = SegmaNavy
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SegmaLime
                        ) {
                            Text(
                                text = "Fit: ${draft.feasibilityScore}/100",
                                style = MaterialTheme.typography.labelLarge,
                                color = SegmaNavy,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Text(
                        text = "Objective: ${draft.objective}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SegmaTextPrimary
                    )
                    Text(
                        text = "Audience: ${draft.targetAudience}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SegmaTextSecondary
                    )

                    if (draft.recommendedFormats.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            draft.platforms.forEach { p ->
                                Surface(shape = RoundedCornerShape(50), color = SegmaNavy) {
                                    Text(
                                        text = p,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            draft.recommendedFormats.forEach { fmt ->
                                Surface(shape = RoundedCornerShape(50), color = SegmaLime.copy(alpha = 0.6f)) {
                                    Text(
                                        text = fmt,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SegmaNavy,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (draft.uploadStrategy.isNotBlank()) {
                        Text(
                            text = "Upload Strategy: ${draft.uploadStrategy}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SegmaTextPrimary
                        )
                    }
                    if (draft.captionsAndHooks.isNotBlank()) {
                        Text(
                            text = "Hooks & Captions:\n${draft.captionsAndHooks}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SegmaTextSecondary
                        )
                    }
                    if (draft.hashtags.isNotEmpty()) {
                        Text(
                            text = draft.hashtags.joinToString(" "),
                            style = MaterialTheme.typography.labelLarge,
                            color = SegmaNavy
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onEditStrategy(draft) },
                            border = BorderStroke(1.dp, SegmaNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp), tint = SegmaNavy)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", color = SegmaNavy)
                        }
                        OutlinedButton(
                            onClick = { onSaveAsDraft(draft) },
                            border = BorderStroke(1.dp, SegmaNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp), tint = SegmaNavy)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Draft", color = SegmaNavy)
                        }
                    }

                    Button(
                        onClick = { onCreateCampaign(draft) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SegmaNavy,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.RocketLaunch, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Campaign")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditStrategyDialog(
    draft: GeneratedStrategyDraft,
    onDismiss: () -> Unit,
    onConfirm: (GeneratedStrategyDraft) -> Unit
) {
    var name by remember { mutableStateOf(draft.campaignName) }
    var objective by remember { mutableStateOf(draft.objective) }
    var audience by remember { mutableStateOf(draft.targetAudience) }
    var upload by remember { mutableStateOf(draft.uploadStrategy) }
    var captions by remember { mutableStateOf(draft.captionsAndHooks) }
    var hashtags by remember { mutableStateOf(draft.hashtags.joinToString(" ")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SegmaSurface,
        title = { Text("Edit AI Strategy", color = SegmaNavy) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Campaign Name") })
                OutlinedTextField(value = objective, onValueChange = { objective = it }, label = { Text("Objective") })
                OutlinedTextField(value = audience, onValueChange = { audience = it }, label = { Text("Target Audience") })
                OutlinedTextField(value = upload, onValueChange = { upload = it }, label = { Text("Upload Strategy") })
                OutlinedTextField(value = captions, onValueChange = { captions = it }, label = { Text("Hooks & Captions") })
                OutlinedTextField(value = hashtags, onValueChange = { hashtags = it }, label = { Text("Hashtags") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        draft.copy(
                            campaignName = name.ifBlank { draft.campaignName },
                            objective = objective.ifBlank { draft.objective },
                            targetAudience = audience,
                            uploadStrategy = upload,
                            captionsAndHooks = captions,
                            hashtags = hashtags.split(" ").map { it.trim() }.filter { it.isNotBlank() }
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SegmaNavy)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SegmaTextSecondary)
            }
        }
    )
}
