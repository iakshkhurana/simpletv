package com.simpletv.data

import android.content.Context

/** Tiny wrapper around SharedPreferences for login + a few settings. */
class Prefs(ctx: Context) {

    private val sp = ctx.getSharedPreferences("simpletv", Context.MODE_PRIVATE)

    var server: String
        get() = sp.getString("server", "") ?: ""
        set(v) = sp.edit().putString("server", v.trim().trimEnd('/')).apply()

    var username: String
        get() = sp.getString("username", "") ?: ""
        set(v) = sp.edit().putString("username", v.trim()).apply()

    var password: String
        get() = sp.getString("password", "") ?: ""
        set(v) = sp.edit().putString("password", v.trim()).apply()

    /** stream_id of the channel that was playing last time; restored on startup. */
    var lastChannelId: Int
        get() = sp.getInt("last_channel", -1)
        set(v) = sp.edit().putInt("last_channel", v).apply()

    /** Launch Live TV automatically when the TV boots. */
    var autoStart: Boolean
        get() = sp.getBoolean("auto_start", true)
        set(v) = sp.edit().putBoolean("auto_start", v).apply()

    /** Use .m3u8 (HLS) live URLs instead of .ts – helps on some servers. */
    var useHls: Boolean
        get() = sp.getBoolean("use_hls", false)
        set(v) = sp.edit().putBoolean("use_hls", v).apply()

    val isLoggedIn: Boolean
        get() = server.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()

    // generic helpers (used for VOD resume positions)
    fun getLong(key: String, def: Long = 0L): Long = sp.getLong(key, def)
    fun putLong(key: String, value: Long) = sp.edit().putLong(key, value).apply()
    fun remove(key: String) = sp.edit().remove(key).apply()

    fun clear() = sp.edit().clear().apply()
}
