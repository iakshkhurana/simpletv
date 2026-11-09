package com.simpletv.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.simpletv.R

/** The "hidden" menu behind the BACK button: Live TV, Movies, Series, Settings, Exit. */
class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val btnLive = findViewById<Button>(R.id.btnLive)
        btnLive.setOnClickListener { backToLive() }

        findViewById<Button>(R.id.btnMovies).setOnClickListener {
            startActivity(
                Intent(this, BrowseActivity::class.java)
                    .putExtra(BrowseActivity.EXTRA_MODE, BrowseActivity.MODE_VOD)
            )
        }
        findViewById<Button>(R.id.btnSeries).setOnClickListener {
            startActivity(
                Intent(this, BrowseActivity::class.java)
                    .putExtra(BrowseActivity.EXTRA_MODE, BrowseActivity.MODE_SERIES)
            )
        }
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btnExit).setOnClickListener { finishAffinity() }

        btnLive.requestFocus()
    }

    private fun backToLive() {
        // LiveActivity is singleTask – this brings the existing instance to the front.
        startActivity(Intent(this, LiveActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }
}
