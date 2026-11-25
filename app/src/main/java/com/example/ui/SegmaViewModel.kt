package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiLocalBudgetExceededException
import com.example.ai.AiQuotaExceededException
import com.example.ai.AiUsageSnapshot
import com.example.ai.GeminiErrorClassifier
import com.example.ai.SegmaAiClient
import com.example.data.SegmaDataRepository
import com.example.data.SegmaDemoCatalog
import com.example.model.AgentMessage
import com.example.model.AudiencePersona
import com.example.model.Campaign
import com.example.model.CampaignEvaluation
import com.example.model.CampaignStatus
import com.example.model.CompetitorResearch
import com.example.model.GeneratedStrategyDraft
import com.example.model.TrendBookmark
import com.example.model.TrendInsight
import com.example.model.UiState
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "SegmaViewModel"
private const val STOP_TIMEOUT_MILLIS = 5000L

class SegmaViewModel(
    private val firestoreRepo: SegmaDataRepository,
    private val aiRepo: SegmaAiClient,
    val currentUserId: String,
    autoSeedProfile: Boolean = true,
    private val requestGuard: ActionRequestGuard = ActionRequestGuard()
) : ViewModel() {

    val aiUsageSnapshot: StateFlow<AiUsageSnapshot> = aiRepo.usageSnapshot
    val activeActionRequests: StateFlow<Set<String>> = requestGuard.activeActions

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        firestoreRepo.observeUserProfile(currentUserId)
            .toUiStateFlow("Failed to load user profile")

    val campaignsState: StateFlow<UiState<List<Campaign>>> =
        firestoreRepo.observeCampaigns(currentUserId)
            .toUiStateFlow("Failed to load campaigns")

    val personasState: StateFlow<UiState<List<AudiencePersona>>> =
        firestoreRepo.observePersonas(currentUserId)
            .toUiStateFlow("Failed to load audience personas")

    val evaluationsState: StateFlow<UiState<List<CampaignEvaluation>>> =
        firestoreRepo.observeEvaluations(currentUserId)
            .toUiStateFlow("Failed to load evaluation reports")

    val competitorsState: StateFlow<UiState<List<CompetitorResearch>>> =
        firestoreRepo.observeCompetitors(currentUserId)
            .toUiStateFlow("Failed to load competitor research")

    val bookmarksState: StateFlow<UiState<List<TrendBookmark>>> =
        firestoreRepo.observeBookmarks(currentUserId)
            .toUiStateFlow("Failed to load bookmarks")

    private val _agentMessages = MutableStateFlow<List<AgentMessage>>(
        listOf(
            AgentMessage(
                id = "welcome_agent",
                isUser = false,
                text = "Hi! I'm Segma Agent, your AI Campaign Strategist. Tell me your product, cause, or campaign goal—and I'll map your audience, angles, hooks, upload times, and content calendar."
            )
        )
    )
    val agentMessages: StateFlow<List<AgentMessage>> = _agentMessages.asStateFlow()

    private val _isAgentGenerating = MutableStateFlow(false)
    val isAgentGenerating: StateFlow<Boolean> = _isAgentGenerating.asStateFlow()

    private val _agentError = MutableStateFlow<String?>(null)
    val agentError: StateFlow<String?> = _agentError.asStateFlow()

    private val _canRetryAgent = MutableStateFlow(false)
    val canRetryAgent: StateFlow<Boolean> = _canRetryAgent.asStateFlow()

    private var pendingRetryRequest: Triple<String, String, Boolean>? = null

    private val _builderDraftState = MutableStateFlow<UiState<GeneratedStrategyDraft?>>(UiState.Success(null))
    val builderDraftState: StateFlow<UiState<GeneratedStrategyDraft?>> = _builderDraftState.asStateFlow()
    private val _builderCacheNotice = MutableStateFlow<String?>(null)
    val builderCacheNotice: StateFlow<String?> = _builderCacheNotice.asStateFlow()

    private val _evaluationDraftState = MutableStateFlow<UiState<CampaignEvaluation?>>(UiState.Success(null))
    val evaluationDraftState: StateFlow<UiState<CampaignEvaluation?>> = _evaluationDraftState.asStateFlow()
    private val _evaluationCacheNotice = MutableStateFlow<String?>(null)
    val evaluationCacheNotice: StateFlow<String?> = _evaluationCacheNotice.asStateFlow()

    private val _competitorDraftState = MutableStateFlow<UiState<CompetitorResearch?>>(UiState.Success(null))
    val competitorDraftState: StateFlow<UiState<CompetitorResearch?>> = _competitorDraftState.asStateFlow()
    private val _competitorCacheNotice = MutableStateFlow<String?>(null)
    val competitorCacheNotice: StateFlow<String?> = _competitorCacheNotice.asStateFlow()

    private val _personaDraftState = MutableStateFlow<UiState<AudiencePersona?>>(UiState.Success(null))
    val personaDraftState: StateFlow<UiState<AudiencePersona?>> = _personaDraftState.asStateFlow()
    private val _personaCacheNotice = MutableStateFlow<String?>(null)
    val personaCacheNotice: StateFlow<String?> = _personaCacheNotice.asStateFlow()

    private val _campaignAiInsightState = MutableStateFlow<UiState<String?>>(UiState.Success(null))
    val campaignAiInsightState: StateFlow<UiState<String?>> = _campaignAiInsightState.asStateFlow()
    private val _insightCacheNotice = MutableStateFlow<String?>(null)
    val insightCacheNotice: StateFlow<String?> = _insightCacheNotice.asStateFlow()

    private val _followUpContentState = MutableStateFlow<UiState<String?>>(UiState.Success(null))
    val followUpContentState: StateFlow<UiState<String?>> = _followUpContentState.asStateFlow()
    private val _followUpCacheNotice = MutableStateFlow<String?>(null)
    val followUpCacheNotice: StateFlow<String?> = _followUpCacheNotice.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    init {
        if (autoSeedProfile) {
            viewModelScope.launch {
                Firebase.auth.currentUser?.let { user ->
                    firestoreRepo.ensureUserProfileAndSeedDemoData(user)
                }
            }
        }
    }

    fun isActionInFlight(actionKey: String): Boolean = requestGuard.isInFlight(actionKey)

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun showFeedback(msg: String) {
        _feedbackMessage.value = msg
    }

    fun clearAgentError() {
        _agentError.value = null
        _canRetryAgent.value = false
        pendingRetryRequest = null
    }

    fun sendAgentPrompt(prompt: String, autoBuildStrategyCard: Boolean = false) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        var userMsgId = ""
        var aiMsgId = ""
        var historyPairs: List<Pair<Boolean, String>> = emptyList()

        launchGuarded(
            actionKey = SegmaActionKeys.AGENT_CHAT,
            beforeLaunch = {
                clearAgentError()
                userMsgId = UUID.randomUUID().toString()
                aiMsgId = UUID.randomUUID().toString()
                historyPairs = _agentMessages.value
                    .filter { it.id != "welcome_agent" && !it.isStreaming && it.text.isNotBlank() }
                    .map { it.isUser to it.text }

                _agentMessages.value = _agentMessages.value +
                    AgentMessage(id = userMsgId, isUser = true, text = trimmed) +
                    AgentMessage(id = aiMsgId, isUser = false, text = "", isStreaming = true)
                _isAgentGenerating.value = true
            }
        ) {
            executeAgentTurn(
                userMsgId = userMsgId,
                aiMsgId = aiMsgId,
                historyPairs = historyPairs,
                prompt = trimmed,
                autoBuildStrategyCard = autoBuildStrategyCard
            )
        }
    }

    fun retryLastAgentPrompt() {
        val retry = pendingRetryRequest ?: return
        val (userMsgId, prompt, autoBuildCard) = retry

        var aiMsgId = ""
        var historyPairs: List<Pair<Boolean, String>> = emptyList()

        launchGuarded(
            actionKey = SegmaActionKeys.AGENT_CHAT,
            beforeLaunch = {
                _agentError.value = null
                _canRetryAgent.value = false

                val msgs = _agentMessages.value
                val userIndex = msgs.indexOfLast { it.id == userMsgId }
                val priorMessages = if (userIndex >= 0) msgs.take(userIndex) else msgs
                historyPairs = priorMessages
                    .filter { it.id != "welcome_agent" && !it.isStreaming && it.text.isNotBlank() }
                    .map { it.isUser to it.text }

                aiMsgId = UUID.randomUUID().toString()
                _agentMessages.value = _agentMessages.value +
                    AgentMessage(id = aiMsgId, isUser = false, text = "", isStreaming = true)
                _isAgentGenerating.value = true
            }
        ) {
            executeAgentTurn(
                userMsgId = userMsgId,
                aiMsgId = aiMsgId,
                historyPairs = historyPairs,
                prompt = prompt,
                autoBuildStrategyCard = autoBuildCard
            )
        }
    }

    private suspend fun executeAgentTurn(
        userMsgId: String,
        aiMsgId: String,
        historyPairs: List<Pair<Boolean, String>>,
        prompt: String,
        autoBuildStrategyCard: Boolean
    ) {
        var finalText = ""
        try {
            aiRepo.streamAgentReply(historyPairs, prompt).collect { accumulated ->
                finalText = accumulated
                updateMessage(aiMsgId) { it.copy(text = accumulated, isStreaming = true) }
            }
            updateMessage(aiMsgId) { it.copy(isStreaming = false) }
            pendingRetryRequest = null
            _canRetryAgent.value = false

            // Deterministic local extraction prevents a redundant second Gemini request per agent reply.
            if (autoBuildStrategyCard || prompt.length > 24) {
                val draft = aiRepo.extractStrategyCardLocally(prompt, finalText)
                updateMessage(aiMsgId) { it.copy(strategyDraft = draft) }
            }
        } catch (e: TimeoutCancellationException) {
            logError("Segma Agent timed out", e)
            removeMessageById(aiMsgId)
            pendingRetryRequest = Triple(userMsgId, prompt, autoBuildStrategyCard)
            _canRetryAgent.value = true
            _agentError.value = "Segma Agent timed out after 60 seconds. Tap Retry to try again."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError("Segma Agent failed", e)
            removeMessageById(aiMsgId)
            pendingRetryRequest = Triple(userMsgId, prompt, autoBuildStrategyCard)
            _canRetryAgent.value = true
            _agentError.value = formatAiError(
                e,
                "Failed to reach Segma AI. Your prompt is saved above—please tap Retry when ready."
            )
        } finally {
            _isAgentGenerating.value = false
        }
    }

    fun generateStrategyCardForLastExchange() {
        if (requestGuard.isInFlight(SegmaActionKeys.AGENT_CHAT) || _isAgentGenerating.value) return
        val msgs = _agentMessages.value
        val lastModelMsg = msgs.lastOrNull { !it.isUser && it.text.isNotBlank() } ?: return
        val lastUserMsg = msgs.lastOrNull { it.isUser }?.text.orEmpty()

        clearAgentError()
        val draft = aiRepo.extractStrategyCardLocally(lastUserMsg, lastModelMsg.text)
        updateMessage(lastModelMsg.id) { it.copy(strategyDraft = draft) }
        _feedbackMessage.value = "Structured strategy card built locally (0 AI requests used)."
    }

    fun updateDraftInMessage(messageId: String, updatedDraft: GeneratedStrategyDraft) {
        updateMessage(messageId) { it.copy(strategyDraft = updatedDraft) }
        _feedbackMessage.value = "Strategy draft updated."
    }

    fun saveStrategyAsCampaign(
        draft: GeneratedStrategyDraft,
        status: CampaignStatus,
        onSaved: (String) -> Unit = {}
    ) {
        launchGuarded(SegmaActionKeys.SAVE_STRATEGY_CAMPAIGN) {
            val activeHandle = (userProfileState.value as? UiState.Success)?.data?.activeHandle
                ?.ifBlank { "@segmacreator" } ?: "@segmacreator"
            val campaign = Campaign(
                name = draft.campaignName,
                brandName = draft.brandName,
                handle = activeHandle,
                objective = draft.objective,
                description = draft.communicationAngle,
                targetAudience = draft.targetAudience,
                audienceTags = draft.audienceTags.take(4),
                platforms = draft.platforms,
                duration = "14 Days",
                budget = "Organic",
                status = status.name,
                demoData = false,
                targetReach = 10000L,
                targetEr = 5.0,
                dataSourceLabel = "Created via Segma Agent • No live metrics yet",
                aiOverview = draft.communicationAngle,
                aiCommunicationStrategy = draft.communicationAngle,
                aiContentPillars = draft.contentPillars,
                aiUploadStrategy = draft.uploadStrategy,
                aiContentCalendar = draft.contentCalendar,
                aiCaptions = draft.captionsAndHooks,
                aiHashtags = draft.hashtags,
                aiAudioRecommendation = draft.audioRecommendation,
                aiNextActionRecommendation = "Review schedule and publish first content piece."
            )
            firestoreRepo.saveCampaign(campaign)
                .onSuccess { id ->
                    _feedbackMessage.value = "Campaign saved as ${status.label}!"
                    onSaved(id)
                }
                .onFailure { err ->
                    _feedbackMessage.value = err.localizedMessage ?: "Failed to save campaign."
                }
        }
    }

    fun startStrategyFromTrend(trend: TrendInsight) {
        val prompt = """
            Build a campaign strategy around this Trend Radar insight:
            Trend: ${trend.title} (${trend.growthLabel})
            Category: ${trend.category.label}
            Top Hashtags: ${trend.topHashtags.joinToString(", ") { "${it.first} (${it.second})" }}
            Peak Interaction Window: ${trend.peakTimeNote}
            Segma Opportunity: ${trend.opportunityBody}
        """.trimIndent()
        sendAgentPrompt(prompt, autoBuildStrategyCard = true)
    }

    fun generateCampaignPlanInBuilder(
        name: String,
        brandName: String,
        objective: String,
        description: String,
        targetAudience: String,
        platforms: List<String>,
        duration: String,
        budget: String,
        forceRefresh: Boolean = false
    ) {
        launchGuarded(
            actionKey = SegmaActionKeys.BUILDER_GENERATE_PLAN,
            beforeLaunch = {
                _builderDraftState.value = UiState.Loading
                _builderCacheNotice.value = null
            }
        ) {
            try {
                val result = aiRepo.generateCampaignBuilderPlan(
                    name = name,
                    brandName = brandName,
                    objective = objective,
                    description = description,
                    targetAudience = targetAudience,
                    platforms = platforms,
                    duration = duration,
                    budget = budget,
                    forceRefresh = forceRefresh
                )
                _builderDraftState.value = UiState.Success(result.data)
                _builderCacheNotice.value = result.cacheLabel
            } catch (e: TimeoutCancellationException) {
                _builderDraftState.value = UiState.Error("Campaign generation timed out after 60 seconds. Your form inputs remain saved.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError("Campaign builder AI failed", e)
                _builderDraftState.value = UiState.Error(formatAiError(e, "Failed to generate campaign plan."))
            }
        }
    }

    fun clearBuilderDraft() {
        _builderDraftState.value = UiState.Success(null)
        _builderCacheNotice.value = null
    }

    fun saveFullCampaign(campaign: Campaign, onSaved: (String) -> Unit = {}) {
        launchGuarded(SegmaActionKeys.SAVE_FULL_CAMPAIGN) {
            firestoreRepo.saveCampaign(campaign)
                .onSuccess { id ->
                    _feedbackMessage.value = "Campaign '${campaign.name}' saved!"
                    onSaved(id)
                }
                .onFailure { err ->
                    _feedbackMessage.value = err.localizedMessage ?: "Failed to save campaign."
                }
        }
    }

    fun updateCampaignStatus(campaignId: String, status: CampaignStatus) {
        launchGuarded(SegmaActionKeys.updateCampaignStatus(campaignId)) {
            firestoreRepo.updateCampaignStatus(campaignId, status)
                .onSuccess { _feedbackMessage.value = "Status updated to ${status.label}." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to update status." }
        }
    }

    fun deleteCampaign(campaignId: String, onDeleted: () -> Unit = {}) {
        launchGuarded(SegmaActionKeys.deleteCampaign(campaignId)) {
            firestoreRepo.deleteCampaign(campaignId)
                .onSuccess {
                    _feedbackMessage.value = "Campaign deleted."
                    onDeleted()
                }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Delete failed." }
        }
    }

    fun refreshCampaignAiAnalysis(campaign: Campaign, forceRefresh: Boolean = false) {
        launchGuarded(
            actionKey = SegmaActionKeys.CAMPAIGN_AI_ANALYSIS,
            beforeLaunch = {
                _campaignAiInsightState.value = UiState.Loading
                _insightCacheNotice.value = null
            }
        ) {
            try {
                val result = aiRepo.analyzeCampaignPerformance(campaign, forceRefresh = forceRefresh)
                _campaignAiInsightState.value = UiState.Success(result.data)
                _insightCacheNotice.value = result.cacheLabel
                if (!result.fromCache) {
                    firestoreRepo.updateCampaignMetricsAndInsight(
                        campaignId = campaign.id,
                        reach = campaign.reach,
                        views = campaign.views,
                        likes = campaign.likes,
                        comments = campaign.comments,
                        shares = campaign.shares,
                        saves = campaign.saves,
                        demoData = campaign.demoData,
                        dataSourceLabel = campaign.dataSourceLabel,
                        aiNextActionRecommendation = result.data
                    )
                }
            } catch (e: TimeoutCancellationException) {
                _campaignAiInsightState.value = UiState.Error("Analysis timed out. Saved metrics remain intact.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError("Campaign AI analysis failed", e)
                _campaignAiInsightState.value = UiState.Error(formatAiError(e, "Analysis failed."))
            }
        }
    }

    fun updateCampaignManualMetrics(
        campaign: Campaign,
        reach: Long,
        views: Long,
        likes: Long,
        comments: Long,
        shares: Long,
        saves: Long
    ) {
        launchGuarded(SegmaActionKeys.updateCampaignMetrics(campaign.id)) {
            firestoreRepo.updateCampaignMetricsAndInsight(
                campaignId = campaign.id,
                reach = reach,
                views = views,
                likes = likes,
                comments = comments,
                shares = shares,
                saves = saves,
                demoData = false,
                dataSourceLabel = "User-Logged Metrics • Updated just now",
                aiNextActionRecommendation = campaign.aiNextActionRecommendation
            ).onSuccess {
                _feedbackMessage.value = "Campaign metrics updated."
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to update metrics."
            }
        }
    }

    fun requestFollowUpContent(campaign: Campaign, forceRefresh: Boolean = false) {
        launchGuarded(
            actionKey = SegmaActionKeys.CAMPAIGN_FOLLOW_UP,
            beforeLaunch = {
                _followUpContentState.value = UiState.Loading
                _followUpCacheNotice.value = null
            }
        ) {
            try {
                val result = aiRepo.generateFollowUpContent(campaign, forceRefresh = forceRefresh)
                _followUpContentState.value = UiState.Success(result.data)
                _followUpCacheNotice.value = result.cacheLabel
            } catch (e: TimeoutCancellationException) {
                _followUpContentState.value = UiState.Error("Follow-up generation timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError("Follow-up content failed", e)
                _followUpContentState.value = UiState.Error(formatAiError(e, "Failed to generate follow-up content."))
            }
        }
    }

    fun clearFollowUpContent() {
        _followUpContentState.value = UiState.Success(null)
        _followUpCacheNotice.value = null
    }

    fun runCampaignEvaluation(campaign: Campaign, forceRefresh: Boolean = false) {
        if (requestGuard.isInFlight(SegmaActionKeys.CAMPAIGN_EVALUATION)) return

        // Reuse existing saved evaluation report from Firestore when campaign hasn't changed and not regenerating
        if (!forceRefresh) {
            val savedEvaluations = (evaluationsState.value as? UiState.Success)?.data.orEmpty()
            val campaignUpdatedMs = campaign.updatedAt?.toDate()?.time ?: 0L
            val matchingSaved = savedEvaluations.firstOrNull { eval ->
                val evalUpdatedMs = eval.updatedAt?.toDate()?.time ?: 0L
                eval.campaignId == campaign.id && (campaign.updatedAt == null || evalUpdatedMs >= campaignUpdatedMs)
            }
            if (matchingSaved != null) {
                _evaluationDraftState.value = UiState.Success(matchingSaved)
                _evaluationCacheNotice.value = "Reused saved evaluation report (0 AI requests used)"
                return
            }
        }

        launchGuarded(
            actionKey = SegmaActionKeys.CAMPAIGN_EVALUATION,
            beforeLaunch = {
                _evaluationDraftState.value = UiState.Loading
                _evaluationCacheNotice.value = null
            }
        ) {
            try {
                val result = aiRepo.evaluateCampaign(campaign, forceRefresh = forceRefresh)
                _evaluationDraftState.value = UiState.Success(result.data)
                _evaluationCacheNotice.value = result.cacheLabel
            } catch (e: TimeoutCancellationException) {
                _evaluationDraftState.value = UiState.Error("Evaluation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError("Evaluation failed", e)
                _evaluationDraftState.value = UiState.Error(formatAiError(e, "Evaluation failed."))
            }
        }
    }

    fun saveEvaluationReport(evaluation: CampaignEvaluation) {
        launchGuarded(SegmaActionKeys.SAVE_EVALUATION_REPORT) {
            firestoreRepo.saveEvaluation(evaluation)
                .onSuccess { _feedbackMessage.value = "Evaluation report saved to History!" }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to save report." }
        }
    }

    fun runCompetitorResearch(
        brandName: String,
        username: String,
        platform: String,
        manualObservedNotes: String,
        userBrandContext: String,
        forceRefresh: Boolean = false
    ) {
        if (requestGuard.isInFlight(SegmaActionKeys.COMPETITOR_RESEARCH)) return

        if (!forceRefresh && manualObservedNotes.isBlank() && userBrandContext.isBlank()) {
            val saved = (competitorsState.value as? UiState.Success)?.data.orEmpty()
            val matchingSaved = saved.firstOrNull { item ->
                item.brandName.equals(brandName.trim(), ignoreCase = true) &&
                    item.username.equals(username.trim(), ignoreCase = true) &&
                    item.platform.equals(platform.trim(), ignoreCase = true)
            }
            if (matchingSaved != null) {
                _competitorDraftState.value = UiState.Success(matchingSaved)
                _competitorCacheNotice.value = "Reused saved competitor research (0 AI requests used)"
                return
            }
        }

        launchGuarded(
            actionKey = SegmaActionKeys.COMPETITOR_RESEARCH,
            beforeLaunch = {
                _competitorDraftState.value = UiState.Loading
                _competitorCacheNotice.value = null
            }
        ) {
            try {
                val result = aiRepo.analyzeCompetitor(
                    brandName = brandName,
                    username = username,
                    platform = platform,
                    manualObservedNotes = manualObservedNotes,
                    userBrandContext = userBrandContext,
                    forceRefresh = forceRefresh
                )
                _competitorDraftState.value = UiState.Success(result.data)
                _competitorCacheNotice.value = result.cacheLabel
            } catch (e: TimeoutCancellationException) {
                _competitorDraftState.value = UiState.Error("Competitor analysis timed out. Your notes remain saved.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError("Competitor research failed", e)
                _competitorDraftState.value = UiState.Error(formatAiError(e, "Competitor research failed."))
            }
        }
    }

    fun saveCompetitorResearch(research: CompetitorResearch) {
        launchGuarded(SegmaActionKeys.SAVE_COMPETITOR_RESEARCH) {
            firestoreRepo.saveCompetitorResearch(research)
                .onSuccess { _feedbackMessage.value = "Competitor insight archived!" }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to save insight." }
        }
    }

    fun generateAndPreviewPersona(brief: String, forceRefresh: Boolean = false) {
        launchGuarded(
            actionKey = SegmaActionKeys.AUDIENCE_PERSONA,
            beforeLaunch = {
                _personaDraftState.value = UiState.Loading
                _personaCacheNotice.value = null
            }
        ) {
            try {
                val result = aiRepo.generateAudiencePersona(brief, forceRefresh = forceRefresh)
                _personaDraftState.value = UiState.Success(result.data)
                _personaCacheNotice.value = result.cacheLabel
            } catch (e: TimeoutCancellationException) {
                _personaDraftState.value = UiState.Error("Persona generation timed out. Your brief remains saved.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError("Persona generation failed", e)
                _personaDraftState.value = UiState.Error(formatAiError(e, "Failed to generate persona."))
            }
        }
    }

    fun savePersona(persona: AudiencePersona) {
        launchGuarded(SegmaActionKeys.SAVE_PERSONA) {
            firestoreRepo.savePersona(persona)
                .onSuccess {
                    _personaDraftState.value = UiState.Success(null)
                    _personaCacheNotice.value = null
                    _feedbackMessage.value = "Persona '${persona.name}' saved!"
                }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to save persona." }
        }
    }

    fun deletePersona(personaId: String) {
        launchGuarded(SegmaActionKeys.deletePersona(personaId)) {
            firestoreRepo.deletePersona(personaId)
                .onSuccess { _feedbackMessage.value = "Persona deleted." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to delete persona." }
        }
    }

    fun toggleBookmark(
        trendId: String,
        title: String,
        category: String,
        summary: String,
        aiRecommendation: String,
        currentlyBookmarked: Boolean
    ) {
        launchGuarded(SegmaActionKeys.toggleBookmark(trendId)) {
            firestoreRepo.toggleBookmark(
                trendId = trendId,
                title = title,
                category = category,
                summary = summary,
                aiRecommendation = aiRecommendation,
                currentlyBookmarked = currentlyBookmarked
            ).onSuccess { nowSaved ->
                _feedbackMessage.value = if (nowSaved) "Saved to Bookmarks!" else "Removed from Bookmarks."
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Bookmark update failed."
            }
        }
    }

    fun updateProfileDetails(
        displayName: String,
        roleTitle: String,
        activeHandle: String,
        secondaryHandle: String
    ) {
        launchGuarded(SegmaActionKeys.UPDATE_PROFILE_DETAILS) {
            firestoreRepo.updateProfileDetails(displayName, roleTitle, activeHandle, secondaryHandle)
                .onSuccess { _feedbackMessage.value = "Profile updated!" }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Profile update failed." }
        }
    }

    fun updateSocialConnection(
        instagramConnected: Boolean,
        instagramHandle: String,
        tiktokConnected: Boolean,
        tiktokHandle: String
    ) {
        launchGuarded(SegmaActionKeys.UPDATE_SOCIAL_CONNECTION) {
            firestoreRepo.updateSocialConnection(
                instagramConnected = instagramConnected,
                instagramHandle = instagramHandle,
                tiktokConnected = tiktokConnected,
                tiktokHandle = tiktokHandle
            ).onSuccess {
                _feedbackMessage.value = "Social account connection updated."
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to update connection."
            }
        }
    }

    fun reseedDemoCampaignIfEmpty() {
        launchGuarded(SegmaActionKeys.RESEED_DEMO_CAMPAIGN) {
            val demo = SegmaDemoCatalog.initialDemoCampaigns.first()
            firestoreRepo.saveCampaign(demo)
                .onSuccess { _feedbackMessage.value = "Demo campaign restored." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Restore failed." }
        }
    }

    /**
     * Atomically acquires [actionKey] synchronously before launching a coroutine, preventing
     * duplicate simultaneous API calls for the same action (even before the coroutine starts).
     * Always releases [actionKey] on completion, failure, or cancellation.
     */
    private fun launchGuarded(
        actionKey: String,
        beforeLaunch: () -> Unit = {},
        block: suspend () -> Unit
    ): Boolean {
        if (!requestGuard.tryAcquire(actionKey)) {
            logWarn("Ignored simultaneous duplicate request for action: $actionKey")
            return false
        }
        beforeLaunch()
        val job = viewModelScope.launch {
            block()
        }
        job.invokeOnCompletion {
            requestGuard.release(actionKey)
        }
        return true
    }

    private fun formatAiError(error: Throwable, fallback: String): String = when (error) {
        is AiLocalBudgetExceededException -> error.message ?: fallback
        is AiQuotaExceededException -> error.message
        else -> GeminiErrorClassifier.classify(error, fallback).userMessage
    }

    private fun logError(message: String, throwable: Throwable) {
        try {
            Log.e(TAG, message, throwable)
        } catch (_: RuntimeException) {
            // Ignored when running in offline pure-JVM unit tests without Android Log stubs
        }
    }

    private fun logWarn(message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) Log.w(TAG, message, throwable) else Log.w(TAG, message)
        } catch (_: RuntimeException) {
            // Ignored when running in offline pure-JVM unit tests without Android Log stubs
        }
    }

    private fun updateMessage(id: String, transform: (AgentMessage) -> AgentMessage) {
        _agentMessages.value = _agentMessages.value.map { msg ->
            if (msg.id == id) transform(msg) else msg
        }
    }

    private fun removeMessageById(id: String) {
        _agentMessages.value = _agentMessages.value.filterNot { it.id == id }
    }

    private fun <T> Flow<T>.toUiStateFlow(fallbackError: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                logWarn(fallbackError, rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )
}
