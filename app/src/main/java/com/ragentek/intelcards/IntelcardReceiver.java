package com.ragentek.intelcards;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ragentek.intelcards.provider.DBUtils;

import static com.ragentek.intelcards.SmsContentObserver.FROM;
import static com.ragentek.intelcards.SmsContentObserver.SET_SHOW_ALARM;
import static com.ragentek.intelcards.SmsContentObserver.TEST_CLEAR_SHOW_RECEIVER;
import static com.ragentek.intelcards.SmsContentObserver.TEST_DELETEALL;
import static com.ragentek.intelcards.SmsContentObserver.TEST_RECEIVER;
import static com.ragentek.intelcards.SmsContentObserver.TEST_SHOWALL;
import static com.ragentek.intelcards.SmsContentObserver.RESET_ALRAM;
import static com.ragentek.intelcards.SmsContentObserver.setAlarmTime;

/**
 * Created by zhaoxue.meng on 2017/7/4.
 */

public class IntelcardReceiver extends BroadcastReceiver {
    private static final String TAG = "IntelcardReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Log.i(TAG, "interlcards start service and reset Alarm");
            Intent updateWidgetService = new Intent(context, IntelcardService.class);
            updateWidgetService.putExtra(FROM, RESET_ALRAM);
            context.startService(updateWidgetService);
        } else if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            Log.i(TAG, "interlcards start service");
            Intent updateWidgetService = new Intent(context, IntelcardService.class);
            context.startService(updateWidgetService);
        }else if (TEST_RECEIVER.equals(intent.getAction())) {
            DBUtils.setAllShowValue(context, 0);
        }else if (TEST_CLEAR_SHOW_RECEIVER.equals(intent.getAction())) {
            setAlarmTime(context, System.currentTimeMillis() + 2 * 1000, 1, SET_SHOW_ALARM);
            setAlarmTime(context, System.currentTimeMillis() + 2 * 1000, 2, SET_SHOW_ALARM);
            setAlarmTime(context, System.currentTimeMillis() + 2 * 1000, 5, SET_SHOW_ALARM);
            setAlarmTime(context, System.currentTimeMillis() + 2 * 1000, 9, SET_SHOW_ALARM);
            setAlarmTime(context, System.currentTimeMillis() + 2 * 1000, 13, SET_SHOW_ALARM);
        }else if (TEST_SHOWALL.equals(intent.getAction())) {
            for (int i = 0; i < 150; i++) {
                setAlarmTime(context, System.currentTimeMillis() + 2 * 1000, i, SET_SHOW_ALARM);
            }
        }else if (TEST_DELETEALL.equals(intent.getAction())) {
            DBUtils.deleteAll(context);
        }
    }
}
