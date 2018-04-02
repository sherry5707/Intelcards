/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.ragentek.intelcards.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * <p>
 * The contract between the IntelCard provider and App. Contains
 * definitions for the supported URIs and data columns.
 * </p>
 * <h3>Overview</h3>
 * <p>
 * IntelCardContract defines the data model of IntelCard related information.
 * This data is stored in a number of tables:
 * </p>
 * <ul>
 * <li>The {@link IntelCardColumns} table holds the intelligent cards</li>
 * </ul>
 */
public final class IntelCardContract {
    /**
     * This authority is used for writing to or querying from the intelcards
     * provider.
     */
    public static final String AUTHORITY = "com.ragentek.intelcards";

    /**
     * This utility class cannot be instantiated
     */
    private IntelCardContract() {
    }

    /**
     * Constants for the IntelCards table, which contains all the intelligent cards.
     */
    protected interface IntelCardColumns extends BaseColumns {
        /**
         * The content:// style URL for this table.
         */
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/intelcards");

        /**
         * The beginning time of the event of the intel card, in UTC milliseconds. Column name.
         * <P>Type: INTEGER (long; millis since epoch)</P>
         */
        String ORDERTIME = "ordertime";

        /**
         * The active time of the intel card, in UTC milliseconds. Column name.
         * <P>Type: INTEGER (long; millis since epoch)</P>
         */
        String APPEARTIME = "appeartime";

        /**
         * The ending time of the event of the intel card, in UTC milliseconds. Column name.
         * <P>Type: INTEGER (long; millis since epoch)</P>
         */
        String DEADTIME = "deadtime";

        /**
         * The (sdk) source of the intel card, i.e. xiaoyuan, etc. Column name
         * <p>
         * <p>Type: STRING</p>
         */
        String SOURCE = "source";

        /**
         * The _id of the intel card from its (sdk) source. Column name.
         * <P>Type: INTEGER (long, foreign key to source sdk tables)</P>
         */
        String SOURCE_ID = "source_id";

        String MSGID = "msg_id";

        /**
         * this indicate whether this cards should be shown.
         * 0 means not
         * 1 means show
         * 2 means it just insert
         * -1 means move from widget and delete from db
         */
        String SHOW = "show";
        String CARD_TYPE = "card_type";
    }

    public static class IntelCard implements IntelCardColumns {

    }
}
