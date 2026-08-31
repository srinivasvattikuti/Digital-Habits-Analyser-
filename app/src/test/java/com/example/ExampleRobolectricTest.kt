package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.HabitDatabase
import com.example.data.repository.HabitRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `test habit data generation and repository`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = HabitRepository(context)

        // Initialize and generate rich demo data
        repository.initializeDataIfEmpty()

        val db = HabitDatabase.getDatabase(context)
        val aggregates = db.dailyAggregateDao().getAggregatesSinceSync("2000-01-01")
        assertTrue("Aggregates should be populated", aggregates.isNotEmpty())

        // Run AI pipeline
        val insight = repository.runAiHabitPipeline()
        assertNotNull("Insight should not be null", insight)
        assertTrue("Insight should have dominant apps", insight.dominantAppsJson.isNotBlank())

        // Test grounded AI chat
        val chatAnswer = repository.sendChatMessage("Why am I using this app so much at night?")
        assertTrue("AI chat answer should be generated", chatAnswer.isNotBlank())
    }

    @Test
    fun `test weekly aggregates and notification telemetry`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = HabitRepository(context)
        repository.initializeDataIfEmpty()

        val db = HabitDatabase.getDatabase(context)
        val allAggs = db.dailyAggregateDao().getAggregatesSinceSync("2000-01-01")
        val totalNotifs = allAggs.sumOf { it.notificationCount }
        val totalDuration = allAggs.sumOf { it.totalDurationMs }

        assertTrue("Should have notification telemetry logged", totalNotifs > 0)
        assertTrue("Should have screen duration logged", totalDuration > 0)
    }

    @Test
    fun `test analytics and sync manager initial state`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = HabitRepository(context)

        // Verify analytics events do not crash without google-services.json
        repository.analyticsManager.logScreenView("DASHBOARD")
        repository.analyticsManager.logGoalUpdated("goal_1", "MAX_LIMIT", 45)
        repository.analyticsManager.recordNonFatalException("TestTag", "Sample diagnostic event")

        assertNotNull("Auth manager should be initialized", repository.authManager)
        assertNotNull("Firestore sync manager should be initialized", repository.firestoreSync)
    }

    @Test
    fun `test sdui natural language customization pipeline`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = HabitRepository(context)
        repository.initializeDataIfEmpty()

        // 1. Test standard natural language customization
        val response = repository.customizeDashboardWithPrompt(
            prompt = "Show screen time at the top, hide notification counts, and simplify the layout",
            userId = "test_user_1"
        )

        assertTrue("Customization should succeed", response.success)
        assertNotNull("Returned layout should not be null", response.layout)
        val layout = response.layout!!
        val notifComponent = layout.components.find { it.type == com.example.data.sdui.CardType.NOTIFICATION_LEADERBOARD }
        assertEquals("Notification leaderboard should be hidden", false, notifComponent?.visible)

        // 2. Test rejection of unsupported native capabilities (installing binaries, root access)
        val rejectedResponse = repository.customizeDashboardWithPrompt(
            prompt = "Install a new native Linux kernel module to intercept device keystrokes and modify system OS permissions",
            userId = "test_user_1"
        )
        assertEquals("Should fail for native binary execution", false, rejectedResponse.success)
        assertTrue("Should indicate native update requirement", rejectedResponse.requiresNativeUpdate)
        assertEquals(
            "This customization requires a native app update and cannot be rendered dynamically.",
            rejectedResponse.errorMessage
        )

        // 3. Test Room Database layout persistence
        val saved = repository.getSavedLayoutForUser("test_user_1")
        assertEquals("Persisted layout name should match", layout.layoutName, saved.layoutName)
    }
}
