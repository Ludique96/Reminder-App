package com.example.arrivalreminders

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MainActivity : ComponentActivity() {
    // Bumped whenever permissions may have changed so the UI re-checks them.
    private var tick by mutableIntStateOf(0)

    private val backgroundLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            tick++
            Monitoring.registerAll(this)
        }

    private val basicLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            tick++
            if (Perms.hasFine(this) && !Perms.hasBackground(this)) {
                // Android requires background location to be requested separately, after foreground.
                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                Monitoring.registerAll(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifier.ensureChannel(this)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    App(tick = tick, onRequestPermissions = { requestPermissions() })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tick++
        Monitoring.registerAll(this)
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        basicLauncher.launch(perms.toTypedArray())
    }
}

@Composable
fun App(tick: Int, onRequestPermissions: () -> Unit) {
    val ctx = LocalContext.current
    var data by remember { mutableStateOf(Storage.load(ctx)) }
    var tab by remember { mutableIntStateOf(0) }
    val permsOk = remember(tick) { Perms.allGranted(ctx) }

    fun commit(new: Storage.Data) {
        data = new
        Storage.save(ctx, new)
        Monitoring.registerAll(ctx)
    }

    Scaffold { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
        ) {
            if (!permsOk) PermissionBanner(onRequestPermissions)
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("To-do") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Places") })
            }
            when (tab) {
                0 -> ItemsScreen(data) { commit(it) }
                else -> PlacesScreen(data) { commit(it) }
            }
        }
    }
}

@Composable
fun PermissionBanner(onRequest: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Reminders need location set to \"Allow all the time\" and notifications turned on " +
                    "so they can fire while the app is closed.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "If nothing happens when you tap, open Settings > Apps > Arrival Reminders > Permissions.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Button(onClick = onRequest, modifier = Modifier.padding(top = 8.dp)) {
                Text("Grant permissions")
            }
        }
    }
}

@Composable
fun ItemsScreen(data: Storage.Data, onChange: (Storage.Data) -> Unit) {
    var text by remember { mutableStateOf("") }
    var selectedPlaceId by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val selectedName = data.places.firstOrNull { it.id == selectedPlaceId }?.name ?: "No trigger"

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("New item") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Box {
                OutlinedButton(onClick = { menuOpen = true }) {
                    Text("When I arrive at: $selectedName")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("No trigger") },
                        onClick = { selectedPlaceId = null; menuOpen = false },
                    )
                    data.places.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name) },
                            onClick = { selectedPlaceId = p.id; menuOpen = false },
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = text.isNotBlank(),
                onClick = {
                    val item = TodoItem(text = text.trim(), placeId = selectedPlaceId)
                    onChange(data.copy(items = data.items + item))
                    text = ""
                },
            ) { Text("Add") }
        }
        if (data.places.isEmpty()) {
            Text(
                "Tip: add a place on the Places tab first, then attach items to it.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(data.items, key = { it.id }) { item ->
                val place = data.places.firstOrNull { it.id == item.placeId }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.done,
                        onCheckedChange = { checked ->
                            onChange(
                                data.copy(items = data.items.map { other ->
                                    if (other.id == item.id) other.copy(done = checked) else other
                                })
                            )
                        },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.text,
                            textDecoration = if (item.done) TextDecoration.LineThrough else null,
                        )
                        if (place != null) {
                            Text("At ${place.name}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    TextButton(onClick = {
                        onChange(data.copy(items = data.items.filter { other -> other.id != item.id }))
                    }) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
fun PlacesScreen(data: Storage.Data, onChange: (Storage.Data) -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var ssid by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("150") }
    var status by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text("Add a place", style = MaterialTheme.typography.titleMedium) }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name (e.g. Home)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item { Text("Wi-Fi trigger", style = MaterialTheme.typography.titleSmall) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("Network name (SSID)") },
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
                        status = "Couldn't read the current network. Connect to Wi-Fi and grant location permission."
                    }
                }) { Text("Use current") }
            }
        }
        item {
            Button(
                enabled = name.isNotBlank() && ssid.isNotBlank(),
                onClick = {
                    val place = Place(name = name.trim(), type = PlaceType.WIFI, ssid = ssid.trim())
                    onChange(data.copy(places = data.places + place))
                    name = ""
                    ssid = ""
                    status = "Saved Wi-Fi place."
                },
            ) { Text("Save Wi-Fi place") }
        }

        item { Text("Location trigger", style = MaterialTheme.typography.titleSmall) }
        item {
            OutlinedTextField(
                value = radius,
                onValueChange = { radius = it },
                label = { Text("Radius in metres (100-1000)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    status = "Getting your location..."
                    val r = (radius.toFloatOrNull() ?: 150f).coerceIn(100f, 1000f)
                    val placeName = name.trim()
                    fetchLocation(ctx) { loc ->
                        if (loc == null) {
                            status = "Couldn't get a location. Check that location is on and permission is granted."
                        } else {
                            val place = Place(
                                name = placeName,
                                type = PlaceType.LOCATION,
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                radiusMeters = r,
                            )
                            onChange(data.copy(places = data.places + place))
                            name = ""
                            status = "Saved this location."
                        }
                    }
                },
            ) { Text("Save my current location") }
        }

        status?.let { message -> item { Text(message, style = MaterialTheme.typography.bodySmall) } }

        item {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Saved places", style = MaterialTheme.typography.titleMedium)
        }
        items(data.places, key = { it.id }) { place ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(place.name)
                    val detail = when (place.type) {
                        PlaceType.WIFI -> "Wi-Fi: ${place.ssid}"
                        PlaceType.LOCATION -> "Location, ${place.radiusMeters.toInt()} m radius"
                    }
                    Text(detail, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = {
                    onChange(
                        data.copy(
                            places = data.places.filter { it.id != place.id },
                            // Items attached to a deleted place keep existing but lose their trigger.
                            items = data.items.map { item ->
                                if (item.placeId == place.id) item.copy(placeId = null) else item
                            },
                        )
                    )
                }) { Text("Delete") }
            }
        }
    }
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
