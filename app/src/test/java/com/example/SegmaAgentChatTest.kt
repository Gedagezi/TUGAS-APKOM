package com.example

import com.example.ai.AiCachedResult
import com.example.ai.AiJsonCodecs
import com.example.ai.AiLocalBudgetExceededException
import com.example.ai.AiQuotaExceededException
import com.example.ai.AiRequestCoordinator
import com.example.ai.AiUsageSnapshot
import com.example.ai.DEFAULT_CACHE_TTL_MS
import com.example.ai.DEFAULT_DAILY_APP_AI_BUDGET
import com.example.ai.GeminiErrorCategory
import com.example.ai.GeminiErrorClassifier
import com.example.ai.InMemoryAiResponseCacheStore
import com.example.ai.InMemoryAiUsageTracker
import com.example.ai.SegmaAiClient
import com.example.ai.SegmaAiRepository
import com.example.ai.SegmaStrategyLocalExtractor
import com.example.data.SegmaDataRepository
import com.example.data.SegmaDemoCatalog
import com.example.model.AudiencePersona
import com.example.model.Campaign
import com.example.model.CampaignEvaluation
import com.example.model.CampaignStatus
import com.example.model.CompetitorResearch
import com.example.model.GeneratedStrategyDraft
import com.example.model.TrendBookmark
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.ActionRequestGuard
import com.example.ui.SegmaActionKeys
import com.example.ui.SegmaViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SegmaAgentChatTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `TEST 1 - initializing ViewModel and viewing saved or demo data triggers zero AI calls`() = runTest(testDispatcher) {
        val fakeAi = RecordingFakeAiClient()
        val dataRepo = NoOpDataRepository(
            campaigns = SegmaDemoCatalog.initialDemoCampaigns,
            evaluations = listOf(CampaignEvaluation(id = "ev-1", campaignId = "demo_eco_2026", overallScore = 88))
        )

        val viewModel = SegmaViewModel(
            firestoreRepo = dataRepo,
            aiRepo = fakeAi,
            currentUserId = "test-uid",
            autoSeedProfile = false
        )
        advanceUntilIdle()

        assertEquals(0, fakeAi.totalDispatches)
        assertEquals(0, viewModel.aiUsageSnapshot.value.usedIn24h)
        assertEquals(DEFAULT_DAILY_APP_AI_BUDGET, viewModel.aiUsageSnapshot.value.dailyLimit)
    }

    @Test
    fun `TEST 2 - single user message triggers exactly 1 Gemini call and extracts strategy card locally`() = runTest(testDispatcher) {
        val fakeAi = RecordingFakeAiClient()
        val viewModel = SegmaViewModel(
            firestoreRepo = NoOpDataRepository(),
            aiRepo = fakeAi,
            currentUserId = "test-uid",
            autoSeedProfile = false
        )

        viewModel.sendAgentPrompt(
            prompt = "Create an eco-friendly campus tumbler launch strategy on Instagram Reels and TikTok",
            autoBuildStrategyCard = true
        )
        advanceUntilIdle()

        // Exactly 1 AI request dispatched; strategy card was extracted deterministically with 0 extra AI calls
        assertEquals(1, fakeAi.recordedChatCalls.size)
        assertEquals(1, fakeAi.totalDispatches)
        val draft = viewModel.agentMessages.value.last().strategyDraft
        assertNotNull(draft)
        assertTrue(draft!!.platforms.contains("Instagram"))
        assertTrue(draft.platforms.contains("TikTok"))

        // Manual "Build Card" also uses 0 AI calls
        viewModel.generateStrategyCardForLastExchange()
        assertEquals(1, fakeAi.totalDispatches)
    }

    @Test
    fun `TEST 3 - rapid repeated clicks and concurrent actions trigger only 1 AI request`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        val fakeAi = RecordingFakeAiClient().apply {
            pendingGate = gate
        }
        val viewModel = SegmaViewModel(
            firestoreRepo = NoOpDataRepository(),
            aiRepo = fakeAi,
            currentUserId = "test-uid",
            autoSeedProfile = false
        )

        viewModel.sendAgentPrompt("Create a Q4 product launch roadmap")
        runCurrent()
        assertTrue(viewModel.isAgentGenerating.value)

        // Repeated Send ignored while generating
        viewModel.sendAgentPrompt("Second overlapping prompt")
        viewModel.sendAgentPrompt("Third overlapping prompt")
        assertEquals(1, fakeAi.recordedChatCalls.size)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.isAgentGenerating.value)

        // Rapid builder clicks while loading
        val builderGate = CompletableDeferred<Unit>()
        fakeAi.pendingGate = builderGate
        viewModel.generateCampaignPlanInBuilder(
            name = "Launch", brandName = "Brand", objective = "Awareness",
            description = "Desc", targetAudience = "Gen Z", platforms = listOf("TikTok"),
            duration = "14 Days", budget = "Organic"
        )
        runCurrent()
        assertTrue(viewModel.builderDraftState.value is UiState.Loading)

        viewModel.generateCampaignPlanInBuilder(
            name = "Launch", brandName = "Brand", objective = "Awareness",
            description = "Desc", targetAudience = "Gen Z", platforms = listOf("TikTok"),
            duration = "14 Days", budget = "Organic"
        )
        assertEquals(1, fakeAi.builderNetworkCalls)
        builderGate.complete(Unit)
        advanceUntilIdle()
        assertTrue(viewModel.builderDraftState.value is UiState.Success)
    }

    @Test
    fun `TEST 4 - history bounded to 8 messages and retains earliest objective locally when trimmed`() {
        val longHistory = (1..10).flatMap { turn ->
            listOf(
                true to if (turn == 1) "Promote sustainable bamboo matcha kit to Gen Z students" else "User turn $turn",
                false to "Model reply $turn"
            )
        }

        val normalized = SegmaAiRepository.normalizeHistoryPairs(longHistory)
        assertEquals(8, normalized.size)
        assertEquals("User turn 7", normalized.first().second)
        assertEquals("Model reply 10", normalized.last().second)

        val anchored = SegmaAiRepository.buildAnchoredPromptIfTrimmed(
            historyMessages = longHistory,
            userPrompt = "Write 3 Day-5 hooks"
        )
        assertTrue(anchored.contains("Promote sustainable bamboo matcha kit to Gen Z students"))
        assertTrue(anchored.contains("Write 3 Day-5 hooks"))
    }

    @Test
    fun `TEST 5 - 24h caching reuses results with 0 API calls and zero budget usage while forceRefresh refreshes`() = runTest {
        var nowMs = 1_000_000L
        val tracker = InMemoryAiUsageTracker(dailyLimit = 12, clock = { nowMs })
        val cache = InMemoryAiResponseCacheStore(clock = { nowMs })
        val coordinator = AiRequestCoordinator(usageTracker = tracker, cacheStore = cache, clock = { nowMs })

        var networkCount = 0
        val first = coordinator.executeCached(
            featureType = "competitor_research",
            normalizedKeyInput = "MatchaCo|@matchaco|Instagram||",
            forceRefresh = false,
            serializer = AiJsonCodecs::encodeCompetitor,
            deserializer = AiJsonCodecs::decodeCompetitor
        ) {
            networkCount++
            CompetitorResearch(brandName = "MatchaCo", username = "@matchaco", platform = "Instagram", differentiationStrategy = "Use POV reels")
        }
        assertFalse(first.fromCache)
        assertEquals(1, networkCount)
        assertEquals(1, tracker.currentSnapshot().usedIn24h)

        // Identical request with different whitespace/casing hits 24h cache -> 0 network calls, 0 budget used
        val second = coordinator.executeCached(
            featureType = "competitor_research",
            normalizedKeyInput = "  matchaco | @MATCHACO | instagram || ",
            forceRefresh = false,
            serializer = AiJsonCodecs::encodeCompetitor,
            deserializer = AiJsonCodecs::decodeCompetitor
        ) {
            networkCount++
            CompetitorResearch(brandName = "Should not run")
        }
        assertTrue(second.fromCache)
        assertEquals("MatchaCo", second.data.brandName)
        assertEquals(1, networkCount)
        assertEquals(1, tracker.currentSnapshot().usedIn24h)

        // Explicit Refresh bypasses cache -> 1 fresh call
        val refreshed = coordinator.executeCached(
            featureType = "competitor_research",
            normalizedKeyInput = "MatchaCo|@matchaco|Instagram||",
            forceRefresh = true,
            serializer = AiJsonCodecs::encodeCompetitor,
            deserializer = AiJsonCodecs::decodeCompetitor
        ) {
            networkCount++
            CompetitorResearch(brandName = "MatchaCo", differentiationStrategy = "Refreshed differentiation")
        }
        assertFalse(refreshed.fromCache)
        assertEquals("Refreshed differentiation", refreshed.data.differentiationStrategy)
        assertEquals(2, networkCount)
        assertEquals(2, tracker.currentSnapshot().usedIn24h)

        // Advance past 24h TTL -> cache expires and rolling 24h usage window resets
        nowMs += DEFAULT_CACHE_TTL_MS + 1_000L
        assertEquals(0, tracker.currentSnapshot().usedIn24h)
    }

    @Test
    fun `TEST 6 - saved evaluation report is reused with zero AI calls unless user taps Regenerate`() = runTest(testDispatcher) {
        val campaign = SegmaDemoCatalog.initialDemoCampaigns.first()
        val savedEval = CampaignEvaluation(
            id = "saved-eval-1",
            campaignId = campaign.id,
            campaignName = campaign.name,
            executionSummary = "Saved Firestore summary",
            overallScore = 91L
        )
        val fakeAi = RecordingFakeAiClient()
        val viewModel = SegmaViewModel(
            firestoreRepo = NoOpDataRepository(
                campaigns = listOf(campaign),
                evaluations = listOf(savedEval)
            ),
            aiRepo = fakeAi,
            currentUserId = "test-uid",
            autoSeedProfile = false
        )
        // Collect evaluationsState briefly so WhileSubscribed upstream populates
        val job = backgroundScope.launch { viewModel.evaluationsState.collect {} }
        runCurrent()

        viewModel.runCampaignEvaluation(campaign, forceRefresh = false)
        advanceUntilIdle()

        assertEquals(0, fakeAi.evaluationNetworkCalls)
        val draft = (viewModel.evaluationDraftState.value as UiState.Success).data
        assertEquals("Saved Firestore summary", draft?.executionSummary)
        assertTrue(viewModel.evaluationCacheNotice.value.orEmpty().contains("0 AI requests"))

        // Explicit Regenerate calls AI once
        viewModel.runCampaignEvaluation(campaign, forceRefresh = true)
        advanceUntilIdle()
        assertEquals(1, fakeAi.evaluationNetworkCalls)
        job.cancel()
    }

    @Test
    fun `TEST 7 - ambiguous HTTP 429 stops retries, preserves prompt and details, and does not claim daily exhaustion`() = runTest(testDispatcher) {
        val rawApiMessage = "HTTP 429 RESOURCE_EXHAUSTED: Quota exceeded for quota metric. Please retry in 45s."
        val parsed = GeminiErrorClassifier.classify(
            IllegalStateException(rawApiMessage),
            "Fallback"
        )
        assertTrue(parsed.isQuotaError)
        assertEquals(GeminiErrorCategory.AMBIGUOUS_RATE_LIMIT, parsed.category)
        assertEquals(45, parsed.retryAfterSeconds)
        assertTrue(parsed.userMessage.contains("~45s"))
        assertTrue(parsed.userMessage.contains(rawApiMessage))
        assertFalse(parsed.userMessage.contains("daily", ignoreCase = true))
        assertTrue(parsed.userMessage.contains("saved", ignoreCase = true))

        var nowMs = 5_000_000L
        val tracker = InMemoryAiUsageTracker(dailyLimit = 12, clock = { nowMs })
        val cache = InMemoryAiResponseCacheStore(clock = { nowMs })
        val coordinator = AiRequestCoordinator(usageTracker = tracker, cacheStore = cache, clock = { nowMs })

        // Simulated 429 failure in coordinator -> sets cooldown, does NOT cache failure, preserves non-daily category
        var attempts = 0
        val failed = runCatching {
            coordinator.executeCached<String>(
                featureType = "audience_persona",
                normalizedKeyInput = "Matcha cafe",
                forceRefresh = false,
                serializer = { it },
                deserializer = { it }
            ) {
                attempts++
                throw IllegalStateException("429 RESOURCE_EXHAUSTED retry after 30s")
            }
        }
        assertTrue(failed.isFailure)
        assertEquals(1, attempts) // No automatic retry!
        assertNull(cache.getString(AiRequestCoordinator.buildCacheKey("audience_persona", "Matcha cafe")))
        assertNotNull(tracker.currentSnapshot().quotaCooldownUntilMs)
        assertEquals(GeminiErrorCategory.AMBIGUOUS_RATE_LIMIT, tracker.currentSnapshot().lastQuotaCategory)

        // Immediate retry blocked locally by cooldown without hitting network
        val duringCooldown = runCatching {
            coordinator.executeCached<String>(
                featureType = "audience_persona",
                normalizedKeyInput = "Another brief",
                forceRefresh = false,
                serializer = { it },
                deserializer = { it }
            ) {
                attempts++
                "Should not run"
            }
        }
        val cooldownException = duringCooldown.exceptionOrNull() as? AiQuotaExceededException
        assertNotNull(cooldownException)
        assertFalse(cooldownException!!.message.contains("daily", ignoreCase = true))
        assertEquals(1, attempts)

        // ViewModel rate-limit error handling preserves user prompt bubble and allows manual retry later
        val fakeAi = RecordingFakeAiClient().apply {
            throwQuotaNextCalls = 1
        }
        val viewModel = SegmaViewModel(
            firestoreRepo = NoOpDataRepository(),
            aiRepo = fakeAi,
            currentUserId = "test-uid",
            autoSeedProfile = false
        )
        val prompt = "Launch eco packaging TikTok series"
        viewModel.sendAgentPrompt(prompt)
        advanceUntilIdle()

        assertTrue(viewModel.agentError.value.orEmpty().contains("Requests Per Minute (RPM)"))
        assertTrue(viewModel.agentError.value.orEmpty().contains("~20s"))
        assertTrue(viewModel.canRetryAgent.value)
        assertEquals(1, viewModel.agentMessages.value.count { it.isUser && it.text == prompt })
        assertEquals(1, fakeAi.recordedChatCalls.size) // Zero automatic retries!

        // Manual retry succeeds without duplicating user bubble
        viewModel.retryLastAgentPrompt()
        advanceUntilIdle()
        assertNull(viewModel.agentError.value)
        assertEquals(1, viewModel.agentMessages.value.count { it.isUser && it.text == prompt })
        assertFalse(viewModel.agentMessages.value.last().isUser)
    }

    @Test
    fun `TEST 8 - local 12 request daily budget blocks extra live calls while preserving cached data`() = runTest {
        var nowMs = 10_000_000L
        val tracker = InMemoryAiUsageTracker(dailyLimit = 12, clock = { nowMs })
        val cache = InMemoryAiResponseCacheStore(clock = { nowMs })
        val coordinator = AiRequestCoordinator(usageTracker = tracker, cacheStore = cache, clock = { nowMs })

        // Pre-populate 1 cached item on call #1, then dispatch 11 more calls to reach 12/12
        coordinator.executeCached(
            featureType = "audience_persona",
            normalizedKeyInput = "cached brief",
            forceRefresh = false,
            serializer = { it },
            deserializer = { it }
        ) { "Cached Persona JSON" }

        repeat(11) { idx ->
            coordinator.prepareLiveChatDispatch()
            coordinator.finishLiveChatDispatch()
            nowMs += 1_000L
        }

        val snap = tracker.currentSnapshot()
        assertEquals(12, snap.usedIn24h)
        assertTrue(snap.isLimitReached)

        // 13th live call is blocked locally before touching Gemini
        val blocked = runCatching { coordinator.prepareLiveChatDispatch() }
        assertTrue(blocked.exceptionOrNull() is AiLocalBudgetExceededException)

        // Cached result STILL succeeds even when daily budget is exhausted!
        val cachedHit = coordinator.executeCached(
            featureType = "audience_persona",
            normalizedKeyInput = "cached brief",
            forceRefresh = false,
            serializer = { it },
            deserializer = { it }
        ) { error("Must not invoke network") }
        assertTrue(cachedHit.fromCache)
        assertEquals("Cached Persona JSON", cachedHit.data)
    }

    @Test
    fun `TEST 9 - multi-turn context continuity and normal alternating history cleanup`() = runTest(testDispatcher) {
        val raw = listOf(
            false to "Orphan model",
            true to "First prompt",
            true to "Retried first prompt",
            false to "First strategist reply",
            true to "Second prompt",
            false to "Second strategist reply",
            true to "Trailing unanswered"
        )
        val normalized = SegmaAiRepository.normalizeHistoryPairs(raw)
        assertEquals(
            listOf(
                true to "Retried first prompt",
                false to "First strategist reply",
                true to "Second prompt",
                false to "Second strategist reply"
            ),
            normalized
        )

        val fakeAi = RecordingFakeAiClient()
        val viewModel = SegmaViewModel(
            firestoreRepo = NoOpDataRepository(),
            aiRepo = fakeAi,
            currentUserId = "test-uid",
            autoSeedProfile = false
        )
        val prompts = listOf(
            "Help me create a campaign to increase awareness of a new product.",
            "Target Gen Z university students on Instagram Reels and TikTok.",
            "Write 3 scroll-stopping hooks and captions for Day 1."
        )
        prompts.forEach {
            viewModel.sendAgentPrompt(it)
            advanceUntilIdle()
        }
        assertEquals(3, fakeAi.recordedChatCalls.size)
        assertEquals(4, fakeAi.recordedChatCalls[2].first.size)
        assertEquals(prompts[2], fakeAi.recordedChatCalls[2].second)
    }

    @Test
    fun `TEST 10 - local strategy extraction and JSON cache codecs preserve campaign draft fields`() {
        val draft = SegmaStrategyLocalExtractor.extractFromAgentReply(
            userPrompt = "Eco-friendly matcha cafe launch for Gen Z university students on TikTok",
            agentReply = """
                Campaign: Matcha Campus Pop-Up
                1. Audience & Feasibility Score: 89/100 for eco-conscious Gen Z students.
                2. Angle & Upload Strategy: Post 5x/week on TikTok and Instagram Reels at 6:30 PM.
                3. Hook: Bring your reusable cup for 20% off!
                4. Hashtags: #CampusMatcha #EcoSip #GenZGreen
                5. Audio: Lo-fi upbeat acoustic groove
            """.trimIndent()
        )
        assertEquals("Matcha Campus Pop-Up", draft.campaignName)
        assertEquals(89, draft.feasibilityScore)
        assertTrue(draft.hashtags.contains("#CampusMatcha"))
        assertTrue(draft.platforms.contains("TikTok"))

        val encoded = AiJsonCodecs.encodeStrategyDraft(draft)
        val decoded = AiJsonCodecs.decodeStrategyDraft(encoded)
        assertEquals(draft, decoded)
    }

    @Test
    fun `TEST 11 - synchronous back-to-back calls before coroutine dispatch execute only once per action`() = runTest(testDispatcher) {
        val fakeAi = RecordingFakeAiClient()
        val dataRepo = NoOpDataRepository()
        val viewModel = SegmaViewModel(
            firestoreRepo = dataRepo,
            aiRepo = fakeAi,
            currentUserId = "test-uid",
            autoSeedProfile = false
        )

        val campaign = SegmaDemoCatalog.initialDemoCampaigns.first()

        // Invoke back-to-back BEFORE runCurrent()/advanceUntilIdle() so synchronous ActionRequestGuard is tested
        viewModel.generateCampaignPlanInBuilder(
            name = "Plan", brandName = "Brand", objective = "Obj",
            description = "Desc", targetAudience = "Gen Z", platforms = listOf("TikTok"),
            duration = "7 Days", budget = "Organic"
        )
        viewModel.generateCampaignPlanInBuilder(
            name = "Plan", brandName = "Brand", objective = "Obj",
            description = "Desc", targetAudience = "Gen Z", platforms = listOf("TikTok"),
            duration = "7 Days", budget = "Organic"
        )

        viewModel.runCompetitorResearch("Comp", "@comp", "TikTok", "", "")
        viewModel.runCompetitorResearch("Comp", "@comp", "TikTok", "", "")

        viewModel.generateAndPreviewPersona("Eco students")
        viewModel.generateAndPreviewPersona("Eco students")

        viewModel.refreshCampaignAiAnalysis(campaign)
        viewModel.refreshCampaignAiAnalysis(campaign)

        viewModel.saveFullCampaign(campaign)
        viewModel.saveFullCampaign(campaign)

        viewModel.toggleBookmark("trend-1", "Title", "Cat", "Sum", "Rec", false)
        viewModel.toggleBookmark("trend-1", "Title", "Cat", "Sum", "Rec", false)
        // Different trend ID is a distinct action and must NOT be blocked
        viewModel.toggleBookmark("trend-2", "Title 2", "Cat", "Sum", "Rec", false)

        assertTrue(viewModel.isActionInFlight(SegmaActionKeys.BUILDER_GENERATE_PLAN))
        assertTrue(viewModel.isActionInFlight(SegmaActionKeys.COMPETITOR_RESEARCH))
        assertTrue(viewModel.isActionInFlight(SegmaActionKeys.AUDIENCE_PERSONA))
        assertTrue(viewModel.isActionInFlight(SegmaActionKeys.CAMPAIGN_AI_ANALYSIS))
        assertTrue(viewModel.isActionInFlight(SegmaActionKeys.SAVE_FULL_CAMPAIGN))
        assertTrue(viewModel.isActionInFlight(SegmaActionKeys.toggleBookmark("trend-1")))
        assertTrue(viewModel.isActionInFlight(SegmaActionKeys.toggleBookmark("trend-2")))

        advanceUntilIdle()

        assertEquals(1, fakeAi.builderNetworkCalls)
        assertEquals(1, fakeAi.competitorNetworkCalls)
        assertEquals(1, fakeAi.personaNetworkCalls)
        assertEquals(1, fakeAi.analysisNetworkCalls)
        assertEquals(1, dataRepo.saveCampaignCalls)
        assertEquals(listOf("trend-1", "trend-2"), dataRepo.toggledBookmarkIds)
        assertTrue(viewModel.activeActionRequests.value.isEmpty())
    }

    @Test
    fun `TEST 12 - ActionRequestGuard releases key after failure so subsequent call can proceed`() = runTest(testDispatcher) {
        val guard = ActionRequestGuard()
        val fakeAi = RecordingFakeAiClient().apply {
            failNextPersonaCalls = 1
        }
        val viewModel = SegmaViewModel(
            firestoreRepo = NoOpDataRepository(),
            aiRepo = fakeAi,
            currentUserId = "test-uid",
            autoSeedProfile = false,
            requestGuard = guard
        )

        viewModel.generateAndPreviewPersona("Brief A")
        advanceUntilIdle()

        assertTrue(viewModel.personaDraftState.value is UiState.Error)
        assertFalse(guard.isInFlight(SegmaActionKeys.AUDIENCE_PERSONA))

        // Subsequent attempt succeeds after failure released the guard
        viewModel.generateAndPreviewPersona("Brief A")
        advanceUntilIdle()

        assertTrue(viewModel.personaDraftState.value is UiState.Success)
        assertEquals(2, fakeAi.personaNetworkCalls)
    }

    @Test
    fun `TEST 13 - GeminiErrorClassifier accurately distinguishes RPM, RPD, TPM, TPD, and temporary capacity errors`() {
        // 1. RPM (Requests Per Minute)
        val rpmMsg = "HTTP 429 RESOURCE_EXHAUSTED: Quota exceeded for quota metric 'GenerateRequestsPerMinutePerProjectPerModel-FreeTier'. Please retry in 18s."
        val rpm = GeminiErrorClassifier.classify(IllegalStateException(rpmMsg), "Fallback")
        assertTrue(rpm.isQuotaError)
        assertEquals(GeminiErrorCategory.RPM_RATE_LIMIT, rpm.category)
        assertEquals(18, rpm.retryAfterSeconds)
        assertTrue(rpm.userMessage.contains("Requests Per Minute (RPM)"))
        assertTrue(rpm.userMessage.contains("~18s"))
        assertFalse(rpm.userMessage.contains("daily", ignoreCase = true))
        assertTrue(rpm.rawErrorDetails.contains(rpmMsg))
        assertTrue(rpm.userMessage.contains(rpmMsg))

        // 2. RPD (Requests Per Day)
        val rpdMsg = "HTTP 429 RESOURCE_EXHAUSTED: Quota exceeded for quotaId GenerateRequestsPerDayPerProjectPerModel-FreeTier, quotaValue: 20."
        val rpd = GeminiErrorClassifier.classify(IllegalStateException(rpdMsg), "Fallback")
        assertTrue(rpd.isQuotaError)
        assertEquals(GeminiErrorCategory.RPD_DAILY_QUOTA, rpd.category)
        assertNull(rpd.retryAfterSeconds)
        assertTrue(rpd.userMessage.contains("Requests Per Day (RPD)"))
        assertTrue(rpd.userMessage.contains("wait for the provider quota reset", ignoreCase = true))
        assertTrue(rpd.userMessage.contains(rpdMsg))

        // 3. TPM (Tokens Per Minute)
        val tpmMsg = "HTTP 429 RESOURCE_EXHAUSTED: InputTokensPerMinutePerProjectPerModel limit exceeded. Retry-After: 25"
        val tpm = GeminiErrorClassifier.classify(IllegalStateException(tpmMsg), "Fallback")
        assertTrue(tpm.isQuotaError)
        assertEquals(GeminiErrorCategory.TPM_RATE_LIMIT, tpm.category)
        assertEquals(25, tpm.retryAfterSeconds)
        assertTrue(tpm.userMessage.contains("Tokens Per Minute (TPM)"))
        assertTrue(tpm.userMessage.contains("~25s"))
        assertFalse(tpm.userMessage.contains("daily", ignoreCase = true))
        assertTrue(tpm.userMessage.contains(tpmMsg))

        // 4. TPD (Tokens Per Day)
        val tpdMsg = "HTTP 429 RESOURCE_EXHAUSTED: Daily quota exceeded for TokensPerDayPerProjectPerModel."
        val tpd = GeminiErrorClassifier.classify(IllegalStateException(tpdMsg), "Fallback")
        assertTrue(tpd.isQuotaError)
        assertEquals(GeminiErrorCategory.TPD_DAILY_QUOTA, tpd.category)
        assertTrue(tpd.userMessage.contains("Tokens Per Day (TPD)"))
        assertTrue(tpd.userMessage.contains("wait for the provider quota reset", ignoreCase = true))
        assertTrue(tpd.userMessage.contains(tpdMsg))

        // 5a. Temporary capacity via 503 UNAVAILABLE
        val cap503Msg = "HTTP 503 UNAVAILABLE: The model is currently overloaded due to high demand. Please retry in 12.4s."
        val cap503 = GeminiErrorClassifier.classify(IllegalStateException(cap503Msg), "Fallback")
        assertTrue(cap503.isQuotaError)
        assertEquals(GeminiErrorCategory.TEMPORARY_CAPACITY, cap503.category)
        assertEquals(13, cap503.retryAfterSeconds)
        assertTrue(cap503.userMessage.contains("high demand or limited model capacity"))
        assertTrue(cap503.userMessage.contains("~13s"))
        assertFalse(cap503.userMessage.contains("daily", ignoreCase = true))
        assertTrue(cap503.userMessage.contains(cap503Msg))

        // 5b. Temporary capacity via 429 RESOURCE_EXHAUSTED
        val cap429Msg = "HTTP 429 RESOURCE_EXHAUSTED: Insufficient model capacity right now. Please retry in 10s."
        val cap429 = GeminiErrorClassifier.classify(IllegalStateException(cap429Msg), "Fallback")
        assertEquals(GeminiErrorCategory.TEMPORARY_CAPACITY, cap429.category)
        assertEquals(10, cap429.retryAfterSeconds)
        assertFalse(cap429.userMessage.contains("daily", ignoreCase = true))
    }

    @Test
    fun `TEST 14 - retry-after extraction handles retryDelay, protobuf seconds, minutes, and cause chains`() {
        assertEquals(20, GeminiErrorClassifier.extractRetryAfterSeconds("""retryDelay: "19.2s""""))
        assertEquals(60, GeminiErrorClassifier.extractRetryAfterSeconds("Retry-After: 60"))
        assertEquals(90, GeminiErrorClassifier.extractRetryAfterSeconds("Please try again in 1.5 minutes"))
        assertEquals(32, GeminiErrorClassifier.extractRetryAfterSeconds("retry_delay { seconds: 32 }"))
        assertNull(GeminiErrorClassifier.extractRetryAfterSeconds("HTTP 429 Too Many Requests"))

        val root = IllegalStateException("quotaId: GenerateRequestsPerMinutePerProjectPerModel, retryDelay: \"14s\"")
        val wrapper = RuntimeException("FirebaseAI request failed", root)
        val parsed = GeminiErrorClassifier.classify(wrapper, "Fallback")
        assertEquals(GeminiErrorCategory.RPM_RATE_LIMIT, parsed.category)
        assertEquals(14, parsed.retryAfterSeconds)
        assertTrue(parsed.rawErrorDetails.contains("FirebaseAI request failed"))
        assertTrue(parsed.rawErrorDetails.contains("GenerateRequestsPerMinutePerProjectPerModel"))
    }

    @Test
    fun `TEST 15 - campaign builder preserves existing draft and reports accurate error when rate limited`() = runTest(testDispatcher) {
        val fakeAi = RecordingFakeAiClient().apply {
            builderError = IllegalStateException("HTTP 429 RESOURCE_EXHAUSTED: TokensPerMinute limit exceeded. Please retry in 24s.")
        }
        val viewModel = SegmaViewModel(
            firestoreRepo = NoOpDataRepository(),
            aiRepo = fakeAi,
            currentUserId = "test-uid",
            autoSeedProfile = false
        )

        // First generate a valid campaign draft
        fakeAi.builderError = null
        viewModel.generateCampaignPlanInBuilder(
            name = "Eco Sip", brandName = "GreenCup", objective = "Awareness",
            description = "Campus launch", targetAudience = "Gen Z", platforms = listOf("TikTok"),
            duration = "14 Days", budget = "Organic"
        )
        advanceUntilIdle()
        assertTrue(viewModel.builderDraftState.value is UiState.Success)

        // Now simulate a temporary TPM failure on Refresh; ViewModel surfaces accurate TPM message with retry duration & no auto-retry
        fakeAi.builderError = IllegalStateException("HTTP 429 RESOURCE_EXHAUSTED: TokensPerMinute limit exceeded. Please retry in 24s.")
        viewModel.generateCampaignPlanInBuilder(
            name = "Eco Sip", brandName = "GreenCup", objective = "Awareness",
            description = "Campus launch", targetAudience = "Gen Z", platforms = listOf("TikTok"),
            duration = "14 Days", budget = "Organic",
            forceRefresh = true
        )
        advanceUntilIdle()

        val errState = viewModel.builderDraftState.value as? UiState.Error
        assertNotNull(errState)
        assertTrue(errState!!.message.contains("Tokens Per Minute (TPM)"))
        assertTrue(errState.message.contains("~24s"))
        assertFalse(errState.message.contains("daily", ignoreCase = true))
        assertEquals(2, fakeAi.builderNetworkCalls) // 1 initial + 1 explicit refresh (no automatic retry)
    }

    private class RecordingFakeAiClient : SegmaAiClient {
        private val _usage = MutableStateFlow(AiUsageSnapshot(0, DEFAULT_DAILY_APP_AI_BUDGET))
        override val usageSnapshot: StateFlow<AiUsageSnapshot> = _usage

        val recordedChatCalls = mutableListOf<Pair<List<Pair<Boolean, String>>, String>>()
        var builderNetworkCalls = 0
        var evaluationNetworkCalls = 0
        var competitorNetworkCalls = 0
        var personaNetworkCalls = 0
        var analysisNetworkCalls = 0
        var totalDispatches = 0
        var failNextCalls: Int = 0
        var failNextPersonaCalls: Int = 0
        var throwQuotaNextCalls: Int = 0
        var builderError: Throwable? = null
        var pendingGate: CompletableDeferred<Unit>? = null
        private var replyCounter = 0

        override fun streamAgentReply(
            historyMessages: List<Pair<Boolean, String>>,
            userPrompt: String
        ): Flow<String> = flow {
            totalDispatches++
            recordedChatCalls.add(historyMessages to userPrompt)
            pendingGate?.let {
                pendingGate = null
                it.await()
            }
            if (throwQuotaNextCalls > 0) {
                throwQuotaNextCalls--
                val raw = IllegalStateException("HTTP 429 RESOURCE_EXHAUSTED: RequestsPerMinute limit exceeded. Please retry in 20s.")
                val classified = GeminiErrorClassifier.classify(raw, "Rate limited")
                throw AiQuotaExceededException(
                    message = classified.userMessage,
                    retryAfterSeconds = classified.retryAfterSeconds,
                    cause = raw,
                    category = classified.category,
                    rawErrorDetails = classified.rawErrorDetails
                )
            }
            if (failNextCalls > 0) {
                failNextCalls--
                throw IllegalStateException("Simulated network failure")
            }
            _usage.value = _usage.value.copy(usedIn24h = _usage.value.usedIn24h + 1)
            replyCounter++
            delay(10)
            emit("Strategy reply #$replyCounter for: $userPrompt on Instagram Reels & TikTok #SegmaAI")
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
            builderNetworkCalls++
            totalDispatches++
            pendingGate?.let {
                pendingGate = null
                it.await()
            }
            builderError?.let { throw it }
            return AiCachedResult(
                data = GeneratedStrategyDraft(
                    campaignName = name,
                    brandName = brandName,
                    objective = objective,
                    targetAudience = targetAudience,
                    audienceTags = listOf("Gen Z"),
                    feasibilityScore = 88,
                    communicationAngle = "Creator-first storytelling",
                    platforms = platforms,
                    recommendedFormats = listOf("Reels", "TikTok"),
                    uploadStrategy = "4x/week",
                    contentCalendar = "Day 1: Hook",
                    captionsAndHooks = "Watch until the end!",
                    hashtags = listOf("#SegmaAI"),
                    audioRecommendation = "Lo-fi beat",
                    contentPillars = listOf("Tips", "Community")
                ),
                fromCache = false
            )
        }

        override suspend fun analyzeCampaignPerformance(
            campaign: Campaign,
            forceRefresh: Boolean
        ): AiCachedResult<String> {
            analysisNetworkCalls++
            totalDispatches++
            return AiCachedResult("Analysis", fromCache = false)
        }

        override suspend fun generateFollowUpContent(
            campaign: Campaign,
            forceRefresh: Boolean
        ): AiCachedResult<String> {
            totalDispatches++
            return AiCachedResult("Follow up", fromCache = false)
        }

        override suspend fun evaluateCampaign(
            campaign: Campaign,
            forceRefresh: Boolean
        ): AiCachedResult<CampaignEvaluation> {
            evaluationNetworkCalls++
            totalDispatches++
            return AiCachedResult(
                CampaignEvaluation(
                    campaignId = campaign.id,
                    campaignName = campaign.name,
                    executionSummary = "Fresh AI evaluation",
                    overallScore = 85L
                ),
                fromCache = false
            )
        }

        override suspend fun analyzeCompetitor(
            brandName: String,
            username: String,
            platform: String,
            manualObservedNotes: String,
            userBrandContext: String,
            forceRefresh: Boolean
        ): AiCachedResult<CompetitorResearch> {
            competitorNetworkCalls++
            totalDispatches++
            return AiCachedResult(CompetitorResearch(brandName = brandName, username = username, platform = platform), fromCache = false)
        }

        override suspend fun generateAudiencePersona(
            brief: String,
            forceRefresh: Boolean
        ): AiCachedResult<AudiencePersona> {
            personaNetworkCalls++
            totalDispatches++
            if (failNextPersonaCalls > 0) {
                failNextPersonaCalls--
                throw IllegalStateException("Persona network error")
            }
            return AiCachedResult(AudiencePersona(name = "Persona"), fromCache = false)
        }
    }

    private class NoOpDataRepository(
        private val campaigns: List<Campaign> = emptyList(),
        private val evaluations: List<CampaignEvaluation> = emptyList()
    ) : SegmaDataRepository {
        var saveCampaignCalls = 0
        val toggledBookmarkIds = mutableListOf<String>()
        override suspend fun ensureUserProfileAndSeedDemoData(user: FirebaseUser) = Result.success(Unit)
        override fun observeUserProfile(uid: String): Flow<UserProfile?> = flowOf(UserProfile(displayName = "Creator"))
        override fun observeCampaigns(uid: String): Flow<List<Campaign>> = flowOf(campaigns)
        override fun observePersonas(uid: String): Flow<List<AudiencePersona>> = emptyFlow()
        override fun observeEvaluations(uid: String): Flow<List<CampaignEvaluation>> = flowOf(evaluations)
        override fun observeCompetitors(uid: String): Flow<List<CompetitorResearch>> = emptyFlow()
        override fun observeBookmarks(uid: String): Flow<List<TrendBookmark>> = emptyFlow()
        override suspend fun updateProfileDetails(
            displayName: String,
            roleTitle: String,
            activeHandle: String,
            secondaryHandle: String
        ) = Result.success(Unit)

        override suspend fun updateSocialConnection(
            instagramConnected: Boolean,
            instagramHandle: String,
            tiktokConnected: Boolean,
            tiktokHandle: String
        ) = Result.success(Unit)

        override suspend fun saveCampaign(campaign: Campaign): Result<String> {
            saveCampaignCalls++
            return Result.success("camp-1")
        }
        override suspend fun updateCampaignStatus(campaignId: String, status: CampaignStatus) = Result.success(Unit)
        override suspend fun updateCampaignMetricsAndInsight(
            campaignId: String,
            reach: Long,
            views: Long,
            likes: Long,
            comments: Long,
            shares: Long,
            saves: Long,
            demoData: Boolean,
            dataSourceLabel: String,
            aiNextActionRecommendation: String
        ) = Result.success(Unit)

        override suspend fun deleteCampaign(campaignId: String) = Result.success(Unit)
        override suspend fun savePersona(persona: AudiencePersona) = Result.success("p-1")
        override suspend fun deletePersona(personaId: String) = Result.success(Unit)
        override suspend fun saveEvaluation(evaluation: CampaignEvaluation) = Result.success("e-1")
        override suspend fun saveCompetitorResearch(research: CompetitorResearch) = Result.success("c-1")
        override suspend fun toggleBookmark(
            trendId: String,
            title: String,
            category: String,
            summary: String,
            aiRecommendation: String,
            currentlyBookmarked: Boolean
        ): Result<Boolean> {
            toggledBookmarkIds.add(trendId)
            return Result.success(!currentlyBookmarked)
        }
    }
}
