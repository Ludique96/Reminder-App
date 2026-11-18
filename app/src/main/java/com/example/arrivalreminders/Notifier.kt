package com.example.arrivalreminders

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifier {
    private const val CHANNEL_ID = "arrivals"

    // Wi-Fi can flap and geofences can fire repeatedly near the edge; don't spam.
    private const val COOLDOWN_MS = 10 * 60 * 1000L

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Arrival reminders", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    /** Called when the phone arrives at a place. Posts a notification listing that place's open items. */
    @SuppressLint("MissingPermission")
    fun arrive(ctx: Context, placeId: String) {
        val data = Storage.load(ctx)
        val place = data.places.firstOrNull { it.id == placeId } ?: return
        val items = data.items.filter { it.placeId == placeId && !it.done }
        if (items.isEmpty()) return

        val prefs = ctx.getSharedPreferences("cooldown", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(placeId, 0L) < COOLDOWN_MS) return

        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return
        ensureChannel(ctx)

        val open = PendingIntent.getActivity(
            ctx,
            0,
            Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val style = NotificationCompat.InboxStyle()
        items.take(6).forEach { style.addLine(it.text) }

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("You're at ${place.name}")
            .setContentText(if (items.size == 1) items[0].text else "${items.size} things to do here")
            .setStyle(style)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(ctx).notify(placeId.hashCode(), notification)
        prefs.edit().putLong(placeId, now).apply()
    }
}
