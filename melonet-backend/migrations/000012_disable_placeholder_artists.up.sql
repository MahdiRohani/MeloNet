-- Disable whitelist artists that still lack a community RVC model.pth.

UPDATE voice_artists
SET enabled = FALSE
WHERE slug IN (
    'mohsen-yeganeh',
    'googoosh',
    'moein',
    'siavash-ghomayshi'
);

UPDATE voice_artists
SET enabled = TRUE
WHERE slug IN (
    'shadmehr',
    'morteza-pashaei',
    'hayedeh',
    'mahasti',
    'mohsen-chavoshi',
    'ebi'
);
