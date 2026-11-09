package com.simpletv.data

data class Category(val id: String, val name: String)

data class Channel(
    val id: Int,            // stream_id
    val num: Int,           // channel number shown on screen (from Xtream "num")
    val name: String,
    val logo: String,
    val categoryId: String,
)

/** One movie (VOD) or one series – both are shown as a poster card. */
data class Card(
    val id: Int,            // stream_id (VOD) or series_id (Series)
    val title: String,
    val poster: String,
    val categoryId: String,
    val ext: String,        // container_extension for VOD, "" for series
)

data class Episode(
    val id: Int,
    val season: Int,
    val num: Int,
    val title: String,
    val ext: String,
)

data class EpgEntry(
    val title: String,
    val start: Long,        // epoch seconds
    val end: Long,
)
