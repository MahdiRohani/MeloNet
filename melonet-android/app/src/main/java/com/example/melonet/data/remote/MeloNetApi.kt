package com.example.melonet.data.remote

import com.example.melonet.data.model.SongDto
import retrofit2.http.GET
import retrofit2.http.Query

interface MeloNetApi {

    @GET("api/songs")
    suspend fun getSongs(
        @Query("category") category: String? = null
    ): List<SongDto>


    @GET("api/search")
    suspend fun searchSongs(
        @Query("q") query: String
    ): List<SongDto>
}