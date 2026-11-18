package com.example.arrivalreminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Perms {
    private fun granted(ctx: Context, permission: String) =
        ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

    fun hasFine(ctx: Context) = granted(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
    fun hasBackground(ctx: Context) = granted(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    fun hasNotifications(ctx: Context) = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    fun allGranted(ctx: Context) = hasFine(ctx) && hasBackground(ctx) && hasNotifications(ctx)
}

object Monitoring {
    /** (Re)registers all triggers from the saved places. Safe to call as often as you like. */
    fun registerAll(ctx: Context) {
        val app = ctx.applicationContext
        try {
            GeofenceManager.register(app)
        } catch (e: Exception) {
            // Missing permission or Play Services; the UI shows permission status.
        }
        try {
            WifiMonitor.register(app)
        } catch (e: Exception) {
            // Same as above.
        }
    }
}
