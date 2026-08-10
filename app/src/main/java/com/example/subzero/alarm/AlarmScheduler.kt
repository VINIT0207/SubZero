package com.example.subzero.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.subzero.data.Asset
import com.example.subzero.data.AssetCategory
import com.example.subzero.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarmsForAsset(context: Context, asset: Asset) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        cancelAlarmsForAsset(context, asset)

        if (asset.isCanceled) return

        val targetDate = if (asset.category == AssetCategory.GIFT_CARD) {
            asset.expiryDate
        } else {
            asset.nextBillingDate
        } ?: return

        val sharedPrefs = context.getSharedPreferences("subzero_prefs", Context.MODE_PRIVATE)
        val alertHour = sharedPrefs.getInt("notification_hour", 9)
        val alertMinute = sharedPrefs.getInt("notification_minute", 0)

        for (nudgeDays in asset.nudgesBeforeCharge) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = targetDate - (nudgeDays.toLong() * 24L * 60L * 60L * 1000L)
                set(Calendar.HOUR_OF_DAY, alertHour)
                set(Calendar.MINUTE, alertMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val nudgeTimeMs = calendar.timeInMillis
            
            if (nudgeTimeMs > System.currentTimeMillis()) {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("ASSET_ID", asset.id)
                    putExtra("ASSET_NAME", asset.name)
                    putExtra("ASSET_COST", asset.cost)
                    putExtra("NUDGE_DAYS", nudgeDays)
                    putExtra("IS_TRIAL", asset.isTrial)
                    putExtra("IS_GIFT_CARD", asset.category == AssetCategory.GIFT_CARD)
                    putExtra("ASSET_NOTES", asset.notes)
                }

                val requestCode = asset.id * 100 + nudgeDays
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                nudgeTimeMs,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                nudgeTimeMs,
                                pendingIntent
                            )
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            nudgeTimeMs,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            nudgeTimeMs,
                            pendingIntent
                        )
                    }
                } catch (e: Exception) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                nudgeTimeMs,
                                pendingIntent
                            )
                        } else {
                            alarmManager.set(
                                AlarmManager.RTC_WAKEUP,
                                nudgeTimeMs,
                                pendingIntent
                            )
                        }
                    } catch (ex: Exception) {
                        // Silent fail for production stability
                    }
                }
            }
        }
    }

    fun cancelAlarmsForAsset(context: Context, asset: Asset) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val possibleNudgeDays = listOf(1, 3, 7, 14, 30)
        for (nudgeDays in possibleNudgeDays) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val requestCode = asset.id * 100 + nudgeDays
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
