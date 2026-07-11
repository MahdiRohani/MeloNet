CREATE TABLE premium_entitlements (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    source VARCHAR(50) NOT NULL DEFAULT 'admin',
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_premium_entitlements_user_id ON premium_entitlements (user_id);
CREATE INDEX idx_premium_entitlements_active ON premium_entitlements (user_id, expires_at)
    WHERE revoked_at IS NULL;

-- Demo credentials: username mahdi / student_test, password melonet123
UPDATE users
SET password_hash = '$2a$10$tFDcJwVIeGIy3Jsq6ghoFOr1WVhgrKYsbb/dSDmvtvOV0jDRwMYLq',
    email = CASE username
        WHEN 'mahdi' THEN 'mahdi@melonet.local'
        WHEN 'student_test' THEN 'student@melonet.local'
        ELSE email
    END
WHERE username IN ('mahdi', 'student_test');

INSERT INTO premium_entitlements (user_id, source, granted_at, expires_at)
SELECT u.id, 'admin', NOW(), NULL
FROM users AS u
WHERE u.username = 'mahdi'
  AND u.is_premium = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM premium_entitlements AS pe
      WHERE pe.user_id = u.id AND pe.revoked_at IS NULL
  );
