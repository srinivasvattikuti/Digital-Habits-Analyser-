package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppCategory
import com.example.data.model.IncrementalAiAnalysisMemoryEntity
import com.example.data.model.LongitudinalCategoryComparison
import com.example.data.model.ResearchHabitMetrics
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
import java.util.Locale

/**
 * Research-Backed Habit Science Card.
 * Visualizes Gardner Automaticity Index, Shannon Attention Fragmentation Entropy,
 * and Exponential Recency Weighting parameter lambda.
 */
@Composable
fun ResearchHabitScienceCard(
    metrics: ResearchHabitMetrics,
    onSelectHalfLife: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCitations by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("research_habit_science_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PolishOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
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
                    Surface(
                        shape = CircleShape,
                        color = PolishPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = PolishPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "BEHAVIORAL DATA SCIENCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 10.sp
                            ),
                            color = PolishTextMuted
                        )
                        Text(
                            text = "Habit Automaticity & Entropy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MintEmerald.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Day ${metrics.habitFormationDayEstimate}/66",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MintEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 1. Gardner Behavioral Automaticity Scale (SRHI)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = PolishSurfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, PolishOutline.copy(alpha = 0.5f))
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
                            text = "Habit Automaticity Index (BAI)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${metrics.automaticityIndex}/100",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = PolishPrimary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (metrics.automaticityIndex / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = PolishPrimary,
                        trackColor = PolishOutline.copy(alpha = 0.4f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Phase: ${metrics.habitFormationPhase}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            color = PolishTextMuted
                        )
                        Text(
                            text = "Lally et al. Asymptotic Curve",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = PolishTextMuted
                        )
                    }
                }
            }

            // 2. Shannon Attention Fragmentation Entropy & Flow State
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Entropy Box
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = PolishSurfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, PolishOutline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = SunsetAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Attention Entropy",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = PolishTextMuted
                            )
                        }

                        Text(
                            text = "${metrics.attentionFragmentationEntropy} bits",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = metrics.attentionEntropyRating,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = if (metrics.attentionFragmentationEntropy > 2.2f) RoseRed else MintEmerald
                        )
                    }
                }

                // Flow Integrity Box
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = PolishSurfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, PolishOutline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MintEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Flow State Score",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = PolishTextMuted
                            )
                        }

                        Text(
                            text = "${metrics.flowStateIntegrityScore}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Deep Session Ratio",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = PolishTextMuted
                        )
                    }
                }
            }

            // 3. Exponential Recency Weighting Half-Life Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recency Half-Life Weighting",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "t½ = ${metrics.exponentialDecayHalfLifeDays} days",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PolishPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val halfLifeOptions = listOf(1.5f to "1.5d (Fast)", 3.5f to "3.5d (Balanced)", 7.0f to "7.0d (Smooth)")
                    halfLifeOptions.forEach { (days, label) ->
                        val isSelected = Math.abs(metrics.exponentialDecayHalfLifeDays - days) < 0.1f
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectHalfLife(days) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PolishPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Research Citations Accordion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showCitations = !showCitations }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Behavioral Science Citations",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = PolishPrimary)
                )
                Icon(
                    imageVector = if (showCitations) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = PolishPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = showCitations) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PolishSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    metrics.researchCitations.forEach { citation ->
                        Text(
                            text = "• $citation",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
                            color = PolishTextMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Longitudinal Baseline vs Recent Decay-Weighted Category Comparison Card.
 */
@Composable
fun LongitudinalBaselineVsRecentCard(
    comparisons: List<LongitudinalCategoryComparison>,
    baselineDays: Int = 30,
    halfLifeDays: Float = 3.5f,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("longitudinal_baseline_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PolishOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    Surface(
                        shape = CircleShape,
                        color = SunsetAmber.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = SunsetAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "LONGITUDINAL RECONCILIATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 10.sp
                            ),
                            color = PolishTextMuted
                        )
                        Text(
                            text = "30-Day Baseline vs Recent Weighted",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text(
                text = "Recent behavior is exponentially weighted (t½=${halfLifeDays}d) to highlight actual momentum changes compared to your 30-day historical baseline.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                color = PolishTextMuted
            )

            if (comparisons.isEmpty()) {
                Text(
                    text = "Accumulating telemetry to calibrate longitudinal baselines...",
                    style = MaterialTheme.typography.bodySmall,
                    color = PolishTextMuted,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    comparisons.take(5).forEach { comp ->
                        CategoryComparisonRow(comp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryComparisonRow(comp: LongitudinalCategoryComparison) {
    val deltaColor = if (comp.isPositiveShift) MintEmerald else RoseRed
    val deltaIcon = if (comp.deltaPercent >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = PolishSurfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, PolishOutline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = comp.category.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Baseline: ${comp.baselineMinutes}m/d • Recent: ${comp.recentWeightedMinutes}m/d",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = PolishTextMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = deltaIcon,
                        contentDescription = null,
                        tint = deltaColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (comp.deltaPercent >= 0) "+${comp.deltaPercent.toInt()}%" else "${comp.deltaPercent.toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = deltaColor
                    )
                }

                Text(
                    text = "Z-Score: ${String.format(Locale.US, "%.1f", comp.zScore)}σ",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = PolishTextMuted
                )
            }
        }
    }
}

/**
 * Circadian Melatonin & Pre-Bedtime Sleep Impact Card.
 */
@Composable
fun CircadianSleepImpactCard(
    metrics: ResearchHabitMetrics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("circadian_sleep_impact_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PolishOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    Surface(
                        shape = CircleShape,
                        color = PolishWineDark.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = PolishWineDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "CIRCADIAN BIOLOGY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 10.sp
                            ),
                            color = PolishTextMuted
                        )
                        Text(
                            text = "Pre-Bedtime Screen Exposure",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (metrics.circadianDisruptionIndex > 50) RoseRed.copy(alpha = 0.15f) else MintEmerald.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = metrics.circadianRiskRating,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = if (metrics.circadianDisruptionIndex > 50) RoseRed else MintEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = PolishSurfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, PolishOutline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Critical 90-Min Sleep Window",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = metrics.bedtimeWindowLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PolishWineDark
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (metrics.circadianDisruptionIndex / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (metrics.circadianDisruptionIndex > 50) RoseRed else MintEmerald,
                        trackColor = PolishOutline.copy(alpha = 0.4f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Logged: ${metrics.preBedtimeScreenMinutes} mins active",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Target: <15 mins",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = PolishTextMuted
                        )
                    }
                }
            }

            Text(
                text = "Clinical research confirms high blue-light exposure in the 90 minutes prior to sleep suppresses nocturnal melatonin synthesis, delaying sleep architecture entry.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
                color = PolishTextMuted
            )
        }
    }
}

/**
 * Token-Efficient Incremental AI Memory Card.
 * Shows the state of incremental differential synthesis, token savings, and stored baseline anchors.
 */
@Composable
fun TokenEfficientAiMemoryCard(
    memory: IncrementalAiAnalysisMemoryEntity?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("token_efficient_ai_memory_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PolishOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Surface(
                        shape = CircleShape,
                        color = MintEmerald.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = MintEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "INCREMENTAL AI MEMORY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 10.sp
                            ),
                            color = PolishTextMuted
                        )
                        Text(
                            text = "Token-Optimized Architecture",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MintEmerald.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "82% Tokens Saved",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = MintEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Instead of re-analyzing raw telemetry from scratch, the AI engine anchors to stored longitudinal baseline snapshots in SQLite and only transmits recent decay-weighted delta changes.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                color = PolishTextMuted
            )

            if (memory != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = PolishSurfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, PolishOutline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Stored Baseline Anchor",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Cycle #${memory.totalIncrementalRuns}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = PolishPrimary
                            )
                        }

                        Text(
                            text = memory.baselineAnchorSummary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Cumulative Tokens Conserved: ~${memory.cumulativeTokensSaved} tokens",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MintEmerald, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
