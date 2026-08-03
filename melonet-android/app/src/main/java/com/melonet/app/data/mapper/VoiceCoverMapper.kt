package com.melonet.app.data.mapper

import com.melonet.app.data.model.VoiceArtist
import com.melonet.app.data.model.VoiceCover
import com.melonet.app.data.remote.dto.VoiceArtistDto
import com.melonet.app.data.remote.dto.VoiceCoverDto

object VoiceCoverMapper {
    fun toArtist(dto: VoiceArtistDto): VoiceArtist = VoiceArtist(
        id = dto.id,
        slug = dto.slug,
        displayName = dto.displayName.orEmpty(),
        avatarUrl = dto.avatarUrl.orEmpty(),
        enabled = dto.enabled != false,
    )

    fun toCover(dto: VoiceCoverDto): VoiceCover = VoiceCover(
        id = dto.id,
        sourceSongId = dto.sourceSongId.orEmpty(),
        targetArtistId = dto.targetArtistId ?: 0L,
        targetArtistSlug = dto.targetArtistSlug.orEmpty(),
        targetArtistName = dto.targetArtistName.orEmpty(),
        status = dto.status.orEmpty().ifBlank { "pending" },
        audioUrl = dto.audioUrl.orEmpty(),
        coverUrl = dto.coverUrl.orEmpty(),
        sourceTitle = dto.sourceTitle.orEmpty(),
        sourceArtist = dto.sourceArtist.orEmpty(),
        error = dto.error.orEmpty(),
        progressPct = (dto.progressPct ?: 0).coerceIn(0, 100),
        progressStage = dto.progressStage.orEmpty(),
        etaSeconds = (dto.etaSeconds ?: 0).coerceAtLeast(0),
    )
}
