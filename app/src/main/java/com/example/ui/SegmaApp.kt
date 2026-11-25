package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.ai.AiRequestCoordinator
import com.example.ai.SegmaAiRepository
import com.example.ai.SharedPreferencesAiResponseCacheStore
import com.example.ai.SharedPreferencesAiUsageTracker
import com.example.auth.authStateFlow
import com.example.data.SegmaFirestoreRepository
import com.example.model.Campaign
import com.example.model.UiState
import com.example.ui.components.SegmaBottomNavBar
import com.example.ui.components.SegmaTab
import com.example.ui.screens.AiEvaluationScreen
import com.example.ui.screens.AudienceProfilingScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CampaignBuilderScreen
import com.example.ui.screens.CampaignDetailScreen
import com.example.ui.screens.CampaignsHubScreen
import com.example.ui.screens.CompetitorResearchScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SegmaAgentScreen
import com.example.ui.screens.TrendRadarScreen
import com.example.ui.screens.WhatsTrendingScreen
import com.example.ui.theme.SegmaBackground
import com.example.ui.theme.SegmaLime
import com.example.ui.theme.SegmaNavy
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private object SegmaRoutes {
    const val CAMPAIGN_DETAIL = "campaign_detail/{campaignId}"
    const val CAMPAIGN_BUILDER = "campaign_builder?campaignId={campaignId}"
    const val AI_EVALUATION = "ai_evaluation"
    const val COMPETITOR_RESEARCH = "competitor_research"
    const val AUDIENCE_PROFILING = "audience_profiling"
    const val WHATS_TRENDING = "whats_trending/{editorialId}"

    fun campaignDetail(id: String) = "campaign_detail/$id"
    fun campaignBuilder(id: String? = null) =
        if (id.isNullOrBlank()) "campaign_builder" else "campaign_builder?campaignId=$id"
    fun whatsTrending(id: String) = "whats_trending/$id"
}

@Composable
fun SegmaApp() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { /* Reactive authStateFlow transitions automatically */ })
    } else {
        key(user.uid) {
            AuthenticatedSegmaShell(currentUserId = user.uid)
        }
    }
}

@Composable
private fun AuthenticatedSegmaShell(currentUserId: String) {
    val viewModel: SegmaViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val coordinator = AiRequestCoordinator(
                    usageTracker = SharedPreferencesAiUsageTracker(app),
                    cacheStore = SharedPreferencesAiResponseCacheStore(app)
                )
                SegmaViewModel(
                    firestoreRepo = SegmaFirestoreRepository(db),
                    aiRepo = SegmaAiRepository(coordinator = coordinator),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val activeBottomTabRoute = when {
        currentRoute == SegmaTab.HOME.route -> SegmaTab.HOME.route
        currentRoute == SegmaTab.AGENT.route -> SegmaTab.AGENT.route
        currentRoute == SegmaTab.INSIGHTS.route || currentRoute == SegmaRoutes.WHATS_TRENDING -> SegmaTab.INSIGHTS.route
        currentRoute == SegmaTab.CAMPAIGNS.route ||
            currentRoute == SegmaRoutes.CAMPAIGN_DETAIL ||
            currentRoute == SegmaRoutes.CAMPAIGN_BUILDER ||
            currentRoute == SegmaRoutes.AI_EVALUATION -> SegmaTab.CAMPAIGNS.route
        currentRoute == SegmaTab.PROFILE.route ||
            currentRoute == SegmaRoutes.AUDIENCE_PROFILING ||
            currentRoute == SegmaRoutes.COMPETITOR_RESEARCH -> SegmaTab.PROFILE.route
        else -> SegmaTab.HOME.route
    }

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val campaignsState by viewModel.campaignsState.collectAsStateWithLifecycle()
    val personasState by viewModel.personasState.collectAsStateWithLifecycle()
    val evaluationsState by viewModel.evaluationsState.collectAsStateWithLifecycle()
    val competitorsState by viewModel.competitorsState.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()

    val aiUsageSnapshot by viewModel.aiUsageSnapshot.collectAsStateWithLifecycle()
    val agentMessages by viewModel.agentMessages.collectAsStateWithLifecycle()
    val isAgentGenerating by viewModel.isAgentGenerating.collectAsStateWithLifecycle()
    val agentError by viewModel.agentError.collectAsStateWithLifecycle()
    val canRetryAgent by viewModel.canRetryAgent.collectAsStateWithLifecycle()
    val builderDraftState by viewModel.builderDraftState.collectAsStateWithLifecycle()
    val builderCacheNotice by viewModel.builderCacheNotice.collectAsStateWithLifecycle()
    val evaluationDraftState by viewModel.evaluationDraftState.collectAsStateWithLifecycle()
    val evaluationCacheNotice by viewModel.evaluationCacheNotice.collectAsStateWithLifecycle()
    val competitorDraftState by viewModel.competitorDraftState.collectAsStateWithLifecycle()
    val competitorCacheNotice by viewModel.competitorCacheNotice.collectAsStateWithLifecycle()
    val personaDraftState by viewModel.personaDraftState.collectAsStateWithLifecycle()
    val personaCacheNotice by viewModel.personaCacheNotice.collectAsStateWithLifecycle()
    val campaignAiInsightState by viewModel.campaignAiInsightState.collectAsStateWithLifecycle()
    val insightCacheNotice by viewModel.insightCacheNotice.collectAsStateWithLifecycle()
    val followUpState by viewModel.followUpContentState.collectAsStateWithLifecycle()
    val followUpCacheNotice by viewModel.followUpCacheNotice.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val campaigns = (campaignsState as? UiState.Success)?.data.orEmpty()
    val personas = (personasState as? UiState.Success)?.data.orEmpty()
    val bookmarks = (bookmarksState as? UiState.Success)?.data.orEmpty()
    val userProfile = (userProfileState as? UiState.Success)?.data

    fun navigateToTab(tab: SegmaTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = SegmaBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SegmaNavy,
                    contentColor = Color.White,
                    actionColor = SegmaLime
                )
            }
        },
        bottomBar = {
            SegmaBottomNavBar(
                currentRoute = activeBottomTabRoute,
                onTabSelected = { tab -> navigateToTab(tab) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = SegmaTab.HOME.route
            ) {
                composable(SegmaTab.HOME.route) {
                    DashboardScreen(
                        userProfileState = userProfileState,
                        campaignsState = campaignsState,
                        onPromptToAgent = { prompt ->
                            navigateToTab(SegmaTab.AGENT)
                            viewModel.sendAgentPrompt(prompt, autoBuildStrategyCard = true)
                        },
                        onOpenCampaignDetail = { id ->
                            navController.navigate(SegmaRoutes.campaignDetail(id))
                        },
                        onOpenCampaignBuilder = {
                            viewModel.clearBuilderDraft()
                            navController.navigate(SegmaRoutes.campaignBuilder())
                        },
                        onOpenOngoingCampaigns = {
                            navigateToTab(SegmaTab.CAMPAIGNS)
                        },
                        onOpenAudienceProfiling = {
                            navController.navigate(SegmaRoutes.AUDIENCE_PROFILING)
                        },
                        onOpenAiEvaluation = {
                            navController.navigate(SegmaRoutes.AI_EVALUATION)
                        },
                        onOpenCompetitorResearch = {
                            navController.navigate(SegmaRoutes.COMPETITOR_RESEARCH)
                        },
                        onOpenWhatsTrending = { editorialId ->
                            navController.navigate(SegmaRoutes.whatsTrending(editorialId))
                        },
                        onOpenProfile = {
                            navigateToTab(SegmaTab.PROFILE)
                        }
                    )
                }

                composable(SegmaTab.AGENT.route) {
                    SegmaAgentScreen(
                        userName = userProfile?.displayName ?: "You",
                        messages = agentMessages,
                        isGenerating = isAgentGenerating,
                        errorMessage = agentError,
                        onSendPrompt = { prompt -> viewModel.sendAgentPrompt(prompt) },
                        onRequestStrategyCard = { viewModel.generateStrategyCardForLastExchange() },
                        onEditDraft = { msgId, updated -> viewModel.updateDraftInMessage(msgId, updated) },
                        onSaveStrategyDraft = { draft, status ->
                            viewModel.saveStrategyAsCampaign(draft, status) { newId ->
                                if (status != com.example.model.CampaignStatus.DRAFT) {
                                    navController.navigate(SegmaRoutes.campaignDetail(newId))
                                }
                            }
                        },
                        onClearError = { viewModel.clearAgentError() },
                        canRetry = canRetryAgent,
                        onRetry = { viewModel.retryLastAgentPrompt() },
                        usageSnapshot = aiUsageSnapshot
                    )
                }

                composable(SegmaTab.INSIGHTS.route) {
                    TrendRadarScreen(
                        bookmarks = bookmarks,
                        onBack = { navigateToTab(SegmaTab.HOME) },
                        onCreateStrategyFromTrend = { trend ->
                            navigateToTab(SegmaTab.AGENT)
                            viewModel.startStrategyFromTrend(trend)
                        },
                        onToggleBookmark = { trend, currentlySaved ->
                            viewModel.toggleBookmark(
                                trendId = trend.id,
                                title = trend.title,
                                category = trend.category.label,
                                summary = trend.opportunityBody,
                                aiRecommendation = trend.peakTimeNote,
                                currentlyBookmarked = currentlySaved
                            )
                        },
                        onOpenWhatsTrending = { id ->
                            navController.navigate(SegmaRoutes.whatsTrending(id))
                        }
                    )
                }

                composable(SegmaTab.CAMPAIGNS.route) {
                    CampaignsHubScreen(
                        campaignsState = campaignsState,
                        onOpenCampaignDetail = { id ->
                            navController.navigate(SegmaRoutes.campaignDetail(id))
                        },
                        onOpenCampaignBuilder = {
                            viewModel.clearBuilderDraft()
                            navController.navigate(SegmaRoutes.campaignBuilder())
                        },
                        onOpenAiEvaluation = {
                            navController.navigate(SegmaRoutes.AI_EVALUATION)
                        },
                        onRestoreDemoCampaign = { viewModel.reseedDemoCampaignIfEmpty() }
                    )
                }

                composable(SegmaTab.PROFILE.route) {
                    ProfileScreen(
                        userProfileState = userProfileState,
                        campaigns = campaigns,
                        personas = personas,
                        bookmarks = bookmarks,
                        onUpdateProfile = { name, role, h1, h2 ->
                            viewModel.updateProfileDetails(name, role, h1, h2)
                        },
                        onUpdateSocialConnection = { igOn, igHandle, ttOn, ttHandle ->
                            viewModel.updateSocialConnection(igOn, igHandle, ttOn, ttHandle)
                        },
                        onOpenPersonaArchive = {
                            navController.navigate(SegmaRoutes.AUDIENCE_PROFILING)
                        },
                        onOpenEvaluationHistory = {
                            navController.navigate(SegmaRoutes.AI_EVALUATION)
                        },
                        onOpenWhatsTrending = { editorialId ->
                            navController.navigate(SegmaRoutes.whatsTrending(editorialId))
                        },
                        aiUsageSnapshot = aiUsageSnapshot
                    )
                }

                composable(
                    route = SegmaRoutes.CAMPAIGN_DETAIL,
                    arguments = listOf(navArgument("campaignId") { type = NavType.StringType })
                ) { entry ->
                    val campId = entry.arguments?.getString("campaignId").orEmpty()
                    val campaign: Campaign? = campaigns.firstOrNull { it.id == campId }
                    CampaignDetailScreen(
                        campaign = campaign,
                        aiInsightState = campaignAiInsightState,
                        followUpState = followUpState,
                        onBack = { navController.popBackStack() },
                        onReviseStrategy = { c ->
                            viewModel.clearBuilderDraft()
                            navController.navigate(SegmaRoutes.campaignBuilder(c.id))
                        },
                        onUpdateStatus = { id, status -> viewModel.updateCampaignStatus(id, status) },
                        onUpdateMetrics = { c, r, v, l, com, sh, sa ->
                            viewModel.updateCampaignManualMetrics(c, r, v, l, com, sh, sa)
                        },
                        onRefreshAiAnalysis = { c, force -> viewModel.refreshCampaignAiAnalysis(c, forceRefresh = force) },
                        onRequestFollowUp = { c, force -> viewModel.requestFollowUpContent(c, forceRefresh = force) },
                        onClearFollowUp = { viewModel.clearFollowUpContent() },
                        onOpenEvaluation = { navController.navigate(SegmaRoutes.AI_EVALUATION) },
                        onDeleteCampaign = { id ->
                            viewModel.deleteCampaign(id) { navController.popBackStack() }
                        },
                        insightCacheNotice = insightCacheNotice,
                        followUpCacheNotice = followUpCacheNotice
                    )
                }

                composable(
                    route = SegmaRoutes.CAMPAIGN_BUILDER,
                    arguments = listOf(
                        navArgument("campaignId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { entry ->
                    val campId = entry.arguments?.getString("campaignId")
                    val existing = campaigns.firstOrNull { it.id == campId }
                    CampaignBuilderScreen(
                        existingCampaign = existing,
                        draftState = builderDraftState,
                        onBack = { navController.popBackStack() },
                        onGenerateWithAi = { n, b, o, d, a, p, dur, bud, force ->
                            viewModel.generateCampaignPlanInBuilder(n, b, o, d, a, p, dur, bud, forceRefresh = force)
                        },
                        onSaveCampaign = { toSave ->
                            viewModel.saveFullCampaign(toSave) { savedId ->
                                navController.popBackStack()
                                navController.navigate(SegmaRoutes.campaignDetail(savedId))
                            }
                        },
                        cacheNotice = builderCacheNotice
                    )
                }

                composable(SegmaRoutes.AI_EVALUATION) {
                    AiEvaluationScreen(
                        campaigns = campaigns,
                        evaluationsState = evaluationsState,
                        draftState = evaluationDraftState,
                        onBack = { navController.popBackStack() },
                        onEvaluateCampaign = { c, force -> viewModel.runCampaignEvaluation(c, forceRefresh = force) },
                        onSaveEvaluation = { eval -> viewModel.saveEvaluationReport(eval) },
                        cacheNotice = evaluationCacheNotice
                    )
                }

                composable(SegmaRoutes.COMPETITOR_RESEARCH) {
                    CompetitorResearchScreen(
                        savedResearchState = competitorsState,
                        draftState = competitorDraftState,
                        onBack = { navController.popBackStack() },
                        onRunResearch = { brand, user, plat, obs, ctx, force ->
                            viewModel.runCompetitorResearch(brand, user, plat, obs, ctx, forceRefresh = force)
                        },
                        onSaveResearch = { res -> viewModel.saveCompetitorResearch(res) },
                        cacheNotice = competitorCacheNotice
                    )
                }

                composable(SegmaRoutes.AUDIENCE_PROFILING) {
                    AudienceProfilingScreen(
                        personasState = personasState,
                        draftState = personaDraftState,
                        onBack = { navController.popBackStack() },
                        onGeneratePersona = { brief, force -> viewModel.generateAndPreviewPersona(brief, forceRefresh = force) },
                        onSavePersona = { p -> viewModel.savePersona(p) },
                        onDeletePersona = { id -> viewModel.deletePersona(id) },
                        cacheNotice = personaCacheNotice
                    )
                }

                composable(
                    route = SegmaRoutes.WHATS_TRENDING,
                    arguments = listOf(navArgument("editorialId") { type = NavType.StringType })
                ) { entry ->
                    val editorialId = entry.arguments?.getString("editorialId").orEmpty()
                    WhatsTrendingScreen(
                        editorialId = editorialId,
                        bookmarks = bookmarks,
                        onBack = { navController.popBackStack() },
                        onToggleBookmark = { item, currentlySaved ->
                            viewModel.toggleBookmark(
                                trendId = item.id,
                                title = item.headline,
                                category = item.categoryTags.firstOrNull() ?: "Viral Audio",
                                summary = item.whyTrending,
                                aiRecommendation = item.aiRecommendations.firstOrNull().orEmpty(),
                                currentlyBookmarked = currentlySaved
                            )
                        },
                        onUseInAgent = { prompt ->
                            navigateToTab(SegmaTab.AGENT)
                            viewModel.sendAgentPrompt(prompt, autoBuildStrategyCard = true)
                        }
                    )
                }
            }
        }
    }
}
