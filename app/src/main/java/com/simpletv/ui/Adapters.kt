package com.simpletv.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.simpletv.R
import com.simpletv.data.Card
import com.simpletv.data.Category
import com.simpletv.data.Channel
import com.simpletv.data.Episode

private fun ViewGroup.inflate(layout: Int): View = LayoutInflater.from(context).inflate(layout, this, false)

/** Grows the item slightly when the remote focuses it – makes D-pad navigation obvious. */
private fun View.scaleOnFocus(scale: Float = 1.06f) {
    setOnFocusChangeListener { v, focused ->
        v.animate().scaleX(if (focused) scale else 1f).scaleY(if (focused) scale else 1f).setDuration(120).start()
    }
}

// ---------------------------------------------------------------- Live TV channel list

class ChannelAdapter(private val onClick: (Channel) -> Unit) : RecyclerView.Adapter<ChannelAdapter.VH>() {

    private var items: List<Channel> = emptyList()
    private var selectedId = -1

    fun submit(list: List<Channel>, selected: Int) {
        items = list
        selectedId = selected
        notifyDataSetChanged()
    }

    fun setSelected(id: Int) {
        selectedId = id
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val num: TextView = v.findViewById(R.id.itemNum)
        val logo: ImageView = v.findViewById(R.id.itemLogo)
        val name: TextView = v.findViewById(R.id.itemName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent.inflate(R.layout.item_channel))
    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val ch = items[position]
        h.num.text = ch.num.toString()
        h.name.text = ch.name
        h.logo.load(ch.logo.ifBlank { null }) {
            placeholder(R.drawable.ic_tv)
            error(R.drawable.ic_tv)
            crossfade(false)
        }
        h.itemView.isSelected = ch.id == selectedId
        h.itemView.setOnClickListener { onClick(ch) }
    }
}

// ---------------------------------------------------------------- Category list (Movies / Series)

class CategoryAdapter(private val onClick: (Category) -> Unit) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    private var items: List<Category> = emptyList()
    private var selectedId = ""

    fun submit(list: List<Category>) {
        items = list
        selectedId = list.firstOrNull()?.id ?: ""
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.catName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent.inflate(R.layout.item_category))
    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val cat = items[position]
        h.name.text = cat.name
        h.itemView.isSelected = cat.id == selectedId
        h.itemView.setOnClickListener {
            selectedId = cat.id
            notifyDataSetChanged()
            onClick(cat)
        }
    }
}

// ---------------------------------------------------------------- Poster grid (Movies / Series)

class CardAdapter(private val onClick: (Card) -> Unit) : RecyclerView.Adapter<CardAdapter.VH>() {

    private var items: List<Card> = emptyList()

    fun submit(list: List<Card>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val poster: ImageView = v.findViewById(R.id.cardPoster)
        val title: TextView = v.findViewById(R.id.cardTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = parent.inflate(R.layout.item_card)
        v.scaleOnFocus()
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val card = items[position]
        h.title.text = card.title
        h.poster.load(card.poster.ifBlank { null }) {
            placeholder(R.drawable.ic_movie)
            error(R.drawable.ic_movie)
            crossfade(true)
        }
        h.itemView.setOnClickListener { onClick(card) }
    }
}

// ---------------------------------------------------------------- Episode list

class EpisodeAdapter(private val onClick: (Episode) -> Unit) : RecyclerView.Adapter<EpisodeAdapter.VH>() {

    private var items: List<Episode> = emptyList()

    fun submit(list: List<Episode>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.episodeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent.inflate(R.layout.item_episode))
    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val ep = items[position]
        val code = "S%02d E%02d".format(ep.season, ep.num)
        h.text.text = if (ep.title.isBlank()) code else "$code   ${ep.title}"
        h.itemView.setOnClickListener { onClick(ep) }
    }
}
