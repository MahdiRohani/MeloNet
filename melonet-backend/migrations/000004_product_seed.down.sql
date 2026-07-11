DELETE FROM notifications;
DELETE FROM message_receipts;
DELETE FROM messages;
DELETE FROM conversation_members;
DELETE FROM conversations;
DELETE FROM download_entitlements;
DELETE FROM play_history;
DELETE FROM follows;
DELETE FROM likes;
DELETE FROM playlist_songs;
DELETE FROM playlists WHERE title IN ('MeloNet Favorites', 'Global Hits');

UPDATE songs SET album_id = NULL WHERE album_id IN (
    SELECT id FROM albums WHERE slug LIKE '%-singles'
);

DELETE FROM albums WHERE slug LIKE '%-singles';

UPDATE songs SET play_count = 0 WHERE play_count > 0;
