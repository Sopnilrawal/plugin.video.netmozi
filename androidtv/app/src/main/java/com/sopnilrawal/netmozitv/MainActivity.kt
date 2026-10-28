package com.sopnilrawal.netmozitv

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(android.R.id.content, DiscoverFragment()).commit()
        }
        ChannelSync.schedule(this)
    }
}

/** Store-like Discover screen: shortcut tiles, then rows of movie and series posters. */
class DiscoverFragment : BrowseSupportFragment() {
    private val rows = ArrayObjectAdapter(ListRowPresenter())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = ContextCompat.getColor(requireContext(), R.color.brand)
        searchAffordanceColor = ContextCompat.getColor(requireContext(), R.color.search_orb)
        setOnSearchClickedListener { openSearch(voice = false) }
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ -> onClick(item) }
        adapter = rows

        val start = ArrayObjectAdapter(CardPresenter()).apply {
            add(ActionTile(ACTION_VOICE, R.string.voice_search, R.drawable.tile_voice))
            add(ActionTile(ACTION_SEARCH, R.string.search, R.drawable.tile_search))
            add(ActionTile(ACTION_SETTINGS, R.string.kodi_settings, R.drawable.tile_kodi))
        }
        rows.add(ListRow(HeaderItem(0, getString(R.string.row_start)), start))
        load()
    }

    private fun load() = lifecycleScope.launch {
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val orders = NetMozi.orders().ifEmpty { listOf("" to "1") }.take(3)
                orders.flatMap { (orderName, order) ->
                    listOf(NetMozi.TYPE_MOVIES to R.string.movies, NetMozi.TYPE_SERIES to R.string.series).map { (type, label) ->
                        val header = if (orderName.isEmpty()) getString(label) else "${getString(label)}: $orderName"
                        header to runCatching { NetMozi.list(type, order) }.getOrDefault(emptyList())
                    }
                }
            }.getOrNull()
        }
        if (loaded == null) {
            Toast.makeText(requireContext(), R.string.load_failed, Toast.LENGTH_LONG).show()
            return@launch
        }
        loaded.filter { it.second.isNotEmpty() }.forEachIndexed { i, (header, titles) ->
            val row = ArrayObjectAdapter(CardPresenter()).apply { titles.forEach { add(it) } }
            rows.add(ListRow(HeaderItem(i + 1L, header), row))
        }
    }

    private fun onClick(item: Any?) {
        when (item) {
            is Title -> Kodi.open(requireContext(), item.kodiPath)
            is ActionTile -> when (item.id) {
                ACTION_VOICE -> openSearch(voice = true)
                ACTION_SEARCH -> openSearch(voice = false)
                ACTION_SETTINGS -> showKodiSettings()
            }
        }
    }

    private fun openSearch(voice: Boolean) {
        startActivity(Intent(requireContext(), SearchActivity::class.java).putExtra(SearchActivity.EXTRA_VOICE, voice))
    }

    private fun showKodiSettings() {
        val ctx = requireContext()
        val s = Kodi.settings(ctx)
        fun field(hint: Int, value: String, type: Int) = EditText(ctx).apply {
            setHint(hint)
            setText(value)
            inputType = type
        }
        val port = field(R.string.kodi_port, s.port, InputType.TYPE_CLASS_NUMBER)
        val user = field(R.string.kodi_user, s.user, InputType.TYPE_CLASS_TEXT)
        val pass = field(R.string.kodi_pass, s.pass, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val pad = (24 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(port)
            addView(user)
            addView(pass)
        }
        AlertDialog.Builder(ctx)
            .setTitle(R.string.kodi_settings)
            .setView(layout)
            .setPositiveButton(R.string.save) { _, _ ->
                Kodi.saveSettings(ctx, Kodi.Settings(port.text.toString(), user.text.toString(), pass.text.toString()))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        private const val ACTION_VOICE = 1
        private const val ACTION_SEARCH = 2
        private const val ACTION_SETTINGS = 3
    }
}
