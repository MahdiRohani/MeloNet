package com.melonet.app.data.repository

import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.common.Result
import com.melonet.app.core.network.safeApiCall
import com.melonet.app.data.mapper.VoiceCoverMapper
import com.melonet.app.data.model.VoiceArtist
import com.melonet.app.data.model.VoiceCover
import com.melonet.app.data.remote.VoiceCoverApi
import com.melonet.app.data.remote.dto.CreateVoiceCoverRequestDto
import kotlinx.coroutines.withContext

class VoiceCoverRepository(
    private val api: VoiceCoverApi,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun listArtists(): Result<List<VoiceArtist>> = withContext(dispatchers.io) {
        when (val result = safeApiCall { api.listArtists() }) {
            is Result.Success -> Result.Success(result.data.map(VoiceCoverMapper::toArtist))
            is Result.Error -> result
        }
    }

    suspend fun listReady(page: Int = 1, limit: Int = 40): Result<List<VoiceCover>> =
        withContext(dispatchers.io) {
            when (val result = safeApiCall { api.listReady(page, limit) }) {
                is Result.Success -> Result.Success(result.data.map(VoiceCoverMapper::toCover))
                is Result.Error -> result
            }
        }

    suspend fun get(id: Long): Result<VoiceCover> = withContext(dispatchers.io) {
        when (val result = safeApiCall { api.get(id) }) {
            is Result.Success -> Result.Success(VoiceCoverMapper.toCover(result.data))
            is Result.Error -> result
        }
    }

    suspend fun create(sourceSongId: String, targetArtistSlug: String): Result<VoiceCover> =
        withContext(dispatchers.io) {
            when (
                val result = safeApiCall {
                    api.create(
                        CreateVoiceCoverRequestDto(
                            sourceSongId = sourceSongId,
                            targetArtistSlug = targetArtistSlug,
                        ),
                    )
                }
            ) {
                is Result.Success -> Result.Success(VoiceCoverMapper.toCover(result.data))
                is Result.Error -> result
            }
        }

    suspend fun delete(id: Long): Result<Unit> = withContext(dispatchers.io) {
        when (val result = safeApiCall { api.delete(id) }) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> result
        }
    }
}
