package com.ragentek.intelcards.ui;

import com.ragentek.intelcards.provider.IntelCardContract.IntelCard;

/**
 * Created by zhaoxue.meng on 2017/3/24.
 */

public class IntelCardQuery {

    public static final String[] PROJECTION = new String[]{
            IntelCard._ID,
            IntelCard.ORDERTIME,
            IntelCard.APPEARTIME,
            IntelCard.DEADTIME,
            IntelCard.SOURCE,
            IntelCard.SHOW,
            IntelCard.MSGID,
            IntelCard.CARD_TYPE,
            IntelCard.SOURCE_ID,
    };
}
