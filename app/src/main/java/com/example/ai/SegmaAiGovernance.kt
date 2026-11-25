package com.example.ai

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AudiencePersona
import com.example.model.CampaignEvaluation
import com.example.model.CompetitorResearch
import com.example.model.GeneratedStrategyDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

const val DEFAULT_DAILY_APP_AI_BUDGET = 12
const val DEFAULT_CACHE_TTL_MS = 24L * 60L * 60L * 1000L
private const val ROLLING_WINDOW_MS = 24L * 60L * 60L * 1000L

enum class GeminiErrorCategory {
    RPM_RATE_LIMIT,
    RPD_DAILY_QUOTA,
    TPM_RATE_LIMIT,
    TPD_DAILY_QUOTA,
    TEMPORARY_CAPACITY,
    AMBIGUOUS_RATE_LIMIT,
    OTHER
}

data class AiUsageSnapshot(
    val usedIn24h: Int = 0,
    val dailyLimit: Int = DEFAULT_DAILY_APP_AI_BUDGET,
    val quotaCooldownUntilMs: Long? = null,
    val lastQuotaMessage: String? = null,
    val lastQuotaCategory: GeminiErrorCategory? = null
) {
    val remaining: Int get() = (dailyLimit - usedIn24h).coerceAtLeast(0)
    val warningThreshold: Int get() = ((dailyLimit * 3) / 4).coerceAtLeast(1)
    val isApproachingLimit: Boolean get() = usedIn24h in warningThreshold until dailyLimit
    val isLimitReached: Boolean get() = usedIn24h >= dailyLimit
}

data class AiCachedResult<T>(
    val data: T,
    val fromCache: Boolean,
    val cacheLabel: String? = null
)

class AiLocalBudgetExceededException(
    val snapshot: AiUsageSnapshot
) : IllegalStateException(
    "Local AI Request Budget reached (${snapshot.usedIn24h}/${snapshot.dailyLimit} requests in 24h). Saved campaigns, demo data, and cached results remain available."
)

class AiQuotaExceededException(
    override val message: String,
    val retryAfterSeconds: Int? = null,
    cause: Throwable? = null,
    val category: GeminiErrorCategory = GeminiErrorCategory.AMBIGUOUS_RATE_LIMIT,
    val rawErrorDetails: String = ""
) : IllegalStateException(message, cause)

data class ParsedAiError(
    val isQuotaError: Boolean,
    val userMessage: String,
    val retryAfterSeconds: Int? = null,
    val category: GeminiErrorCategory = GeminiErrorCategory.OTHER,
    val rawErrorDetails: String = ""
)

object GeminiErrorClassifier {
    private const val MAX_RAW_DETAILS_CHARS = 500
    private val WORD_RPM = Regex("""\brpm\b""")
    private val WORD_RPD = Regex("""\brpd\b""")
    private val WORD_TPM = Regex("""\btpm\b""")
    private val WORD_TPD = Regex("""\btpd\b""")
    private val WORD_UNAVAILABLE = Regex("""\bunavailable\b""")

    fun classify(error: Throwable, fallbackMessage: String): ParsedAiError {
        val rawDetails = extractRawErrorDetails(error)
        val lower = rawDetails.lowercase()
        val compact = lower.replace(Regex("""[_\-\s]+"""), "")

        val hasQuotaOr429Signal = lower.contains("429") ||
            lower.contains("resource_exhausted") ||
            compact.contains("resourceexhausted") ||
            compact.contains("quotaexceeded") ||
            compact.contains("ratelimit") ||
            lower.contains("too many requests") ||
            lower.contains("exceeded your current quota") ||
            lower.contains("quota metric") ||
            lower.contains("quota limit") ||
            compact.contains("dailylimit") ||
            compact.contains("dailyquota")

        val hasCapacitySignal = lower.contains("503") ||
            lower.contains("502") ||
            lower.contains("504") ||
            WORD_UNAVAILABLE.containsMatchIn(lower) ||
            compact.contains("serviceunavailable") ||
            lower.contains("temporarily unavailable") ||
            lower.contains("overloaded") ||
            lower.contains("high demand") ||
            lower.contains("capacity") ||
            lower.contains("server busy")

        val mentionsTokens = lower.contains("token")
        val mentionsRequests = lower.contains("request") || lower.contains("req/")
        val mentionsPerDay = compact.contains("perday") ||
            lower.contains("/day") ||
            compact.contains("dailyquota") ||
            compact.contains("dailylimit") ||
            compact.contains("dailyfreetier") ||
            compact.contains("24hours") ||
            (lower.contains("daily") && (hasQuotaOr429Signal || mentionsTokens || mentionsRequests))
        val mentionsPerMinute = compact.contains("perminute") ||
            lower.contains("/min") ||
            lower.contains("per min")

        val category = when {
            WORD_TPD.containsMatchIn(lower) ||
                compact.contains("tokensperday") ||
                compact.contains("tokenperday") ||
                compact.contains("tokencountperday") ||
                (mentionsTokens && mentionsPerDay) -> GeminiErrorCategory.TPD_DAILY_QUOTA

            WORD_RPD.containsMatchIn(lower) ||
                compact.contains("requestsperday") ||
                compact.contains("requestperday") ||
                (mentionsPerDay && (hasQuotaOr429Signal || mentionsRequests)) -> GeminiErrorCategory.RPD_DAILY_QUOTA

            WORD_TPM.containsMatchIn(lower) ||
                compact.contains("tokensperminute") ||
                compact.contains("tokenperminute") ||
                compact.contains("tokencountperminute") ||
                (mentionsTokens && mentionsPerMinute) -> GeminiErrorCategory.TPM_RATE_LIMIT

            WORD_RPM.containsMatchIn(lower) ||
                compact.contains("requestsperminute") ||
                compact.contains("requestperminute") ||
                (mentionsPerMinute && (hasQuotaOr429Signal || mentionsRequests)) -> GeminiErrorCategory.RPM_RATE_LIMIT

            hasCapacitySignal -> GeminiErrorCategory.TEMPORARY_CAPACITY

            hasQuotaOr429Signal -> GeminiErrorCategory.AMBIGUOUS_RATE_LIMIT

            else -> GeminiErrorCategory.OTHER
        }

        val retryAfterSec = extractRetryAfterSeconds(rawDetails)
        if (category == GeminiErrorCategory.OTHER) {
            val baseMessage = error.localizedMessage?.takeIf { it.isNotBlank() } ?: fallbackMessage
            val userMessage = if (rawDetails.isNotBlank() && !baseMessage.contains(rawDetails) && error.cause != null) {
                "$baseMessage\nAPI details: ${rawDetails.take(MAX_RAW_DETAILS_CHARS)}"
            } else {
                baseMessage
            }
            return ParsedAiError(
                isQuotaError = false,
                userMessage = userMessage,
                retryAfterSeconds = retryAfterSec,
                category = GeminiErrorCategory.OTHER,
                rawErrorDetails = rawDetails
            )
        }

        val summary = buildCategoryMessage(category, retryAfterSec)
        val userMessage = if (rawDetails.isNotBlank()) {
            "$summary\nAPI details: ${rawDetails.take(MAX_RAW_DETAILS_CHARS)}"
        } else {
            summary
        }
        return ParsedAiError(
            isQuotaError = true,
            userMessage = userMessage,
            retryAfterSeconds = retryAfterSec,
            category = category,
            rawErrorDetails = rawDetails
        )
    }

    private fun extractRawErrorDetails(error: Throwable): String =
        generateSequence(error) { it.cause }
            .map { ex ->
                val className = ex::class.simpleName.orEmpty()
                val msg = ex.message.orEmpty().trim()
                when {
                    className.isNotBlank() && msg.isNotBlank() -> "$className: $msg"
                    msg.isNotBlank() -> msg
                    else -> className
                }
            }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" | ")

    private fun buildCategoryMessage(
        category: GeminiErrorCategory,
        retryAfterSeconds: Int?
    ): String {
        val retryHint = retryAfterSeconds?.takeIf { it > 0 }?.let { " Try again in ~${it}s." }.orEmpty()
        return when (category) {
            GeminiErrorCategory.RPM_RATE_LIMIT ->
                "Gemini Requests Per Minute (RPM) rate limit reached. This is a temporary per-minute limit.$retryHint Your messages and campaign drafts are saved."
            GeminiErrorCategory.TPM_RATE_LIMIT ->
                "Gemini Tokens Per Minute (TPM) rate limit reached. This is a temporary per-minute token limit.$retryHint Consider shortening your prompt if needed. Your messages and campaign drafts are saved."
            GeminiErrorCategory.RPD_DAILY_QUOTA -> {
                val resetHint = retryAfterSeconds?.takeIf { it > 0 }?.let { " (provider suggested ~${it}s)" }.orEmpty()
                "Gemini daily Requests Per Day (RPD) quota is exhausted$resetHint. You must wait for the provider quota reset before sending new live AI requests. Your messages and campaign drafts are saved."
            }
            GeminiErrorCategory.TPD_DAILY_QUOTA -> {
                val resetHint = retryAfterSeconds?.takeIf { it > 0 }?.let { " (provider suggested ~${it}s)" }.orEmpty()
                "Gemini daily Tokens Per Day (TPD) quota is exhausted$resetHint. You must wait for the provider quota reset before sending new live AI requests. Your messages and campaign drafts are saved."
            }
            GeminiErrorCategory.TEMPORARY_CAPACITY -> {
                val waitText = retryHint.ifBlank { " Please wait briefly before tapping Retry." }
                "Gemini is temporarily unavailable due to high demand or limited model capacity.$waitText Your messages and campaign drafts are saved."
            }
            GeminiErrorCategory.AMBIGUOUS_RATE_LIMIT -> {
                val waitText = retryHint.ifBlank { " Please wait briefly before tapping Retry." }
                "Gemini rate or quota limit reached (HTTP 429).$waitText Your messages and campaign drafts are saved."
            }
            GeminiErrorCategory.OTHER -> ""
        }
    }

    internal fun extractRetryAfterSeconds(text: String): Int? {
        val minutePatterns = listOf(
            Regex("""(?:retry[\s_-]*(?:after|in)|try\s+again\s+in|wait)\s*(?:[:=]\s*|\s+)["']?(\d{1,3}(?:\.\d+)?)\s*(?:m|min|mins|minute|minutes)\b""", RegexOption.IGNORE_CASE)
        )
        for (regex in minutePatterns) {
            val match = regex.find(text)
            val minutes = match?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            if (minutes != null && minutes > 0.0) {
                val seconds = kotlin.math.ceil(minutes * 60.0).toInt()
                if (seconds in 1..86400) return seconds
            }
        }

        val secondPatterns = listOf(
            Regex("""(?:retry[\s_-]*(?:after|in)|retryDelay|try\s+again\s+in)\s*(?:[:=]\s*|\s+)["']?(\d{1,5}(?:\.\d+)?)\s*(?:s|sec|secs|second|seconds)?\b""", RegexOption.IGNORE_CASE),
            Regex("""retry_delay\s*\{[^}]*seconds\s*:\s*(\d{1,5})""", RegexOption.IGNORE_CASE)
        )
        for (regex in secondPatterns) {
            val match = regex.find(text)
            val value = match?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            if (value != null && value > 0.0) {
                val seconds = kotlin.math.ceil(value).toInt()
                if (seconds in 1..86400) return seconds
            }
        }
        return null
    }
}

interface AiUsageTracker {
    val dailyLimit: Int
    val usageSnapshot: StateFlow<AiUsageSnapshot>
    fun recordDispatchedRequest()
    fun recordQuotaCooldown(
        retryAfterSeconds: Int?,
        message: String,
        category: GeminiErrorCategory = GeminiErrorCategory.AMBIGUOUS_RATE_LIMIT
    )
    fun clearQuotaCooldownIfExpired()
    fun currentSnapshot(): AiUsageSnapshot
}

class InMemoryAiUsageTracker(
    override val dailyLimit: Int = DEFAULT_DAILY_APP_AI_BUDGET,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : AiUsageTracker {
    private val lock = Any()
    private val timestamps = mutableListOf<Long>()
    private var cooldownUntilMs: Long? = null
    private var lastQuotaMsg: String? = null
    private var lastQuotaCat: GeminiErrorCategory? = null
    private val _snapshot = MutableStateFlow(AiUsageSnapshot(0, dailyLimit))
    override val usageSnapshot: StateFlow<AiUsageSnapshot> = _snapshot.asStateFlow()

    override fun recordDispatchedRequest() {
        synchronized(lock) {
            val now = clock()
            pruneOld(now)
            timestamps.add(now)
            refreshSnapshot(now)
        }
    }

    override fun recordQuotaCooldown(
        retryAfterSeconds: Int?,
        message: String,
        category: GeminiErrorCategory
    ) {
        synchronized(lock) {
            val now = clock()
            cooldownUntilMs = retryAfterSeconds?.takeIf { it > 0 }?.let { now + it * 1000L }
            lastQuotaMsg = message
            lastQuotaCat = category
            refreshSnapshot(now)
        }
    }

    override fun clearQuotaCooldownIfExpired() {
        synchronized(lock) {
            val now = clock()
            if (cooldownUntilMs != null && now >= cooldownUntilMs!!) {
                cooldownUntilMs = null
            }
            refreshSnapshot(now)
        }
    }

    override fun currentSnapshot(): AiUsageSnapshot = synchronized(lock) {
        val now = clock()
        pruneOld(now)
        refreshSnapshot(now)
        _snapshot.value
    }

    private fun pruneOld(now: Long) {
        val cutoff = now - ROLLING_WINDOW_MS
        timestamps.removeAll { it < cutoff }
    }

    private fun refreshSnapshot(now: Long) {
        val activeCooldown = cooldownUntilMs?.takeIf { now < it }
        _snapshot.value = AiUsageSnapshot(
            usedIn24h = timestamps.size,
            dailyLimit = dailyLimit,
            quotaCooldownUntilMs = activeCooldown,
            lastQuotaMessage = if (activeCooldown != null) lastQuotaMsg else null,
            lastQuotaCategory = if (activeCooldown != null) lastQuotaCat else null
        )
    }
}

class SharedPreferencesAiUsageTracker(
    context: Context,
    override val dailyLimit: Int = DEFAULT_DAILY_APP_AI_BUDGET,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : AiUsageTracker {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("segma_ai_usage_prefs", Context.MODE_PRIVATE)
    private val lock = Any()
    private val _snapshot = MutableStateFlow(loadAndPruneSnapshot())
    override val usageSnapshot: StateFlow<AiUsageSnapshot> = _snapshot.asStateFlow()

    override fun recordDispatchedRequest() {
        synchronized(lock) {
            val now = clock()
            val list = readPrunedTimestamps(now).toMutableList()
            list.add(now)
            writeTimestamps(list)
            _snapshot.value = buildSnapshot(list.size, now)
        }
    }

    override fun recordQuotaCooldown(
        retryAfterSeconds: Int?,
        message: String,
        category: GeminiErrorCategory
    ) {
        synchronized(lock) {
            val now = clock()
            val until = retryAfterSeconds?.takeIf { it > 0 }?.let { now + it * 1000L } ?: 0L
            prefs.edit()
                .putLong(KEY_COOLDOWN_UNTIL, until)
                .putString(KEY_LAST_QUOTA_MSG, message)
                .putString(KEY_LAST_QUOTA_CAT, category.name)
                .apply()
            _snapshot.value = loadAndPruneSnapshot()
        }
    }

    override fun clearQuotaCooldownIfExpired() {
        synchronized(lock) {
            val now = clock()
            val until = prefs.getLong(KEY_COOLDOWN_UNTIL, 0L)
            if (until > 0L && now >= until) {
                prefs.edit()
                    .remove(KEY_COOLDOWN_UNTIL)
                    .remove(KEY_LAST_QUOTA_MSG)
                    .remove(KEY_LAST_QUOTA_CAT)
                    .apply()
            }
            _snapshot.value = loadAndPruneSnapshot()
        }
    }

    override fun currentSnapshot(): AiUsageSnapshot = synchronized(lock) {
        loadAndPruneSnapshot().also { _snapshot.value = it }
    }

    private fun loadAndPruneSnapshot(): AiUsageSnapshot {
        val now = clock()
        val pruned = readPrunedTimestamps(now)
        writeTimestamps(pruned)
        return buildSnapshot(pruned.size, now)
    }

    private fun readPrunedTimestamps(now: Long): List<Long> {
        val raw = prefs.getString(KEY_TIMESTAMPS, "[]") ?: "[]"
        val cutoff = now - ROLLING_WINDOW_MS
        val arr = try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
        val kept = ArrayList<Long>(arr.length())
        for (i in 0 until arr.length()) {
            val ts = arr.optLong(i, -1L)
            if (ts >= cutoff) kept.add(ts)
        }
        return kept
    }

    private fun writeTimestamps(list: List<Long>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(KEY_TIMESTAMPS, arr.toString()).apply()
    }

    private fun buildSnapshot(count: Int, now: Long): AiUsageSnapshot {
        val rawCooldown = prefs.getLong(KEY_COOLDOWN_UNTIL, 0L)
        val activeCooldown = rawCooldown.takeIf { it > now }
        val msg = if (activeCooldown != null) prefs.getString(KEY_LAST_QUOTA_MSG, null) else null
        val catName = if (activeCooldown != null) prefs.getString(KEY_LAST_QUOTA_CAT, null) else null
        val cat = catName?.let { name -> GeminiErrorCategory.entries.firstOrNull { it.name == name } }
        return AiUsageSnapshot(
            usedIn24h = count,
            dailyLimit = dailyLimit,
            quotaCooldownUntilMs = activeCooldown,
            lastQuotaMessage = msg,
            lastQuotaCategory = cat
        )
    }

    private companion object {
        const val KEY_TIMESTAMPS = "dispatched_timestamps_24h"
        const val KEY_COOLDOWN_UNTIL = "quota_cooldown_until_ms"
        const val KEY_LAST_QUOTA_MSG = "quota_last_msg"
        const val KEY_LAST_QUOTA_CAT = "quota_last_cat"
    }
}

interface AiResponseCacheStore {
    fun getString(key: String, ttlMs: Long = DEFAULT_CACHE_TTL_MS): String?
    fun putString(key: String, value: String)
}

class InMemoryAiResponseCacheStore(
    private val clock: () -> Long = { System.currentTimeMillis() }
) : AiResponseCacheStore {
    private val map = LinkedHashMap<String, Pair<Long, String>>(64, 0.75f, true)

    @Synchronized
    override fun getString(key: String, ttlMs: Long): String? {
        val entry = map[key] ?: return null
        if (clock() - entry.first > ttlMs) {
            map.remove(key)
            return null
        }
        return entry.second
    }

    @Synchronized
    override fun putString(key: String, value: String) {
        if (map.size > 100) {
            val eldest = map.entries.firstOrNull()?.key
            if (eldest != null) map.remove(eldest)
        }
        map[key] = clock() to value
    }
}

class SharedPreferencesAiResponseCacheStore(
    context: Context,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : AiResponseCacheStore {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("segma_ai_response_cache", Context.MODE_PRIVATE)
    private val memory = InMemoryAiResponseCacheStore(clock)

    @Synchronized
    override fun getString(key: String, ttlMs: Long): String? {
        memory.getString(key, ttlMs)?.let { return it }
        val raw = prefs.getString(key, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            val savedAt = obj.optLong("savedAt", 0L)
            val value = obj.optString("value", "")
            if (value.isBlank() || clock() - savedAt > ttlMs) {
                prefs.edit().remove(key).apply()
                null
            } else {
                memory.putString(key, value)
                value
            }
        } catch (_: Exception) {
            prefs.edit().remove(key).apply()
            null
        }
    }

    @Synchronized
    override fun putString(key: String, value: String) {
        if (value.isBlank()) return
        memory.putString(key, value)
        val payload = JSONObject()
            .put("savedAt", clock())
            .put("value", value)
            .toString()
        prefs.edit().putString(key, payload).apply()
    }
}

class AiRequestCoordinator(
    val usageTracker: AiUsageTracker = InMemoryAiUsageTracker(),
    val cacheStore: AiResponseCacheStore = InMemoryAiResponseCacheStore(),
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val mutex = Mutex()
    private val inFlightKeys = mutableSetOf<String>()

    val usageSnapshot: StateFlow<AiUsageSnapshot> = usageTracker.usageSnapshot

    suspend fun <T> executeCached(
        featureType: String,
        normalizedKeyInput: String,
        forceRefresh: Boolean,
        ttlMs: Long = DEFAULT_CACHE_TTL_MS,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
        networkCall: suspend () -> T
    ): AiCachedResult<T> {
        val cacheKey = buildCacheKey(featureType, normalizedKeyInput)

        if (!forceRefresh) {
            cacheStore.getString(cacheKey, ttlMs)?.let { raw ->
                deserializer(raw)?.let { cached ->
                    return AiCachedResult(
                        data = cached,
                        fromCache = true,
                        cacheLabel = "Reused from 24h cache (0 AI requests used)"
                    )
                }
            }
        }

        val acquired = mutex.withLock {
            if (inFlightKeys.contains(cacheKey)) {
                false
            } else {
                inFlightKeys.add(cacheKey)
                true
            }
        }
        if (!acquired) {
            throw IllegalStateException("An identical AI request is already in progress.")
        }

        try {
            ensureCanDispatchLiveRequest()
            usageTracker.recordDispatchedRequest()
            val result = try {
                networkCall()
            } catch (e: Exception) {
                handleDispatchFailure(e)
                throw e
            }
            val encoded = serializer(result)
            if (encoded.isNotBlank()) {
                cacheStore.putString(cacheKey, encoded)
            }
            return AiCachedResult(data = result, fromCache = false)
        } finally {
            mutex.withLock {
                inFlightKeys.remove(cacheKey)
            }
        }
    }

    suspend fun prepareLiveChatDispatch(): Unit {
        val acquired = mutex.withLock {
            if (inFlightKeys.contains(AGENT_CHAT_FLIGHT_KEY)) {
                false
            } else {
                inFlightKeys.add(AGENT_CHAT_FLIGHT_KEY)
                true
            }
        }
        if (!acquired) {
            throw IllegalStateException("Segma Agent is already generating a response.")
        }
        try {
            ensureCanDispatchLiveRequest()
            usageTracker.recordDispatchedRequest()
        } catch (e: Throwable) {
            mutex.withLock { inFlightKeys.remove(AGENT_CHAT_FLIGHT_KEY) }
            throw e
        }
    }

    suspend fun finishLiveChatDispatch(failure: Throwable? = null) {
        if (failure != null) {
            handleDispatchFailure(failure)
        }
        mutex.withLock {
            inFlightKeys.remove(AGENT_CHAT_FLIGHT_KEY)
        }
    }

    private fun ensureCanDispatchLiveRequest() {
        usageTracker.clearQuotaCooldownIfExpired()
        val snap = usageTracker.currentSnapshot()
        val cooldown = snap.quotaCooldownUntilMs
        if (cooldown != null && clock() < cooldown) {
            val remainingSec = ((cooldown - clock()) / 1000L).coerceAtLeast(1L).toInt()
            throw AiQuotaExceededException(
                message = snap.lastQuotaMessage
                    ?: "Gemini rate or capacity cooldown is active. Please wait ~${remainingSec}s before trying again. Your messages and campaign drafts are saved.",
                retryAfterSeconds = remainingSec,
                category = snap.lastQuotaCategory ?: GeminiErrorCategory.AMBIGUOUS_RATE_LIMIT
            )
        }
        if (snap.isLimitReached) {
            throw AiLocalBudgetExceededException(snap)
        }
    }

    private fun handleDispatchFailure(e: Throwable) {
        val parsed = GeminiErrorClassifier.classify(e, "")
        if (parsed.isQuotaError) {
            usageTracker.recordQuotaCooldown(parsed.retryAfterSeconds, parsed.userMessage, parsed.category)
        }
    }

    companion object {
        private const val AGENT_CHAT_FLIGHT_KEY = "segma_agent_chat_in_flight"

        fun buildCacheKey(featureType: String, rawInput: String): String {
            val normalized = rawInput.trim()
                .lowercase()
                .replace(Regex("\\s*\\|\\s*"), "|")
                .replace(Regex("\\s+"), " ")
            val digest = MessageDigest.getInstance("SHA-256")
                .digest("$featureType::$normalized".toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            return "${featureType}_${digest.take(32)}"
        }
    }
}

object AiJsonCodecs {
    fun encodeStrategyDraft(d: GeneratedStrategyDraft): String = jsonObject(
        "campaignName" to d.campaignName,
        "brandName" to d.brandName,
        "objective" to d.objective,
        "targetAudience" to d.targetAudience,
        "audienceTags" to d.audienceTags,
        "feasibilityScore" to d.feasibilityScore,
        "communicationAngle" to d.communicationAngle,
        "platforms" to d.platforms,
        "recommendedFormats" to d.recommendedFormats,
        "uploadStrategy" to d.uploadStrategy,
        "contentCalendar" to d.contentCalendar,
        "captionsAndHooks" to d.captionsAndHooks,
        "hashtags" to d.hashtags,
        "audioRecommendation" to d.audioRecommendation,
        "contentPillars" to d.contentPillars
    )

    fun decodeStrategyDraft(raw: String): GeneratedStrategyDraft? = try {
        val m = parseJsonObject(raw) ?: return null
        GeneratedStrategyDraft(
            campaignName = m.str("campaignName", "New AI Campaign"),
            brandName = m.str("brandName", "My Brand"),
            objective = m.str("objective", "Increase engagement"),
            targetAudience = m.str("targetAudience"),
            audienceTags = m.strList("audienceTags"),
            feasibilityScore = m.int("feasibilityScore", 85),
            communicationAngle = m.str("communicationAngle"),
            platforms = m.strList("platforms").ifEmpty { listOf("Instagram", "TikTok") },
            recommendedFormats = m.strList("recommendedFormats"),
            uploadStrategy = m.str("uploadStrategy"),
            contentCalendar = m.str("contentCalendar"),
            captionsAndHooks = m.str("captionsAndHooks"),
            hashtags = m.strList("hashtags"),
            audioRecommendation = m.str("audioRecommendation"),
            contentPillars = m.strList("contentPillars")
        )
    } catch (_: Exception) {
        null
    }

    fun encodeEvaluation(e: CampaignEvaluation): String = jsonObject(
        "campaignId" to e.campaignId,
        "campaignName" to e.campaignName,
        "executionSummary" to e.executionSummary,
        "objectiveAchievement" to e.objectiveAchievement,
        "targetVsActual" to e.targetVsActual,
        "engagementAnalysis" to e.engagementAnalysis,
        "contentEffectiveness" to e.contentEffectiveness,
        "strengths" to e.strengths,
        "weaknesses" to e.weaknesses,
        "influencingFactors" to e.influencingFactors,
        "futureRecommendations" to e.futureRecommendations,
        "overallScore" to e.overallScore
    )

    fun decodeEvaluation(raw: String): CampaignEvaluation? = try {
        val m = parseJsonObject(raw) ?: return null
        CampaignEvaluation(
            campaignId = m.str("campaignId"),
            campaignName = m.str("campaignName"),
            executionSummary = m.str("executionSummary"),
            objectiveAchievement = m.str("objectiveAchievement"),
            targetVsActual = m.str("targetVsActual"),
            engagementAnalysis = m.str("engagementAnalysis"),
            contentEffectiveness = m.str("contentEffectiveness"),
            strengths = m.str("strengths"),
            weaknesses = m.str("weaknesses"),
            influencingFactors = m.str("influencingFactors"),
            futureRecommendations = m.str("futureRecommendations"),
            overallScore = m.long("overallScore", 75L)
        )
    } catch (_: Exception) {
        null
    }

    fun encodeCompetitor(r: CompetitorResearch): String = jsonObject(
        "brandName" to r.brandName,
        "username" to r.username,
        "platform" to r.platform,
        "dataMode" to r.dataMode,
        "contentThemes" to r.contentThemes,
        "publishingFrequency" to r.publishingFrequency,
        "dominantFormats" to r.dominantFormats,
        "observedMetricsSummary" to r.observedMetricsSummary,
        "communicationStyle" to r.communicationStyle,
        "strengths" to r.strengths,
        "opportunities" to r.opportunities,
        "differentiationStrategy" to r.differentiationStrategy
    )

    fun decodeCompetitor(raw: String): CompetitorResearch? = try {
        val m = parseJsonObject(raw) ?: return null
        CompetitorResearch(
            brandName = m.str("brandName"),
            username = m.str("username"),
            platform = m.str("platform"),
            dataMode = m.str("dataMode"),
            contentThemes = m.str("contentThemes"),
            publishingFrequency = m.str("publishingFrequency"),
            dominantFormats = m.str("dominantFormats"),
            observedMetricsSummary = m.str("observedMetricsSummary"),
            communicationStyle = m.str("communicationStyle"),
            strengths = m.str("strengths"),
            opportunities = m.str("opportunities"),
            differentiationStrategy = m.str("differentiationStrategy")
        )
    } catch (_: Exception) {
        null
    }

    fun encodePersona(p: AudiencePersona): String = jsonObject(
        "name" to p.name,
        "ageRange" to p.ageRange,
        "location" to p.location,
        "interests" to p.interests,
        "painPoints" to p.painPoints,
        "preferredFormats" to p.preferredFormats,
        "toneOfVoice" to p.toneOfVoice,
        "feasibilityScore" to p.feasibilityScore,
        "summary" to p.summary
    )

    fun decodePersona(raw: String): AudiencePersona? = try {
        val m = parseJsonObject(raw) ?: return null
        AudiencePersona(
            name = m.str("name", "Target Segment"),
            ageRange = m.str("ageRange", "18-24"),
            location = m.str("location", "Urban Digital"),
            interests = m.strList("interests"),
            painPoints = m.str("painPoints"),
            preferredFormats = m.strList("preferredFormats"),
            toneOfVoice = m.str("toneOfVoice"),
            feasibilityScore = m.long("feasibilityScore", 82L),
            summary = m.str("summary")
        )
    } catch (_: Exception) {
        null
    }

    private fun jsonObject(vararg entries: Pair<String, Any?>): String {
        val sb = StringBuilder("{")
        entries.forEachIndexed { index, (k, v) ->
            if (index > 0) sb.append(',')
            sb.append(jsonString(k)).append(':').append(jsonValue(v))
        }
        sb.append('}')
        return sb.toString()
    }

    private fun jsonValue(v: Any?): String = when (v) {
        null -> "null"
        is Number, is Boolean -> v.toString()
        is Iterable<*> -> v.joinToString(prefix = "[", postfix = "]", separator = ",") { jsonValue(it) }
        else -> jsonString(v.toString())
    }

    private fun jsonString(s: String): String = buildString {
        append('"')
        for (ch in s) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                else -> if (ch.code < 0x20) append(String.format("\\u%04x", ch.code)) else append(ch)
            }
        }
        append('"')
    }

    private fun Map<String, Any?>.str(key: String, default: String = ""): String =
        (this[key] as? String) ?: default

    private fun Map<String, Any?>.int(key: String, default: Int): Int =
        (this[key] as? Number)?.toInt() ?: default

    private fun Map<String, Any?>.long(key: String, default: Long): Long =
        (this[key] as? Number)?.toLong() ?: default

    private fun Map<String, Any?>.strList(key: String): List<String> =
        (this[key] as? List<*>)?.mapNotNull { (it as? String)?.takeIf { s -> s.isNotBlank() } }.orEmpty()

    private fun parseJsonObject(raw: String): Map<String, Any?>? {
        val parser = MiniJsonParser(raw)
        return parser.parseObjectOrNull()
    }

    private class MiniJsonParser(private val src: String) {
        private var pos = 0

        fun parseObjectOrNull(): Map<String, Any?>? {
            skipWs()
            if (pos >= src.length || src[pos] != '{') return null
            return parseObject()
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            val map = LinkedHashMap<String, Any?>()
            skipWs()
            if (peek() == '}') {
                pos++
                return map
            }
            while (pos < src.length) {
                skipWs()
                val key = parseString()
                skipWs()
                expect(':')
                skipWs()
                map[key] = parseValue()
                skipWs()
                when (peek()) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return map
                    }
                    else -> throw IllegalArgumentException("Expected , or } at $pos")
                }
            }
            throw IllegalArgumentException("Unterminated JSON object")
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val list = ArrayList<Any?>()
            skipWs()
            if (peek() == ']') {
                pos++
                return list
            }
            while (pos < src.length) {
                skipWs()
                list.add(parseValue())
                skipWs()
                when (peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return list
                    }
                    else -> throw IllegalArgumentException("Expected , or ] at $pos")
                }
            }
            throw IllegalArgumentException("Unterminated JSON array")
        }

        private fun parseValue(): Any? {
            skipWs()
            return when (val ch = peek()) {
                '"' -> parseString()
                '{' -> parseObject()
                '[' -> parseArray()
                't' -> { pos += 4; true }
                'f' -> { pos += 5; false }
                'n' -> { pos += 4; null }
                else -> {
                    if (ch == '-' || ch in '0'..'9') parseNumber()
                    else throw IllegalArgumentException("Unexpected char $ch at $pos")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < src.length) {
                val ch = src[pos++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        val esc = src[pos++]
                        when (esc) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                val hex = src.substring(pos, pos + 4)
                                pos += 4
                                sb.append(hex.toInt(16).toChar())
                            }
                            else -> sb.append(esc)
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            throw IllegalArgumentException("Unterminated string")
        }

        private fun parseNumber(): Number {
            val start = pos
            if (peek() == '-') pos++
            while (pos < src.length && src[pos] in '0'..'9') pos++
            val isFloat = pos < src.length && (src[pos] == '.' || src[pos] == 'e' || src[pos] == 'E')
            if (isFloat) {
                if (pos < src.length && src[pos] == '.') {
                    pos++
                    while (pos < src.length && src[pos] in '0'..'9') pos++
                }
                if (pos < src.length && (src[pos] == 'e' || src[pos] == 'E')) {
                    pos++
                    if (pos < src.length && (src[pos] == '+' || src[pos] == '-')) pos++
                    while (pos < src.length && src[pos] in '0'..'9') pos++
                }
                return src.substring(start, pos).toDouble()
            }
            return src.substring(start, pos).toLong()
        }

        private fun skipWs() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        private fun peek(): Char = if (pos < src.length) src[pos] else '\u0000'

        private fun expect(expected: Char) {
            if (pos >= src.length || src[pos] != expected) {
                throw IllegalArgumentException("Expected $expected at $pos")
            }
            pos++
        }
    }
}

/**
 * Deterministic zero-API-cost strategy card extractor.
 * Eliminates the duplicate Gemini JSON call after Segma Agent already generated a strategy reply.
 */
object SegmaStrategyLocalExtractor {

    fun extractFromAgentReply(userPrompt: String, agentReply: String): GeneratedStrategyDraft {
        val combined = "$userPrompt\n$agentReply"
        val cleanLines = agentReply.lines().map { it.trim() }.filter { it.isNotBlank() }

        val campaignName = extractCampaignTitle(userPrompt, cleanLines)
        val feasibility = Regex("""(\d{2,3})\s*/\s*100""")
            .find(agentReply)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.coerceIn(50, 100) ?: 84

        val hashtags = Regex("""#[A-Za-z0-9_]{2,32}""")
            .findAll(agentReply)
            .map { it.value }
            .distinct()
            .take(8)
            .toList()

        val detectedPlatforms = buildList {
            if (combined.contains("instagram", ignoreCase = true) || combined.contains("reels", ignoreCase = true)) add("Instagram")
            if (combined.contains("tiktok", ignoreCase = true)) add("TikTok")
            if (combined.contains("youtube", ignoreCase = true) || combined.contains("shorts", ignoreCase = true)) add("YouTube Shorts")
            if (combined.contains("linkedin", ignoreCase = true)) add("LinkedIn")
            if (combined.contains("twitter", ignoreCase = true) || Regex("""\bx\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined)) add("X")
        }.ifEmpty { listOf("Instagram", "TikTok") }

        val detectedFormats = buildList {
            if (combined.contains("reel", ignoreCase = true)) add("Reels")
            if (combined.contains("carousel", ignoreCase = true)) add("Carousel Posts")
            if (combined.contains("tiktok", ignoreCase = true) || combined.contains("short video", ignoreCase = true)) add("Short Videos")
            if (combined.contains("stor", ignoreCase = true)) add("Stories")
            if (combined.contains("short", ignoreCase = true)) add("Shorts")
        }.distinct().ifEmpty { listOf("Reels", "Carousel Posts", "Stories") }

        val audienceSection = extractSectionSnippet(cleanLines, listOf("audience", "persona", "segment", "target"))
            ?: "Digital creators & target segment from campaign brief"
        val angleSection = extractSectionSnippet(cleanLines, listOf("angle", "messaging", "strategy", "communication"))
            ?: cleanLines.firstOrNull { !it.startsWith("#") }?.take(220)
            ?: userPrompt.take(180)
        val uploadSection = extractSectionSnippet(cleanLines, listOf("upload", "schedule", "cadence", "posting", "window", "time"))
            ?: "Consistent 4–5x/week publishing during evening peak windows"
        val hooksSection = extractSectionSnippet(cleanLines, listOf("hook", "caption", "copy", "cta", "headline"))
            ?: cleanLines.filter { it.startsWith("-") || it.startsWith("*") || it.contains("CTA", ignoreCase = true) }
                .take(3).joinToString("\n").take(360)
        val audioSection = extractSectionSnippet(cleanLines, listOf("audio", "music", "sound"))
            ?: "Upbeat modern instrumental or trending creator-friendly audio bed"

        val pillars = cleanLines
            .filter { line ->
                line.contains("pillar", ignoreCase = true) ||
                    line.startsWith("- ") ||
                    line.startsWith("* ") ||
                    Regex("""^\d+\.""").containsMatchIn(line)
            }
            .map { it.replace(Regex("""^[-*\d.]+\s*"""), "").replace("**", "").take(64) }
            .filter { it.length > 6 }
            .distinct()
            .take(4)

        val audienceTags = buildList {
            if (combined.contains("gen z", ignoreCase = true)) add("Gen Z")
            if (combined.contains("student", ignoreCase = true) || combined.contains("university", ignoreCase = true)) add("University Students")
            if (combined.contains("eco", ignoreCase = true) || combined.contains("sustainab", ignoreCase = true)) add("Eco-Conscious")
            if (combined.contains("creator", ignoreCase = true)) add("Creators")
            if (isEmpty()) addAll(detectedPlatforms.take(2))
        }.take(4)

        return GeneratedStrategyDraft(
            campaignName = campaignName,
            brandName = "My Brand",
            objective = userPrompt.trim().take(160),
            targetAudience = audienceSection.take(220),
            audienceTags = audienceTags,
            feasibilityScore = feasibility,
            communicationAngle = angleSection.take(260),
            platforms = detectedPlatforms,
            recommendedFormats = detectedFormats,
            uploadStrategy = uploadSection.take(240),
            contentCalendar = cleanLines.take(6).joinToString("\n").take(500),
            captionsAndHooks = hooksSection.ifBlank { "Open with a 2-second visual hook tied to audience pain point + clear Save/Share CTA." },
            hashtags = hashtags.ifEmpty { listOf("#DigitalCampaign", "#ContentStrategy", "#SegmaAI") },
            audioRecommendation = audioSection.take(180),
            contentPillars = pillars.ifEmpty { listOf("Education & Value", "Social Proof", "Behind the Scenes") }
        )
    }

    private fun extractCampaignTitle(userPrompt: String, lines: List<String>): String {
        val explicitTitle = lines.firstOrNull {
            it.startsWith("Campaign:", ignoreCase = true) || it.startsWith("Title:", ignoreCase = true)
        }?.substringAfter(":")?.replace("**", "")?.trim()
        if (!explicitTitle.isNullOrBlank()) return explicitTitle.take(48)

        val words = userPrompt
            .replace(Regex("""[^A-Za-z0-9\s]"""), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it.lowercase() !in STOP_WORDS }
            .take(4)
        return if (words.isNotEmpty()) {
            words.joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }.take(44)
        } else {
            "AI Strategy Campaign"
        }
    }

    private fun extractSectionSnippet(lines: List<String>, keywords: List<String>): String? {
        val idx = lines.indexOfFirst { line ->
            keywords.any { kw -> line.contains(kw, ignoreCase = true) }
        }
        if (idx < 0) return null
        val collected = buildList {
            add(lines[idx].replace("**", ""))
            if (idx + 1 < lines.size && !lines[idx + 1].matches(Regex("""^\d+\..*"""))) {
                add(lines[idx + 1].replace("**", ""))
            }
        }
        return collected.joinToString(" ").trim().takeIf { it.isNotBlank() }
    }

    private val STOP_WORDS = setOf(
        "help", "want", "create", "make", "campaign", "for", "the", "and",
        "with", "about", "that", "this", "ideas", "plan", "strategy", "next"
    )
}
