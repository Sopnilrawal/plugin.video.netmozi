package com.sopnilrawal.netmozitv

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns

/** Answers Android TV global (voice) search with matching TMDB titles that open in this app. */
class SuggestionProvider : ContentProvider() {
    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_TEXT_2,
                SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE,
                SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR,
                SearchManager.SUGGEST_COLUMN_CONTENT_TYPE,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA,
            )
        )
        val query = selectionArgs?.firstOrNull()?.trim() ?: uri.lastPathSegment?.trim().orEmpty()
        if (query.length < 2 || query == SearchManager.SUGGEST_URI_PATH_QUERY) return cursor
        Prefs(context!!).applyToTmdb()
        val titles = runCatching { Tmdb.search(query) }.getOrDefault(emptyList())
        titles.take(20).forEachIndexed { i, m ->
            cursor.addRow(
                arrayOf(
                    i,
                    m.title,
                    if (m.isTv) "TV show" else "Movie",
                    m.posterUrl,
                    m.year.toIntOrNull(),
                    "video/mp4",
                    "netmozitv://title?type=${m.type}&id=${m.id}",
                )
            )
        }
        return cursor
    }

    override fun getType(uri: Uri): String = SearchManager.SUGGEST_MIME_TYPE
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
}
