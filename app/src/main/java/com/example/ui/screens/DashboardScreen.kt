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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProactiveNudge
import com.example.data.model.UserProfileEntity
import com.example.data.model.UserRole
import com.example.ui.components.AccountCloudSyncBottomSheet
import com.example.ui.components.DashboardCustomizerSheet
import com.example.ui.components.UserProfileEditDialog
import com.example.ui.components.UserProfileScheduleBanner
import com.example.ui.components.renderDynamicDashboardComponents
import com.example.ui.theme.PolishAiCallout
import com.example.ui.theme.PolishOutline
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishSurfaceVariant
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.theme.PolishWineDark
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
    onSaveProfile: (UserProfileEntity) -> Unit = {},
    onApplyRolePreset: (UserRole) -> Unit = {},
    onUpdateGoalTarget: (String, Int) -> Unit = { _, _ -> },
    onToggleGoal: (String, Boolean) -> Unit = { _, _ -> },
    onNudgeAction: (ProactiveNudge) -> Unit = {},
    onAskAboutHour: ((Int) -> Unit)? = null,
    onSignInWithGoogle: () -> Unit = {},
    onSignInWithEmail: (String, String) -> Unit = { _, _ -> },
    onRegisterWithEmail: (String, String, String) -> Unit = { _, _, _ -> },
    onSignInLocally: (String, String) -> Unit = { _, _ -> },
    onSignOut: () -> Unit = {},
    onBackupToCloud: () -> Unit = {},
    onRestoreFromCloud: () -> Unit = {},
    onOpenCustomizer: () -> Unit = {},
    onCloseCustomizer: () -> Unit = {},
    onSendCustomizationPrompt: (String) -> Unit = {},
    onApplyPresetLayout: (String) -> Unit = {},
    onResetLayoutToDefault: () -> Unit = {},
    onToggleComponentVisibility: (String, Boolean) -> Unit = { _, _ -> },
    onMoveComponent: (String, Boolean) -> Unit = { _, _ -> },
    onSelectHalfLife: (Float) -> Unit = {},
    onDismissStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var showCloudSyncSheet by remember { mutableStateOf(false) }

    if (showProfileDialog) {
        UserProfileEditDialog(
            currentProfile = state.userProfile,
            onDismiss = { showProfileDialog = false },
            onSaveProfile = { updated ->
                onSaveProfile(updated)
            },
            onApplyRolePreset = { role ->
                onApplyRolePreset(role)
            }
        )
    }

    if (showCloudSyncSheet) {
        AccountCloudSyncBottomSheet(
            authUser = state.authUser,
            authState = state.authState,
            cloudSyncStatus = state.cloudSyncStatus,
            lastSyncTimestamp = state.lastCloudSyncTimestamp,
            lastSyncSummary = state.lastSyncSummary,
            cloudSyncError = state.cloudSyncError,
            onSignInWithGoogle = onSignInWithGoogle,
            onSignInWithEmail = onSignInWithEmail,
            onRegisterWithEmail = onRegisterWithEmail,
            onSignInLocally = onSignInLocally,
            onSignOut = onSignOut,
            onBackupToCloud = onBackupToCloud,
            onRestoreFromCloud = onRestoreFromCloud,
            onDismiss = { showCloudSyncSheet = false }
        )
    }

    // Natural Language SDUI Customization Dialog/Sheet
    DashboardCustomizerSheet(
        isOpen = state.isCustomizerSheetOpen,
        currentLayout = state.activeLayoutConfig,
        isCustomizing = state.isCustomizingLayout,
        customizationExplanation = state.customizationExplanation,
        customizationError = state.customizationError,
        isNativeUpdateError = state.isNativeUpdateError,
        onDismiss = onCloseCustomizer,
        onSendPrompt = onSendCustomizationPrompt,
        onApplyPreset = onApplyPresetLayout,
        onResetToDefault = onResetLayoutToDefault,
        onToggleComponentVisibility = onToggleComponentVisibility,
        onMoveComponent = onMoveComponent
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 96.dp, top = 12.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header with title and quick actions
        item(key = "dashboard_header") {
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
                    // Customize SDUI Layout Button
                    IconButton(
                        onClick = onOpenCustomizer,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PolishPrimaryContainer)
                            .testTag("header_sdui_customizer_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Customize Dashboard Layout",
                            tint = PolishWineDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Firebase Cloud Sync & Account Button
                    IconButton(
                        onClick = { showCloudSyncSheet = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.authUser != null) PolishPrimaryContainer else PolishSurfaceVariant
                            )
                            .testTag("header_cloud_sync_button")
                    ) {
                        if (state.cloudSyncStatus == com.example.data.firebase.CloudSyncStatus.SYNCING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PolishWineDark
                            )
                        } else {
                            Icon(
                                imageVector = if (state.authUser != null) Icons.Default.CloudDone else Icons.Outlined.Cloud,
                                contentDescription = "Firebase Cloud & Account",
                                tint = if (state.authUser != null) PolishWineDark else PolishTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Profile Dialog Button
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PolishPrimaryContainer)
                            .testTag("header_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile & Schedule",
                            tint = PolishWineDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Refresh Live Telemetry
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

                    // Ask AI Chatbot
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

        // Active Custom Layout Badge (if customized)
        if (state.activeLayoutConfig.layoutName != "Holistic Overview") {
            item(key = "active_custom_layout_banner") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PolishPrimaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, PolishOutline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DashboardCustomize,
                                contentDescription = null,
                                tint = PolishPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Active Layout: ${state.activeLayoutConfig.layoutName}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PolishWineDark
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Customize",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PolishPrimary,
                                modifier = Modifier
                                    .clickable { onOpenCustomizer() }
                                    .padding(4.dp)
                            )
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PolishTextMuted,
                                modifier = Modifier
                                    .clickable { onResetLayoutToDefault() }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        // User Profile & Schedule Context Banner
        item(key = "user_profile_schedule_banner") {
            UserProfileScheduleBanner(
                profile = state.userProfile,
                onEditProfileClick = { showProfileDialog = true }
            )
        }

        // Status Message Banner (if active)
        if (state.statusMessage != null) {
            item(key = "status_message_banner") {
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

        // Permission Notice Card (if not fully enabled)
        if (!state.hasUsageAccess || !state.hasNotificationAccess) {
            item(key = "permission_notice_card") {
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

        // 2. Server-Driven UI Dynamic Components
        renderDynamicDashboardComponents(
            layoutConfig = state.activeLayoutConfig,
            state = state,
            onFilterSelected = onFilterSelected,
            onRunAiAnalysis = onRunAiAnalysis,
            onNavigateToChat = onNavigateToChat,
            onAskAboutHour = { hour -> onAskAboutHour?.invoke(hour) },
            onNudgeAction = onNudgeAction,
            onUpdateGoalTarget = onUpdateGoalTarget,
            onToggleGoal = onToggleGoal,
            onPopulateDemoData = onPopulateDemoData,
            onOpenCustomizer = onOpenCustomizer,
            onSelectHalfLife = onSelectHalfLife
        )
    }
}
