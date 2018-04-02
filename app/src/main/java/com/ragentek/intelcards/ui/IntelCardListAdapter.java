package com.ragentek.intelcards.ui;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.ragentek.intelcards.R;
import com.ragentek.intelcards.provider.DBUtils;
import com.ragentek.intelcards.provider.IntelCardContract;

import org.json.JSONException;

import java.util.HashMap;

import cn.com.xy.sms.sdk.ui.card.CardDataEntity;
import cn.com.xy.sms.sdk.ui.cardservice.DuoquCardService;

public class IntelCardListAdapter extends RecyclerView.Adapter<IntelCardListItemViewHolder>
        implements IntelCardGroupBuilder.GroupCreator {
    static final String TAG = "IntelCard.ListAdapter";

    private final Context mContext;
    private final LayoutInflater mInflater;
    private Cursor mCursor;
    private final IntelCardGroupBuilder mIntelCardGroupBuilder;
    private final HashMap<Long, Integer> mDayGroups = new HashMap<>();
    private ViewGroup parent;

    public IntelCardListAdapter(Context context) {
        super();
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mIntelCardGroupBuilder = new IntelCardGroupBuilder(this);
    }

    void changeCursor(Cursor cursor) {
        if (cursor != null) {
            mCursor = cursor;
            mIntelCardGroupBuilder.addGroups(cursor);
            notifyDataSetChanged();
        }
    }

    @Override
    public IntelCardListItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        this.parent = parent;
        View view = mInflater.inflate(R.layout.intelcard_list_item, parent, false);
        return new IntelCardListItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final IntelCardListItemViewHolder holder, int position) {
        holder.setIsRecyclable(false);
        if (mCursor == null || !mCursor.moveToPosition(position)) return;

        final int cardId = mCursor.getInt(mCursor.getColumnIndex(IntelCardContract.IntelCard.SOURCE_ID));
        holder.rowId = mCursor.getLong(mCursor.getColumnIndex(IntelCardContract.IntelCard._ID));

        // Check if the day group has changed and display a header if necessary.
        int currentGroup = getDayGroupForCall(holder.rowId);
        int previousGroup = getPreviousDayGroup(mCursor);
        if (currentGroup != previousGroup) {
            holder.mDayGroupll.setVisibility(View.VISIBLE);
            holder.mDayGroupLabel.setText(getGroupDescription(currentGroup));
        } else {
            holder.mDayGroupll.setVisibility(View.GONE);
        }

        //add richbubbleview
        View richBubbleView = getItemView(String.valueOf(cardId), parent);
        if (richBubbleView == null) {
            return;
        }

        holder.mDigit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopMenu(holder, cardId);
            }
        });
        removeViewFromParent(richBubbleView);
        holder.mViewGroup.removeAllViews();
        holder.mViewGroup.addView(richBubbleView);
    }

    private void removeViewFromParent(View v) {
        ViewParent parent = v.getParent();
        if (parent instanceof ViewGroup) {
            ViewGroup p = (ViewGroup) parent;
            p.removeView(v);
        }
    }

    private void showPopMenu(IntelCardListItemViewHolder holder, final int cardId) {
        final PopupWindow popupWindow = new PopupWindow(mContext);
        popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        View popView = LayoutInflater.from(mContext).inflate(R.layout.menu_dialog, null);
        popupWindow.setContentView(popView);
        popupWindow.setBackgroundDrawable(mContext.getDrawable(R.drawable.menu_shape));
        //popupWindow.setBackgroundDrawable(new ColorDrawable(mContext.getColor(R.color.menu_white_bg)));
        popupWindow.setOutsideTouchable(false);
        popupWindow.setFocusable(true);
        popupWindow.update();
        popupWindow.showAsDropDown(holder.mDigit,mContext.getResources().getDimensionPixelOffset(R.dimen.pop_menu_xoof),
                mContext.getResources().getDimensionPixelOffset(R.dimen.pop_menu_yoof));
        LinearLayout show_more = (LinearLayout) popView.findViewById(R.id.show_detail);
        show_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    DuoquCardService.toWebPage(mContext, String.valueOf(cardId));
                    popupWindow.dismiss();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        LinearLayout ignore = (LinearLayout) popView.findViewById(R.id.ignore_card);
        ignore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DBUtils.deleteInDB(mContext, cardId);
                /*Intent updateWidgetIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                mContext.sendBroadcast(updateWidgetIntent);*/
                popupWindow.dismiss();
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        } else {
            return 0;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mCursor == null){
            return -1;
        }
        mCursor.moveToPosition(position);
        return (int) mCursor.getLong(mCursor.getColumnIndex(IntelCardContract.IntelCard.SOURCE_ID));
    }

    @Override
    public void addGroup(int cursorPosition, int size) {
        // do nothing now because we don't have card group
    }

    @Override
    public void setDayGroup(long rowId, int dayGroup) {
        if (!mDayGroups.containsKey(rowId)) {
            mDayGroups.put(rowId, dayGroup);
        }
    }

    @Override
    public void clearDayGroups() {
        mDayGroups.clear();
    }

    /**
     * Retrieves the day group of the previous card.  Used to determine if the day
     * group has changed and to trigger display of the day group text.
     *
     * @param cursor The card cursor.
     * @return The previous day group, or DAY_GROUP_NONE if this is the first call.
     */
    private int getPreviousDayGroup(Cursor cursor) {
        // We want to restore the position in the cursor at the end.
        int startingPosition = cursor.getPosition();
        int dayGroup = IntelCardGroupBuilder.DAY_GROUP_NONE;
        if (cursor.moveToPrevious()) {
            long previousRowId = cursor.getLong(cursor.getColumnIndex(IntelCardContract.IntelCard._ID));
            dayGroup = getDayGroupForCall(previousRowId);
        }
        cursor.moveToPosition(startingPosition);
        return dayGroup;
    }

    /**
     * Given a card Id, look up the day group that the card belongs to.  The day group data is
     * populated in {@link com.ragentek.intelcards.ui.IntelCardGroupBuilder}.
     *
     * @param cardId The card to retrieve the day group for.
     * @return The day group for the card.
     */
    private int getDayGroupForCall(long cardId) {
        if (mDayGroups.containsKey(cardId)) {
            return mDayGroups.get(cardId);
        }
        return IntelCardGroupBuilder.DAY_GROUP_NONE;
    }

    private View bubbleView;

    private View getItemView(String id, final ViewGroup viewGroup) {
        bubbleView = null;
        DuoquCardService.queryCardDataById(mContext, id, new DuoquCardService.CardCallback() {
            @Override
            public void onSuccess(int i, Object o) {
                if (i == 0) {
                    if (o == null) return;
                    CardDataEntity cde = (CardDataEntity) o;
                    Activity activity = (Activity) mContext;
                    bubbleView = DuoquCardService.getRichBubbleView(activity, cde.mCardResJson, String.valueOf(cde.mId),
                            cde.mContent, cde.mPhoneNum, cde.mSmsReciveTime, null, viewGroup, null);
                }
            }

            @Override
            public void onFailed(int i) {

            }
        });
        return bubbleView;
    }

    private CharSequence getGroupDescription(int group) {
        if (group == IntelCardGroupBuilder.DAY_GROUP_TODAY) {
            return mContext.getResources().getString(R.string.daygroup_today);
        } else if (group == IntelCardGroupBuilder.DAY_GROUP_TOMORROW) {
            return mContext.getResources().getString(R.string.daygroup_tomorrow);
        } else if (group == IntelCardGroupBuilder.DAY_GROUP_EARLIER) {
            return mContext.getResources().getString(R.string.daygroup_earlier);
        } else {
            return mContext.getResources().getString(R.string.daygroup_more);
        }
    }
}
