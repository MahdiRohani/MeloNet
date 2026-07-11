DELETE FROM premium_entitlements;

UPDATE users
SET password_hash = '',
    email = NULL
WHERE username IN ('mahdi', 'student_test');

DROP TABLE IF EXISTS premium_entitlements;
