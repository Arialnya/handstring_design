package com.example.handstring.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.handstring.model.HealthData;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HealthDataDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "health_data.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_HEALTH_DATA = "health_data";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_HEART_RATE = "heart_rate";
    private static final String COLUMN_BLOOD_OXYGEN = "blood_oxygen";
    private static final String COLUMN_BRIGHTNESS = "brightness";
    private static final String COLUMN_DEEP_SLEEP = "deep_sleep";
    private static final String COLUMN_LIGHT_SLEEP = "light_sleep";
    private static final String COLUMN_AWAKE = "awake";

    public HealthDataDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_HEALTH_DATA + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TIMESTAMP + " INTEGER,"
                + COLUMN_HEART_RATE + " INTEGER,"
                + COLUMN_BLOOD_OXYGEN + " INTEGER,"
                + COLUMN_BRIGHTNESS + " INTEGER,"
                + COLUMN_DEEP_SLEEP + " INTEGER,"
                + COLUMN_LIGHT_SLEEP + " INTEGER,"
                + COLUMN_AWAKE + " INTEGER"
                + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_HEALTH_DATA + " ADD COLUMN " + COLUMN_BLOOD_OXYGEN + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_HEALTH_DATA + " ADD COLUMN " + COLUMN_BRIGHTNESS + " INTEGER DEFAULT 0");
        }
    }

    public long insertHealthData(HealthData data) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TIMESTAMP, data.getTimestamp().getTime());
        values.put(COLUMN_HEART_RATE, data.getHeartRate());
        values.put(COLUMN_BLOOD_OXYGEN, data.getBloodOxygen());
        values.put(COLUMN_BRIGHTNESS, data.getBrightness());
        values.put(COLUMN_DEEP_SLEEP, data.getDeepSleepMinutes());
        values.put(COLUMN_LIGHT_SLEEP, data.getLightSleepMinutes());
        values.put(COLUMN_AWAKE, data.getAwakeMinutes());
        return db.insert(TABLE_HEALTH_DATA, null, values);
    }

    public List<HealthData> getAllHealthData() {
        List<HealthData> dataList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_HEALTH_DATA;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                HealthData data = new HealthData();
                data.setId(cursor.getLong(0));
                data.setTimestamp(new Date(cursor.getLong(1)));
                data.setHeartRate(cursor.getInt(2));
                data.setBloodOxygen(cursor.getInt(3));
                data.setBrightness(cursor.getInt(4));
                data.setDeepSleepMinutes(cursor.getInt(5));
                data.setLightSleepMinutes(cursor.getInt(6));
                data.setAwakeMinutes(cursor.getInt(7));
                dataList.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return dataList;
    }

    public List<HealthData> getHealthDataByDateRange(Date startDate, Date endDate) {
        List<HealthData> dataList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_HEALTH_DATA + 
                           " WHERE " + COLUMN_TIMESTAMP + " BETWEEN ? AND ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, 
            new String[]{String.valueOf(startDate.getTime()), String.valueOf(endDate.getTime())});

        if (cursor.moveToFirst()) {
            do {
                HealthData data = new HealthData();
                data.setId(cursor.getLong(0));
                data.setTimestamp(new Date(cursor.getLong(1)));
                data.setHeartRate(cursor.getInt(2));
                data.setBloodOxygen(cursor.getInt(3));
                data.setBrightness(cursor.getInt(4));
                data.setDeepSleepMinutes(cursor.getInt(5));
                data.setLightSleepMinutes(cursor.getInt(6));
                data.setAwakeMinutes(cursor.getInt(7));
                dataList.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return dataList;
    }
} 