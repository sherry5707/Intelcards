package com.ragentek.intelcards.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ragentek.intelcards.R;


public class IntelCardListItemViewHolder extends RecyclerView.ViewHolder {
    public final LinearLayout mDayGroupll;
    public final TextView mDayGroupLabel;
    public final LinearLayout mViewGroup;
    public final RelativeLayout mDigit;
    //public LinearLayout mMenu;

    public long rowId = -1;

    public IntelCardListItemViewHolder(View itemView) {
        super(itemView);
        mDayGroupLabel = (TextView) itemView.findViewById(R.id.day_group_label);
        mDayGroupll = (LinearLayout) itemView.findViewById(R.id.day_group_ll);
        mViewGroup = (LinearLayout) itemView.findViewById(R.id.content_view_group);
        mDigit = (RelativeLayout) itemView.findViewById(R.id.digit);
        //mMenu = (LinearLayout) itemView.findViewById(R.id.item_menu);
    }
}
