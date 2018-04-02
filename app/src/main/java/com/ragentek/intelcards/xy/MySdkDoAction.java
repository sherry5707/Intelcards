package com.ragentek.intelcards.xy;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.util.Map;

import cn.com.xy.sms.sdk.action.AbsSdkDoAction;
import cn.com.xy.sms.sdk.constant.Constant;
import cn.com.xy.sms.sdk.ui.card.CardDataManager;

public class MySdkDoAction extends AbsSdkDoAction {
    private static final String TAG="MySdkDoAction";
    @Override
    public void sendSms(Context context, String s, String s1, int i, Map<String, String> map) {

    }

    @Override
    public void openSms(Context context, String s, Map<String, String> map) {

    }

    @Override
    public String getContactName(Context context, String s) {
        return null;
    }

    @Override
    public void markAsReadForDatabase(Context context, String msgId) {
        Log.d(TAG, "markAsReadForDatabase: ");
        ContentValues values = new ContentValues();
        Uri uri = Uri.parse("content://sms");
        values.put("read", 1);
        context.getContentResolver().update(uri, values, "_id = " + msgId, null);
    }

    @Override
    public void deleteMsgForDatabase(Context context, String msgId) {
        Log.d(TAG, "deleteMsgForDatabase: ");
        Uri uri =  Uri.parse("content://sms");
        context.getContentResolver().delete(uri, "_id ="+msgId, null);
    }

    @Override
    public void onEventCallback(int eventType, Map<String, Object> map) {
        if(eventType == AbsSdkDoAction.SDK_EVENT_LOAD_COMPLETE){
            CardDataManager.getInstance(Constant.getContext()).setmSdkInitStatus(eventType);
            Log.i(TAG,"init XIAOYUAN complete");
        }
    }
}
