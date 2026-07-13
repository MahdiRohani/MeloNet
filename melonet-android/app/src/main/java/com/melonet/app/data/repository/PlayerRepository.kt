package com.melonet.app.data.repository

import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.common.Result
import com.melonet.app.core.network.safeApiCall
import com.melonet.app.data.mapper.SongMapper
import com.melonet.app.data.model.Song
import com.melonet.app.data.remote.CatalogApi
import com.melonet.app.data.remote.LibraryApi
import com.melonet.app.data.remote.dto.PlayEventRequestDto
import kotlinx.coroutines.withContext
import java.io.File

class PlayerRepository(
    private val catalogApi: CatalogApi,
    private val libraryApi: LibraryApi,
    private val offlineSongResolver: OfflineSongResolver,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun getSong(id: String): Result<Song> = withContext(dispatchers.io) {
        if (id.startsWith("local_")) {
            return@withContext Result.Error(
                com.melonet.app.core.common.AppError.Unknown("Local song must be played from queue"),
            )
        }
        when (val result = safeApiCall { catalogApi.getSong(id) }) {
            is Result.Success -> Result.Success(SongMapper.toModel(result.data))
            is Result.Error -> result
        }
    }

    suspend fun recordPlay(songId: String, durationSec: Int, source: String): Result<Unit> =
        withContext(dispatchers.io) {
            if (songId.startsWith("local_")) {
                return@withContext Result.Success(Unit)
            }
            when (
                val result = safeApiCall {
                    libraryApi.recordPlay(
                        id = songId,
                        request = PlayEventRequestDto(
                            durationPlayedSec = durationSec,
                            source = source,
                        ),
                    )
                }
            ) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> result
            }
        }

    suspend fun resolveAudioUri(song: Song): String {
        if (song.id.startsWith("local_") || song.category == "local") {
            return song.audioUrl
        }
        val localPath = offlineSongResolver.localPathFor(song.id)
        if (!localPath.isNullOrBlank() && File(localPath).exists()) {
            return localPath
        }
        return song.audioUrl
    }
}
