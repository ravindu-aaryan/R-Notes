package com.example.screennotes.capture

/** Turns raw text fragments from the screen into one clean block of text. */
object TextExtractor {

    const val MAX_CHARS = 50_000

    private val whitespace = Regex("\\s+")

    fun combine(fragments: List<String>): String {
        val out = StringBuilder()
        var previous: String? = null
        for (fragment in fragments) {
            val line = fragment.replace(whitespace, " ").trim()
            if (line.isEmpty() || line == previous) continue
            if (out.isNotEmpty()) out.append('\n')
            out.append(line)
            previous = line
            if (out.length >= MAX_CHARS) break
        }
        return out.toString().take(MAX_CHARS)
    }
}
