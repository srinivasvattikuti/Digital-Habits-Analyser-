package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppCategory
import com.example.data.model.AppInfoEntity
import com.example.data.model.AppRecommendationEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.DailyAggregateEntity
import com.example.data.model.HabitInsightEntity
import com.example.data.model.UsageEventEntity
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

    val dashboardState: StateFlow<DashboardUiState> = combine(
        _selectedFilter,
        repository.allAggregates,
        repository.recentEvents,
        repository.latestInsight,
        repository.recommendations,
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
        val syncing = args[5] as Boolean
        val analyzing = args[6] as Boolean
        val chatLoading = args[7] as Boolean
        val statusMsg = args[8] as? String

        // Compute startDate string
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
            _statusMessage.value = "Generating rich 7-day multi-app habit history..."
            repository.loadRichDemoData()
            _statusMessage.value = "Multi-day habit database populated and analyzed!"
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
