package com.example.arrivalreminders

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

/** The list of saved places. Tap one to edit it. */
@Composable
fun PlacesScreen(data: Storage.Data, onEdit: (Place) -> Unit) {
    if (data.places.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No places yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Add your home Wi-Fi or a spot on the map, then attach reminders to it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(data.places, key = { it.id }) { place ->
                val open = data.items.count { it.placeId == place.id && !it.done }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onEdit(place) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(place.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                when (place.type) {
                                    PlaceType.WIFI -> "Wi-Fi: ${place.ssid}"
                                    PlaceType.LOCATION -> "Location, ${place.radiusMeters.toInt()} m radius"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                if (open == 1) "1 open reminder" else "$open open reminders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

data class AddressResult(val label: String, val lat: Double, val lon: Double)

/** Add or edit one place. Pass initial = null to add. */
@Composable
fun PlaceDialog(
    initial: Place?,
    onSave: (Place) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    var type by remember { mutableStateOf(initial?.type ?: PlaceType.WIFI) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var ssid by remember { mutableStateOf(initial?.ssid ?: "") }
    var lat by remember { mutableStateOf<Double?>(initial?.latitude) }
    var lon by remember { mutableStateOf<Double?>(initial?.longitude) }
    var radius by remember { mutableFloatStateOf(initial?.radiusMeters ?: 150f) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<AddressResult>>(emptyList()) }
    var chosen by remember {
        mutableStateOf<String?>(if (initial?.latitude != null) "Saved location" else null)
    }
    var status by remember { mutableStateOf<String?>(null) }

    val valid = name.isNotBlank() && when (type) {
        PlaceType.WIFI -> ssid.isNotBlank()
        PlaceType.LOCATION -> lat != null && lon != null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New place" else "Edit place") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == PlaceType.WIFI,
                        onClick = { type = PlaceType.WIFI },
                        label = { Text("Wi-Fi network") },
                    )
                    FilterChip(
                        selected = type == PlaceType.LOCATION,
                        onClick = { type = PlaceType.LOCATION },
                        label = { Text("Location") },
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Home)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (type == PlaceType.WIFI) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = ssid,
                            onValueChange = { ssid = it },
                            label = { Text("Network name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            val current = WifiMonitor.currentSsid(ctx)
                            if (current != null) {
                                ssid = current
                                status = null
                            } else {
                                status = "Couldn't read the current network. Connect to Wi-Fi and allow location access."
                            }
                        }) { Text("Use current") }
                    }
                } else {
                    Text(
                        if (chosen != null) "Selected: $chosen" else "No location chosen yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Search for an address") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            enabled = query.isNotBlank(),
                            onClick = {
                                status = "Searching..."
                                searchAddress(ctx, query.trim()) { found ->
                                    if (found == null) {
                                        status = "Address search isn't available right now. Try your current location instead."
                                    } else if (found.isEmpty()) {
                                        status = "No matches. Try a fuller address."
                                    } else {
                                        results = found
                                        status = "Tap a result to select it."
                                    }
                                }
                            },
                        ) { Text("Search") }
                    }
                    results.forEach { r ->
                        Text(
                            r.label,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    lat = r.lat
                                    lon = r.lon
                                    chosen = r.label
                                    results = emptyList()
                                    status = null
                                }
                                .padding(vertical = 8.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            status = "Getting your location..."
                            fetchLocation(ctx) { loc ->
                                if (loc == null) {
                                    status = "Couldn't get a location. Check that location is on and allowed."
                                } else {
                                    lat = loc.latitude
                                    lon = loc.longitude
                                    chosen = "Current location"
                                    results = emptyList()
                                    status = null
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Use my current location") }
                    Text(
                        "Alert radius: ${radius.toInt()} m",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Slider(
                        value = radius,
                        onValueChange = { radius = it },
                        valueRange = 100f..1000f,
                    )
                }

                status?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete place", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val isWifi = type == PlaceType.WIFI
                    onSave(
                        (initial ?: Place(name = "", type = type)).copy(
                            name = name.trim(),
                            type = type,
                            ssid = if (isWifi) ssid.trim() else null,
                            latitude = if (isWifi) null else lat,
                            longitude = if (isWifi) null else lon,
                            radiusMeters = radius,
                        )
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Looks up an address with the phone's built-in geocoder. Calls back with null if unavailable. */
@Suppress("DEPRECATION")
private fun searchAddress(ctx: Context, query: String, onResult: (List<AddressResult>?) -> Unit) {
    if (!Geocoder.isPresent()) {
        onResult(null)
        return
    }
    val main = Handler(Looper.getMainLooper())
    Thread {
        val found: List<AddressResult>? = try {
            Geocoder(ctx, Locale.getDefault()).getFromLocationName(query, 5)
                ?.map { AddressResult(it.getAddressLine(0) ?: query, it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
        main.post { onResult(found) }
    }.start()
}

@SuppressLint("MissingPermission")
private fun fetchLocation(ctx: Context, onResult: (Location?) -> Unit) {
    if (!Perms.hasFine(ctx)) {
        onResult(null)
        return
    }
    LocationServices.getFusedLocationProviderClient(ctx)
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { onResult(it) }
        .addOnFailureListener { onResult(null) }
}
