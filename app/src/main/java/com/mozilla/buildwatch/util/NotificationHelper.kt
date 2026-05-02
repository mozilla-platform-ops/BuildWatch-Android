package com.mozilla.buildwatch.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mozilla.buildwatch.MainActivity
import com.mozilla.buildwatch.R
import com.mozilla.buildwatch.model.Push

object NotificationHelper {

    const val CHANNEL_ID = "build_complete"
    const val EXTRA_PUSH_ID = "pushId"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Build Complete",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifies when a watched try push finishes"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun fireNotification(context: Context, push: Push, failureCount: Int) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val title = if (failureCount == 0) "Try push passed" else "Try push failed"
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PUSH_ID, push.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            push.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(push.displayTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(push.id, notification)
    }
}
