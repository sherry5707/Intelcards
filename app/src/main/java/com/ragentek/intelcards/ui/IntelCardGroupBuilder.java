/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ragentek.intelcards.ui;

import android.database.Cursor;
import android.text.format.Time;

import com.ragentek.intelcards.provider.IntelCardContract;
import com.ragentek.intelcards.utils.DateUtils;


/**
 * Groups together cards in the call log.  The primary grouping attempts to group together calls
 * to and from the same number into a single row on the call log.
 * A secondary grouping assigns calls, grouped via the primary grouping, to "day groups".  The day
 * groups provide a means of identifying the calls which occurred "Today", "Yesterday", "Last week",
 * or "Other".
 * <p>
 * This class is meant to be used in conjunction with {@link GroupingListAdapter}.
 */
class IntelCardGroupBuilder {
    private static final String TAG = "IntelCardGroupBuilder";

    public interface GroupCreator {

        /**
         * Defines the interface for adding a group to the call log.
         * The primary group for a call log groups the calls together based on the number which was
         * dialed.
         *
         * @param cursorPosition The starting position of the group in the cursor.
         * @param size           The size of the group.
         */
        void addGroup(int cursorPosition, int size);

        /**
         * Defines the interface for tracking the day group each call belongs to.  Calls in a call
         * group are assigned the same day group as the first call in the group.  The day group
         * assigns calls to the buckets: Today, Yesterday, Last week, and Other
         *
         * @param rowId    The row Id of the current call.
         * @param dayGroup The day group the call belongs in.
         */
        void setDayGroup(long rowId, int dayGroup);

        /**
         * Defines the interface for clearing the day groupings information on rebind/regroup.
         */
        void clearDayGroups();
    }

    /**
     * Day grouping for intel cards used to represent no associated day group.  Used primarily
     * when retrieving the previous day group, but there is no previous day group (i.e. we are at
     * the start of the list).
     */
    public static final int DAY_GROUP_NONE = -1;

    /**
     * Day grouping for intel cards which begins today.
     */
    public static final int DAY_GROUP_TODAY = 0;

    /**
     * Day grouping for intel cards which begins tomorrow.
     */
    public static final int DAY_GROUP_TOMORROW = 1;

    /**
     * Day grouping for calls which occurred before last week.
     */
    private static final int DAY_GROUP_OTHER = 2;

    public static final int DAY_GROUP_EARLIER = 3;
    /**
     * Instance of the time object used for time calculations.
     */
    private static final Time TIME = new Time();

    /**
     * The object on which the groups are created.
     */
    private final GroupCreator mGroupCreator;

    public IntelCardGroupBuilder(GroupCreator groupCreator) {
        mGroupCreator = groupCreator;
    }

    /**
     * Finds all groups of adjacent entries in the call log which should be grouped together and
     * calls {@link GroupCreator#addGroup(int, int)} on {@link #mGroupCreator} for each of
     * them.
     * <p>
     * For entries that are not grouped with others, we do not need to create a group of size one.
     * <p>
     * It assumes that the cursor will not change during its execution.
     *
     * @see GroupingListAdapter#addGroups(Cursor)
     */
    public void addGroups(Cursor cursor) {
        final int count = cursor.getCount();
        if (count == 0) {
            return;
        }

        // Clear any previous day grouping information.
        mGroupCreator.clearDayGroups();

        // Get current system time, used for calculating which day group cards belong to.
        long currentTime = System.currentTimeMillis();
        cursor.moveToFirst();

        // Determine the day group for the first card in the cursor.
        long firstCardId = cursor.getLong(cursor.getColumnIndex(IntelCardContract.IntelCard._ID));
        long lastBegin = cursor.getLong(cursor.getColumnIndex(IntelCardContract.IntelCard.ORDERTIME));
        int groupDayGroup = getDayGroup(lastBegin, currentTime);
        mGroupCreator.setDayGroup(firstCardId, groupDayGroup);

        int groupSize = 1;

        while (cursor.moveToNext()) {
            long begin = cursor.getLong(cursor.getColumnIndex(IntelCardContract.IntelCard.ORDERTIME));
            groupDayGroup = getDayGroup(begin, currentTime);

            // Group with the date.
            if (isSameDate(lastBegin, begin)) {
                // Increment the size of the group.
                groupSize++;
            } else {
                // The date has changed. Determine the day group for the new group.
                groupDayGroup = getDayGroup(begin, currentTime);

                // Create a group for the previous group of cards
                mGroupCreator.addGroup(cursor.getPosition() - groupSize, groupSize);

                // Start a new group; it will include at least the current card.
                groupSize = 1;

                // Update the group values to those of the current call.
                lastBegin = begin;
            }

            // Save the day group associated with the current call.
            final long currentCardId = cursor.getLong(cursor.getColumnIndex(IntelCardContract.IntelCard._ID));
            mGroupCreator.setDayGroup(currentCardId, groupDayGroup);
        }

        // Create a group for the last set of cards.
        mGroupCreator.addGroup(count - groupSize, groupSize);
    }

    /**
     * Given a call date and the current date, determine which date group the call belongs in.
     *
     * @param date The call date.
     * @param now  The current date.
     * @return The date group the call belongs in.
     */
    private int getDayGroup(long date, long now) {
        int days = DateUtils.getDayDifference(TIME, date, now);
        if (days == 0) {
            return DAY_GROUP_TODAY;
        } else if (days == 1) {
            return DAY_GROUP_TOMORROW;
        } else if (days < 0) {
            return DAY_GROUP_EARLIER;
        } else {
            return DAY_GROUP_OTHER;
        }
    }

    private boolean isSameDate(long date1, long date2) {
        int days = DateUtils.getDayDifference(TIME, date1, date2);
        return days == 0;
    }
}
