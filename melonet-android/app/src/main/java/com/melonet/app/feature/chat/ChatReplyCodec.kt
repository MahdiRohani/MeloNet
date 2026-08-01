package com.melonet.app.feature.chat

/**
 * Client-side reply encoding embedded in message content (no backend schema change).
 * Format: `\u2063REPLY\u2063{id}\u2063{author}\u2063{preview}\u2063{body}`
 */
object ChatReplyCodec {
    private const val MARK = '\u2063'

    data class Parsed(
        val replyToId: String?,
        val replyAuthor: String?,
        val replyPreview: String?,
        val body: String,
    )

    fun encode(
        replyToId: String,
        author: String,
        preview: String,
        body: String,
    ): String {
        val safeAuthor = author.replace(MARK, ' ').trim().take(40)
        val safePreview = preview.replace(MARK, ' ').replace('\n', ' ').trim().take(80)
        return buildString {
            append(MARK)
            append("REPLY")
            append(MARK)
            append(replyToId.replace(MARK, ' '))
            append(MARK)
            append(safeAuthor)
            append(MARK)
            append(safePreview)
            append(MARK)
            append(body)
        }
    }

    fun parse(content: String): Parsed {
        if (content.isEmpty() || content[0] != MARK) {
            return Parsed(null, null, null, content)
        }
        val prefix = "${MARK}REPLY$MARK"
        if (!content.startsWith(prefix)) {
            return Parsed(null, null, null, content)
        }
        var rest = content.removePrefix(prefix)
        fun nextField(): String? {
            val idx = rest.indexOf(MARK)
            if (idx < 0) return null
            val value = rest.substring(0, idx)
            rest = rest.substring(idx + 1)
            return value
        }
        val id = nextField() ?: return Parsed(null, null, null, content)
        val author = nextField() ?: return Parsed(null, null, null, content)
        val preview = nextField() ?: return Parsed(null, null, null, content)
        return Parsed(
            replyToId = id,
            replyAuthor = author,
            replyPreview = preview,
            body = rest,
        )
    }

    fun displayBody(content: String): String = parse(content).body

    fun conversationPreview(content: String): String = displayBody(content)
}
