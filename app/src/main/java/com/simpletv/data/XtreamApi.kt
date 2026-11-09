package com.simpletv.data

import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Minimal Xtream Codes API client – the same protocol XCIPTV / IPTV Smarters use.
 *
 *   {server}/player_api.php?username=U&password=P[&action=...]
 *   Live  : {server}/live/U/P/{stream_id}.ts   (or .m3u8)
 *   Movie : {server}/movie/U/P/{stream_id}.{ext}
 *   Series: {server}/series/U/P/{episode_id}.{ext}
 */
class XtreamApi(private val server: String, private val user: String, private val pass: String) {

    // ---------- HTTP ----------

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun apiUrl(action: String?, extra: String = ""): String {
        val sb = StringBuilder("$server/player_api.php?username=${enc(user)}&password=${enc(pass)}")
        if (action != null) sb.append("&action=").append(action)
        sb.append(extra)
        return sb.toString()
    }

    private fun get(url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "SimpleTV/1.0")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            return resp.body?.string() ?: throw IOException("Empty response")
        }
    }

    /** Raw JSON for an API action (cached by Repo). */
    fun raw(action: String, extra: String = ""): String = get(apiUrl(action, extra))

    /** Checks credentials. Returns the full JSON (user_info + server_info). */
    fun login(): JSONObject {
        val text = get(apiUrl(null))
        val json = try { JSONObject(text) } catch (e: Exception) { throw IOException("Server did not return JSON – check the URL") }
        val ui = json.optJSONObject("user_info") ?: throw IOException("Invalid response from server")
        if (ui.optInt("auth", 0) != 1) {
            throw IOException("Wrong username/password (${ui.str("status").ifBlank { "auth failed" }})")
        }
        return json
    }

    fun seriesEpisodes(seriesId: Int): List<Episode> =
        parseEpisodes(get(apiUrl("get_series_info", "&series_id=$seriesId")))

    fun shortEpg(streamId: Int, limit: Int = 2): List<EpgEntry> {
        val json = JSONObject(get(apiUrl("get_short_epg", "&stream_id=$streamId&limit=$limit")))
        val arr = json.optJSONArray("epg_listings") ?: return emptyList()
        val out = ArrayList<EpgEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out += EpgEntry(
                title = b64(o.str("title")),
                start = o.str("start_timestamp").toLongOrNull() ?: 0L,
                end = o.str("stop_timestamp").toLongOrNull() ?: 0L,
            )
        }
        return out
    }

    // ---------- Stream URLs ----------

    fun liveUrl(ch: Channel, hls: Boolean): String =
        if (hls) "$server/live/$user/$pass/${ch.id}.m3u8" else "$server/live/$user/$pass/${ch.id}.ts"

    fun vodUrl(card: Card): String = "$server/movie/$user/$pass/${card.id}.${card.ext.ifBlank { "mp4" }}"

    fun episodeUrl(ep: Episode): String = "$server/series/$user/$pass/${ep.id}.${ep.ext.ifBlank { "mp4" }}"

    companion object {

        private val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .build()

        /** optString() returns the text "null" for JSON null – this returns "" instead. */
        fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key, "")

        private fun b64(s: String): String = try {
            String(Base64.decode(s, Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: Exception) { s }

        // ---------- Parsers (static so cached JSON can be re-parsed) ----------

        fun parseCategories(json: String): List<Category> {
            val arr = JSONArray(json)
            val out = ArrayList<Category>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out += Category(o.str("category_id"), o.str("category_name"))
            }
            return out
        }

        fun parseChannels(json: String): List<Channel> {
            val arr = JSONArray(json)
            val out = ArrayList<Channel>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.str("stream_id").toIntOrNull() ?: continue
                out += Channel(
                    id = id,
                    num = o.str("num").toIntOrNull() ?: (i + 1),
                    name = o.str("name"),
                    logo = o.str("stream_icon"),
                    categoryId = o.str("category_id"),
                )
            }
            return out
        }

        fun parseVod(json: String): List<Card> {
            val arr = JSONArray(json)
            val out = ArrayList<Card>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.str("stream_id").toIntOrNull() ?: continue
                out += Card(
                    id = id,
                    title = o.str("name"),
                    poster = o.str("stream_icon"),
                    categoryId = o.str("category_id"),
                    ext = o.str("container_extension").ifBlank { "mp4" },
                )
            }
            return out
        }

        fun parseSeries(json: String): List<Card> {
            val arr = JSONArray(json)
            val out = ArrayList<Card>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.str("series_id").toIntOrNull() ?: continue
                out += Card(
                    id = id,
                    title = o.str("name"),
                    poster = o.str("cover"),
                    categoryId = o.str("category_id"),
                    ext = "",
                )
            }
            return out
        }

        /** "episodes" is usually an object keyed by season ("1": [...]) but some panels send an array. */
        fun parseEpisodes(json: String): List<Episode> {
            val root = JSONObject(json)
            val eps = root.opt("episodes") ?: return emptyList()
            val out = ArrayList<Episode>()

            fun add(o: JSONObject) {
                val id = o.str("id").toIntOrNull() ?: return
                out += Episode(
                    id = id,
                    season = o.str("season").toIntOrNull() ?: 0,
                    num = o.str("episode_num").toIntOrNull() ?: 0,
                    title = o.str("title"),
                    ext = o.str("container_extension").ifBlank { "mp4" },
                )
            }

            when (eps) {
                is JSONObject -> {
                    val keys = eps.keys()
                    while (keys.hasNext()) {
                        val arr = eps.optJSONArray(keys.next()) ?: continue
                        for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(it) }
                    }
                }
                is JSONArray -> {
                    for (i in 0 until eps.length()) {
                        val inner = eps.optJSONArray(i)
                        if (inner != null) {
                            for (j in 0 until inner.length()) inner.optJSONObject(j)?.let { add(it) }
                        } else {
                            eps.optJSONObject(i)?.let { add(it) }
                        }
                    }
                }
            }
            return out.sortedWith(compareBy({ it.season }, { it.num }))
        }
    }
}
