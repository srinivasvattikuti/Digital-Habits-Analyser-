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
}
