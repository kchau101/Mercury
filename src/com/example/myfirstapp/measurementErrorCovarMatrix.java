package com.example.myfirstapp;

import org.ejml.simple.SimpleMatrix;

import android.util.Log;

public class measurementErrorCovarMatrix 
{
	private double accelPitchError, accelRollError;
	private double gyroPitchError, gyroRollError;
	private double gyroPitchOmegaError, gyroRollOmegaError;

	private double [][] measurementErrorCovarianceMatrix = {
			{accelPitchError, 0, 0, 0, 0, 0},
			{0, accelRollError, 0, 0, 0, 0},
			{0, 0, gyroPitchError, 0, 0, 0},
			{0, 0, 0, gyroRollError, 0, 0},
			{0, 0, 0, 0, gyroPitchOmegaError, 0},
			{0, 0, 0, 0, 0, gyroRollOmegaError}
	};
	
	private SimpleMatrix mat;
	
	measurementErrorCovarMatrix()
	{
		mat = new SimpleMatrix(measurementErrorCovarianceMatrix);
		Log.v("CovarMatSize", Integer.toString(mat.numRows()) +"x" + Integer.toString(mat.numCols()) );
	}
	public SimpleMatrix getSimpleMatrix()
	{
		return mat;
	}
	public void setAccelPitchError(double pitchError)
	{
		accelPitchError = pitchError;
		mat.set(0, 0, accelPitchError);
	}
	
	public void setAccelRollError(double rollError)
	{
		accelPitchError = rollError;
		mat.set(1, 1, accelRollError);
	}
	
	public void setGyroPitchError(double pitchError)
	{
		gyroPitchError = pitchError;
		mat.set(2, 2, gyroPitchError);
	}
	
	public void setGyroRollError(double rollError)
	{
		gyroRollError = rollError;
		mat.set(3, 3, gyroRollError);
	}
	
	public void setGyroPitchOmegaError(double omegaError)
	{
		gyroPitchOmegaError = omegaError;
		mat.set(4, 4, gyroPitchOmegaError);
	}
	
	public void setGyroRollOmegaError(double omegaError)
	{
		gyroRollOmegaError = omegaError;
		mat.set(5, 5, gyroRollOmegaError);
	}
	
}
