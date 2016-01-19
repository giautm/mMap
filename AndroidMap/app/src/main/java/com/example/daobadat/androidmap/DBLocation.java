package com.example.daobadat.androidmap;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by daobadat on 1/17/2016.
 */

public class DBLocation extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "DBLocation.db";
    public static final String INFORMATIONS_TABLE_NAME = "locationInformations";
    public static final String INFORMATIONS_COLUMN_ID = "id";
    public static final String INFORMATIONS_COLUMN_NAME = "name";
    public static final String INFORMATIONS_COLUMN_LOCATION = "location";
    private HashMap hp;

    public DBLocation(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL("create table locationInformations "
                + "(id integer primary key, name text,location text)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS locationInformations");
        onCreate(db);
    }

    public boolean insertStudentInformations(String name, String location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("location", location);
        db.insert("locationInformations", null, contentValues);
        return true;
    }

    public Cursor getData(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from locationInformations where id=" + id + "", null);
        return res;
    }

    public int getIDFromPositonAdapter(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select id from locationInformations Limit 1 Offset " + id, null);
        res.moveToFirst();
        return Integer.parseInt(res.getString(res.getColumnIndex(INFORMATIONS_COLUMN_ID)));
    }

    public int numberOfRows() {
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, INFORMATIONS_TABLE_NAME);
        return numRows;
    }

    public Integer deleteLocationInformations(Integer id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("locationInformations", "id = ? ", new String[] { Integer.toString(id) });
    }

    public ArrayList<String> getAllLocationInformations() {
        ArrayList<String> array_list = new ArrayList<String>();

        // hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from locationInformations", null);
        res.moveToFirst();

        while (res.isAfterLast() == false) {
            array_list.add(res.getString(res.getColumnIndex(INFORMATIONS_COLUMN_NAME)));
            res.moveToNext();
        }
        return array_list;
    }

}
