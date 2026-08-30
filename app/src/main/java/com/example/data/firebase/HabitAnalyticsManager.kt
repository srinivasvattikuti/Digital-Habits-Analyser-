package com.example.data.firebase

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Production-grade Firebase Analytics and Diagnostic Telemetry Manager.
 * Tracks screen views, habit goal changes, focus sessions, AI insight generations,
 * and cloud database sync events.
 */
class HabitAnalyticsManager(private val context: Context) {

    private val TAG = "HabitAnalytics"
    private var firebaseAnalytics: FirebaseAnalytics? = null

    init {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAnalytics init deferred or unavailable: ${e.message}")
        }
    }

    fun logScreenView(screenName: String) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            Log.d(TAG, "Logged screen_view: $screenName")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging screen view: ${e.message}")
        }
    }

    fun logGoalUpdated(goalId: String, goalType: String, newTargetMinutes: Int) {
        try {
            val bundle = Bundle().apply {
                putString("goal_id", goalId)
                putString("goal_type", goalType)
                putInt("target_minutes", newTargetMinutes)
            }
            firebaseAnalytics?.logEvent("habit_goal_updated", bundle)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging goal update: ${e.message}")
        }
    }

    fun logFocusSessionCompleted(durationMinutes: Int, blockedAppsCount: Int) {
        try {
            val bundle = Bundle().apply {
                putInt("duration_minutes", durationMinutes)
                putInt("blocked_apps_count", blockedAppsCount)
            }
            firebaseAnalytics?.logEvent("focus_session_completed", bundle)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging focus session: ${e.message}")
        }
    }

    fun logCloudSync(action: String, success: Boolean, itemsCount: Int = 0) {
        try {
            val bundle = Bundle().apply {
                putString("sync_action", action)
                putBoolean("is_success", success)
                putInt("items_count", itemsCount)
            }
            firebaseAnalytics?.logEvent("cloud_firestore_sync", bundle)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging cloud sync: ${e.message}")
        }
    }

    fun logAiInsightGenerated(scoreBalance: Int, primaryTrigger: String) {
        try {
            val bundle = Bundle().apply {
                putInt("balance_score", scoreBalance)
                putString("primary_trigger", primaryTrigger)
            }
            firebaseAnalytics?.logEvent("ai_insight_generated", bundle)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging AI insight: ${e.message}")
        }
    }

    fun logProactiveNudgeAction(nudgeId: String, actionType: String) {
        try {
            val bundle = Bundle().apply {
                putString("nudge_id", nudgeId)
                putString("action_type", actionType)
            }
            firebaseAnalytics?.logEvent("proactive_nudge_interaction", bundle)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging nudge action: ${e.message}")
        }
    }

    fun logAuthEvent(eventType: String, method: String) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.METHOD, method)
            }
            firebaseAnalytics?.logEvent(eventType, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging auth event: ${e.message}")
        }
    }

    /**
     * Non-fatal error logger for diagnostic crashlytics & observability.
     */
    fun recordNonFatalException(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, "NonFatalDiagnostic: $message", throwable)
        try {
            val bundle = Bundle().apply {
                putString("tag", tag)
                putString("error_message", message.take(100))
            }
            firebaseAnalytics?.logEvent("app_non_fatal_error", bundle)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
