package com.sopnilrawal.netmozitv

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val gridPadding = PaddingValues(start = Flick.ScreenPadding, end = Flick.ScreenPadding, top = 8.dp, bottom = 48.dp)

@Composable
private fun PosterGrid(items: List<Media>, actions: AppActions, modifier: Modifier = Modifier, state: androidx.compose.foundation.lazy.grid.LazyGridState = rememberLazyGridState(), firstFocus: FocusRequester? = null) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(180.dp),
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = gridPadding,
        horizontalArrangement = Arrangement.spacedBy(26.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        items(items.size, key = { items[it].key }) { i ->
            val m = items[i]
            val mod = if (i == 0 && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier
            PosterCard(m, onClick = { actions.details(m) }, width = 180.dp, showTitle = true, modifier = mod)
        }
    }
}

/** Search bar with a microphone button, "Trending now" below until something is typed or said. */
@Composable
fun SearchScreen(start: Screen.Search, actions: AppActions) {
    val ctx = LocalContext.current
    var query by rememberSaveable(start) { mutableStateOf(start.query) }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { query = it }
        }
    }
    val startVoice = {
        try {
            voice.launch(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a movie or TV show")
            )
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, R.string.voice_missing, Toast.LENGTH_SHORT).show()
        }
    }
    val trending by rememberLoad(Unit) { Tmdb.trendingAll() }
    var results by remember { mutableStateOf<List<Media>?>(null) }
    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = null
            return@LaunchedEffect
        }
        delay(450)
        results = withContext(Dispatchers.IO) { runCatching { Tmdb.search(query.trim()) }.getOrDefault(emptyList()) }
    }
    val micFocus = remember { FocusRequester() }
    LaunchedEffect(start) {
        runCatching { micFocus.requestFocus() }
        if (start.voice) startVoice()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = Flick.ScreenPadding, end = Flick.ScreenPadding, top = 36.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InputField(query, { query = it }, "Search movies & TV shows", Modifier.weight(1f), leading = true, imeAction = ImeAction.Search)
            FocusBox(onClick = startVoice, modifier = Modifier.size(62.dp).focusRequester(micFocus), shape = RoundedCornerShape(50)) { focused ->
                Box(Modifier.fillMaxSize().background(if (focused) Flick.Accent else Flick.SurfaceHi), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Mic, contentDescription = "Voice search", tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        }
        val list = results
        if (list == null) {
            SectionTitle("Trending now")
            when (val t = trending) {
                null -> Message(if (Tmdb.hasKey) "Loading…" else "Add your TMDB API key in Settings.")
                else -> PosterGrid(t, actions)
            }
        } else {
            SectionTitle("Results for “$query”")
            if (list.isEmpty()) Message("Nothing found. Try another title.") else PosterGrid(list, actions)
        }
    }
}

/** Grid for a genre, Hindi, trending, popular or top rated, loading more pages as you scroll. */
@Composable
fun BrowseScreen(s: Screen.Browse, actions: AppActions) {
    val items = remember(s) { mutableStateListOf<Media>() }
    var page by remember(s) { mutableIntStateOf(0) }
    var loading by remember(s) { mutableStateOf(false) }
    var done by remember(s) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val grid = rememberLazyGridState()
    val firstFocus = remember(s) { FocusRequester() }

    fun loadMore() {
        if (loading || done || page >= 20) return
        loading = true
        scope.launch {
            val next = page + 1
            val found = withContext(Dispatchers.IO) {
                runCatching {
                    when (s.source) {
                        Source.TRENDING -> Tmdb.trending(s.type, page = next)
                        Source.POPULAR -> Tmdb.popular(s.type, next)
                        Source.TOP_RATED -> Tmdb.topRated(s.type, next)
                        Source.DISCOVER -> Tmdb.discover(s.type, s.genre, s.language, next)
                    }
                }.getOrDefault(emptyList())
            }
            if (found.isEmpty()) done = true
            val known = items.map { it.key }.toHashSet()
            val wasEmpty = items.isEmpty()
            items.addAll(found.filter { it.key !in known })
            page = next
            loading = false
            if (wasEmpty && items.isNotEmpty()) {
                delay(50)
                runCatching { firstFocus.requestFocus() }
            }
        }
    }

    LaunchedEffect(s) { loadMore() }
    val nearEnd by remember(s) {
        derivedStateOf { (grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= items.size - 12 }
    }
    LaunchedEffect(nearEnd, items.size) { if (nearEnd && items.isNotEmpty()) loadMore() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = Flick.ScreenPadding, top = 36.dp), verticalAlignment = Alignment.Bottom) {
            Text(s.title.uppercase(), color = Flick.Accent, fontFamily = Flick.Condensed, fontSize = 52.sp)
            Spacer(Modifier.width(16.dp))
            Text(if (s.type == Tmdb.TV) "TV shows" else "Movies", color = Flick.TextDim, fontSize = 20.sp, modifier = Modifier.padding(bottom = 10.dp))
        }
        Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) {
            Message(if (loading || !done) "Loading…" else "Nothing to show here yet.")
        } else {
            PosterGrid(items, actions, state = grid, firstFocus = firstFocus)
        }
    }
}

@Composable
fun MyListScreen(actions: AppActions) {
    Column(Modifier.fillMaxSize()) {
        Text("MY LIST", color = Flick.Accent, fontFamily = Flick.Condensed, fontSize = 52.sp, modifier = Modifier.padding(start = Flick.ScreenPadding, top = 36.dp, bottom = 12.dp))
        if (actions.myList.isEmpty()) Message("Press + on any movie or show to add it here.")
        else PosterGrid(actions.myList.toList(), actions)
    }
}

/** Big backdrop with poster, details, Play / Stremio / Kodi / + buttons and "More like this". */
@Composable
fun DetailScreen(s: Screen.Detail, prefs: Prefs, actions: AppActions) {
    val ctx = LocalContext.current
    val full by rememberLoad(s) { Tmdb.details(s.media) }
    val recs by rememberLoad(s) { Tmdb.recommendations(s.media) }
    val m = full?.media ?: s.media
    val d = full?.details
    val playFocus = remember(s) { FocusRequester() }
    LaunchedEffect(s) {
        delay(100)
        runCatching { playFocus.requestFocus() }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 48.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(600.dp)) {
                AsyncImage(m.backdropUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Flick.Bg, 0.65f to Flick.Bg.copy(alpha = 0.45f), 1f to Color.Transparent)))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Flick.Bg)))
                Row(Modifier.align(Alignment.BottomStart).padding(start = Flick.ScreenPadding, bottom = 24.dp), verticalAlignment = Alignment.Bottom) {
                    AsyncImage(
                        m.posterUrl, contentDescription = m.title, contentScale = ContentScale.Crop,
                        modifier = Modifier.width(230.dp).aspectRatio(2f / 3f).background(Flick.Surface, RoundedCornerShape(12.dp)),
                    )
                    Spacer(Modifier.width(36.dp))
                    Column(Modifier.widthIn(max = 820.dp)) {
                        Text(m.title.uppercase(), color = Flick.Accent, fontFamily = Flick.Condensed, fontSize = 58.sp, lineHeight = 58.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(10.dp))
                        val meta = listOfNotNull(
                            m.year.ifEmpty { null },
                            if (m.rating > 0) "★ %.1f".format(m.rating) else null,
                            d?.runtime?.let { if (it >= 60) "${it / 60}h ${it % 60}m" else "${it}m" },
                            if (m.isTv) "Series" else "Movie",
                        )
                        Text(meta.joinToString("   •   "), color = Flick.Text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        if (!d?.genres.isNullOrEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(d!!.genres.joinToString("  ·  "), color = Flick.TextDim, fontSize = 17.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(m.overview, color = Flick.TextDim, fontSize = 18.sp, lineHeight = 26.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            PrimaryButton("Play", Icons.Filled.PlayArrow, onClick = { Players.play(ctx, prefs, m, d) }, modifier = Modifier.focusRequester(playFocus))
                            if (d?.imdbId != null) {
                                SecondaryButton(Icons.Filled.Tv, text = "Stremio", onClick = {
                                    if (!Players.stremio(ctx, m, d.imdbId)) Toast.makeText(ctx, R.string.stremio_missing, Toast.LENGTH_SHORT).show()
                                })
                            }
                            SecondaryButton(Icons.Filled.Tv, text = "Kodi", onClick = { Players.kodi(ctx, m, d) })
                            SecondaryButton(if (actions.inList(m)) Icons.Filled.Check else Icons.Filled.Add, text = "My List", onClick = { actions.toggleList(m) })
                        }
                    }
                }
            }
        }
        item { MediaRow("More like this", recs.orEmpty(), actions.details) }
    }
}

/** TMDB key, region, default player and Kodi connection. */
@Composable
fun SettingsScreen(prefs: Prefs, onSaved: () -> Unit) {
    val ctx = LocalContext.current
    val kodi = remember { Kodi.settings(ctx) }
    var key by remember { mutableStateOf(prefs.tmdbKey) }
    var region by remember { mutableStateOf(prefs.region) }
    var player by remember { mutableStateOf(prefs.player) }
    var port by remember { mutableStateOf(kodi.port) }
    var user by remember { mutableStateOf(kodi.user) }
    var pass by remember { mutableStateOf(kodi.pass) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Flick.ScreenPadding, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("SETTINGS", color = Flick.Accent, fontFamily = Flick.Condensed, fontSize = 52.sp)
        Label("TMDB API key", "Free at themoviedb.org → Settings → API (the API key or the Read Access Token).")
        InputField(key, { key = it }, "TMDB API key", Modifier.widthIn(max = 900.dp).fillMaxWidth())
        Label("Region", "Two letters, used for the Top 10 list, e.g. US or IN.")
        InputField(region, { region = it.take(2) }, "US", Modifier.width(160.dp))
        Label("Play with", "What the Play button opens.")
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Pill("Stremio", player == Prefs.PLAYER_STREMIO, onClick = { player = Prefs.PLAYER_STREMIO })
            Pill("Kodi (NetMozi)", player == Prefs.PLAYER_KODI, onClick = { player = Prefs.PLAYER_KODI })
        }
        Label("Kodi connection", "In Kodi: Settings → Services → Control → Allow remote control via HTTP.")
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            InputField(port, { port = it.filter(Char::isDigit) }, "8080", Modifier.width(160.dp))
            InputField(user, { user = it }, "Username", Modifier.width(260.dp))
            InputField(pass, { pass = it }, "Password", Modifier.width(260.dp), password = true)
        }
        Spacer(Modifier.height(10.dp))
        PrimaryButton("Save", Icons.Filled.Save, onClick = {
            prefs.tmdbKey = key
            prefs.region = region
            prefs.player = player
            prefs.applyToTmdb()
            Kodi.saveSettings(ctx, Kodi.Settings(port, user, pass))
            Toast.makeText(ctx, "Saved", Toast.LENGTH_SHORT).show()
            onSaved()
        })
    }
}

@Composable
private fun Label(title: String, hint: String) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(title, color = Flick.Text, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
        Text(hint, color = Flick.TextDim, fontSize = 16.sp)
    }
}

/** Rounded text field that shows a red outline when focused (opens the on-screen keyboard). */
@Composable
fun InputField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: Boolean = false,
    password: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        singleLine = true,
        interactionSource = source,
        textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
        cursorBrush = SolidColor(Flick.Accent),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Flick.Surface, RoundedCornerShape(50))
                    .border(if (focused) 3.dp else 1.dp, if (focused) Flick.Accent else Flick.Border, RoundedCornerShape(50))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Flick.TextDim, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(14.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, color = Flick.TextDim, fontSize = 20.sp)
                    inner()
                }
            }
        },
    )
}
