package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppRecommendationEntity
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
import com.example.ui.theme.PolishWineDark
import com.example.ui.theme.RoseRed
import com.example.ui.theme.SunsetAmber
import com.example.viewmodel.AppUsageSummary
import com.example.viewmodel.CategoryUsageStat
import com.example.viewmodel.DailyUsageTrendStat
import com.example.viewmodel.HourlyUsageStat
import java.util.Locale

@Composable
fun SummaryMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("metric_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, PolishOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
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
                        contentDescription = title,
                        tint = PolishWineDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
fun UsageOverTimeCard(
    dailyTrends: List<DailyUsageTrendStat>,
    modifier: Modifier = Modifier
) {
    var selectedDayIndex by remember(dailyTrends) {
        mutableStateOf(if (dailyTrends.isNotEmpty()) dailyTrends.size - 1 else 0)
    }

    val maxMinutes = (dailyTrends.maxOfOrNull { it.totalMinutes } ?: 1).coerceAtLeast(60)

    Card(
        modifier = modifier.testTag("usage_over_time_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PolishOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Usage Over Time",
                        tint = PolishPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Usage Over Time",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PolishSurfaceVariant
                ) {
                    Text(
                        text = "Interactive Days",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = PolishWineDark
                    )
                }
            }

            if (dailyTrends.isNotEmpty()) {
                // Interactive Bar Chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PolishSurfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailyTrends.forEachIndexed { index, day ->
                        val isSelected = index == selectedDayIndex
                        val ratio = (day.totalMinutes.toFloat() / maxMinutes).coerceIn(0.1f, 1f)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
                                .clickable { selectedDayIndex = index }
                                .testTag("trend_day_bar_$index"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((70 * ratio).dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(if (isSelected) PolishPrimary else PolishMediumRose.copy(alpha = 0.7f))
                                    .then(
                                        if (isSelected) Modifier.border(1.5.dp, PolishWineDark, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        else Modifier
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = day.displayLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) PolishWineDark else PolishTextMuted
                            )
                        }
                    }
                }

                // Selected Day Summary Card
                val selectedDay = dailyTrends.getOrNull(selectedDayIndex) ?: dailyTrends.last()
                val hours = selectedDay.totalMinutes / 60
                val mins = selectedDay.totalMinutes % 60
                val durStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PolishPrimaryContainer.copy(alpha = 0.4f))
                        .border(1.dp, PolishPrimaryContainer, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "${selectedDay.displayLabel} Summary",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PolishWineDark
                            )
                            Text(
                                text = "$durStr • ${selectedDay.openCount} opens (${selectedDay.compulsiveOpens} compulsive)",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = PolishWineDark.copy(alpha = 0.85f)
                            )
                        }

                        if (selectedDay.steps > 0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.7f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsWalk,
                                        contentDescription = null,
                                        tint = PolishWineDark,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${selectedDay.steps} steps",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = PolishWineDark
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Historical daily aggregates will populate as background usage data is captured.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PolishTextMuted
                )
            }
        }
    }
}

@Composable
fun HourlyUsageHeatmapCard(
    hourlyStats: List<HourlyUsageStat>,
    aiSummary: String? = null,
    onAskAboutHour: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val maxMinutes = (hourlyStats.maxOfOrNull { it.totalMinutes } ?: 1).coerceAtLeast(1)
    var selectedHour by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier.testTag("hourly_heatmap_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PolishOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ACTIVITY DENSITY (24H)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 12.sp
                    ),
                    color = PolishTextMuted
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PolishSurfaceVariant
                ) {
                    Text(
                        text = "Tap hour to inspect",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = PolishWineDark
                    )
                }
            }

            // 24-column interactive heat grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PolishSurfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                for (hour in 0..23) {
                    val stat = hourlyStats.firstOrNull { it.hour == hour }
                    val mins = stat?.totalMinutes ?: 0
                    val isSelected = selectedHour == hour
                    val ratio = (mins.toFloat() / maxMinutes).coerceIn(0.12f, 1f)

                    val barColor = when {
                        isSelected -> PolishWineDark
                        mins == 0 -> PolishSurfaceVariant
                        mins < 10 -> PolishLightRose
                        mins < 25 -> PolishMediumRose
                        else -> PolishPrimary
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clickable {
                                selectedHour = if (selectedHour == hour) null else hour
                            }
                            .testTag("heatmap_hour_$hour"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (mins == 0) 10.dp else (56 * ratio).dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(barColor)
                                .then(
                                    if (isSelected) Modifier.border(1.5.dp, PolishPrimary, RoundedCornerShape(3.dp))
                                    else Modifier
                                )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (hour % 6 == 0) {
                            Text(
                                text = "${hour}h",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) PolishWineDark else PolishTextMuted
                            )
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }

            // Selected Hour Detail Expandable
            AnimatedVisibility(
                visible = selectedHour != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                selectedHour?.let { hour ->
                    val stat = hourlyStats.firstOrNull { it.hour == hour }
                    val mins = stat?.totalMinutes ?: 0
                    val opens = stat?.openCount ?: 0
                    val hourLabel = when {
                        hour == 0 -> "12:00 AM – 1:00 AM (Midnight)"
                        hour < 12 -> "$hour:00 AM – ${hour + 1}:00 AM"
                        hour == 12 -> "12:00 PM – 1:00 PM (Noon)"
                        else -> "${hour - 12}:00 PM – ${hour - 11}:00 PM"
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PolishPrimaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, PolishPrimaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = hourLabel,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PolishWineDark
                                )
                                Text(
                                    text = "$mins min • $opens opens",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PolishPrimary
                                )
                            }

                            if (onAskAboutHour != null) {
                                OutlinedButton(
                                    onClick = { onAskAboutHour(hour) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PolishWineDark),
                                    border = BorderStroke(1.dp, PolishOutline)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = PolishPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Ask AI: Why do I use my phone during this hour?",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Time segment badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeatmapLegendItem(color = PolishSurfaceVariant, label = "Inactive")
                HeatmapLegendItem(color = PolishLightRose, label = "Light (<10m)")
                HeatmapLegendItem(color = PolishMediumRose, label = "Moderate")
                HeatmapLegendItem(color = PolishPrimary, label = "Dense (>25m)")
            }

            // Nested AI Insight Callout matching Design HTML
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PolishAiCallout)
                    .border(1.dp, PolishOutline, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI Insight",
                            tint = PolishPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "AI Behavioral Insight",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = PolishPrimary
                        )
                    }
                    Text(
                        text = aiSummary ?: "Late-night screen loops peak between 10 PM and 12 AM. Compulsive micro-checks are 2.4x higher after 9 PM notifications.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = PolishWineDark
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
            color = PolishTextMuted
        )
    }
}

@Composable
fun CompulsiveVsIntentionalGaugeCard(
    compulsiveScore: Int, // 0-100
    totalOpens: Int,
    compulsiveSummary: String,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateFloatAsState(
        targetValue = compulsiveScore.toFloat(),
        animationSpec = tween(durationMillis = 800),
        label = "compulsive_gauge"
    )

    val (badgeColor, badgeTitle, badgeDescription) = when {
        compulsiveScore >= 60 -> Triple(PolishPrimary, "High Reflex Loop", "Significant rapid checking & infinite scrolling triggers.")
        compulsiveScore >= 35 -> Triple(SunsetAmber, "Moderate Compulsion", "Balanced usage with occasional reflexive unlocks.")
        else -> Triple(MintEmerald, "Mindful & Intentional", "High proportion of deliberate, sustained focus sessions.")
    }

    Card(
        modifier = modifier.testTag("compulsive_gauge_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PolishOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = "Habit Quality",
                        tint = PolishPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Compulsive vs Intentional",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PolishPrimaryContainer
                ) {
                    Text(
                        text = badgeTitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PolishWineDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Arc Gauge
                Box(
                    modifier = Modifier.size(92.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(82.dp)) {
                        val strokeWidth = 9.dp.toPx()
                        val sweepAngle = 260f
                        val startAngle = 140f

                        // Background track
                        drawArc(
                            color = PolishSurfaceVariant,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Progress arc
                        val progressSweep = (animatedScore / 100f) * sweepAngle
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(PolishLightRose, PolishMediumRose, PolishPrimary)
                            ),
                            startAngle = startAngle,
                            sweepAngle = progressSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${compulsiveScore}%",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = PolishWineDark
                        )
                        Text(
                            text = "Compulsive",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = PolishTextMuted
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = badgeDescription,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = compulsiveSummary.ifBlank {
                            "Calculated from app opens lasting under 30s, rapid notification triggers, and nocturnal wakeups."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDistributionCard(
    categoryStats: List<CategoryUsageStat>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("category_distribution_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PolishOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Categories",
                        tint = PolishPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Category Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Multi-segment horizontal bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(PolishSurfaceVariant)
            ) {
                categoryStats.forEach { stat ->
                    if (stat.percentage > 0) {
                        Box(
                            modifier = Modifier
                                .weight(stat.percentage.toFloat())
                                .height(14.dp)
                                .background(getCategoryComposeColor(stat.category.name))
                        )
                    }
                }
            }

            // Grid of categories
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categoryStats.take(5).forEach { stat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(getCategoryComposeColor(stat.category.name))
                            )
                            Text(
                                text = stat.category.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${stat.totalMinutes} min (${stat.percentage}%)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageRowItem(
    app: AppUsageSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_row_${app.appName.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PolishOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App Icon Placeholder / Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PolishSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(1).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = PolishWineDark
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${app.totalMinutes} min",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = PolishPrimary
                    )
                }

                LinearProgressIndicator(
                    progress = { (app.percentage / 100f).coerceIn(0.05f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PolishPrimary,
                    trackColor = PolishSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${app.openCount} opens • ${app.notificationCount} notifs",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = PolishTextMuted
                    )
                    if (app.compulsiveOpens > 0) {
                        Text(
                            text = "${app.compulsiveOpens} reflex checks",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = PolishPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationItemCard(
    recommendation: AppRecommendationEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rec_card_${recommendation.suggestedAppName.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PolishRecoContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PolishOnReco,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = recommendation.efficiencyBadge,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PolishOnReco
                        )
                    }
                }
                Text(
                    text = "Alternative for ${recommendation.targetAppName}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = PolishOnReco.copy(alpha = 0.75f)
                )
            }

            Text(
                text = "Switch to: ${recommendation.suggestedAppName}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PolishOnReco
            )

            Text(
                text = recommendation.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = PolishOnReco.copy(alpha = 0.85f)
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Key Upgrades:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PolishOnReco
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = recommendation.keyBenefits,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = PolishOnReco.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

fun getCategoryComposeColor(cat: String): Color {
    return when (cat) {
        "SOCIAL" -> PolishPrimary
        "ENTERTAINMENT" -> PolishMediumRose
        "PRODUCTIVITY" -> Color(0xFF4A6572)
        "SHOPPING" -> SunsetAmber
        "FINANCE" -> Color(0xFF0284C7)
        "COMMUNICATION" -> Color(0xFF7C3AED)
        "UTILITIES" -> Color(0xFF64748B)
        "HEALTH" -> MintEmerald
        "GAMES" -> Color(0xFFE11D48)
        else -> PolishWineDark
    }
}
