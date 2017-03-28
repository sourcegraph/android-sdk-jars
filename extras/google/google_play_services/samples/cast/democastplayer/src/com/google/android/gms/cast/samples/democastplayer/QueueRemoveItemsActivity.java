// Copyright 2015 Google Inc. All Rights Reserved.

package com.google.android.gms.cast.samples.democastplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Activity to remove items from the queue.
 */
public class QueueRemoveItemsActivity extends BaseQueueActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.queue_update_items_activity,
                R.string.queue_remove_activity_title);

        mQueueAdapter = new QueueItemAdapter(this, android.R.layout.simple_spinner_item,
                mQueueListFragment, false, true, false);
        mQueueAdapter.populateAdapterWithIntent(getIntent());
        mQueueListFragment.setListAdapter(mQueueAdapter);
    }

    @Override
    protected void onOkButtonClick() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_ITEM_IDS, mQueueAdapter.getCheckedItemIds());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
