package com.ragentek.intelcards.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.ragentek.intelcards.IntelcardService;
import com.ragentek.intelcards.R;
import com.ragentek.intelcards.ui.IntelCardListActivity;

import cn.com.xy.sms.sdk.ui.card.CardDataEntity;
import cn.com.xy.sms.sdk.ui.cardservice.DuoquCardService;

import static com.ragentek.intelcards.SmsContentObserver.FROM;
import static com.ragentek.intelcards.SmsContentObserver.SET_IGNORE_ALARM;
import static com.ragentek.intelcards.SmsContentObserver.SHOW_DETAIL;
import static com.ragentek.intelcards.provider.IntelCardContract.IntelCardColumns.SOURCE_ID;

/**
 * Created by dell on 2017/5/10.
 */

public class CardWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "CardWidgetProvider";
    private long lastPullTime;
    private static final long PERIOD = 500;
    private RemoteViews bubbleView;
    private ServiceHandler mServiceHandler;
    private Context mContext;
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int appWidgetId= (int) msg.obj;
            updateWidgets(mContext, appWidgetId);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int appWidgetId : appWidgetIds) {
            //updateWidgets(context, appWidgetId);
            Message msg = mServiceHandler.obtainMessage();
            msg.obj=appWidgetId;
            mContext=context;
            mServiceHandler.sendMessage(msg);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }


    private RemoteViews getItemView(final Context mContext, final String id) {
        if (mContext == null) return null;
        bubbleView = null;
        DuoquCardService.queryCardDataById(mContext, id, new DuoquCardService.CardCallback() {
            @Override
            public void onSuccess(int i, Object o) {
                if (i == 0) {
                    if (o == null) return;
                    CardDataEntity cde = (CardDataEntity) o;
                    bubbleView = DuoquCardService.getRichRemoteBubbleView(mContext, cde.mCardResJson, id, cde.mContent,
                            cde.mPhoneNum, cde.mSmsReciveTime, null);
                }
            }

            @Override
            public void onFailed(int i) {

            }
        });
        return bubbleView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(mServiceHandler==null) {
            HandlerThread thread = new HandlerThread("getRemoteView");
            thread.start();
            Looper mHandlerLooper = thread.getLooper();
            mServiceHandler = new ServiceHandler(mHandlerLooper);
        }

        //if the time between first request with second request less than 30s,the second request not resolve
        long nowtime = System.currentTimeMillis();
        if (nowtime - lastPullTime < PERIOD) {
            Log.i(TAG, "onReceive update return");
            return;
        }
        Log.i(TAG, "onReceive update");
        lastPullTime = nowtime;
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager awm = AppWidgetManager.getInstance(context);
            int[] mAppWidgetIds = awm.getAppWidgetIds(new ComponentName(context,
                    CardWidgetProvider.class));
            for (int appWidgetId : mAppWidgetIds) {
                //updateWidgets(context, appWidgetId);
                Message msg = mServiceHandler.obtainMessage();
                msg.obj=appWidgetId;
                mContext=context;
                mServiceHandler.sendMessage(msg);
            }
        }
    }

    public void updateWidgets(Context context, int appWidgetId) {
        AppWidgetManager awm = AppWidgetManager.getInstance(context);
        RemoteViews widgetViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        Cursor cursor = DBUtils.queryForShow(context);
        if (cursor != null && cursor.moveToFirst()) {

            widgetViews.removeAllViews(R.id.card1);
            widgetViews.removeAllViews(R.id.card2);
            widgetViews.setViewVisibility(R.id.cardll1, View.GONE);
            widgetViews.setViewVisibility(R.id.cardll2, View.GONE);
            widgetViews.setViewVisibility(R.id.diliver, View.GONE);
            Log.i(TAG, "cursor.getCount:" + cursor.getCount());
            if (cursor.getCount() == 1) {
                widgetViews.setViewVisibility(R.id.cardll1, View.VISIBLE);
                addItemView(context, cursor, widgetViews, R.id.card1, R.id.card_menu1);
            }
            if (cursor.getCount() >= 2) {
                widgetViews.setViewVisibility(R.id.cardll1, View.VISIBLE);
                widgetViews.setViewVisibility(R.id.cardll2, View.VISIBLE);
                widgetViews.setViewVisibility(R.id.diliver, View.VISIBLE);
                boolean flag = false;
                do {
                    if (flag == false) {
                        flag = addItemView(context, cursor, widgetViews, R.id.card1, R.id.card_menu1);
                        Log.i(TAG, "flag:" + flag);
                    } else {
                        addItemView(context, cursor, widgetViews, R.id.card2, R.id.card_menu2);
                        cursor.close();
                        break;
                    }
                } while (cursor.moveToNext());
            }
            awm.updateAppWidget(appWidgetId, widgetViews);
        }
    }

    private boolean addItemView(Context context, Cursor cursor, RemoteViews remoteViews, int cardId, int menuId) {
        int source_id1 = cursor.getInt(cursor.getColumnIndex(SOURCE_ID));
        Log.i(TAG, "addItemView,source_id=" + source_id1);
        RemoteViews views = getItemView(context, String.valueOf(source_id1));
        if (views == null) return false;
        remoteViews.addView(cardId, views);
        //show detail
        //Intent showDetail1 = new Intent(context, IntelCardDetailActivity.class);
        Intent showDetail1 = new Intent(context, IntelcardService.class);
        //为了区分上下两个pendingIntent需要setAction
        showDetail1.setAction("showDetailIntent");
        showDetail1.putExtra(FROM, SHOW_DETAIL);
        Bundle detailBundle = new Bundle();
        detailBundle.putInt(IntelCardContract.IntelCard.SOURCE_ID, source_id1);
        showDetail1.putExtras(detailBundle);
        PendingIntent clickshowDetailIntent1 = PendingIntent.getService(context, source_id1, showDetail1, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(cardId, clickshowDetailIntent1);
        //ignore
        Intent removeIntent1 = new Intent(context, IntelcardService.class);
        removeIntent1.setAction("removeIntent");
        removeIntent1.putExtra(FROM, SET_IGNORE_ALARM);
        Bundle bundle1 = new Bundle();
        bundle1.putInt(IntelCardContract.IntelCard.SOURCE_ID, source_id1);
        removeIntent1.putExtras(bundle1);
        PendingIntent removeService1 = PendingIntent.getService(context, source_id1, removeIntent1, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(menuId, removeService1);
        //showMore
        Intent showMoreIntent = new Intent(context, IntelCardListActivity.class);
        //showMoreIntent.setAction("removeIntent");
        PendingIntent showMorePending = PendingIntent.getActivity(context, source_id1, showMoreIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.show_more_cards, showMorePending);
        return true;
    }
}
