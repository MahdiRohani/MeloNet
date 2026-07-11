package com.melonet.app.feature.profile.data.mapper

import com.melonet.app.domain.model.User
import com.melonet.app.feature.profile.data.dto.UserDto

object UserMapper {

    fun toDomain(dto: UserDto): User = User(
        id = dto.id,
        username = dto.username,
        displayName = dto.displayName,
        email = dto.email.orEmpty(),
        avatarUrl = dto.avatarUrl.orEmpty(),
        bio = dto.bio.orEmpty(),
        isPremium = dto.isPremium
    )
}
