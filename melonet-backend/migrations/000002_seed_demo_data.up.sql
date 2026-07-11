INSERT INTO users (username, avatar_url, is_premium)
SELECT 'mahdi', '/static/covers/cover1.jpg', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'mahdi');

INSERT INTO users (username, avatar_url, is_premium)
SELECT 'student_test', '/static/covers/cover2.jpg', FALSE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'student_test');

INSERT INTO songs (title, artist, cover_url, audio_url, category, lyrics, duration_sec)
SELECT
    format('MeloNet Track %s', seed.i),
    format('Artist Name %s', ((seed.i % 7) + 1)),
    format('/static/covers/cover%s.jpg', seed.i),
    format('/static/audio/song%s.mp3', seed.i),
    (ARRAY['Iranian', 'Global', 'Nostalgia', 'New', 'Popular'])[1 + (seed.i % 5)],
    format('[00:00] Intro Track %s\n[00:05] This is synchronization lyrics test\n[00:12] MeloNet is working perfectly!', seed.i),
    150 + (seed.i * 2)
FROM generate_series(1, 50) AS seed(i)
WHERE NOT EXISTS (SELECT 1 FROM songs LIMIT 1);
