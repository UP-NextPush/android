package org.unifiedpush.distributor.nextpush.distributor

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DB_NAME = "apps_db"
private const val DB_VERSION = 1

private const val TABLE_APPS = "apps"
private const val FIELD_PACKAGE_NAME = "packageName"
private const val FIELD_CONNECTOR_TOKEN = "connectorToken"
private const val FIELD_APP_TOKEN = "appToken"
private const val CREATE_TABLE_APPS = "CREATE TABLE apps (" +
        "$FIELD_PACKAGE_NAME TEXT," +
        "$FIELD_CONNECTOR_TOKEN TEXT," +
        "$FIELD_APP_TOKEN TEXT," +
        "PRIMARY KEY ($FIELD_CONNECTOR_TOKEN));"

class MessagingDatabase(context: Context):
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_APPS)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        throw IllegalStateException("Upgrades not supported")
    }

    fun registerApp(packageName: String, connectorToken: String, appToken: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(FIELD_PACKAGE_NAME, packageName)
            put(FIELD_CONNECTOR_TOKEN, connectorToken)
            put(FIELD_APP_TOKEN, appToken)
        }
        db.insert(TABLE_APPS, null, values)
    }

    fun unregisterApp(connectorToken: String) {
        val db = writableDatabase
        val selection = "$FIELD_CONNECTOR_TOKEN = ?"
        val selectionArgs = arrayOf(connectorToken)
        db.delete(TABLE_APPS, selection, selectionArgs)
    }

    fun isRegistered(packageName: String, connectorToken: String): Boolean {
        val db = readableDatabase
        val selection = "$FIELD_PACKAGE_NAME = ? AND $FIELD_CONNECTOR_TOKEN = ?"
        val selectionArgs = arrayOf(packageName, connectorToken)
        return db.query(
                TABLE_APPS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        ).use { cursor ->
            (cursor != null && cursor.count > 0)
        }
    }

    fun getPackageName(connectorToken: String): String {
        val db = readableDatabase
        val projection = arrayOf(FIELD_PACKAGE_NAME)
        val selection = "$FIELD_CONNECTOR_TOKEN = ?"
        val selectionArgs = arrayOf(connectorToken)
        return db.query(
            TABLE_APPS,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        ).use { cursor ->
            val column = cursor.getColumnIndex(FIELD_PACKAGE_NAME)
            if (cursor.moveToFirst() && column >= 0) cursor.getString(column) else ""
        }
    }

    fun getAppToken(connectorToken: String): String {
        val db = readableDatabase
        val projection = arrayOf(FIELD_APP_TOKEN)
        val selection = "$FIELD_CONNECTOR_TOKEN = ?"
        val selectionArgs = arrayOf(connectorToken)
        return db.query(
            TABLE_APPS,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        ).use { cursor ->
            val column = cursor.getColumnIndex(FIELD_APP_TOKEN)
            if (cursor.moveToFirst() && column >= 0) cursor.getString(column) else ""
        }
    }

    fun getConnectorToken(appToken: String): String {
        val db = readableDatabase
        val projection = arrayOf(FIELD_CONNECTOR_TOKEN)
        val selection = "$FIELD_APP_TOKEN = ?"
        val selectionArgs = arrayOf(appToken)
        return db.query(
            TABLE_APPS,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        ).use { cursor ->
            val column = cursor.getColumnIndex(FIELD_CONNECTOR_TOKEN)
            if (cursor.moveToFirst() && column >= 0) cursor.getString(column) else ""
        }
    }

    fun listTokens(): List<String> {
        val db = readableDatabase
        val projection = arrayOf(FIELD_CONNECTOR_TOKEN)
        return db.query(
            TABLE_APPS,
            projection,
            null,
            null,
            null,
            null,
            null
        ).use{ cursor ->
            generateSequence { if (cursor.moveToNext()) cursor else null }
                .mapNotNull{
                    val column = cursor.getColumnIndex(FIELD_CONNECTOR_TOKEN)
                    if (column >= 0) it.getString(column) else null }
                .toList()
        }
    }
}
