package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class CampaignStatus(val label: String) {
    DRAFT("Draft"),
    SCHEDULED("Scheduled"),
    ONGOING("Ongoing"),
    COMPLETED("Completed");

    companion object {
        fun fromRaw(raw: String?): CampaignStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DRAFT
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Zhilan Nur Rayfan",
    val roleTitle: String = "Content Creator & Strategist",
    val plan: String = "PRO",
    val activeHandle: String = "@eitssampah",
    val secondaryHandle: String = "@gedagezhi",
    val instagramConnected: Boolean = true,
    val instagramHandle: String = "@eitssampah",
    val tiktokConnected: Boolean = false,
    val tiktokHandle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Campaign(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val brandName: String = "",
    val handle: String = "",
    val objective: String = "",
    val description: String = "",
    val targetAudience: String = "",
    val audienceTags: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
    val duration: String = "",
    val budget: String = "",
    val status: String = CampaignStatus.DRAFT.name,
    val demoData: Boolean = false,
    val targetReach: Long = 0L,
    val targetEr: Double = 0.0,
    val reach: Long = 0L,
    val views: Long = 0L,
    val likes: Long = 0L,
    val comments: Long = 0L,
    val shares: Long = 0L,
    val saves: Long = 0L,
    val engagementRate: Double = 0.0,
    val dataSourceLabel: String = "",
    val aiOverview: String = "",
    val aiCommunicationStrategy: String = "",
    val aiContentPillars: List<String> = emptyList(),
    val aiUploadStrategy: String = "",
    val aiContentCalendar: String = "",
    val aiCaptions: String = "",
    val aiHashtags: List<String> = emptyList(),
    val aiAudioRecommendation: String = "",
    val aiNextActionRecommendation: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val statusEnum: CampaignStatus
        get() = CampaignStatus.fromRaw(status)

    val computedEngagementRate: Double
        get() {
            if (reach <= 0L) return engagementRate
            val totalInteractions = likes + comments + shares + saves
            return ((totalInteractions.toDouble() / reach.toDouble()) * 100.0)
        }
}

@Keep
@IgnoreExtraProperties
data class AudiencePersona(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val ageRange: String = "",
    val location: String = "",
    val interests: List<String> = emptyList(),
    val painPoints: String = "",
    val preferredFormats: List<String> = emptyList(),
    val toneOfVoice: String = "",
    val feasibilityScore: Long = 80L,
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CampaignEvaluation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val campaignId: String = "",
    val campaignName: String = "",
    val executionSummary: String = "",
    val objectiveAchievement: String = "",
    val targetVsActual: String = "",
    val engagementAnalysis: String = "",
    val contentEffectiveness: String = "",
    val strengths: String = "",
    val weaknesses: String = "",
    val influencingFactors: String = "",
    val futureRecommendations: String = "",
    val overallScore: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CompetitorResearch(
    @DocumentId val id: String = "",
    val userId: String = "",
    val brandName: String = "",
    val username: String = "",
    val platform: String = "",
    val dataMode: String = "",
    val contentThemes: String = "",
    val publishingFrequency: String = "",
    val dominantFormats: String = "",
    val observedMetricsSummary: String = "",
    val communicationStyle: String = "",
    val strengths: String = "",
    val opportunities: String = "",
    val differentiationStrategy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TrendBookmark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trendId: String = "",
    val title: String = "",
    val category: String = "",
    val summary: String = "",
    val aiRecommendation: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class TrendCategory(val label: String) {
    ALL("All"),
    HASHTAGS("Hashtags"),
    MUSIC_AUDIO("Music/Audio"),
    FORMATS("Formats"),
    NICHE_TOPICS("Niche Topics")
}

data class TrendInsight(
    val id: String,
    val title: String,
    val category: TrendCategory,
    val growthLabel: String,
    val peakTimeNote: String,
    val chartPoints: List<Float>,
    val topHashtags: List<Pair<String, String>>,
    val opportunityTitle: String,
    val opportunityBody: String,
    val sourceLabel: String = "Demo Snapshot • Curated Trend Radar"
)

data class ViralAudioTrack(
    val rank: Int,
    val title: String,
    val artist: String,
    val hookTiming: String,
    val aiUsageTip: String
)

data class TrendingEditorialItem(
    val id: String,
    val badgeTitle: String,
    val headline: String,
    val updatedLabel: String,
    val readTimeLabel: String,
    val categoryTags: List<String>,
    val tracks: List<ViralAudioTrack>,
    val whyTrending: String,
    val responsiveSegments: String,
    val aiRecommendations: List<String>,
    val isDemoSnapshot: Boolean = true
)

data class AgentMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
    val strategyDraft: GeneratedStrategyDraft? = null
)

data class GeneratedStrategyDraft(
    val campaignName: String,
    val brandName: String,
    val objective: String,
    val targetAudience: String,
    val audienceTags: List<String>,
    val feasibilityScore: Int,
    val communicationAngle: String,
    val platforms: List<String>,
    val recommendedFormats: List<String>,
    val uploadStrategy: String,
    val contentCalendar: String,
    val captionsAndHooks: String,
    val hashtags: List<String>,
    val audioRecommendation: String,
    val contentPillars: List<String>
)

data class SegmaNotification(
    val id: String,
    val title: String,
    val body: String,
    val severity: String,
    val timestampLabel: String
)
