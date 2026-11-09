package com.simpletv.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.simpletv.R
import com.simpletv.data.Prefs
import com.simpletv.data.Repo
import com.simpletv.data.XtreamApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/** Xtream login screen: server URL + username + password (same fields as XCIPTV). */
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val prefs = Prefs(this)
        val etServer = findViewById<EditText>(R.id.etServer)
        val etUser = findViewById<EditText>(R.id.etUser)
        val etPass = findViewById<EditText>(R.id.etPass)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val status = findViewById<TextView>(R.id.tvStatus)

        etServer.setText(prefs.server)
        etUser.setText(prefs.username)
        etPass.setText(prefs.password)
        etServer.requestFocus()

        btnLogin.setOnClickListener {
            val rawServer = etServer.text.toString()
            var user = etUser.text.toString().trim()
            var pass = etPass.text.toString().trim()

            // If someone pasted a full M3U / get.php link, pull user & pass out of it.
            if (user.isEmpty() || pass.isEmpty()) {
                runCatching {
                    val u = Uri.parse(rawServer.trim())
                    if (user.isEmpty()) user = u.getQueryParameter("username") ?: ""
                    if (pass.isEmpty()) pass = u.getQueryParameter("password") ?: ""
                }
                etUser.setText(user); etPass.setText(pass)
            }

            val server = normalizeServer(rawServer)
            if (server.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                status.text = getString(R.string.fill_all_fields)
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            status.text = getString(R.string.connecting)

            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) { XtreamApi(server, user, pass).login() }
                    prefs.server = server
                    prefs.username = user
                    prefs.password = pass
                    prefs.lastChannelId = -1
                    Repo.clearCache(this@LoginActivity)
                    Repo.liveDirty = true
                    startActivity(Intent(this@LoginActivity, LiveActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    status.text = getString(R.string.login_failed, e.message ?: "unknown error")
                    btnLogin.isEnabled = true
                }
            }
        }
    }

    /** "host:8080", "http://host:8080/", or a full get.php link -> "http://host:8080" */
    private fun normalizeServer(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return ""
        if (!s.startsWith("http://", ignoreCase = true) && !s.startsWith("https://", ignoreCase = true)) {
            s = "http://$s"
        }
        return try {
            val u = URL(s)
            val port = if (u.port != -1) ":${u.port}" else ""
            "${u.protocol}://${u.host}$port"
        } catch (e: Exception) {
            s.trimEnd('/')
        }
    }
}
