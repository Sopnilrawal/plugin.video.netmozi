package com.sopnilrawal.netmozitv

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder

/** A movie or series listed on netmozi.com. */
data class Title(
    val name: String,
    val year: String,
    val url: String,
    val thumb: String,
    val isSeries: Boolean,
) {
    /** The page of the NetMozi Kodi add-on that lists this title's sources (or seasons). */
    val kodiPath: String
        get() = "plugin://plugin.video.netmozi/?action=${if (isSeries) "series" else "movie"}&url=$url"
}

/** Scrapes the same listing pages as the Kodi add-on (resources/lib/indexers/navigator.py). */
object NetMozi {
    const val BASE = "https://netmozi.com/"
    const val TYPE_ALL = ""
    const val TYPE_MOVIES = "1"
    const val TYPE_SERIES = "2"

    private const val UA =
        "Mozilla/5.0 (Linux; Android 11; SHIELD Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

    private fun get(url: String): Document = Jsoup.connect(url).userAgent(UA).timeout(15000).get()

    /** Sort orders offered by the site, as (label, value). */
    fun orders(): List<Pair<String, String>> {
        val group = get(BASE).selectFirst("div.btn-group") ?: return emptyList()
        return group.select("label").mapNotNull { label ->
            val input = label.selectFirst("input.orderRadioInput") ?: return@mapNotNull null
            val name = label.text().trim()
            if (name.isEmpty()) null else name to input.attr("value")
        }
    }

    fun list(type: String, order: String = "1", search: String = "", page: Int = 1): List<Title> {
        val q = URLEncoder.encode(search, "UTF-8")
        return get("${BASE}?page=$page&type=$type&order=$order&search=$q")
            .select("div.col-sm-4.col_main")
            .mapNotNull(::parse)
    }

    private fun parse(e: Element): Title? = try {
        val nameEl = e.selectFirst("div.col_name")!!
        val small = nameEl.select("small")
        val isSeries = small.any { it.text().contains("sorozat") }
        small.remove()
        val cols = e.select("div.col-sm-6")
        val rows = cols[1].select("div.row")
        Title(
            name = nameEl.text().trim(),
            year = rows.firstOrNull()?.ownText()?.trim().orEmpty(),
            url = e.selectFirst("a.col_a")!!.attr("href"),
            thumb = cols[0].selectFirst("img")!!.absUrl("src"),
            isSeries = isSeries,
        )
    } catch (ex: Exception) {
        null
    }
}
