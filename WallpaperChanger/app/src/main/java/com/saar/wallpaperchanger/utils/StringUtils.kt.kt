package com.saar.wallpaperchanger.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

object stringUtils {
    fun wordToNumber(input: String): Int {
        var word = input.lowercase()
        val baseNumbersMap = mapOf(
            "first" to 1, "second" to 2, "third" to 3, "fourth" to 4, "fifth" to 5,
            "sixth" to 6, "seventh" to 7, "eighth" to 8, "ninth" to 9, "tenth" to 10,
            "eleventh" to 11, "twelfth" to 12, "thirteenth" to 13, "fourteenth" to 14, "fifteenth" to 15,
            "sixteenth" to 16, "seventeenth" to 17, "eighteenth" to 18, "nineteenth" to 19
        )

        val tensMap = mapOf(
            "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
            "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90
        )

        // Direct lookup for numbers up to nineteen
        if (baseNumbersMap.containsKey(word)) {
            return baseNumbersMap[word]!!
        }

        // Split compound words like "twenty-first" or "thirty-second"
        for (tens in tensMap.keys) {
            if (word.startsWith(tens)) {
                val suffix = word.removePrefix("$tens-")
                val tensValue = tensMap[tens] ?: 0
                val suffixValue = baseNumbersMap[suffix] ?: 0
                return tensValue + suffixValue
            }
        }

        throw IllegalArgumentException("Unknown number word: $word")
    }


    fun extractNameDateAndPlace(line: String): Triple<String, String, Int> {
        val inputDateFormat = SimpleDateFormat("dd.MM.yy")
        val outputDateFormat = SimpleDateFormat("yyyy-MM-dd")
        var date: String
        var strippedString: Array<String> = line.split("\\)".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()

        var place: Int = strippedString[0].trim { it <= ' ' }.toInt()

        strippedString =
            strippedString[1].split("-".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        var name: String = strippedString[0].trim { it <= ' ' }
        date = strippedString[1].trim { it <= ' ' }
        var inputDate: Date? = null
        try {
            inputDate = inputDateFormat.parse(date)
        } catch (e: ParseException) {
            throw RuntimeException(e)
        }
        val outputDateStr = outputDateFormat.format(inputDate)
        return Triple(name, outputDateStr, place)
    }

}