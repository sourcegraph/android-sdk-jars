/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.auth.sample.suw;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.io.IOException;

/**
 * AuthSetupWizardWorkflowLauncher is a simple activity to configure and launch
 * the Google Account setup flow as it would appear in a OEM setup wizard.
 * <p>
 * This example is only meaningful to OEMs who are implementing their own
 * setup wizard for first run device setup and want to integrate google account
 * setup into that flow.
 *
 */
public class AuthSetupWizardWorkflowLauncher
        extends Activity implements AccountManagerCallback<Bundle> {
    private static final String TAG = "AuthSuwSample";

    private static final String EXTRA_IS_SETUP_WIZARD = "firstRun";
    private static final String EXTRA_IS_IMMERSIVE_MODE = "useImmersiveMode";
    private static final String EXTRA_IS_CARRIER_SETUP_LAUNCHED = "carrierSetupLaunched";
    private static final String EXTRA_THEME = "theme";

    private static final int REQUEST_ADD_ACCOUNT = 1;

    private HandlerThread mHandlerThread;
    private TextView mOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_suw_launcher);
        mOut = (TextView) findViewById(R.id.message);
        mHandlerThread = new HandlerThread(TAG + ".HandlerThread");
        mHandlerThread.start();
    }

    /** Called by button in the layout */
    public void launchGoogleAuthSetupWizardWorkflow(@SuppressWarnings("unused") View view) {
        // Configure setupwizard related options.
        Bundle options = new Bundle();
        options.putBoolean(EXTRA_IS_SETUP_WIZARD, true);
        // Suppresses navigation bar.
        options.putBoolean(EXTRA_IS_IMMERSIVE_MODE, true);
        options.putBoolean(EXTRA_IS_CARRIER_SETUP_LAUNCHED, false);
        /*
         * After LMP the preferred theme is material light. But "holo" (holo
         * dark) and "holo_light" are also supported.
         */
        String theme = (Build.VERSION.SDK_INT  >= 21) ? "material_light" : "holo";
        options.putString(EXTRA_THEME, theme);
        AccountManager am = AccountManager.get(this);
        // handler specifying the execution thread of the callback.
        Handler handler = new Handler(mHandlerThread.getLooper());
        am.addAccount(
                "com.google",  // accountType
                null,  // authTokenType
                null,  // requiredFeatures
                options,  // addAccountOptions
                null, // activity, don't start a new task stack.
                this,  // AccountManagerCallback
                handler);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode != RESULT_OK) {
            mOut.setText("Result not ok!");
        } else if (requestCode == REQUEST_ADD_ACCOUNT) {
            if (resultData != null) {
                mOut.setText("Result: " + resultData.getExtras());
            } else {
                mOut.setText("Result ok but no data!");
            }
        } else {
            mOut.setText("Unrecognized request code!");
        }
    }

    @Override
    public void run(AccountManagerFuture<Bundle> resultFuture) {
        String outputMsg = null;
        try {
            final Bundle bundle = resultFuture.getResult();
            Intent addAccountActivityIntent = bundle.getParcelable(AccountManager.KEY_INTENT);
            if (addAccountActivityIntent != null) {
                Log.w(TAG, "Staring addAccountActivityIntent.");
                startActivityForResult(addAccountActivityIntent, REQUEST_ADD_ACCOUNT);
            } else {
                Log.w(TAG, "No add account workflow intent returned!!!");
            }
            outputMsg = bundle.toString();
        } catch (OperationCanceledException | AuthenticatorException | IOException e) {
            outputMsg = e.getMessage();
        }
        final String resultMsg = outputMsg;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mOut.setText(resultMsg);
            }
        });
    }
}