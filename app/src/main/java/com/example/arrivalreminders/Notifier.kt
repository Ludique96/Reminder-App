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

    const val ACTION_DONE = "com.example.arrivalreminders.ACTION_DONE"
    const val EXTRA_PLACE_ID = "place_id"

    private const val MAX_LINES = 6

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Arrival reminders", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    /** Called when the phone arrives at a place. Posts a notification listing that place's open items. */
    @SuppressLint("MissingPermission")
    fun arrive(ctx: Context, placeId: String) {
        var data = Storage.load(ctx)
        val place = data.places.firstOrNull { it.id == placeId } ?: return

        val prefs = ctx.getSharedPreferences("cooldown", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        // Wi-Fi can flap and geofences can fire repeatedly near the edge; the user picks how long to wait.
        val cooldownMs = AppSettings.cooldownMinutes(ctx) * 60_000L
        if (now - prefs.getLong(placeId, 0L) < cooldownMs) return

        // Repeating items that were ticked off on a previous visit come back fresh.
        val refreshed = data.items.map {
            if (it.placeId == placeId && it.repeat && it.done) it.copy(done = false) else it
        }
        if (refreshed != data.items) {
            data = data.copy(items = refreshed)
            Storage.save(ctx, data)
        }

        val items = data.items.filter { it.placeId == placeId && !it.done }
        if (items.isEmpty()) return

        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return
        ensureChannel(ctx)

        val open = PendingIntent.getActivity(
            ctx,
            0,
            Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val doneIntent = Intent(ctx, NotificationActionReceiver::class.java)
            .setAction(ACTION_DONE)
            .putExtra(EXTRA_PLACE_ID, placeId)
        val donePending = PendingIntent.getBroadcast(
            ctx,
            placeId.hashCode(),
            doneIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val style = NotificationCompat.InboxStyle()
        items.take(MAX_LINES).forEach { style.addLine(it.text) }
        if (items.size > MAX_LINES) style.setSummaryText("+${items.size - MAX_LINES} more")

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pin)
            .setContentTitle("You're at ${place.name}")
            .setContentText(if (items.size == 1) items[0].text else "${items.size} things to do here")
            .setStyle(style)
            .setContentIntent(open)
            .addAction(0, if (items.size == 1) "Done" else "Mark all done", donePending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(ctx).notify(placeId.hashCode(), notification)
        prefs.edit().putLong(placeId, now).apply()
    }
}
