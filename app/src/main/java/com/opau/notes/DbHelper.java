package com.opau.notes;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {


    public DbHelper(Context context) {
        super(context, "notes.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE folders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, color INTEGER)");
        db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, content TEXT, type TEXT, folder_id INTEGER)");
        db.execSQL("INSERT INTO folders (id, name) VALUES (-1, \"dummy\")");
    }


    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        //nothing...
    }
}