package com.simpletv.data

import android.content.Context
import java.io.File

/**
 * Loads lists from the Xtream API and caches the raw JSON on disk for 12 hours,
 * so the TV shows channels within a second on every startup.
 */
object Repo {

    private const val CACHE_TTL_MS = 12 * 60 * 60 * 1000L

    /** Set by Settings when lists were refreshed / login changed; LiveActivity reloads on resume. */
    @Volatile
    var liveDirty = false

    fun api(ctx: Context): XtreamApi {
        val p = Prefs(ctx)
        return XtreamApi(p.server, p.username, p.password)
    }

    // ---------- public loaders (call from a background thread) ----------

    fun loadLive(ctx: Context, force: Boolean): Pair<List<Channel>, List<Category>> {
        val api = api(ctx)
        val channels = cachedParse(ctx, "live_streams", force, { api.raw("get_live_streams") }) { XtreamApi.parseChannels(it) }
        val cats = cachedParse(ctx, "live_cats", force, { api.raw("get_live_categories") }) { XtreamApi.parseCategories(it) }
        return channels to cats
    }

    fun loadVod(ctx: Context, force: Boolean): Pair<List<Card>, List<Category>> {
        val api = api(ctx)
        val items = cachedParse(ctx, "vod_streams", force, { api.raw("get_vod_streams") }) { XtreamApi.parseVod(it) }
        val cats = cachedParse(ctx, "vod_cats", force, { api.raw("get_vod_categories") }) { XtreamApi.parseCategories(it) }
        return items to cats
    }

    fun loadSeries(ctx: Context, force: Boolean): Pair<List<Card>, List<Category>> {
        val api = api(ctx)
        val items = cachedParse(ctx, "series", force, { api.raw("get_series") }) { XtreamApi.parseSeries(it) }
        val cats = cachedParse(ctx, "series_cats", force, { api.raw("get_series_categories") }) { XtreamApi.parseCategories(it) }
        return items to cats
    }

    fun clearCache(ctx: Context) {
        ctx.cacheDir.listFiles()?.filter { it.name.startsWith("xtream_") }?.forEach { it.delete() }
    }

    // ---------- internals ----------

    private fun cacheFile(ctx: Context, key: String) = File(ctx.cacheDir, "xtream_$key.json")

    private fun <T> cachedParse(
        ctx: Context,
        key: String,
        force: Boolean,
        fetch: () -> String,
        parse: (String) -> T,
    ): T {
        val f = cacheFile(ctx, key)
        if (!force && f.exists() && System.currentTimeMillis() - f.lastModified() < CACHE_TTL_MS) {
            val cached = runCatching { f.readText() }.getOrNull()
            if (cached != null) {
                val parsed = runCatching { parse(cached) }.getOrNull()
                if (parsed != null) return parsed
                f.delete() // corrupt cache – fall through to network
            }
        }
        val fresh = fetch()
        val parsed = try {
            parse(fresh)
        } catch (e: Exception) {
            throw java.io.IOException("Server returned unexpected data (${fresh.take(60).replace('\n', ' ')})")
        }
        runCatching { f.writeText(fresh) }
        return parsed
    }
}
