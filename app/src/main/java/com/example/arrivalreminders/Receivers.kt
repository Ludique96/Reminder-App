package com.example.arrivalreminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/** Fired by Play Services when you enter a saved location. */
class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        event.triggeringGeofences?.forEach { Notifier.arrive(context, it.requestId) }
    }
}

/** Fired on any Wi-Fi network change. We check which network we're on and match it to saved places. */
class WifiReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        Thread {
            try {
                // Give the connection a moment to settle so the SSID is readable.
                Thread.sleep(2000)
                val ssid = WifiMonitor.currentSsid(app) ?: return@Thread
                Storage.load(app).places
                    .filter { it.type == PlaceType.WIFI && it.ssid == ssid }
                    .forEach { Notifier.arrive(app, it.id) }
            } finally {
                pending.finish()
            }
        }.start()
    }
}

/** Geofences and network callbacks are cleared on reboot/app update, so register them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Monitoring.registerAll(context)
    }
}
