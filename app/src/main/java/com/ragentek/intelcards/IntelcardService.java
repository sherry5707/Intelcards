package com.ragentek.intelcards;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.ragentek.intelcards.provider.DBUtils;
import com.ragentek.intelcards.provider.IntelCardContract;
import com.ragentek.intelcards.ui.IntelCardListActivity;
import com.ragentek.intelcards.ui.IntelCardQuery;

import org.json.JSONException;

import cn.com.xy.sms.sdk.ui.card.CardDataEntity;
import cn.com.xy.sms.sdk.ui.cardservice.DuoquCardService;

import static com.ragentek.intelcards.SmsContentObserver.FROM;
import static com.ragentek.intelcards.SmsContentObserver.SET_DELETE_ALARM;
import static com.ragentek.intelcards.SmsContentObserver.SET_IGNORE_ALARM;
import static com.ragentek.intelcards.SmsContentObserver.SET_SHOW_ALARM;
import static com.ragentek.intelcards.SmsContentObserver.RESET_ALRAM;
import static com.ragentek.intelcards.SmsContentObserver.SHOW_DETAIL;
import static com.ragentek.intelcards.SmsContentObserver.setAlarmTime;

/**
 * Created by zhaoxue.meng on 2017/6/26.
 */

public class IntelcardService extends Service {
    private static final String TAG = "IntelcardService";
    private SmsContentObserver smsContentObserver;
    private long lastRemoveTime;
    private long lastShowTime;
    private static final long PERIOD = 500;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "IntelcardService oncreate");
        // 在这里启动
        Handler mHandler = new Handler();
        smsContentObserver = new SmsContentObserver(mHandler, this, SmsContentObserver.MSG_SMS_WHAT);
        getContentResolver().registerContentObserver(Uri.parse("content://sms"), true, smsContentObserver);
        //updateCard:
        DuoquCardService.addDataListener(this, new DuoquCardService.DataListener() {
            @Override
            public void onCardUpdate(CardDataEntity cardDataEntity) {
                Log.i(TAG, "onCreate-onCardUpdate,source_id:"+cardDataEntity.mId);
                DBUtils.updateCardInDB(IntelcardService.this,cardDataEntity);
                if(DBUtils.getShowValue(IntelcardService.this,cardDataEntity.mId)!=0){
                    SmsContentObserver.setAlarmTime(IntelcardService.this, cardDataEntity.mAppearTime, cardDataEntity.mId, SET_SHOW_ALARM);
                }
                Intent updateWidgetIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                IntelcardService.this.sendBroadcast(updateWidgetIntent);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (intent != null) {

            int from = intent.getIntExtra(FROM, -1);
            switch (from) {
                case SET_SHOW_ALARM: {
                    Bundle bundle = intent.getExtras();
                    if (bundle == null) break;
                    int source_id = bundle.getInt(IntelCardContract.IntelCard.SOURCE_ID);
                    Log.i(TAG, "should show cards,update db and widget,source_id is " + source_id);
                    updateWidget(this, source_id, 1);
                    Log.i(TAG, "after show we set deadAlarm");
                    long deadTime = DBUtils.queryDeadTime(this, source_id);
                    if (deadTime == -1) break;
                    setAlarmTime(this, deadTime, source_id, SET_DELETE_ALARM);
                    //if the time between first request with second request less than 500ms,the second request not resolve
                    long nowtime = System.currentTimeMillis();
                    if (nowtime - lastShowTime < PERIOD) break;
                    lastShowTime = nowtime;
                    Intent updateWidgetIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    this.sendBroadcast(updateWidgetIntent);
                    break;
                }
                case SET_IGNORE_ALARM: {
                    Bundle bundle = intent.getExtras();
                    if (bundle == null) break;
                    int source_id = bundle.getInt(IntelCardContract.IntelCard.SOURCE_ID);
                    Log.i(TAG, "should ignore cards,update db and widget,source_id is " + source_id);
                    updateWidget(this, source_id, 0);
                    //if the time between first request with second request less than 500ms,the second request not resolve
                    long nowtime = System.currentTimeMillis();
                    if (nowtime - lastRemoveTime < PERIOD) break;
                    lastRemoveTime = nowtime;
                    Intent updateWidgetIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    this.sendBroadcast(updateWidgetIntent);
                    break;
                }
                case SET_DELETE_ALARM: {
                    //删除dead卡片 currentTime>deadTime
                    DBUtils.deleteDeadCard(this);
                    Bundle bundle = intent.getExtras();
                    if (bundle == null) break;
                    int source_id = bundle.getInt(IntelCardContract.IntelCard.SOURCE_ID);
                    DBUtils.deleteInDB(this,source_id);
                    break;
                }
                case RESET_ALRAM: {
                    Log.i(TAG, "update_widget");
                    //reset Alarm for db,if appearTime<currentTime<deadTime,set RemoveAlarm;
                    //if currentTime<appearTime,set ShowAlarm;if currentTime>deadTime,Remove
                    Cursor mCurosr = DBUtils.queryExceptZero(this);
                    if (mCurosr == null || mCurosr.getCount() == 0) break;
                    long currentTime = System.currentTimeMillis();
                    while (mCurosr.moveToNext()) {
                        long appearTime = mCurosr.getLong(mCurosr.getColumnIndex(IntelCardContract.IntelCard.ORDERTIME));
                        long deadTime = mCurosr.getLong(mCurosr.getColumnIndex(IntelCardContract.IntelCard.DEADTIME));
                        int source_id = mCurosr.getInt(mCurosr.getColumnIndex(IntelCardContract.IntelCard.SOURCE_ID));
                        if (currentTime > deadTime) {
                            Log.i(TAG, "currentTime" + currentTime + ",appearTime:" + appearTime + ",deadTime:" + deadTime + ",remove");
                            updateWidget(this, source_id, 0);
                        }
                        if (currentTime < appearTime) {
                            setAlarmTime(this, appearTime, source_id, SET_SHOW_ALARM);
                        }
                        if (currentTime > appearTime && currentTime < deadTime) {
                            //设置该source_id的alarm 是 一个即将展示卡片的alarm
                            Log.i(TAG, "currentTime" + currentTime + ",appearTime:" + appearTime + ",show");
                            setAlarmTime(this, deadTime, source_id, SET_DELETE_ALARM);
                        }
                    }
                    mCurosr.close();
                    Intent updateWidgetIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    this.sendBroadcast(updateWidgetIntent);
                    break;
                }
                case SHOW_DETAIL:{
                    Log.i(TAG,"show detail");
                    Bundle bundle = intent.getExtras();
                    if (bundle == null) break;
                    int source_id = bundle.getInt(IntelCardContract.IntelCard.SOURCE_ID);
                    try {
                        DuoquCardService.toWebPage(this, String.valueOf(source_id));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                default:
                    break;
            }
        }
        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "ondestroy");
        super.onDestroy();
        getContentResolver().unregisterContentObserver(smsContentObserver);
    }

    public static int updateWidget(Context context, int source_id, int showValue) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(IntelCardContract.IntelCard.SHOW, showValue);
        return DBUtils.setShowValue(context, source_id, contentValues);
    }
}
