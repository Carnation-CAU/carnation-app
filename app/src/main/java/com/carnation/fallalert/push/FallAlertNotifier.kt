package com.carnation.fallalert.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.carnation.fallalert.MainActivity
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.util.roomLabel
import com.carnation.fallalert.util.toPercent

/**
 * 낙상 의심 알림 표시. FCM 이든 목업이든 이 진입점 하나만 쓴다.
 *
 * 알림은 놓치면 안 되므로 중요도를 HIGH 로 두되, 문구는 "낙상"이 아니라
 * **"낙상 의심"** 으로 고정한다. (명세 8장 안전 원칙)
 */
object FallAlertNotifier {

    const val CHANNEL_ID = "fall_suspected"
    const val EXTRA_WINDOW_ID = "window_id"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "낙상 의심 알림",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "보호 대상자의 낙상 의심 상황을 즉시 알립니다."
            enableVibration(true)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    @android.annotation.SuppressLint("MissingPermission") // hasPermission() 으로 먼저 확인한다.
    fun notify(context: Context, event: FallEvent) {
        if (!hasPermission(context)) return
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WINDOW_ID, event.windowId)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            event.windowId.hashCode(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("낙상 의심")
            .setContentText("${roomLabel(event.roomId)} · 위험도 ${event.riskScore.toPercent()}%")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(event.windowId.hashCode(), notification)
    }

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
