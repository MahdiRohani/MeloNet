package handler

import (
	"errors"
	"strconv"

	"melonet-backend/internal/http/response"
	"melonet-backend/internal/service"

	"github.com/gin-gonic/gin"
)

type CatalogHandler struct {
	catalog *service.CatalogService
}

func NewCatalogHandler(catalog *service.CatalogService) *CatalogHandler {
	return &CatalogHandler{catalog: catalog}
}

func (h *CatalogHandler) ListSongs(c *gin.Context) {
	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	songs, meta, err := h.catalog.ListSongs(c.Request.Context(), service.SongListQuery{
		Category: c.Query("category"),
		GenreID:  parseOptionalInt64Query(c, "genre_id"),
		ArtistID: parseOptionalInt64Query(c, "artist_id"),
		AlbumID:  parseOptionalInt64Query(c, "album_id"),
		Sort:     c.Query("sort"),
		Page:     page,
		Limit:    limit,
	})
	if err != nil {
		response.InternalError(c, "failed to list songs")
		return
	}
	response.OKWithMeta(c, songs, meta)
}

func (h *CatalogHandler) GetSong(c *gin.Context) {
	songID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid song id")
		return
	}

	song, err := h.catalog.GetSong(c.Request.Context(), songID)
	if err != nil {
		mapCatalogError(c, err)
		return
	}
	response.OK(c, song)
}

func (h *CatalogHandler) Popular(c *gin.Context) {
	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	songs, meta, err := h.catalog.Popular(c.Request.Context(), page, limit)
	if err != nil {
		response.InternalError(c, "failed to list popular songs")
		return
	}
	response.OKWithMeta(c, songs, meta)
}

func (h *CatalogHandler) Newest(c *gin.Context) {
	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	songs, meta, err := h.catalog.Newest(c.Request.Context(), page, limit)
	if err != nil {
		response.InternalError(c, "failed to list new songs")
		return
	}
	response.OKWithMeta(c, songs, meta)
}

func (h *CatalogHandler) Trending(c *gin.Context) {
	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	songs, meta, err := h.catalog.Trending(c.Request.Context(), page, limit)
	if err != nil {
		response.InternalError(c, "failed to list trending songs")
		return
	}
	response.OKWithMeta(c, songs, meta)
}

func (h *CatalogHandler) ListArtists(c *gin.Context) {
	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	artists, meta, err := h.catalog.ListArtists(c.Request.Context(), c.Query("q"), page, limit)
	if err != nil {
		response.InternalError(c, "failed to list artists")
		return
	}
	response.OKWithMeta(c, artists, meta)
}

func (h *CatalogHandler) GetArtist(c *gin.Context) {
	artistID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid artist id")
		return
	}

	artist, err := h.catalog.GetArtist(c.Request.Context(), artistID)
	if err != nil {
		mapCatalogError(c, err)
		return
	}
	response.OK(c, artist)
}

func (h *CatalogHandler) ListArtistSongs(c *gin.Context) {
	artistID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid artist id")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	songs, meta, err := h.catalog.ListArtistSongs(c.Request.Context(), artistID, page, limit)
	if err != nil {
		response.InternalError(c, "failed to list artist songs")
		return
	}
	response.OKWithMeta(c, songs, meta)
}

func (h *CatalogHandler) GetAlbum(c *gin.Context) {
	albumID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid album id")
		return
	}

	album, err := h.catalog.GetAlbum(c.Request.Context(), albumID)
	if err != nil {
		mapCatalogError(c, err)
		return
	}
	response.OK(c, album)
}

func (h *CatalogHandler) ListAlbumSongs(c *gin.Context) {
	albumID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid album id")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	songs, meta, err := h.catalog.ListAlbumSongs(c.Request.Context(), albumID, page, limit)
	if err != nil {
		response.InternalError(c, "failed to list album songs")
		return
	}
	response.OKWithMeta(c, songs, meta)
}

func (h *CatalogHandler) ListGenres(c *gin.Context) {
	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 50)
	genres, meta, err := h.catalog.ListGenres(c.Request.Context(), page, limit)
	if err != nil {
		response.InternalError(c, "failed to list genres")
		return
	}
	response.OKWithMeta(c, genres, meta)
}

func (h *CatalogHandler) GetGenre(c *gin.Context) {
	genreID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid genre id")
		return
	}

	genre, err := h.catalog.GetGenre(c.Request.Context(), genreID)
	if err != nil {
		mapCatalogError(c, err)
		return
	}
	response.OK(c, genre)
}

func (h *CatalogHandler) ListGenreSongs(c *gin.Context) {
	genreID, err := parsePathID(c, "id")
	if err != nil {
		response.BadRequest(c, "invalid_id", "invalid genre id")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	songs, meta, err := h.catalog.ListGenreSongs(c.Request.Context(), genreID, page, limit)
	if err != nil {
		response.InternalError(c, "failed to list genre songs")
		return
	}
	response.OKWithMeta(c, songs, meta)
}

type SearchHandler struct {
	search *service.SearchService
}

func NewSearchHandler(search *service.SearchService) *SearchHandler {
	return &SearchHandler{search: search}
}

func (h *SearchHandler) Search(c *gin.Context) {
	query := c.Query("q")
	if query == "" {
		response.BadRequest(c, "invalid_query", "search query is required")
		return
	}

	page, limit := service.ParsePagination(c.Query("page"), c.Query("limit"), 20)
	result, meta, err := h.search.Search(c.Request.Context(), query, c.Query("type"), page, limit)
	if err != nil {
		response.BadRequest(c, "invalid_query", err.Error())
		return
	}

	response.OKWithMeta(c, result, meta)
}

type HomeHandler struct {
	home *service.HomeService
}

func NewHomeHandler(home *service.HomeService) *HomeHandler {
	return &HomeHandler{home: home}
}

func (h *HomeHandler) Feed(c *gin.Context) {
	feed, err := h.home.Feed(c.Request.Context())
	if err != nil {
		response.InternalError(c, "failed to load home feed")
		return
	}
	response.OK(c, feed)
}

func parsePathID(c *gin.Context, key string) (int64, error) {
	value := c.Param(key)
	if value == "" {
		return 0, errors.New("missing path parameter")
	}
	return strconv.ParseInt(value, 10, 64)
}

func parseOptionalInt64Query(c *gin.Context, key string) *int64 {
	value := c.Query(key)
	if value == "" {
		return nil
	}
	parsed, err := strconv.ParseInt(value, 10, 64)
	if err != nil {
		return nil
	}
	return &parsed
}

func mapCatalogError(c *gin.Context, err error) {
	if errors.Is(err, service.ErrNotFound) {
		response.NotFound(c, "resource not found")
		return
	}
	response.InternalError(c, "catalog request failed")
}
