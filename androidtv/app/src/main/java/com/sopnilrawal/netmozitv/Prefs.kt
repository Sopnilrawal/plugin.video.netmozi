package com.sopnilrawal.netmozitv

import android.content.Context
import org.json.JSONArray
import java.util.Locale

/** App settings and the "My List" watchlist. */
class Prefs(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("app", Context.MODE_PRIVATE)

    var tmdbKey: String
        get() = p.getString("tmdb_key", null)?.takeIf { it.isNotBlank() } ?: BuildConfig.TMDB_API_KEY
        set(value) = p.edit().putString("tmdb_key", value.trim()).apply()

    var region: String
        get() = p.getString("region", null)?.takeIf { it.length == 2 } ?: Locale.getDefault().country.ifEmpty { "US" }
        set(value) = p.edit().putString("region", value.trim().uppercase()).apply()

    /** "stremio" or "kodi". */
    var player: String
        get() = p.getString("player", PLAYER_STREMIO)!!
        set(value) = p.edit().putString("player", value).apply()

    fun myList(): List<Media> {
        val arr = runCatching { JSONArray(p.getString("my_list", "[]")) }.getOrDefault(JSONArray())
        return (0 until arr.length()).mapNotNull { Media.fromJson(arr.getJSONObject(it), null) }
    }

    fun saveMyList(items: List<Media>) {
        val arr = JSONArray()
        items.forEach { arr.put(it.toJson()) }
        p.edit().putString("my_list", arr.toString()).apply()
    }

    /** Pushes the TMDB settings to the client. */
    fun applyToTmdb() {
        Tmdb.apiKey = tmdbKey
        Tmdb.region = region
    }

    companion object {
        const val PLAYER_STREMIO = "stremio"
        const val PLAYER_KODI = "kodi"
    }
}
