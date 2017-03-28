// Copyright 2014 Google Inc. All Rights Reserved.

package com.google.android.gms.cast.samples.democastplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.google.android.gms.cast.CastMediaControlIntent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Display a list of available receiver applications.
 */
public class ReceiverAppsListPreference extends ListPreference {

    private static final String APP_ID_NAME_SEPARATOR = ":";
    private static final String APP_SEPARATOR = ",";
    private static final String DEFAULT_RECEIVER_APP_ID =
            CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;

    public ReceiverAppsListPreference(Context context, AttributeSet attrSet) {
        super(context, attrSet);
    }

    /**
     * To display the entry value and summary side by side.
     */
    private class TwoColumnArrayAdapter extends ArrayAdapter<CharSequence> {
        private CharSequence[] mEntrySummaries = null;
        private int mSelectedIndex = 0;

        public TwoColumnArrayAdapter(Context context, int textViewResourceId,
                CharSequence[] entries, CharSequence[] entrySummaries, int selectedIndex) {
            super(context, textViewResourceId, entries);
            mEntrySummaries = entrySummaries;
            mSelectedIndex = selectedIndex;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            View view = inflater.inflate(R.layout.two_column_list_preference_item, parent, false);
            TextView titleTextView = (TextView) view.findViewById(R.id.entry_title);
            titleTextView.setText(getEntries()[position]);
            CheckedTextView summaryTextView =
                    (CheckedTextView) view.findViewById(R.id.entry_summary);
            summaryTextView.setText(mEntrySummaries[position]);
            if (position == mSelectedIndex) {
                summaryTextView.setChecked(true);
            }
            return view;
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        List<ReceiverApp> receiverAppList = getAvailableReceiverAppsFromSharedPreferences();
        CharSequence[] entriesArray = new CharSequence[receiverAppList.size()];
        CharSequence[] valuesArray = new CharSequence[receiverAppList.size()];
        for (int i = 0; i < receiverAppList.size(); ++i) {
            entriesArray[i] = receiverAppList.get(i).getName();
            valuesArray[i] = receiverAppList.get(i).getId();
        }
        setEntries(entriesArray);
        setEntryValues(valuesArray);

        int selectedIndex = findIndexOfValue(getValue());
        ListAdapter adapter = new TwoColumnArrayAdapter(getContext(),
                R.layout.two_column_list_preference_item, getEntries(), getEntryValues(),
                selectedIndex);
        builder.setAdapter(adapter, this);

        super.onPrepareDialogBuilder(builder);
    }

    private List<ReceiverApp> getAvailableReceiverAppsFromSharedPreferences() {
        List<ReceiverApp> receiverAppList = new ArrayList<ReceiverApp>();

        // Always add the default app to the top of the list.
        ReceiverApp defaultReceiverApp = getDefaultReceiverApp();
        receiverAppList.add(defaultReceiverApp);

        // Add additional apps to the list, remove duplications.
        SharedPreferences sharedPref = getSharedPreferences();
        String commaSeparatedAdditionalApps = sharedPref.getString(
                AppConstants.PREF_KEY_ADDITIONAL_RECEIVER_APPS, "");
        Set<String> additionalAppSet = new HashSet<String>(
                Arrays.asList(commaSeparatedAdditionalApps.split(APP_SEPARATOR)));
        additionalAppSet.remove(defaultReceiverApp.toString());
        for (String appString : additionalAppSet) {
            ReceiverApp receiverApp = new ReceiverApp();
            if (receiverApp.parseFromColumnSeparatedString(appString)) {
                receiverAppList.add(receiverApp);
            }
        }

        return receiverAppList;
    }

    private ReceiverApp getDefaultReceiverApp() {
        return new ReceiverApp(DEFAULT_RECEIVER_APP_ID,
                getContext().getString(R.string.default_receiver_app_name));
    }

    private class ReceiverApp {

        // The receiver application ID, e.g. "ABCD1234".
        private String mId;
        // The corresponding human readable name of the ID, e.g. "Default media receiver".
        private String mName;

        public ReceiverApp() {
            mId = null;
            mName = null;
        }

        public ReceiverApp(String id, String name) {
            mId = id;
            mName = name;
        }

        public String getId() {
            return mId;
        }

        public String getName() {
            return mName;
        }

        public boolean parseFromColumnSeparatedString(String appString) {
            String[] elems = appString.split(APP_ID_NAME_SEPARATOR);
            if (elems.length != 2) {
                return false;
            }
            mId = elems[0];
            mName = elems[1];
            return true;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%s%s%s", mId, APP_ID_NAME_SEPARATOR, mName);
        }
    }
}
