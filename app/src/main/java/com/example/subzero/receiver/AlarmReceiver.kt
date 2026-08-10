package com.example.subzero.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.subzero.MainActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val assetId = intent.getIntExtra("ASSET_ID", 0)
        val assetName = intent.getStringExtra("ASSET_NAME") ?: "Subscription"
        val assetCost = intent.getDoubleExtra("ASSET_COST", 0.0)
        val nudgeDays = intent.getIntExtra("NUDGE_DAYS", 0)
        val isTrial = intent.getBooleanExtra("IS_TRIAL", false)
        val isGiftCard = intent.getBooleanExtra("IS_GIFT_CARD", false)
        val notes = intent.getStringExtra("ASSET_NOTES") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "subzero_alerts_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SubZero Financial Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts before subscription renewals and trial expirations."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            assetId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = when {
            isTrial -> "Your \$$assetCost $assetName free trial expires in $nudgeDays day(s). Tap to check cancellation links."
            isGiftCard -> "Warning: your \$$assetCost $assetName store credit expires in $nudgeDays day(s)! Spend it now."
            else -> "Your \$$assetCost $assetName subscription charges in $nudgeDays day(s). Review your active billing!"
        }

        val title = when {
            isTrial -> "Action Required: Free Trial Ending"
            isGiftCard -> "Forgotten Credit Alert!"
            else -> "Subscription Renewal Coming Up"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(assetId * 100 + nudgeDays, notification)
    }
}
