package com.example.arrivalreminders

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    // Bumped whenever permissions may have changed so the UI re-checks them.
    private var tick by mutableIntStateOf(0)
    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)

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
                // On most phones this doesn't show "All the time"; the app's settings card covers that.
                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                Monitoring.registerAll(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Notifier.ensureChannel(this)
        themeMode = AppSettings.themeMode(this)
        setContent {
            // Keep status/navigation bar icons readable when the app theme differs from the phone's.
            val dark = themeMode.isDark()
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                        detectDarkMode = { dark },
                    ),
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                        detectDarkMode = { dark },
                    ),
                )
                onDispose {}
            }
            AppTheme(themeMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    App(
                        tick = tick,
                        themeMode = themeMode,
                        onThemeChange = {
                            themeMode = it
                            AppSettings.setThemeMode(this@MainActivity, it)
                        },
                        onRequestPermissions = { requestPermissions() },
                    )
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

/** Opens this app's page in the phone's Settings, where "Allow all the time" lives. */
fun openAppSettings(ctx: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", ctx.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.startActivity(intent)
}

private const val TAB_COUNT = 3

/**
 * Swipe left/right on empty space to change tab. Items handle their own horizontal swipes
 * (swipe-to-delete) first, so this only fires where nothing else claims the drag.
 */
private fun Modifier.swipeToSwitchTabs(onSwipe: (direction: Int) -> Unit): Modifier =
    pointerInput(Unit) {
        val threshold = 72.dp.toPx()
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                if (total <= -threshold) onSwipe(1) else if (total >= threshold) onSwipe(-1)
            },
            onDragCancel = { total = 0f },
            onHorizontalDrag = { _, dragAmount -> total += dragAmount },
        )
    }

@Composable
fun App(
    tick: Int,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onRequestPermissions: () -> Unit,
) {
    val ctx = LocalContext.current
    // Reloaded whenever the app resumes so changes made from a notification show up.
    var data by remember(tick) { mutableStateOf(Storage.load(ctx)) }
    var tab by remember { mutableIntStateOf(0) }

    var showItemDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<TodoItem?>(null) }
    var showPlaceDialog by remember { mutableStateOf(false) }
    var editPlace by remember { mutableStateOf<Place?>(null) }

    val fine = remember(tick) { Perms.hasFine(ctx) }
    val background = remember(tick) { Perms.hasBackground(ctx) }
    val notifications = remember(tick) { Perms.hasNotifications(ctx) }
    val permsOk = fine && background && notifications

    fun commit(new: Storage.Data) {
        data = new
        Storage.save(ctx, new)
        Monitoring.registerAll(ctx)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    label = { Text("To-do") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    label = { Text("Places") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(SettingsIcon, contentDescription = null) },
                    label = { Text("Settings") },
                )
            }
        },
        floatingActionButton = {
            if (tab != 2) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (tab == 0) {
                            editItem = null
                            showItemDialog = true
                        } else {
                            editPlace = null
                            showPlaceDialog = true
                        }
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(if (tab == 0) "Add item" else "Add place") },
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (!permsOk) {
                PermissionCard(
                    fine = fine,
                    background = background,
                    notifications = notifications,
                    onRequest = onRequestPermissions,
                )
            }
            Box(
                Modifier
                    .weight(1f)
                    .swipeToSwitchTabs { direction ->
                        tab = (tab + direction).coerceIn(0, TAB_COUNT - 1)
                    }
            ) {
                when (tab) {
                    0 -> ItemsScreen(
                        data = data,
                        onChange = { commit(it) },
                        onEdit = { item ->
                            editItem = item
                            showItemDialog = true
                        },
                    )
                    1 -> PlacesScreen(
                        data = data,
                        onEdit = { place ->
                            editPlace = place
                            showPlaceDialog = true
                        },
                    )
                    else -> SettingsScreen(
                        themeMode = themeMode,
                        onThemeChange = onThemeChange,
                    )
                }
            }
        }
    }

    if (showItemDialog) {
        val current = editItem
        ItemDialog(
            data = data,
            initial = current,
            onSave = { saved ->
                val exists = data.items.any { it.id == saved.id }
                val items = if (exists) {
                    data.items.map { if (it.id == saved.id) saved else it }
                } else {
                    data.items + saved
                }
                commit(data.copy(items = items))
                showItemDialog = false
            },
            onDelete = if (current == null) null else ({
                commit(data.copy(items = data.items.filter { it.id != current.id }))
                showItemDialog = false
            }),
            onDismiss = { showItemDialog = false },
        )
    }

    if (showPlaceDialog) {
        val current = editPlace
        PlaceDialog(
            initial = current,
            onSave = { saved ->
                val exists = data.places.any { it.id == saved.id }
                val places = if (exists) {
                    data.places.map { if (it.id == saved.id) saved else it }
                } else {
                    data.places + saved
                }
                commit(data.copy(places = places))
                showPlaceDialog = false
            },
            onDelete = if (current == null) null else ({
                commit(
                    data.copy(
                        places = data.places.filter { it.id != current.id },
                        // Items attached to a deleted place keep existing but lose their trigger.
                        items = data.items.map { item ->
                            if (item.placeId == current.id) item.copy(placeId = null) else item
                        },
                    )
                )
                showPlaceDialog = false
            }),
            onDismiss = { showPlaceDialog = false },
        )
    }
}

@Composable
fun PermissionCard(
    fine: Boolean,
    background: Boolean,
    notifications: Boolean,
    onRequest: () -> Unit,
) {
    val ctx = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Finish setup so reminders can fire in the background",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            StatusLine(fine, "Location access")
            StatusLine(background, "Location set to \"Allow all the time\"")
            StatusLine(notifications, "Notifications")
            if (!background) {
                Text(
                    "Android doesn't offer \"All the time\" in the pop-up. Tap Open app settings, " +
                        "then Permissions > Location > Allow all the time (and turn on Use precise location).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                "While you're there, set Battery to Unrestricted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            if (!fine || !notifications) {
                Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                    Text("Grant permissions")
                }
                OutlinedButton(onClick = { openAppSettings(ctx) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open app settings")
                }
            } else {
                Button(onClick = { openAppSettings(ctx) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open app settings")
                }
            }
        }
    }
}

@Composable
private fun StatusLine(ok: Boolean, label: String) {
    Text(
        (if (ok) "✓  " else "✗  ") + label,
        style = MaterialTheme.typography.bodyMedium,
        color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
}
