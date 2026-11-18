package com.example.arrivalreminders

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/** The to-do list, grouped by the place each item is attached to. */
@Composable
fun ItemsScreen(
    data: Storage.Data,
    onChange: (Storage.Data) -> Unit,
    onEdit: (TodoItem) -> Unit,
) {
    val groups = buildList<Pair<Place?, List<TodoItem>>> {
        data.places.forEach { p ->
            add(p to data.items.filter { it.placeId == p.id }.sortedBy { it.done })
        }
        add(
            null to data.items
                .filter { item -> data.places.none { it.id == item.placeId } }
                .sortedBy { it.done }
        )
    }.filter { it.second.isNotEmpty() }

    if (groups.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Nothing to remember yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tap Add item to create a reminder that shows up when you arrive somewhere.",
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        ) {
            groups.forEach { (place, list) ->
                item(key = "header-${place?.id ?: "none"}") { GroupHeader(place, list) }
                items(list, key = { it.id }) { todo ->
                    ItemRow(
                        item = todo,
                        onToggle = { checked ->
                            onChange(
                                data.copy(items = data.items.map {
                                    if (it.id == todo.id) it.copy(done = checked) else it
                                })
                            )
                        },
                        onClick = { onEdit(todo) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(place: Place?, list: List<TodoItem>) {
    Column(Modifier.padding(top = 16.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                place?.name ?: "No place",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp).weight(1f),
            )
            Text(
                "${list.count { !it.done }} open",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (place == null) {
            Text(
                "These won't notify you anywhere",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ItemRow(item: TodoItem, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.done, onCheckedChange = onToggle)
        Column(Modifier.weight(1f)) {
            Text(
                item.text,
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
                color = if (item.done) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (item.repeat) {
                Text(
                    "Repeats every arrival",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.Default.Edit,
            contentDescription = "Edit",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp).size(20.dp),
        )
    }
}

/** Add or edit one item. Pass initial = null to add. */
@Composable
fun ItemDialog(
    data: Storage.Data,
    initial: TodoItem?,
    onSave: (TodoItem) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("ui", Context.MODE_PRIVATE) }
    val lastPlace = prefs.getString("last_place", null)

    var text by remember { mutableStateOf(initial?.text ?: "") }
    var placeId by remember {
        mutableStateOf(
            if (initial != null) initial.placeId
            else lastPlace?.takeIf { id -> data.places.any { it.id == id } }
        )
    }
    var repeat by remember { mutableStateOf(initial?.repeat ?: false) }
    val focus = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New reminder" else "Edit reminder") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("What do you need to do?") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                Text("Remind me when I arrive at", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = placeId == null,
                        onClick = { placeId = null },
                        label = { Text("Nowhere") },
                    )
                    data.places.forEach { p ->
                        FilterChip(
                            selected = placeId == p.id,
                            onClick = { placeId = p.id },
                            label = { Text(p.name) },
                        )
                    }
                }
                if (data.places.isEmpty()) {
                    Text(
                        "Add a place on the Places tab to get arrival reminders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Repeat every time")
                        Text(
                            if (repeat) "Comes back on every arrival" else "Reminds you until you tick it off",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = repeat, onCheckedChange = { repeat = it })
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete reminder", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            LaunchedEffect(Unit) {
                if (initial == null) runCatching { focus.requestFocus() }
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = {
                    prefs.edit().putString("last_place", placeId).apply()
                    onSave(
                        (initial ?: TodoItem(text = "")).copy(
                            text = text.trim(),
                            placeId = placeId,
                            repeat = repeat,
                        )
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
