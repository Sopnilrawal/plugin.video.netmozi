package com.sopnilrawal.netmozitv

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Source { TRENDING, POPULAR, TOP_RATED, DISCOVER }

sealed interface Screen {
    data object Home : Screen
    data class Search(val voice: Boolean = false, val query: String = "") : Screen
    data class Browse(val title: String, val type: String, val source: Source, val genre: Int? = null, val language: String? = null) : Screen
    data class Detail(val media: Media) : Screen
    data object MyList : Screen
    data object Settings : Screen
}

/** Everything a screen can ask the app to do. */
class AppActions(
    val open: (Screen) -> Unit,
    val play: (Media) -> Unit,
    val toggleList: (Media) -> Unit,
    val myList: SnapshotStateList<Media>,
) {
    val details: (Media) -> Unit = { open(Screen.Detail(it)) }
    fun inList(m: Media): Boolean = myList.any { it.key == m.key }
}

class MainActivity : ComponentActivity() {
    private val pending = mutableStateOf<Screen?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        prefs.applyToTmdb()
        if (savedInstanceState == null) pending.value = screenFor(intent)
        setContent { FlickApp(prefs, pending) }
        ChannelSync.schedule(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pending.value = screenFor(intent)
    }

    private fun screenFor(intent: Intent?): Screen? {
        if (intent == null) return null
        if (intent.action == Intent.ACTION_SEARCH) return Screen.Search(query = intent.getStringExtra(SearchManager.QUERY).orEmpty())
        val data = intent.data ?: return null
        if (data.scheme != "netmozitv") return null
        return when (data.host) {
            "search" -> Screen.Search(voice = data.getQueryParameter("voice") == "1")
            "title" -> {
                val id = data.getQueryParameter("id")?.toIntOrNull() ?: return null
                Screen.Detail(Media.stub(data.getQueryParameter("type") ?: Tmdb.MOVIE, id))
            }
            else -> null
        }
    }
}

@Composable
fun FlickApp(prefs: Prefs, pending: MutableState<Screen?>) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val myList = remember { mutableStateListOf<Media>().apply { addAll(prefs.myList()) } }
    val homeCache = remember { mutableStateMapOf<String, HomeData>() }
    var mode by rememberSaveable { mutableStateOf(Tmdb.MOVIE) }
    var keyVersion by remember { mutableStateOf(0) }

    val actions = remember {
        AppActions(
            open = { stack.add(it) },
            play = { m ->
                scope.launch {
                    val full = withContext(Dispatchers.IO) { runCatching { Tmdb.details(m) }.getOrNull() }
                    Players.play(ctx, prefs, full?.media ?: m, full?.details)
                }
            },
            toggleList = { m ->
                val i = myList.indexOfFirst { it.key == m.key }
                if (i >= 0) myList.removeAt(i) else myList.add(0, m)
                prefs.saveMyList(myList)
            },
            myList = myList,
        )
    }

    LaunchedEffect(pending.value) {
        pending.value?.let {
            stack.add(it)
            pending.value = null
        }
    }
    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

    Row(Modifier.fillMaxSize().background(Flick.Bg)) {
        Sidebar(stack.last()) { target ->
            stack.clear()
            stack.add(Screen.Home)
            if (target != Screen.Home) stack.add(target)
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            when (val s = stack.last()) {
                Screen.Home -> key(keyVersion) { HomeScreen(mode, { mode = it }, homeCache, actions) }
                is Screen.Search -> SearchScreen(s, actions)
                is Screen.Browse -> BrowseScreen(s, actions)
                is Screen.Detail -> DetailScreen(s, prefs, actions)
                Screen.MyList -> MyListScreen(actions)
                Screen.Settings -> SettingsScreen(prefs, onSaved = {
                    homeCache.clear()
                    keyVersion++
                    ChannelSync.schedule(ctx)
                    stack.clear()
                    stack.add(Screen.Home)
                })
            }
        }
    }
}

private data class NavItem(val screen: Screen, val icon: ImageVector, val label: String)

private val hindiScreen = Screen.Browse("Hindi Movies", Tmdb.MOVIE, Source.DISCOVER, language = "hi")

/** Left navigation rail: icons only, expanding to show labels when it has focus. */
@Composable
private fun Sidebar(current: Screen, onSelect: (Screen) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val width by animateDpAsState(if (expanded) 230.dp else 92.dp, label = "sidebar")
    val items = listOf(
        NavItem(Screen.Home, Icons.Filled.Home, "Home"),
        NavItem(Screen.Search(), Icons.Filled.Search, "Search"),
        NavItem(hindiScreen, Icons.Filled.Translate, "Hindi"),
        NavItem(Screen.MyList, Icons.Filled.Bookmark, "My List"),
        NavItem(Screen.Settings, Icons.Filled.Settings, "Settings"),
    )
    Column(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(Brush.horizontalGradient(listOf(Color.Black, Flick.Bg)))
            .onFocusChanged { expanded = it.hasFocus }
            .padding(horizontal = 16.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (expanded) "NETMOZI" else "N", color = Flick.Accent, fontFamily = Flick.Condensed, fontSize = 34.sp,
            modifier = Modifier.padding(start = 10.dp, bottom = 36.dp),
        )
        for (item in items) {
            val selected = when (item.screen) {
                is Screen.Search -> current is Screen.Search
                hindiScreen -> current is Screen.Browse && current.language == "hi"
                else -> current == item.screen
            }
            FocusBox(onClick = { onSelect(item.screen) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), focusedScale = 1f, outline = false) { focused ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                selected -> Flick.Accent
                                focused -> Flick.SurfaceHi
                                else -> Color.Transparent
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = if (selected || focused) Color.White else Flick.TextDim, modifier = Modifier.size(28.dp))
                    if (expanded) {
                        Spacer(Modifier.width(16.dp))
                        Text(item.label, color = Color.White, fontSize = 19.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
