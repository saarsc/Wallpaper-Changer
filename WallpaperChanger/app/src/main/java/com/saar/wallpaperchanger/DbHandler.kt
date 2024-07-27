package com.saar.wallpaperchanger

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Arrays
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.stream.Collectors

class DbHandler(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private val context: Context

    init {
        conn = writableDatabase
        this.context = context
    }

    override fun onCreate(db: SQLiteDatabase) {
        createTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)

        // Create tables again
        createTable(db)
    }

    private fun createTable(db: SQLiteDatabase) {
        val CREATE_DB =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ( " + KEY_ID + " INTEGER PRIMARY KEY," + KEY_PATH + " TEXT UNIQUE, " + KEY_NAME + " TEXT, " + KEY_ARTIST + " TEXT," + KEY_USED + " BOOLEAN DEFAULT 0, " + KEY_ORDER + " INTEGER, " + KEY_DATE + " DATE, vinyl BOOLEAN DEFAULT 0, only_weekend BOOLEAN DEFAULT 0);"
        db.execSQL(CREATE_DB)
    }

    private fun resetStats() {
        val CREATE_STATS =
            "CREATE TABLE IF NOT EXISTS " + TABLE_STATS_TABLE_NAME + " ( " + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT, " + KEY_STATS_FIRST_PLAYED + " DATE," + KEY_STATS_LAST_PLAYED + " DATE," + KEY_STATS_FIRST_PLAYED_NAME + " TEXT," + KEY_STATS_LAST_PLAYED_NAME + " TEXT, " + KEY_STATS_OCCURRENCES + " INTEGER);"
        conn.execSQL(CREATE_STATS)
    }

    fun resetArtistsData() {
        val c = conn.rawQuery("SELECT MIN(DATE) FROM photos where date != ''", arrayOf())
        var firstDate = util.today().toString()
        if (c.moveToNext()) {
            firstDate = c.getString(0)
        }
        conn.execSQL("DROP TABLE IF EXISTS " + TABLE_ARTISTS_DATA)
        val createArtist = "CREATE TABLE artist_stats AS " +
                "SELECT ARTIST, " +
                "IFNULL(ROUND(COUNT(CASE WHEN USED = 0 AND only_weekend != 1 THEN 1 END) * 1.0 / COUNT(CASE WHEN only_weekend != 1 THEN 1 END), 2), 0) AS PERCENTAGE, " +
                "IFNULL(ROUND(COUNT(CASE WHEN USED = 0 AND (VINYL = 1 OR only_weekend = 1) THEN 1 END) * 1.0 / COUNT(CASE WHEN VINYL = 1 OR only_weekend = 1 THEN 1 END), 2), 0) " +
                "AS VINYL_PERCENTAGE, COUNT(*) AS TOTAL, " +
                "CASE WHEN MAX(DATE) IS NULL THEN '" + firstDate + "' ELSE MAX(DATE) END as latest_date FROM photos GROUP BY ARTIST"
        conn.execSQL(createArtist)
    }

    /**
     * _->_
     * Populating the DB with all homescreen pictures
     */
    private fun initNewData(vinylBackup: HashMap<String, List<Int>?>) {
        val sharedPreferences = context.getSharedPreferences("USED_ORDER", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("num", 1)
        editor.apply()

        for (file in util.getAllFilePaths("/storage/emulated/0/Homescreen")) {
            val values = ContentValues()
            val splitPath = file.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var fileName = splitPath[splitPath.size - 1].trim { it <= ' ' }
            val artistName = splitPath[splitPath.size - 2].trim { it <= ' ' }

            if (fileName.contains(".")) {
                fileName = fileName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0].trim { it <= ' ' }
            }

            values.put(KEY_PATH, file.trim { it <= ' ' })
            values.put(KEY_NAME, fileName)
            values.put(KEY_ARTIST, artistName)
            values.put(KEY_USED, false)
            if (vinylBackup[fileName + artistName] != null) {
                values.put("vinyl", 1)
                values.put("only_weekend", vinylBackup[fileName + artistName]!![1])
            }
            try {
                conn.insert(TABLE_NAME, null, values)
            } catch (e: SQLiteConstraintException) {
                Log.d("WALLPAPER INIT DATA", "trying to add an existing path")
            }
        }
    }

    /**
     * String  -> List<String>
     * Lists all the files within the given path
     *
     * @param directoryName -> The Top folder to search
     * @return List<String> resultList -> list with all the paths of files in the directory and subdirectories
    </String></String> */
    private fun listf(directoryName: String): List<String> {
        var resultList: List<String> = ArrayList()
        val dir = Paths.get(directoryName)
        try {
            Files.walk(dir, Int.MAX_VALUE).filter { path: Path? -> Files.isRegularFile(path) }
                .use { stream ->
                    resultList = stream.map { obj: Path? -> java.lang.String.valueOf(obj) }
                        .sorted().collect(Collectors.toList())
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return resultList
    }


    /**
     * Normalizing the chance of getting an artist.
     * available / artist total
     */
    private fun percentageBasedArtist(artist: String?): String? {
        var result: String? = null
        if (this.shouldUseVinyl()) {
            result = this.percentageBasedArtistForColumn("VINYL_PERCENTAGE", artist)
        }
        if (result == null) {
            result = this.percentageBasedArtistForColumn(KEY_PERCENTAGE, artist)
        }

        //        double r = BigDecimal.valueOf(Math.random() * cursor.getFloat(2)).setScale(2, RoundingMode.HALF_UP).doubleValue();
//        while (r > 0) {
//            r -= cursor.getFloat(1);
//            cursor.moveToNext();
//        }
//        cursor.moveToPrevious();
        return result
    }

    private fun percentageBasedArtistForColumn(column: String, artist: String?): String? {
        var cursor = this.getPercentBasedCursor(column, artist)
        var noResults = cursor.count == 0

        if (noResults) {
            cursor = this.getPercentBasedCursor(column, "")
            noResults = cursor.count == 0
        }
        if (noResults) {
            return null
        }
        cursor.moveToFirst()
        return cursor.getString(0)
    }

    private fun getPercentBasedCursor(column: String, artist: String?): Cursor {
        var query = "SELECT ARTIST, " +
                "CASE" +
                " WHEN latest_date IS NULL OR latest_date = '' THEN " + column +
                " ELSE " + column + " * (1.0 + (julianday('now') - julianday(latest_date)))" +
                " END AS combined_score" +
                " FROM ARTIST_STATS WHERE " + column + " > 0 "
        var args: Array<String?>? = null
        if (!artist!!.isEmpty()) {
            query += "AND ARTIST != ? "
            args = arrayOf(artist)
        }
        //SELECT ARTIST,  CASE WHEN latest_date IS NULL OR latest_date = '' THEN  PERCENTAGE  ELSE     (1.0 + (julianday('now') - julianday(latest_date)))  END AS combined_score FROM ARTIST_STATS WHERE  PERCENTAGE > 0
        query += "ORDER BY RANDOM() * combined_score LIMIT 1"
        //        return conn.rawQuery("SELECT " + KEY_ARTIST + ", " + column + ",(SELECT SUM(" + column + ") FROM " + TABLE_ARTISTS_DATA + " WHERE " + KEY_ARTIST +" !='" + artist + "') AS SUM FROM " + TABLE_ARTISTS_DATA + " WHERE " + KEY_ARTIST + " != '" + artist + "' ORDER BY RANDOM();", null);
        return conn.rawQuery(query, args)
    }

    /**
     * String -> List<Photo>
     * Checks the DB for all available photos (AKA where used is set to false / 0).
     * If available allows only different artist than the latest artist.
     * Handles resetting the DB and sending notification when needed
     *
     * @param artist -> previous day album artist
     * @return List<Photo> photoList -> A list object with all the possible photos
    </Photo></Photo> */
    fun availablePhotos(artist: String?, checkForStatus: Boolean): MutableList<Photo> {
        var photoList: MutableList<Photo> = ArrayList()

        var selectQuery =
            "SELECT *, (SELECT COUNT(*) FROM " + TABLE_NAME + ") AS amount_left FROM " + TABLE_NAME + " WHERE ARTIST= ? AND USED = 0"
        selectQuery += if (this.shouldUseVinyl()) {
            " AND vinyl = 1"
        } else {
            " AND only_weekend != 1"
        }

        val selected_artist = this.percentageBasedArtist(artist)
        var cursor = conn.rawQuery(selectQuery, arrayOf(selected_artist))

        if (!cursor.moveToFirst() || !checkForStatus) {
            selectQuery = "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_USED + " = 0"
            cursor = conn.rawQuery(selectQuery, null)
        }

        if (cursor.moveToFirst()) {
            do {
                val path = cursor.getString(1)
                val name = cursor.getString(2)
                val rowArtist = cursor.getString(3)
                if (artist == "" || rowArtist != artist) {
                    val photo = Photo(path, name, rowArtist)
                    photoList.add(photo)
                }
            } while (cursor.moveToNext())
        }

        if (this.availableAlbumsCount() <= 5 && checkForStatus) {
            val sharedPreferences = context.getSharedPreferences("ROUND_NAME", Context.MODE_PRIVATE)
            val name = sharedPreferences.getString("name", "Round")

            if (name == "Round" || !name!!.lowercase(Locale.getDefault()).contains("round")) {
                sendNewRoundNotification()
            }

            if (this.availableAlbumsCount() <= 0) {
                val editor = sharedPreferences.edit()
                editor.putString("name", "Round")
                editor.putString("current_round", name)
                editor.apply()

                util.sendRequest("new", name, context)

                resetDB()

                photoList = availablePhotos("", true)
            }
        }

        cursor.close()

        if (photoList.size == 0) {
            photoList = availablePhotos("", true)
        }

        return photoList
    }

    fun shouldUseVinyl(): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val dayOfWeek = calendar[Calendar.DAY_OF_WEEK]
        val sp = context.getSharedPreferences("vinylWeekend", Context.MODE_PRIVATE)
        return (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY) && sp.getBoolean(
            "shouldUseVinyl",
            true
        )
    }

    fun availableArtistsWithPercentage(): List<String> {
        val stats: MutableList<String> = ArrayList()
        val selectQuery = "SELECT * FROM " + TABLE_ARTISTS_DATA + " ORDER BY PERCENTAGE DESC"
        val cursor = conn.rawQuery(selectQuery, arrayOf())

        if (cursor.moveToFirst()) {
            do {
                try {
                    val stat = cursor.getFloat(1)
                    val name = cursor.getString(0)
                    stats.add("$name - $stat")
                } catch (e: Exception) {
                    Log.d("VAL", cursor.getType(1).toString())
                }
            } while (cursor.moveToNext())
        }
        return stats
    }

    val newPhoto: Photo
        /**
         * _->Photo
         * Handles everything that involves interacting with the DB. picks the new image and returns it to service method
         *
         * @return photo -  the new photo to set as the wallpaper
         */
        get() {
            var sp = context.getSharedPreferences("currentAlbum", Context.MODE_PRIVATE)
            val editor = sp.edit()

            val artistName = sp.getString("ARTIST", "")

            val photoList: List<Photo> = this.availablePhotos(artistName, true)
            val rnd = Random()

            val index = rnd.nextInt(photoList.size)
            val photo = photoList[index]

            editor.putString("PATH", photo.path)
            editor.putString("NAME", photo.name)
            editor.putString("ARTIST", photo.artist)
            editor.apply()

            val used_order = getPlace(true)
            val values = ContentValues()

            val dt = Date()
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yy")
            //        String date = LocalDateTime.from(dt.toInstant().atZone(ZoneId.of("Israel"))).plusDays(1).format(formatter);
            val date = util.today().plusDays(1).toString()

            values.put(KEY_USED, true)
            values.put(KEY_ORDER, used_order)
            values.put(KEY_DATE, date)
            conn.update(TABLE_NAME, values, KEY_PATH + " =?", arrayOf(photo.path))

            sp = context.getSharedPreferences("ROUND_NAME", Context.MODE_PRIVATE)

            //        updateStats(photo.getName(), date, sp.getString("current_round","Twelfth Round (Day)"));
            this.resetArtistsData()

            return photo
        }

    /**F
     * _->_
     * Resetting the DB and initialize it
     */
    fun resetDB() {
        val vinylBackup =
            vinyl
        onUpgrade(conn, 1, 1)
        initNewData(vinylBackup)
        resetArtistsData()
    }

    private val vinyl: HashMap<String, List<Int>?>
        get() {
            val query = "SELECT * FROM photos WHERE vinyl = 1"
            val cursor = conn.rawQuery(query, null)
            val data = HashMap<String, List<Int>?>()
            if (cursor.moveToFirst()) {
                do {
                    val name = cursor.getString(cursor.getColumnIndex(KEY_NAME))
                    val artist = cursor.getString(cursor.getColumnIndex(KEY_ARTIST))

                    val vinylData =
                        Arrays.asList(1, cursor.getInt(cursor.getColumnIndex("only_weekend")))
                    data[name + artist] = vinylData
                } while (cursor.moveToNext())
            }
            return data
        }

    /**
     * Boolean -> Int
     * get the album number from the SP
     *
     * @param update -> Whether or not to also update the value (++)
     * @return used_order -> the current used place
     */
    fun getPlace(update: Boolean): Int {
        val sharedPreferences = context.getSharedPreferences("USED_ORDER", Context.MODE_PRIVATE)
        val used_order = sharedPreferences.getInt("num", 1)
        if (update) {
            val editor = sharedPreferences.edit()
            editor.putInt("num", used_order + 1)
            editor.apply()
        }
        return used_order
    }

    /**
     * _->_
     * Parse the data from the relevant gKeep note. Inserts it into the DB
     */
    fun restoreDB() {
        conn.execSQL("UPDATE " + TABLE_NAME + " SET " + KEY_USED + " = 0")
        conn.execSQL("UPDATE " + TABLE_NAME + " SET " + KEY_ORDER + " = null")
        conn.execSQL("UPDATE " + TABLE_NAME + " SET " + KEY_DATE + " = null")
        val inputDateFormat = SimpleDateFormat("dd.MM.yy")
        val outputDateFormat = SimpleDateFormat("yyyy-MM-dd")
        val data = util.sendRequest("restore", "", context)
        if (data != "ERROR") {
            val splitData = data.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var i = 0
            if (splitData.size > 0) {
                var strippedString: Array<String>
                var place: Int
                var name: String? = null
                var date: String

                for (album in splitData) {
                    if (album != "") {
                        val values = ContentValues()

                        strippedString = album.split("\\)".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()

                        place = strippedString[0].trim { it <= ' ' }.toInt()

                        strippedString =
                            strippedString[1].split("-".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        name = strippedString[0].trim { it <= ' ' }
                        date = strippedString[1].trim { it <= ' ' }
                        var inputDate: Date? = null
                        try {
                            inputDate = inputDateFormat.parse(date)
                        } catch (e: ParseException) {
                            throw RuntimeException(e)
                        }
                        val outputDateStr = outputDateFormat.format(inputDate)
                        values.put(KEY_ORDER, place)
                        values.put(KEY_USED, true)
                        values.put(KEY_DATE, outputDateStr)
                        val rows =
                            conn.update(TABLE_NAME, values, KEY_NAME + " =?", arrayOf<String>(name))
                        if (rows == 0) {
                            Log.e("Mismatch name", name)
                        }
                        i += 1
                    }
                }

                util.changeWallpaper(context, name, false)
            }
            val sharedPreferences = context.getSharedPreferences("USED_ORDER", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putInt("num", i)
            editor.apply()
        }
        resetArtistsData()
    }

    fun restoreStats() {
        conn.execSQL("DROP TABLE IF EXISTS " + TABLE_STATS_TABLE_NAME)
        resetStats()
        val prevData = util.sendRequest("restore", "yes", context)

        try {
            val jsonData = JSONObject(prevData)
            val keys = jsonData.keys()

            while (keys.hasNext()) {
                val key = keys.next()

                val roundData =
                    jsonData[key].toString().split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                for (album in roundData) {
                    if (album != "") {
                        var withoutNumber: Array<String>? = null
                        try {
                            withoutNumber =
                                album.split("\\)".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()[1].split("-".toRegex())
                                    .dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                        } catch (e: Exception) {
                            Log.e("Current album: ", album)
                        }

                        val name = withoutNumber!![0].trim { it <= ' ' }
                        val date = withoutNumber[1]
                        //                        updateStats(name,date,key);
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun updateStats(name: String, date: String, key: String) {
        if (conn.rawQuery(
                "SELECT * FROM " + TABLE_STATS_TABLE_NAME + " WHERE " + KEY_NAME + " = ?",
                arrayOf(name)
            ).count == 0
        ) {
            val values = ContentValues()

            values.put(KEY_NAME, name)
            values.put(KEY_STATS_LAST_PLAYED, date)
            values.put(KEY_STATS_FIRST_PLAYED, date)
            values.put(KEY_STATS_OCCURRENCES, 1)
            values.put(KEY_STATS_FIRST_PLAYED_NAME, key)
            values.put(KEY_STATS_LAST_PLAYED_NAME, key)
            conn.insert(TABLE_STATS_TABLE_NAME, null, values)
        } else {
            conn.execSQL(
                "UPDATE " + TABLE_STATS_TABLE_NAME + " SET " + KEY_STATS_OCCURRENCES + " = " + KEY_STATS_OCCURRENCES + " +1 WHERE " + KEY_NAME + " = ?",
                arrayOf(name)
            )
            conn.execSQL(
                "UPDATE " + TABLE_STATS_TABLE_NAME + " SET " + KEY_STATS_FIRST_PLAYED + " = ?, " + KEY_STATS_FIRST_PLAYED_NAME + "=? WHERE " + KEY_NAME + " = ? AND " + KEY_STATS_FIRST_PLAYED_NAME + "> ?",
                arrayOf(date, key, name, date)
            )
            conn.execSQL(
                "UPDATE " + TABLE_STATS_TABLE_NAME + " SET " + KEY_STATS_LAST_PLAYED + " = ?," + KEY_STATS_LAST_PLAYED_NAME + "=? WHERE " + KEY_NAME + " = ? AND " + KEY_STATS_LAST_PLAYED + " < ?",
                arrayOf(date, key, name, date)
            )
        }
    }

    /**
     * _->_
     * Sends notification if the next round name has yet to been set
     * Support quick reply from the notification
     */
    fun sendNewRoundNotification() {
        val KEY_ROUND_NAME = "next_round_name"

        val notificationIntent = Intent(
            "com.saar.wallpaperchanger.roundnoitification",
            null,
            context,
            NotificationReceiver::class.java
        )

        notificationIntent.putExtra("NAME", "Round")

        val nameLabel = "Pick new name"
        val remoteInput = RemoteInput.Builder(KEY_ROUND_NAME).setLabel(nameLabel).build()
        val namePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val action = NotificationCompat.Action.Builder(
            R.drawable.notificationaddname,
            nameLabel,
            namePendingIntent
        )
            .addRemoteInput(remoteInput)
            .build()

        val builder = NotificationCompat.Builder(context, "Set Round Name")
            .setContentTitle("I think you have forgotten something...")
            .setSmallIcon(R.drawable.notificationicon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Hello! you need to set the next round name for the love of god please do it or else!")
            )
            .addAction(action)


        val notificationManagerCompat = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                "android.permission.POST_NOTIFICATIONS"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManagerCompat.notify(1, builder.build())
    }

    val allNames: List<String>
        /**
         * used for the search auto-complete
         *
         * @return List<String> names  a list of all the albums names in the DB
        </String> */
        get() {
            val cursor = conn.rawQuery("SELECT * FROM " + TABLE_NAME, null)

            val names: MutableList<String> = ArrayList()

            if (cursor.moveToFirst()) {
                names.add(cursor.getString(cursor.getColumnIndex(KEY_NAME)))
                while (cursor.moveToNext()) {
                    names.add(cursor.getString(cursor.getColumnIndex(KEY_NAME)))
                }
            }
            cursor.close()

            return names
        }

    /**
     * Given an album name returns the date that it was played
     *
     * @param name the album name to search
     * @return date the date it was plated on
     */
    fun getAlbumDate(name: String): String {
        var date = "$name has yet to been played"
        val cursor = conn.rawQuery("select DATE from photos where NAME =?", arrayOf(name))

        if (cursor.moveToFirst()) {
            date = cursor.getString(0)
        }
        cursor.close()
        if (date == null) {
            date = "$name has yet to been played"
        }
        return date
    }

    /**
     * Given an album name returns the date that it was played
     *
     * @param name the album name to search
     * @return if the album is marked as vinyl or not
     */
    fun isVinyl(name: String): Boolean {
        val cursor = conn.rawQuery("select vinyl from photos where NAME =?", arrayOf(name))

        if (cursor.moveToFirst()) {
            return cursor.getInt(0) == 1
        }
        cursor.close()
        return false
    }

    fun isOnlyWeekend(name: String): Boolean {
        val cursor = conn.rawQuery("select only_weekend from photos where NAME =?", arrayOf(name))

        if (cursor.moveToFirst()) {
            return cursor.getInt(0) == 1
        }
        cursor.close()
        return false
    }

    /**
     * Given an album name returns the date that it was played
     *
     * @param name the album name to search
     * @return if the album is marked as vinyl or not
     */
    fun setVinyl(name: String, vararg options: Boolean) {
        val isVinyl = options[0]
        val values = ContentValues()
        values.put("vinyl", isVinyl)
        if (options.size > 1) {
            values.put("only_weekend", options[1])
        }
        conn.update(TABLE_NAME, values, KEY_NAME + " =?", arrayOf(name))
    }

    private fun availableAlbumsCount(): Int {
        return conn.rawQuery(
            "select * from " + TABLE_NAME + " where " + KEY_USED + " =0",
            null
        ).count
    }

    /**
     * Given an album names returns all of its data
     *
     * @param album - album name to search
     * @return photo object containing all of the data
     */
    fun getAlbumData(album: String): Photo? {
        val cursor = conn.rawQuery(
            "select * from " + TABLE_NAME + " where " + KEY_NAME + " =?",
            arrayOf(album)
        )
        var albumData: Photo? = null

        if (cursor.moveToFirst()) {
            val path = cursor.getString(1)
            val name = cursor.getString(2)
            val artist = cursor.getString(3)
            albumData = Photo(path, name, artist)
        }

        cursor.close()
        return albumData
    }

    fun firstAlbumDate(): String {
        val cursor = conn.rawQuery(
            "SELECT " + KEY_DATE + " FROM " + TABLE_NAME + " WHERE " + KEY_ORDER + " =?",
            arrayOf("1")
        )
        if (cursor.moveToFirst()) {
            return cursor.getString(0)
        }

        return ""
    }

    val rowsCount: Long
        get() = DatabaseUtils.queryNumEntries(conn, TABLE_NAME)

    val allArtistNames: List<String>
        /**
         * used for the search auto-complete-artist
         *
         * @return List<String> names  a list of all the artist names in the DB
        </String> */
        get() {
            val cursor = conn.rawQuery("SELECT * FROM " + TABLE_NAME, null)

            val names = HashSet<String>()

            if (cursor.moveToFirst()) {
                names.add(cursor.getString(cursor.getColumnIndex(KEY_ARTIST)))
                while (cursor.moveToNext()) {
                    names.add(cursor.getString(cursor.getColumnIndex(KEY_ARTIST)))
                }
            }
            cursor.close()

            return ArrayList(names)
        }

    fun searchByArtist(artist: String): String {
        val totalAmount = conn.rawQuery(
            "select * from " + TABLE_NAME + " where " + KEY_ARTIST + " =?",
            arrayOf(artist)
        ).count

        val usedAmount = conn.rawQuery(
            "select * from " + TABLE_NAME + " where " + KEY_ARTIST + " =? and " + KEY_USED + " = 1",
            arrayOf(artist)
        ).count

        return "$usedAmount / $totalAmount"
    }

    fun getAllAlbumsByArtist(artist: String): List<Photo> {
        val cursor = conn.rawQuery(
            "SELECT " + KEY_NAME + "," + KEY_DATE + "  FROM " + TABLE_NAME + " WHERE " + KEY_ARTIST + " =?",
            arrayOf(artist)
        )

        val albumsList: MutableList<Photo> = ArrayList()
        if (cursor.moveToFirst()) {
            albumsList.add(Photo(cursor.getString(1), cursor.getString(0)))
            while (cursor.moveToNext()) {
                albumsList.add(Photo(cursor.getString(1), cursor.getString(0)))
            }
        }
        cursor.close()
        return albumsList
    }


    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "homescreen"

        //    Main / Albums
        private const val TABLE_NAME = "photos"
        private const val KEY_ID = "ID"
        private const val KEY_NAME = "NAME"
        private const val KEY_ARTIST = "ARTIST"
        private const val KEY_PATH = "PATH"
        private const val KEY_USED = "USED"
        private const val KEY_ORDER = "USED_ORDER"
        private const val KEY_DATE = "DATE"

        // stats
        private const val KEY_STATS_LAST_PLAYED = "last_played"
        private const val KEY_STATS_OCCURRENCES = "occurrences"
        private const val KEY_STATS_FIRST_PLAYED = "first_played"
        private const val KEY_STATS_LAST_PLAYED_NAME = "last_played_name"
        private const val KEY_STATS_FIRST_PLAYED_NAME = "first_played_name"
        private const val TABLE_STATS_TABLE_NAME = "stats"

        //    Artist
        private const val TABLE_ARTISTS_DATA = "artist_stats"
        private const val KEY_TOTAL_ALBUMS = "total"
        private const val KEY_USED_ALBUMS = "used"
        private const val KEY_PERCENTAGE = "percentage"


        private lateinit var conn: SQLiteDatabase
    }
}

