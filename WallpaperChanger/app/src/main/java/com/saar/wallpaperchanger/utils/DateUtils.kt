package com.saar.wallpaperchanger.utils

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

object dateUtils {
    fun today(): LocalDateTime {
        val dt = Date()
        return LocalDateTime.from(dt.toInstant().atZone(ZoneId.of("Israel")))
    }

}