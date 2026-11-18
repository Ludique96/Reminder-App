package com.example.arrivalreminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** The Settings tab: appearance and how often reminders may repeat. */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    val ctx = LocalContext.current
    var cooldown by remember { mutableIntStateOf(AppSettings.cooldownMinutes(ctx)) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
    ) {
        SectionTitle("Appearance")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { onThemeChange(mode) },
                    label = { Text(mode.label) },
                )
            }
        }

        SectionTitle("Reminder frequency")
        Text(
            "After a reminder for a place, wait at least this long before reminding you about it again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppSettings.COOLDOWN_OPTIONS_MINUTES.forEach { minutes ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        cooldown = minutes
                        AppSettings.setCooldownMinutes(ctx, minutes)
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = cooldown == minutes,
                    onClick = {
                        cooldown = minutes
                        AppSettings.setCooldownMinutes(ctx, minutes)
                    },
                )
                Text(AppSettings.cooldownLabel(minutes))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}
