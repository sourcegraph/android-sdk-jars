// Copyright 2015 Google Inc. All Rights Reserved.

package com.google.android.gms.cast.samples.democastplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

/**
 * Base activity for queuing operations.
 */
public abstract class BaseQueueActivity extends ActionBarActivity {

    public static final String EXTRA_REPEAT_MODE = "EXTRA_REPEAT_MODE";
    public static final String EXTRA_START_INDEX = "EXTRA_START_INDEX";
    public static final String EXTRA_CURRENT_ITEM_ID = "EXTRA_CURRENT_ITEM_ID";
    public static final String EXTRA_INSERTED_ITEM = "EXTRA_INSERTED_ITEM";
    public static final String EXTRA_INSERT_BEFORE_ITEM_ID = "EXTRA_INSERT_BEFORE_ITEM_ID";
    public static final String EXTRA_START_TIME = "EXTRA_START_TIME";
    public static final String EXTRA_ITEM_IDS = "EXTRA_ITEM_IDS";
    public static final String EXTRA_ITEMS = "EXTRA_ITEMS";

    protected QueueListFragment mQueueListFragment;
    protected QueueItemAdapter mQueueAdapter;

    protected void onCreate(Bundle savedInstanceState, int contentViewId, int titleId) {
        super.onCreate(savedInstanceState);
        setContentView(contentViewId);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(titleId);

        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Button okButton = (Button) findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOkButtonClick();
            }
        });

        mQueueListFragment = (QueueListFragment)
                getSupportFragmentManager().findFragmentById(R.id.list_fragment);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED, new Intent());
        finish();
    }

    protected abstract void onOkButtonClick();
}
