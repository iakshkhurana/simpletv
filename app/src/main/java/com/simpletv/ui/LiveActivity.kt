package com.simpletv.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simpletv.R
import com.simpletv.data.Category
import com.simpletv.data.Channel
import com.simpletv.data.Prefs
import com.simpletv.data.Repo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The "set-top box" screen. App opens straight into this.
 *
 *  Remote keys:
 *   0-9              type a channel number (tunes after 2 s or on OK)
 *   UP / CH+         next channel          DOWN / CH-   previous channel
 *   OK / LEFT/RIGHT  open channel list     (in list: LEFT/RIGHT = change category, OK = play, BACK = close)
 *   INFO / GUIDE     show channel info + EPG
 *   BACK / MENU      open the menu (Movies, Series, Settings, Exit)
 */
class LiveActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null

    private lateinit var infoBar: View
    private lateinit var chNum: TextView
    private lateinit var chName: TextView
    private lateinit var epgNow: TextView
    private lateinit var epgNext: TextView
    private lateinit var numEntry: TextView
    private lateinit var channelPanel: View
    private lateinit var panelTitle: TextView
    private lateinit var channelList: RecyclerView
    private lateinit var loading: TextView
    private lateinit var errorText: TextView
    private lateinit var adapter: ChannelAdapter

    private var allChannels: List<Channel> = emptyList()
    private var cats: List<Category> = emptyList()        // first entry = "All"
    private var catIndex = 0
    private var filtered: List<Channel> = emptyList()
    private var current: Channel? = null
    private var triedFallback = false

    private val handler = Handler(Looper.getMainLooper())
    private val hideInfoRunnable = Runnable { infoBar.visibility = View.GONE }
    private val hidePanelRunnable = Runnable { hidePanel() }
    private val commitDigitsRunnable = Runnable { tuneByNumber() }
    private var digits = ""
    private var epgJob: Job? = null

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    // ------------------------------------------------------------------ lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        prefs = Prefs(this)

        playerView = findViewById(R.id.playerView)
        infoBar = findViewById(R.id.infoBar)
        chNum = findViewById(R.id.chNum)
        chName = findViewById(R.id.chName)
        epgNow = findViewById(R.id.epgNow)
        epgNext = findViewById(R.id.epgNext)
        numEntry = findViewById(R.id.numEntry)
        channelPanel = findViewById(R.id.channelPanel)
        panelTitle = findViewById(R.id.panelTitle)
        channelList = findViewById(R.id.channelList)
        loading = findViewById(R.id.loading)
        errorText = findViewById(R.id.errorText)

        adapter = ChannelAdapter { ch ->
            hidePanel()
            tune(ch)
        }
        channelList.layoutManager = LinearLayoutManager(this)
        channelList.adapter = adapter

        loadChannels(force = false)
    }

    override fun onResume() {
        super.onResume()
        if (Repo.liveDirty) {
            Repo.liveDirty = false
            loadChannels(force = false)
        }
    }

    override fun onStart() {
        super.onStart()
        // Coming back from the menu / a movie: resume the live channel.
        current?.let { if (player == null) playUrl(Repo.api(this).liveUrl(it, prefs.useHls)) }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    // ------------------------------------------------------------------ data

    private fun loadChannels(force: Boolean) {
        loading.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val (channels, categories) = withContext(Dispatchers.IO) { Repo.loadLive(this@LiveActivity, force) }
                allChannels = channels
                cats = listOf(Category(ALL, getString(R.string.all_channels))) + categories
                loading.visibility = View.GONE
                if (allChannels.isEmpty()) {
                    showError(getString(R.string.no_channels))
                    return@launch
                }
                val startWith = allChannels.firstOrNull { it.id == prefs.lastChannelId } ?: allChannels.first()
                applyCategory(0)
                if (current?.id != startWith.id || player == null) tune(startWith)
            } catch (e: Exception) {
                loading.visibility = View.GONE
                showError(getString(R.string.load_error, e.message ?: ""))
            }
        }
    }

    private fun applyCategory(index: Int) {
        if (cats.isEmpty()) return
        catIndex = ((index % cats.size) + cats.size) % cats.size
        val cat = cats[catIndex]
        filtered = if (cat.id == ALL) allChannels else allChannels.filter { it.categoryId == cat.id }
        panelTitle.text = "${cat.name}  (${filtered.size})"
        adapter.submit(filtered, current?.id ?: -1)
    }

    // ------------------------------------------------------------------ player

    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }
        val p = ExoPlayer.Builder(this).build()
        p.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val ch = current ?: return
                if (!triedFallback) {
                    // .ts failed? try .m3u8 (and vice-versa) once before giving up.
                    triedFallback = true
                    playUrl(Repo.api(this@LiveActivity).liveUrl(ch, !prefs.useHls))
                } else {
                    showError(getString(R.string.stream_error, error.errorCodeName))
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) errorText.visibility = View.GONE
            }
        })
        playerView.player = p
        player = p
        return p
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        playerView.player = null
    }

    private fun playUrl(url: String) {
        val p = ensurePlayer()
        p.setMediaItem(MediaItem.fromUri(url))
        p.prepare()
        p.playWhenReady = true
    }

    private fun tune(ch: Channel) {
        current = ch
        triedFallback = false
        prefs.lastChannelId = ch.id
        errorText.visibility = View.GONE
        playUrl(Repo.api(this).liveUrl(ch, prefs.useHls))
        adapter.setSelected(ch.id)
        showInfo(ch)
        fetchEpg(ch)
    }

    private fun step(delta: Int) {
        val list = if (filtered.isEmpty()) allChannels else filtered
        if (list.isEmpty()) return
        val idx = list.indexOfFirst { it.id == current?.id }
        val next = if (idx < 0) list.first() else list[((idx + delta) % list.size + list.size) % list.size]
        tune(next)
    }

    // ------------------------------------------------------------------ info bar + EPG

    private fun showInfo(ch: Channel) {
        chNum.text = ch.num.toString()
        chName.text = ch.name
        epgNow.text = ""
        epgNext.text = ""
        infoBar.visibility = View.VISIBLE
        handler.removeCallbacks(hideInfoRunnable)
        handler.postDelayed(hideInfoRunnable, INFO_TIMEOUT_MS)
    }

    private fun fetchEpg(ch: Channel) {
        epgJob?.cancel()
        epgJob = lifecycleScope.launch {
            val list = try {
                withContext(Dispatchers.IO) { Repo.api(this@LiveActivity).shortEpg(ch.id, 2) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            if (current?.id != ch.id) return@launch
            list.getOrNull(0)?.let {
                epgNow.text = getString(R.string.epg_now, it.title, timeFmt.format(Date(it.start * 1000)), timeFmt.format(Date(it.end * 1000)))
            }
            list.getOrNull(1)?.let {
                epgNext.text = getString(R.string.epg_next, it.title, timeFmt.format(Date(it.start * 1000)))
            }
        }
    }

    private fun showError(msg: String) {
        errorText.text = msg
        errorText.visibility = View.VISIBLE
    }

    // ------------------------------------------------------------------ channel panel

    private fun showPanel() {
        if (allChannels.isEmpty()) return
        adapter.submit(filtered, current?.id ?: -1)
        channelPanel.visibility = View.VISIBLE
        focusCurrentOrFirst()
        restartPanelTimer()
    }

    private fun hidePanel() {
        channelPanel.visibility = View.GONE
        handler.removeCallbacks(hidePanelRunnable)
    }

    private fun restartPanelTimer() {
        handler.removeCallbacks(hidePanelRunnable)
        handler.postDelayed(hidePanelRunnable, PANEL_TIMEOUT_MS)
    }

    private fun focusCurrentOrFirst() {
        val pos = filtered.indexOfFirst { it.id == current?.id }.coerceAtLeast(0)
        channelList.scrollToPosition(pos)
        channelList.post {
            channelList.findViewHolderForAdapterPosition(pos)?.itemView?.requestFocus()
                ?: channelList.requestFocus()
        }
    }

    // ------------------------------------------------------------------ number entry

    private fun onDigit(d: Int) {
        digits += d
        numEntry.text = digits
        numEntry.visibility = View.VISIBLE
        handler.removeCallbacks(commitDigitsRunnable)
        if (digits.length >= 4) tuneByNumber() else handler.postDelayed(commitDigitsRunnable, DIGIT_TIMEOUT_MS)
    }

    private fun tuneByNumber() {
        handler.removeCallbacks(commitDigitsRunnable)
        val n = digits.toIntOrNull()
        digits = ""
        numEntry.visibility = View.GONE
        if (n == null) return
        val ch = allChannels.firstOrNull { it.num == n }
        if (ch != null) tune(ch) else Toast.makeText(this, getString(R.string.no_such_channel, n), Toast.LENGTH_SHORT).show()
    }

    // ------------------------------------------------------------------ remote keys

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Number keys always work, even while the list is open.
        if (keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
            hidePanel()
            onDigit(keyCode - KeyEvent.KEYCODE_0)
            return true
        }

        if (channelPanel.visibility == View.VISIBLE) {
            restartPanelTimer()
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> { hidePanel(); return true }
                KeyEvent.KEYCODE_DPAD_LEFT -> { applyCategory(catIndex - 1); focusCurrentOrFirst(); return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { applyCategory(catIndex + 1); focusCurrentOrFirst(); return true }
            }
            // UP/DOWN/OK are handled by the RecyclerView items themselves.
            return super.onKeyDown(keyCode, event)
        }

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP -> { step(+1); return true }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> { step(-1); return true }

            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (digits.isNotEmpty()) tuneByNumber() else showPanel()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> { showPanel(); return true }

            KeyEvent.KEYCODE_INFO, KeyEvent.KEYCODE_GUIDE -> {
                current?.let { showInfo(it); fetchEpg(it) }
                return true
            }

            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_MENU -> {
                if (digits.isNotEmpty()) {
                    digits = ""; numEntry.visibility = View.GONE
                } else {
                    startActivity(Intent(this, MenuActivity::class.java))
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private const val ALL = "ALL"
        private const val INFO_TIMEOUT_MS = 5_000L
        private const val PANEL_TIMEOUT_MS = 12_000L
        private const val DIGIT_TIMEOUT_MS = 2_000L
    }
}
