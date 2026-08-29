package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppUsageRowItem
import com.example.ui.components.CategoryDistributionCard
import com.example.ui.components.CompulsiveVsIntentionalGaugeCard
import com.example.ui.components.HourlyUsageHeatmapCard
import com.example.ui.components.SummaryMetricCard
import com.example.ui.components.UsageOverTimeCard
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

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onFilterSelected: (DateFilter) -> Unit,
    onRefreshTelemetry: () -> Unit,
    onPopulateDemoData: () -> Unit,
    onRunAiAnalysis: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onNavigateToChat: () -> Unit,
    onAskAboutHour: ((Int) -> Unit)? = null,
    onDismissStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 96.dp, top = 12.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Professional Polish Header matching Design HTML
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HABIT INSIGHTS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 11.sp
                        ),
                        color = PolishTextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onRefreshTelemetry,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PolishSurfaceVariant)
                            .testTag("refresh_telemetry_button")
                    ) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PolishWineDark
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Telemetry",
                                tint = PolishWineDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNavigateToChat,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PolishSurfaceVariant)
                            .testTag("header_ai_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Ask AI",
                            tint = PolishWineDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // 2. Status message banner (if active)
        if (state.statusMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PolishAiCallout,
                    border = BorderStroke(1.dp, PolishOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PolishPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = state.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = PolishWineDark
                            )
                        }
                        Text(
                            text = "Dismiss",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PolishPrimary,
                            modifier = Modifier
                                .clickable { onDismissStatus() }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }

        // 3. Permission Notice Card (if not fully enabled)
        if (!state.hasUsageAccess || !state.hasNotificationAccess) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("permission_notice_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, PolishOutline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Permissions",
                                tint = PolishPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Grant Tracking Permissions",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "To track real-time app session durations and notification counts, grant Usage Access or populate demo history below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!state.hasUsageAccess) {
                                Button(
                                    onClick = onOpenUsageSettings,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("grant_usage_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Usage Access", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }
                            if (!state.hasNotificationAccess) {
                                OutlinedButton(
                                    onClick = onOpenNotificationSettings,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("grant_notif_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, PolishPrimary)
                                ) {
                                    Text("Notif Access", style = MaterialTheme.typography.labelSmall, color = PolishPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Hero Section: Daily Usage Card matching Design HTML (#FFDAD6 container)
        item {
            val hours = state.totalScreenTimeMinutes / 60
            val mins = state.totalScreenTimeMinutes % 60
            val timeFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_daily_usage_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PolishPrimaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Usage",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = PolishWineDark
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.45f)
                        ) {
                            Text(
                                text = state.selectedFilter.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = PolishWineDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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

        // 5. Quick Filter Pills & AI Trigger
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter pills
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

                // AI Action / Pipeline Button
                Button(
                    onClick = onRunAiAnalysis,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("run_ai_pipeline_button")
                ) {
                    if (state.isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Analyze", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // 6. Key Metric Cards Grid (2x2)
        item {
            val hours = state.totalScreenTimeMinutes / 60
            val mins = state.totalScreenTimeMinutes % 60
            val timeFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryMetricCard(
                        title = "Screen Time",
                        value = timeFormatted,
                        subtitle = "Total active usage",
                        icon = Icons.Default.Schedule,
                        iconTint = PolishPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricCard(
                        title = "App Opens",
                        value = "${state.totalOpens}",
                        subtitle = "Total launches",
                        icon = Icons.Default.Smartphone,
                        iconTint = PolishPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryMetricCard(
                        title = "Compulsive Ratio",
                        value = "${state.compulsiveScore}%",
                        subtitle = "Reflex micro-checks (<30s)",
                        icon = Icons.Default.TouchApp,
                        iconTint = PolishPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricCard(
                        title = "Notifications",
                        value = "${state.totalNotifications}",
                        subtitle = if (state.currentSteps > 0) "${state.currentSteps} steps logged" else "Alert frequency",
                        icon = if (state.currentSteps > 0) Icons.Default.DirectionsWalk else Icons.Default.Notifications,
                        iconTint = PolishPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 7. Interactive Usage Over Time Chart
        item {
            UsageOverTimeCard(dailyTrends = state.dailyTrendStats)
        }

        // 8. Activity Density (24h) Heatmap & AI Insight Card matching Design HTML
        item {
            HourlyUsageHeatmapCard(
                hourlyStats = state.hourlyStats,
                aiSummary = state.latestInsight?.keyTakeaway,
                onAskAboutHour = onAskAboutHour
            )
        }

        // 8. Recommendation Action Banner matching Design HTML (#D1E1FF container)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToChat() }
                    .testTag("recommendation_action_card"),
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

        // 9. Compulsive vs Intentional Gauge
        item {
            CompulsiveVsIntentionalGaugeCard(
                compulsiveScore = state.compulsiveScore,
                totalOpens = state.totalOpens,
                compulsiveSummary = state.latestInsight?.compulsiveSummary ?: ""
            )
        }

        // 10. Category Breakdown
        item {
            CategoryDistributionCard(categoryStats = state.categoryStats)
        }

        // 11. Top Dominating Apps Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App Usage & Focus Dominance",
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

        // Top Apps Items
        items(state.topApps.take(8)) { app ->
            AppUsageRowItem(app = app)
        }

        // 12. Demo Data Generation Button
        item {
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
}
