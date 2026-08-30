package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotificationFrequencyStat
import com.example.data.model.DayTrendData
import com.example.data.model.WeeklyChartTrendsState
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.PolishAiCallout
import com.example.ui.theme.PolishLightRose
import com.example.ui.theme.PolishMediumRose
import com.example.ui.theme.PolishOnPrimaryContainer
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
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlin.math.abs
import kotlin.math.roundToInt

// ============================================================================
// 1. PRIMARY HERO WEEKLY TRENDS & NOTIFICATIONS VISUALIZATION
// ============================================================================

@Composable
fun WeeklyTrendsOverviewCard(
    weeklyTrends: WeeklyChartTrendsState,
    dailyScreenBudgetMinutes: Int = 210,
    onOpenInsightDeepDive: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (weeklyTrends.dayTrends.isEmpty()) return

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedDayIndex by remember { mutableIntStateOf(weeklyTrends.dayTrends.size - 1) }

    val activeDay = weeklyTrends.dayTrends.getOrNull(selectedDayIndex) ?: weeklyTrends.dayTrends.last()

    // Chart producers for Vico
    val screenTimeModelProducer = remember { CartesianChartModelProducer.build() }
    val notificationModelProducer = remember { CartesianChartModelProducer.build() }
    val correlationModelProducer = remember { CartesianChartModelProducer.build() }

    LaunchedEffect(weeklyTrends.dayTrends) {
        if (weeklyTrends.dayTrends.isNotEmpty()) {
            val screenTimes = weeklyTrends.dayTrends.map { it.screenTimeMinutes }
            val notifications = weeklyTrends.dayTrends.map { it.notificationCount }
            val opens = weeklyTrends.dayTrends.map { it.openCount }

            screenTimeModelProducer.runTransaction {
                lineSeries {
                    series(screenTimes)
                }
            }

            notificationModelProducer.runTransaction {
                columnSeries {
                    series(notifications)
                }
            }

            correlationModelProducer.runTransaction {
                lineSeries {
                    series(screenTimes)
                    series(notifications.map { it * 2 }) // scaled for visual correlation
                }
            }
        }
    }

    val dayLabels = weeklyTrends.dayTrends.map { it.dayName }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_trends_overview_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, PolishOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PolishPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Weekly Trends",
                            tint = PolishWineDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "WEEKLY TELEMETRY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 10.sp
                            ),
                            color = PolishWineDark
                        )
                        Text(
                            text = "Screen Time & Alerts",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 7-day badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PolishSurfaceVariant,
                    border = BorderStroke(1.dp, PolishOutline)
                ) {
                    Text(
                        text = "Past 7 Days",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = PolishWineDark
                    )
                }
            }

            // Summary Metric Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val totalHours = weeklyTrends.totalWeeklyScreenTimeMinutes / 60
                val totalRemMin = weeklyTrends.totalWeeklyScreenTimeMinutes % 60
                val avgHours = weeklyTrends.avgDailyScreenTimeMinutes / 60
                val avgRemMin = weeklyTrends.avgDailyScreenTimeMinutes % 60

                SummaryMetricPill(
                    icon = Icons.Default.Smartphone,
                    label = "Total Screen Time",
                    value = "${totalHours}h ${totalRemMin}m",
                    subtext = "Avg ${avgHours}h ${avgRemMin}m/day",
                    deltaPct = weeklyTrends.weeklyScreenTimeTrendDeltaPct,
                    invertDeltaColor = true
                )

                SummaryMetricPill(
                    icon = Icons.Default.NotificationsActive,
                    label = "Total Notifications",
                    value = "${weeklyTrends.totalWeeklyNotifications}",
                    subtext = "Avg ${weeklyTrends.avgDailyNotifications}/day",
                    deltaPct = weeklyTrends.weeklyNotificationTrendDeltaPct,
                    invertDeltaColor = true
                )

                SummaryMetricPill(
                    icon = Icons.Default.TouchApp,
                    label = "Alert Conversion",
                    value = "${weeklyTrends.notificationToOpenConversionRate}%",
                    subtext = "Triggered App Unlock",
                    deltaPct = null
                )

                SummaryMetricPill(
                    icon = Icons.Default.Speed,
                    label = "Peak Disruption",
                    value = weeklyTrends.peakNotificationDay,
                    subtext = "${weeklyTrends.peakNotificationCount} alerts",
                    deltaPct = null
                )
            }

            // Chart Tab Switcher
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PolishSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabs = listOf(
                        "Screen Time" to Icons.Default.ShowChart,
                        "Categories" to Icons.Default.BarChart,
                        "Notifications" to Icons.Default.Notifications,
                        "Correlation" to Icons.Default.CompareArrows
                    )

                    tabs.forEachIndexed { index, (title, icon) ->
                        val isSelected = selectedTab == index
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) PolishPrimary else Color.Transparent,
                            label = "tabBg"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else PolishTextSecondary,
                            label = "tabText"
                        )

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = textColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Interactive Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PolishSurfaceVariant.copy(alpha = 0.25f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Vico Screen Time Line Chart
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberLineCartesianLayer(),
                                startAxis = rememberStartAxis(
                                    valueFormatter = CartesianValueFormatter { value, _, _ ->
                                        val totalM = value.toInt()
                                        if (totalM >= 60) "${totalM / 60}h" else "${totalM}m"
                                    }
                                ),
                                bottomAxis = rememberBottomAxis(
                                    valueFormatter = CartesianValueFormatter { value, _, _ ->
                                        val idx = value.toInt().coerceIn(0, (dayLabels.size - 1).coerceAtLeast(0))
                                        dayLabels.getOrElse(idx) { "" }
                                    }
                                )
                            ),
                            modelProducer = screenTimeModelProducer,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    1 -> {
                        // Category Daily Breakdown Visualizer
                        WeeklyCategoryStackedBarView(
                            dayTrends = weeklyTrends.dayTrends,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    2 -> {
                        // Vico Notification Frequency Column Chart
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberColumnCartesianLayer(),
                                startAxis = rememberStartAxis(
                                    valueFormatter = CartesianValueFormatter { value, _, _ ->
                                        "${value.toInt()}"
                                    }
                                ),
                                bottomAxis = rememberBottomAxis(
                                    valueFormatter = CartesianValueFormatter { value, _, _ ->
                                        val idx = value.toInt().coerceIn(0, (dayLabels.size - 1).coerceAtLeast(0))
                                        dayLabels.getOrElse(idx) { "" }
                                    }
                                )
                            ),
                            modelProducer = notificationModelProducer,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    3 -> {
                        // Screen Time vs Notifications Dual-Trend Chart
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberLineCartesianLayer(),
                                startAxis = rememberStartAxis(
                                    valueFormatter = CartesianValueFormatter { value, _, _ ->
                                        "${value.toInt()}"
                                    }
                                ),
                                bottomAxis = rememberBottomAxis(
                                    valueFormatter = CartesianValueFormatter { value, _, _ ->
                                        val idx = value.toInt().coerceIn(0, (dayLabels.size - 1).coerceAtLeast(0))
                                        dayLabels.getOrElse(idx) { "" }
                                    }
                                )
                            ),
                            modelProducer = correlationModelProducer,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Interactive Day Tap Selector Ribbon
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT DAY TO INSPECT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp,
                            fontSize = 10.sp
                        ),
                        color = PolishTextMuted
                    )
                    Text(
                        text = "Tap a day for telemetry breakdown",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = PolishTextSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    weeklyTrends.dayTrends.forEachIndexed { index, day ->
                        val isSelected = selectedDayIndex == index
                        DayChipItem(
                            day = day,
                            isSelected = isSelected,
                            budgetMinutes = dailyScreenBudgetMinutes,
                            onClick = { selectedDayIndex = index }
                        )
                    }
                }
            }

            // Detailed Day Inspection Card
            AnimatedContent(
                targetState = activeDay,
                label = "day_inspection"
            ) { day ->
                DayDetailInspectionCard(day = day, budgetMinutes = dailyScreenBudgetMinutes)
            }

            // Contextual Behavioral Insight Callout
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PolishAiCallout,
                border = BorderStroke(1.dp, PolishLightRose)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PolishPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "AI Habit Insight",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "AI WEEKLY SYNTHESIS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                fontSize = 10.sp
                            ),
                            color = PolishWineDark
                        )
                        Text(
                            text = weeklyTrends.screenTimeVersusNotificationInsight,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            color = PolishWineDark
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. DAY CHIP & INSPECTION SUB-COMPONENTS
// ============================================================================

@Composable
private fun DayChipItem(
    day: DayTrendData,
    isSelected: Boolean,
    budgetMinutes: Int,
    onClick: () -> Unit
) {
    val isOverBudget = day.screenTimeMinutes > budgetMinutes
    val borderColor = when {
        isSelected -> PolishPrimary
        isOverBudget -> RoseRed.copy(alpha = 0.6f)
        else -> PolishOutline
    }
    val containerColor = when {
        isSelected -> PolishPrimaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = day.dayName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    color = if (isSelected) PolishWineDark else PolishTextSecondary
                )
                if (day.isToday) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MintEmerald)
                    )
                }
            }

            val hrs = day.screenTimeMinutes / 60
            val mins = day.screenTimeMinutes % 60
            val timeStr = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"

            Text(
                text = timeStr,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = if (isOverBudget) RoseRed else MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = PolishTextMuted,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "${day.notificationCount}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = PolishTextMuted
                )
            }
        }
    }
}

@Composable
private fun DayDetailInspectionCard(
    day: DayTrendData,
    budgetMinutes: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PolishSurfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, PolishOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Text(
                        text = "${day.dayName} (${day.fullDateLabel})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (day.isToday) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MintEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Today",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = MintEmerald
                            )
                        }
                    }
                }

                val hrs = day.screenTimeMinutes / 60
                val mins = day.screenTimeMinutes % 60
                Text(
                    text = "${hrs}h ${mins}m active",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    ),
                    color = PolishPrimary
                )
            }

            // Quick stats in 4 columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DayStatCell(
                    title = "Notifications",
                    value = "${day.notificationCount} alerts",
                    icon = Icons.Default.NotificationsActive,
                    color = SunsetAmber
                )
                DayStatCell(
                    title = "App Opens",
                    value = "${day.openCount} unlocks",
                    icon = Icons.Default.TouchApp,
                    color = PolishWineDark
                )
                DayStatCell(
                    title = "Impulse Picks",
                    value = "${day.compulsiveOpens} reflex",
                    icon = Icons.Default.Alarm,
                    color = RoseRed
                )
            }

            // Top apps on that day
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, PolishOutline),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "TOP APP USAGE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = PolishTextMuted
                            )
                            Text(
                                text = "${day.topApp} (${day.topAppMinutes}m)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, PolishOutline),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = SunsetAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "TOP DISRUPTER",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = PolishTextMuted
                            )
                            Text(
                                text = "${day.topNotifyingApp} (${day.topNotifyingAppCount} alerts)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayStatCell(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                ),
                color = PolishTextMuted
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ============================================================================
// 3. CATEGORY STACKED BAR VIEW (7-DAY BREAKDOWN)
// ============================================================================

@Composable
fun WeeklyCategoryStackedBarView(
    dayTrends: List<DayTrendData>,
    modifier: Modifier = Modifier
) {
    val maxDayMinutes = dayTrends.maxOfOrNull { it.screenTimeMinutes }?.coerceAtLeast(180) ?: 180

    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Bar Chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            dayTrends.forEach { day ->
                val totalM = day.screenTimeMinutes.coerceAtLeast(1)
                val heightFraction = (totalM.toFloat() / maxDayMinutes.toFloat()).coerceIn(0.08f, 1.0f)

                val socialFrac = (day.socialMinutes.toFloat() / totalM.toFloat()).coerceIn(0f, 1f)
                val prodFrac = (day.productivityMinutes.toFloat() / totalM.toFloat()).coerceIn(0f, 1f)
                val entFrac = (day.entertainmentMinutes.toFloat() / totalM.toFloat()).coerceIn(0f, 1f)
                val otherFrac = (1f - socialFrac - prodFrac - entFrac).coerceAtLeast(0f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .fillMaxHeight(heightFraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Entertainment
                            if (entFrac > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(entFrac.coerceAtLeast(0.01f))
                                        .background(PolishMediumRose)
                                )
                            }
                            // Social
                            if (socialFrac > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(socialFrac.coerceAtLeast(0.01f))
                                        .background(PolishPrimary)
                                )
                            }
                            // Productivity
                            if (prodFrac > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(prodFrac.coerceAtLeast(0.01f))
                                        .background(MintEmerald)
                                )
                            }
                            // Other
                            if (otherFrac > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(otherFrac.coerceAtLeast(0.01f))
                                        .background(SunsetAmber)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = day.dayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        color = if (day.isToday) PolishPrimary else PolishTextSecondary
                    )
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendPill(color = PolishPrimary, label = "Social")
            Spacer(modifier = Modifier.width(12.dp))
            LegendPill(color = MintEmerald, label = "Productivity")
            Spacer(modifier = Modifier.width(12.dp))
            LegendPill(color = PolishMediumRose, label = "Entertainment")
            Spacer(modifier = Modifier.width(12.dp))
            LegendPill(color = SunsetAmber, label = "Other")
        }
    }
}

@Composable
private fun LegendPill(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = PolishTextSecondary
        )
    }
}

// ============================================================================
// 4. NOTIFICATION FREQUENCY LEADERBOARD CARD
// ============================================================================

@Composable
fun NotificationFrequencyLeaderboardCard(
    apps: List<AppNotificationFrequencyStat>,
    totalWeeklyNotifications: Int,
    peakDisruptionHour: String = "7:00 PM - 9:00 PM",
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("notification_frequency_leaderboard_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, PolishOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PolishRecoContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Notification Disruptions",
                            tint = PolishWineDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "INTERRUPTION LEADERBOARD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 10.sp
                            ),
                            color = PolishWineDark
                        )
                        Text(
                            text = "Top Alert Disrupters",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PolishSurfaceVariant,
                    border = BorderStroke(1.dp, PolishOutline)
                ) {
                    Text(
                        text = "$totalWeeklyNotifications Alerts",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = PolishWineDark
                    )
                }
            }

            // Peak disruption window chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SunsetAmber.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, SunsetAmber.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = SunsetAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Peak Alert Window: $peakDisruptionHour (48% of impulse unlocks)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // App items
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                apps.forEachIndexed { index, appStat ->
                    AppNotificationLeaderboardRow(
                        rank = index + 1,
                        appStat = appStat
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNotificationLeaderboardRow(
    rank: Int,
    appStat: AppNotificationFrequencyStat
) {
    val progressFraction = (appStat.percentOfTotal / 100f).coerceIn(0.05f, 1f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PolishSurfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, PolishOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (rank <= 3) PolishPrimary else PolishSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$rank",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = if (rank <= 3) Color.White else PolishWineDark
                            )
                        }
                    }

                    Column {
                        Text(
                            text = appStat.appName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = appStat.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = PolishTextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${appStat.totalNotifications} alerts",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        ),
                        color = PolishPrimary
                    )
                    Text(
                        text = "${appStat.percentOfTotal}% of total",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = PolishTextSecondary
                    )
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PolishPrimary,
                trackColor = PolishOutline
            )

            // Conversion rate tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Unlock Conversion: ${appStat.openConversionRate.toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    color = if (appStat.openConversionRate > 50f) RoseRed else MintEmerald
                )

                // Mini daily spark indicators
                if (appStat.dailyCounts.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val maxCount = appStat.dailyCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
                        appStat.dailyCounts.takeLast(7).forEach { count ->
                            val hFrac = (count.toFloat() / maxCount.toFloat()).coerceIn(0.15f, 1f)
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height((12 * hFrac).dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(PolishPrimary.copy(alpha = 0.7f))
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 5. HELPER METRIC PILL
// ============================================================================

@Composable
private fun SummaryMetricPill(
    icon: ImageVector,
    label: String,
    value: String,
    subtext: String,
    deltaPct: Float? = null,
    invertDeltaColor: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PolishSurfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, PolishOutline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = PolishPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    ),
                    color = PolishTextMuted
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (deltaPct != null && deltaPct != 0f) {
                    val isPositive = deltaPct > 0
                    val isGood = if (invertDeltaColor) !isPositive else isPositive
                    val tint = if (isGood) MintEmerald else RoseRed
                    val arrowIcon = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown

                    Icon(
                        imageVector = arrowIcon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${if (isPositive) "+" else ""}${deltaPct.roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = tint
                    )
                }

                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = PolishTextSecondary
                )
            }
        }
    }
}
