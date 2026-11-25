package com.example.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe, atomic in-flight request guard keyed by action identifier.
 * Prevents multiple simultaneous API calls (AI or Firestore) for the same action.
 */
class ActionRequestGuard {
    private val lock = Any()
    private val inFlightKeys = LinkedHashSet<String>()
    private val _activeActions = MutableStateFlow<Set<String>>(emptySet())
    val activeActions: StateFlow<Set<String>> = _activeActions.asStateFlow()

    fun tryAcquire(actionKey: String): Boolean = synchronized(lock) {
        if (!inFlightKeys.add(actionKey)) {
            return false
        }
        _activeActions.value = inFlightKeys.toSet()
        true
    }

    fun release(actionKey: String) {
        synchronized(lock) {
            if (inFlightKeys.remove(actionKey)) {
                _activeActions.value = inFlightKeys.toSet()
            }
        }
    }

    fun isInFlight(actionKey: String): Boolean = synchronized(lock) {
        inFlightKeys.contains(actionKey)
    }
}

object SegmaActionKeys {
    const val AGENT_CHAT = "agent_chat"
    const val BUILDER_GENERATE_PLAN = "builder_generate_plan"
    const val CAMPAIGN_AI_ANALYSIS = "campaign_ai_analysis"
    const val CAMPAIGN_FOLLOW_UP = "campaign_follow_up"
    const val CAMPAIGN_EVALUATION = "campaign_evaluation"
    const val COMPETITOR_RESEARCH = "competitor_research"
    const val AUDIENCE_PERSONA = "audience_persona"

    const val SAVE_STRATEGY_CAMPAIGN = "save_strategy_campaign"
    const val SAVE_FULL_CAMPAIGN = "save_full_campaign"
    const val SAVE_EVALUATION_REPORT = "save_evaluation_report"
    const val SAVE_COMPETITOR_RESEARCH = "save_competitor_research"
    const val SAVE_PERSONA = "save_persona"
    const val UPDATE_PROFILE_DETAILS = "update_profile_details"
    const val UPDATE_SOCIAL_CONNECTION = "update_social_connection"
    const val RESEED_DEMO_CAMPAIGN = "reseed_demo_campaign"

    fun updateCampaignStatus(campaignId: String): String = "update_campaign_status:$campaignId"
    fun deleteCampaign(campaignId: String): String = "delete_campaign:$campaignId"
    fun updateCampaignMetrics(campaignId: String): String = "update_campaign_metrics:$campaignId"
    fun deletePersona(personaId: String): String = "delete_persona:$personaId"
    fun toggleBookmark(trendId: String): String = "toggle_bookmark:$trendId"
}
