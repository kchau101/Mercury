package com.example.myfirstapp;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;
import android.widget.ImageView;
import android.util.Log;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.String;

public class MainActivity extends ActionBarActivity implements SensorEventListener {
	private float xAccel, yAccel, zAccel;
	private float xGyro, yGyro, zGyro;
	private long timestamp;
	
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Sensor mGyro;
	
	private StringBuilder builder;
	
	
	private final float NS2S = 1.0f/1000000000.0f;
	private float dt = 0.0f;
	private boolean accelReady, gyroReady,startLogging = false;
	private Button logButton;
	
	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}

	
	OnClickListener loggerListener = new OnClickListener() {
		public void onClick(View v)
		{
			if (startLogging == true)
			{
				Log.v("Writing", "Stopped Logging");
				FileOutputStream fOut;
				File output = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
						"dataLog.csv");
				
				try
				{
					fOut = new FileOutputStream(output, false);
					fOut.write(builder.toString().getBytes());
					fOut.flush();
					fOut.close();
					Log.v("File Write", "Written to: " + Environment.getExternalStoragePublicDirectory(
							Environment.DIRECTORY_DOCUMENTS).toString());
				}
				catch (IOException e)
				{
					Log.v("IOError", e.getLocalizedMessage());
					e.printStackTrace();
				}
				builder.setLength(0);
				startLogging = false;
				logButton.setText("Start Logging");
			}
			else
			{
				Log.v("Reading", "Started Logging");
				startLogging = true;
				logButton.setText("Stop Logging");
			}
		}
	};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
        	Log.v("Timing", "test");
        	PlaceholderFragment fragment = new PlaceholderFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
            mSensorManager.registerListener(this,mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(this,mGyro, SensorManager.SENSOR_DELAY_GAME);
            
            builder  = new StringBuilder(0);

        }
    }
    
    protected void onResume(){
    	super.onResume();
    	
    	mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    	mSensorManager.registerListener(this,mGyro, SensorManager.SENSOR_DELAY_GAME);
    }
    
    protected void onPause() {
    	super.onPause();
    	
    	mSensorManager.unregisterListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
    	
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            Log.v("Expanded", "Fragment");
            return rootView;
        }

    }

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		// Can be ignored for demo?
	}


	@Override
	public void onSensorChanged(SensorEvent arg0) 
	{
		logButton = (Button) findViewById(R.id.loggerButton);
        logButton.setOnClickListener(loggerListener);
        
		// TODO Auto-generated method stub
		Sensor source = (Sensor) arg0.sensor;
		if (source.equals(mAccelerometer)) 
		{

			xAccel = arg0.values[0]/9.81f;
			yAccel = arg0.values[1]/9.81f;
			zAccel = arg0.values[2]/9.81f;
			
			accelReady = true;
		}
		else if (source.equals(mGyro)) 
		{

				
				
				xGyro = (float) Math.toDegrees(arg0.values[0]);
				yGyro = (float) Math.toDegrees(arg0.values[1]);
				zGyro = (float) Math.toDegrees(arg0.values[2]);
			
				gyroReady = true;
		}
		dt = (arg0.timestamp - timestamp) * NS2S;
		if (gyroReady && accelReady && startLogging)
		{
			builder.append(xAccel + ", " + yAccel + ", " + zAccel +
					", " + xGyro + ", " + yGyro + ", " + zGyro + ", " +
					dt + "\n");
			gyroReady = false;
			accelReady = false;
		}
		timestamp = arg0.timestamp;
		
	}

}
