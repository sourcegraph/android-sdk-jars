package com.google.android.tagmanager.examples.helloworld;

import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.TagManager;

/**
 * An {@link Activity} that reads background and text color from a local
 * Json file and applies those colors to text view.
 */
public class MainActivity extends Activity {
    private static final String TAG = "GTMExample";
    private static final String CONTAINER_ID = "GTM-XXXX";
    private static final String BACKGROUND_COLOR_KEY = "background-color";
    private static final String TEXT_COLOR_KEY = "text-color";

    // Set to false for release build.
    private static final Boolean DEVELOPER_BUILD = true;
    private ContainerHolder mContainerHolder = null;

    private void setContainerHolder(ContainerHolder containerHolder) {
      this.mContainerHolder = containerHolder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEVELOPER_BUILD) {
            StrictMode.enableDefaults();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new DownloadContainerTask(this).execute(CONTAINER_ID);
    }

    private void updateColors() {
        Log.i(TAG, "updateColors");
        TextView textView = (TextView) findViewById(R.id.hello_world);
        textView.setBackgroundColor(getColor(BACKGROUND_COLOR_KEY));
        textView.setTextColor(getColor(TEXT_COLOR_KEY));
    }

    /**
     * Returns an integer representing a color.
     */
    private int getColor(String key) {
        String colorName = "";
        if (mContainerHolder != null) {
          colorName = mContainerHolder.getContainer().getString(key);
        }
        return colorFromColorName(colorName);
    }

    /**
     * Looks up the externalized string resource and displays it in a pop-up dialog box.
     *
     * @param stringKey
     */
    private void displayErrorToUser(int stringKey) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage(getResources().getString(stringKey));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
        });
        alertDialog.show();
    }

    public void colorButtonClicked(@SuppressWarnings("unused") View view) {
        Log.i(TAG, "colorButtonClicked");
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Getting colors");
        // The container holder might have not been set at this moment. For an example that shows
        // how to use a splash screen to guarantee that the container holder will be initialized,
        // see cuteanimals example.
        if (mContainerHolder != null) {
          alertDialog.setMessage(BACKGROUND_COLOR_KEY + " = "
                  + mContainerHolder.getContainer().getString(BACKGROUND_COLOR_KEY)
                  + " " + TEXT_COLOR_KEY + " = "
                  + mContainerHolder.getContainer().getString(TEXT_COLOR_KEY));
        } else {
          alertDialog.setMessage("The container isn't ready. Using default application values");

        }
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
        });
        alertDialog.show();
        updateColors();
    }

    public void refreshButtonClicked(@SuppressWarnings("unused") View view) {
        Log.i(TAG, "refreshButtonClicked");
        if (mContainerHolder != null) {
          mContainerHolder.refresh();
        }
    }

    public int colorFromColorName(String colorName) {
        try {
            return Color.parseColor(colorName);
        } catch (Exception e) {
            return Color.BLACK;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    // This AsyncTask class will set the Container Holder object once this task is completed.
    private class DownloadContainerTask extends AsyncTask<String, Void, Boolean> {
        private static final long TIMEOUT_FOR_CONTAINER_OPEN_MILLISECONDS = 2000;
        private static final int DEFAULT_CONTAINER_RESOURCE_ID = R.raw.gtm_xxxx_json;

        private Activity mActivity;
        private ContainerHolder mContainerHolder;

        public DownloadContainerTask(Activity activity) {
            this.mActivity = activity;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String containerId = params[0];

            TagManager tagManager = TagManager.getInstance(mActivity);
            PendingResult<ContainerHolder> pending = tagManager.loadContainerPreferNonDefault(
                    containerId, DEFAULT_CONTAINER_RESOURCE_ID);

            mContainerHolder = pending.await(TIMEOUT_FOR_CONTAINER_OPEN_MILLISECONDS,
                    TimeUnit.MILLISECONDS);
            if (!mContainerHolder.getStatus().isSuccess()) {
                Log.e("HelloWorld", "failure loading container");
                displayErrorToUser(R.string.load_error);
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // Set up the containerHolder object.
            setContainerHolder(mContainerHolder);
            // Modify the background-color and text-color of text based on the value
            // from configuration.
            updateColors();
        }
    }
}
