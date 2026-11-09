package com.simpletv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simpletv.data.Prefs
import com.simpletv.ui.LiveActivity

/**
 * Opens Live TV when the TV finishes booting (if enabled in Settings).
 * On Android 10+ the system may block this unless the app has the
 * "Display over other apps" permission – see README section "Auto-start".
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = Prefs(context)
        if (!prefs.autoStart || !prefs.isLoggedIn) return
        try {
            context.startActivity(
                Intent(context, LiveActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            // Blocked by the system – nothing we can do here.
        }
    }
}
