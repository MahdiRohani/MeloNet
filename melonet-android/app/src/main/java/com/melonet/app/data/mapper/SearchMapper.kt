package com.melonet.app.data.mapper

import com.melonet.app.data.model.Artist
import com.melonet.app.data.model.SearchFilter
import com.melonet.app.data.model.SearchResultItem
import com.melonet.app.data.model.SearchUser
import com.melonet.app.data.model.Song
import com.melonet.app.data.remote.dto.ArtistDto
import com.melonet.app.data.remote.dto.SearchResponseDto
import com.melonet.app.data.remote.dto.SongDto
import com.melonet.app.data.remote.dto.UserSearchResultDto

object SearchMapper {

    fun toResultItems(dto: SearchResponseDto, filter: SearchFilter): List<SearchResultItem> {
        return when (filter) {
            SearchFilter.SONG -> dto.songs.orEmpty().map { SearchResultItem.SongItem(songToModel(it)) }
            SearchFilter.ARTIST -> dto.artists.orEmpty().map { SearchResultItem.ArtistItem(artistToModel(it)) }
            SearchFilter.USER -> dto.users.orEmpty().map { SearchResultItem.UserItem(userToModel(it)) }
            SearchFilter.ALL -> buildList {
                dto.songs.orEmpty().forEach { add(SearchResultItem.SongItem(songToModel(it))) }
                dto.artists.orEmpty().forEach { add(SearchResultItem.ArtistItem(artistToModel(it))) }
                dto.users.orEmpty().forEach { add(SearchResultItem.UserItem(userToModel(it))) }
            }
        }
    }

    private fun songToModel(dto: SongDto): Song = Song(
        id = dto.id,
        title = dto.title,
        artistName = dto.artistName ?: dto.artist.orEmpty(),
        coverUrl = dto.coverImageUrl ?: dto.coverUrl.orEmpty(),
        audioUrl = dto.audioUrl.orEmpty(),
        category = dto.category.orEmpty(),
        lyrics = dto.lyrics.orEmpty(),
        durationSec = dto.durationSec ?: 0,
        genre = dto.genre,
        albumTitle = dto.albumTitle,
    )

    private fun artistToModel(dto: ArtistDto): Artist = ArtistMapper.toModel(dto)

    private fun userToModel(dto: UserSearchResultDto): SearchUser = SearchUser(
        id = dto.id,
        username = dto.username,
        displayName = dto.displayName,
        avatarUrl = dto.avatarUrl.orEmpty(),
        isPremium = dto.isPremium ?: false,
    )
}
