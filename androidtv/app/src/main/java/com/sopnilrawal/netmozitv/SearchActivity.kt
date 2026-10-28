package com.sopnilrawal.netmozitv

import android.app.Activity
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : FragmentActivity() {
    private lateinit var fragment: NetMoziSearchFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragment = supportFragmentManager.findFragmentById(android.R.id.content) as? NetMoziSearchFragment
            ?: NetMoziSearchFragment().also {
                supportFragmentManager.beginTransaction().replace(android.R.id.content, it).commitNow()
            }
        if (savedInstanceState == null) handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent) {
        val query = intent.getStringExtra(SearchManager.QUERY)
        val voice = intent.getBooleanExtra(EXTRA_VOICE, false) || intent.data?.getQueryParameter("voice") == "1"
        when {
            !query.isNullOrBlank() -> fragment.setSearchQuery(query, true)
            voice -> fragment.view?.post { fragment.startRecognition() } ?: fragment.startVoiceOnStart()
        }
    }

    companion object {
        const val EXTRA_VOICE = "voice"
    }
}

/** Search screen with a microphone button; results are shown as poster rows. */
class NetMoziSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {
    private val rows = ArrayObjectAdapter(ListRowPresenter())
    private var job: Job? = null
    private var voiceOnStart = false

    // Uses the system voice input (Google on the Shield), so no microphone permission is needed.
    private val speech = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) setSearchQuery(result.data, true)
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSearchResultProvider(this)
        setOnItemViewClickedListener(OnItemViewClickedListener { _, item, _, _ ->
            if (item is Title) Kodi.open(requireContext(), item.kodiPath)
        })
        setSpeechRecognitionCallback {
            try {
                speech.launch(recognizerIntent)
            } catch (e: ActivityNotFoundException) {
                // No voice input on this device: the on-screen keyboard still works.
            }
        }
    }

    fun startVoiceOnStart() {
        voiceOnStart = true
    }

    override fun onStart() {
        super.onStart()
        if (voiceOnStart) {
            voiceOnStart = false
            view?.post { startRecognition() }
        }
    }

    override fun getResultsAdapter(): ObjectAdapter = rows

    override fun onQueryTextChange(newQuery: String): Boolean {
        search(newQuery, 700)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        search(query, 0)
        return true
    }

    private fun search(query: String, wait: Long) {
        job?.cancel()
        if (query.trim().length < 2) {
            rows.clear()
            return
        }
        job = lifecycleScope.launch {
            delay(wait)
            val found = withContext(Dispatchers.IO) {
                runCatching { NetMozi.list(NetMozi.TYPE_ALL, search = query.trim()) }.getOrDefault(emptyList())
            }
            rows.clear()
            val (series, movies) = found.partition { it.isSeries }
            listOf(R.string.movies to movies, R.string.series to series).forEachIndexed { i, (label, titles) ->
                if (titles.isNotEmpty()) {
                    val row = ArrayObjectAdapter(CardPresenter()).apply { titles.forEach { add(it) } }
                    rows.add(ListRow(HeaderItem(i.toLong(), getString(label)), row))
                }
            }
        }
    }
}
