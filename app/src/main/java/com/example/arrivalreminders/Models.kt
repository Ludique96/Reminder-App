package com.example.arrivalreminders

import java.util.UUID

enum class PlaceType { WIFI, LOCATION }

/** A place that can trigger reminders: either a Wi-Fi network or a map location. */
data class Place(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: PlaceType,
    val ssid: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Float = 150f,
)

/** A to-do item. If placeId is set, it is shown in a notification when you arrive at that place. */
data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val placeId: String? = null,
    val done: Boolean = false,
)
