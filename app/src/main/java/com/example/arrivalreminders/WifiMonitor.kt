package com.example.arrivalreminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager

/**
 * Watches for Wi-Fi network changes using a PendingIntent-based network callback,
 * which the system delivers even when the app process isn't running.
 */
object WifiMonitor {
    private fun pendingIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx,
        2,
        Intent(ctx, WifiReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    fun register(ctx: Context) {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return
        val pi = pendingIntent(ctx)

        try {
            cm.unregisterNetworkCallback(pi)
        } catch (e: Exception) {
            // Wasn't registered yet.
        }

        if (Storage.load(ctx).places.none { it.type == PlaceType.WIFI }) return

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        cm.registerNetworkCallback(request, pi)
    }

    /** Name of the Wi-Fi network we're connected to, or null if unknown (needs location permission). */
    @Suppress("DEPRECATION")
    fun currentSsid(ctx: Context): String? {
        val wm = ctx.applicationContext.getSystemService(WifiManager::class.java) ?: return null
        val raw = wm.connectionInfo?.ssid ?: return null
        val ssid = raw.trim('"')
        return if (ssid.isBlank() || ssid == "<unknown ssid>") null else ssid
    }
}
