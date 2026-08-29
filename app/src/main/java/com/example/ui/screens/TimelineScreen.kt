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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import com.example.data.model.UsageEventEntity
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.PolishOutline
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishSurfaceVariant
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.theme.PolishWineDark
import com.example.ui.theme.SunsetAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineScreen(
    events: List<UsageEventEntity>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }

    val timeFormat = remember { SimpleDateFormat("h:mm:ss a", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    val filteredEvents = events.filter { event ->
        val matchesQuery = searchQuery.isBlank() ||
                event.appName.contains(searchQuery, ignoreCase = true) ||
                event.packageName.contains(searchQuery, ignoreCase = true)
        val matchesType = when (selectedTypeFilter) {
            "OPENS" -> event.eventType == "OPEN"
            "SESSIONS" -> event.eventType == "SESSION"
            "NOTIFICATIONS" -> event.eventType == "NOTIFICATION"
            "COMPULSIVE" -> event.isCompulsiveTrigger
            else -> true
        }
        matchesQuery && matchesType
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("timeline_screen"),
        contentPadding = PaddingValues(bottom = 96.dp, top = 12.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "TELEMETRY FEED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontSize = 11.sp
                    ),
                    color = PolishTextMuted
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Usage Events & Timestamps",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Granular time series of open timestamps, session lengths, and notifications",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timeline_search_field"),
                placeholder = { Text("Search by app name or package...", style = MaterialTheme.typography.bodyMedium, color = PolishTextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = PolishWineDark
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = PolishPrimary,
                    unfocusedIndicatorColor = PolishOutline
                )
            )
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("ALL", "OPENS", "SESSIONS", "NOTIFICATIONS", "COMPULSIVE")
                filters.forEach { filter ->
                    ElevatedFilterChip(
                        selected = selectedTypeFilter == filter,
                        onClick = { selectedTypeFilter = filter },
                        label = { Text(filter) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("timeline_filter_$filter"),
                        colors = FilterChipDefaults.elevatedFilterChipColors(
                            selectedContainerColor = PolishPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = PolishTextSecondary
                        )
                    )
                }
            }
        }

        // Events List
        if (filteredEvents.isEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = PolishTextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No Usage Events Found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Events are continuously logged when you open apps or receive notifications.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishTextMuted
                        )
                    }
                }
            }
        } else {
            items(filteredEvents) { event ->
                TimelineEventCard(event = event, timeFormat = timeFormat, dayFormat = dayFormat)
            }
        }
    }
}

@Composable
private fun TimelineEventCard(
    event: UsageEventEntity,
    timeFormat: SimpleDateFormat,
    dayFormat: SimpleDateFormat
) {
    val date = Date(event.timestamp)
    val timeStr = timeFormat.format(date)
    val dayStr = dayFormat.format(date)

    val (icon, color, typeLabel) = when (event.eventType) {
        "NOTIFICATION" -> Triple(Icons.Default.Notifications, SunsetAmber, "Notification Received")
        "SESSION" -> Triple(Icons.Default.Schedule, PolishPrimary, "Active Session")
        else -> Triple(Icons.Default.PlayArrow, MintEmerald, "App Launched")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("event_item_${event.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PolishOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
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
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.appName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = PolishTextMuted
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$typeLabel • $dayStr",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (event.durationMs > 0) {
                        val durationSeconds = event.durationMs / 1000
                        val mins = durationSeconds / 60
                        val secs = durationSeconds % 60
                        val durStr = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                        Text(
                            text = durStr,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PolishPrimary
                        )
                    }
                }

                if (event.isCompulsiveTrigger) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PolishPrimaryContainer
                    ) {
                        Text(
                            text = "Reflex micro-open (<30s)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = PolishWineDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
