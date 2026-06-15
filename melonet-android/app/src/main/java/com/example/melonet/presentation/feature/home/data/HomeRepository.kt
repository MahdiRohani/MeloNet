package com.example.melonet.presentation.feature.home.data



import com.example.melonet.data.model.SongDto
import com.example.melonet.data.remote.MeloNetApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeRepository(private val api: MeloNetApi) {

    suspend fun getSongsByCategory(category: String): List<SongDto> {
        return withContext(Dispatchers.IO) {
            try {
                api.getSongs(category = category)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getAllSongs(): List<SongDto> {
        return withContext(Dispatchers.IO) {
            try {
                api.getSongs(category = null)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}