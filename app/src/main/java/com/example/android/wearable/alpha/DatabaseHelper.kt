package com.example.android.wearable.alpha

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // 테이블 생성
        val createTable = "CREATE TABLE menus (date TEXT PRIMARY KEY, breakfast TEXT, lunch TEXT)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 테이블 업그레이드
        db.execSQL("DROP TABLE IF EXISTS menus")
        onCreate(db)
    }

    fun insertOrUpdateMenu(date: String, breakfast: String, lunch: String) {
        val db = this.writableDatabase
        val query = "REPLACE INTO menus (date, breakfast, lunch) VALUES ('$date', '$breakfast', '$lunch')"
        db.execSQL(query)
    }

    fun deleteOldData() {
        val db = this.writableDatabase
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM.dd"))
        val query = "DELETE FROM menus WHERE date < '$today'"
        db.execSQL(query)
    }

    fun getMenu(): List<Pair<String, String>> {
        val db = this.readableDatabase
        val query = "SELECT date, breakfast, lunch FROM menus"
        val cursor = db.rawQuery(query, null)
        val menuList = mutableListOf<Pair<String, String>>()

        if (cursor.moveToFirst()) {
            do {
                val date = cursor.getString(0)
                val breakfast = cursor.getString(1)
                val lunch = cursor.getString(2)
                menuList.add(Pair("$date 아침 $breakfast", "$date 점심 $lunch"))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return menuList
    }

    fun getLastSavedDate(): LocalDate? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT MAX(date) FROM menus", null)
        var lastSavedDate: LocalDate? = null

        if (cursor.moveToFirst()) {
            val dateString = cursor.getString(0)
            if (!dateString.isNullOrEmpty()) {
                lastSavedDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("MM.dd"))
            }
        }
        cursor.close()
        return lastSavedDate
    }

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "lunchmenu.db"
    }
}
