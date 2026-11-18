package com.sopnilrawal.netmozitv

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

/** Starts playback in Stremio (by IMDb id) or in Kodi through the NetMozi add-on's search. */
object Players {
    fun play(ctx: Context, prefs: Prefs, m: Media, d: Details?) {
        if (prefs.player == Prefs.PLAYER_STREMIO) {
            if (d?.imdbId != null) {
                if (stremio(ctx, m, d.imdbId)) return
                Toast.makeText(ctx, R.string.stremio_missing, Toast.LENGTH_SHORT).show()
            }
        }
        kodi(ctx, m, d)
    }

    fun stremio(ctx: Context, m: Media, imdbId: String): Boolean = try {
        val kind = if (m.isTv) "series" else "movie"
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("stremio:///detail/$kind/$imdbId")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

    fun kodi(ctx: Context, m: Media, d: Details?) {
        // NetMozi lists Hungarian titles, so search with the Hungarian title when TMDB has one.
        val title = d?.hungarianTitle ?: m.title
        val type = if (m.isTv) "2" else "1"
        Kodi.open(ctx, "plugin://plugin.video.netmozi/?action=movies&page=1&type=$type&order=1&search=${URLEncoder.encode(title, "UTF-8")}")
    }
}
