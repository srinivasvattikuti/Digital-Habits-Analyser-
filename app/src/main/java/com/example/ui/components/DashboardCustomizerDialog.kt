package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sdui.CardType
import com.example.data.sdui.DashboardLayoutConfig
import com.example.data.sdui.DynamicComponentConfig
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.PolishLightRose
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
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

val SUGGESTED_PROMPTS = listOf(
    "Show screen time at the top, hide notifications, and simplify the layout",
    "Focus mode: prioritize habit limits and hide distracting secondary charts",
    "Sleep & nighttime guard: put bedtime forecast and activity heatmap at top",
    "Distraction audit: show interruption leaderboard and compulsive reflex gauge first",
    "Deep analytics: enable all charts, week-over-week deltas, and radar equilibrium"
)

/**
 * Natural Language Server-Driven UI Customization Sheet.
 * Allows users to type prompts, choose presets, toggle components, and view raw JSON payloads.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardCustomizerSheet(
    isOpen: Boolean,
    currentLayout: DashboardLayoutConfig,
    isCustomizing: Boolean,
    customizationExplanation: String?,
    customizationError: String?,
    isNativeUpdateError: Boolean,
    onDismiss: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onApplyPreset: (String) -> Unit,
    onResetToDefault: () -> Unit,
    onToggleComponentVisibility: (String, Boolean) -> Unit,
    onMoveComponent: (String, Boolean) -> Unit // id, moveUp
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var promptInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: AI Prompt, 1: Presets & Components, 2: JSON Payload

    val moshi = remember {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }
    val layoutJson = remember(currentLayout) {
        try {
            moshi.adapter(DashboardLayoutConfig::class.java).indent("  ").toJson(currentLayout)
        } catch (e: Exception) {
            "{}"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp),
                shape = CircleShape,
                color = PolishOutline
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(PolishPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DashboardCustomize,
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Dashboard Customizer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Server-Driven UI Engine • Gemini LLM",
                            style = MaterialTheme.typography.labelSmall,
                            color = PolishTextMuted
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_customizer_btn")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = PolishPrimary,
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("AI Prompt", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Components", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("JSON Schema", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                )
            }

            // Security Error Banner (Mandatory explicit message if native capabilities requested)
            if (customizationError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("customizer_error_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isNativeUpdateError) RoseRed.copy(alpha = 0.12f) else SunsetAmber.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, if (isNativeUpdateError) RoseRed else SunsetAmber)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isNativeUpdateError) Icons.Default.ErrorOutline else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isNativeUpdateError) RoseRed else SunsetAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (isNativeUpdateError) "Native Capability Rejection" else "Customization Notice",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isNativeUpdateError) RoseRed else SunsetAmber
                            )
                            Text(
                                text = customizationError,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Active Explanation Callout
            if (customizationExplanation != null && customizationError == null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PolishPrimaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = customizationExplanation,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = PolishWineDark
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // AI Prompt Tab
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = "Describe how you'd like your dashboard structured:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Input Box
                        item {
                            OutlinedTextField(
                                value = promptInput,
                                onValueChange = { promptInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sdui_prompt_input_field"),
                                placeholder = {
                                    Text(
                                        "e.g. Show screen time at top, hide notification counts, and simplify layout...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PolishTextMuted
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PolishPrimary,
                                    unfocusedBorderColor = PolishOutline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                maxLines = 4,
                                minLines = 3,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (promptInput.isNotBlank() && !isCustomizing) {
                                                onSendPrompt(promptInput)
                                            }
                                        },
                                        enabled = promptInput.isNotBlank() && !isCustomizing,
                                        modifier = Modifier.testTag("sdui_submit_prompt_btn")
                                    ) {
                                        if (isCustomizing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = PolishPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Send Prompt",
                                                tint = if (promptInput.isNotBlank()) PolishPrimary else PolishTextMuted
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        // Suggested Prompt Pills
                        item {
                            Text(
                                text = "Or tap a suggested configuration:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = PolishTextMuted
                            )
                        }

                        item {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SUGGESTED_PROMPTS.forEach { prompt ->
                                    Surface(
                                        modifier = Modifier
                                            .clickable {
                                                promptInput = prompt
                                                onSendPrompt(prompt)
                                            }
                                            .testTag("suggested_prompt_chip"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        border = BorderStroke(1.dp, PolishOutline.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = PolishPrimary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = prompt,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Presets Quick Actions
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Quick Presets:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = PolishTextMuted
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PresetButton(
                                    name = "Minimal Focus",
                                    icon = Icons.Default.Tune,
                                    onClick = { onApplyPreset("MINIMAL_FOCUS") },
                                    modifier = Modifier.weight(1f)
                                )
                                PresetButton(
                                    name = "Sleep Guard",
                                    icon = Icons.Default.AutoAwesome,
                                    onClick = { onApplyPreset("SLEEP_BEDTIME") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PresetButton(
                                    name = "Detox Mode",
                                    icon = Icons.Default.Warning,
                                    onClick = { onApplyPreset("DISTRACTION_REDUCER") },
                                    modifier = Modifier.weight(1f)
                                )
                                PresetButton(
                                    name = "Full Analytics",
                                    icon = Icons.Default.DashboardCustomize,
                                    onClick = { onApplyPreset("DATA_INTENSIVE") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Components & Reordering Tab
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "Active Components (${currentLayout.components.count { it.visible }} visible):",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val sortedComponents = currentLayout.components.sortedBy { it.position }
                        itemsIndexed(sortedComponents, key = { _, comp -> comp.id }) { index, comp ->
                            ComponentInspectorRow(
                                component = comp,
                                isFirst = index == 0,
                                isLast = index == sortedComponents.size - 1,
                                onToggle = { visible -> onToggleComponentVisibility(comp.id, visible) },
                                onMoveUp = { onMoveComponent(comp.id, true) },
                                onMoveDown = { onMoveComponent(comp.id, false) }
                            )
                        }
                    }
                }

                2 -> {
                    // JSON Schema Tab
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live Server-Driven UI JSON:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = PolishTextMuted
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MintEmerald.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "VALID SCHEMA",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp
                                    ),
                                    color = MintEmerald,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, PolishOutline)
                        ) {
                            LazyColumn(modifier = Modifier.padding(12.dp)) {
                                item {
                                    Text(
                                        text = layoutJson,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onResetToDefault,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PolishOutline),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reset_layout_to_default_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Reset",
                        modifier = Modifier.size(16.dp),
                        tint = PolishTextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Reset Default",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = PolishTextSecondary
                    )
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("apply_and_close_customizer_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Apply",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Done",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetButton(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, PolishOutline),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = modifier
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = PolishPrimary)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ComponentInspectorRow(
    component: DynamicComponentConfig,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (component.visible) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, PolishOutline.copy(alpha = if (component.visible) 0.6f else 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (component.visible) PolishPrimaryContainer else PolishSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${component.position}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (component.visible) PolishWineDark else PolishTextMuted
                        )
                    }
                }

                Column {
                    Text(
                        text = component.title ?: component.type.displayName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (component.visible) MaterialTheme.colorScheme.onSurface else PolishTextMuted
                        )
                    )
                    Text(
                        text = component.type.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = PolishTextMuted
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Move Up",
                        tint = if (!isFirst) PolishTextSecondary else PolishTextMuted.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Move Down",
                        tint = if (!isLast) PolishTextSecondary else PolishTextMuted.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Switch(
                    checked = component.visible,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PolishPrimary,
                        uncheckedTrackColor = PolishSurfaceVariant
                    ),
                    modifier = Modifier.size(width = 38.dp, height = 24.dp)
                )
            }
        }
    }
}
