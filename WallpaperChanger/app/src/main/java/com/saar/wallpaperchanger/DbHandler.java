package com.saar.wallpaperchanger;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DbHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "homescreen";
    private static final String TABLE_NAME = "photos";
    private static final String KEY_ID = "ID";
    private static final String KEY_NAME = "NAME";
    private static final String KEY_ARTIST = "ARTIST";
    private static final String KEY_PATH = "PATH";
    private static final String KEY_USED = "USED";
    private static final String KEY_ORDER = "USED_ORDER";
    private static final String KEY_DATE = "DATE";

    private static final String KEY_STATS_LAST_PLAYED = "last_played";
    private static final String KEY_STATS_OCCURRENCES = "occurrences";
    private static final String KEY_STATS_FIRST_PLAYED = "first_played";
    private static final String KEY_STATS_LAST_PLAYED_NAME = "last_played_name";
    private static final String KEY_STATS_FIRST_PLAYED_NAME = "first_played_name";
    private static final String TABLE_STATS_TABLE_NAME = "stats";


    private static SQLiteDatabase conn;

    private final Context context;

    public DbHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        conn = getWritableDatabase();
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

        // Create tables again
        createTable(db);
    }
    private void createTable(@NonNull SQLiteDatabase db){
        String CREATE_DB = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ( " + KEY_ID + " INTEGER PRIMARY KEY," + KEY_PATH + " TEXT UNIQUE, " + KEY_NAME + " TEXT, " + KEY_ARTIST + " TEXT," + KEY_USED + " BOOLEAN, " + KEY_ORDER + " INTEGER, " + KEY_DATE + " DATE );";
        db.execSQL(CREATE_DB);
    }
    private void resetStats() {
        String CREATE_STATS = "CREATE TABLE IF NOT EXISTS " + TABLE_STATS_TABLE_NAME + " ( " + KEY_ID + " INTEGER PRIMARY KEY," +  KEY_NAME+ " TEXT, " + KEY_STATS_FIRST_PLAYED +" DATE," + KEY_STATS_LAST_PLAYED + " DATE," + KEY_STATS_FIRST_PLAYED_NAME +" TEXT," +KEY_STATS_LAST_PLAYED_NAME+" TEXT, "+ KEY_STATS_OCCURRENCES+ " INTEGER);";
        conn.execSQL(CREATE_STATS);

    }
    /**
     * _->_
     * Populating the DB with all homescreen pictures
     */
    private void initNewData() {

        SharedPreferences sharedPreferences = context.getSharedPreferences("USED_ORDER", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("num", 1);
        editor.apply();

        List<String> files = listf("/storage/emulated/0/Homescreen");

        for (String file : files) {
            ContentValues values = new ContentValues();
            String[] splitPath = file.split("/");
            String fileName = splitPath[splitPath.length - 1];
            String artistName = splitPath[splitPath.length - 2];

            if (fileName.contains(".")) {
                fileName = fileName.split("\\.")[0];

            }

            values.put(KEY_PATH, file.trim());
            values.put(KEY_NAME, fileName.trim());
            values.put(KEY_ARTIST, artistName.trim());
            values.put(KEY_USED, false);

            try {
                conn.insert(TABLE_NAME, null, values);

            } catch (SQLiteConstraintException e) {
                Log.d("WALLPAPER INIT DATA", "trying to add an existing path");
            }
        }
    }

    /**
     * String  -> List<String>
     * Lists all the files within the given path
     *
     * @param directoryName -> The Top folder to search
     * @return List<String> resultList -> list with all the paths of files in the directory and subdirectories
     */
    private List<String> listf(String directoryName) {
        List<String> resultList = new ArrayList<>();
        Path dir = Paths.get(directoryName);
        try (Stream<Path> stream = Files.walk(dir, Integer.MAX_VALUE).filter(Files::isRegularFile)) {
            resultList = stream.map(String::valueOf).sorted().collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    /**
     * String -> List<Photo>
     * Checks the DB for all available photos (AKA where used is set to false / 0).
     * If available allows only different artist than the latest artist.
     * Handles resetting the DB and sending notification when needed
     *
     * @param artist -> previous day album artist
     * @return List<Photo> photoList -> A list object with all the possible photos
     */
    public List<Photo> availablePhotos(String artist, Boolean checkForStatus) {
        List<Photo> photoList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_USED + " = 0";
        Cursor cursor = conn.rawQuery(selectQuery, null);

        int rowsCount = cursor.getCount();
        if (cursor.moveToFirst()) {
            do {
                String path = cursor.getString(1);
                String name = cursor.getString(2);
                String rowArtist = cursor.getString(3);
                if (artist.equals("") || !rowArtist.equals(artist)) {
                    Photo photo = new Photo(path, name, rowArtist);
                    photoList.add(photo);
                }

            } while (cursor.moveToNext());
        }

        if (rowsCount <= 5 && checkForStatus) {

            SharedPreferences sharedPreferences = context.getSharedPreferences("ROUND_NAME", Context.MODE_PRIVATE);
            String name = sharedPreferences.getString("name", "Round");

            if (name.equals("Round") || !name.toLowerCase().contains("round")) {
                sendNewRoundNotification();
            }

            if (rowsCount <= 0) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("name", "Round");
                editor.putString("current_round",name);
                editor.apply();

                util.sendRequest("new", name,context);

                resetDB();

                photoList = availablePhotos("",true);
            }
        }

        cursor.close();

        if (photoList.size() == 0) {

            photoList = availablePhotos("",true);
        }

        return photoList;
    }

    /**
     * _->Photo
     * Handles everything that involves interacting with the DB. picks the new image and returns it to service method
     *
     * @return photo -  the new photo to set as the wallpaper
     */
    public Photo getNewPhoto() {
        SharedPreferences sp = context.getSharedPreferences("currentAlbum", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        String artistName = sp.getString("ARTIST", "");

        List<Photo> photoList = this.availablePhotos(artistName,true);
        Random rnd = new Random();

        int index = rnd.nextInt(photoList.size());
        Photo photo = photoList.get(index);

        editor.putString("PATH", photo.getPath());
        editor.putString("NAME", photo.getName());
        editor.putString("ARTIST", photo.getArtist());
        editor.apply();

        int used_order = getPlace(true);
        ContentValues values = new ContentValues();

        Date dt = new Date();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
        String date = LocalDateTime.from(dt.toInstant().atZone(ZoneId.of("Israel"))).plusDays(1).format(formatter);
        values.put(KEY_USED, true);
        values.put(KEY_ORDER, used_order);
        values.put(KEY_DATE, date);
        conn.update(TABLE_NAME, values, KEY_PATH + " =?", new String[]{photo.getPath()});

        sp = context.getSharedPreferences("ROUND_NAME",Context.MODE_PRIVATE);
        updateStats(photo.getName(), date, sp.getString("current_round","Twelfth Round (Day)"));

        return photo;
    }

    /**
     * _->_
     * Resetting the DB and initialize it
     */
    public void resetDB() {
        onUpgrade(conn, 1, 1);
        initNewData();
    }

    /**
     * Boolean -> Int
     * get the album number from the SP
     *
     * @param update -> Whether or not to also update the value (++)
     * @return used_order -> the current used place
     */
    public int getPlace(Boolean update) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("USED_ORDER", Context.MODE_PRIVATE);
        int used_order = sharedPreferences.getInt("num", 1);
        if (update) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("num", used_order + 1);
            editor.apply();
        }
        return used_order;
    }

    /**
     * _->_
     * Parse the data from the relevant gKeep note. Inserts it into the DB
     */
    public void restoreDB() {

        conn.execSQL("UPDATE " + TABLE_NAME + " SET " + KEY_USED + " = 0");
        conn.execSQL("UPDATE " + TABLE_NAME + " SET " + KEY_ORDER + " = null");
        conn.execSQL("UPDATE " + TABLE_NAME + " SET " + KEY_DATE + " = null");

        String data = util.sendRequest("restore", "",context);
        if (!data.equals("ERROR")) {
            String[] splitData = data.split("\n");
            int i = 0;
            if (splitData.length > 0) {
                String[] strippedString;
                int place;
                String name = null;
                String date;

                for (String album : splitData) {
                    if (!album.equals("")) {
                        ContentValues values = new ContentValues();

                        strippedString = album.split("\\)");

                        place = Integer.parseInt(strippedString[0].trim());

                        strippedString = strippedString[1].split("-");
                        name = strippedString[0].trim();
                        date = strippedString[1].trim();

                        values.put(KEY_ORDER, place);
                        values.put(KEY_USED, true);
                        values.put(KEY_DATE, date);
                        int rows = conn.update(TABLE_NAME, values, KEY_NAME + " =?", new String[]{name});
                        if (rows == 0) {
                            Log.e("Mismatch name", name);
                        }
                        i += 1;
                    }
                }

                util.changeWallpaper(context, name, false);
            }
            SharedPreferences sharedPreferences = context.getSharedPreferences("USED_ORDER", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("num", i);
            editor.apply();
        }
    }

    public void restoreStats(){
        conn.execSQL("DROP TABLE IF EXISTS " + TABLE_STATS_TABLE_NAME);
        resetStats();
        final String prevData = util.sendRequest("restore","yes",context);

        try {
            JSONObject jsonData = new JSONObject(prevData);
            Iterator<String>  keys= jsonData.keys();

            while(keys.hasNext()){
                String key = keys.next();

                final String[] roundData= jsonData.get(key).toString().split("\n");
                for (String album: roundData){
                    if(!album.equals("")) {
                        String[] withoutNumber = null;
                        try {
                            withoutNumber = album.split("\\)")[1].split("-");
                        } catch (Exception e) {
                            Log.e("Current album: ", album);
                        }

                        String name = withoutNumber[0].trim();
                        String date = withoutNumber[1];
                        updateStats(name,date,key);
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void updateStats(String name, String date,String key){

        if (conn.rawQuery("SELECT * FROM " + TABLE_STATS_TABLE_NAME + " WHERE " + KEY_NAME + " = ?", new String[]{name}).getCount() == 0) {
            ContentValues values = new ContentValues();

            values.put(KEY_NAME, name);
            values.put(KEY_STATS_LAST_PLAYED, date);
            values.put(KEY_STATS_FIRST_PLAYED, date);
            values.put(KEY_STATS_OCCURRENCES, 1);
            values.put(KEY_STATS_FIRST_PLAYED_NAME, key);
            values.put(KEY_STATS_LAST_PLAYED_NAME, key);
            conn.insert(TABLE_STATS_TABLE_NAME, null, values);
        } else {
            conn.execSQL("UPDATE " + TABLE_STATS_TABLE_NAME + " SET " + KEY_STATS_OCCURRENCES + " = " + KEY_STATS_OCCURRENCES + " +1 WHERE " + KEY_NAME + " = ?", new String[]{name});
            conn.execSQL("UPDATE " + TABLE_STATS_TABLE_NAME + " SET " + KEY_STATS_FIRST_PLAYED + " = ?, " + KEY_STATS_FIRST_PLAYED_NAME + "=? WHERE " + KEY_NAME + " = ? AND " + KEY_STATS_FIRST_PLAYED_NAME + "> ?", new String[]{date, key, name, date});
            conn.execSQL("UPDATE " + TABLE_STATS_TABLE_NAME + " SET " + KEY_STATS_LAST_PLAYED + " = ?," + KEY_STATS_LAST_PLAYED_NAME + "=? WHERE " + KEY_NAME + " = ? AND " + KEY_STATS_LAST_PLAYED + " < ?", new String[]{date, key, name, date});
        }
    }
    /**
     * _->_
     * Sends notification if the next round name has yet to been set
     * Support quick reply from the notification
     */
    public void sendNewRoundNotification() {
        final String KEY_ROUND_NAME = "next_round_name";

        Intent notificationIntent = new Intent("com.saar.wallpaperchanger.roundnoitification", null, context, NotificationReceiver.class);

        notificationIntent.putExtra("NAME", "Round");

        String nameLabel = "Pick new name";
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_ROUND_NAME).setLabel(nameLabel).build();
        PendingIntent namePendingIntent = PendingIntent.getBroadcast(context, 1, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.notificationaddname, nameLabel, namePendingIntent)
                .addRemoteInput(remoteInput)
                .build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "Set Round Name")
                .setContentTitle("I think you have forgotten something...")
                .setSmallIcon(R.drawable.notificationicon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Hello! you need to set the next round name for the love of god please do it or else!"))
                .addAction(action);


        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(1, builder.build());
    }

    /**
     * used for the search auto-complete
     *
     * @return List<String> names  a list of all the albums names in the DB
     */
    public List<String> getAllNames() {
        Cursor cursor = conn.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        List<String> names = new ArrayList<>();

        if (cursor.moveToFirst()) {
            names.add(cursor.getString(cursor.getColumnIndex(KEY_NAME)));
            while (cursor.moveToNext()) {
                names.add(cursor.getString(cursor.getColumnIndex(KEY_NAME)));
            }
        }
        cursor.close();

        return names;

    }

    /**
     * Given an album name returns the date that it was played
     *
     * @param name the album name to search
     * @return date the date it was plated on
     */
    public String getAlbumDate(String name) {
        String date = name + " has yet to been played";
        Cursor cursor = conn.rawQuery("select DATE from photos where NAME =?", new String[]{name});

        if (cursor.moveToFirst()) {
            date = cursor.getString(0);
        }
        cursor.close();
        if (date == null) {
            date = name + " has yet to been played";
        }
        return date;
    }

    /**
     * Given an album names returns all of its data
     *
     * @param album - album name to search
     * @return photo object containing all of the data
     */
    public Photo getAlbumData(String album) {
        Cursor cursor = conn.rawQuery("select * from " + TABLE_NAME + " where " + KEY_NAME + " =?", new String[]{album});
        Photo albumData = null;

        if (cursor.moveToFirst()) {
            String path = cursor.getString(1);
            String name = cursor.getString(2);
            String artist = cursor.getString(3);
            albumData = new Photo(path, name, artist);
        }

        cursor.close();
        return albumData;
    }

    public long getRowsCount() {
        return DatabaseUtils.queryNumEntries(conn, TABLE_NAME);
    }

    /**
     * used for the search auto-complete-artist
     *
     * @return List<String> names  a list of all the artist names in the DB
     */
    public List<String> getAllArtistNames() {
        Cursor cursor = conn.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        HashSet<String> names = new HashSet<>();

        if (cursor.moveToFirst()) {
            names.add(cursor.getString(cursor.getColumnIndex(KEY_ARTIST)));
            while (cursor.moveToNext()) {
                names.add(cursor.getString(cursor.getColumnIndex(KEY_ARTIST)));
            }
        }
        cursor.close();

        return new ArrayList<>(names);

    }

    public String searchByArtist(String artist) {
        int totalAmount = conn.rawQuery("select * from " + TABLE_NAME + " where " + KEY_ARTIST + " =?", new String[]{artist}).getCount();

        int usedAmount = conn.rawQuery("select * from " + TABLE_NAME + " where " + KEY_ARTIST + " =? and " + KEY_USED + " = 1", new String[]{artist}).getCount();

        return usedAmount + " / " + totalAmount;

    }

    public List<Photo> getAllAlbumsByArtist(String artist) {
        Cursor cursor = conn.rawQuery("SELECT " + KEY_NAME + "," + KEY_DATE + "  FROM " + TABLE_NAME + " WHERE " + KEY_ARTIST + " =?", new String[]{artist});

        List<Photo> albumsList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            albumsList.add(new Photo(cursor.getString(1), cursor.getString(0)));
            while (cursor.moveToNext()) {
                albumsList.add(new Photo(cursor.getString(1), cursor.getString(0)));
            }

        }
        cursor.close();
        return albumsList;

    }


}

