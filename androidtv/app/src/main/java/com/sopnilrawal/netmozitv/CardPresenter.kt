package com.sopnilrawal.netmozitv

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide

/** A shortcut tile (voice search, search, settings) shown next to the posters. */
data class ActionTile(val id: Int, val label: Int, val image: Int)

/** Draws movie/series posters and action tiles as cards. */
class CardPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val card = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setInfoAreaBackgroundColor(ContextCompat.getColor(context, R.color.card_bg))
            val density = resources.displayMetrics.density
            setMainImageDimensions((160 * density).toInt(), (240 * density).toInt())
        }
        return ViewHolder(card)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val card = viewHolder.view as ImageCardView
        when (item) {
            is Title -> {
                card.titleText = item.name
                card.contentText = listOf(item.year, if (item.isSeries) card.context.getString(R.string.series_tag) else "")
                    .filter { it.isNotEmpty() }.joinToString(" • ")
                Glide.with(card.context).load(item.thumb).centerCrop().into(card.mainImageView)
            }
            is ActionTile -> {
                card.titleText = card.context.getString(item.label)
                card.contentText = null
                card.mainImage = ContextCompat.getDrawable(card.context, item.image)
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val card = viewHolder.view as ImageCardView
        Glide.with(card.context).clear(card.mainImageView)
        card.mainImage = null
    }
}
