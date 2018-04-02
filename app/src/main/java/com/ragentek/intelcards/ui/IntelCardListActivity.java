package com.ragentek.intelcards.ui;

import android.app.ActionBar;
import android.app.LoaderManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toolbar;

import com.ragentek.intelcards.IntelcardService;
import com.ragentek.intelcards.R;
import com.ragentek.intelcards.SmsContentObserver;
import com.ragentek.intelcards.provider.DBUtils;
import com.ragentek.intelcards.provider.IntelCardContract;

import cn.com.xy.sms.sdk.ui.card.CardDataEntity;
import cn.com.xy.sms.sdk.ui.cardservice.DuoquCardService;

import static com.ragentek.intelcards.SmsContentObserver.SET_SHOW_ALARM;

public class IntelCardListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "IntelCard.List";

    private RecyclerView mRecyclerView;
    private IntelCardListAdapter mListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intelcard_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView mTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        mTitle.setText(R.string.app_name);
        setActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        mRecyclerView = (RecyclerView) findViewById(R.id.intelcard_recycler_list);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        if (mListAdapter == null) {
            mListAdapter = new IntelCardListAdapter(this);
            mRecyclerView.setAdapter(mListAdapter);
        }

        getLoaderManager().initLoader(0, null, this);
        Intent intent = new Intent(IntelCardListActivity.this, IntelcardService.class);
        startService(intent);
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
        //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);	//注意上下顺序不能变！！
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String currentTime = String.valueOf(System.currentTimeMillis());
        Log.i(TAG, "currentTime:" + currentTime);
        return new CursorLoader(this, IntelCardContract.IntelCard.CONTENT_URI,
                IntelCardQuery.PROJECTION,
                IntelCardContract.IntelCard.DEADTIME + " > " + currentTime + " OR " + IntelCardContract.IntelCard.DEADTIME + " = " + currentTime,
                null, IntelCardContract.IntelCard.ORDERTIME + "," + IntelCardContract.IntelCard.CARD_TYPE);
    }



    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (mListAdapter == null) {
            mListAdapter = new IntelCardListAdapter(this);
            mRecyclerView.setAdapter(mListAdapter);
        }
        mListAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mListAdapter != null) {
            mListAdapter.changeCursor(null);
        }
    }

    public static String getICCID(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getSimSerialNumber();
    }
}
