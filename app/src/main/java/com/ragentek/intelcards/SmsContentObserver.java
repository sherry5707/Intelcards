package com.ragentek.intelcards;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.ragentek.intelcards.provider.DBUtils;
import com.ragentek.intelcards.provider.IntelCardContract;

import java.util.ArrayList;
import java.util.List;

import cn.com.xy.sms.sdk.ui.card.CardDataCallBack;
import cn.com.xy.sms.sdk.ui.card.CardDataEntity;
import cn.com.xy.sms.sdk.ui.cardservice.DuoquCardService;

/**
 * Created by zhaoxue.meng on 2017/6/26.
 */

public class SmsContentObserver extends ContentObserver {
    private static final String TAG = "SmsContentObserver";
    public static final String FROM = "from";
    public static final String TEST_RECEIVER = "com.ragentek.intelcards.alarm.action";
    public static final String TEST_CLEAR_SHOW_RECEIVER = "com.ragentek.intelcards.clearshow.action.test";
    public static final String TEST_SHOWALL = "com.ragentek.intelcards.alarm.action2";
    public static final String TEST_DELETEALL = "com.ragentek.intelcards.alarm.action3";
    public static final int SET_SHOW_ALARM = 0;
    public static final int SET_IGNORE_ALARM = 1;
    public static final int TEST_REMOVE_ALARM = 2;
    public static final int RESET_ALRAM = 3;
    public static final int SET_DELETE_ALARM =4;
    public static final int SHOW_DETAIL=5;
    //type
    public static final String [] TYPE={"airplane","train","ship","movie","hotel","hydropowercoal","express","credit","bank","car",
            "groupbuy","operator"};
    private final Context mContext;
    /**
     * 观察所有内容
     */
    public static final int MSG_SMS_WHAT = 1;
    /**
     * 仅观察收件箱
     */
    public static final int MSG_SMS_INBOX_WHAT = 2;
    private Uri privios_uri;
    private Looper mHandlerLooper;
    private ParseMsgHandler mParseMsgHandler;

    public SmsContentObserver(Handler handler, Context context, int observerType) {
        super(handler);
        this.mContext = context;
        //init handlerThread
        HandlerThread thread = new HandlerThread("ParseMessage");
        thread.start();
        mHandlerLooper = thread.getLooper();
        mParseMsgHandler = new ParseMsgHandler(mHandlerLooper);
    }

    private class ParseMsgHandler extends Handler{
        public ParseMsgHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Uri uri= (Uri) msg.obj;
            Log.i(TAG, "r:" + uri);
            Cursor cursor = mContext.getContentResolver().query(uri, new String[]{"_id", "address", "body", "type", "date"}, null, null, "date desc");
            if (cursor != null&&cursor.moveToFirst()) {//最后收到的短信在第一条. This method will return false if the cursor is empty
                int msgId = cursor.getInt(cursor.getColumnIndex("_id"));
                final String msgAddr = cursor.getString(cursor.getColumnIndex("address"));
                String msgBody = cursor.getString(cursor.getColumnIndex("body"));
                String msgDate = cursor.getString(cursor.getColumnIndex("date"));
                CardDataEntity cde = new CardDataEntity();
                cde.mMsgId = String.valueOf(msgId);
                cde.mContent = msgBody;
                cde.mSmsReciveTime = Long.parseLong(msgDate);
                cde.mPhoneNum = msgAddr;
                final List<CardDataEntity> msgs = new ArrayList<>();
                msgs.add(cde);

                DuoquCardService.parseMessageCard(mContext, new CardDataCallBack() {
                    @Override
                    public void execute(Object... objects) {
                        int result = -1;
                        try {
                            result = Integer.parseInt(objects == null ? "" : objects[0].toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (result == 0) {
                            Log.e(TAG, "解析失败");
                        }
                        if (result == 1) {
                            Log.i(TAG, "解析完成");
                            List<CardDataEntity> e = (List<CardDataEntity>) objects[1];
                            if(e!=null) {
                                Log.i(TAG,"解析出了"+e.size()+"条短信");
                                ContentValues cv = new ContentValues();
                                for (CardDataEntity i : e) {
                                    if(i.mDeleteFlag==1){
                                        Log.i(TAG,"删除了短信"+i.mId);
                                        DBUtils.deleteInDB(mContext,i.mId);
                                        continue;
                                    }
                                    cv.clear();
                                    cv.put(IntelCardContract.IntelCard.SOURCE_ID, i.mId);
                                    cv.put(IntelCardContract.IntelCard.SOURCE, i.mContent);
                                    cv.put(IntelCardContract.IntelCard.DEADTIME, i.mDeadline);
                                    cv.put(IntelCardContract.IntelCard.ORDERTIME, i.mOrderTime);
                                    cv.put(IntelCardContract.IntelCard.APPEARTIME, i.mAppearTime);
                                    cv.put(IntelCardContract.IntelCard.MSGID, i.mMsgId);
                                    cv.put(IntelCardContract.IntelCard.SHOW,DBUtils.getShowValue(mContext,i.mId));
                                    cv.put(IntelCardContract.IntelCard.CARD_TYPE,getType(i.mCardType));
                                    DBUtils.insertMsgsToDB(mContext, cv);
                                    if(DBUtils.getShowValue(mContext,i.mId)!=0){
                                        setAlarmTime(mContext, i.mAppearTime, i.mId, SET_SHOW_ALARM);
                                    }
                                }
                            }
                        }
                        if (result == 2) {
                            Log.i(TAG, "开始解析");
                        }
                        if (result == 3) {
                            Log.i(TAG, "解析中");
                        }
                    }
                }, msgs);
                cursor.close();
            }
        }
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if (uri == null) {
            uri = Uri.parse("content://sms/inbox");
        }
        if ("content://sms/raw".equals(uri.toString())) return;
        if (privios_uri == null || !uri.toString().equals(privios_uri.toString())) {
            Message message=Message.obtain();
            message.obj=uri;
            privios_uri = uri;
            mParseMsgHandler.sendMessage(message);
        }
    }

    public static int getType(String typeName){
        Log.i(TAG,"typeName:"+typeName);
        for(int i=0;i<TYPE.length;i++){
            if(TYPE[i].equals(typeName)){
                return i+1;
            }
        }
        return 0;
    }

    public static void setAlarmTime(final Context context, final long triggerAtMillis, final int source_id, int type) {
        switch (type) {
            case SET_SHOW_ALARM: {
                Log.i(TAG, "setAlarm");
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                Intent intent = new Intent(context, IntelcardService.class);
                intent.putExtra(FROM, SET_SHOW_ALARM);

                Bundle bundle = new Bundle();
                bundle.putInt(IntelCardContract.IntelCard.SOURCE_ID, source_id);
                intent.putExtras(bundle);

                PendingIntent serviceIntent = PendingIntent.getService(context, source_id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, serviceIntent);
                break;
            }
            case SET_IGNORE_ALARM: {
                Log.i(TAG, "ignoreAlarm");
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                Intent intent = new Intent(context, IntelcardService.class);
                intent.putExtra(FROM, SET_IGNORE_ALARM);

                Bundle bundle = new Bundle();
                bundle.putInt(IntelCardContract.IntelCard.SOURCE_ID, source_id);
                intent.putExtras(bundle);
                PendingIntent serviceIntent = PendingIntent.getService(context, source_id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, serviceIntent);
                break;
            }
            case SET_DELETE_ALARM: {
                Log.i(TAG, "removeAlarm");
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                Intent intent = new Intent(context, IntelcardService.class);
                intent.putExtra(FROM, SET_DELETE_ALARM);

                Bundle bundle = new Bundle();
                bundle.putInt(IntelCardContract.IntelCard.SOURCE_ID, source_id);
                intent.putExtras(bundle);
                PendingIntent serviceIntent = PendingIntent.getService(context, source_id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, serviceIntent);
                break;
            }
            case TEST_REMOVE_ALARM: {
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(context, IntelcardService.class);
                intent.putExtra(FROM, TEST_REMOVE_ALARM);

                PendingIntent serviceIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, serviceIntent);
                break;
            }
            default:
                break;
        }
    }
}
