DROP INDEX IF EXISTS idx_song_cache_cached_at;
DROP TABLE IF EXISTS song_cache;

ALTER TABLE messages ALTER COLUMN song_id TYPE INTEGER USING NULLIF(song_id, '')::INTEGER;
ALTER TABLE download_entitlements ALTER COLUMN song_id TYPE INTEGER USING song_id::INTEGER;
ALTER TABLE playlist_songs ALTER COLUMN song_id TYPE INTEGER USING song_id::INTEGER;
ALTER TABLE play_history ALTER COLUMN song_id TYPE INTEGER USING song_id::INTEGER;
ALTER TABLE likes ALTER COLUMN song_id TYPE INTEGER USING song_id::INTEGER;

ALTER TABLE download_entitlements
    ADD CONSTRAINT download_entitlements_song_id_fkey
    FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE;

ALTER TABLE playlist_songs
    ADD CONSTRAINT playlist_songs_song_id_fkey
    FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE;

ALTER TABLE play_history
    ADD CONSTRAINT play_history_song_id_fkey
    FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE;

ALTER TABLE likes
    ADD CONSTRAINT likes_song_id_fkey
    FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE;
