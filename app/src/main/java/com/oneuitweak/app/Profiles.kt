package com.oneuitweak.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Profile(
    val name: String,
    val anim: Float,
    val font: Float,
    val nightMode: String,
    val autoRotate: Boolean,
    val timeoutSec: Int
)

object ProfileStore {
    private const val PREFS = "onetweak_profiles"
    private const val KEY = "list"

    fun load(context: Context): List<Profile> = runCatching {
        val arr = JSONArray(prefs(context).getString(KEY, "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Profile(
                name = o.getString("name"),
                anim = o.getDouble("anim").toFloat(),
                font = o.getDouble("font").toFloat(),
                nightMode = o.getString("night"),
                autoRotate = o.getBoolean("rotate"),
                timeoutSec = o.getInt("timeout")
            )
        }
    }.getOrDefault(emptyList())

    fun save(context: Context, list: List<Profile>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject()
                    .put("name", p.name)
                    .put("anim", p.anim.toDouble())
                    .put("font", p.font.toDouble())
                    .put("night", p.nightMode)
                    .put("rotate", p.autoRotate)
                    .put("timeout", p.timeoutSec)
            )
        }
        prefs(context).edit().putString(KEY, arr.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
