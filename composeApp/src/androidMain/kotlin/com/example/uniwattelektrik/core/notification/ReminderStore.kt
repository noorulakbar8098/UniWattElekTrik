package com.example.uniwattelektrik.core.notification

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tiny SharedPreferences-backed persistence for scheduled reminders.
 *
 * Why not Room: a single JSON array in SharedPreferences is plenty for
 * <1000 reminders (well over what a user will ever have) and avoids a Room
 * dependency + KSP + migration ceremony for a 4-field entity.
 *
 * Read on:
 *  - boot complete           → rehydrate every alarm
 *  - schedule(id, …)         → upsert
 *  - cancel(id)              → remove
 *  - app process death       → next launch re-reads the store
 */
internal object ReminderStore {

    data class Entry(
        val id: String,
        val triggerAtMillis: Long,
        val title: String,
        val body: String,
        val routeKey: String?,
        val taskId: String?,
    )

    private const val PREFS = "uw.reminders"
    private const val KEY   = "entries"

    fun all(context: Context): List<Entry> {
        val raw = prefs(context).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) add(fromJson(arr.getJSONObject(i)))
            }
        }.getOrDefault(emptyList())
    }

    fun put(context: Context, entry: Entry) {
        val current = all(context).filter { it.id != entry.id } + entry
        write(context, current)
    }

    fun remove(context: Context, id: String) {
        val current = all(context).filter { it.id != id }
        write(context, current)
    }

    fun clear(context: Context) = write(context, emptyList())

    private fun write(context: Context, entries: List<Entry>) {
        val arr = JSONArray()
        entries.forEach { arr.put(toJson(it)) }
        prefs(context).edit().putString(KEY, arr.toString()).apply()
    }

    private fun toJson(e: Entry): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("t",  e.triggerAtMillis)
        put("ti", e.title)
        put("bo", e.body)
        e.routeKey?.let { put("rk", it) }
        e.taskId?.let   { put("tk", it) }
    }

    private fun fromJson(o: JSONObject) = Entry(
        id              = o.getString("id"),
        triggerAtMillis = o.getLong("t"),
        title           = o.getString("ti"),
        body            = o.getString("bo"),
        routeKey        = o.optString("rk", "").ifBlank { null },
        taskId          = o.optString("tk", "").ifBlank { null },
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

