package com.saar.wallpaperchanger

import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.util.regex.Pattern

class CSVReader(reader: Reader?, private val delimiter: String) {
    private val reader = BufferedReader(reader)
    @JvmField
    var header: Array<String> = TODO()

    init {
        try {
            // Read the first line as the header
            val line = this.reader.readLine()
            if (line != null) {
                header = this.splitLine(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun splitLine(line: String): Array<String> {
        return line.split(Pattern.quote(this.delimiter).toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
    }

    fun readNext(): Array<String>? {
        try {
            val line = reader.readLine()
            if (line != null) {
                return this.splitLine(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun readAll(): List<Array<String>> {
        val rows: MutableList<Array<String>> = ArrayList()
        var row: Array<String>
        while ((readNext().also { row = it!! }) != null) {
            rows.add(row)
        }
        return rows
    }

    fun close() {
        try {
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
