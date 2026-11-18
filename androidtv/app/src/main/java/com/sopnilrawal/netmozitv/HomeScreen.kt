package com.sopnilrawal.netmozitv

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.Locale

/** A chip in the Genres grid: a TMDB genre, or an original language (Hindi). */
data class Genre(val label: String, val icon: ImageVector, val id: Int? = null, val language: String? = null, val subtitle: String? = null)

fun genresFor(type: String): List<Genre> = if (type == Tmdb.MOVIE) {
    listOf(
        Genre("Hindi Movies", Icons.Filled.Translate, language = "hi", subtitle = "हिन्दी"),
        Genre("Action", Icons.Filled.Whatshot, 28),
        Genre("Adventure", Icons.Filled.Explore, 12),
        Genre("Animation", Icons.Filled.Brush, 16),
        Genre("Comedy", Icons.Filled.Mood, 35),
        Genre("Crime", Icons.Filled.Gavel, 80),
        Genre("Documentary", Icons.Filled.VideoLibrary, 99),
        Genre("Drama", Icons.Filled.LocalMovies, 18),
        Genre("Family", Icons.Filled.Group, 10751),
        Genre("Fantasy", Icons.Filled.Stars, 14),
        Genre("History", Icons.Filled.AccountBalance, 36),
        Genre("Horror", Icons.Filled.NightsStay, 27),
        Genre("Music", Icons.Filled.MusicNote, 10402),
        Genre("Mystery", Icons.Filled.Fingerprint, 9648),
        Genre("Romance", Icons.Filled.Favorite, 10749),
        Genre("Sci-Fi", Icons.Filled.Memory, 878),
        Genre("Thriller", Icons.Filled.FlashOn, 53),
        Genre("War", Icons.Filled.Security, 10752),
        Genre("Western", Icons.Filled.Landscape, 37),
    )
} else {
    listOf(
        Genre("Hindi Shows", Icons.Filled.Translate, language = "hi", subtitle = "हिन्दी"),
        Genre("Action & Adventure", Icons.Filled.Whatshot, 10759),
        Genre("Animation", Icons.Filled.Brush, 16),
        Genre("Comedy", Icons.Filled.Mood, 35),
        Genre("Crime", Icons.Filled.Gavel, 80),
        Genre("Documentary", Icons.Filled.VideoLibrary, 99),
        Genre("Drama", Icons.Filled.LocalMovies, 18),
        Genre("Family", Icons.Filled.Group, 10751),
        Genre("Kids", Icons.Filled.ChildCare, 10762),
        Genre("Mystery", Icons.Filled.Fingerprint, 9648),
        Genre("Reality", Icons.Filled.Videocam, 10764),
        Genre("Sci-Fi & Fantasy", Icons.Filled.Memory, 10765),
        Genre("War & Politics", Icons.Filled.Public, 10768),
        Genre("Western", Icons.Filled.Landscape, 37),
    )
}

class HomeData(
    val hero: List<Media>,
    val top10: List<Media>,
    val trending: List<Media>,
    val hindi: List<Media>,
    val popular: List<Media>,
    val topRated: List<Media>,
) {
    val isEmpty: Boolean get() = hero.isEmpty() && trending.isEmpty() && popular.isEmpty()
}

suspend fun loadHome(type: String): HomeData = coroutineScope {
    fun load(block: () -> List<Media>) = async(Dispatchers.IO) { runCatching(block).getOrDefault(emptyList()) }
    val trending = load { Tmdb.trending(type) }
    val top10 = load { Tmdb.popular(type) }
    val hindi = load { Tmdb.discover(type, language = "hi") }
    val popular = load { Tmdb.trending(type, "day") }
    val topRated = load { Tmdb.topRated(type) }
    val t = trending.await()
    HomeData(
        hero = t.filter { it.backdrop != null }.take(6),
        top10 = top10.await().take(10),
        trending = t,
        hindi = hindi.await(),
        popular = popular.await(),
        topRated = topRated.await(),
    )
}

@Composable
fun HomeScreen(
    mode: String,
    onMode: (String) -> Unit,
    cache: MutableMap<String, HomeData>,
    actions: AppActions,
) {
    if (!Tmdb.hasKey) {
        NeedsKey(actions)
        return
    }
    var reload by remember { mutableIntStateOf(0) }
    val data by rememberLoad(mode, reload) { cache[mode] ?: loadHome(mode).also { if (!it.isEmpty) cache[mode] = it } }
    val home = data
    val playFocus = remember { FocusRequester() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 48.dp)) {
        item(key = "hero") {
            Hero(home?.hero.orEmpty(), mode, onMode, actions, playFocus)
        }
        when {
            home == null -> item { Message("Loading…") }
            home.isEmpty -> item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Message("Couldn't load from TMDB. Check the internet connection and the API key in Settings.")
                    PrimaryButton("Retry", Icons.Filled.PlayArrow, onClick = { cache.remove(mode); reload++ })
                }
            }
            else -> {
                item(key = "genres") { GenreGrid(mode, actions) }
                item(key = "top10") {
                    val country = Locale("", Tmdb.region).displayCountry.ifEmpty { Tmdb.region }
                    Top10Row(
                        "Top 10 ${if (mode == Tmdb.MOVIE) "Movies" else "Shows"} in $country", home.top10, actions,
                        onMore = { actions.open(Screen.Browse("Popular ${if (mode == Tmdb.MOVIE) "Movies" else "Shows"}", mode, Source.POPULAR)) },
                    )
                }
                item(key = "trending") {
                    MediaRow("Trending now", home.trending, actions.details, trailing = {
                        ViewMore { actions.open(Screen.Browse("Trending now", mode, Source.TRENDING)) }
                    })
                }
                item(key = "hindi") {
                    val label = if (mode == Tmdb.MOVIE) "Hindi Movies" else "Hindi Shows"
                    MediaRow(label, home.hindi, actions.details, landscape = true, trailing = {
                        ViewMore { actions.open(Screen.Browse(label, mode, Source.DISCOVER, language = "hi")) }
                    })
                }
                if (actions.myList.isNotEmpty()) item(key = "mylist") { MediaRow("My List", actions.myList.toList(), actions.details) }
                item(key = "popular") { MediaRow("Popular today", home.popular, actions.details, landscape = true) }
                item(key = "toprated") {
                    MediaRow("Top rated", home.topRated, actions.details, trailing = {
                        ViewMore { actions.open(Screen.Browse("Top rated", mode, Source.TOP_RATED)) }
                    })
                }
            }
        }
    }
    LaunchedEffect(home != null) {
        if (home != null && !home.isEmpty) runCatching { playFocus.requestFocus() }
    }
}

@Composable
private fun Hero(items: List<Media>, mode: String, onMode: (String) -> Unit, actions: AppActions, playFocus: FocusRequester) {
    var index by remember(items) { mutableIntStateOf(0) }
    var focusedInside by remember { mutableStateOf(false) }
    LaunchedEffect(items, focusedInside) {
        if (items.size > 1) while (true) {
            delay(8000)
            if (!focusedInside) index = (index + 1) % items.size
        }
    }
    val m = items.getOrNull(index)
    Box(Modifier.fillMaxWidth().height(500.dp)) {
        Crossfade(targetState = m, label = "hero") { current ->
            AsyncImage(current?.backdropUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Flick.Bg, 0.6f to Flick.Bg.copy(alpha = 0.2f), 1f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Flick.Bg)))

        Row(Modifier.align(Alignment.TopEnd).padding(top = 28.dp, end = Flick.ScreenPadding), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Pill("Movies", mode == Tmdb.MOVIE, onClick = { onMode(Tmdb.MOVIE) })
            Pill("TV", mode == Tmdb.TV, onClick = { onMode(Tmdb.TV) })
        }

        if (m != null) {
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = Flick.ScreenPadding, bottom = 30.dp)
                    .widthIn(max = 760.dp)
                    .onFocusChanged { focusedInside = it.hasFocus },
            ) {
                Text(
                    m.title.uppercase(), color = Flick.Accent, fontFamily = Flick.Condensed, fontSize = 64.sp, lineHeight = 62.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    listOfNotNull(m.year.ifEmpty { null }, if (m.rating > 0) "★ %.1f".format(m.rating) else null, if (m.isTv) "Series" else "Movie")
                        .joinToString("   •   "),
                    color = Flick.Text, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(10.dp))
                Text(m.overview, color = Flick.TextDim, fontSize = 17.sp, lineHeight = 24.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton("Play", Icons.Filled.PlayArrow, onClick = { actions.play(m) }, modifier = Modifier.focusRequester(playFocus))
                    SecondaryButton(if (actions.inList(m)) Icons.Filled.Check else Icons.Filled.Add, onClick = { actions.toggleList(m) })
                    SecondaryButton(Icons.Filled.Info, onClick = { actions.details(m) })
                }
            }
            Dots(items.size, index, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun GenreGrid(mode: String, actions: AppActions) {
    Column {
    SectionTitle("Genres")
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height(156.dp),
        contentPadding = PaddingValues(horizontal = Flick.ScreenPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(genresFor(mode), key = { it.label }) { g ->
            GenreChip(g) { actions.open(Screen.Browse(g.label, mode, Source.DISCOVER, genre = g.id, language = g.language)) }
        }
    }    }
}

@Composable
private fun GenreChip(g: Genre, onClick: () -> Unit) {
    val highlight = g.language != null
    FocusBox(onClick, Modifier.width(250.dp).height(62.dp), shape = RoundedCornerShape(12.dp), focusedScale = 1.04f, outline = false) { focused ->
        Row(
            Modifier
                .fillMaxSize()
                .background(
                    if (highlight) Brush.horizontalGradient(listOf(Flick.Accent.copy(alpha = 0.55f), Flick.Surface))
                    else Brush.horizontalGradient(listOf(Flick.Surface, Flick.Surface))
                )
                .border(if (focused) 3.dp else 1.dp, if (focused) Flick.Accent else Flick.Border, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Flick.Accent.copy(alpha = if (highlight) 0.9f else 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(g.icon, contentDescription = null, tint = if (highlight) Color.White else Flick.Accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(g.label, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (g.subtitle != null) Text(g.subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Top10Row(title: String, items: List<Media>, actions: AppActions, onMore: () -> Unit) {
    if (items.isEmpty()) return
    Column {
    SectionTitle(title, trailing = { ViewMore(onMore) })
    LazyRow(
        contentPadding = PaddingValues(horizontal = Flick.ScreenPadding, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(items, key = { _, m -> m.key }) { i, m ->
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${i + 1}",
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 170.sp,
                        lineHeight = 170.sp,
                        fontFamily = Flick.Condensed,
                        drawStyle = Stroke(width = 6f, join = StrokeJoin.Round),
                    ),
                    modifier = Modifier.offset(y = 28.dp),
                )
                PosterCard(m, onClick = { actions.details(m) }, width = 160.dp, modifier = Modifier.offset(x = (-18).dp))
            }
        }
    }    }
}

@Composable
private fun NeedsKey(actions: AppActions) {
    Column(
        Modifier.fillMaxSize().padding(Flick.ScreenPadding),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("WELCOME", color = Flick.Accent, fontFamily = Flick.Condensed, fontSize = 64.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Discover uses The Movie Database (TMDB) for posters, genres, Hindi movies and Top 10 lists.\n" +
                "Get a free API key at themoviedb.org → Settings → API, then enter it in Settings.",
            color = Flick.TextDim, fontSize = 20.sp, lineHeight = 30.sp,
        )
        Spacer(Modifier.height(28.dp))
        val focus = remember { FocusRequester() }
        PrimaryButton("Open Settings", Icons.Filled.Settings, onClick = { actions.open(Screen.Settings) }, modifier = Modifier.focusRequester(focus))
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    }
}
