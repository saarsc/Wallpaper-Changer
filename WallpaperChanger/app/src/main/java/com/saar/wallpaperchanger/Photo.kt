package com.saar.wallpaperchanger

class Photo {
    @JvmField
    var path: String? = null
    @JvmField
    var name: String
    @JvmField
    var artist: String? = null
    var date: String? = null

    constructor(path: String?, name: String, artist: String?) {
        this.path = path
        this.name = name
        this.artist = artist
    }

    constructor(date: String?, name: String) {
        this.date = date
        this.name = name
    }

    override fun toString(): String {
        if (this.date != null) {
            return this.name + " - " + this.date
        }
        return this.name + "- לא נוגן"
    }
}
