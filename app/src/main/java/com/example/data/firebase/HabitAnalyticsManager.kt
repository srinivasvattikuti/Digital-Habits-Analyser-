package com.example.data.firebase

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Production-grade Firebase Analytics and Diagnostic Telemetry Manager with Crashlytics.
 * Tracks screen views, habit goal changes, focus sessions, AI insight generations,
 * cloud database sync events, custom breadcrumbs, and non-fatal error reports.
 */
class HabitAnalyticsManager(private val context: Context) {

    private val TAG = "HabitAnalytics"
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var firebaseCrashlytics: FirebaseCrashlytics? = null

    init {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAnalytics init deferred or unavailable: ${e.message}")
        }

        try {
            firebaseCrashlytics = FirebaseCrashlytics.getInstance()
            firebaseCrashlytics?.setCrashlyticsCollectionEnabled(true)
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseCrashlytics init deferred or unavailable: ${e.message}")
        }
    }

    fun setUserId(userId: String?) {
        try {
            firebaseAnalytics?.setUserId(userId)
            if (userId != null) {
                firebaseCrashlytics?.setUserId(userId)
            } else {
                firebaseCrashlytics?.setUserId("")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting user ID: ${e.message}")
        }
    }

    fun setUserRole(role: String) {
        try {
            firebaseAnalytics?.setUserProperty("user_role", role)
            firebaseCrashlytics?.setCustomKey("user_role", role)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting user role: ${e.message}")
        }
    }

    fun logScreenView(screenName: String) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            firebaseCrashlytics?.log("Navigated to screen: $screenName")
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
            firebaseCrashlytics?.log("Goal updated: $goalId ($goalType) -> $newTargetMinutes min")
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
            firebaseCrashlytics?.log("Focus session finished: $durationMinutes min, $blockedAppsCount blocked")
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
            firebaseCrashlytics?.setCustomKey("last_sync_success", success)
            firebaseCrashlytics?.setCustomKey("last_sync_items", itemsCount)
            firebaseCrashlytics?.log("Firestore sync: $action, success=$success, items=$itemsCount")
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
            firebaseCrashlytics?.setCustomKey("balance_score", scoreBalance)
            firebaseCrashlytics?.log("AI insight generated: score=$scoreBalance, trigger=$primaryTrigger")
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
            firebaseCrashlytics?.log("Nudge interaction: $nudgeId -> $actionType")
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
            firebaseCrashlytics?.log("Auth event: $eventType ($method)")
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
            if (throwable != null) {
                firebaseCrashlytics?.recordException(throwable)
            } else {
                firebaseCrashlytics?.recordException(Exception("[$tag] $message"))
            }
            firebaseCrashlytics?.log("NonFatal logged [$tag]: $message")
        } catch (e: Exception) {
            // Ignore
        }
    }
}
