package com.simpletv.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simpletv.R
import com.simpletv.data.Episode
import com.simpletv.data.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Episode list for one series. */
class SeriesDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
    }

    private var seriesName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series_detail)

        val seriesId = intent.getIntExtra(EXTRA_ID, -1)
        seriesName = intent.getStringExtra(EXTRA_TITLE) ?: ""
        findViewById<TextView>(R.id.seriesTitle).text = seriesName

        val loading = findViewById<TextView>(R.id.loading)
        val list = findViewById<RecyclerView>(R.id.episodeList)
        list.layoutManager = LinearLayoutManager(this)
        val adapter = EpisodeAdapter { play(it) }
        list.adapter = adapter

        lifecycleScope.launch {
            try {
                val eps = withContext(Dispatchers.IO) { Repo.api(this@SeriesDetailActivity).seriesEpisodes(seriesId) }
                adapter.submit(eps)
                if (eps.isEmpty()) {
                    loading.text = getString(R.string.nothing_found)
                } else {
                    loading.visibility = View.GONE
                    list.post { list.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() }
                }
            } catch (e: Exception) {
                loading.text = getString(R.string.load_error, e.message ?: "")
            }
        }
    }

    private fun play(ep: Episode) {
        val title = "$seriesName  •  " + "S%02d E%02d".format(ep.season, ep.num) +
            if (ep.title.isNotBlank()) "  ${ep.title}" else ""
        startActivity(
            Intent(this, PlayerActivity::class.java)
                .putExtra(PlayerActivity.EXTRA_URL, Repo.api(this).episodeUrl(ep))
                .putExtra(PlayerActivity.EXTRA_TITLE, title)
        )
    }
}
