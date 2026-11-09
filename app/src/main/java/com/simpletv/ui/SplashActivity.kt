package com.simpletv.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.simpletv.data.Prefs

/** No UI – just routes to Login (first time) or straight into Live TV. */
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = if (Prefs(this).isLoggedIn) LiveActivity::class.java else LoginActivity::class.java
        startActivity(Intent(this, target))
        finish()
    }
}
