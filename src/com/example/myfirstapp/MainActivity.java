package com.example.myfirstapp;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;
import android.widget.ImageView;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

import java.lang.String;
import java.lang.Math;

import org.ejml.simple.SimpleMatrix;;

public class MainActivity extends ActionBarActivity implements SensorEventListener {
	private float xAccelAngle, yAccelAngle, xAccelOmega, yAccelOmega, accelMag;
	private float xGyroAngle, yGyroAngle, zGyroAngle;
	private float xKalmanAngle, yKalmanAngle;
	private float accelAnimAngle, gyroAnimAngle, kalmanAnimAngle;
	private long timestamp;
	
	private final float processError = 0.001f;
	private double[][] observationMatrix = {
			{1, 0, 0, 0},
			{0, 0, 1, 0},
			{1, 0, 0, 0},
			{0, 1, 0, 0},
			{0, 0, 1, 0},
			{0, 0, 0, 1}
	};
	private double [][] processErrorCovarianceMatrix = {
			{processError, 0, 0, 0},
			{0, processError, 0, 0},
			{0, 0, processError, 0},
			{0, 0, 0, processError}
	};
	
	private measurementErrorCovarMatrix pMat = new measurementErrorCovarMatrix();
	
	
	private SimpleMatrix mA;
	private SimpleMatrix mH = new SimpleMatrix(observationMatrix);
	private SimpleMatrix mQ = new SimpleMatrix(processErrorCovarianceMatrix);
	private SimpleMatrix mR;
	
	private SimpleMatrix Xn, Pn;
	
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Sensor mGyro;
	
	
	
	private static final float NS2S = 1.0f/1000000000.0f;
	private static final float noise = 0.05f;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
            
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
            mSensorManager.registerListener(this,mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(this,mGyro, SensorManager.SENSOR_DELAY_GAME);
            accelAnimAngle = 0.0f;
            gyroAnimAngle = 0.0f;
            kalmanAnimAngle = 0.0f;
            xAccelOmega = 0.0f;
            yAccelOmega = 0.0f;
            xAccelAngle = 0.0f;
            yAccelAngle = 0.0f;
            xGyroAngle = 0.0f;
            yGyroAngle = 0.0f;
            zGyroAngle = 0.0f;
            double[][] x0 = {
            		{0},
            		{0},
            		{0},
            		{0}
            };
            
            double[][] p0 = { //Estimates of initial error
            		{0.1, 0, 0, 0},
            		{0, 0.01, 0, 0},
            		{0, 0, 0.1, 0},
            		{0, 0, 0, 0.01}
            };
            
            Xn = new SimpleMatrix(x0);
            Pn = new SimpleMatrix(p0);
            
            pMat.setAccelPitchError(0.5);
            pMat.setAccelRollError(0.3);
            pMat.setGyroPitchError(0.05);
            pMat.setGyroPitchOmegaError(0.05);
            pMat.setGyroRollError(0.1);
            pMat.setGyroRollOmegaError(0.1);
            mR = pMat.getSimpleMatrix();
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

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		// Can be ignored for demo?
	}


	@Override
	public void onSensorChanged(SensorEvent arg0) {
		// TODO Auto-generated method stub
		Sensor source = (Sensor) arg0.sensor;
		
		String xText;
		String yText;
		String zText;
		
		if (source.equals(mAccelerometer)) {

			float xAccel = arg0.values[0]/9.81f;
			float yAccel = arg0.values[1]/9.81f;
			float zAccel = arg0.values[2]/9.81f;
			accelMag = (float) Math.sqrt(xAccel*xAccel + yAccel*yAccel +zAccel*zAccel);
			
			if (accelMag >= 1.1f)
			{
				pMat.setAccelPitchError((float) 2*(accelMag - 1) +  + 0.5);
	            pMat.setAccelRollError((float) 1.5*(accelMag - 1) + 0.3);
	            mR = pMat.getSimpleMatrix();
			}
			else
			{
				pMat.setAccelPitchError(0.5f);
	            pMat.setAccelRollError(0.3f);
				mR = pMat.getSimpleMatrix();
			}
			
			float xAngle = (float) Math.toDegrees(Math.atan2(yAccel, zAccel));
			float yAngle = (float) Math.toDegrees(Math.atan2(-xAccel, Math.sqrt(yAccel*yAccel + zAccel*zAccel)));

			RotateAnimation ahrsAnim = new RotateAnimation(accelAnimAngle, -xAngle,
					Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
			ahrsAnim.setInterpolator(new LinearInterpolator());
			ahrsAnim.setRepeatCount(0);
			ahrsAnim.setDuration(50); //Approximately 4 frames

			TextView xAxisView = (TextView) findViewById(R.id.x_accel);
			TextView yAxisView = (TextView) findViewById(R.id.y_accel);
			TextView zAxisView = (TextView) findViewById(R.id.z_accel);
			TextView xAngleView = (TextView) findViewById(R.id.x_angle_accel);
			TextView yAngleView = (TextView) findViewById(R.id.y_angle_accel);
			ImageView ahrsAccelIndicator = (ImageView) findViewById(R.id.ahrsAccelIndicator);

			xText = "X-axis:	" + Float.toString(xAccel) + " G";
			yText = "Y-axis:	" + Float.toString(yAccel) + " G";
			zText = "Z-axis:	" + Float.toString(zAccel) + " G";

			xAxisView.setText(xText);
			yAxisView.setText(yText);
			zAxisView.setText(zText);
			xAngleView.setText("Pitch Angle:" + Float.toString(xAngle) + " degs");
			yAngleView.setText("Roll Angle:" + Float.toString(yAngle) + " degs");

			ahrsAccelIndicator.startAnimation(ahrsAnim);
			accelAnimAngle = -xAngle;
			
			xAccelOmega = xAngle - xAccelAngle;
			yAccelOmega = yAngle - yAccelAngle;
			xAccelAngle = xAngle;
			yAccelAngle = yAngle;
		}
		else if (source.equals(mGyro)) {
			
			if (timestamp !=0) {
				float dt = (arg0.timestamp - timestamp) * NS2S;
				
				float xOmega = (float) Math.toDegrees(arg0.values[0]);
				float yOmega = (float) Math.toDegrees(arg0.values[1]);
				float zOmega = (float) Math.toDegrees(arg0.values[2]);
				
				
				float omegaMag = (float) Math.sqrt(xOmega*xOmega + yOmega *yOmega + zOmega*zOmega);
				
				if (omegaMag > noise) {
					double[][] stateTransitionMatrix = {
							{1, dt, 0, 0},
							{0, 1, 0, 0},
							{0, 0, 1, dt},
							{0, 0, 0, 1}
					};
					
					mA = new SimpleMatrix(stateTransitionMatrix);
					
					if  ((Math.abs(xOmega - xAccelOmega ) < 0.1)
							&& (0.98 < accelMag) && (accelMag < 1.02f))
					{
						xGyroAngle = xAccelAngle;
					}
					if ( (Math.abs(yOmega - yAccelOmega ) < 0.05)) {
						yGyroAngle = yAccelAngle;
					}
					
					
					float xAngle = (float) xGyroAngle + (xOmega * dt);
					float yAngle = (float) yGyroAngle + (yOmega * dt);
					float zAngle = (float) zGyroAngle + (zOmega * dt);
					
					
					RotateAnimation ahrsAnim = new RotateAnimation(gyroAnimAngle, -xAngle,
							Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
					ahrsAnim.setInterpolator(new LinearInterpolator());
					ahrsAnim.setRepeatCount(0);
					ahrsAnim.setDuration(50); //Approximately 4 frames
					
					TextView xAxisView = (TextView) findViewById(R.id.x_gyro);
					TextView yAxisView = (TextView) findViewById(R.id.y_gyro);
					TextView zAxisView = (TextView) findViewById(R.id.z_gyro);
					TextView xAngleView = (TextView) findViewById(R.id.x_angle_gyro);
					TextView yAngleView = (TextView) findViewById(R.id.y_angle_gyro);
					TextView zAngleView = (TextView) findViewById(R.id.z_angle_gyro);
					ImageView ahrsGyroIndicator = (ImageView) findViewById(R.id.ahrsGyroIndicator);
					
					xAxisView.setText("X-axis:	" + Float.toString(xOmega) + " degs/s");
					yAxisView.setText("Y-axis:	" + Float.toString(yOmega) + " degs/s");
					zAxisView.setText("Z-axis:	" + Float.toString(zOmega) + " degs/s");
					xAngleView.setText("X-axis:	" + Float.toString(xAngle) + " degs");
					yAngleView.setText("Y-axis:	" + Float.toString(yAngle) + " degs");
					zAngleView.setText("Z-axis:	" + Float.toString(zAngle) + " degs");
					
					ahrsGyroIndicator.startAnimation(ahrsAnim);
					gyroAnimAngle = -xAngle;
					
					xGyroAngle = xAngle;
					yGyroAngle = yAngle;
					zGyroAngle = zAngle;
					
					SimpleMatrix Xp = mA.mult(Xn);
					SimpleMatrix Pp = ( ( mA.mult(Pn) ).mult(mA.transpose()) ).plus(mQ);
					double [][] zn = {
							{(double) xAccelAngle},
							{(double) yAccelAngle},
							{xGyroAngle},
							{xOmega},
							{yGyroAngle},
							{yOmega}
					};
					
					SimpleMatrix y = new SimpleMatrix(zn).minus(mH.mult(Xp));
					SimpleMatrix mS = mH.mult(Pp).mult(mH.transpose()).plus(mR);
					SimpleMatrix mK = Pp.mult(mH.transpose()).mult(mS.invert());
					ImageView ahrsKalmanIndicator = (ImageView) findViewById(R.id.ahrsKalmanIndicator);
					
					Xn = Xp.plus(mK.mult(y));
					Pn = (SimpleMatrix.identity(4).minus(mK.mult(mH))).mult(Pp);
					
					TextView xKalman = (TextView) findViewById(R.id.x_kalman);
					TextView yKalman = (TextView) findViewById(R.id.y_kalman);
					TextView xOmegaKalman = (TextView) findViewById(R.id.xOmegaKalman);
					TextView yOmegaKalman = (TextView) findViewById(R.id.yOmegaKalman);
					
					xKalmanAngle = (float) Xn.get(0,0);
					yKalmanAngle = (float) Xn.get(2, 0);
					
					xKalman.setText("Pitch Angle: " + Float.toString(xKalmanAngle));
					yKalman.setText("Roll Angle: " + Float.toString(yKalmanAngle));
					xOmegaKalman.setText("Pitch Velocity: " + Float.toString((float) Xn.get(1,0)) + "degs/s");
					yOmegaKalman.setText("Roll Velocity: " + Float.toString((float) Xn.get(3,0)) + "degs/s");
					
					RotateAnimation ahrsKalmanAnim = new RotateAnimation(kalmanAnimAngle,(float) -xKalmanAngle,
							Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
					ahrsKalmanAnim.setInterpolator(new LinearInterpolator());
					ahrsKalmanAnim.setRepeatCount(0);
					ahrsKalmanAnim.setDuration(50); //Approximately 4 frames
					
					ahrsKalmanIndicator.startAnimation(ahrsKalmanAnim);
					kalmanAnimAngle = -xKalmanAngle;
				}
			}
			timestamp = arg0.timestamp;
		}
	}

}
