package com.simpletv.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.simpletv.R
import com.simpletv.data.Prefs

/** Full-screen player for Movies and Episodes, with on-screen controls and resume support. */
class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
    }

    private lateinit var playerView: PlayerView
    private lateinit var titleView: TextView
    private lateinit var prefs: Prefs
    private var player: ExoPlayer? = null
    private var url: String = ""
    private val handler = Handler(Looper.getMainLooper())

    private val posKey get() = "pos_" + url.hashCode()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        prefs = Prefs(this)

        url = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
        playerView = findViewById(R.id.playerView)
        titleView = findViewById(R.id.playerTitle)
        titleView.text = intent.getStringExtra(EXTRA_TITLE) ?: ""
        handler.postDelayed({ titleView.visibility = View.GONE }, 4_000)
    }

    override fun onStart() {
        super.onStart()
        val p = ExoPlayer.Builder(this).build()
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    prefs.remove(posKey)
                    finish()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(this@PlayerActivity, getString(R.string.stream_error, error.errorCodeName), Toast.LENGTH_LONG).show()
            }
        })
        playerView.player = p
        p.setMediaItem(MediaItem.fromUri(url))
        val saved = prefs.getLong(posKey)
        if (saved > 0) p.seekTo(saved)
        p.prepare()
        p.playWhenReady = true
        player = p
        playerView.requestFocus()
    }

    override fun onStop() {
        super.onStop()
        player?.let {
            // Remember where we were, unless the movie is basically finished.
            val dur = it.duration
            if (dur > 0 && it.currentPosition < dur - 30_000) prefs.putLong(posKey, it.currentPosition)
            else prefs.remove(posKey)
            it.release()
        }
        player = null
        playerView.player = null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && playerView.isControllerFullyVisible) {
            playerView.hideController()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
