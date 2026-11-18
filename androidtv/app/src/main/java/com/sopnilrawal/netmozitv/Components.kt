package com.sopnilrawal.netmozitv

import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Flick {
    val Bg = Color(0xFF0E0E11)
    val Surface = Color(0xFF1B1B20)
    val SurfaceHi = Color(0xFF2A2A31)
    val Accent = Color(0xFFE50914)
    val Text = Color.White
    val TextDim = Color(0xFFB4B4BA)
    val Border = Color(0xFF3C3C44)
    val Condensed = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))
    val ScreenPadding = 48.dp
}

/** Loads a value off the main thread; null while loading or on failure. */
@Composable
fun <T> rememberLoad(vararg keys: Any?, loader: suspend () -> T): State<T?> =
    produceState<T?>(initialValue = null, *keys) {
        value = withContext(Dispatchers.IO) { runCatching { loader() }.getOrNull() }
    }

/** A D-pad focusable, clickable box that grows and gets a red outline when focused. */
@Composable
fun FocusBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    focusedScale: Float = 1.06f,
    outline: Boolean = true,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) focusedScale else 1f, label = "focusScale")
    var m = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
    if (outline && focused) m = m.border(3.dp, Flick.Accent, shape)
    Box(
        m.clickable(interactionSource = source, indication = null, onClick = onClick),
        content = { content(focused) },
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(start = Flick.ScreenPadding, end = Flick.ScreenPadding, top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).height(26.dp).background(Flick.Accent))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Flick.Text, fontFamily = Flick.Condensed, fontSize = 28.sp, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Pill shaped toggle, like the Movies / TV switch. */
@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusBox(onClick, modifier, shape = RoundedCornerShape(50), outline = false) { focused ->
        Box(
            Modifier
                .background(if (selected) Flick.Accent.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.35f))
                .border(2.dp, if (focused || selected) Flick.Accent else Color.White.copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(horizontal = 26.dp, vertical = 10.dp),
        ) {
            Text(text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** White "Play" style button. */
@Composable
fun PrimaryButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusBox(onClick, modifier, shape = RoundedCornerShape(8.dp)) { focused ->
        Row(
            Modifier
                .background(if (focused) Color.White else Color.White.copy(alpha = 0.88f))
                .padding(horizontal = 34.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, color = Color.Black, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Dark button with an icon and optional text (the "+" button). */
@Composable
fun SecondaryButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, text: String? = null) {
    FocusBox(onClick, modifier, shape = RoundedCornerShape(8.dp)) { focused ->
        Row(
            Modifier
                .background(if (focused) Flick.SurfaceHi else Color.White.copy(alpha = 0.16f))
                .padding(horizontal = if (text == null) 16.dp else 24.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = text, tint = Color.White, modifier = Modifier.size(26.dp))
            if (text != null) {
                Spacer(Modifier.width(10.dp))
                Text(text, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun PosterCard(m: Media, onClick: () -> Unit, modifier: Modifier = Modifier, width: Dp = 150.dp, showTitle: Boolean = false) {
    Column(modifier.width(width)) {
        FocusBox(onClick, Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
            Box(Modifier.fillMaxSize().background(Flick.Surface), contentAlignment = Alignment.Center) {
                Text(m.title, color = Flick.TextDim, fontSize = 15.sp, modifier = Modifier.padding(10.dp))
                AsyncImage(m.posterUrl, contentDescription = m.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        if (showTitle) {
            Text(
                m.title, color = Flick.Text, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp, start = 2.dp, end = 2.dp),
            )
        }
    }
}

/** Wide card with the backdrop image and the title on top. */
@Composable
fun LandscapeCard(m: Media, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusBox(onClick, modifier.width(320.dp).aspectRatio(16f / 9f)) {
        Box(Modifier.fillMaxSize().background(Flick.Surface)) {
            AsyncImage(m.backdropUrl, contentDescription = m.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.9f))))
            Text(
                m.title, color = Color.White, fontFamily = Flick.Condensed, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
            )
        }
    }
}

@Composable
fun MediaRow(title: String, items: List<Media>, onOpen: (Media) -> Unit, landscape: Boolean = false, trailing: (@Composable () -> Unit)? = null) {
    if (items.isEmpty()) return
    Column {
        SectionTitle(title, trailing = trailing)
        LazyRow(
            contentPadding = PaddingValues(horizontal = Flick.ScreenPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(items, key = { it.key }) { m ->
                if (landscape) LandscapeCard(m, onClick = { onOpen(m) }) else PosterCard(m, onClick = { onOpen(m) })
            }
        }
    }
}

/** "View More >" link at the end of a section title. */
@Composable
fun ViewMore(onClick: () -> Unit) {
    FocusBox(onClick, shape = RoundedCornerShape(6.dp), outline = false) { focused ->
        Text(
            "View More  ›", color = if (focused) Flick.Accent else Flick.TextDim, fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun Dots(count: Int, index: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            Box(
                Modifier
                    .height(8.dp)
                    .width(if (i == index) 26.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (i == index) Flick.Accent else Color.White.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
fun Message(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(Flick.ScreenPadding), contentAlignment = Alignment.Center) {
        Text(text, color = Flick.TextDim, fontSize = 20.sp)
    }
}
