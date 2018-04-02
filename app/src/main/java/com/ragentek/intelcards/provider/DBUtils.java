package com.ragentek.intelcards.provider;

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.ragentek.intelcards.SmsContentObserver;
import com.ragentek.intelcards.ui.IntelCardQuery;

import cn.com.xy.sms.sdk.ui.card.CardDataEntity;
import cn.com.xy.sms.sdk.ui.cardservice.DuoquCardService;

import static com.ragentek.intelcards.provider.IntelCardContract.IntelCardColumns.APPEARTIME;
import static com.ragentek.intelcards.provider.IntelCardContract.IntelCardColumns.CONTENT_URI;
import static com.ragentek.intelcards.provider.IntelCardContract.IntelCardColumns.DEADTIME;
import static com.ragentek.intelcards.provider.IntelCardContract.IntelCardColumns.SHOW;
import static com.ragentek.intelcards.provider.IntelCardContract.IntelCardColumns.SOURCE_ID;

/**
 * Created by zhaoxue.meng on 2017/7/3.
 */

public class DBUtils {
    private static final String TAG = "DBUtils";

    public static void insertMsgsToDB(Context context, ContentValues contentValues) {
        Log.i(TAG, "insertMsgsToDB");
        if (hasBeenInserted(context, (Integer) contentValues.get(IntelCardContract.IntelCard.SOURCE_ID))) {
            Log.i(TAG, "this data has been inserted so db will be update");
            updateCardInDB(context,contentValues);
            return;
        }
        context.getContentResolver().insert(CONTENT_URI, contentValues);
    }

    public static void updateCardInDB(Context context,ContentValues contentValues){
        context.getContentResolver().update(CONTENT_URI, contentValues,
                IntelCardContract.IntelCard.SOURCE_ID + "=" + contentValues.get(IntelCardContract.IntelCard.SOURCE_ID),
                null);
    }

    public static void updateCardInDB(Context context,CardDataEntity cardDataEntity){
        ContentValues cv = new ContentValues();
        cv.put(IntelCardContract.IntelCard.SOURCE_ID, cardDataEntity.mId);
        cv.put(IntelCardContract.IntelCard.SOURCE, cardDataEntity.mContent);
        cv.put(IntelCardContract.IntelCard.DEADTIME, cardDataEntity.mDeadline);
        cv.put(IntelCardContract.IntelCard.ORDERTIME, cardDataEntity.mOrderTime);
        cv.put(IntelCardContract.IntelCard.APPEARTIME, cardDataEntity.mAppearTime);
        cv.put(IntelCardContract.IntelCard.MSGID, cardDataEntity.mMsgId);
        cv.put(IntelCardContract.IntelCard.SHOW,DBUtils.getShowValue(context,cardDataEntity.mId));
        cv.put(IntelCardContract.IntelCard.CARD_TYPE, SmsContentObserver.getType(cardDataEntity.mCardType));
        context.getContentResolver().update(CONTENT_URI, cv,
                IntelCardContract.IntelCard.SOURCE_ID + "=" + cv.get(IntelCardContract.IntelCard.SOURCE_ID),
                null);
    }

    public static int getShowValue(Context context,int source_id){
        Cursor cursor = context.getContentResolver().query(CONTENT_URI, null, SOURCE_ID + "=" + source_id, null, APPEARTIME);
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            int show = cursor.getInt(cursor.getColumnIndex(SHOW));
            cursor.close();
            return show;
        }
        return 2;
    }

    private static boolean hasBeenInserted(Context context, int cardID) {
        Cursor cursor = context.getContentResolver().query(CONTENT_URI,
                null, IntelCardContract.IntelCard.SOURCE_ID + "=?", new String[]{String.valueOf(cardID)}, null);
        int count = cursor == null ? 0 : cursor.getCount();
        if (cursor != null) {
            cursor.close();
        }
        return count > 0;
    }

    static Cursor queryForShow(Context context) {
        return context.getContentResolver().query(CONTENT_URI, IntelCardQuery.PROJECTION,
                IntelCardContract.IntelCard.SHOW + " = 1",
                null, IntelCardContract.IntelCard.ORDERTIME + "," + IntelCardContract.IntelCard.CARD_TYPE);
    }

    public static int setShowValue(Context context, int source_id, ContentValues cv) {
        Log.i(TAG, "setShowValue:" + source_id + ",cv:" + cv.get(SHOW));
        int count = context.getContentResolver().update(CONTENT_URI, cv, SOURCE_ID + " = " + source_id, null);
        return count;
    }

    public static void setAllShowValue(Context context, int value) {
        Log.i(TAG, "setAllShowNone");
        ContentValues cv = new ContentValues();
        cv.put(IntelCardContract.IntelCard.SHOW, value);
        context.getContentResolver().update(CONTENT_URI, cv, null, null);
    }

    public static void deleteDeadCard(Context context) {
        Log.i(TAG, "deleteDeadCard");
        long currentTime = System.currentTimeMillis();
        //context.getContentResolver().delete(CONTENT_URI, DEADTIME + " < " + currentTime, null);
        Cursor cursor = context.getContentResolver().query(CONTENT_URI, null, DEADTIME + " < " + currentTime, null, null);
        if (cursor == null || cursor.getCount() == 0) {
            return;
        }
        while (cursor.moveToNext()) {
            int source_id = cursor.getInt(cursor.getColumnIndex(SOURCE_ID));
            deleteInDB(context, source_id);
        }
        cursor.close();
    }

    public static long queryDeadTime(Context context, int source_id) {
        Log.i(TAG, "queryDeadTime");
        Cursor cursor = context.getContentResolver().query(CONTENT_URI, null, SOURCE_ID + "=" + source_id, null, APPEARTIME);
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            long deadTime = cursor.getLong(cursor.getColumnIndex(DEADTIME));
            cursor.close();
            return deadTime;
        }
        return -1;
    }

    public static int queryShowValue(Context context, int sourceId) {
        Cursor cursor = context.getContentResolver().query(CONTENT_URI, null, IntelCardContract.IntelCard.SOURCE_ID + "=" + sourceId, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int success = cursor.getInt(cursor.getColumnIndex(IntelCardContract.IntelCard.SHOW));
            cursor.close();
            return success;
        }
        return -1;
    }

    public static Cursor queryExceptZero(Context context) {
        Log.i(TAG, "queryExceptZero");
        return context.getContentResolver().query(CONTENT_URI, null, SHOW + "<>" + 0, null, APPEARTIME);
    }

    public static void deleteInDB(final Context context, int source_id) {
        Log.i(TAG, "deleteInDB:source_id" + source_id);
        context.getContentResolver().delete(CONTENT_URI, SOURCE_ID + "=" + source_id, null);
        DuoquCardService.deleteCardDataById(context, String.valueOf(source_id), new DuoquCardService.CardCallback() {
            @Override
            public void onSuccess(int i, Object o) {
                Log.i(TAG, "delete from xiaoyuan,what:" + i);
                //if the time between first request with second request less than 30s,the second request not resolve
                Intent updateWidgetIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                context.sendBroadcast(updateWidgetIntent);
            }

            @Override
            public void onFailed(int i) {
                Log.e(TAG, "delete fail");
            }
        });
    }

    public static void deleteAll(Context context) {
        context.getContentResolver().delete(CONTENT_URI, null, null);
    }
}
