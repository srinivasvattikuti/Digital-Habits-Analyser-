package com.example.data.collector

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.example.data.db.HabitDatabase
import com.example.data.model.AppCategory
import com.example.data.model.AppInfoEntity
import com.example.data.model.DailyAggregateEntity
import com.example.data.model.UsageEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UsageStatsCollector(private val context: Context) {

    private val db = HabitDatabase.getDatabase(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun collectInstalledApps(): List<AppInfoEntity> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val appList = mutableListOf<AppInfoEntity>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(pm).toString()
            val isSystem = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: Exception) {
                false
            }
            val category = AppCategory.fromPackage(pkg, appName)
            val iconColor = getAppColorHex(category)

            appList.add(
                AppInfoEntity(
                    packageName = pkg,
                    appName = appName,
                    category = category.name,
                    isSystemApp = isSystem,
                    iconColorHex = iconColor
                )
            )
        }

        if (appList.isNotEmpty()) {
            db.appInfoDao().insertApps(appList)
        }
        appList
    }

    suspend fun collectUsageEventsAndAggregates(daysBack: Int = 1): Boolean = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission()) return@withContext false

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext false

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -daysBack)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val eventObj = UsageEvents.Event()

        val parsedEvents = mutableListOf<UsageEventEntity>()
        val appOpenTimestamps = mutableMapOf<String, Long>() // pkg -> lastResumeTime
        val aggregateMap = mutableMapOf<String, DailyAggregateAccumulator>()

        val pm = context.packageManager

        while (events.hasNextEvent()) {
            events.getNextEvent(eventObj)
            val pkg = eventObj.packageName ?: continue
            val time = eventObj.timeStamp

            val eventCal = Calendar.getInstance().apply { timeInMillis = time }
            val hour = eventCal.get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = eventCal.get(Calendar.DAY_OF_WEEK)
            val dateStr = dateFormat.format(Date(time))
            val aggKey = "$dateStr-$pkg"

            val appName = try {
                val info = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(info).toString()
            } catch (e: Exception) {
                pkg.substringAfterLast(".")
            }

            val accumulator = aggregateMap.getOrPut(aggKey) {
                DailyAggregateAccumulator(
                    dateStr = dateStr,
                    packageName = pkg,
                    appName = appName,
                    category = AppCategory.fromPackage(pkg, appName).name
                )
            }

            val isForeground = (eventObj.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && eventObj.eventType == UsageEvents.Event.ACTIVITY_RESUMED))

            val isBackground = (eventObj.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && eventObj.eventType == UsageEvents.Event.ACTIVITY_PAUSED))

            if (isForeground) {
                appOpenTimestamps[pkg] = time
                accumulator.openCount++

                parsedEvents.add(
                    UsageEventEntity(
                        packageName = pkg,
                        appName = appName,
                        eventType = "OPEN",
                        timestamp = time,
                        durationMs = 0L,
                        hourOfDay = hour,
                        dayOfWeek = dayOfWeek,
                        dateStr = dateStr,
                        isCompulsiveTrigger = false
                    )
                )
            } else if (isBackground) {
                val openTime = appOpenTimestamps[pkg]
                if (openTime != null && time >= openTime) {
                    val duration = time - openTime
                    accumulator.totalDurationMs += duration
                    val durationMin = (duration / 60000).toInt()

                    when (hour) {
                        in 5..11 -> accumulator.morningMinutes += durationMin
                        in 12..16 -> accumulator.afternoonMinutes += durationMin
                        in 17..21 -> accumulator.eveningMinutes += durationMin
                        else -> accumulator.nightMinutes += durationMin
                    }

                    val isCompulsive = duration in 1..29999 // Under 30 seconds quick check
                    if (isCompulsive) {
                        accumulator.compulsiveOpens++
                    }

                    parsedEvents.add(
                        UsageEventEntity(
                            packageName = pkg,
                            appName = appName,
                            eventType = "SESSION",
                            timestamp = openTime,
                            durationMs = duration,
                            hourOfDay = hour,
                            dayOfWeek = dayOfWeek,
                            dateStr = dateStr,
                            isCompulsiveTrigger = isCompulsive
                        )
                    )
                    appOpenTimestamps.remove(pkg)
                }
            }
        }

        if (parsedEvents.isNotEmpty()) {
            db.usageEventDao().insertEvents(parsedEvents)
        }

        val dailyAggregates = aggregateMap.values.map { acc ->
            DailyAggregateEntity(
                id = "${acc.dateStr}-${acc.packageName}",
                dateStr = acc.dateStr,
                packageName = acc.packageName,
                appName = acc.appName,
                category = acc.category,
                totalDurationMs = acc.totalDurationMs,
                openCount = acc.openCount,
                notificationCount = 0,
                morningMinutes = acc.morningMinutes,
                afternoonMinutes = acc.afternoonMinutes,
                eveningMinutes = acc.eveningMinutes,
                nightMinutes = acc.nightMinutes,
                compulsiveOpens = acc.compulsiveOpens
            )
        }

        if (dailyAggregates.isNotEmpty()) {
            db.dailyAggregateDao().insertAggregates(dailyAggregates)
        }

        true
    }

    private fun getAppColorHex(category: AppCategory): String {
        return when (category) {
            AppCategory.SOCIAL -> "#EC4899"
            AppCategory.PRODUCTIVITY -> "#10B981"
            AppCategory.ENTERTAINMENT -> "#8B5CF6"
            AppCategory.SHOPPING -> "#F59E0B"
            AppCategory.FINANCE -> "#06B6D4"
            AppCategory.COMMUNICATION -> "#3B82F6"
            AppCategory.UTILITIES -> "#64748B"
            AppCategory.HEALTH -> "#14B8A6"
            AppCategory.GAMES -> "#EF4444"
            AppCategory.OTHER -> "#6366F1"
        }
    }

    private data class DailyAggregateAccumulator(
        val dateStr: String,
        val packageName: String,
        val appName: String,
        val category: String,
        var totalDurationMs: Long = 0L,
        var openCount: Int = 0,
        var morningMinutes: Int = 0,
        var afternoonMinutes: Int = 0,
        var eveningMinutes: Int = 0,
        var nightMinutes: Int = 0,
        var compulsiveOpens: Int = 0
    )
}
