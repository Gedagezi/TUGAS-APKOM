package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.AudiencePersona
import com.example.model.Campaign
import com.example.model.CampaignEvaluation
import com.example.model.CampaignStatus
import com.example.model.CompetitorResearch
import com.example.model.TrendBookmark
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

interface SegmaDataRepository {
    suspend fun ensureUserProfileAndSeedDemoData(user: FirebaseUser): Result<Unit>
    fun observeUserProfile(uid: String): Flow<UserProfile?>
    fun observeCampaigns(uid: String): Flow<List<Campaign>>
    fun observePersonas(uid: String): Flow<List<AudiencePersona>>
    fun observeEvaluations(uid: String): Flow<List<CampaignEvaluation>>
    fun observeCompetitors(uid: String): Flow<List<CompetitorResearch>>
    fun observeBookmarks(uid: String): Flow<List<TrendBookmark>>
    suspend fun updateProfileDetails(
        displayName: String,
        roleTitle: String,
        activeHandle: String,
        secondaryHandle: String
    ): Result<Unit>
    suspend fun updateSocialConnection(
        instagramConnected: Boolean,
        instagramHandle: String,
        tiktokConnected: Boolean,
        tiktokHandle: String
    ): Result<Unit>
    suspend fun saveCampaign(campaign: Campaign): Result<String>
    suspend fun updateCampaignStatus(campaignId: String, status: CampaignStatus): Result<Unit>
    suspend fun updateCampaignMetricsAndInsight(
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
    ): Result<Unit>
    suspend fun deleteCampaign(campaignId: String): Result<Unit>
    suspend fun savePersona(persona: AudiencePersona): Result<String>
    suspend fun deletePersona(personaId: String): Result<Unit>
    suspend fun saveEvaluation(evaluation: CampaignEvaluation): Result<String>
    suspend fun saveCompetitorResearch(research: CompetitorResearch): Result<String>
    suspend fun toggleBookmark(
        trendId: String,
        title: String,
        category: String,
        summary: String,
        aiRecommendation: String,
        currentlyBookmarked: Boolean
    ): Result<Boolean>
}

class SegmaFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : SegmaDataRepository {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    override suspend fun ensureUserProfileAndSeedDemoData(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        val path = "users/$uid"
        return try {
            val existingProfile = userDoc(uid).get().await()
            if (!existingProfile.exists()) {
                val displayName = user.displayName?.takeIf { it.isNotBlank() }?.take(80)
                    ?: "SEGMA Creator"
                val handleSeed = displayName.lowercase()
                    .replace(Regex("[^a-z0-9]"), "")
                    .take(14)
                    .ifBlank { "creator" }

                val profilePayload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "roleTitle" to "Digital Marketing Strategist",
                    "plan" to "PRO",
                    "activeHandle" to "@eitssampah",
                    "secondaryHandle" to "@$handleSeed",
                    "instagramConnected" to true,
                    "instagramHandle" to "@eitssampah",
                    "tiktokConnected" to false,
                    "tiktokHandle" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDoc(uid).set(profilePayload).await()

                // Seed clearly labeled Demo Data campaigns so first-time users can explore workflows immediately
                val existingCampaigns = userDoc(uid).collection("campaigns").limit(1).get().await()
                if (existingCampaigns.isEmpty) {
                    SegmaDemoCatalog.initialDemoCampaigns.forEach { demo ->
                        val campRef = userDoc(uid).collection("campaigns").document(demo.id)
                        campRef.set(buildCampaignCreatePayload(uid, demo)).await()
                    }
                }

                val existingPersonas = userDoc(uid).collection("personas").limit(1).get().await()
                if (existingPersonas.isEmpty) {
                    val p = SegmaDemoCatalog.initialDemoPersona
                    userDoc(uid).collection("personas").document(p.id)
                        .set(buildPersonaCreatePayload(uid, p)).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val path = "users/$uid"
        emitAll(
            userDoc(uid).snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    override fun observeCampaigns(uid: String): Flow<List<Campaign>> = flow {
        val path = "users/$uid/campaigns"
        emitAll(
            userDoc(uid).collection("campaigns").snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Campaign::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    override fun observePersonas(uid: String): Flow<List<AudiencePersona>> = flow {
        val path = "users/$uid/personas"
        emitAll(
            userDoc(uid).collection("personas").snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AudiencePersona::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    override fun observeEvaluations(uid: String): Flow<List<CampaignEvaluation>> = flow {
        val path = "users/$uid/evaluations"
        emitAll(
            userDoc(uid).collection("evaluations").snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CampaignEvaluation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    override fun observeCompetitors(uid: String): Flow<List<CompetitorResearch>> = flow {
        val path = "users/$uid/competitors"
        emitAll(
            userDoc(uid).collection("competitors").snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CompetitorResearch::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    override fun observeBookmarks(uid: String): Flow<List<TrendBookmark>> = flow {
        val path = "users/$uid/bookmarks"
        emitAll(
            userDoc(uid).collection("bookmarks").snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TrendBookmark::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    override suspend fun updateProfileDetails(
        displayName: String,
        roleTitle: String,
        activeHandle: String,
        secondaryHandle: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauth"}"
        return try {
            val validUid = requireUserId()
            require(displayName.trim().isNotEmpty()) { "Display name cannot be empty." }
            userDoc(validUid).update(
                mapOf(
                    "displayName" to displayName.trim().take(80),
                    "roleTitle" to roleTitle.trim().take(80),
                    "activeHandle" to activeHandle.trim().take(60),
                    "secondaryHandle" to secondaryHandle.trim().take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun updateSocialConnection(
        instagramConnected: Boolean,
        instagramHandle: String,
        tiktokConnected: Boolean,
        tiktokHandle: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauth"}"
        return try {
            val validUid = requireUserId()
            userDoc(validUid).update(
                mapOf(
                    "instagramConnected" to instagramConnected,
                    "instagramHandle" to instagramHandle.trim().take(60),
                    "tiktokConnected" to tiktokConnected,
                    "tiktokHandle" to tiktokHandle.trim().take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun saveCampaign(campaign: Campaign): Result<String> {
        val uid = auth.currentUser?.uid
        var path = "users/${uid ?: "unauth"}/campaigns"
        return try {
            val validUid = requireUserId()
            require(campaign.name.trim().isNotEmpty()) { "Campaign name is required." }
            require(campaign.objective.trim().isNotEmpty()) { "Campaign objective is required." }
            val col = userDoc(validUid).collection("campaigns")
            val docRef = if (campaign.id.isBlank()) col.document() else col.document(campaign.id)
            path = docRef.path

            val existingSnap = if (campaign.id.isNotBlank()) docRef.get().await() else null
            if (existingSnap != null && existingSnap.exists()) {
                docRef.update(buildCampaignStrategyUpdatePayload(campaign)).await()
            } else {
                docRef.set(buildCampaignCreatePayload(validUid, campaign)).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun updateCampaignStatus(campaignId: String, status: CampaignStatus): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauth"}/campaigns/$campaignId"
        return try {
            val validUid = requireUserId()
            require(campaignId.isNotBlank()) { "Campaign ID is required." }
            userDoc(validUid).collection("campaigns").document(campaignId)
                .update(
                    mapOf(
                        "status" to status.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

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
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauth"}/campaigns/$campaignId"
        return try {
            val validUid = requireUserId()
            val safeReach = reach.coerceIn(0L, 1_000_000_000L)
            val er = if (safeReach > 0L) {
                (((likes + comments + shares + saves).toDouble() / safeReach.toDouble()) * 100.0)
                    .coerceIn(0.0, 100.0)
            } else 0.0
            userDoc(validUid).collection("campaigns").document(campaignId)
                .update(
                    mapOf(
                        "reach" to safeReach,
                        "views" to views.coerceIn(0L, 1_000_000_000L),
                        "likes" to likes.coerceIn(0L, 1_000_000_000L),
                        "comments" to comments.coerceIn(0L, 1_000_000_000L),
                        "shares" to shares.coerceIn(0L, 1_000_000_000L),
                        "saves" to saves.coerceIn(0L, 1_000_000_000L),
                        "engagementRate" to er,
                        "demoData" to demoData,
                        "dataSourceLabel" to dataSourceLabel.take(120),
                        "aiNextActionRecommendation" to aiNextActionRecommendation.take(2000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteCampaign(campaignId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauth"}/campaigns/$campaignId"
        return try {
            val validUid = requireUserId()
            userDoc(validUid).collection("campaigns").document(campaignId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override suspend fun savePersona(persona: AudiencePersona): Result<String> {
        val uid = auth.currentUser?.uid
        var path = "users/${uid ?: "unauth"}/personas"
        return try {
            val validUid = requireUserId()
            require(persona.name.trim().isNotEmpty()) { "Persona name is required." }
            val docRef = userDoc(validUid).collection("personas").document()
            path = docRef.path
            docRef.set(buildPersonaCreatePayload(validUid, persona)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deletePersona(personaId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauth"}/personas/$personaId"
        return try {
            val validUid = requireUserId()
            userDoc(validUid).collection("personas").document(personaId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override suspend fun saveEvaluation(evaluation: CampaignEvaluation): Result<String> {
        val uid = auth.currentUser?.uid
        var path = "users/${uid ?: "unauth"}/evaluations"
        return try {
            val validUid = requireUserId()
            val docRef = userDoc(validUid).collection("evaluations").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to validUid,
                "campaignId" to evaluation.campaignId.take(128).ifBlank { "manual" },
                "campaignName" to evaluation.campaignName.take(120).ifBlank { "Untitled Campaign" },
                "executionSummary" to evaluation.executionSummary.take(2000),
                "objectiveAchievement" to evaluation.objectiveAchievement.take(1500),
                "targetVsActual" to evaluation.targetVsActual.take(1500),
                "engagementAnalysis" to evaluation.engagementAnalysis.take(1500),
                "contentEffectiveness" to evaluation.contentEffectiveness.take(1500),
                "strengths" to evaluation.strengths.take(1500),
                "weaknesses" to evaluation.weaknesses.take(1500),
                "influencingFactors" to evaluation.influencingFactors.take(1500),
                "futureRecommendations" to evaluation.futureRecommendations.take(2000),
                "overallScore" to evaluation.overallScore.coerceIn(0L, 100L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun saveCompetitorResearch(research: CompetitorResearch): Result<String> {
        val uid = auth.currentUser?.uid
        var path = "users/${uid ?: "unauth"}/competitors"
        return try {
            val validUid = requireUserId()
            val docRef = userDoc(validUid).collection("competitors").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to validUid,
                "brandName" to research.brandName.take(100).ifBlank { "Competitor" },
                "username" to research.username.take(80).ifBlank { "@competitor" },
                "platform" to research.platform.take(50).ifBlank { "Instagram" },
                "dataMode" to research.dataMode.take(80).ifBlank { "AI Strategic Analysis" },
                "contentThemes" to research.contentThemes.take(1500),
                "publishingFrequency" to research.publishingFrequency.take(300),
                "dominantFormats" to research.dominantFormats.take(500),
                "observedMetricsSummary" to research.observedMetricsSummary.take(1000),
                "communicationStyle" to research.communicationStyle.take(1000),
                "strengths" to research.strengths.take(1500),
                "opportunities" to research.opportunities.take(1500),
                "differentiationStrategy" to research.differentiationStrategy.take(2000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun toggleBookmark(
        trendId: String,
        title: String,
        category: String,
        summary: String,
        aiRecommendation: String,
        currentlyBookmarked: Boolean
    ): Result<Boolean> {
        val uid = auth.currentUser?.uid
        val safeDocId = trendId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val path = "users/${uid ?: "unauth"}/bookmarks/$safeDocId"
        return try {
            val validUid = requireUserId()
            val docRef = userDoc(validUid).collection("bookmarks").document(safeDocId)
            if (currentlyBookmarked) {
                docRef.delete().await()
                Result.success(false)
            } else {
                val payload = mapOf(
                    "userId" to validUid,
                    "trendId" to safeDocId,
                    "title" to title.take(160).ifBlank { "Saved Trend" },
                    "category" to category.take(60).ifBlank { "Insight" },
                    "summary" to summary.take(1000),
                    "aiRecommendation" to aiRecommendation.take(1500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun buildCampaignCreatePayload(uid: String, c: Campaign): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to c.name.trim().take(120).ifBlank { "Untitled Campaign" },
        "brandName" to c.brandName.trim().take(100),
        "handle" to c.handle.trim().take(60),
        "objective" to c.objective.trim().take(200).ifBlank { "Brand Awareness" },
        "description" to c.description.trim().take(1500),
        "targetAudience" to c.targetAudience.trim().take(300),
        "audienceTags" to c.audienceTags.map { it.take(40) }.take(10),
        "platforms" to c.platforms.map { it.take(30) }.take(10),
        "duration" to c.duration.trim().take(80),
        "budget" to c.budget.trim().take(80),
        "status" to CampaignStatus.fromRaw(c.status).name,
        "demoData" to c.demoData,
        "targetReach" to c.targetReach.coerceIn(0L, 1_000_000_000L),
        "targetEr" to c.targetEr.coerceIn(0.0, 100.0),
        "reach" to c.reach.coerceIn(0L, 1_000_000_000L),
        "views" to c.views.coerceIn(0L, 1_000_000_000L),
        "likes" to c.likes.coerceIn(0L, 1_000_000_000L),
        "comments" to c.comments.coerceIn(0L, 1_000_000_000L),
        "shares" to c.shares.coerceIn(0L, 1_000_000_000L),
        "saves" to c.saves.coerceIn(0L, 1_000_000_000L),
        "engagementRate" to c.engagementRate.coerceIn(0.0, 100.0),
        "dataSourceLabel" to c.dataSourceLabel.trim().take(120),
        "aiOverview" to c.aiOverview.trim().take(3000),
        "aiCommunicationStrategy" to c.aiCommunicationStrategy.trim().take(2000),
        "aiContentPillars" to c.aiContentPillars.map { it.take(100) }.take(10),
        "aiUploadStrategy" to c.aiUploadStrategy.trim().take(1500),
        "aiContentCalendar" to c.aiContentCalendar.trim().take(4000),
        "aiCaptions" to c.aiCaptions.trim().take(3000),
        "aiHashtags" to c.aiHashtags.map { it.take(50) }.take(20),
        "aiAudioRecommendation" to c.aiAudioRecommendation.trim().take(500),
        "aiNextActionRecommendation" to c.aiNextActionRecommendation.trim().take(2000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun buildCampaignStrategyUpdatePayload(c: Campaign): Map<String, Any> = mapOf(
        "name" to c.name.trim().take(120).ifBlank { "Untitled Campaign" },
        "brandName" to c.brandName.trim().take(100),
        "handle" to c.handle.trim().take(60),
        "objective" to c.objective.trim().take(200).ifBlank { "Brand Awareness" },
        "description" to c.description.trim().take(1500),
        "targetAudience" to c.targetAudience.trim().take(300),
        "audienceTags" to c.audienceTags.map { it.take(40) }.take(10),
        "platforms" to c.platforms.map { it.take(30) }.take(10),
        "duration" to c.duration.trim().take(80),
        "budget" to c.budget.trim().take(80),
        "status" to CampaignStatus.fromRaw(c.status).name,
        "targetReach" to c.targetReach.coerceIn(0L, 1_000_000_000L),
        "targetEr" to c.targetEr.coerceIn(0.0, 100.0),
        "aiOverview" to c.aiOverview.trim().take(3000),
        "aiCommunicationStrategy" to c.aiCommunicationStrategy.trim().take(2000),
        "aiContentPillars" to c.aiContentPillars.map { it.take(100) }.take(10),
        "aiUploadStrategy" to c.aiUploadStrategy.trim().take(1500),
        "aiContentCalendar" to c.aiContentCalendar.trim().take(4000),
        "aiCaptions" to c.aiCaptions.trim().take(3000),
        "aiHashtags" to c.aiHashtags.map { it.take(50) }.take(20),
        "aiAudioRecommendation" to c.aiAudioRecommendation.trim().take(500),
        "aiNextActionRecommendation" to c.aiNextActionRecommendation.trim().take(2000),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun buildPersonaCreatePayload(uid: String, p: AudiencePersona): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to p.name.trim().take(100).ifBlank { "Target Persona" },
        "ageRange" to p.ageRange.trim().take(50),
        "location" to p.location.trim().take(100),
        "interests" to p.interests.map { it.take(50) }.take(10),
        "painPoints" to p.painPoints.trim().take(1000),
        "preferredFormats" to p.preferredFormats.map { it.take(50) }.take(10),
        "toneOfVoice" to p.toneOfVoice.trim().take(200),
        "feasibilityScore" to p.feasibilityScore.coerceIn(0L, 100L),
        "summary" to p.summary.trim().take(1500),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun rethrowOrLog(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
