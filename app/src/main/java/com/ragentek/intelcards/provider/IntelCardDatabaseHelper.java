/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ragentek.intelcards.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper class for opening the database from multiple providers.  Also provides
 * some common functionality.
 */
class IntelCardDatabaseHelper extends SQLiteOpenHelper {
    static final String TAG = "IntelCard.DBHelper";

    // Database and table names
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "intelcard.db";
    static final String INTELCARD_TABLE_NAME = "intelcard_table";

    private static void createIntelCardTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + INTELCARD_TABLE_NAME + " (" +
                IntelCardContract.IntelCardColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                IntelCardContract.IntelCardColumns.ORDERTIME + " LONG, " +
                IntelCardContract.IntelCardColumns.APPEARTIME + " LONG, " +
                IntelCardContract.IntelCardColumns.DEADTIME + " LONG, " +
                IntelCardContract.IntelCardColumns.SOURCE + " STRING, " +
                IntelCardContract.IntelCardColumns.SHOW + " INTEGER, " +
                IntelCardContract.IntelCardColumns.MSGID + " INTEGER, " +
                IntelCardContract.IntelCardColumns.CARD_TYPE + " INTEGER, " +
                IntelCardContract.IntelCardColumns.SOURCE_ID + " INTEGER NOT NULL DEFAULT -1);");
    }

    private static IntelCardDatabaseHelper instance = null;

    public static IntelCardDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new IntelCardDatabaseHelper(context);
        }
        return instance;
    }

    private IntelCardDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createIntelCardTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
