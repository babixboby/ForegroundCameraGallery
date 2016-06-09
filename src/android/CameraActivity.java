/*
	    Copyright 2012 Bruno Carreira - Lucas Farias - Rafael Luna - Vin�cius Fonseca.

		Licensed under the Apache License, Version 2.0 (the "License");
		you may not use this file except in compliance with the License.
		You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

		Unless required by applicable law or agreed to in writing, software
		distributed under the License is distributed on an "AS IS" BASIS,
		WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
		See the License for the specific language governing permissions and
   		limitations under the License.
 */

package com.tuxpan.foregroundcameragalleryplugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.FrameLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.ZoomControls;
import android.view.Display;
/**
 * Camera Activity Class. Configures Android camera to take picture and show it.
 */
public class CameraActivity extends Activity {

	private static final String TAG = "CameraActivity";

	private Camera mCamera;
	private CameraPreview mPreview;
	private boolean pressed = false;
	int currentZoomLevel = 0, maxZoomLevel = 0;
	private OrientationEventListener mOrientationEventListener;
	private int mOrientation = -1;

	private static final int ORIENTATION_PORTRAIT_NORMAL = 1;
	private static final int ORIENTATION_PORTRAIT_INVERTED = 2;
	private static final int ORIENTATION_LANDSCAPE_NORMAL = 3;
	private static final int ORIENTATION_LANDSCAPE_INVERTED = 4;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getResources().getIdentifier("foregroundcameraplugin", "layout", getPackageName()));
		// Create an instance of Camera
		mCamera = getCameraInstance();

		if (mCamera == null) {
			setResult(RESULT_CANCELED);
			finish();
			return;
		}

		final Camera.Parameters params = mCamera.getParameters();

		params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

		mCamera.setParameters(params);

		// Create a Preview and set it as the content of activity.
		mPreview = new CameraPreview(this, mCamera);

		FrameLayout preview = (FrameLayout) findViewById(getResources().getIdentifier("camera_preview", "id", getPackageName()));
		preview.addView(mPreview);
		
		// Add a listener to the Capture button
		ImageButton captureButton = (ImageButton) findViewById(getResources().getIdentifier("button_capture", "id", getPackageName()));
		captureButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				if (pressed || mCamera == null)
					return;

				// Set pressed = true to prevent freezing.
				// Issue 1 at
				// http://code.google.com/p/foreground-camera-plugin/issues/detail?id=1
				pressed = true;

				// get an image from the camera
				mCamera.autoFocus(new AutoFocusCallback() {

					public void onAutoFocus(boolean success, Camera camera) {
						mCamera.takePicture(null, null, null, mPicture);
					}
				});
			}
		});

		ImageButton cancelButton = (ImageButton) findViewById(getResources().getIdentifier("button_cancel", "id", getPackageName()));
		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pressed = false;
				setResult(RESULT_CANCELED);
				finish();
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
	}


	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (mCamera != null) {
			try {
				mCamera.stopPreview();
				mCamera.setPreviewCallback(null);
			} catch (Exception e) {
				Log.d(TAG, "Exception stopping camera: " + e.getMessage());
			}
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
		super.onDestroy();
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
			Log.d(TAG, "Camera not available: " + e.getMessage());
		}
		return c; // returns null if camera is unavailable
	}

	private PictureCallback mPicture = new PictureCallback() {

		public void onPictureTaken(byte[] data, Camera camera) {

			Uri fileUri = (Uri) getIntent().getExtras().get(MediaStore.EXTRA_OUTPUT);

			File pictureFile = new File(fileUri.getPath());

			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
			} catch (FileNotFoundException e) {
				Log.d(TAG, "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.d(TAG, "Error accessing file: " + e.getMessage());
			}

			ExifInterface exif;
			try {
				exif = new ExifInterface(pictureFile.getAbsolutePath());
				switch (mOrientation) {
				case ORIENTATION_PORTRAIT_NORMAL:
					// image.put(Media.ORIENTATION, 90);
					exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
					break;
				case ORIENTATION_LANDSCAPE_NORMAL:
					exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
					break;
				case ORIENTATION_PORTRAIT_INVERTED:
					exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_270));
					break;
				case ORIENTATION_LANDSCAPE_INVERTED:
					exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_180));
					break;
				}
				exif.saveAttributes();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			setResult(RESULT_OK);
			pressed = false;
			finish();
		}

	};
}
