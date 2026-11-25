package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
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
import com.example.model.AudiencePersona
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SegmaOutlinedCard
import com.example.ui.theme.SegmaBackground
import com.example.ui.theme.SegmaDangerRed
import com.example.ui.theme.SegmaLime
import com.example.ui.theme.SegmaNavy
import com.example.ui.theme.SegmaTextPrimary
import com.example.ui.theme.SegmaTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudienceProfilingScreen(
    personasState: UiState<List<AudiencePersona>>,
    draftState: UiState<AudiencePersona?>,
    onBack: () -> Unit,
    onGeneratePersona: (String, Boolean) -> Unit,
    onSavePersona: (AudiencePersona) -> Unit,
    onDeletePersona: (String) -> Unit,
    modifier: Modifier = Modifier,
    cacheNotice: String? = null
) {
    var briefInput by remember { mutableStateOf("") }

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
                text = "Audience Profiling & Archive",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SegmaNavy
            )
        }

        SegmaOutlinedCard {
            Text(
                text = "Generate AI Audience Persona",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SegmaNavy
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = briefInput,
                onValueChange = { briefInput = it },
                label = { Text("Describe product, niche, or target group") },
                placeholder = { Text("e.g. Eco-friendly matcha cafe near university campuses") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    onGeneratePersona(
                        briefInput.trim().ifBlank { "Gen Z university students interested in lifestyle and sustainability" },
                        false
                    )
                },
                enabled = draftState !is UiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = SegmaLime, contentColor = SegmaNavy),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("persona_generate_btn")
            ) {
                if (draftState is UiState.Loading) {
                    CircularProgressIndicator(color = SegmaNavy, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Profiling Audience...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Persona with Gemini", fontWeight = FontWeight.Bold)
                }
            }
            if (draftState is UiState.Error) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(draftState.message, color = SegmaDangerRed, style = MaterialTheme.typography.bodyMedium)
            }
        }

        val draft = (draftState as? UiState.Success)?.data
        if (draft != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cacheNotice ?: "Generated persona preview",
                    style = MaterialTheme.typography.labelSmall,
                    color = SegmaTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        onGeneratePersona(
                            briefInput.trim().ifBlank { "Gen Z university students interested in lifestyle and sustainability" },
                            true
                        )
                    },
                    enabled = draftState !is UiState.Loading,
                    border = BorderStroke(1.dp, SegmaNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("persona_regenerate_btn")
                ) {
                    Icon(Icons.Filled.Refresh, null, tint = SegmaNavy, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Regenerate", style = MaterialTheme.typography.labelSmall, color = SegmaNavy)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            PersonaCard(
                persona = draft,
                isDraft = true,
                onSave = { onSavePersona(draft) },
                onDelete = {}
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Saved Persona Archive",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = SegmaNavy
        )
        Spacer(modifier = Modifier.height(10.dp))

        when (personasState) {
            is UiState.Loading -> CircularProgressIndicator(color = SegmaNavy)
            is UiState.Error -> Text(personasState.message, color = SegmaDangerRed)
            is UiState.Success -> {
                if (personasState.data.isEmpty()) {
                    EmptyStateCard(
                        title = "No saved audience personas",
                        message = "Generate a persona above and save it for quick reuse across campaigns."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        personasState.data.forEach { p ->
                            PersonaCard(
                                persona = p,
                                isDraft = false,
                                onSave = {},
                                onDelete = { onDeletePersona(p.id) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonaCard(
    persona: AudiencePersona,
    isDraft: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    SegmaOutlinedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = persona.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SegmaNavy
                )
                Text(
                    text = "Age ${persona.ageRange} • ${persona.location}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SegmaTextSecondary
                )
            }
            Surface(shape = RoundedCornerShape(50), color = SegmaLime) {
                Text(
                    text = "Fit ${persona.feasibilityScore}/100",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SegmaNavy,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            if (!isDraft) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete persona", tint = SegmaTextSecondary)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(persona.summary, style = MaterialTheme.typography.bodyMedium, color = SegmaTextPrimary)
        if (persona.painPoints.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Pain Points: ${persona.painPoints}", style = MaterialTheme.typography.bodyMedium, color = SegmaTextSecondary)
        }
        if (persona.toneOfVoice.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tone of Voice: ${persona.toneOfVoice}", style = MaterialTheme.typography.labelLarge, color = SegmaNavy)
        }
        if (persona.interests.isNotEmpty() || persona.preferredFormats.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                persona.interests.forEach { tag ->
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFFEEF1FA)) {
                        Text(tag, style = MaterialTheme.typography.labelSmall, color = SegmaNavy, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
                persona.preferredFormats.forEach { fmt ->
                    Surface(shape = RoundedCornerShape(50), color = SegmaNavy) {
                        Text(fmt, style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
            }
        }
        if (isDraft) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = SegmaNavy, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save to Persona Archive", fontWeight = FontWeight.Bold)
            }
        }
    }
}
