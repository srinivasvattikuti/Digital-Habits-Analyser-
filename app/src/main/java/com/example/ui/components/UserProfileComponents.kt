package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfileEntity
import com.example.data.model.UserRole
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
import com.example.ui.theme.SunsetAmber
import java.util.Calendar

// ============================================================================
// 1. DASHBOARD PROFILE & SCHEDULE SUMMARY BANNER
// ============================================================================

@Composable
fun UserProfileScheduleBanner(
    profile: UserProfileEntity?,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProfile = profile ?: UserProfileEntity()
    val isDayOff = activeProfile.isTodayDayOff()
    val currentRole = activeProfile.getRole()
    val isKid = activeProfile.isKidMode || activeProfile.age < 13

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("user_profile_schedule_banner"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PolishOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Avatar, Name/Role, and Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(PolishPrimary, PolishMediumRose)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isKid -> Icons.Default.School
                                currentRole == UserRole.WORKING_PROFESSIONAL || currentRole == UserRole.REMOTE_FREELANCER -> Icons.Default.Work
                                else -> Icons.Default.Face
                            },
                            contentDescription = "Profile Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = activeProfile.name.ifBlank { "You" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isKid) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PolishRecoContainer,
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    Text(
                                        text = "🎒 Kid Mode",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = PolishOnReco,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${activeProfile.occupationTitle} • Age ${activeProfile.age} • ${activeProfile.gender}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = onEditProfileClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PolishOutline),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("edit_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = PolishWineDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Customize",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PolishWineDark
                    )
                }
            }

            HorizontalDivider(color = PolishOutline.copy(alpha = 0.5f))

            // Schedule Context Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Focus / School Hours Chip
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = PolishSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isKid) Icons.Default.School else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = PolishWineDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = if (isKid) "School Hours" else "Focus Hours",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = PolishTextMuted
                            )
                            Text(
                                text = formatHourRange(activeProfile.focusStartHour, activeProfile.focusEndHour),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = PolishWineDark
                            )
                        }
                    }
                }

                // Bedtime Chip
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = PolishSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Target Bedtime",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = PolishTextMuted
                            )
                            Text(
                                text = formatHour(activeProfile.bedtimeHour),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = PolishPrimary
                            )
                        }
                    }
                }
            }

            // Day Status & AI Guidance pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDayOff) PolishRecoContainer else PolishAiCallout,
                border = BorderStroke(1.dp, if (isDayOff) PolishOnReco.copy(alpha = 0.2f) else PolishOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isDayOff) Icons.Default.WbSunny else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isDayOff) PolishOnReco else PolishWineDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isDayOff) {
                            "Scheduled Day Off: Screen pacing is relaxed today. Enjoy offline recovery!"
                        } else {
                            "AI is calibrating habit predictions to your ${activeProfile.occupationTitle} profile."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isDayOff) PolishOnReco else PolishWineDark
                    )
                }
            }
        }
    }
}

// ============================================================================
// 2. COMPREHENSIVE USER PROFILE & HABIT CALIBRATION DIALOG
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileEditDialog(
    currentProfile: UserProfileEntity?,
    onDismiss: () -> Unit,
    onSaveProfile: (UserProfileEntity) -> Unit,
    onApplyRolePreset: (UserRole) -> Unit
) {
    val initial = currentProfile ?: UserProfileEntity()

    var name by remember { mutableStateOf(initial.name) }
    var age by remember { mutableIntStateOf(initial.age) }
    var gender by remember { mutableStateOf(initial.gender) }
    var selectedRole by remember { mutableStateOf(initial.getRole()) }
    var occupationTitle by remember { mutableStateOf(initial.occupationTitle) }
    var focusStartHour by remember { mutableIntStateOf(initial.focusStartHour) }
    var focusEndHour by remember { mutableIntStateOf(initial.focusEndHour) }
    var bedtimeHour by remember { mutableIntStateOf(initial.bedtimeHour) }
    var wakeHour by remember { mutableIntStateOf(initial.wakeHour) }
    var dailyScreenTimeMinutes by remember { mutableIntStateOf(initial.dailyScreenTimeTargetMinutes) }
    var isKidMode by remember { mutableStateOf(initial.isKidMode) }
    
    // Days off set
    val allDays = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    var selectedDaysOff by remember { mutableStateOf(initial.getDaysOffList().toSet()) }

    // Habit focus tags
    val availableGoalTags = listOf(
        "REDUCE_BEDTIME_SCROLL" to "🌙 Reduce Bedtime Scroll",
        "PROTECT_FOCUS" to "⚡ Deep Focus Protection",
        "LIMIT_SOCIAL_UNLOCKS" to "🛑 Limit Reflex Unlocks",
        "PHYSICAL_ACTIVITY" to "🏃 Daily Steps & Movement",
        "STUDY_HOMEWORK" to "🎒 Study & Homework Routine",
        "MEALTIME_NO_PHONE" to "🍽️ Phone-Free Meals"
    )
    var selectedGoals by remember { mutableStateOf(initial.getGoalsList().toSet()) }

    val genders = listOf("Female", "Male", "Non-binary", "Prefer not to say")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_profile_edit_dialog"),
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
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
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = PolishWineDark,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Profile & Schedule",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Calibrates AI forecasts, nudges & habits",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = PolishTextMuted
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Quick Role Presets
                Text(
                    text = "1. SELECT PERSONA PRESET",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = PolishTextMuted
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UserRole.entries.forEach { role ->
                        val isSelected = selectedRole == role
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedRole = role
                                occupationTitle = role.displayName
                                focusStartHour = role.defaultFocusStart
                                focusEndHour = role.defaultFocusEnd
                                bedtimeHour = role.defaultBedtime
                                dailyScreenTimeMinutes = role.defaultScreenLimitMinutes
                                isKidMode = role.defaultIsKid
                                if (role == UserRole.KID_STUDENT) {
                                    age = 10
                                } else if (role == UserRole.TEEN_STUDENT) {
                                    age = 15
                                }
                            },
                            label = {
                                Text(
                                    text = when (role) {
                                        UserRole.KID_STUDENT -> "🎒 Kid / School"
                                        UserRole.TEEN_STUDENT -> "🎓 Teen Student"
                                        UserRole.COLLEGE_STUDENT -> "🏛️ College"
                                        UserRole.WORKING_PROFESSIONAL -> "💼 Professional"
                                        UserRole.REMOTE_FREELANCER -> "💻 Freelance / Remote"
                                        UserRole.HOMEMAKER_PARENT -> "🏡 Homemaker"
                                        UserRole.RETIRED -> "🌿 Retired"
                                        UserRole.OTHER -> "⚙️ Custom"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PolishWineDark,
                                selectedLabelColor = Color.White,
                                containerColor = PolishSurfaceVariant,
                                labelColor = PolishWineDark
                            ),
                            border = BorderStroke(1.dp, if (isSelected) PolishWineDark else PolishOutline)
                        )
                    }
                }

                // Section 2: Personal Details (Name, Age, Gender)
                Text(
                    text = "2. PERSONAL DETAILS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = PolishTextMuted
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PolishWineDark,
                        unfocusedBorderColor = PolishOutline
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Age Stepper
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PolishOutline),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (age > 5) age-- },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Age", modifier = Modifier.size(16.dp))
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Age", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = PolishTextMuted)
                                Text("$age yrs", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            IconButton(
                                onClick = { if (age < 100) age++ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Age", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Kid Mode Switch Toggle
                    Surface(
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isKidMode) PolishRecoContainer else PolishSurfaceVariant,
                        border = BorderStroke(1.dp, if (isKidMode) PolishOnReco.copy(alpha = 0.3f) else PolishOutline)
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { isKidMode = !isKidMode }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Kid Safe Mode", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
                                Text(if (isKidMode) "Active 🎒" else "Off", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = PolishTextMuted)
                            }
                            Switch(
                                checked = isKidMode,
                                onCheckedChange = { isKidMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PolishWineDark
                                ),
                                modifier = Modifier.testTag("kid_mode_switch")
                            )
                        }
                    }
                }

                // Gender Select
                Text(
                    text = "Gender",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = PolishTextSecondary
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    genders.forEach { g ->
                        val isSel = gender == g
                        FilterChip(
                            selected = isSel,
                            onClick = { gender = g },
                            label = { Text(g, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PolishWineDark,
                                selectedLabelColor = Color.White,
                                containerColor = PolishSurfaceVariant,
                                labelColor = PolishWineDark
                            ),
                            border = BorderStroke(1.dp, if (isSel) PolishWineDark else PolishOutline)
                        )
                    }
                }

                // Section 3: Schedule & Focus / School Hours
                Text(
                    text = "3. SCHEDULE & FOCUS HOURS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = PolishTextMuted
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PolishSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isKidMode) "School / Homework Hours: ${formatHourRange(focusStartHour, focusEndHour)}"
                            else "Work / Focus Hours: ${formatHourRange(focusStartHour, focusEndHour)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = PolishWineDark
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Start Hour
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Start: ${formatHour(focusStartHour)}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = PolishTextMuted)
                                Slider(
                                    value = focusStartHour.toFloat(),
                                    onValueChange = { focusStartHour = it.toInt() },
                                    valueRange = 6f..14f,
                                    steps = 7,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PolishWineDark,
                                        activeTrackColor = PolishWineDark
                                    )
                                )
                            }

                            // End Hour
                            Column(modifier = Modifier.weight(1f)) {
                                Text("End: ${formatHour(focusEndHour)}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = PolishTextMuted)
                                Slider(
                                    value = focusEndHour.toFloat(),
                                    onValueChange = { focusEndHour = it.toInt() },
                                    valueRange = 13f..21f,
                                    steps = 7,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PolishWineDark,
                                        activeTrackColor = PolishWineDark
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 4: Scheduled Days Off
                Text(
                    text = "4. SCHEDULED DAYS OFF / WEEKENDS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = PolishTextMuted
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dayNames = listOf(
                        "MONDAY" to "Mon",
                        "TUESDAY" to "Tue",
                        "WEDNESDAY" to "Wed",
                        "THURSDAY" to "Thu",
                        "FRIDAY" to "Fri",
                        "SATURDAY" to "Sat",
                        "SUNDAY" to "Sun"
                    )

                    dayNames.forEach { (full, short) ->
                        val isDayOff = selectedDaysOff.contains(full)
                        FilterChip(
                            selected = isDayOff,
                            onClick = {
                                selectedDaysOff = if (isDayOff) {
                                    selectedDaysOff - full
                                } else {
                                    selectedDaysOff + full
                                }
                            },
                            label = { Text(short, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PolishOnReco,
                                selectedLabelColor = Color.White,
                                containerColor = PolishSurfaceVariant,
                                labelColor = PolishWineDark
                            ),
                            border = BorderStroke(1.dp, if (isDayOff) PolishOnReco else PolishOutline)
                        )
                    }
                }

                Text(
                    text = "Days off (${selectedDaysOff.size} selected): AI allows relaxed pacing and does not penalize recreational screen time.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = PolishTextMuted
                )

                // Section 5: Sleep Schedule & Screen Time Target
                Text(
                    text = "5. SLEEP & DAILY SCREEN BUDGET",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = PolishTextMuted
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PolishSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Target Bedtime: ${formatHour(bedtimeHour)} (Wake: ${formatHour(wakeHour)})",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = PolishWineDark
                        )

                        Slider(
                            value = bedtimeHour.toFloat(),
                            onValueChange = { bedtimeHour = it.toInt() },
                            valueRange = 19f..24f,
                            steps = 4,
                            colors = SliderDefaults.colors(
                                thumbColor = PolishPrimary,
                                activeTrackColor = PolishPrimary
                            )
                        )

                        Text(
                            text = "Daily Screen Time Limit: ${dailyScreenTimeMinutes / 60}h ${dailyScreenTimeMinutes % 60}m",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = PolishWineDark
                        )

                        Slider(
                            value = dailyScreenTimeMinutes.toFloat(),
                            onValueChange = { dailyScreenTimeMinutes = (it / 15).toInt() * 15 },
                            valueRange = 60f..360f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = PolishWineDark,
                                activeTrackColor = PolishWineDark
                            )
                        )
                    }
                }

                // Section 6: Primary Habit Focus Areas
                Text(
                    text = "6. PRIMARY HABIT PRIORITIES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = PolishTextMuted
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableGoalTags.forEach { (key, label) ->
                        val isSelected = selectedGoals.contains(key)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedGoals = if (isSelected) {
                                    selectedGoals - key
                                } else {
                                    selectedGoals + key
                                }
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PolishWineDark,
                                selectedLabelColor = Color.White,
                                containerColor = PolishSurfaceVariant,
                                labelColor = PolishWineDark
                            ),
                            border = BorderStroke(1.dp, if (isSelected) PolishWineDark else PolishOutline)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = initial.copy(
                        name = name.ifBlank { "You" },
                        age = age,
                        gender = gender,
                        roleKey = selectedRole.name,
                        occupationTitle = occupationTitle.ifBlank { selectedRole.displayName },
                        focusStartHour = focusStartHour,
                        focusEndHour = focusEndHour,
                        daysOffCsv = selectedDaysOff.joinToString(","),
                        bedtimeHour = bedtimeHour,
                        wakeHour = wakeHour,
                        dailyScreenTimeTargetMinutes = dailyScreenTimeMinutes,
                        primaryGoalsCsv = selectedGoals.joinToString(","),
                        isKidMode = isKidMode,
                        isProfileCompleted = true,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSaveProfile(updated)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save & Calibrate AI", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", color = PolishTextMuted)
            }
        }
    )
}

// ============================================================================
// HELPER FORMATTING FUNCTIONS
// ============================================================================

private fun formatHour(hour: Int): String {
    return when {
        hour == 0 || hour == 24 -> "12:00 AM (Midnight)"
        hour == 12 -> "12:00 PM (Noon)"
        hour < 12 -> "$hour:00 AM"
        else -> "${hour - 12}:00 PM"
    }
}

private fun formatHourRange(startHour: Int, endHour: Int): String {
    val startStr = if (startHour == 12) "12 PM" else if (startHour < 12) "$startHour AM" else "${startHour - 12} PM"
    val endStr = if (endHour == 12) "12 PM" else if (endHour < 12) "$endHour AM" else "${endHour - 12} PM"
    return "$startStr - $endStr"
}
