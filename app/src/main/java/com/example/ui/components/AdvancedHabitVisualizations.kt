package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppCategory
import com.example.data.model.BehaviorForecast
import com.example.data.model.GoalProgressItem
import com.example.data.model.HabitDimensionScore
import com.example.data.model.HabitGoalEntity
import com.example.data.model.ProactiveNudge
import com.example.data.model.WeekOverWeekCategoryStat
import com.example.data.model.WeekOverWeekSummary
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ============================================================================
// 1. WEEK-OVER-WEEK USAGE COMPARISON SECTION
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekOverWeekComparisonCard(
    summary: WeekOverWeekSummary?,
    modifier: Modifier = Modifier
) {
    if (summary == null) return

    var selectedCategoryFilter by remember { mutableStateOf<String?>("ALL") }
    var showDetails by remember { mutableStateOf(false) }

    val isScreenTimeReduced = summary.totalPercentChange <= 0
    val totalPctDisplay = abs(summary.totalPercentChange).roundToInt()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("week_over_week_comparison_card"),
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PolishPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isScreenTimeReduced) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = PolishWineDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "WEEK-OVER-WEEK DYNAMICS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 11.sp
                            ),
                            color = PolishTextMuted
                        )
                        Text(
                            text = "7-Day Usage vs Previous Week",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Delta Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isScreenTimeReduced) MintEmerald.copy(alpha = 0.12f) else RoseRed.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (isScreenTimeReduced) MintEmerald.copy(alpha = 0.4f) else RoseRed.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isScreenTimeReduced) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = if (isScreenTimeReduced) MintEmerald else RoseRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${if (summary.totalPercentChange > 0) "+" else ""}${summary.totalPercentChange.roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            ),
                            color = if (isScreenTimeReduced) MintEmerald else RoseRed
                        )
                    }
                }
            }

            // Headline Synthesis Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PolishPrimaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, PolishOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PolishWineDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = summary.headlineInsight,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        ),
                        color = PolishWineDark
                    )
                }
            }

            // Key Metrics Comparison Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Daily Avg Screen Time Box
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = PolishSurfaceVariant,
                    border = BorderStroke(1.dp, PolishOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Daily Screen Avg",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = PolishTextMuted
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${summary.currentWeekDailyAvgMinutes / 60}h ${summary.currentWeekDailyAvgMinutes % 60}m",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Prev: ${summary.previousWeekDailyAvgMinutes / 60}h ${summary.previousWeekDailyAvgMinutes % 60}m/d",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = PolishTextSecondary
                        )
                    }
                }

                // Compulsive Reflex Opens Box
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = PolishSurfaceVariant,
                    border = BorderStroke(1.dp, PolishOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Compulsive Opens",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = PolishTextMuted
                        )
                        Text(
                            text = "${summary.currentWeekCompulsiveOpens}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val compDiff = summary.compulsivePercentChange.roundToInt()
                        Text(
                            text = "${if (compDiff > 0) "+" else ""}$compDiff% vs prev (${summary.previousWeekCompulsiveOpens})",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (summary.compulsivePercentChange <= 0) MintEmerald else RoseRed
                        )
                    }
                }

                // Steps / Movement Box
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = PolishSurfaceVariant,
                    border = BorderStroke(1.dp, PolishOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Avg Daily Steps",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = PolishTextMuted
                        )
                        Text(
                            text = "%,d".format(summary.currentWeekAvgSteps),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val stepDiff = summary.stepsPercentChange.roundToInt()
                        Text(
                            text = "${if (stepDiff > 0) "+" else ""}$stepDiff% vs prev",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (summary.stepsPercentChange >= 0) MintEmerald else PolishTextMuted
                        )
                    }
                }
            }

            // Category Level Breakdown Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Percentage Changes",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Current vs Previous Week",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = PolishTextMuted
                )
            }

            // Category Comparison Items
            val categoriesToShow = summary.categoryChanges.filter { it.currentWeekMinutes > 0 || it.previousWeekMinutes > 0 }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                categoriesToShow.forEach { catStat ->
                    CategoryComparisonRow(stat = catStat)
                }
            }
        }
    }
}

@Composable
fun CategoryComparisonRow(
    stat: WeekOverWeekCategoryStat,
    modifier: Modifier = Modifier
) {
    val pct = stat.percentChange.roundToInt()
    val isPositiveTrend = stat.isPositiveTrend
    val isImprovement = isPositiveTrend && abs(pct) > 0

    val maxVal = maxOf(stat.currentWeekMinutes, stat.previousWeekMinutes, 1).toFloat()
    val currRatio = (stat.currentWeekMinutes / maxVal).coerceIn(0.05f, 1f)
    val prevRatio = (stat.previousWeekMinutes / maxVal).coerceIn(0.05f, 1f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PolishSurfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, PolishOutline.copy(alpha = 0.6f))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(stat.category.colorHex)))
                    )
                    Text(
                        text = stat.category.displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Percentage Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPositiveTrend) MintEmerald.copy(alpha = 0.12f) else RoseRed.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${if (pct > 0) "+" else ""}$pct%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        ),
                        color = if (isPositiveTrend) MintEmerald else RoseRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Dual Progress Bars Comparison
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Current Week Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Now",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        ),
                        color = PolishTextMuted,
                        modifier = Modifier.width(32.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PolishOutline.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(currRatio)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(android.graphics.Color.parseColor(stat.category.colorHex)))
                        )
                    }
                    Text(
                        text = "${stat.currentWeekDailyAvgMinutes}m/d",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.End
                    )
                }

                // Previous Week Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Prev",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        color = PolishTextMuted,
                        modifier = Modifier.width(32.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(PolishOutline.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(prevRatio)
                                .clip(RoundedCornerShape(3.dp))
                                .background(PolishTextMuted.copy(alpha = 0.6f))
                        )
                    }
                    Text(
                        text = "${stat.previousWeekDailyAvgMinutes}m/d",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        color = PolishTextMuted,
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

// ============================================================================
// 2. INTERACTIVE HABIT GOALS SECTION & TARGET ADJUSTER
// ============================================================================

@Composable
fun InteractiveHabitGoalsCard(
    goalProgressList: List<GoalProgressItem>,
    onUpdateGoalTarget: (String, Int) -> Unit,
    onToggleGoal: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingGoal by remember { mutableStateOf<HabitGoalEntity?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_habit_goals_card"),
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PolishPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = PolishWineDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "HABIT TARGETS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 11.sp
                            ),
                            color = PolishTextMuted
                        )
                        Text(
                            text = "Daily Habit Goals",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val achievedCount = goalProgressList.count { it.isAchieved && it.goal.isEnabled }
                val totalEnabled = goalProgressList.count { it.goal.isEnabled }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MintEmerald.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MintEmerald.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "$achievedCount / $totalEnabled Met",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = MintEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // List of Goals
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                goalProgressList.forEach { item ->
                    GoalProgressRow(
                        item = item,
                        onEditClick = { editingGoal = item.goal },
                        onToggle = { isEnabled -> onToggleGoal(item.goal.id, isEnabled) }
                    )
                }
            }
        }
    }

    // Goal Target Editor Dialog
    if (editingGoal != null) {
        val goal = editingGoal!!
        var tempTarget by remember(goal) { mutableStateOf(goal.targetValue.toFloat()) }

        val (minVal, maxVal, step) = when (goal.category) {
            "SOCIAL" -> Triple(15f, 180f, 15f)
            "PRODUCTIVITY" -> Triple(30f, 300f, 15f)
            "BEDTIME" -> Triple(20f, 24f, 1f)
            "STEPS" -> Triple(2000f, 20000f, 500f)
            "COMPULSIVE_OPENS" -> Triple(10f, 150f, 5f)
            else -> Triple(10f, 200f, 10f)
        }

        AlertDialog(
            onDismissRequest = { editingGoal = null },
            title = {
                Text(
                    text = "Adjust Goal: ${goal.title}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = goal.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = PolishTextSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PolishPrimaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (goal.category == "BEDTIME") "${tempTarget.toInt()}:00" else "${tempTarget.toInt()} ${goal.unit}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp
                                ),
                                color = PolishWineDark
                            )
                            Text(
                                text = if (goal.goalType == "MAX_LIMIT") "Maximum Daily Limit" else "Minimum Daily Target",
                                style = MaterialTheme.typography.labelSmall,
                                color = PolishTextMuted
                            )
                        }
                    }

                    Slider(
                        value = tempTarget,
                        onValueChange = { tempTarget = it },
                        valueRange = minVal..maxVal,
                        steps = ((maxVal - minVal) / step).toInt() - 1,
                        colors = SliderDefaults.colors(
                            thumbColor = PolishPrimary,
                            activeTrackColor = PolishPrimary,
                            inactiveTrackColor = PolishOutline
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateGoalTarget(goal.id, tempTarget.toInt())
                        editingGoal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingGoal = null }) {
                    Text("Cancel", color = PolishTextMuted)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun GoalProgressRow(
    item: GoalProgressItem,
    onEditClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val goal = item.goal
    val progressAnimated by animateFloatAsState(
        targetValue = item.progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "goal_progress"
    )

    val icon = when (goal.category) {
        "SOCIAL" -> Icons.Default.TouchApp
        "PRODUCTIVITY" -> Icons.Default.Psychology
        "BEDTIME" -> Icons.Default.NightsStay
        "STEPS" -> Icons.Default.DirectionsWalk
        "COMPULSIVE_OPENS" -> Icons.Default.NotificationsActive
        else -> Icons.Default.AutoAwesome
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (item.isAchieved) MintEmerald.copy(alpha = 0.06f) else PolishSurfaceVariant,
        border = BorderStroke(
            1.dp,
            if (item.isAchieved) MintEmerald.copy(alpha = 0.35f) else PolishOutline
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(goal.colorHex)).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(android.graphics.Color.parseColor(goal.colorHex)),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.statusText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = if (item.isAchieved) MintEmerald else PolishTextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Goal",
                            tint = PolishTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { progressAnimated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(android.graphics.Color.parseColor(goal.colorHex)),
                trackColor = PolishOutline.copy(alpha = 0.4f)
            )
        }
    }
}

// ============================================================================
// 3. BEHAVIORAL FORECASTING ENGINE CARD
// ============================================================================

@Composable
fun BehaviorForecastCard(
    forecast: BehaviorForecast?,
    onOpenCopilot: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (forecast == null) return

    val isHighPacing = forecast.pacingStatus == "PACING_HIGH"
    val isOptimal = forecast.pacingStatus == "OPTIMAL"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("behavior_forecast_card"),
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PolishPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = PolishWineDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "PREDICTIVE INTELLIGENCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 11.sp
                            ),
                            color = PolishTextMuted
                        )
                        Text(
                            text = "Behavioral Forecast",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PolishWineDark.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "${forecast.confidenceScore}% Confidence",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = PolishWineDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Projected End of Day Screen Time Block
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = PolishPrimaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, PolishOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Projected End-of-Day Screen Time",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = PolishTextMuted
                        )
                        Text(
                            text = "${forecast.projectedTodayMinutes / 60}h ${forecast.projectedTodayMinutes % 60}m",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp
                            ),
                            color = PolishWineDark
                        )
                        Text(
                            text = if (isHighPacing) "Pacing +${forecast.pacingPacePercent}% above standard average" else if (isOptimal) "Pacing -${abs(forecast.pacingPacePercent)}% below average (Optimal)" else "Pacing exactly on track with weekly baseline",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (isHighPacing) RoseRed else if (isOptimal) MintEmerald else PolishTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isHighPacing) RoseRed.copy(alpha = 0.15f) else MintEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isHighPacing) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isHighPacing) RoseRed else MintEmerald,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Bedtime Doomscroll Risk Box
            val riskColor = when (forecast.bedtimeDoomscrollRisk) {
                "HIGH" -> RoseRed
                "MODERATE" -> SunsetAmber
                else -> MintEmerald
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = riskColor.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, riskColor.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = riskColor,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Bedtime Doomscroll Risk:",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = forecast.bedtimeDoomscrollRisk,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                ),
                                color = riskColor
                            )
                        }
                        Text(
                            text = forecast.bedtimeRiskReason,
                            style = MaterialTheme.typography.bodySmall.copy(
                                lineHeight = 16.sp,
                                fontSize = 12.sp
                            ),
                            color = PolishTextSecondary
                        )
                    }
                }
            }

            // Recommended Micro-Habit
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PolishSurfaceVariant,
                border = BorderStroke(1.dp, PolishOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "RECOMMENDED ACTION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            ),
                            color = PolishTextMuted
                        )
                        Text(
                            text = forecast.recommendedMicroHabit,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onOpenCopilot,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PolishPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Ask AI",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. HABIT DIMENSION RADAR / HOLISTIC BALANCE VISUALIZATION
// ============================================================================

@Composable
fun HabitDimensionsRadarCard(
    dimensions: List<HabitDimensionScore>,
    modifier: Modifier = Modifier
) {
    if (dimensions.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_dimensions_radar_card"),
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PolishPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = PolishWineDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "HOLISTIC EQUILIBRIUM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 11.sp
                            ),
                            color = PolishTextMuted
                        )
                        Text(
                            text = "5-Pillar Habit Balance",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val avgScore = dimensions.map { it.score }.average().roundToInt()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PolishPrimaryContainer
                ) {
                    Text(
                        text = "Score: $avgScore / 100",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = PolishWineDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Radar Polygon Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                RadarPolygonChart(dimensions = dimensions)
            }

            // Dimension Pills List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dimensions.forEach { dim ->
                    DimensionItemRow(dim = dim)
                }
            }
        }
    }
}

@Composable
fun RadarPolygonChart(
    dimensions: List<HabitDimensionScore>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(160.dp)) {
        val count = dimensions.size.coerceAtLeast(3)
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (size.minDimension / 2f) * 0.85f

        // Draw concentric guide rings
        for (step in 1..4) {
            val stepRadius = radius * (step / 4f)
            val ringPath = Path()
            for (i in 0 until count) {
                val angle = (2 * PI / count * i) - (PI / 2)
                val x = center.x + (stepRadius * cos(angle)).toFloat()
                val y = center.y + (stepRadius * sin(angle)).toFloat()
                if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
            }
            ringPath.close()
            drawPath(
                path = ringPath,
                color = Color.LightGray.copy(alpha = 0.35f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Draw radial spoke lines
        for (i in 0 until count) {
            val angle = (2 * PI / count * i) - (PI / 2)
            val endX = center.x + (radius * cos(angle)).toFloat()
            val endY = center.y + (radius * sin(angle)).toFloat()
            drawLine(
                color = Color.LightGray.copy(alpha = 0.4f),
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw dynamic score polygon
        val scorePath = Path()
        dimensions.forEachIndexed { i, dim ->
            val angle = (2 * PI / count * i) - (PI / 2)
            val scoreRadius = radius * (dim.score.coerceIn(10, 100) / 100f)
            val x = center.x + (scoreRadius * cos(angle)).toFloat()
            val y = center.y + (scoreRadius * sin(angle)).toFloat()
            if (i == 0) scorePath.moveTo(x, y) else scorePath.lineTo(x, y)
        }
        scorePath.close()

        // Fill polygon
        drawPath(
            path = scorePath,
            color = Color(0xFFB42340).copy(alpha = 0.25f),
            style = Fill
        )

        // Stroke polygon border
        drawPath(
            path = scorePath,
            color = Color(0xFFB42340),
            style = Stroke(width = 2.5.dp.toPx())
        )

        // Draw node dots
        dimensions.forEachIndexed { i, dim ->
            val angle = (2 * PI / count * i) - (PI / 2)
            val scoreRadius = radius * (dim.score.coerceIn(10, 100) / 100f)
            val x = center.x + (scoreRadius * cos(angle)).toFloat()
            val y = center.y + (scoreRadius * sin(angle)).toFloat()
            drawCircle(
                color = Color(0xFFB42340),
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun DimensionItemRow(
    dim: HabitDimensionScore,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PolishSurfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, PolishOutline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = dim.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• ${dim.ratingLabel}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = Color(android.graphics.Color.parseColor(dim.statusColorHex))
                    )
                }
                Text(
                    text = dim.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = PolishTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(android.graphics.Color.parseColor(dim.statusColorHex)).copy(alpha = 0.12f)
            ) {
                Text(
                    text = "${dim.score}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    ),
                    color = Color(android.graphics.Color.parseColor(dim.statusColorHex)),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

// ============================================================================
// 5. PROACTIVE NUDGES & ACTIONABLE SUGGESTIONS SECTION
// ============================================================================

@Composable
fun ProactiveNudgesSection(
    nudges: List<ProactiveNudge>,
    onNudgeAction: (ProactiveNudge) -> Unit,
    modifier: Modifier = Modifier
) {
    if (nudges.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("proactive_nudges_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REAL-TIME HABIT NUDGES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    fontSize = 11.sp
                ),
                color = PolishTextMuted
            )
            Text(
                text = "${nudges.size} Active",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = PolishWineDark
            )
        }

        nudges.forEach { nudge ->
            ProactiveNudgeCard(nudge = nudge, onAction = { onNudgeAction(nudge) })
        }
    }
}

@Composable
fun ProactiveNudgeCard(
    nudge: ProactiveNudge,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (borderColor, containerColor, iconColor) = when (nudge.severity) {
        "ALERT" -> Triple(RoseRed, RoseRed.copy(alpha = 0.08f), RoseRed)
        "WARNING" -> Triple(SunsetAmber, SunsetAmber.copy(alpha = 0.08f), SunsetAmber)
        "SUCCESS" -> Triple(MintEmerald, MintEmerald.copy(alpha = 0.08f), MintEmerald)
        else -> Triple(PolishOutline, PolishPrimaryContainer.copy(alpha = 0.35f), PolishWineDark)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (nudge.type) {
                        "GOAL_WARNING" -> Icons.Default.Warning
                        "MISSED_HABIT" -> Icons.Default.Alarm
                        "MOMENTUM_STREAK" -> Icons.Default.CheckCircle
                        else -> Icons.Default.AutoAwesome
                    },
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = nudge.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = nudge.message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 16.sp,
                        fontSize = 11.sp
                    ),
                    color = PolishTextSecondary
                )
            }

            if (nudge.actionText != null) {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = nudge.actionText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
