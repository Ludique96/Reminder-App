package com.example.arrivalreminders

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Tiny on-device JSON store. Everything stays on the phone. */
object Storage {
    data class Data(
        val places: List<Place> = emptyList(),
        val items: List<TodoItem> = emptyList(),
    )

    private const val FILE_NAME = "data.json"
    private val lock = Any()

    fun load(context: Context): Data = synchronized(lock) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return@synchronized Data()
        try {
            val root = JSONObject(file.readText())
            Data(
                places = root.optJSONArray("places").toObjects { placeFromJson(it) },
                items = root.optJSONArray("items").toObjects { itemFromJson(it) },
            )
        } catch (e: Exception) {
            Data()
        }
    }

    fun save(context: Context, data: Data) {
        synchronized(lock) {
            val root = JSONObject()
            root.put("places", JSONArray(data.places.map { placeToJson(it) }))
            root.put("items", JSONArray(data.items.map { itemToJson(it) }))
            val tmp = File(context.filesDir, "$FILE_NAME.tmp")
            tmp.writeText(root.toString())
            tmp.renameTo(File(context.filesDir, FILE_NAME))
        }
    }

    private fun <T> JSONArray?.toObjects(build: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return (0 until length()).map { build(getJSONObject(it)) }
    }

    private fun placeToJson(p: Place): JSONObject = JSONObject()
        .put("id", p.id)
        .put("name", p.name)
        .put("type", p.type.name)
        .put("ssid", p.ssid ?: JSONObject.NULL)
        .put("latitude", p.latitude ?: JSONObject.NULL)
        .put("longitude", p.longitude ?: JSONObject.NULL)
        .put("radius", p.radiusMeters.toDouble())

    private fun placeFromJson(o: JSONObject): Place = Place(
        id = o.getString("id"),
        name = o.getString("name"),
        type = PlaceType.valueOf(o.getString("type")),
        ssid = if (o.isNull("ssid")) null else o.getString("ssid"),
        latitude = if (o.isNull("latitude")) null else o.getDouble("latitude"),
        longitude = if (o.isNull("longitude")) null else o.getDouble("longitude"),
        radiusMeters = o.optDouble("radius", 150.0).toFloat(),
    )

    private fun itemToJson(i: TodoItem): JSONObject = JSONObject()
        .put("id", i.id)
        .put("text", i.text)
        .put("placeId", i.placeId ?: JSONObject.NULL)
        .put("done", i.done)
        .put("repeat", i.repeat)

    private fun itemFromJson(o: JSONObject): TodoItem = TodoItem(
        id = o.getString("id"),
        text = o.getString("text"),
        placeId = if (o.isNull("placeId")) null else o.getString("placeId"),
        done = o.optBoolean("done", false),
        repeat = o.optBoolean("repeat", false),
    )
}
