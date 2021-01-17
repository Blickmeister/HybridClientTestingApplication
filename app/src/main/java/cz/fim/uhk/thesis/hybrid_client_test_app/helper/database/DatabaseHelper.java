package cz.fim.uhk.thesis.hybrid_client_test_app.helper.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "library_management.db";
    private static final String TABLE_NAME = "library";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME + "(library_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "library_name TEXT NOT NULL UNIQUE," +
                "library_apk_name TEXT NOT NULL UNIQUE," +
                "library_main_class TEXT NOT NULL," +
                "library_desc TEXT" +
                "library_is_active NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS library");
        onCreate(db);
    }

    public boolean insertData(String name, String apkName, String mainClass, String description, boolean isActive) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("library_name", name);
        contentValues.put("library_apk_name", apkName);
        contentValues.put("library_main_class", mainClass);
        contentValues.put("library_desc", description);
        contentValues.put("library_is_active", isActive);
        long result = db.insert(TABLE_NAME,null ,contentValues);
        return result != -1;
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("select * from " + TABLE_NAME,null);
    }

    public String getApkName(String libraryName) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_NAME + " where library_name = '"+ libraryName + "'",
                null);
        res.moveToFirst(); // library_name je unikátní (vždy jen jeden výsledek)
        return res.getString(2);
    }

    public String getClassName(String libraryName) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_NAME + " where library_name = '"+ libraryName + "'",
                null);
        res.moveToFirst(); // library_name je unikátní (vždy jen jeden výsledek)
        return res.getString(3);
    }
}
