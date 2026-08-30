package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AuthAndPermissionsOnboardingScreen
import com.example.ui.screens.AskAiScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.RecommendationsScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.theme.DigitalHabitsTheme
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.VibrantIndigo
import com.example.viewmodel.HabitTrackerViewModel

enum class HabitDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    TIMELINE("timeline", "Timeline", Icons.Default.Schedule, "nav_timeline"),
    INSIGHTS("insights", "Insights", Icons.Default.AutoAwesome, "nav_insights"),
    RECOMMENDATIONS("recommendations", "Nudges", Icons.Default.ThumbUp, "nav_recommendations"),
    ASK_AI("ask_ai", "Ask AI", Icons.Default.Psychology, "nav_ask_ai")
}

class MainActivity : ComponentActivity() {

    private val viewModel: HabitTrackerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DigitalHabitsTheme {
                HabitTrackerApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissionsAndLoad()
    }
}

@Composable
fun HabitTrackerApp(viewModel: HabitTrackerViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(HabitDestination.DASHBOARD) }
    var hasDismissedOnboarding by rememberSaveable { mutableStateOf(false) }

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val recentEvents by viewModel.recentEvents.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val latestInsight by viewModel.latestInsight.collectAsStateWithLifecycle()

    val showOnboarding = !hasDismissedOnboarding && (dashboardState.authUser == null || !dashboardState.hasUsageAccess)

    LaunchedEffect(currentDestination) {
        viewModel.logScreenView(currentDestination.name)
    }

    if (showOnboarding) {
        AuthAndPermissionsOnboardingScreen(
            authUser = dashboardState.authUser,
            authState = dashboardState.authState,
            hasUsageAccess = dashboardState.hasUsageAccess,
            hasNotificationAccess = dashboardState.hasNotificationAccess,
            onSignInWithGoogle = viewModel::signInWithGoogle,
            onSignInWithEmail = viewModel::signInWithEmail,
            onRegisterWithEmail = viewModel::registerWithEmail,
            onContinueLocally = { email, displayName ->
                viewModel.signInLocally(email, displayName)
                hasDismissedOnboarding = true
            },
            onOpenUsageSettings = viewModel::openUsageSettings,
            onOpenNotificationSettings = viewModel::openNotificationListenerSettings,
            onCompleteOnboarding = {
                hasDismissedOnboarding = true
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    HabitDestination.entries.forEach { dest ->
                        val isSelected = currentDestination == dest
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentDestination = dest },
                            icon = {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = dest.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = dest.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                                        fontSize = 10.sp
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = com.example.ui.theme.PolishWineDark,
                                selectedTextColor = com.example.ui.theme.PolishWineDark,
                                indicatorColor = com.example.ui.theme.PolishPrimaryContainer,
                                unselectedIconColor = com.example.ui.theme.PolishTextSecondary,
                                unselectedTextColor = com.example.ui.theme.PolishTextSecondary
                            ),
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                AnimatedContent(
                    targetState = currentDestination,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { destination ->
                    when (destination) {
                        HabitDestination.DASHBOARD -> DashboardScreen(
                            state = dashboardState,
                            onFilterSelected = viewModel::setDateFilter,
                            onRefreshTelemetry = viewModel::refreshTelemetry,
                            onPopulateDemoData = viewModel::populateDemoData,
                            onRunAiAnalysis = viewModel::runAiAnalysis,
                            onOpenUsageSettings = viewModel::openUsageSettings,
                            onOpenNotificationSettings = viewModel::openNotificationListenerSettings,
                            onNavigateToChat = { currentDestination = HabitDestination.ASK_AI },
                            onSaveProfile = viewModel::saveUserProfile,
                            onApplyRolePreset = viewModel::applyRolePreset,
                            onUpdateGoalTarget = viewModel::updateGoalTarget,
                            onToggleGoal = viewModel::toggleGoalEnabled,
                            onSignInWithGoogle = viewModel::signInWithGoogle,
                            onSignInWithEmail = viewModel::signInWithEmail,
                            onRegisterWithEmail = viewModel::registerWithEmail,
                            onSignInLocally = viewModel::signInLocally,
                            onSignOut = viewModel::signOut,
                            onBackupToCloud = viewModel::backupToCloud,
                            onRestoreFromCloud = viewModel::restoreFromCloud,
                            onNudgeAction = { nudge ->
                                viewModel.sendChatMessage("How can I act on this habit recommendation: '${nudge.title} - ${nudge.message}'?")
                                currentDestination = HabitDestination.ASK_AI
                            },
                            onAskAboutHour = { hour ->
                                val timeDesc = when {
                                    hour == 0 -> "12:00 AM (midnight)"
                                    hour < 12 -> "$hour:00 AM"
                                    hour == 12 -> "12:00 PM (noon)"
                                    else -> "${hour - 12}:00 PM"
                                }
                                viewModel.sendChatMessage("Why am I using my phone so much around $timeDesc?")
                                currentDestination = HabitDestination.ASK_AI
                            },
                            onDismissStatus = viewModel::clearStatusMessage
                        )
                        HabitDestination.TIMELINE -> TimelineScreen(
                            events = recentEvents
                        )
                        HabitDestination.INSIGHTS -> InsightsScreen(
                            insight = latestInsight,
                            isAnalyzing = dashboardState.isAnalyzing,
                            onRunAnalysis = viewModel::runAiAnalysis,
                            weekOverWeekSummary = dashboardState.weekOverWeekSummary,
                            behaviorForecast = dashboardState.behaviorForecast,
                            habitDimensions = dashboardState.habitDimensions,
                            weeklyTrends = dashboardState.weeklyChartTrends,
                            dailyScreenBudgetMinutes = dashboardState.userProfile?.dailyScreenTimeTargetMinutes ?: 210,
                            onOpenCopilot = { currentDestination = HabitDestination.ASK_AI }
                        )
                        HabitDestination.RECOMMENDATIONS -> RecommendationsScreen(
                            recommendations = recommendations
                        )
                        HabitDestination.ASK_AI -> AskAiScreen(
                            chatMessages = chatMessages,
                            isChatLoading = dashboardState.isChatLoading,
                            onSendMessage = viewModel::sendChatMessage,
                            onClearChat = viewModel::clearChatHistory
                        )
                    }
                }
            }
        }
    }
}
