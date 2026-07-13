package com.melonet.app.data.remote

import com.melonet.app.data.remote.dto.LrcLibResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Client for LRCLIB (https://lrclib.net) - a free, key-less lyrics provider that
 * returns time-synced LRC lyrics, ideal for karaoke.
 */
interface LyricsApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("artist_name") artistName: String,
        @Query("track_name") trackName: String,
        @Query("duration") durationSec: Int?,
    ): LrcLibResponseDto

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("q") query: String,
    ): List<LrcLibResponseDto>
}
