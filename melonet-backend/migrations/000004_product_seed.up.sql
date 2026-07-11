-- Minimal product seed: albums, system playlists, social graph, chat, notifications.

UPDATE songs
SET play_count = id
WHERE play_count = 0;

INSERT INTO albums (artist_id, title, slug, cover_url, release_date)
SELECT
    a.id,
    format('%s - Singles', a.name),
    format('%s-singles', a.slug),
    format('/static/covers/cover%s.jpg', ((a.id - 1) % 50) + 1),
    CURRENT_DATE - ((a.id % 365) || ' days')::interval
FROM artists AS a
WHERE NOT EXISTS (
    SELECT 1 FROM albums AS al WHERE al.artist_id = a.id AND al.slug = format('%s-singles', a.slug)
);

UPDATE songs AS s
SET album_id = al.id
FROM albums AS al
WHERE al.artist_id = s.artist_id
  AND al.slug = format('%s-singles', (SELECT slug FROM artists WHERE id = s.artist_id))
  AND s.album_id IS NULL;

INSERT INTO playlists (owner_id, title, description, visibility, cover_url, is_system)
SELECT
    u.id,
    'MeloNet Favorites',
    'Curated starter playlist for local development.',
    'public'::playlist_visibility,
    '/static/covers/cover1.jpg',
    TRUE
FROM users AS u
WHERE u.username = 'mahdi'
  AND NOT EXISTS (
      SELECT 1 FROM playlists AS p WHERE p.owner_id = u.id AND p.title = 'MeloNet Favorites'
  );

INSERT INTO playlists (owner_id, title, description, visibility, cover_url, is_system)
SELECT
    u.id,
    'Global Hits',
    'Popular tracks across genres.',
    'public'::playlist_visibility,
    '/static/covers/cover2.jpg',
    TRUE
FROM users AS u
WHERE u.username = 'mahdi'
  AND NOT EXISTS (
      SELECT 1 FROM playlists AS p WHERE p.owner_id = u.id AND p.title = 'Global Hits'
  );

INSERT INTO playlist_songs (playlist_id, song_id, position)
SELECT p.id, s.id, ROW_NUMBER() OVER (PARTITION BY p.id ORDER BY s.id)
FROM playlists AS p
JOIN songs AS s ON s.id <= 10
WHERE p.title = 'MeloNet Favorites'
  AND NOT EXISTS (
      SELECT 1 FROM playlist_songs AS ps WHERE ps.playlist_id = p.id AND ps.song_id = s.id
  );

INSERT INTO playlist_songs (playlist_id, song_id, position)
SELECT p.id, s.id, ROW_NUMBER() OVER (PARTITION BY p.id ORDER BY s.play_count DESC, s.id)
FROM playlists AS p
JOIN songs AS s ON s.category IN ('Global', 'Popular')
WHERE p.title = 'Global Hits'
  AND NOT EXISTS (
      SELECT 1 FROM playlist_songs AS ps WHERE ps.playlist_id = p.id AND ps.song_id = s.id
  );

INSERT INTO likes (user_id, song_id)
SELECT u.id, s.id
FROM users AS u
JOIN songs AS s ON s.id <= 5
WHERE u.username = 'mahdi'
ON CONFLICT DO NOTHING;

INSERT INTO follows (follower_id, following_id)
SELECT follower.id, following.id
FROM users AS follower
JOIN users AS following ON following.username = 'mahdi'
WHERE follower.username = 'student_test'
ON CONFLICT DO NOTHING;

INSERT INTO play_history (user_id, song_id, played_at, duration_played_sec, source)
SELECT u.id, s.id, NOW() - ((s.id || ' minutes')::interval), LEAST(s.duration_sec, 120), 'player'
FROM users AS u
JOIN songs AS s ON s.id <= 3
WHERE u.username = 'mahdi'
  AND NOT EXISTS (
      SELECT 1 FROM play_history AS ph WHERE ph.user_id = u.id AND ph.song_id = s.id
  );

INSERT INTO download_entitlements (user_id, song_id, granted_at)
SELECT u.id, s.id, NOW()
FROM users AS u
JOIN songs AS s ON s.id <= 2
WHERE u.username = 'mahdi'
  AND u.is_premium = TRUE
ON CONFLICT DO NOTHING;

WITH conv AS (
    INSERT INTO conversations (type)
    SELECT 'direct'::conversation_type
    WHERE NOT EXISTS (
        SELECT 1
        FROM conversations AS c
        JOIN conversation_members AS cm1 ON cm1.conversation_id = c.id
        JOIN conversation_members AS cm2 ON cm2.conversation_id = c.id AND cm2.user_id <> cm1.user_id
        JOIN users AS u1 ON u1.id = cm1.user_id AND u1.username = 'mahdi'
        JOIN users AS u2 ON u2.id = cm2.user_id AND u2.username = 'student_test'
    )
    RETURNING id
)
INSERT INTO conversation_members (conversation_id, user_id)
SELECT conv.id, u.id
FROM conv
JOIN users AS u ON u.username IN ('mahdi', 'student_test')
ON CONFLICT DO NOTHING;

DO $$
DECLARE
    conv_id INTEGER;
    mahdi_id INTEGER;
    student_id INTEGER;
BEGIN
    SELECT id INTO mahdi_id FROM users WHERE username = 'mahdi';
    SELECT id INTO student_id FROM users WHERE username = 'student_test';

    SELECT c.id
    INTO conv_id
    FROM conversations AS c
    JOIN conversation_members AS cm1 ON cm1.conversation_id = c.id AND cm1.user_id = mahdi_id
    JOIN conversation_members AS cm2 ON cm2.conversation_id = c.id AND cm2.user_id = student_id
    LIMIT 1;

    IF conv_id IS NULL THEN
        RETURN;
    END IF;

    IF EXISTS (SELECT 1 FROM messages WHERE conversation_id = conv_id) THEN
        RETURN;
    END IF;

    INSERT INTO messages (conversation_id, sender_id, msg_type, content, delivery_status, created_at, updated_at)
    VALUES
        (conv_id, mahdi_id, 'text', 'سلام! به MeloNet خوش اومدی 🎵', 'sent', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'),
        (conv_id, student_id, 'text', 'مرسی! پلی‌لیست Global Hits عالیه.', 'sent', NOW() - INTERVAL '110 minutes', NOW() - INTERVAL '110 minutes'),
        (conv_id, mahdi_id, 'text', 'بعدا یه آهنگ share می‌کنم.', 'sent', NOW() - INTERVAL '100 minutes', NOW() - INTERVAL '100 minutes');
END $$;

INSERT INTO message_receipts (message_id, user_id, status, updated_at)
SELECT
    m.id,
    receiver.user_id,
    'delivered'::message_delivery_status,
    m.created_at
FROM messages AS m
JOIN LATERAL (
    SELECT cm.user_id
    FROM conversation_members AS cm
    WHERE cm.conversation_id = m.conversation_id
      AND cm.user_id <> m.sender_id
    LIMIT 1
) AS receiver ON TRUE
ON CONFLICT DO NOTHING;

INSERT INTO notifications (user_id, type, title, body, payload)
SELECT
    u.id,
    'follow'::notification_type,
    'New follower',
    'student_test started following you.',
    jsonb_build_object('follower_username', 'student_test')
FROM users AS u
WHERE u.username = 'mahdi'
  AND NOT EXISTS (
      SELECT 1 FROM notifications AS n
      WHERE n.user_id = u.id AND n.type = 'follow'::notification_type
  );
