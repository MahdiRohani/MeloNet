package com.melonet.app.data.mapper

import com.melonet.app.data.model.PublicUser
import com.melonet.app.data.model.SocialUser
import com.melonet.app.data.remote.dto.PublicUserDto
import com.melonet.app.data.remote.dto.SocialUserDto

object SocialMapper {
    fun toPublicUser(dto: PublicUserDto): PublicUser = PublicUser(
        id = dto.id,
        username = dto.username,
        displayName = dto.displayName,
        avatarUrl = dto.avatarUrl.orEmpty(),
        bio = dto.bio.orEmpty(),
        isPremium = dto.isPremium,
        followerCount = dto.followerCount,
        followingCount = dto.followingCount,
        isFollowing = dto.isFollowing,
        isSelf = dto.isSelf,
    )

    fun toSocialUser(dto: SocialUserDto): SocialUser = SocialUser(
        id = dto.id,
        username = dto.username,
        displayName = dto.displayName,
        avatarUrl = dto.avatarUrl.orEmpty(),
        isPremium = dto.isPremium,
    )
}
