package com.simpletv.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simpletv.R
import com.simpletv.data.Card
import com.simpletv.data.Category
import com.simpletv.data.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Movies (VOD) or Series browser: categories on the left, poster grid on the right. */
class BrowseActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_VOD = "vod"
        const val MODE_SERIES = "series"
        private const val ALL = "ALL"
        private const val COLUMNS = 5
    }

    private lateinit var mode: String
    private var allCards: List<Card> = emptyList()
    private lateinit var catAdapter: CategoryAdapter
    private lateinit var cardAdapter: CardAdapter
    private lateinit var loading: TextView
    private lateinit var errorText: TextView
    private lateinit var grid: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_VOD

        findViewById<TextView>(R.id.browseTitle).text =
            getString(if (mode == MODE_VOD) R.string.movies else R.string.series)
        loading = findViewById(R.id.loading)
        errorText = findViewById(R.id.errorText)

        val catList = findViewById<RecyclerView>(R.id.catList)
        catList.layoutManager = LinearLayoutManager(this)
        catAdapter = CategoryAdapter { filter(it) }
        catList.adapter = catAdapter

        grid = findViewById(R.id.grid)
        grid.layoutManager = GridLayoutManager(this, COLUMNS)
        cardAdapter = CardAdapter { open(it) }
        grid.adapter = cardAdapter

        load()
    }

    private fun load() {
        loading.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val (cards, cats) = withContext(Dispatchers.IO) {
                    if (mode == MODE_VOD) Repo.loadVod(this@BrowseActivity, false)
                    else Repo.loadSeries(this@BrowseActivity, false)
                }
                allCards = cards
                catAdapter.submit(listOf(Category(ALL, getString(R.string.all))) + cats)
                cardAdapter.submit(cards)
                loading.visibility = View.GONE
                if (cards.isEmpty()) {
                    errorText.text = getString(R.string.nothing_found)
                    errorText.visibility = View.VISIBLE
                } else {
                    grid.post { grid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() }
                }
            } catch (e: Exception) {
                loading.visibility = View.GONE
                errorText.text = getString(R.string.load_error, e.message ?: "")
                errorText.visibility = View.VISIBLE
            }
        }
    }

    private fun filter(cat: Category) {
        cardAdapter.submit(if (cat.id == ALL) allCards else allCards.filter { it.categoryId == cat.id })
        grid.scrollToPosition(0)
    }

    private fun open(card: Card) {
        if (mode == MODE_VOD) {
            startActivity(
                Intent(this, PlayerActivity::class.java)
                    .putExtra(PlayerActivity.EXTRA_URL, Repo.api(this).vodUrl(card))
                    .putExtra(PlayerActivity.EXTRA_TITLE, card.title)
            )
        } else {
            startActivity(
                Intent(this, SeriesDetailActivity::class.java)
                    .putExtra(SeriesDetailActivity.EXTRA_ID, card.id)
                    .putExtra(SeriesDetailActivity.EXTRA_TITLE, card.title)
            )
        }
    }
}
