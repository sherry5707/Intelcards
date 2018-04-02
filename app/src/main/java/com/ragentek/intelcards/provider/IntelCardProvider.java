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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

public class IntelCardProvider extends ContentProvider {
    private static final String TAG = "IntelCard.Provider";

    private static final int INTELCARD = 1;
    private static final int INTELCARD_ID = 2;
    private Context mContext;

    private static final UriMatcher sURLMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURLMatcher.addURI(IntelCardContract.AUTHORITY, "intelcards", INTELCARD);
        sURLMatcher.addURI(IntelCardContract.AUTHORITY, "intelcards/#", INTELCARD_ID);
    }

    public IntelCardProvider() {
    }

    @Override
    public boolean onCreate() {
        mContext = getContext();
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projectionIn, String selection, String[] selectionArgs,
                        String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        // Generate the body of the query
        int match = sURLMatcher.match(uri);
        switch (match) {
            case INTELCARD:
                qb.setTables(IntelCardDatabaseHelper.INTELCARD_TABLE_NAME);
                if (TextUtils.isEmpty(sort)) {
                    sort = IntelCardContract.IntelCardColumns.ORDERTIME + " ASC";
                }
                break;
            case INTELCARD_ID:
                qb.setTables(IntelCardDatabaseHelper.INTELCARD_TABLE_NAME);
                qb.appendWhere(IntelCardContract.IntelCardColumns._ID + "=");
                qb.appendWhere(uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }

        SQLiteDatabase db = IntelCardDatabaseHelper.getInstance(getContext()).getReadableDatabase();
        Cursor ret = qb.query(db, projectionIn, selection, selectionArgs,
                null, null, sort);

        if (ret == null) {
            Log.e(TAG, "query failed");
        } else if (getContext().getContentResolver() != null) {
            ret.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return ret;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        int match = sURLMatcher.match(uri);
        switch (match) {
            case INTELCARD:
                return "vnd.ragentek.cursor.dir/intelcards";
            case INTELCARD_ID:
                return "vnd.ragentek.cursor.item/intelcards";
            default:
                throw new IllegalArgumentException("Unknown URL");
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String where, String[] whereArgs) {
        int count;
        SQLiteDatabase db = IntelCardDatabaseHelper.getInstance(getContext()).getWritableDatabase();
        switch (sURLMatcher.match(uri)) {
            case INTELCARD:
                count = db.update(IntelCardDatabaseHelper.INTELCARD_TABLE_NAME, values, where, whereArgs);
                break;
            case INTELCARD_ID:
                String cardId = uri.getLastPathSegment();
                count = db.update(IntelCardDatabaseHelper.INTELCARD_TABLE_NAME, values,
                        IntelCardContract.IntelCardColumns._ID + "=" + cardId,
                        null);
                break;
            default: {
                throw new UnsupportedOperationException(
                        "Cannot update URL: " + uri);
            }
        }
        mContext.getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        long rowId;
        SQLiteDatabase db = IntelCardDatabaseHelper.getInstance(getContext()).getWritableDatabase();
        switch (sURLMatcher.match(uri)) {
            case INTELCARD:
                rowId = db.insert(IntelCardDatabaseHelper.INTELCARD_TABLE_NAME, null, initialValues);
                break;
            default:
                throw new IllegalArgumentException("Cannot insert from URL: " + uri);
        }
        Log.d(TAG, "insert url " + uri + "; rowId = " + rowId);

        Uri uriResult = ContentUris.withAppendedId(IntelCardContract.IntelCardColumns.CONTENT_URI, rowId);
        if (getContext().getContentResolver() != null) {
            getContext().getContentResolver().notifyChange(uriResult, null);
        }
        return uriResult;
    }

    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        int count;
        String primaryKey;
        SQLiteDatabase db = IntelCardDatabaseHelper.getInstance(getContext()).getWritableDatabase();
        switch (sURLMatcher.match(uri)) {
            case INTELCARD:
                count = db.delete(IntelCardDatabaseHelper.INTELCARD_TABLE_NAME, where, whereArgs);
                break;
            case INTELCARD_ID:
                primaryKey = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where)) {
                    where = IntelCardContract.IntelCardColumns._ID + "=" + primaryKey;
                } else {
                    where = IntelCardContract.IntelCardColumns._ID + "=" + primaryKey +
                            " AND (" + where + ")";
                }
                count = db.delete(IntelCardDatabaseHelper.INTELCARD_TABLE_NAME, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Cannot delete from URL: " + uri);
        }
        Log.d(TAG, "delete url " + uri + "; count = " + count);
        if (getContext().getContentResolver() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }
}
