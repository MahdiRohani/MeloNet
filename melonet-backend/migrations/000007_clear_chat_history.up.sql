-- Wipe existing chat history (conversations + messages + receipts).
DELETE FROM message_receipts;
DELETE FROM messages;
DELETE FROM conversation_members;
DELETE FROM conversations;
