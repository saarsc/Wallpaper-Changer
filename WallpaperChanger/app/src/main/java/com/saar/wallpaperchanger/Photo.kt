package com.saar.wallpaperchanger;

import androidx.annotation.NonNull;

public class Photo {
    String path;
    String name;
    String artist;
    String date;

    public Photo(String path, String name, String artist) {
        this.path = path;
        this.name = name;
        this.artist = artist;

    }

    public Photo(String date, String name) {
        this.date = date;
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    @NonNull
    @Override
    public String toString() {
        if (this.date != null) {
            return this.name + " - " + this.date;
        }
        return this.name + "- לא נוגן";

    }
}
