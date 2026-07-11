package com.melonet.app.data.mapper

import com.melonet.app.data.model.User
import com.melonet.app.data.remote.dto.UserDto

object UserMapper {

    fun toModel(dto: UserDto): User = User(
        id = dto.id,
        username = dto.username,
        displayName = dto.displayName,
        email = dto.email.orEmpty(),
        avatarUrl = dto.avatarUrl.orEmpty(),
        bio = dto.bio.orEmpty(),
        isPremium = dto.isPremium
    )
}
