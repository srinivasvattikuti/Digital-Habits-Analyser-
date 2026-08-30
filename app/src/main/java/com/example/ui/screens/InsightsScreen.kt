package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BehaviorForecast
import com.example.data.model.HabitDimensionScore
import com.example.data.model.HabitInsightEntity
import com.example.data.model.WeekOverWeekSummary
import com.example.data.model.WeeklyChartTrendsState
import com.example.ui.components.BehaviorForecastCard
import com.example.ui.components.HabitDimensionsRadarCard
import com.example.ui.components.WeekOverWeekComparisonCard
import com.example.ui.components.WeeklyTrendsOverviewCard
import com.example.ui.components.NotificationFrequencyLeaderboardCard
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.PolishAiCallout
import com.example.ui.theme.PolishOutline
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishSurfaceVariant
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishWineDark
import com.example.ui.theme.RoseRed
import com.example.ui.theme.SunsetAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InsightsScreen(
    insight: HabitInsightEntity?,
    isAnalyzing: Boolean,
    onRunAnalysis: () -> Unit,
    weekOverWeekSummary: WeekOverWeekSummary? = null,
    behaviorForecast: BehaviorForecast? = null,
    habitDimensions: List<HabitDimensionScore> = emptyList(),
    weeklyTrends: WeeklyChartTrendsState = WeeklyChartTrendsState(),
    dailyScreenBudgetMinutes: Int = 210,
    onOpenCopilot: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("insights_screen"),
        contentPadding = PaddingValues(bottom = 96.dp, top = 12.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HABIT SYNTHESIS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 11.sp
                        ),
                        color = PolishTextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "AI Behavioral Insights",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Button(
                    onClick = onRunAnalysis,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("re_analyze_button")
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Sync Pipeline", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        if (insight == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, PolishOutline)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(28.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Generating Habit Model...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap 'Sync Pipeline' to evaluate time series telemetry and generate behavioral synthesis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishTextMuted
                        )
                    }
                }
            }
        } else {
            // 1. Executive Key Takeaway Card (#FFDAD6 hero feel)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("key_takeaway_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PolishPrimaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = PolishWineDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Primary Behavioral Takeaway",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PolishWineDark
                                    )
                                }
                            }
                            Text(
                                text = dateFormat.format(Date(insight.timestamp)),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = PolishWineDark.copy(alpha = 0.7f)
                            )
                        }

                        Text(
                            text = insight.keyTakeaway,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 22.sp
                            ),
                            color = PolishWineDark
                        )
                    }
                }
            }

            // 2. Weekly Vico Trends & Notification Interruption Engine
            if (weeklyTrends.dayTrends.isNotEmpty()) {
                item {
                    WeeklyTrendsOverviewCard(
                        weeklyTrends = weeklyTrends,
                        dailyScreenBudgetMinutes = dailyScreenBudgetMinutes,
                        onOpenInsightDeepDive = onOpenCopilot
                    )
                }

                if (weeklyTrends.topNotifyingApps.isNotEmpty()) {
                    item {
                        NotificationFrequencyLeaderboardCard(
                            apps = weeklyTrends.topNotifyingApps,
                            totalWeeklyNotifications = weeklyTrends.totalWeeklyNotifications
                        )
                    }
                }
            }

            // 2b. Week-Over-Week Dynamics (Calculated Averages & Shift)
            if (weekOverWeekSummary != null) {
                item {
                    WeekOverWeekComparisonCard(summary = weekOverWeekSummary)
                }
            }

            // 3. 5-Pillar Holistic Equilibrium Radar Chart
            if (habitDimensions.isNotEmpty()) {
                item {
                    HabitDimensionsRadarCard(dimensions = habitDimensions)
                }
            }

            // 4. Behavioral Forecasting & Bedtime Doomscroll Predictor
            if (behaviorForecast != null) {
                item {
                    BehaviorForecastCard(
                        forecast = behaviorForecast,
                        onOpenCopilot = onOpenCopilot
                    )
                }
            }

            // 5. Behavioral Patterns Grid (Peak active hours, Compulsive score, Productivity trend)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Peak Time
                    InsightFeatureCard(
                        icon = Icons.Default.NightsStay,
                        iconTint = PolishPrimary,
                        title = "Time-of-Day Activity Peak",
                        value = insight.peakActiveHours,
                        description = "Identifies peak screen time windows (e.g. post-work evening wind-down or midnight doomscroll loops)."
                    )

                    // Compulsive vs Intentional
                    InsightFeatureCard(
                        icon = Icons.Default.TouchApp,
                        iconTint = PolishPrimary,
                        title = "Compulsive vs Intentional Index (${insight.compulsiveScore}%)",
                        value = insight.compulsiveSummary,
                        description = "Measures reflexive unlocks (<30 seconds) triggered by push notifications vs deliberate deep focus sessions."
                    )

                    // Week-over-Week trend
                    InsightFeatureCard(
                        icon = Icons.Default.TrendingUp,
                        iconTint = MintEmerald,
                        title = "Week-Over-Week Productivity Shift",
                        value = insight.productivityTrend,
                        description = "Evaluates historical trendlines across social, work, and mindful categories."
                    )
                }
            }

            // 3. Full Comprehensive AI Synthesis (Markdown style card)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("full_synthesis_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, PolishOutline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PolishPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Comprehensive Model Synthesis",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = insight.fullAnalysisText,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightFeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PolishOutline),
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PolishSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PolishWineDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = PolishTextMuted
                )
            }
        }
    }
}
