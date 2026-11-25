package com.example.ai

import com.example.model.AudiencePersona
import com.example.model.Campaign
import com.example.model.CampaignEvaluation
import com.example.model.CompetitorResearch
import com.example.model.GeneratedStrategyDraft
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

interface SegmaAiClient {
    val usageSnapshot: StateFlow<AiUsageSnapshot>

    fun streamAgentReply(
        historyMessages: List<Pair<Boolean, String>>,
        userPrompt: String
    ): Flow<String>

    fun extractStrategyCardLocally(
        userPrompt: String,
        agentReply: String
    ): GeneratedStrategyDraft

    suspend fun generateCampaignBuilderPlan(
        name: String,
        brandName: String,
        objective: String,
        description: String,
        targetAudience: String,
        platforms: List<String>,
        duration: String,
        budget: String,
        forceRefresh: Boolean = false
    ): AiCachedResult<GeneratedStrategyDraft>

    suspend fun analyzeCampaignPerformance(
        campaign: Campaign,
        forceRefresh: Boolean = false
    ): AiCachedResult<String>

    suspend fun generateFollowUpContent(
        campaign: Campaign,
        forceRefresh: Boolean = false
    ): AiCachedResult<String>

    suspend fun evaluateCampaign(
        campaign: Campaign,
        forceRefresh: Boolean = false
    ): AiCachedResult<CampaignEvaluation>

    suspend fun analyzeCompetitor(
        brandName: String,
        username: String,
        platform: String,
        manualObservedNotes: String,
        userBrandContext: String,
        forceRefresh: Boolean = false
    ): AiCachedResult<CompetitorResearch>

    suspend fun generateAudiencePersona(
        brief: String,
        forceRefresh: Boolean = false
    ): AiCachedResult<AudiencePersona>
}

class SegmaAiRepository(
    private val coordinator: AiRequestCoordinator = AiRequestCoordinator()
) : SegmaAiClient {

    override val usageSnapshot: StateFlow<AiUsageSnapshot> = coordinator.usageSnapshot

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_GEMINI_FLASH,
            generationConfig = generationConfig {
                maxOutputTokens = 700
                temperature = 0.7f
            },
            systemInstruction = content {
                text(
                    """
                    You are Segma Agent, AI Digital Campaign Strategist in SEGMA.AI.
                    Expertise: social media strategy, campaign planning, hooks/captions/CTAs, audience segmentation, and engagement optimization.
                    Rules:
                    - Use prior turns naturally; avoid repeating boilerplate.
                    - If core context (audience, platform, goal) is missing, give a brief starting direction + 2-3 targeted questions.
                    - For full plans, use concise numbered sections: (1) Audience & Feasibility Score (0-100), (2) Angle, Formats & Posting Schedule, (3) Pillars, Hooks, Captions & CTA, (4) Hashtags, Optimization & Audio Mood.
                    - Keep replies focused and practical. Never invent live platform analytics or chart rankings.
                    """.trimIndent()
                )
            }
        )
    }

    private val assistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_GEMINI_FLASH,
            generationConfig = generationConfig {
                maxOutputTokens = 500
                temperature = 0.6f
            },
            systemInstruction = content {
                text(
                    "You are SEGMA.AI's concise digital marketing analyst and copywriter. " +
                        "Provide crisp, actionable output strictly grounded in provided data without inventing metrics."
                )
            }
        )
    }

    private val strategySchema by lazy {
        Schema.obj(
            properties = mapOf(
                "campaignName" to Schema.string("Short compelling campaign name"),
                "brandName" to Schema.string("Brand or creator name"),
                "objective" to Schema.string("Primary campaign objective"),
                "targetAudience" to Schema.string("Concise target audience persona"),
                "audienceTags" to Schema.array(Schema.string(), "Up to 4 short tags"),
                "feasibilityScore" to Schema.integer("Estimated fit score 0-100"),
                "communicationAngle" to Schema.string("Messaging angle and tone"),
                "platforms" to Schema.array(Schema.string(), "Recommended social platforms"),
                "recommendedFormats" to Schema.array(Schema.string(), "Recommended content formats"),
                "uploadStrategy" to Schema.string("Recommended cadence and posting windows"),
                "contentCalendar" to Schema.string("Concise multi-day publishing plan"),
                "captionsAndHooks" to Schema.string("Sample hooks, captions, and CTAs"),
                "hashtags" to Schema.array(Schema.string(), "Up to 8 hashtags"),
                "audioRecommendation" to Schema.string("Creative audio mood suggestion"),
                "contentPillars" to Schema.array(Schema.string(), "3 to 4 core content pillars")
            )
        )
    }

    private val strategyJsonModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_GEMINI_FLASH,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = strategySchema
                maxOutputTokens = 900
                temperature = 0.55f
            }
        )
    }

    private val evaluationSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "executionSummary" to Schema.string("Concise summary of campaign execution"),
                "objectiveAchievement" to Schema.string("Assessment of objective achievement"),
                "targetVsActual" to Schema.string("Comparison between targets and actual performance"),
                "engagementAnalysis" to Schema.string("Analysis of ER, likes, comments, shares, saves"),
                "contentEffectiveness" to Schema.string("Effectiveness of formats and messaging"),
                "strengths" to Schema.string("Key campaign strengths"),
                "weaknesses" to Schema.string("Areas needing improvement"),
                "influencingFactors" to Schema.string("Internal and external factors affecting results"),
                "futureRecommendations" to Schema.string("Actionable recommendations for future campaigns"),
                "overallScore" to Schema.integer("Overall evaluation score 0-100")
            )
        )
    }

    private val evaluationJsonModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_GEMINI_FLASH,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = evaluationSchema
                maxOutputTokens = 900
                temperature = 0.4f
            }
        )
    }

    private val competitorSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "contentThemes" to Schema.string("Likely content themes and pillars"),
                "publishingFrequency" to Schema.string("Recommended benchmark cadence or observed frequency"),
                "dominantFormats" to Schema.string("Primary content formats on this platform"),
                "observedMetricsSummary" to Schema.string("Summary of provided observations or note that live scraping is unavailable"),
                "communicationStyle" to Schema.string("Tone of voice and storytelling patterns"),
                "strengths" to Schema.string("Key competitive strengths"),
                "opportunities" to Schema.string("Untapped content gaps and opportunities"),
                "differentiationStrategy" to Schema.string("Concrete differentiation strategy for the user's brand")
            )
        )
    }

    private val competitorJsonModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_GEMINI_FLASH,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = competitorSchema
                maxOutputTokens = 750
                temperature = 0.45f
            }
        )
    }

    private val personaSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "name" to Schema.string("Persona archetype name"),
                "ageRange" to Schema.string("Age bracket e.g. 18-24"),
                "location" to Schema.string("Primary geographic/lifestyle setting"),
                "interests" to Schema.array(Schema.string(), "Up to 5 interests"),
                "painPoints" to Schema.string("Key frustrations, objections, or motivations"),
                "preferredFormats" to Schema.array(Schema.string(), "Up to 4 preferred social formats"),
                "toneOfVoice" to Schema.string("Best-resonating communication style"),
                "feasibilityScore" to Schema.integer("Audience opportunity score 0-100"),
                "summary" to Schema.string("Concise strategic summary")
            )
        )
    }

    private val personaJsonModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_GEMINI_FLASH,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = personaSchema
                maxOutputTokens = 550
                temperature = 0.6f
            }
        )
    }

    override fun streamAgentReply(
        historyMessages: List<Pair<Boolean, String>>,
        userPrompt: String
    ): Flow<String> = flow {
        val sanitizedHistory = normalizeHistoryPairs(historyMessages, DEFAULT_MAX_CONTEXT_MESSAGES)
        val anchoredPrompt = buildAnchoredPromptIfTrimmed(
            historyMessages = historyMessages,
            userPrompt = userPrompt,
            maxMessages = DEFAULT_MAX_CONTEXT_MESSAGES
        )
        val chatHistory: List<Content> = sanitizedHistory.map { (isUser, text) ->
            content(if (isUser) "user" else "model") { text(text) }
        }

        coordinator.prepareLiveChatDispatch()
        var failure: Throwable? = null
        try {
            val chat = chatModel.startChat(history = chatHistory)
            val buffer = StringBuilder()
            withTimeout(60_000L) {
                chat.sendMessageStream(anchoredPrompt).collect { chunk ->
                    val part = chunk.text.orEmpty()
                    if (part.isNotEmpty()) {
                        buffer.append(part)
                        emit(buffer.toString())
                    }
                }
            }
            if (buffer.isBlank()) {
                throw IllegalStateException("Segma Agent returned an empty response.")
            }
        } catch (t: Throwable) {
            failure = t
            if (t is CancellationException) throw t
            throw mapToTypedAiException(t, "Failed to reach Segma AI. Please try again.")
        } finally {
            withContext(NonCancellable) {
                coordinator.finishLiveChatDispatch(failure)
            }
        }
    }

    override fun extractStrategyCardLocally(
        userPrompt: String,
        agentReply: String
    ): GeneratedStrategyDraft = SegmaStrategyLocalExtractor.extractFromAgentReply(userPrompt, agentReply)

    override suspend fun generateCampaignBuilderPlan(
        name: String,
        brandName: String,
        objective: String,
        description: String,
        targetAudience: String,
        platforms: List<String>,
        duration: String,
        budget: String,
        forceRefresh: Boolean
    ): AiCachedResult<GeneratedStrategyDraft> {
        val normalizedPlatforms = platforms.map { it.trim() }.filter { it.isNotBlank() }.sorted()
        val keyInput = listOf(
            name,
            brandName,
            objective,
            description,
            targetAudience,
            normalizedPlatforms.joinToString(","),
            duration,
            budget.ifBlank { "organic" }
        ).joinToString("|")

        return executeCachedGuarded(
            featureType = "campaign_builder",
            normalizedKeyInput = keyInput,
            forceRefresh = forceRefresh,
            serializer = AiJsonCodecs::encodeStrategyDraft,
            deserializer = AiJsonCodecs::decodeStrategyDraft
        ) {
            val response = withTimeout(60_000L) {
                strategyJsonModel.generateContent(
                    """
                    Create a concise, structured digital marketing campaign plan for SEGMA.AI:
                    - Campaign: $name | Brand: $brandName
                    - Objective: $objective
                    - Description: ${description.ifBlank { "Not specified" }}
                    - Target Audience: $targetAudience
                    - Platforms: ${platforms.joinToString(", ")}
                    - Duration: $duration | Budget: ${budget.ifBlank { "Organic-first" }}
                    Keep pillars, upload schedule, multi-day calendar, hooks/captions, hashtags, and audio mood practical and compact.
                    """.trimIndent()
                )
            }
            val jsonStr = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Model returned empty campaign plan.")
            parseStrategyJson(JSONObject(jsonStr))
        }
    }

    override suspend fun analyzeCampaignPerformance(
        campaign: Campaign,
        forceRefresh: Boolean
    ): AiCachedResult<String> {
        val hasMetrics = campaign.reach > 0L || campaign.views > 0L ||
            campaign.likes > 0L || campaign.comments > 0L || campaign.shares > 0L || campaign.saves > 0L
        if (!hasMetrics) {
            return AiCachedResult(
                data = "Insufficient performance data: This campaign currently has 0 recorded Reach/Engagement metrics. Once live or demo metrics are recorded, Segma AI can analyze engagement patterns and recommend optimizations.",
                fromCache = false,
                cacheLabel = null
            )
        }

        val erFormatted = String.format(Locale.US, "%.1f", campaign.computedEngagementRate)
        val keyInput = listOf(
            campaign.id,
            campaign.status,
            campaign.objective,
            campaign.reach,
            campaign.views,
            campaign.likes,
            campaign.comments,
            campaign.shares,
            campaign.saves,
            campaign.targetReach,
            campaign.targetEr,
            campaign.aiCommunicationStrategy
        ).joinToString("|")

        return executeCachedGuarded(
            featureType = "campaign_analysis",
            normalizedKeyInput = keyInput,
            forceRefresh = forceRefresh,
            serializer = { it },
            deserializer = { it.takeIf { s -> s.isNotBlank() } }
        ) {
            val response = withTimeout(60_000L) {
                assistantModel.generateContent(
                    """
                    Analyze this campaign's performance strictly from the given numbers (Demo Data: ${campaign.demoData}):
                    - Campaign: ${campaign.name} (${campaign.handle}) | Status: ${campaign.status}
                    - Objective: ${campaign.objective}
                    - Target Reach: ${campaign.targetReach}, Actual Reach: ${campaign.reach}
                    - Target ER: ${campaign.targetEr}%, Actual ER: $erFormatted%
                    - Views: ${campaign.views}, Likes: ${campaign.likes}, Comments: ${campaign.comments}, Shares: ${campaign.shares}, Saves: ${campaign.saves}
                    - Audience: ${campaign.targetAudience}
                    - Strategy: ${campaign.aiCommunicationStrategy}
                    Provide a concise bullet summary: target vs actual, key engagement signal (shares/saves/comments), and 2 next actions.
                    """.trimIndent()
                )
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("AI analysis returned an empty response.")
        }
    }

    override suspend fun generateFollowUpContent(
        campaign: Campaign,
        forceRefresh: Boolean
    ): AiCachedResult<String> {
        val keyInput = listOf(
            campaign.id,
            campaign.name,
            campaign.platforms.sorted().joinToString(","),
            campaign.targetAudience,
            campaign.objective,
            campaign.aiCommunicationStrategy
        ).joinToString("|")

        return executeCachedGuarded(
            featureType = "follow_up_content",
            normalizedKeyInput = keyInput,
            forceRefresh = forceRefresh,
            serializer = { it },
            deserializer = { it.takeIf { s -> s.isNotBlank() } }
        ) {
            val response = withTimeout(60_000L) {
                assistantModel.generateContent(
                    """
                    Create 3 concise Part-2 follow-up content concepts for "${campaign.name}" (${campaign.platforms.joinToString(", ")}).
                    Audience: ${campaign.targetAudience} | Objective: ${campaign.objective}
                    Angle: ${campaign.aiCommunicationStrategy}
                    For each: Format, 2-second Hook, Caption, and CTA.
                    """.trimIndent()
                )
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Follow-up content generation returned empty text.")
        }
    }

    override suspend fun evaluateCampaign(
        campaign: Campaign,
        forceRefresh: Boolean
    ): AiCachedResult<CampaignEvaluation> {
        val hasMetrics = campaign.reach > 0L || campaign.likes > 0L || campaign.views > 0L
        val erFormatted = String.format(Locale.US, "%.1f", campaign.computedEngagementRate)
        val metricsBlock = if (hasMetrics) {
            "Actual Reach: ${campaign.reach} (Target: ${campaign.targetReach}), Views: ${campaign.views}, Likes: ${campaign.likes}, Comments: ${campaign.comments}, Shares: ${campaign.shares}, Saves: ${campaign.saves}, ER: $erFormatted% (Target ER: ${campaign.targetEr}%). Data label: ${campaign.dataSourceLabel}."
        } else {
            "NO ACTUAL METRICS AVAILABLE (all zeros). Note in targetVsActual and engagementAnalysis that metrics are not yet recorded; evaluate readiness/strategy quality without fabricating numbers."
        }

        val keyInput = listOf(
            campaign.id,
            campaign.name,
            campaign.status,
            campaign.objective,
            campaign.duration,
            campaign.reach,
            campaign.views,
            campaign.likes,
            campaign.comments,
            campaign.shares,
            campaign.saves,
            campaign.targetReach,
            campaign.targetEr,
            campaign.aiOverview
        ).joinToString("|")

        return executeCachedGuarded(
            featureType = "campaign_evaluation",
            normalizedKeyInput = keyInput,
            forceRefresh = forceRefresh,
            serializer = AiJsonCodecs::encodeEvaluation,
            deserializer = AiJsonCodecs::decodeEvaluation
        ) {
            val response = withTimeout(60_000L) {
                evaluationJsonModel.generateContent(
                    """
                    Evaluate this digital campaign honestly and concisely based strictly on provided fields:
                    Campaign: ${campaign.name} (${campaign.brandName}) | Status: ${campaign.status}
                    Objective: ${campaign.objective} | Platforms: ${campaign.platforms.joinToString(", ")} | Duration: ${campaign.duration}
                    Audience: ${campaign.targetAudience}
                    Overview: ${campaign.aiOverview}
                    $metricsBlock
                    """.trimIndent()
                )
            }
            val jsonStr = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Evaluation returned empty JSON.")
            val obj = JSONObject(jsonStr)
            CampaignEvaluation(
                campaignId = campaign.id,
                campaignName = campaign.name,
                executionSummary = obj.optString("executionSummary"),
                objectiveAchievement = obj.optString("objectiveAchievement"),
                targetVsActual = obj.optString("targetVsActual"),
                engagementAnalysis = obj.optString("engagementAnalysis"),
                contentEffectiveness = obj.optString("contentEffectiveness"),
                strengths = obj.optString("strengths"),
                weaknesses = obj.optString("weaknesses"),
                influencingFactors = obj.optString("influencingFactors"),
                futureRecommendations = obj.optString("futureRecommendations"),
                overallScore = obj.optLong("overallScore", 75L).coerceIn(0L, 100L)
            )
        }
    }

    override suspend fun analyzeCompetitor(
        brandName: String,
        username: String,
        platform: String,
        manualObservedNotes: String,
        userBrandContext: String,
        forceRefresh: Boolean
    ): AiCachedResult<CompetitorResearch> {
        val dataMode = if (manualObservedNotes.isNotBlank()) {
            "AI Analysis + User-Provided Public Observations"
        } else {
            "AI Strategic Estimation (No Live Platform API Connected)"
        }
        val keyInput = listOf(brandName, username, platform, manualObservedNotes, userBrandContext).joinToString("|")

        return executeCachedGuarded(
            featureType = "competitor_research",
            normalizedKeyInput = keyInput,
            forceRefresh = forceRefresh,
            serializer = AiJsonCodecs::encodeCompetitor,
            deserializer = AiJsonCodecs::decodeCompetitor
        ) {
            val response = withTimeout(60_000L) {
                competitorJsonModel.generateContent(
                    """
                    Strategic competitor analysis for SEGMA.AI. Do NOT fabricate live follower counts or private stats; if none are provided, state clearly that live competitor API/scraping is not connected.
                    Competitor: $brandName ($username) on $platform
                    Public Observations: ${manualObservedNotes.ifBlank { "None provided (no live API connected)." }}
                    Our Brand Context: ${userBrandContext.ifBlank { "Digital brand differentiating on $platform" }}
                    """.trimIndent()
                )
            }
            val jsonStr = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Competitor analysis returned empty JSON.")
            val obj = JSONObject(jsonStr)
            CompetitorResearch(
                brandName = brandName,
                username = username,
                platform = platform,
                dataMode = dataMode,
                contentThemes = obj.optString("contentThemes"),
                publishingFrequency = obj.optString("publishingFrequency"),
                dominantFormats = obj.optString("dominantFormats"),
                observedMetricsSummary = obj.optString("observedMetricsSummary"),
                communicationStyle = obj.optString("communicationStyle"),
                strengths = obj.optString("strengths"),
                opportunities = obj.optString("opportunities"),
                differentiationStrategy = obj.optString("differentiationStrategy")
            )
        }
    }

    override suspend fun generateAudiencePersona(
        brief: String,
        forceRefresh: Boolean
    ): AiCachedResult<AudiencePersona> {
        return executeCachedGuarded(
            featureType = "audience_persona",
            normalizedKeyInput = brief,
            forceRefresh = forceRefresh,
            serializer = AiJsonCodecs::encodePersona,
            deserializer = AiJsonCodecs::decodePersona
        ) {
            val response = withTimeout(60_000L) {
                personaJsonModel.generateContent(
                    "Generate a concise, actionable target audience persona for this campaign brief:\n$brief"
                )
            }
            val jsonStr = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Persona generation returned empty JSON.")
            val obj = JSONObject(jsonStr)
            AudiencePersona(
                name = obj.optString("name", "Target Segment"),
                ageRange = obj.optString("ageRange", "18-24"),
                location = obj.optString("location", "Urban Digital"),
                interests = obj.optJSONArray("interests").toStringList(6),
                painPoints = obj.optString("painPoints"),
                preferredFormats = obj.optJSONArray("preferredFormats").toStringList(5),
                toneOfVoice = obj.optString("toneOfVoice"),
                feasibilityScore = obj.optLong("feasibilityScore", 82L).coerceIn(0L, 100L),
                summary = obj.optString("summary")
            )
        }
    }

    private suspend fun <T> executeCachedGuarded(
        featureType: String,
        normalizedKeyInput: String,
        forceRefresh: Boolean,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
        networkCall: suspend () -> T
    ): AiCachedResult<T> {
        return try {
            coordinator.executeCached(
                featureType = featureType,
                normalizedKeyInput = normalizedKeyInput,
                forceRefresh = forceRefresh,
                serializer = serializer,
                deserializer = deserializer,
                networkCall = networkCall
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            throw mapToTypedAiException(t, "Failed to complete AI request.")
        }
    }

    private fun mapToTypedAiException(t: Throwable, fallback: String): Throwable {
        if (t is AiLocalBudgetExceededException || t is AiQuotaExceededException) {
            return t
        }
        val parsed = GeminiErrorClassifier.classify(t, fallback)
        return if (parsed.isQuotaError) {
            AiQuotaExceededException(
                message = parsed.userMessage,
                retryAfterSeconds = parsed.retryAfterSeconds,
                cause = t,
                category = parsed.category,
                rawErrorDetails = parsed.rawErrorDetails
            )
        } else {
            t
        }
    }

    private fun parseStrategyJson(obj: JSONObject): GeneratedStrategyDraft = GeneratedStrategyDraft(
        campaignName = obj.optString("campaignName", "New AI Campaign"),
        brandName = obj.optString("brandName", "My Brand"),
        objective = obj.optString("objective", "Increase engagement and awareness"),
        targetAudience = obj.optString("targetAudience", "Gen Z & Young Professionals"),
        audienceTags = obj.optJSONArray("audienceTags").toStringList(4),
        feasibilityScore = obj.optInt("feasibilityScore", 85).coerceIn(0, 100),
        communicationAngle = obj.optString("communicationAngle"),
        platforms = obj.optJSONArray("platforms").toStringList(4).ifEmpty { listOf("Instagram", "TikTok") },
        recommendedFormats = obj.optJSONArray("recommendedFormats").toStringList(4),
        uploadStrategy = obj.optString("uploadStrategy"),
        contentCalendar = obj.optString("contentCalendar"),
        captionsAndHooks = obj.optString("captionsAndHooks"),
        hashtags = obj.optJSONArray("hashtags").toStringList(10),
        audioRecommendation = obj.optString("audioRecommendation"),
        contentPillars = obj.optJSONArray("contentPillars").toStringList(5)
    )

    private fun JSONArray?.toStringList(max: Int): List<String> {
        if (this == null) return emptyList()
        return (0 until minOf(length(), max))
            .mapNotNull { idx -> optString(idx).takeIf { it.isNotBlank() } }
    }

    companion object {
        const val MODEL_GEMINI_FLASH = "gemini-3.8-flash"
        const val DEFAULT_MAX_CONTEXT_MESSAGES = 8

        /**
         * Normalizes prior conversation pairs for Gemini startChat(history):
         * - Removes blank messages
         * - Collapses consecutive same-role messages
         * - Ensures history starts with a user turn and ends with a model turn
         * - Retains up to [maxMessages] recent messages (default 8 = 4 user + 4 model exchanges)
         */
        internal fun normalizeHistoryPairs(
            historyMessages: List<Pair<Boolean, String>>,
            maxMessages: Int = DEFAULT_MAX_CONTEXT_MESSAGES
        ): List<Pair<Boolean, String>> {
            val cleaned = cleanAlternatingTurns(historyMessages)
            var bounded = cleaned.takeLast(maxMessages)
            while (bounded.isNotEmpty() && !bounded.first().first) {
                bounded = bounded.drop(1)
            }
            while (bounded.isNotEmpty() && bounded.last().first) {
                bounded = bounded.dropLast(1)
            }
            return bounded
        }

        /**
         * When older turns are trimmed beyond [maxMessages], prepends the earliest user campaign
         * objective locally to the current prompt so Segma Agent keeps objective context with 0 extra API calls.
         */
        internal fun buildAnchoredPromptIfTrimmed(
            historyMessages: List<Pair<Boolean, String>>,
            userPrompt: String,
            maxMessages: Int = DEFAULT_MAX_CONTEXT_MESSAGES
        ): String {
            val cleaned = cleanAlternatingTurns(historyMessages)
            if (cleaned.size <= maxMessages) return userPrompt
            val earliestObjective = cleaned.firstOrNull { it.first }?.second
                ?.replace(Regex("\\s+"), " ")
                ?.take(140)
                ?.takeIf { it.isNotBlank() }
                ?: return userPrompt
            return "[Initial campaign context: $earliestObjective]\n\n$userPrompt"
        }

        private fun cleanAlternatingTurns(
            historyMessages: List<Pair<Boolean, String>>
        ): List<Pair<Boolean, String>> {
            val cleaned = mutableListOf<Pair<Boolean, String>>()
            for ((isUser, rawText) in historyMessages) {
                val trimmed = rawText.trim()
                if (trimmed.isEmpty()) continue
                if (cleaned.isNotEmpty() && cleaned.last().first == isUser) {
                    cleaned[cleaned.lastIndex] = isUser to trimmed
                } else {
                    cleaned.add(isUser to trimmed)
                }
            }
            return cleaned
        }
    }
}
