package com.saar.wallpaperchanger.utils

import android.app.Activity
import android.content.ContentValues
import android.net.Uri
import android.widget.Toast
import com.saar.wallpaperchanger.CSVReader
import com.saar.wallpaperchanger.CSVWriter
import com.saar.wallpaperchanger.DbHandler
import com.saar.wallpaperchanger.utils.imageUtils.changeWallpaper
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

object fileUtils {
    fun getAllFilePaths(directory: String): List<String> {
        val startPath = Paths.get(directory)
        return Files.walk(startPath)
            .filter { Files.isRegularFile(it) }
            .map(Path::toString)
            .collect(Collectors.toList())
    }

    fun exportData(activity: Activity, uri: Uri) {
        try {
            val outputStream = activity.contentResolver.openOutputStream(uri)
            val writer = CSVWriter(OutputStreamWriter(outputStream), '|', '"', "\n")

            // Query the database to get the data to export
            val db = DbHandler(activity.applicationContext).readableDatabase
            val cursor = db.rawQuery("SELECT * FROM photos", null)

            // Write the column names to the CSV file
            writer.writeNext(cursor.columnNames)

            // Write the data to the CSV file
            while (cursor.moveToNext()) {
                val row = arrayOfNulls<String>(cursor.columnCount)
                for (i in 0 until cursor.columnCount) {
                    row[i] = cursor.getString(i)
                }
                writer.writeNext(row)
            }

            // Close the CSV writer and the database cursor
            writer.close()
            cursor.close()

            // Show a toast message indicating that the data was exported
            Toast.makeText(activity, "Data exported to $uri", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(activity, "Error exporting data", Toast.LENGTH_SHORT).show()
        }
    }

    fun importDat(activity: Activity, uri: Uri) {
        try {
            val inputStream = activity.contentResolver.openInputStream(uri)
            val reader = CSVReader(InputStreamReader(inputStream), "|")

            // Delete all existing data from the table
            val dbHandler = DbHandler(activity.applicationContext)
            val db = dbHandler.readableDatabase
            dbHandler.onUpgrade(db, 1, 1)

            // Read the data from the CSV file and insert it into the table
            var row: Array<String>? = null
            while (true) {
                // Read the next line
                val nextRow = reader.readNext()
                if (nextRow == null) break  // Exit loop if no more rows

                // Assign the read row to the variable
                row = nextRow

                // Initialize ContentValues and populate it with row data
                val values = ContentValues()
                for (i in row.indices) {
                    values.put(reader.header[i], row[i])
                }
                db.insert("photos", null, values)
            }


            // Close the CSV reader and the database
            dbHandler.resetArtistsData()
            val c = db.rawQuery(
                "select NAME from photos WHERE USED = 1 ORDER BY USED_ORDER DESC limit 1",
                arrayOf()
            )
            c.moveToFirst()
            if (c.count > 0) {
                val latestAlbum = c.getString(0)
                changeWallpaper(activity.applicationContext, latestAlbum, false)
            }
            c.close()
            reader.close()
            db.close()

            // Show a toast message indicating that the data was imported
            Toast.makeText(activity, "Data imported from $uri", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(activity, "Error importing data", Toast.LENGTH_SHORT).show()
        }
    }
}