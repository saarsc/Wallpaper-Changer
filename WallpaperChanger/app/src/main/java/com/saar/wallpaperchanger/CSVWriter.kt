package com.saar.wallpaperchanger

import java.io.IOException
import java.io.Writer

class CSVWriter(
    private val writer: Writer,
    private val separator: Char,
    private val quote: Char,
    private val lineEnd: String
) {
    @Throws(IOException::class)
    fun writeNext(values: Array<String?>?) {
        if (values == null || values.size == 0) {
            return
        }

        val sb = StringBuilder()
        for (i in values.indices) {
            if (i > 0) {
                sb.append(separator)
            }
            if (values[i] != null) {
                if (values[i]!!.contains(quote.toString())) {
                    sb.append(quote).append(
                        values[i]!!.replace(
                            quote.toString(),
                            quote.toString() + quote.toString()
                        )
                    ).append(
                        quote
                    )
                } else if (values[i]!!.contains(separator.toString()) || values[i]!!.contains("\n")) {
                    sb.append(quote).append(values[i]).append(quote)
                } else {
                    sb.append(values[i])
                }
            }
        }
        sb.append(lineEnd)
        writer.write(sb.toString())
    }

    @Throws(IOException::class)
    fun close() {
        writer.close()
    }

    companion object {
        private const val DEFAULT_SEPARATOR = ','
        private const val DEFAULT_QUOTE = '"'
        private const val DEFAULT_LINE_END = "\n"
    }
}
