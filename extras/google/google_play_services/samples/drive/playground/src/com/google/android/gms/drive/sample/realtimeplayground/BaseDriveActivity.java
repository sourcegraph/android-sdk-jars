// Copyright 2013 Google Inc. All Rights Reserved.

package com.google.android.gms.drive.sample.realtimeplayground;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

/**
 * The BaseDriveActivity handles authentication and the connection to the Drive services.  Each
 * activity that interacts with Drive should extend this class.
 * <p>The connection is requested in onStart, and disconnected in onStop.
 * <p>Extend {@link #onClientConnected()} to be notified when the connection is active.
 */
public abstract class BaseDriveActivity extends ActionBarActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "BaseDriveActivity";

    protected static final String EXTRA_ACCOUNT_NAME = "accountName";
    private static final String EXTRA_RESOLVING_ERROR = "resolvingError";

    // Magic value indicating use the GMS Core default account
    protected static final String DEFAULT_ACCOUNT = "DEFAULT ACCOUNT";

    protected static final int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    protected static final int NEXT_AVAILABLE_REQUEST_CODE = 2;

    // This variable can only be accessed from the UI thread.
    protected GoogleApiClient mGoogleApiClient;

    // Tracks whether the app is already resolving an error.
    private boolean mResolvingError = false;

    protected String mAccountName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Determine the active account:
        // In the saved instance bundle?
        // In the intent?
        // If not found, use the default account.
        if (savedInstanceState != null) {
            mAccountName = savedInstanceState.getString(EXTRA_ACCOUNT_NAME);
            mResolvingError = savedInstanceState.getBoolean(EXTRA_RESOLVING_ERROR, false);
        }
        if (mAccountName == null) {
            mAccountName = getIntent().getStringExtra(EXTRA_ACCOUNT_NAME);
        }
        if (mAccountName == null) {
            Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
            if (accounts.length > 0) {
                mAccountName = accounts[0].name;
                Log.d(TAG, "No account specified, selecting " + mAccountName);
            } else {
                mAccountName = DEFAULT_ACCOUNT;
                Log.d(TAG, "No enabled accounts, changing to DEFAULT ACCOUNT");
            }
        }

        setupGoogleApiClient();
    }

    protected void setupGoogleApiClient() {
        Log.d(TAG, "API client setup");
        cleanupGoogleApiClient();

        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addScope(Drive.SCOPE_FILE);
        // If account name is unset in the builder, the default is used
        if (!DEFAULT_ACCOUNT.equals(mAccountName)) {
            builder.setAccountName(mAccountName);
        } else {
            Log.d(TAG, "No account specified, selecting default account.");
        }
        mGoogleApiClient = builder.build();
    }

    private void cleanupGoogleApiClient() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.unregisterConnectionFailedListener(this);
            mGoogleApiClient = null;
        }
    }

    /**
     * Invoked when the drive client has successfully connected.  This can be used by extending
     * activities to perform actions once the client is fully initialized.
     */
    protected abstract void onClientConnected();

    protected String getAccountName() {
        return mAccountName;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_ACCOUNT_NAME, mAccountName);
        outState.putBoolean(EXTRA_RESOLVING_ERROR, mResolvingError);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onResume");
        super.onStart();
        disconnectGoogleApiClient();
        cleanupGoogleApiClient();
        setupGoogleApiClient();
        if (!mResolvingError) {
            connectGoogleApiClient();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                mResolvingError = false;
                if (resultCode == RESULT_OK) {
                    connectGoogleApiClient();
                } else {
                    Log.w(TAG, "Canceled request to connect to Play Services: " + resultCode);
                }
                break;
            default:
                Log.w(TAG, "Unexpected activity request code" + requestCode);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        disconnectGoogleApiClient();
        cleanupGoogleApiClient();
        super.onStop();
    }

    /**
     * Initiates a connection request (if not already connected) that will result in a call to
     * {@link #onClientConnected}.
     */
    protected void connectGoogleApiClient() {
        if (!mGoogleApiClient.isConnected()) {
            Log.i(TAG, "Connecting to GoogleApiClient");
            mGoogleApiClient.connect();
        }
    }

    /**
     * Disconnects the client if currently connected.  Provided for components that want to
     * override the default behavior of disconnecting in {@link #onStop}.
     */
    private void disconnectGoogleApiClient() {
        if (mGoogleApiClient != null) {
            Log.i(TAG, "Disconnecting from GoogleApiClient");
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
        onClientConnected();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connections suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: " + result.getErrorCode());

        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                connectGoogleApiClient();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            ErrorDialogFragment fragment = ErrorDialogFragment.newInstance(result.getErrorCode());
            fragment.show(getSupportFragmentManager(), "errordialog");
            mResolvingError = true;
        }
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    private void onErrorDialogDismissed() {
        mResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        // Argument tag for the error code passed to the error dialog.
        private static final String ARG_ERROR_CODE = "dialog_error_code";

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(ARG_ERROR_CODE);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    getActivity(), RESOLVE_CONNECTION_REQUEST_CODE);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((BaseDriveActivity) getActivity()).onErrorDialogDismissed();
            super.onDismiss(dialog);
        }

        public static ErrorDialogFragment newInstance(int errorCode) {
            ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_ERROR_CODE, errorCode);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }
    }
}
