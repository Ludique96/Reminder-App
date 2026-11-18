package com.example.arrivalreminders

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/** Registers one geofence per saved LOCATION place with Google Play Services. */
object GeofenceManager {
    private fun pendingIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx,
        1,
        Intent(ctx, GeofenceReceiver::class.java),
        // Geofencing fills in extras, so the PendingIntent must be mutable.
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    @SuppressLint("MissingPermission")
    fun register(ctx: Context) {
        val client = LocationServices.getGeofencingClient(ctx)
        val pi = pendingIntent(ctx)
        val places = Storage.load(ctx).places.filter {
            it.type == PlaceType.LOCATION && it.latitude != null && it.longitude != null
        }

        // Clear old registrations, then add the current set.
        client.removeGeofences(pi).addOnCompleteListener {
            if (places.isEmpty() || !Perms.hasFine(ctx)) return@addOnCompleteListener

            val geofences = places.map { p ->
                Geofence.Builder()
                    .setRequestId(p.id)
                    .setCircularRegion(p.latitude!!, p.longitude!!, p.radiusMeters)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()
            }
            // Initial trigger 0: don't fire just because you're already inside when (re)registering.
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(0)
                .addGeofences(geofences)
                .build()
            client.addGeofences(request, pi)
        }
    }
}
