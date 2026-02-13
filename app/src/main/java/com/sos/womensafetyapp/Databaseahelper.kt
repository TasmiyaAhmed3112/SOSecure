package com.sos.womensafetyapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.database.Cursor



class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "sos.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE emergency_contacts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                phone TEXT
            )
        """
        db.execSQL(createTable)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        db.execSQL("DROP TABLE IF EXISTS emergency_contacts")
        onCreate(db)
    }

    fun insertContact(name: String, phone: String) {
        val db = writableDatabase
        val values = ContentValues()

        values.put("name", name)
        values.put("phone", phone)

        db.insert("emergency_contacts", null, values)
    }
    fun deleteContact(id: Int): Int {
        val db = writableDatabase
        val result = db.delete("emergency_contacts", "id = ?", arrayOf(id.toString()))
        db.close()
        return result
    }


    fun getAllContacts(): Cursor {
        val db = readableDatabase
        return db.rawQuery("SELECT * FROM emergency_contacts", null)
    }
}
