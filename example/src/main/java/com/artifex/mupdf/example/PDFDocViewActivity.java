package com.artifex.mupdf.example;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;

import com.artifex.mupdf.android.DocActivityView;

/**
 * @author archko
 */
public class PDFDocViewActivity extends FragmentActivity implements SensorEventListener {

    private String mPath;
    private DocActivityView mDocActivityView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  set up UI
        setContentView(R.layout.activity_doc_view);
		mDocActivityView = (DocActivityView) findViewById(R.id.doc_view);

        parseIntent();

        //  start the view
        mDocActivityView.showUI(true);  //  set to false for no built-in UI
        mDocActivityView.start(mPath);
    }

    private void parseIntent() {
        if (TextUtils.isEmpty(mPath)) {
            Intent intent = getIntent();

            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri uri = intent.getData();
                System.out.println("URI to open is: " + uri);
                if (uri.toString().startsWith("content://")) {
                    String reason = null;
                    Cursor cursor = null;
                    try {
                        cursor = getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                        if (cursor.moveToFirst()) {
                            String str = cursor.getString(0);
                            if (str == null) {
                                reason = "Couldn't parse data in intent";
                            } else {
                                uri = Uri.parse(str);
                            }
                        }
                    } catch (Exception e2) {
                        System.out.println("Exception in Transformer Prime file manager code: " + e2);
                        reason = e2.toString();
                    } finally {
                        if (null != cursor) {
                            cursor.close();
                        }
                    }
                }
                String path = Uri.decode(uri.getEncodedPath());
                if (path == null) {
                    path = uri.toString();
                }
                mPath = path;
            } else {
                if (!TextUtils.isEmpty(getIntent().getStringExtra("path"))) {
                    mPath = getIntent().getStringExtra("path");
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private SensorManager sensorManager;
    private float[] gravity = {0f, -9.81f, 0f};
    private long gravityAge = 0;

    private int prevOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

    @Override
    protected void onResume() {
        super.onResume();

		/*sensorManager = null;

		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		if (sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() > 0) {
			gravity[0] = 0f;
			gravity[1] = -9.81f;
			gravity[2] = 0f;
			gravityAge = 0;
			sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_NORMAL);
			prevOrientation = options.getInt(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			setRequestedOrientation(prevOrientation);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}*/

        //documentView.setVerticalScrollLock(options.getBoolean(Options.PREF_VERTICAL_SCROLL_LOCK, true));
        /*if (options.getBoolean(Options.PREF_FULLSCREEN, true)) {
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}*/
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        //saveCurrentPage();

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            sensorManager = null;
            /*SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
            edit.putInt(Options.PREF_PREV_ORIENTATION, prevOrientation);
			//Log.v(TAG, "prevOrientation saved: "+prevOrientation);
			edit.apply();*/
        }
    }

    private void setOrientation(int orientation) {
        if (orientation != prevOrientation) {
            //Log.v(TAG, "setOrientation: "+orientation);
            setRequestedOrientation(orientation);
            prevOrientation = orientation;
        }
    }

    /**
     * Called when accuracy changes.
     * This method is empty, but it's required by relevant interface.
     */
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        gravity[0] = 0.8f * gravity[0] + 0.2f * event.values[0];
        gravity[1] = 0.8f * gravity[1] + 0.2f * event.values[1];
        gravity[2] = 0.8f * gravity[2] + 0.2f * event.values[2];

        float sq0 = gravity[0] * gravity[0];
        float sq1 = gravity[1] * gravity[1];
        float sq2 = gravity[2] * gravity[2];

        gravityAge++;

        if (gravityAge < 4) {
            // ignore initial hiccups
            return;
        }

        if (sq1 > 3 * (sq0 + sq2)) {
            if (gravity[1] > 4)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            else if (gravity[1] < -4 && Integer.parseInt(Build.VERSION.SDK) >= 9)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        } else if (sq0 > 3 * (sq1 + sq2)) {
            if (gravity[0] > 4)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            else if (gravity[0] < -4 && Integer.parseInt(Build.VERSION.SDK) >= 9)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        }
    }

    //===========================================
}
