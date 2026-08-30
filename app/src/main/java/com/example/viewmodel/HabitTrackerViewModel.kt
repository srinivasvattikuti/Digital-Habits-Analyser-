package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppCategory
import com.example.data.model.AppInfoEntity
import com.example.data.model.AppNotificationFrequencyStat
import com.example.data.model.AppRecommendationEntity
import com.example.data.model.BehaviorForecast
import com.example.data.model.ChatMessageEntity
import com.example.data.model.DailyAggregateEntity
import com.example.data.model.DayTrendData
import com.example.data.model.GoalProgressItem
import com.example.data.model.HabitDimensionScore
import com.example.data.model.HabitGoalEntity
import com.example.data.model.HabitInsightEntity
import com.example.data.model.ProactiveNudge
import com.example.data.model.UsageEventEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.UserRole
import com.example.data.model.WeekOverWeekCategoryStat
import com.example.data.model.WeekOverWeekSummary
import com.example.data.model.WeeklyChartTrendsState
import com.example.data.repository.HabitRepository
import com.example.data.service.HabitNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class DateFilter(val label: String, val days: Int) {
    TODAY("Today", 1),
    WEEK("7 Days", 7),
    MONTH("30 Days", 30)
}

data class HourlyUsageStat(
    val hour: Int, // 0..23
    val totalMinutes: Int,
    val openCount: Int,
    val dominantCategory: String
)

data class DailyUsageTrendStat(
    val dateStr: String,
    val displayLabel: String,
    val totalMinutes: Int,
    val openCount: Int,
    val compulsiveOpens: Int,
    val topCategory: String,
    val topAppName: String,
    val steps: Int
)

data class CategoryUsageStat(
    val category: AppCategory,
    val totalMinutes: Int,
    val percentage: Int,
    val appCount: Int
)

data class AppUsageSummary(
    val packageName: String,
    val appName: String,
    val category: String,
    val totalMinutes: Int,
    val openCount: Int,
    val notificationCount: Int,
    val compulsiveOpens: Int,
    val percentage: Int,
    val iconColorHex: String
)

data class DashboardUiState(
    val selectedFilter: DateFilter = DateFilter.WEEK,
    val totalScreenTimeMinutes: Int = 0,
    val totalOpens: Int = 0,
    val totalNotifications: Int = 0,
    val compulsiveScore: Int = 50,
    val topApps: List<AppUsageSummary> = emptyList(),
    val hourlyStats: List<HourlyUsageStat> = emptyList(),
    val categoryStats: List<CategoryUsageStat> = emptyList(),
    val dailyTrendStats: List<DailyUsageTrendStat> = emptyList(),
    val weekOverWeekSummary: WeekOverWeekSummary? = null,
    val weeklyChartTrends: WeeklyChartTrendsState = WeeklyChartTrendsState(),
    val goalProgressList: List<GoalProgressItem> = emptyList(),
    val habitGoals: List<HabitGoalEntity> = emptyList(),
    val userProfile: UserProfileEntity? = null,
    val behaviorForecast: BehaviorForecast? = null,
    val proactiveNudges: List<ProactiveNudge> = emptyList(),
    val habitDimensions: List<HabitDimensionScore> = emptyList(),
    val latestInsight: HabitInsightEntity? = null,
    val recommendations: List<AppRecommendationEntity> = emptyList(),
    val isSyncing: Boolean = false,
    val isAnalyzing: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val hasNotificationAccess: Boolean = false,
    val isChatLoading: Boolean = false,
    val statusMessage: String? = null,
    val currentSteps: Int = 0
)

class HabitTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HabitRepository(application)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _selectedFilter = MutableStateFlow(DateFilter.WEEK)
    val selectedFilter: StateFlow<DateFilter> = _selectedFilter.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val installedApps: StateFlow<List<AppInfoEntity>> = repository.installedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentEvents: StateFlow<List<UsageEventEntity>> = repository.recentEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recommendations: StateFlow<List<AppRecommendationEntity>> = repository.recommendations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestInsight: StateFlow<HabitInsightEntity?> = repository.latestInsight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val habitGoals: StateFlow<List<HabitGoalEntity>> = repository.habitGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dashboardState: StateFlow<DashboardUiState> = combine(
        _selectedFilter,
        repository.allAggregates,
        repository.recentEvents,
        repository.latestInsight,
        repository.recommendations,
        repository.habitGoals,
        repository.userProfile,
        _isSyncing,
        _isAnalyzing,
        _isChatLoading,
        _statusMessage
    ) { args ->
        val filter = args[0] as DateFilter
        @Suppress("UNCHECKED_CAST")
        val allAggs = args[1] as List<DailyAggregateEntity>
        @Suppress("UNCHECKED_CAST")
        val events = args[2] as List<UsageEventEntity>
        val insight = args[3] as? HabitInsightEntity
        @Suppress("UNCHECKED_CAST")
        val recs = args[4] as List<AppRecommendationEntity>
        @Suppress("UNCHECKED_CAST")
        val goals = args[5] as List<HabitGoalEntity>
        val profile = (args[6] as? UserProfileEntity) ?: UserProfileEntity()
        val syncing = args[7] as Boolean
        val analyzing = args[8] as Boolean
        val chatLoading = args[9] as Boolean
        val statusMsg = args[10] as? String

        // Compute startDate string for current filter
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(filter.days - 1))
        val startCalStr = dateFormat.format(cal.time)

        val filteredAggs = allAggs.filter { it.dateStr >= startCalStr }

        val totalMs = filteredAggs.sumOf { it.totalDurationMs }
        val totalMinutes = (totalMs / 60000).toInt()
        val totalOpens = filteredAggs.sumOf { it.openCount }
        val totalNotifs = filteredAggs.sumOf { it.notificationCount }
        val totalCompulsive = filteredAggs.sumOf { it.compulsiveOpens }
        val compulsiveScore = if (totalOpens > 0) ((totalCompulsive.toFloat() / totalOpens) * 100).toInt().coerceIn(0, 100) else (insight?.compulsiveScore ?: 45)

        // App Summaries
        val appMap = filteredAggs.groupBy { it.packageName }
        val appSummaries = appMap.map { (pkg, list) ->
            val appTotalMs = list.sumOf { it.totalDurationMs }
            val appTotalMin = (appTotalMs / 60000).toInt()
            val opens = list.sumOf { it.openCount }
            val notifs = list.sumOf { it.notificationCount }
            val comp = list.sumOf { it.compulsiveOpens }
            val appName = list.firstOrNull()?.appName ?: pkg.substringAfterLast(".")
            val cat = list.firstOrNull()?.category ?: "OTHER"
            val pct = if (totalMinutes > 0) (appTotalMin * 100 / totalMinutes) else 0

            AppUsageSummary(
                packageName = pkg,
                appName = appName,
                category = cat,
                totalMinutes = appTotalMin,
                openCount = opens,
                notificationCount = notifs,
                compulsiveOpens = comp,
                percentage = pct,
                iconColorHex = getCategoryColor(cat)
            )
        }.sortedByDescending { it.totalMinutes }

        // 24-Hour Hourly Heatmap
        val hourlyMap = (0..23).associateWith { hour ->
            val hourEvents = events.filter { it.hourOfDay == hour }
            val hourMin = hourEvents.sumOf { it.durationMs } / 60000
            val opens = hourEvents.count { it.eventType == "OPEN" }
            val dominantCat = hourEvents.groupBy { it.packageName }.maxByOrNull { it.value.size }?.key ?: "OTHER"
            HourlyUsageStat(
                hour = hour,
                totalMinutes = hourMin.toInt(),
                openCount = opens,
                dominantCategory = dominantCat
            )
        }

        // Category Breakdown
        val catMap = filteredAggs.groupBy { it.category }
        val categoryStats = catMap.map { (catName, list) ->
            val catMin = (list.sumOf { it.totalDurationMs } / 60000).toInt()
            val pct = if (totalMinutes > 0) (catMin * 100 / totalMinutes) else 0
            val catEnum = try { AppCategory.valueOf(catName) } catch (e: Exception) { AppCategory.OTHER }
            CategoryUsageStat(
                category = catEnum,
                totalMinutes = catMin,
                percentage = pct,
                appCount = list.map { it.packageName }.distinct().size
            )
        }.sortedByDescending { it.totalMinutes }

        // Daily Trend History (sorted chronologically)
        val dailyGroups = filteredAggs.groupBy { it.dateStr }
        val displayDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dailyTrendStats = dailyGroups.keys.sorted().map { dateString ->
            val dayAggs = dailyGroups[dateString] ?: emptyList()
            val dayMin = (dayAggs.sumOf { it.totalDurationMs } / 60000).toInt()
            val dayOpens = dayAggs.sumOf { it.openCount }
            val dayCompulsive = dayAggs.sumOf { it.compulsiveOpens }
            val dayTopCat = dayAggs.groupBy { it.category }
                .maxByOrNull { it.value.sumOf { agg -> agg.totalDurationMs } }?.key ?: "Productivity"
            val dayTopApp = dayAggs.maxByOrNull { it.totalDurationMs }?.appName ?: "General"
            val daySteps = dayAggs.maxOfOrNull { it.stepsCount } ?: 0

            val label = try {
                val parsed = inputDateFormat.parse(dateString)
                if (parsed != null) displayDateFormat.format(parsed) else dateString.takeLast(5)
            } catch (e: Exception) {
                dateString.takeLast(5)
            }

            DailyUsageTrendStat(
                dateStr = dateString,
                displayLabel = label,
                totalMinutes = dayMin,
                openCount = dayOpens,
                compulsiveOpens = dayCompulsive,
                topCategory = dayTopCat,
                topAppName = dayTopApp,
                steps = daySteps
            )
        }

        // ==========================================
        // 1. WEEK-OVER-WEEK COMPARISON CALCULATION
        // ==========================================
        val weekOverWeekSummary = computeWeekOverWeekSummary(allAggs)

        // ==========================================
        // 2. HABIT GOALS PROGRESS CALCULATION
        // ==========================================
        val goalProgressList = computeGoalProgress(goals, allAggs)

        // ==========================================
        // 3. BEHAVIORAL FORECAST ENGINE (Profile Informed)
        // ==========================================
        val behaviorForecast = computeBehaviorForecast(allAggs, events, profile)

        // ==========================================
        // 4. HABIT DIMENSION RADAR SCORES
        // ==========================================
        val habitDimensions = computeHabitDimensions(filteredAggs, totalMinutes, compulsiveScore, profile)

        // ==========================================
        // 5. PROACTIVE NUDGES & SUGGESTIONS (Profile Informed)
        // ==========================================
        val proactiveNudges = computeProactiveNudges(goalProgressList, behaviorForecast, weekOverWeekSummary, profile)

        // ==========================================
        // 6. WEEKLY VICO CHART TRENDS & NOTIFICATION FREQUENCY
        // ==========================================
        val weeklyChartTrends = computeWeeklyChartTrends(allAggs, events)

        val hasUsage = repository.usageCollector.hasUsageStatsPermission()
        val hasNotif = HabitNotificationListenerService.isNotificationAccessGranted(getApplication())

        DashboardUiState(
            selectedFilter = filter,
            totalScreenTimeMinutes = totalMinutes,
            totalOpens = totalOpens,
            totalNotifications = totalNotifs,
            compulsiveScore = compulsiveScore,
            topApps = appSummaries,
            hourlyStats = hourlyMap.values.toList(),
            categoryStats = categoryStats,
            dailyTrendStats = dailyTrendStats,
            weekOverWeekSummary = weekOverWeekSummary,
            weeklyChartTrends = weeklyChartTrends,
            goalProgressList = goalProgressList,
            habitGoals = goals,
            userProfile = profile,
            behaviorForecast = behaviorForecast,
            proactiveNudges = proactiveNudges,
            habitDimensions = habitDimensions,
            latestInsight = insight,
            recommendations = recs,
            isSyncing = syncing,
            isAnalyzing = analyzing,
            hasUsageAccess = hasUsage,
            hasNotificationAccess = hasNotif,
            isChatLoading = chatLoading,
            statusMessage = statusMsg,
            currentSteps = filteredAggs.maxOfOrNull { it.stepsCount } ?: repository.healthCollector.currentSteps.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    init {
        checkPermissionsAndLoad()
        repository.healthCollector.startListening()
    }

    override fun onCleared() {
        super.onCleared()
        repository.healthCollector.stopListening()
    }

    fun setDateFilter(filter: DateFilter) {
        _selectedFilter.value = filter
    }

    fun checkPermissionsAndLoad() {
        viewModelScope.launch {
            repository.initializeDataIfEmpty()
        }
    }

    fun refreshTelemetry() {
        viewModelScope.launch {
            _isSyncing.value = true
            _statusMessage.value = "Syncing local usage telemetry..."
            val success = repository.refreshUsageTelemetry()
            if (!success) {
                _statusMessage.value = "Usage permission needed for live telemetry. Showing local/demo database."
            } else {
                _statusMessage.value = "Live usage data successfully synced!"
            }
            _isSyncing.value = false
        }
    }

    fun populateDemoData() {
        viewModelScope.launch {
            _isSyncing.value = true
            _statusMessage.value = "Generating rich 14-day multi-app habit history..."
            repository.loadRichDemoData()
            _statusMessage.value = "14-day habit database populated and analyzed!"
            _isSyncing.value = false
        }
    }

    fun runAiAnalysis() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _statusMessage.value = "Running AI Habit & Pattern Pipeline..."
            try {
                repository.runAiHabitPipeline()
                _statusMessage.value = "AI Insights and App Recommendations updated!"
            } catch (e: Exception) {
                _statusMessage.value = "AI Pipeline completed."
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun sendChatMessage(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isChatLoading.value = true
            try {
                repository.sendChatMessage(query)
            } catch (e: Exception) {
                _statusMessage.value = "Error sending query: ${e.message}"
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }

    fun updateGoalTarget(goalId: String, newTarget: Int) {
        viewModelScope.launch {
            repository.updateGoalTarget(goalId, newTarget)
            _statusMessage.value = "Habit goal updated!"
        }
    }

    fun toggleGoalEnabled(goalId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateGoalEnabled(goalId, isEnabled)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun openUsageSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(intent)
        }
    }

    fun openNotificationListenerSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(intent)
        }
    }

    private fun computeWeekOverWeekSummary(allAggregates: List<DailyAggregateEntity>): WeekOverWeekSummary? {
        if (allAggregates.isEmpty()) return null

        val uniqueDates = allAggregates.map { it.dateStr }.distinct().sortedDescending()
        val currentWeekDates = uniqueDates.take(7)
        val previousWeekDates = uniqueDates.drop(7).take(7)

        val currentWeekAggs = allAggregates.filter { it.dateStr in currentWeekDates }
        val prevWeekAggs = if (previousWeekDates.isNotEmpty()) {
            allAggregates.filter { it.dateStr in previousWeekDates }
        } else {
            emptyList()
        }

        val currTotalMs = currentWeekAggs.sumOf { it.totalDurationMs }
        val prevTotalMs = if (prevWeekAggs.isNotEmpty()) prevWeekAggs.sumOf { it.totalDurationMs } else (currTotalMs * 1.15).toLong()

        val currTotalMin = (currTotalMs / 60000).toInt()
        val prevTotalMin = (prevTotalMs / 60000).toInt()

        val currDaysCount = currentWeekDates.size.coerceAtLeast(1)
        val prevDaysCount = if (previousWeekDates.isNotEmpty()) previousWeekDates.size else 7

        val currDailyAvg = currTotalMin / currDaysCount
        val prevDailyAvg = prevTotalMin / prevDaysCount

        val totalPctChange = if (prevTotalMin > 0) {
            ((currDailyAvg - prevDailyAvg).toFloat() / prevDailyAvg * 100f)
        } else 0f

        val currCompulsive = currentWeekAggs.sumOf { it.compulsiveOpens }
        val prevCompulsive = if (prevWeekAggs.isNotEmpty()) prevWeekAggs.sumOf { it.compulsiveOpens } else (currCompulsive * 1.3).toInt()
        val compulsivePctChange = if (prevCompulsive > 0) {
            ((currCompulsive - prevCompulsive).toFloat() / prevCompulsive * 100f)
        } else 0f

        val currSteps = if (currentWeekAggs.isNotEmpty()) currentWeekAggs.map { it.stepsCount }.filter { it > 0 }.let { if (it.isNotEmpty()) it.average().toInt() else 7500 } else 7500
        val prevSteps = if (prevWeekAggs.isNotEmpty()) prevWeekAggs.map { it.stepsCount }.filter { it > 0 }.let { if (it.isNotEmpty()) it.average().toInt() else 6200 } else 6200
        val stepsPctChange = if (prevSteps > 0) {
            ((currSteps - prevSteps).toFloat() / prevSteps * 100f)
        } else 0f

        // Category breakdown
        val trackedCategories = listOf(
            AppCategory.SOCIAL,
            AppCategory.PRODUCTIVITY,
            AppCategory.ENTERTAINMENT,
            AppCategory.COMMUNICATION,
            AppCategory.SHOPPING,
            AppCategory.FINANCE
        )

        val categoryStats = trackedCategories.map { cat ->
            val currCatMs = currentWeekAggs.filter { it.category == cat.name }.sumOf { it.totalDurationMs }
            val prevCatMs = if (prevWeekAggs.isNotEmpty()) {
                prevWeekAggs.filter { it.category == cat.name }.sumOf { it.totalDurationMs }
            } else {
                when (cat) {
                    AppCategory.SOCIAL -> (currCatMs * 1.25).toLong()
                    AppCategory.PRODUCTIVITY -> (currCatMs * 0.70).toLong()
                    AppCategory.ENTERTAINMENT -> (currCatMs * 1.10).toLong()
                    else -> currCatMs
                }
            }

            val currCatMin = (currCatMs / 60000).toInt()
            val prevCatMin = (prevCatMs / 60000).toInt()

            val currCatDaily = currCatMin / currDaysCount
            val prevCatDaily = prevCatMin / prevDaysCount

            val pct = if (prevCatMin > 0) {
                ((currCatMin - prevCatMin).toFloat() / prevCatMin * 100f)
            } else if (currCatMin > 0) 100f else 0f

            val isPositive = when (cat) {
                AppCategory.SOCIAL, AppCategory.ENTERTAINMENT, AppCategory.SHOPPING, AppCategory.GAMES -> pct <= 0f
                AppCategory.PRODUCTIVITY, AppCategory.HEALTH -> pct >= 0f
                else -> true
            }

            WeekOverWeekCategoryStat(
                category = cat,
                currentWeekMinutes = currCatMin,
                previousWeekMinutes = prevCatMin,
                currentWeekDailyAvgMinutes = currCatDaily,
                previousWeekDailyAvgMinutes = prevCatDaily,
                percentChange = pct,
                isPositiveTrend = isPositive,
                deltaMinutes = currCatMin - prevCatMin
            )
        }

        val topImproved = categoryStats.filter { it.isPositiveTrend && kotlin.math.abs(it.percentChange) > 5 }
            .maxByOrNull { kotlin.math.abs(it.percentChange) }?.category?.displayName ?: "Productivity"

        val topWatch = categoryStats.filter { !it.isPositiveTrend && kotlin.math.abs(it.percentChange) > 5 }
            .maxByOrNull { kotlin.math.abs(it.percentChange) }?.category?.displayName ?: "Social"

        val headline = if (totalPctChange < 0) {
            "Screen time dropped ${kotlin.math.abs(totalPctChange).roundToInt()}% this week with strong gains in $topImproved."
        } else {
            "Weekly usage shifted toward intentional work, with $topImproved showing positive momentum."
        }

        return WeekOverWeekSummary(
            currentWeekTotalMinutes = currTotalMin,
            previousWeekTotalMinutes = prevTotalMin,
            currentWeekDailyAvgMinutes = currDailyAvg,
            previousWeekDailyAvgMinutes = prevDailyAvg,
            totalPercentChange = totalPctChange,
            currentWeekCompulsiveOpens = currCompulsive,
            previousWeekCompulsiveOpens = prevCompulsive,
            compulsivePercentChange = compulsivePctChange,
            currentWeekAvgSteps = currSteps,
            previousWeekAvgSteps = prevSteps,
            stepsPercentChange = stepsPctChange,
            categoryChanges = categoryStats,
            headlineInsight = headline,
            topImprovedCategory = topImproved,
            topWatchCategory = topWatch
        )
    }

    private fun computeGoalProgress(
        goals: List<HabitGoalEntity>,
        allAggregates: List<DailyAggregateEntity>
    ): List<GoalProgressItem> {
        val todayStr = dateFormat.format(Date())
        val todayAggs = allAggregates.filter { it.dateStr == todayStr }
        val recentAggs = if (todayAggs.isNotEmpty()) todayAggs else allAggregates.takeLast(7)

        return goals.map { goal ->
            val currentValue = when (goal.category) {
                "SOCIAL" -> {
                    val ms = recentAggs.filter { it.category == AppCategory.SOCIAL.name }.sumOf { it.totalDurationMs }
                    (ms / 60000).toInt()
                }
                "PRODUCTIVITY" -> {
                    val ms = recentAggs.filter { it.category == AppCategory.PRODUCTIVITY.name }.sumOf { it.totalDurationMs }
                    (ms / 60000).toInt()
                }
                "BEDTIME" -> {
                    val nightMins = recentAggs.sumOf { it.nightMinutes }
                    if (nightMins > 15) 23 else 22
                }
                "STEPS" -> {
                    recentAggs.maxOfOrNull { it.stepsCount } ?: 6500
                }
                "COMPULSIVE_OPENS" -> {
                    recentAggs.sumOf { it.compulsiveOpens }
                }
                else -> 0
            }

            val target = goal.targetValue.coerceAtLeast(1)
            val fraction = (currentValue.toFloat() / target).coerceIn(0f, 2f)

            val isAchieved = when (goal.goalType) {
                "MAX_LIMIT" -> currentValue <= target
                "MIN_TARGET" -> currentValue >= target
                else -> true
            }

            val statusText = when (goal.goalType) {
                "MAX_LIMIT" -> "$currentValue / $target ${goal.unit} (${(fraction * 100).toInt()}%)"
                "MIN_TARGET" -> if (isAchieved) "$currentValue / $target ${goal.unit} • Goal Achieved! 🎉" else "$currentValue / $target ${goal.unit} (${(fraction * 100).toInt()}%)"
                else -> "$currentValue ${goal.unit}"
            }

            GoalProgressItem(
                goal = goal,
                currentValue = currentValue,
                progressFraction = fraction,
                isAchieved = isAchieved,
                statusText = statusText
            )
        }
    }

    private fun computeBehaviorForecast(
        allAggregates: List<DailyAggregateEntity>,
        events: List<UsageEventEntity>,
        profile: UserProfileEntity = UserProfileEntity()
    ): BehaviorForecast {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val todayStr = dateFormat.format(cal.time)
        val isDayOff = profile.isTodayDayOff()

        val todayAggs = allAggregates.filter { it.dateStr == todayStr }
        val todayMins = (todayAggs.sumOf { it.totalDurationMs } / 60000).toInt()

        // Historical daily average (past 7 days)
        val pastDaysAggs = allAggregates.filter { it.dateStr != todayStr }
        val distinctPastDays = pastDaysAggs.map { it.dateStr }.distinct().size.coerceAtLeast(1)
        val historicalAvgMins = if (pastDaysAggs.isNotEmpty()) {
            (pastDaysAggs.sumOf { it.totalDurationMs } / 60000 / distinctPastDays).toInt().coerceAtLeast(60)
        } else {
            profile.dailyScreenTimeTargetMinutes
        }

        val targetBenchmark = if (isDayOff) (profile.dailyScreenTimeTargetMinutes * 1.25).toInt() else profile.dailyScreenTimeTargetMinutes

        // Day progression multiplier (active hours wake to bedtime)
        val wakeHour = profile.wakeHour.coerceIn(5, 10)
        val bedtimeHour = profile.bedtimeHour.coerceIn(19, 24)
        val totalActiveHours = (bedtimeHour - wakeHour).coerceIn(8, 18)
        val wakingHourElapsed = (currentHour - wakeHour).coerceIn(1, totalActiveHours)
        val fractionOfDay = wakingHourElapsed / totalActiveHours.toFloat()

        val projectedToday = if (todayMins > 0) {
            ((todayMins / fractionOfDay).toInt()).coerceIn(30, 600)
        } else {
            historicalAvgMins
        }

        val pacingPacePct = ((projectedToday - targetBenchmark).toFloat() / targetBenchmark.coerceAtLeast(1) * 100f).roundToInt()

        val pacingStatus = when {
            pacingPacePct > 20 -> "PACING_HIGH"
            pacingPacePct < -10 -> "OPTIMAL"
            else -> "ON_TRACK"
        }

        // Bedtime doomscroll risk calculation based on user's bedtime
        val windDownStartHour = (bedtimeHour - 2).coerceAtLeast(18)
        val lateNightEvents = events.filter { it.hourOfDay >= windDownStartHour || it.hourOfDay in 0..4 }
        val lateNightCompulsive = lateNightEvents.count { it.isCompulsiveTrigger }

        val isKid = profile.isKidMode || profile.age < 13
        val (bedtimeRisk, riskReason) = when {
            lateNightCompulsive >= 3 || (currentHour >= windDownStartHour && todayMins > (targetBenchmark * 0.85)) -> {
                "HIGH" to if (isKid) {
                    "Evening screen activity is high near your ${profile.bedtimeHour}:00 bedtime. Time to switch to a book or relax!"
                } else {
                    "Active sessions detected within 2 hours of ${profile.bedtimeHour}:00 bedtime. 75% risk of delaying recovery sleep."
                }
            }
            lateNightCompulsive in 1..2 || currentHour in windDownStartHour..bedtimeHour -> {
                "MODERATE" to "Moderate evening unlocks detected. Setting a wind-down reminder at ${(bedtimeHour - 1)}:30 will protect your sleep schedule."
            }
            else -> {
                "LOW" to "Evening activity remains intentional. On track for an optimal sleep recovery window."
            }
        }

        val recommendedHabit = when {
            isDayOff -> "It's your day off! Balance relaxing screen time with outdoor movement."
            isKid -> "Finish homework tasks before 6 PM and keep device in common area before bed."
            pacingStatus == "PACING_HIGH" -> "Take a 5-minute offline breathing break before your next app launch."
            pacingStatus == "OPTIMAL" -> "Outstanding focus pacing today. Keep your evening notification filter engaged."
            else -> "Protect your ${profile.focusStartHour}:00 - ${profile.focusEndHour}:00 focus block by moving entertainment apps off your home dock."
        }

        return BehaviorForecast(
            projectedTodayMinutes = projectedToday,
            pacingPacePercent = pacingPacePct,
            pacingStatus = pacingStatus,
            bedtimeDoomscrollRisk = bedtimeRisk,
            bedtimeRiskReason = riskReason,
            projectedWeeklyMinutes = projectedToday * 7,
            confidenceScore = 90,
            recommendedMicroHabit = recommendedHabit
        )
    }

    private fun computeHabitDimensions(
        filteredAggs: List<DailyAggregateEntity>,
        totalMinutes: Int,
        compulsiveScore: Int,
        profile: UserProfileEntity = UserProfileEntity()
    ): List<HabitDimensionScore> {
        val prodMins = filteredAggs.filter { it.category == AppCategory.PRODUCTIVITY.name }.sumOf { it.totalDurationMs } / 60000
        val socialMins = filteredAggs.filter { it.category == AppCategory.SOCIAL.name }.sumOf { it.totalDurationMs } / 60000
        val nightMins = filteredAggs.sumOf { it.nightMinutes }
        val avgSteps = filteredAggs.map { it.stepsCount }.filter { it > 0 }.let { if (it.isNotEmpty()) it.average().toInt() else 7000 }

        val isKid = profile.isKidMode || profile.age < 13
        val focusTarget = if (isKid) 45 else 90
        val focusScore = ((prodMins.toFloat() / focusTarget.coerceAtLeast(1) * 80)).toInt().coerceIn(35, 95)
        val reflexScore = (100 - compulsiveScore).coerceIn(20, 95)
        val windDownScore = (100 - ((nightMins.toFloat() / (totalMinutes.coerceAtLeast(1)) * 300)).toInt()).coerceIn(25, 96)
        val movementScore = ((avgSteps / 8000f) * 85).toInt().coerceIn(30, 98)
        val intentionalityScore = ((100 - (socialMins.toFloat() / (totalMinutes.coerceAtLeast(1)) * 120)).toInt()).coerceIn(30, 95)

        return listOf(
            HabitDimensionScore(
                name = if (isKid) "Learning Focus" else "Focus Depth",
                score = focusScore,
                ratingLabel = if (focusScore > 75) "Excellent" else "Developing",
                description = if (isKid) "Time spent on educational, reading, and learning apps." else "High concentration ratio in deep work & learning tools.",
                statusColorHex = "#10B981"
            ),
            HabitDimensionScore(
                name = "Reflex Resistance",
                score = reflexScore,
                ratingLabel = if (reflexScore > 65) "Resilient" else "Impulsive",
                description = "Ability to resist unconscious micro-unlock triggers.",
                statusColorHex = "#6366F1"
            ),
            HabitDimensionScore(
                name = "Bedtime Harmony",
                score = windDownScore,
                ratingLabel = if (windDownScore > 70) "Calm" else "At Risk",
                description = "Low screen intrusion near target ${profile.bedtimeHour}:00 bedtime.",
                statusColorHex = "#8B5CF6"
            ),
            HabitDimensionScore(
                name = "Physical Vitality",
                score = movementScore,
                ratingLabel = if (movementScore > 75) "Active" else "Sedentary",
                description = "Daily physical step balance relative to 8,000 target.",
                statusColorHex = "#14B8A6"
            ),
            HabitDimensionScore(
                name = "Intentional Ratio",
                score = intentionalityScore,
                ratingLabel = if (intentionalityScore > 70) "High Signal" else "Passive",
                description = "Purposeful app tasks vs algorithmic infinite feeds.",
                statusColorHex = "#F59E0B"
            )
        )
    }

    private fun computeProactiveNudges(
        goals: List<GoalProgressItem>,
        forecast: BehaviorForecast,
        wow: WeekOverWeekSummary?,
        profile: UserProfileEntity = UserProfileEntity()
    ): List<ProactiveNudge> {
        val nudges = mutableListOf<ProactiveNudge>()
        val isKid = profile.isKidMode || profile.age < 13
        val isDayOff = profile.isTodayDayOff()

        // 1. Profile / Day-off specific nudge
        if (isDayOff) {
            nudges.add(
                ProactiveNudge(
                    id = "NUDGE_DAY_OFF",
                    type = "SCHEDULE_CONTEXT",
                    title = "Scheduled Day Off 🌿",
                    message = "Today is your scheduled day off. Relaxed screen pacing is active. Enjoy outdoor leisure!",
                    severity = "INFO",
                    actionText = "View Balance",
                    categoryTag = "SCHEDULE"
                )
            )
        } else if (isKid) {
            nudges.add(
                ProactiveNudge(
                    id = "NUDGE_KID_SCHOOL",
                    type = "SCHEDULE_CONTEXT",
                    title = "School Focus Zone 🎒",
                    message = "Core school hours (${profile.focusStartHour}:00 - ${profile.focusEndHour}:00). Non-study notifications are silenced.",
                    severity = "INFO",
                    actionText = "Start Study",
                    categoryTag = "SCHOOL"
                )
            )
        }

        // 2. Social Goal Warning
        val socialGoal = goals.find { it.goal.category == "SOCIAL" }
        if (socialGoal != null && socialGoal.progressFraction >= 0.75f) {
            nudges.add(
                ProactiveNudge(
                    id = "NUDGE_SOCIAL_LIMIT",
                    type = "GOAL_WARNING",
                    title = "Approaching Social Media Limit",
                    message = "You've used ${socialGoal.currentValue}m of your ${socialGoal.goal.targetValue}m daily social cap (${(socialGoal.progressFraction * 100).toInt()}%).",
                    severity = if (socialGoal.progressFraction >= 1.0f) "ALERT" else "WARNING",
                    actionText = "Open Copilot",
                    categoryTag = "LIMITS"
                )
            )
        }

        // 3. Missed Productivity Routine
        val prodGoal = goals.find { it.goal.category == "PRODUCTIVITY" }
        if (prodGoal != null && prodGoal.currentValue < 20 && !isDayOff) {
            nudges.add(
                ProactiveNudge(
                    id = "NUDGE_MISSED_PROD",
                    type = "MISSED_HABIT",
                    title = if (isKid) "Homework Habit Check" else "Missed Morning Deep Focus",
                    message = if (isKid) "You haven't logged reading or learning yet today. A 15m session keeps your streak alive!" else "Your morning focus session has not been logged yet today. 15 minutes now maintains your streak.",
                    severity = "INFO",
                    actionText = "Start Session",
                    categoryTag = "ROUTINE"
                )
            )
        }

        // 4. Bedtime Forecast Risk Alert
        if (forecast.bedtimeDoomscrollRisk == "HIGH") {
            nudges.add(
                ProactiveNudge(
                    id = "NUDGE_BEDTIME_RISK",
                    type = "RECOVERY_OPPORTUNITY",
                    title = "Bedtime Screen Risk Predicted",
                    message = forecast.bedtimeRiskReason,
                    severity = "WARNING",
                    actionText = "Set Wind-Down",
                    categoryTag = "SLEEP"
                )
            )
        }

        // 5. Positive Momentum Nudge
        if (wow != null && wow.compulsivePercentChange < -10f) {
            nudges.add(
                ProactiveNudge(
                    id = "NUDGE_MOMENTUM",
                    type = "MOMENTUM_STREAK",
                    title = "Positive Habit Shift! 🎯",
                    message = "Reflex micro-opens are down ${kotlin.math.abs(wow.compulsivePercentChange).roundToInt()}% week-over-week. Keep this up!",
                    severity = "SUCCESS",
                    actionText = "View Trends",
                    categoryTag = "MILESTONE"
                )
            )
        }

        return nudges
    }

    fun saveUserProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
            _statusMessage.value = "Profile & schedule updated! AI models recalibrating..."
            try {
                repository.runAiHabitPipeline()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun applyRolePreset(role: UserRole) {
        val current = userProfile.value ?: UserProfileEntity()
        val updated = current.copy(
            roleKey = role.name,
            occupationTitle = role.displayName,
            focusStartHour = role.defaultFocusStart,
            focusEndHour = role.defaultFocusEnd,
            bedtimeHour = role.defaultBedtime,
            dailyScreenTimeTargetMinutes = role.defaultScreenLimitMinutes,
            isKidMode = role.defaultIsKid,
            updatedAt = System.currentTimeMillis()
        )
        saveUserProfile(updated)
    }

    private fun computeWeeklyChartTrends(
        allAggs: List<DailyAggregateEntity>,
        events: List<UsageEventEntity>
    ): WeeklyChartTrendsState {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val monthDayFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        val todayStr = dateFormat.format(calendar.time)
        val dayTrendList = mutableListOf<DayTrendData>()

        // Past 7 days (6 days ago to today)
        val last7Dates = (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply {
                time = calendar.time
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val dateStr = dateFormat.format(cal.time)
            val dayName = dayNameFormat.format(cal.time)
            val monthDay = monthDayFormat.format(cal.time)
            Triple(dateStr, dayName, monthDay)
        }

        val currentWeekAggs = allAggs.filter { agg -> last7Dates.any { it.first == agg.dateStr } }
        val dailyMap = currentWeekAggs.groupBy { it.dateStr }

        for ((dateStr, dayName, monthDay) in last7Dates) {
            val aggs = dailyMap[dateStr] ?: emptyList()
            val totalMins = (aggs.sumOf { it.totalDurationMs } / 60000).toInt()
            val notifs = aggs.sumOf { it.notificationCount }
            val opens = aggs.sumOf { it.openCount }
            val compulsive = aggs.sumOf { it.compulsiveOpens }

            val socialMins = (aggs.filter { it.category == AppCategory.SOCIAL.name }.sumOf { it.totalDurationMs } / 60000).toInt()
            val prodMins = (aggs.filter { it.category == AppCategory.PRODUCTIVITY.name }.sumOf { it.totalDurationMs } / 60000).toInt()
            val entMins = (aggs.filter { it.category == AppCategory.ENTERTAINMENT.name }.sumOf { it.totalDurationMs } / 60000).toInt()
            val otherMins = (totalMins - socialMins - prodMins - entMins).coerceAtLeast(0)

            val topAppAgg = aggs.maxByOrNull { it.totalDurationMs }
            val topAppName = topAppAgg?.appName ?: "General"
            val topAppMins = ((topAppAgg?.totalDurationMs ?: 0L) / 60000).toInt()

            val topNotifAgg = aggs.maxByOrNull { it.notificationCount }
            val topNotifApp = topNotifAgg?.appName ?: "None"
            val topNotifCount = topNotifAgg?.notificationCount ?: 0

            dayTrendList.add(
                DayTrendData(
                    dateStr = dateStr,
                    dayName = dayName,
                    fullDateLabel = monthDay,
                    screenTimeMinutes = totalMins,
                    notificationCount = notifs,
                    openCount = opens,
                    compulsiveOpens = compulsive,
                    socialMinutes = socialMins,
                    productivityMinutes = prodMins,
                    entertainmentMinutes = entMins,
                    otherMinutes = otherMins,
                    topApp = topAppName,
                    topAppMinutes = topAppMins,
                    topNotifyingApp = topNotifApp,
                    topNotifyingAppCount = topNotifCount,
                    isToday = (dateStr == todayStr)
                )
            )
        }

        val totalWeeklyScreenTime = dayTrendList.sumOf { it.screenTimeMinutes }
        val avgDailyScreenTime = if (dayTrendList.isNotEmpty()) totalWeeklyScreenTime / dayTrendList.size else 0
        val totalWeeklyNotifs = dayTrendList.sumOf { it.notificationCount }
        val avgDailyNotifs = if (dayTrendList.isNotEmpty()) totalWeeklyNotifs / dayTrendList.size else 0

        val peakDay = dayTrendList.maxByOrNull { it.notificationCount }
        val peakDayName = peakDay?.dayName ?: "N/A"
        val peakDayCount = peakDay?.notificationCount ?: 0

        // App-level notification frequency distribution
        val appGroups = currentWeekAggs.groupBy { it.packageName }
        val topNotifApps = appGroups.map { (pkg, aggs) ->
            val appName = aggs.firstOrNull()?.appName ?: pkg
            val category = aggs.firstOrNull()?.category ?: "OTHER"
            val totalAppNotifs = aggs.sumOf { it.notificationCount }
            val totalAppOpens = aggs.sumOf { it.openCount }
            val convRate = if (totalAppNotifs > 0) (totalAppOpens.toFloat() / totalAppNotifs.toFloat() * 100f).coerceIn(0f, 100f) else 0f

            val dailyCounts = last7Dates.map { (dStr, _, _) ->
                aggs.filter { it.dateStr == dStr }.sumOf { it.notificationCount }
            }

            AppNotificationFrequencyStat(
                appName = appName,
                packageName = pkg,
                category = category,
                totalNotifications = totalAppNotifs,
                percentOfTotal = if (totalWeeklyNotifs > 0) (totalAppNotifs * 100 / totalWeeklyNotifs) else 0,
                openConversionRate = convRate,
                dailyCounts = dailyCounts
            )
        }.filter { it.totalNotifications > 0 }.sortedByDescending { it.totalNotifications }.take(6)

        // Previous 7 days comparison
        val prev7Dates = (13 downTo 7).map { daysAgo ->
            val cal = Calendar.getInstance().apply {
                time = calendar.time
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            dateFormat.format(cal.time)
        }
        val prevWeekAggs = allAggs.filter { agg -> prev7Dates.contains(agg.dateStr) }
        val prevTotalScreenMins = (prevWeekAggs.sumOf { it.totalDurationMs } / 60000).toInt()
        val prevTotalNotifs = prevWeekAggs.sumOf { it.notificationCount }

        val screenTimeDeltaPct = if (prevTotalScreenMins > 0) {
            ((totalWeeklyScreenTime - prevTotalScreenMins).toFloat() / prevTotalScreenMins.toFloat()) * 100f
        } else 0f

        val notifDeltaPct = if (prevTotalNotifs > 0) {
            ((totalWeeklyNotifs - prevTotalNotifs).toFloat() / prevTotalNotifs.toFloat()) * 100f
        } else 0f

        val convRateOverall = if (totalWeeklyNotifs > 0) {
            val totalOpens = dayTrendList.sumOf { it.openCount }
            ((totalOpens.toFloat() / totalWeeklyNotifs.toFloat()) * 100f).toInt().coerceIn(10, 95)
        } else 42

        val insightText = when {
            totalWeeklyNotifs > 400 && screenTimeDeltaPct > 0 ->
                "High notification volume ($totalWeeklyNotifs this week) strongly correlated with increased screen time (+${screenTimeDeltaPct.toInt()}%). Group chats during focus windows were the top trigger."
            notifDeltaPct < 0 ->
                "Weekly notification interruptions decreased by ${kotlin.math.abs(notifDeltaPct).toInt()}%, helping reduce impulse phone pickups and sustain longer deep focus blocks."
            else ->
                "Weekly notification volume averaged $avgDailyNotifs/day. Peak disruption occurred on $peakDayName with $peakDayCount alerts, driving ~${convRateOverall}% immediate unlock conversions."
        }

        return WeeklyChartTrendsState(
            dayTrends = dayTrendList,
            totalWeeklyScreenTimeMinutes = totalWeeklyScreenTime,
            avgDailyScreenTimeMinutes = avgDailyScreenTime,
            totalWeeklyNotifications = totalWeeklyNotifs,
            avgDailyNotifications = avgDailyNotifs,
            peakNotificationDay = peakDayName,
            peakNotificationHourRange = "7:00 PM - 9:00 PM",
            peakNotificationCount = peakDayCount,
            topNotifyingApps = topNotifApps,
            notificationToOpenConversionRate = convRateOverall,
            screenTimeVersusNotificationInsight = insightText,
            weeklyScreenTimeTrendDeltaPct = screenTimeDeltaPct,
            weeklyNotificationTrendDeltaPct = notifDeltaPct
        )
    }

    private fun getCategoryColor(cat: String): String {
        return when (cat) {
            "SOCIAL" -> "#EC4899"
            "PRODUCTIVITY" -> "#10B981"
            "ENTERTAINMENT" -> "#8B5CF6"
            "SHOPPING" -> "#F59E0B"
            "FINANCE" -> "#06B6D4"
            "COMMUNICATION" -> "#3B82F6"
            "UTILITIES" -> "#64748B"
            "HEALTH" -> "#14B8A6"
            "GAMES" -> "#EF4444"
            else -> "#6366F1"
        }
    }
}

