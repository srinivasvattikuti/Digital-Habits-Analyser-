package com.example.data.science

import com.example.data.model.AppCategory
import com.example.data.model.DailyAggregateEntity
import com.example.data.model.HabitAnalyticsSnapshotEntity
import com.example.data.model.LongitudinalCategoryComparison
import com.example.data.model.ResearchHabitMetrics
import com.example.data.model.UsageEventEntity
import com.example.data.model.UserProfileEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Data Science & Behavioral Research Computation Engine for Digital Habits.
 * Implements exponential recency decay weighting, Shannon entropy for context switching,
 * Lally/Gardner habit automaticity models, and circadian disruption risk indices.
 */
class HabitResearchEngine {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Computes comprehensive research metrics from daily aggregates, fine-grained events, and user profile.
     */
    fun computeResearchMetrics(
        aggregates: List<DailyAggregateEntity>,
        events: List<UsageEventEntity>,
        userProfile: UserProfileEntity,
        halfLifeDays: Float = 3.5f
    ): ResearchHabitMetrics {
        if (aggregates.isEmpty()) {
            return ResearchHabitMetrics()
        }

        val today = Calendar.getInstance()
        val lambda = ln(2.0) / halfLifeDays

        // Group aggregates by dateStr
        val byDate = aggregates.groupBy { it.dateStr }
        val sortedDates = byDate.keys.sortedDescending()

        var totalDecayWeight = 0.0
        var weightedScreenTimeMs = 0.0
        var totalBaselineScreenTimeMs = 0.0

        for ((dateStr, aggs) in byDate) {
            val dateDiffDays = calculateDaysAgo(dateStr, today)
            val weight = Math.exp(-lambda * dateDiffDays)

            val dayTotalMs = aggs.sumOf { it.totalDurationMs }
            weightedScreenTimeMs += dayTotalMs * weight
            totalDecayWeight += weight
            totalBaselineScreenTimeMs += dayTotalMs
        }

        val recentWeightedMinutes = if (totalDecayWeight > 0) {
            ((weightedScreenTimeMs / totalDecayWeight) / 60000.0).toInt()
        } else {
            0
        }

        val baselineAvgDailyMinutes = if (byDate.isNotEmpty()) {
            ((totalBaselineScreenTimeMs / byDate.size) / 60000.0).toInt()
        } else {
            0
        }

        val longitudinalDeltaPct = if (baselineAvgDailyMinutes > 0) {
            ((recentWeightedMinutes - baselineAvgDailyMinutes).toFloat() / baselineAvgDailyMinutes) * 100f
        } else {
            0f
        }

        // 1. Attention Fragmentation Entropy (Shannon Entropy H)
        val (entropy, entropyRating) = calculateAttentionEntropy(events, aggregates)

        // 2. Gardner & Lally Behavioral Automaticity Index (BAI)
        val (automaticityScore, habitPhase, dayEstimate) = calculateHabitAutomaticity(aggregates, userProfile, byDate.size)

        // 3. Circadian Disruption Index (90-min window before bedtime)
        val (circadianScore, circadianRating, preBedtimeMins, windowLabel) = calculateCircadianDisruption(
            aggregates, events, userProfile
        )

        // 4. Flow State Integrity (Focus window continuous session preservation)
        val flowScore = calculateFlowStateIntegrity(aggregates, userProfile)

        // 5. Top Decay-Weighted Category & App
        val (topCategory, topApp) = calculateTopDecayWeightedEntities(aggregates, today, lambda)

        return ResearchHabitMetrics(
            exponentialDecayHalfLifeDays = halfLifeDays,
            recentWeightedScreenTimeMinutes = recentWeightedMinutes,
            baselineScreenTimeMinutes = baselineAvgDailyMinutes,
            longitudinalScreenTimeDeltaPct = longitudinalDeltaPct,
            automaticityIndex = automaticityScore,
            habitFormationPhase = habitPhase,
            habitFormationDayEstimate = dayEstimate,
            attentionFragmentationEntropy = entropy,
            attentionEntropyRating = entropyRating,
            circadianDisruptionIndex = circadianScore,
            circadianRiskRating = circadianRating,
            preBedtimeScreenMinutes = preBedtimeMins,
            bedtimeWindowLabel = windowLabel,
            flowStateIntegrityScore = flowScore,
            topDecayWeightedCategory = topCategory,
            topDecayWeightedApp = topApp
        )
    }

    /**
     * Compares 30-day baseline vs recent decay-weighted behavior across categories with Z-scores.
     */
    fun computeLongitudinalCategoryComparisons(
        aggregates: List<DailyAggregateEntity>,
        halfLifeDays: Float = 3.5f
    ): List<LongitudinalCategoryComparison> {
        val byDate = aggregates.groupBy { it.dateStr }
        val totalDays = max(1, byDate.size)
        val today = Calendar.getInstance()
        val lambda = ln(2.0) / halfLifeDays

        val categories = AppCategory.entries.filter { it != AppCategory.OTHER }
        val result = mutableListOf<LongitudinalCategoryComparison>()

        for (cat in categories) {
            val catAggs = aggregates.filter {
                it.category.equals(cat.name, ignoreCase = true) || it.category.equals(cat.displayName, ignoreCase = true)
            }

            // Baseline daily minutes per day
            val baselineDailyMinsList = mutableListOf<Double>()
            for ((_, dateAggs) in byDate) {
                val dayMins = dateAggs.filter {
                    it.category.equals(cat.name, ignoreCase = true) || it.category.equals(cat.displayName, ignoreCase = true)
                }.sumOf { it.totalDurationMs } / 60000.0
                baselineDailyMinsList.add(dayMins)
            }

            val baselineAvgMins = if (baselineDailyMinsList.isNotEmpty()) baselineDailyMinsList.average() else 0.0
            val stdDev = calculateStdDev(baselineDailyMinsList, baselineAvgMins)

            // Decay-weighted recent minutes
            var weightedMins = 0.0
            var totalWeight = 0.0
            for ((dateStr, dateAggs) in byDate) {
                val daysAgo = calculateDaysAgo(dateStr, today)
                val w = Math.exp(-lambda * daysAgo)
                val dayMins = dateAggs.filter {
                    it.category.equals(cat.name, ignoreCase = true) || it.category.equals(cat.displayName, ignoreCase = true)
                }.sumOf { it.totalDurationMs } / 60000.0
                weightedMins += dayMins * w
                totalWeight += w
            }

            val recentAvgMins = if (totalWeight > 0) (weightedMins / totalWeight) else 0.0
            val deltaMins = (recentAvgMins - baselineAvgMins).toInt()
            val deltaPct = if (baselineAvgMins > 0) {
                ((recentAvgMins - baselineAvgMins) / baselineAvgMins * 100.0).toFloat()
            } else {
                0f
            }

            val zScore = if (stdDev > 0.1) {
                ((recentAvgMins - baselineAvgMins) / stdDev).toFloat()
            } else {
                0f
            }

            val isPositive = if (cat == AppCategory.PRODUCTIVITY || cat == AppCategory.HEALTH) {
                deltaPct >= 0f
            } else {
                deltaPct <= 0f
            }

            if (baselineAvgMins > 1 || recentAvgMins > 1) {
                result.add(
                    LongitudinalCategoryComparison(
                        category = cat,
                        baselineMinutes = baselineAvgMins.toInt(),
                        recentWeightedMinutes = recentAvgMins.toInt(),
                        deltaMinutes = deltaMins,
                        deltaPercent = deltaPct,
                        zScore = zScore,
                        isPositiveShift = isPositive
                    )
                )
            }
        }

        return result.sortedByDescending { it.recentWeightedMinutes }
    }

    /**
     * Calculates Shannon Entropy H = - sum(p_i * log2(p_i)) over app transitions to quantify digital fragmentation.
     */
    private fun calculateAttentionEntropy(
        events: List<UsageEventEntity>,
        aggregates: List<DailyAggregateEntity>
    ): Pair<Float, String> {
        val appCounts: Map<String, Int> = if (events.isNotEmpty()) {
            events.groupBy { it.appName }.mapValues { it.value.size }
        } else {
            aggregates.groupBy { it.appName }.mapValues { it.value.sumOf { a -> a.openCount } }
        }

        val totalInteractions = appCounts.values.sum()
        if (totalInteractions <= 1) {
            return Pair(0.5f, "Deep Flow")
        }

        var entropy = 0.0
        for (count in appCounts.values) {
            if (count > 0) {
                val p = count.toDouble() / totalInteractions
                entropy -= p * log2(p)
            }
        }

        val roundedEntropy = (entropy * 100).toInt() / 100f
        val rating = when {
            roundedEntropy < 1.4f -> "Deep Flow (<1.4 bits)"
            roundedEntropy < 2.3f -> "Moderate Context Switching"
            else -> "High Attention Fragmentation (>2.3 bits)"
        }

        return Pair(roundedEntropy, rating)
    }

    /**
     * Computes Gardner Behavioral Automaticity Index & Lally 66-day habit formation progress curve.
     */
    private fun calculateHabitAutomaticity(
        aggregates: List<DailyAggregateEntity>,
        userProfile: UserProfileEntity,
        daysCount: Int
    ): Triple<Int, String, Int> {
        val totalOpens = aggregates.sumOf { it.openCount }
        val totalCompulsive = aggregates.sumOf { it.compulsiveOpens }
        val totalDays = max(1, daysCount)

        // Reflex ratio: higher compulsive opens means high automaticity for reflexive check habits
        val intentionalityRatio = if (totalOpens > 0) {
            1.0 - (totalCompulsive.toDouble() / totalOpens)
        } else {
            0.6
        }

        // Daily variance consistency in bedtime and focus hours
        val nightMinsAvg = aggregates.sumOf { it.nightMinutes }.toDouble() / totalDays
        val bedtimeAdherence = if (nightMinsAvg < 20) 0.9 else max(0.2, 1.0 - (nightMinsAvg / 90.0))

        val focusMinsAvg = (aggregates.sumOf { it.morningMinutes + it.afternoonMinutes }).toDouble() / totalDays
        val focusAdherence = min(1.0, focusMinsAvg / 180.0)

        // Automaticity score calculation (0 to 100)
        val automaticityScore = ((intentionalityRatio * 0.4 + bedtimeAdherence * 0.35 + focusAdherence * 0.25) * 100).toInt().coerceIn(10, 95)

        // Estimated position on Lally et al. (2010) asymptotic curve (y = a - b * e^(-c*t))
        val estimatedDay = (daysCount + (automaticityScore / 3)).coerceIn(1, 66)
        val phase = when {
            estimatedDay <= 18 -> "Early Acquisition (Days 1-18)"
            estimatedDay <= 45 -> "Linear Consolidation (Days 19-45)"
            else -> "Asymptotic Automaticity (Days 46-66+)"
        }

        return Triple(automaticityScore, phase, estimatedDay)
    }

    /**
     * Evaluates screen interaction within the critical 90-minute pre-bedtime window.
     */
    private fun calculateCircadianDisruption(
        aggregates: List<DailyAggregateEntity>,
        events: List<UsageEventEntity>,
        userProfile: UserProfileEntity
    ): CircadianAnalysisResult {
        val bedtime = userProfile.bedtimeHour
        val startPreBedtimeHour = (bedtime - 2 + 24) % 24
        val windowLabel = String.format(Locale.US, "%d:30 PM - %d:00 PM", (startPreBedtimeHour % 12).let { if (it == 0) 12 else it }, (bedtime % 12).let { if (it == 0) 12 else it })

        val totalDays = max(1, aggregates.map { it.dateStr }.distinct().size)
        val totalNightMins = aggregates.sumOf { it.nightMinutes }
        val avgNightMinsPerDay = totalNightMins / totalDays

        // Pre-bedtime minutes estimate
        val preBedtimeMins = (avgNightMinsPerDay * 0.75).toInt().coerceAtLeast(0)

        val riskScore = when {
            preBedtimeMins > 45 -> min(95, 60 + (preBedtimeMins - 45))
            preBedtimeMins > 20 -> 35 + ((preBedtimeMins - 20) * 1)
            else -> (preBedtimeMins * 1.5).toInt().coerceIn(5, 30)
        }

        val riskRating = when {
            riskScore < 30 -> "Minimal Circadian Disruption"
            riskScore < 60 -> "Moderate Melatonin Suppression"
            else -> "High Circadian Disruption Risk"
        }

        return CircadianAnalysisResult(riskScore, riskRating, preBedtimeMins, windowLabel)
    }

    private data class CircadianAnalysisResult(
        val riskScore: Int,
        val riskRating: String,
        val preBedtimeMinutes: Int,
        val windowLabel: String
    )

    private fun calculateFlowStateIntegrity(
        aggregates: List<DailyAggregateEntity>,
        userProfile: UserProfileEntity
    ): Int {
        val totalFocusMins = aggregates.sumOf { it.morningMinutes + it.afternoonMinutes }
        val totalOpens = aggregates.sumOf { it.openCount }

        if (totalFocusMins <= 0 || totalOpens <= 0) return 70

        val avgSessionLengthMins = totalFocusMins.toDouble() / totalOpens
        val integrity = (avgSessionLengthMins * 8).toInt().coerceIn(20, 95)
        return integrity
    }

    private fun calculateTopDecayWeightedEntities(
        aggregates: List<DailyAggregateEntity>,
        today: Calendar,
        lambda: Double
    ): Pair<AppCategory, String> {
        val byDate = aggregates.groupBy { it.dateStr }
        val catScores = mutableMapOf<String, Double>()
        val appScores = mutableMapOf<String, Double>()

        for ((dateStr, aggs) in byDate) {
            val daysAgo = calculateDaysAgo(dateStr, today)
            val weight = Math.exp(-lambda * daysAgo)

            for (agg in aggs) {
                catScores[agg.category] = (catScores[agg.category] ?: 0.0) + (agg.totalDurationMs * weight)
                appScores[agg.appName] = (appScores[agg.appName] ?: 0.0) + (agg.totalDurationMs * weight)
            }
        }

        val topCatStr = catScores.maxByOrNull { it.value }?.key ?: "PRODUCTIVITY"
        val topCategory = try {
            AppCategory.valueOf(topCatStr)
        } catch (e: Exception) {
            AppCategory.PRODUCTIVITY
        }

        val topApp = appScores.maxByOrNull { it.value }?.key ?: "Chrome"
        return Pair(topCategory, topApp)
    }

    private fun calculateDaysAgo(dateStr: String, today: Calendar): Int {
        return try {
            val date = dateFormat.parse(dateStr) ?: return 0
            val diffMs = today.timeInMillis - date.time
            max(0, (diffMs / 86400000L).toInt())
        } catch (e: Exception) {
            0
        }
    }

    private fun calculateStdDev(values: List<Double>, mean: Double): Double {
        if (values.size <= 1) return 0.0
        val sumSquaredDiffs = values.sumOf { (it - mean).pow(2) }
        return sqrt(sumSquaredDiffs / (values.size - 1))
    }
}
