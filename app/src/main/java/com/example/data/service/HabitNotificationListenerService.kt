package com.example.data.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.db.HabitDatabase
import com.example.data.model.AppCategory
import com.example.data.model.DailyAggregateEntity
import com.example.data.model.UsageEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HabitNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (packageName == this.packageName) return // Ignore self-notifications

        val timestamp = sbn.postTime
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dateStr = dateFormat.format(Date(timestamp))

        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".")
        }

        serviceScope.launch {
            try {
                val db = HabitDatabase.getDatabase(applicationContext)

                // 1. Record Usage Event for Notification
                val event = UsageEventEntity(
                    packageName = packageName,
                    appName = appName,
                    eventType = "NOTIFICATION",
                    timestamp = timestamp,
                    durationMs = 0L,
                    hourOfDay = hour,
                    dayOfWeek = dayOfWeek,
                    dateStr = dateStr,
                    isCompulsiveTrigger = true
                )
                db.usageEventDao().insertEvent(event)

                // 2. Increment daily aggregate notification count
                val aggId = "$dateStr-$packageName"
                val existing = db.dailyAggregateDao().getAggregatesSinceSync(dateStr)
                    .firstOrNull { it.id == aggId }

                val updatedAgg = existing?.copy(
                    notificationCount = existing.notificationCount + 1
                ) ?: DailyAggregateEntity(
                    id = aggId,
                    dateStr = dateStr,
                    packageName = packageName,
                    appName = appName,
                    category = AppCategory.fromPackage(packageName, appName).name,
                    totalDurationMs = 0L,
                    openCount = 0,
                    notificationCount = 1
                )
                db.dailyAggregateDao().insertAggregate(updatedAgg)
            } catch (e: Exception) {
                Log.e("NotificationListener", "Failed to process notification: ${e.message}")
            }
        }
    }

    companion object {
        fun isNotificationAccessGranted(context: Context): Boolean {
            val packageName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            if (flat != null) {
                val names = flat.split(":")
                for (name in names) {
                    val cn = ComponentName.unflattenFromString(name)
                    if (cn != null && cn.packageName == packageName) {
                        return true
                    }
                }
            }
            return false
        }
    }
}
