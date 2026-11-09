package com.simpletv.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.simpletv.R
import com.simpletv.data.Prefs
import com.simpletv.data.Repo
import com.simpletv.data.XtreamApi.Companion.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val prefs = Prefs(this)

        val swAuto = findViewById<SwitchCompat>(R.id.swAutoStart)
        swAuto.isChecked = prefs.autoStart
        swAuto.setOnCheckedChangeListener { _, checked -> prefs.autoStart = checked }

        val swHls = findViewById<SwitchCompat>(R.id.swHls)
        swHls.isChecked = prefs.useHls
        swHls.setOnCheckedChangeListener { _, checked -> prefs.useHls = checked }

        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            Repo.clearCache(this)
            Repo.liveDirty = true
            Toast.makeText(this, getString(R.string.lists_refreshed), Toast.LENGTH_SHORT).show()
        }

        val accountInfo = findViewById<TextView>(R.id.accountInfo)
        findViewById<Button>(R.id.btnAccount).setOnClickListener {
            accountInfo.text = getString(R.string.connecting)
            lifecycleScope.launch {
                try {
                    val json = withContext(Dispatchers.IO) { Repo.api(this@SettingsActivity).login() }
                    val ui = json.optJSONObject("user_info")
                    val exp = ui?.str("exp_date")?.toLongOrNull()
                    val expText = if (exp == null || exp == 0L) getString(R.string.unlimited)
                    else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(exp * 1000))
                    accountInfo.text = getString(
                        R.string.account_info,
                        prefs.username,
                        ui?.str("status") ?: "",
                        expText,
                        ui?.str("active_cons") ?: "?",
                        ui?.str("max_connections") ?: "?",
                        prefs.server,
                    )
                } catch (e: Exception) {
                    accountInfo.text = getString(R.string.load_error, e.message ?: "")
                }
            }
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            prefs.clear()
            Repo.clearCache(this)
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        }

        swAuto.requestFocus()
    }
}
