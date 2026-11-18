package com.sopnilrawal.netmozitv

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A movie or TV show from TMDB. */
data class Media(
    val id: Int,
    val type: String,
    val title: String,
    val overview: String,
    val poster: String?,
    val backdrop: String?,
    val year: String,
    val rating: Double,
) {
    val isTv: Boolean get() = type == Tmdb.TV
    val posterUrl: String? get() = poster?.let { "https://image.tmdb.org/t/p/w342$it" }
    val backdropUrl: String? get() = (backdrop ?: poster)?.let { "https://image.tmdb.org/t/p/w1280$it" }
    val key: String get() = "$type:$id"

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("media_type", type).put("title", title).put("overview", overview)
        .put("poster_path", poster).put("backdrop_path", backdrop).put("release_date", year).put("vote_average", rating)

    companion object {
        fun stub(type: String, id: Int) = Media(id, type, "", "", null, null, "", 0.0)

        fun fromJson(o: JSONObject, defaultType: String?): Media? {
            val type = o.optString("media_type").ifEmpty { defaultType ?: return null }
            if (type != Tmdb.MOVIE && type != Tmdb.TV) return null
            val title = o.optString("title").ifEmpty { o.optString("name") }
            if (title.isEmpty()) return null
            val date = o.optString("release_date").ifEmpty { o.optString("first_air_date") }
            return Media(
                id = o.optInt("id"),
                type = type,
                title = title,
                overview = o.optString("overview"),
                poster = o.str("poster_path"),
                backdrop = o.str("backdrop_path"),
                year = date.take(4),
                rating = o.optDouble("vote_average", 0.0),
            )
        }

        private fun JSONObject.str(name: String): String? =
            if (isNull(name)) null else optString(name).takeIf { it.isNotEmpty() }
    }
}

/** Extra information shown on the details page and used to start playback. */
data class Details(val runtime: Int?, val genres: List<String>, val imdbId: String?, val hungarianTitle: String?)

class Full(val media: Media, val details: Details)

/** Minimal TMDB v3 client. Accepts a v3 API key or a v4 read access token. */
object Tmdb {
    const val MOVIE = "movie"
    const val TV = "tv"

    @Volatile var apiKey: String = ""
    @Volatile var region: String = "US"

    val hasKey: Boolean get() = apiKey.isNotBlank()

    private fun get(path: String, params: Map<String, String> = emptyMap()): JSONObject {
        val key = apiKey.trim()
        if (key.isEmpty()) throw IOException("TMDB API key missing")
        val bearer = key.length > 40
        val all = linkedMapOf("language" to "en-US") + params + (if (bearer) emptyMap() else mapOf("api_key" to key))
        val query = all.entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
        val conn = URL("https://api.themoviedb.org/3$path?$query").openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        conn.setRequestProperty("Accept", "application/json")
        if (bearer) conn.setRequestProperty("Authorization", "Bearer $key")
        try {
            if (conn.responseCode != 200) throw IOException("TMDB HTTP ${conn.responseCode}")
            return JSONObject(conn.inputStream.bufferedReader().readText())
        } finally {
            conn.disconnect()
        }
    }

    private fun list(path: String, type: String?, params: Map<String, String> = emptyMap()): List<Media> {
        val results = get(path, params).optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).mapNotNull { Media.fromJson(results.getJSONObject(it), type) }
    }

    fun trending(type: String, window: String = "week", page: Int = 1): List<Media> =
        list("/trending/$type/$window", type, mapOf("page" to "$page"))

    fun trendingAll(page: Int = 1): List<Media> = list("/trending/all/week", null, mapOf("page" to "$page"))

    fun popular(type: String, page: Int = 1): List<Media> =
        list("/$type/popular", type, mapOf("page" to "$page", "region" to region))

    fun topRated(type: String, page: Int = 1): List<Media> =
        list("/$type/top_rated", type, mapOf("page" to "$page", "region" to region))

    fun discover(type: String, genre: Int? = null, language: String? = null, page: Int = 1): List<Media> {
        val params = mutableMapOf("page" to "$page", "sort_by" to "popularity.desc", "include_adult" to "false")
        if (genre != null) params["with_genres"] = "$genre"
        if (language != null) params["with_original_language"] = language
        return list("/discover/$type", type, params)
    }

    fun search(query: String): List<Media> =
        list("/search/multi", null, mapOf("query" to query, "include_adult" to "false"))

    fun recommendations(m: Media): List<Media> = list("/${m.type}/${m.id}/recommendations", m.type)

    fun details(m: Media): Full {
        val o = get("/${m.type}/${m.id}", mapOf("append_to_response" to "external_ids,translations"))
        val media = Media.fromJson(o, m.type) ?: m
        val runtime = if (m.type == TV) {
            o.optJSONArray("episode_run_time")?.optInt(0)?.takeIf { it > 0 }
        } else {
            o.optInt("runtime").takeIf { it > 0 }
        }
        val genres = o.optJSONArray("genres")?.let { a -> (0 until a.length()).map { a.getJSONObject(it).optString("name") } }.orEmpty()
        val imdb = (o.optJSONObject("external_ids")?.optString("imdb_id") ?: o.optString("imdb_id")).takeIf { it.startsWith("tt") }
        return Full(media, Details(runtime, genres, imdb, translatedTitle(o, "hu")))
    }

    private fun translatedTitle(o: JSONObject, language: String): String? {
        val list = o.optJSONObject("translations")?.optJSONArray("translations") ?: return null
        for (i in 0 until list.length()) {
            val t = list.getJSONObject(i)
            if (t.optString("iso_639_1") != language) continue
            val data = t.optJSONObject("data") ?: continue
            val title = data.optString("title").ifEmpty { data.optString("name") }
            if (title.isNotEmpty()) return title
        }
        return null
    }
}
