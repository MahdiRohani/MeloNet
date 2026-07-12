package postgres

import (
	"context"
	"errors"
	"fmt"
	"strings"

	"melonet-backend/internal/domain"
	"melonet-backend/internal/domain/db"

	"github.com/jackc/pgx/v5"
)

type PlaylistRepository struct {
	db *DB
}

func NewPlaylistRepository(db *DB) *PlaylistRepository {
	return &PlaylistRepository{db: db}
}

const playlistSelectColumns = `
	p.id,
	p.owner_id,
	p.title,
	p.description,
	p.visibility,
	p.cover_object_key,
	p.cover_url,
	p.is_system,
	p.created_at,
	p.updated_at,
	u.display_name AS owner_name,
	COALESCE(sc.song_count, 0) AS song_count
`

const playlistFromClause = `
	FROM playlists AS p
	INNER JOIN users AS u ON u.id = p.owner_id
	LEFT JOIN (
		SELECT playlist_id, COUNT(*)::int AS song_count
		FROM playlist_songs
		GROUP BY playlist_id
	) AS sc ON sc.playlist_id = p.id
`

type PlaylistScope string

const (
	PlaylistScopeMine    PlaylistScope = "mine"
	PlaylistScopeSystem  PlaylistScope = "system"
	PlaylistScopeFriends PlaylistScope = "friends"
	PlaylistScopeAll     PlaylistScope = "all"
)

func (r *PlaylistRepository) List(ctx context.Context, userID int64, scope PlaylistScope, page, limit int) ([]db.Playlist, int, error) {
	scope = normalizePlaylistScope(scope)
	offset := (page - 1) * limit

	whereClause, args := playlistScopeClause(scope, userID, 1)

	var total int
	countQuery := "SELECT COUNT(*) " + playlistFromClause + whereClause
	if err := r.db.Pool.QueryRow(ctx, countQuery, args...).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count playlists: %w", err)
	}

	listArgs := append(args, limit, offset)
	limitPos := len(args) + 1
	offsetPos := len(args) + 2
	listQuery := fmt.Sprintf(`
		SELECT%s%s%s
		ORDER BY p.is_system DESC, p.updated_at DESC, p.id ASC
		LIMIT $%d OFFSET $%d
	`, playlistSelectColumns, playlistFromClause, whereClause, limitPos, offsetPos)

	rows, err := r.db.Pool.Query(ctx, listQuery, listArgs...)
	if err != nil {
		return nil, 0, fmt.Errorf("list playlists: %w", err)
	}
	defer rows.Close()

	playlists, err := scanPlaylists(rows)
	if err != nil {
		return nil, 0, err
	}
	return playlists, total, nil
}

func (r *PlaylistRepository) ListPublicByOwner(ctx context.Context, ownerID int64, page, limit int) ([]db.Playlist, int, error) {
	offset := (page - 1) * limit

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*)
		FROM playlists
		WHERE owner_id = $1
		  AND visibility = 'public'::playlist_visibility
		  AND is_system = FALSE
	`, ownerID).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count public playlists: %w", err)
	}

	listQuery := fmt.Sprintf(`
		SELECT%s%s
		WHERE p.owner_id = $1
		  AND p.visibility = 'public'::playlist_visibility
		  AND p.is_system = FALSE
		ORDER BY p.updated_at DESC, p.id ASC
		LIMIT $2 OFFSET $3
	`, playlistSelectColumns, playlistFromClause)

	rows, err := r.db.Pool.Query(ctx, listQuery, ownerID, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("list public playlists: %w", err)
	}
	defer rows.Close()

	playlists, err := scanPlaylists(rows)
	if err != nil {
		return nil, 0, err
	}
	return playlists, total, nil
}

func (r *PlaylistRepository) GetByID(ctx context.Context, playlistID int64) (db.Playlist, error) {
	row := r.db.Pool.QueryRow(ctx, `
		SELECT`+playlistSelectColumns+ playlistFromClause+ `
		WHERE p.id = $1
	`, playlistID)

	playlist, err := scanPlaylist(row.Scan)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.Playlist{}, ErrNotFound
		}
		return db.Playlist{}, fmt.Errorf("get playlist: %w", err)
	}
	return playlist, nil
}

func (r *PlaylistRepository) Create(ctx context.Context, ownerID int64, title, description string, visibility domain.PlaylistVisibility) (db.Playlist, error) {
	var playlist db.Playlist
	err := r.db.Pool.QueryRow(ctx, `
		INSERT INTO playlists (owner_id, title, description, visibility)
		VALUES ($1, $2, $3, $4::playlist_visibility)
		RETURNING id, owner_id, title, description, visibility, cover_object_key, cover_url, is_system, created_at, updated_at
	`, ownerID, title, description, string(visibility)).Scan(
		&playlist.ID,
		&playlist.OwnerID,
		&playlist.Title,
		&playlist.Description,
		&playlist.Visibility,
		&playlist.CoverObjectKey,
		&playlist.CoverURL,
		&playlist.IsSystem,
		&playlist.CreatedAt,
		&playlist.UpdatedAt,
	)
	if err != nil {
		return db.Playlist{}, fmt.Errorf("create playlist: %w", err)
	}
	return playlist, nil
}

func (r *PlaylistRepository) Update(ctx context.Context, playlistID int64, title, description *string, visibility *domain.PlaylistVisibility) (db.Playlist, error) {
	setParts := make([]string, 0, 4)
	args := make([]any, 0, 5)
	argPos := 1

	if title != nil {
		setParts = append(setParts, fmt.Sprintf("title = $%d", argPos))
		args = append(args, *title)
		argPos++
	}
	if description != nil {
		setParts = append(setParts, fmt.Sprintf("description = $%d", argPos))
		args = append(args, *description)
		argPos++
	}
	if visibility != nil {
		setParts = append(setParts, fmt.Sprintf("visibility = $%d::playlist_visibility", argPos))
		args = append(args, string(*visibility))
		argPos++
	}

	if len(setParts) == 0 {
		return r.GetByID(ctx, playlistID)
	}

	setParts = append(setParts, "updated_at = NOW()")
	args = append(args, playlistID)

	query := fmt.Sprintf(`
		UPDATE playlists
		SET %s
		WHERE id = $%d AND is_system = FALSE
		RETURNING id, owner_id, title, description, visibility, cover_object_key, cover_url, is_system, created_at, updated_at
	`, strings.Join(setParts, ", "), argPos)

	var playlist db.Playlist
	err := r.db.Pool.QueryRow(ctx, query, args...).Scan(
		&playlist.ID,
		&playlist.OwnerID,
		&playlist.Title,
		&playlist.Description,
		&playlist.Visibility,
		&playlist.CoverObjectKey,
		&playlist.CoverURL,
		&playlist.IsSystem,
		&playlist.CreatedAt,
		&playlist.UpdatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.Playlist{}, ErrNotFound
		}
		return db.Playlist{}, fmt.Errorf("update playlist: %w", err)
	}
	return playlist, nil
}

func (r *PlaylistRepository) Delete(ctx context.Context, playlistID, ownerID int64) error {
	tag, err := r.db.Pool.Exec(ctx, `
		DELETE FROM playlists
		WHERE id = $1 AND owner_id = $2 AND is_system = FALSE
	`, playlistID, ownerID)
	if err != nil {
		return fmt.Errorf("delete playlist: %w", err)
	}
	if tag.RowsAffected() == 0 {
		return ErrNotFound
	}
	return nil
}

func (r *PlaylistRepository) ListSongs(ctx context.Context, playlistID int64, page, limit int) ([]CachedSong, int, error) {
	offset := (page - 1) * limit

	var total int
	if err := r.db.Pool.QueryRow(ctx, `
		SELECT COUNT(*)
		FROM playlist_songs
		WHERE playlist_id = $1
	`, playlistID).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count playlist songs: %w", err)
	}

	rows, err := r.db.Pool.Query(ctx, `
		SELECT`+cachedSongSelectColumns+`
		FROM playlist_songs AS ps
		INNER JOIN song_cache AS sc ON sc.audius_id = ps.song_id
		WHERE ps.playlist_id = $1
		ORDER BY ps.position ASC, ps.added_at ASC
		LIMIT $2 OFFSET $3
	`, playlistID, limit, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("list playlist songs: %w", err)
	}
	defer rows.Close()

	songs, err := scanCachedSongs(rows)
	if err != nil {
		return nil, 0, err
	}
	return songs, total, nil
}

func (r *PlaylistRepository) AddSong(ctx context.Context, playlistID int64, songID string) error {
	tx, err := r.db.Pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("begin tx: %w", err)
	}
	defer tx.Rollback(ctx)

	var isSystem bool
	err = tx.QueryRow(ctx, `
		SELECT is_system FROM playlists WHERE id = $1
	`, playlistID).Scan(&isSystem)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return ErrNotFound
		}
		return fmt.Errorf("get playlist: %w", err)
	}
	if isSystem {
		return ErrForbidden
	}

	var nextPosition int
	err = tx.QueryRow(ctx, `
		SELECT COALESCE(MAX(position), 0) + 1
		FROM playlist_songs
		WHERE playlist_id = $1
	`, playlistID).Scan(&nextPosition)
	if err != nil {
		return fmt.Errorf("next position: %w", err)
	}

	_, err = tx.Exec(ctx, `
		INSERT INTO playlist_songs (playlist_id, song_id, position)
		VALUES ($1, $2, $3)
		ON CONFLICT (playlist_id, song_id) DO NOTHING
	`, playlistID, songID, nextPosition)
	if err != nil {
		return fmt.Errorf("add playlist song: %w", err)
	}

	_, err = tx.Exec(ctx, `UPDATE playlists SET updated_at = NOW() WHERE id = $1`, playlistID)
	if err != nil {
		return fmt.Errorf("touch playlist: %w", err)
	}

	return tx.Commit(ctx)
}

func (r *PlaylistRepository) RemoveSong(ctx context.Context, playlistID int64, songID string) error {
	tx, err := r.db.Pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("begin tx: %w", err)
	}
	defer tx.Rollback(ctx)

	var isSystem bool
	err = tx.QueryRow(ctx, `
		SELECT is_system FROM playlists WHERE id = $1
	`, playlistID).Scan(&isSystem)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return ErrNotFound
		}
		return fmt.Errorf("get playlist: %w", err)
	}
	if isSystem {
		return ErrForbidden
	}

	tag, err := tx.Exec(ctx, `
		DELETE FROM playlist_songs
		WHERE playlist_id = $1 AND song_id = $2
	`, playlistID, songID)
	if err != nil {
		return fmt.Errorf("remove playlist song: %w", err)
	}
	if tag.RowsAffected() == 0 {
		return ErrNotFound
	}

	if err := r.compactPositions(ctx, tx, playlistID); err != nil {
		return err
	}

	_, err = tx.Exec(ctx, `UPDATE playlists SET updated_at = NOW() WHERE id = $1`, playlistID)
	if err != nil {
		return fmt.Errorf("touch playlist: %w", err)
	}

	return tx.Commit(ctx)
}

func (r *PlaylistRepository) ReorderSongs(ctx context.Context, playlistID int64, songIDs []string) error {
	if len(songIDs) == 0 {
		return fmt.Errorf("song_ids required")
	}

	tx, err := r.db.Pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("begin tx: %w", err)
	}
	defer tx.Rollback(ctx)

	var isSystem bool
	err = tx.QueryRow(ctx, `
		SELECT is_system FROM playlists WHERE id = $1
	`, playlistID).Scan(&isSystem)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return ErrNotFound
		}
		return fmt.Errorf("get playlist: %w", err)
	}
	if isSystem {
		return ErrForbidden
	}

	var existingCount int
	err = tx.QueryRow(ctx, `
		SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = $1
	`, playlistID).Scan(&existingCount)
	if err != nil {
		return fmt.Errorf("count playlist songs: %w", err)
	}
	if existingCount != len(songIDs) {
		return fmt.Errorf("song_ids must include all playlist songs")
	}

	for position, songID := range songIDs {
		tag, err := tx.Exec(ctx, `
			UPDATE playlist_songs
			SET position = $3
			WHERE playlist_id = $1 AND song_id = $2
		`, playlistID, songID, position+1)
		if err != nil {
			return fmt.Errorf("reorder song %d: %w", songID, err)
		}
		if tag.RowsAffected() == 0 {
			return ErrNotFound
		}
	}

	_, err = tx.Exec(ctx, `UPDATE playlists SET updated_at = NOW() WHERE id = $1`, playlistID)
	if err != nil {
		return fmt.Errorf("touch playlist: %w", err)
	}

	return tx.Commit(ctx)
}

func (r *PlaylistRepository) CanView(ctx context.Context, playlist db.Playlist, userID int64) (bool, error) {
	if playlist.IsSystem {
		return true, nil
	}
	if playlist.OwnerID == userID {
		return true, nil
	}
	if playlist.Visibility == domain.PlaylistPublic {
		return true, nil
	}
	return false, nil
}

func (r *PlaylistRepository) compactPositions(ctx context.Context, tx pgx.Tx, playlistID int64) error {
	rows, err := tx.Query(ctx, `
		SELECT song_id
		FROM playlist_songs
		WHERE playlist_id = $1
		ORDER BY position ASC, added_at ASC
	`, playlistID)
	if err != nil {
		return fmt.Errorf("list positions: %w", err)
	}
	defer rows.Close()

	position := 1
	for rows.Next() {
		var songID string
		if err := rows.Scan(&songID); err != nil {
			return fmt.Errorf("scan song id: %w", err)
		}
		if _, err := tx.Exec(ctx, `
			UPDATE playlist_songs SET position = $3 WHERE playlist_id = $1 AND song_id = $2
		`, playlistID, songID, position); err != nil {
			return fmt.Errorf("set position: %w", err)
		}
		position++
	}
	return rows.Err()
}

func normalizePlaylistScope(scope PlaylistScope) PlaylistScope {
	switch scope {
	case PlaylistScopeMine, PlaylistScopeSystem, PlaylistScopeFriends:
		return scope
	default:
		return PlaylistScopeAll
	}
}

func playlistScopeClause(scope PlaylistScope, userID int64, startArg int) (string, []any) {
	switch scope {
	case PlaylistScopeMine:
		return fmt.Sprintf(" WHERE p.owner_id = $%d AND p.is_system = FALSE", startArg), []any{userID}
	case PlaylistScopeSystem:
		return " WHERE p.is_system = TRUE", nil
	case PlaylistScopeFriends:
		return fmt.Sprintf(`
			WHERE p.visibility = 'public'::playlist_visibility
			  AND p.is_system = FALSE
			  AND p.owner_id <> $%d
			  AND EXISTS (
			      SELECT 1 FROM follows AS f
			      WHERE f.follower_id = $%d AND f.following_id = p.owner_id
			  )
		`, startArg, startArg), []any{userID}
	default:
		return fmt.Sprintf(`
			WHERE p.is_system = TRUE
			   OR p.owner_id = $%d
			   OR (
			       p.visibility = 'public'::playlist_visibility
			       AND EXISTS (
			           SELECT 1 FROM follows AS f
			           WHERE f.follower_id = $%d AND f.following_id = p.owner_id
			       )
			   )
		`, startArg, startArg), []any{userID}
	}
}

type playlistScanner interface {
	Next() bool
	Scan(dest ...any) error
	Err() error
}

func scanPlaylists(rows playlistScanner) ([]db.Playlist, error) {
	playlists := make([]db.Playlist, 0)
	for rows.Next() {
		playlist, err := scanPlaylist(rows.Scan)
		if err != nil {
			return nil, err
		}
		playlists = append(playlists, playlist)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate playlists: %w", err)
	}
	return playlists, nil
}

func scanPlaylist(scan func(dest ...any) error) (db.Playlist, error) {
	var playlist db.Playlist
	if err := scan(
		&playlist.ID,
		&playlist.OwnerID,
		&playlist.Title,
		&playlist.Description,
		&playlist.Visibility,
		&playlist.CoverObjectKey,
		&playlist.CoverURL,
		&playlist.IsSystem,
		&playlist.CreatedAt,
		&playlist.UpdatedAt,
		&playlist.OwnerName,
		&playlist.SongCount,
	); err != nil {
		return db.Playlist{}, err
	}
	return playlist, nil
}

var ErrForbidden = errors.New("forbidden")
