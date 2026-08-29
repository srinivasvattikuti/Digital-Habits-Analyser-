package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.components.SummaryMetricCard
import com.example.ui.theme.DigitalHabitsTheme
import com.example.ui.theme.PolishPrimary
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun habit_dashboard_metric_screenshot() {
        composeTestRule.setContent {
            DigitalHabitsTheme {
                SummaryMetricCard(
                    title = "Screen Time",
                    value = "3h 42m",
                    subtitle = "Total active usage",
                    icon = Icons.Default.Schedule,
                    iconTint = PolishPrimary
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/habit_metric.png")
    }
}

