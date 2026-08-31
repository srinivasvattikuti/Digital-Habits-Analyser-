package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProactiveNudge
import com.example.data.sdui.CardType
import com.example.data.sdui.ComponentParameters
import com.example.data.sdui.DashboardLayoutConfig
import com.example.data.sdui.DynamicComponentConfig
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.PolishAiCallout
import com.example.ui.theme.PolishLightRose
import com.example.ui.theme.PolishMediumRose
import com.example.ui.theme.PolishOnPrimaryContainer
import com.example.ui.theme.PolishOnReco
import com.example.ui.theme.PolishOutline
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishRecoContainer
import com.example.ui.theme.PolishSurfaceVariant
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.theme.PolishWineDark
import com.example.ui.theme.RoseRed
import com.example.ui.theme.SunsetAmber
import com.example.viewmodel.DashboardUiState
import com.example.viewmodel.DateFilter

/**
 * Dynamic Server-Driven UI (SDUI) Renderer for the HabitFlow Dashboard.
 * Parses the incoming DashboardLayoutConfig JSON and dynamically renders native components
 * according to ordered positions, visibility states, custom titles, and parameter filters.
 */
fun LazyListScope.renderDynamicDashboardComponents(
    layoutConfig: DashboardLayoutConfig,
    state: DashboardUiState,
    onFilterSelected: (DateFilter) -> Unit,
    onRunAiAnalysis: () -> Unit,
    onNavigateToChat: () -> Unit,
    onAskAboutHour: (Int) -> Unit,
    onNudgeAction: (ProactiveNudge) -> Unit,
    onUpdateGoalTarget: (String, Int) -> Unit,
    onToggleGoal: (String, Boolean) -> Unit,
    onPopulateDemoData: () -> Unit,
    onOpenCustomizer: () -> Unit,
    onSelectHalfLife: (Float) -> Unit = {}
) {
    val spacing: Dp = when (layoutConfig.density) {
        "COMPACT" -> 8.dp
        "SPACIOUS" -> 18.dp
        else -> 12.dp
    }

    // Filter Chips & AI Analyze row
    item(key = "sdui_filter_controls") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateFilter.entries.forEach { filter ->
                    ElevatedFilterChip(
                        selected = state.selectedFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = { Text(filter.label) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}"),
                        colors = FilterChipDefaults.elevatedFilterChipColors(
                            selectedContainerColor = PolishPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = PolishTextSecondary
                        )
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Customize with AI Button
                OutlinedButton(
                    onClick = onOpenCustomizer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PolishPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("open_sdui_customizer_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Customize UI",
                        modifier = Modifier.size(14.dp),
                        tint = PolishPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Customize",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PolishPrimary
                    )
                }

                // AI Action / Pipeline Button
                Button(
                    onClick = onRunAiAnalysis,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("run_ai_pipeline_button")
                ) {
                    if (state.isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("Analyze", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    // Sort visible components by position
    val visibleComponents = layoutConfig.components
        .filter { it.visible }
        .sortedBy { it.position }

    visibleComponents.forEach { component ->
        renderIndividualComponent(
            component = component,
            state = state,
            onNavigateToChat = onNavigateToChat,
            onAskAboutHour = onAskAboutHour,
            onNudgeAction = onNudgeAction,
            onUpdateGoalTarget = onUpdateGoalTarget,
            onToggleGoal = onToggleGoal,
            onSelectHalfLife = onSelectHalfLife
        )
    }

    // Demo Data Generation Footer
    item(key = "sdui_demo_data_footer") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedButton(
                onClick = onPopulateDemoData,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, PolishOutline),
                modifier = Modifier.testTag("populate_demo_data_btn")
            ) {
                Text(
                    "Populate 7-Day Demo History",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = PolishPrimary
                )
            }
        }
    }
}

/**
 * Dispatches and renders individual component based on its dynamic CardType.
 */
private fun LazyListScope.renderIndividualComponent(
    component: DynamicComponentConfig,
    state: DashboardUiState,
    onNavigateToChat: () -> Unit,
    onAskAboutHour: (Int) -> Unit,
    onNudgeAction: (ProactiveNudge) -> Unit,
    onUpdateGoalTarget: (String, Int) -> Unit,
    onToggleGoal: (String, Boolean) -> Unit,
    onSelectHalfLife: (Float) -> Unit = {}
) {
    val key = "sdui_comp_${component.id}_${component.position}"

    when (component.type) {
        CardType.HERO_USAGE -> {
            item(key = key) {
                DynamicHeroDailyUsageCard(
                    title = component.title ?: "DAILY ACTIVITY",
                    state = state,
                    onClick = onNavigateToChat
                )
            }
        }

        CardType.SUMMARY_METRICS -> {
            item(key = key) {
                DynamicSummaryMetricsGrid(
                    params = component.parameters,
                    state = state
                )
            }
        }

        CardType.PROACTIVE_NUDGES -> {
            if (state.proactiveNudges.isNotEmpty()) {
                item(key = key) {
                    ProactiveNudgesSection(
                        nudges = state.proactiveNudges,
                        onNudgeAction = onNudgeAction
                    )
                }
            }
        }

        CardType.WEEKLY_TRENDS -> {
            item(key = key) {
                WeeklyTrendsOverviewCard(
                    weeklyTrends = state.weeklyChartTrends,
                    dailyScreenBudgetMinutes = state.userProfile?.dailyScreenTimeTargetMinutes ?: 210,
                    onOpenInsightDeepDive = onNavigateToChat
                )
            }
        }

        CardType.NOTIFICATION_LEADERBOARD -> {
            if (state.weeklyChartTrends.topNotifyingApps.isNotEmpty()) {
                item(key = key) {
                    NotificationFrequencyLeaderboardCard(
                        apps = state.weeklyChartTrends.topNotifyingApps,
                        totalWeeklyNotifications = state.weeklyChartTrends.totalWeeklyNotifications
                    )
                }
            }
        }

        CardType.WEEK_OVER_WEEK -> {
            item(key = key) {
                WeekOverWeekComparisonCard(summary = state.weekOverWeekSummary)
            }
        }

        CardType.GOALS_TRACKER -> {
            item(key = key) {
                InteractiveHabitGoalsCard(
                    goalProgressList = state.goalProgressList,
                    onUpdateGoalTarget = onUpdateGoalTarget,
                    onToggleGoal = onToggleGoal
                )
            }
        }

        CardType.BEHAVIOR_FORECAST -> {
            item(key = key) {
                BehaviorForecastCard(
                    forecast = state.behaviorForecast,
                    onOpenCopilot = onNavigateToChat
                )
            }
        }

        CardType.RADAR_DIMENSIONS -> {
            item(key = key) {
                HabitDimensionsRadarCard(dimensions = state.habitDimensions)
            }
        }

        CardType.HOURLY_HEATMAP -> {
            item(key = key) {
                HourlyUsageHeatmapCard(
                    hourlyStats = state.hourlyStats,
                    aiSummary = state.latestInsight?.keyTakeaway,
                    onAskAboutHour = onAskAboutHour
                )
            }
        }

        CardType.CATEGORY_DISTRIBUTION -> {
            item(key = key) {
                CategoryDistributionCard(categoryStats = state.categoryStats)
            }
        }

        CardType.COMPULSIVE_GAUGE -> {
            item(key = key) {
                CompulsiveVsIntentionalGaugeCard(
                    compulsiveScore = state.compulsiveScore,
                    totalOpens = state.totalOpens,
                    compulsiveSummary = state.latestInsight?.compulsiveSummary ?: ""
                )
            }
        }

        CardType.TOP_APPS -> {
            val maxApps = component.parameters.maxAppsCount.coerceAtLeast(1)
            val appsToShow = state.topApps.take(maxApps)

            item(key = "${key}_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = component.title ?: "App Usage & Focus Dominance",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${state.topApps.size} apps tracked",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = PolishTextMuted
                    )
                }
            }

            items(appsToShow, key = { app -> "${key}_app_${app.packageName}" }) { app ->
                AppUsageRowItem(app = app)
            }
        }

        CardType.USAGE_OVER_TIME -> {
            item(key = key) {
                UsageOverTimeCard(dailyTrends = state.dailyTrendStats)
            }
        }

        CardType.RECOMMENDATION_BANNER -> {
            item(key = key) {
                DynamicRecommendationBanner(onClick = onNavigateToChat)
            }
        }

        CardType.AI_INSIGHT_BANNER -> {
            if (state.latestInsight != null) {
                item(key = key) {
                    DynamicAiInsightBanner(
                        title = component.title ?: "BEHAVIORAL ENGINE INSIGHT",
                        keyTakeaway = state.latestInsight.keyTakeaway,
                        compulsiveSummary = state.latestInsight.compulsiveSummary,
                        onClick = onNavigateToChat
                    )
                }
            }
        }

        CardType.RESEARCH_HABIT_SCIENCE -> {
            item(key = key) {
                ResearchHabitScienceCard(
                    metrics = state.researchMetrics,
                    onSelectHalfLife = onSelectHalfLife
                )
            }
        }

        CardType.LONGITUDINAL_BASELINE_COMPARISON -> {
            item(key = key) {
                LongitudinalBaselineVsRecentCard(
                    comparisons = state.longitudinalComparisons,
                    baselineDays = 30,
                    halfLifeDays = state.halfLifeDays
                )
            }
        }

        CardType.CIRCADIAN_SLEEP_IMPACT -> {
            item(key = key) {
                CircadianSleepImpactCard(
                    metrics = state.researchMetrics
                )
            }
        }

        CardType.AI_INCREMENTAL_MEMORY -> {
            item(key = key) {
                TokenEfficientAiMemoryCard(
                    memory = state.incrementalMemory
                )
            }
        }
    }
}

@Composable
fun DynamicHeroDailyUsageCard(
    title: String,
    state: DashboardUiState,
    onClick: () -> Unit
) {
    val hours = state.totalScreenTimeMinutes / 60
    val mins = state.totalScreenTimeMinutes % 60
    val timeFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("dynamic_hero_usage_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = PolishPrimaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(22.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PolishPrimary)
                    )
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            fontSize = 11.sp
                        ),
                        color = PolishOnPrimaryContainer
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = state.selectedFilter.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PolishWineDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 36.sp
                    ),
                    color = PolishWineDark
                )
                Text(
                    text = "total activity",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = PolishWineDark.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Multi-segment category bar in hero
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.45f)
                        .height(12.dp)
                        .background(PolishPrimary)
                )
                Box(
                    modifier = Modifier
                        .weight(0.30f)
                        .height(12.dp)
                        .background(PolishMediumRose)
                )
                Box(
                    modifier = Modifier
                        .weight(0.25f)
                        .height(12.dp)
                        .background(PolishLightRose)
                )
            }

            // Category Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SOCIAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = PolishWineDark.copy(alpha = 0.65f)
                )
                Text(
                    text = "ENTERTAINMENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = PolishWineDark.copy(alpha = 0.65f)
                )
                Text(
                    text = "UTILITY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = PolishWineDark.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
fun DynamicSummaryMetricsGrid(
    params: ComponentParameters,
    state: DashboardUiState
) {
    val hours = state.totalScreenTimeMinutes / 60
    val mins = state.totalScreenTimeMinutes % 60
    val timeFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

    val activeItems = mutableListOf<@Composable (Modifier) -> Unit>()

    if (params.showScreenTime) {
        activeItems.add { mod ->
            SummaryMetricCard(
                title = "Screen Time",
                value = timeFormatted,
                subtitle = "Total active usage",
                icon = Icons.Default.Schedule,
                iconTint = PolishPrimary,
                modifier = mod
            )
        }
    }

    if (params.showOpens) {
        activeItems.add { mod ->
            SummaryMetricCard(
                title = "App Opens",
                value = "${state.totalOpens}",
                subtitle = "Total launches",
                icon = Icons.Default.Smartphone,
                iconTint = PolishPrimary,
                modifier = mod
            )
        }
    }

    if (params.showCompulsiveRatio) {
        activeItems.add { mod ->
            SummaryMetricCard(
                title = "Compulsive Ratio",
                value = "${state.compulsiveScore}%",
                subtitle = "Reflex micro-checks (<30s)",
                icon = Icons.Default.TouchApp,
                iconTint = PolishPrimary,
                modifier = mod
            )
        }
    }

    if (params.showNotifications) {
        activeItems.add { mod ->
            SummaryMetricCard(
                title = "Notifications",
                value = "${state.totalNotifications}",
                subtitle = if (params.showSteps && state.currentSteps > 0) "${state.currentSteps} steps logged" else "Alert frequency",
                icon = if (params.showSteps && state.currentSteps > 0) Icons.Default.DirectionsWalk else Icons.Default.Notifications,
                iconTint = PolishPrimary,
                modifier = mod
            )
        }
    }

    if (activeItems.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in activeItems.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                activeItems[i](Modifier.weight(1f))
                if (i + 1 < activeItems.size) {
                    activeItems[i + 1](Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DynamicRecommendationBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("dynamic_recommendation_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PolishRecoContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "RECOMMENDATION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    color = PolishOnReco
                )
                Text(
                    text = "Switch to 'Forest' or 'Matter' for deep focus?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = PolishOnReco
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open Recommendation",
                tint = PolishOnReco,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun DynamicAiInsightBanner(
    title: String,
    keyTakeaway: String,
    compulsiveSummary: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("dynamic_ai_insight_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PolishAiCallout),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PolishPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PolishPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = PolishPrimary
                )
                Text(
                    text = keyTakeaway,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (compulsiveSummary.isNotBlank()) {
                    Text(
                        text = compulsiveSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = PolishTextSecondary
                    )
                }
            }
        }
    }
}
