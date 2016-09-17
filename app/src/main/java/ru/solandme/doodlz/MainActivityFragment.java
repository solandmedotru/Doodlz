package ru.solandme.doodlz;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class MainActivityFragment extends Fragment {

    private DoodleView doodleView;
    private float acceleration;
    private float currentAcceleration;
    private float lastAcceleration;
    private boolean dialogOnScreen = false;

    private static final int ACCELERATION_THRESHOLD = 100000;
    private static final int SAVE_IMAGE_PERMISSION_REQUEST_CODE = 1;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        setHasOptionsMenu(true);
        doodleView = (DoodleView) view.findViewById(R.id.doodleView);

        acceleration = 0.00f;
        currentAcceleration = SensorManager.GRAVITY_EARTH;
        lastAcceleration = SensorManager.GRAVITY_EARTH;
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        enableAccelerometerListening();
    }

    private void enableAccelerometerListening() {
        SensorManager sensorManager = (SensorManager) getActivity()
                .getSystemService(Context.SENSOR_SERVICE);

        sensorManager.registerListener(sensorEventListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        disableAccelerometerListening();
    }

    private void disableAccelerometerListening() {
        SensorManager sensorManager = (SensorManager) getActivity()
                .getSystemService(Context.SENSOR_SERVICE);

        sensorManager.unregisterListener(sensorEventListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    }

    private final SensorEventListener sensorEventListener =
            new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    if (!dialogOnScreen) {
                        float x = event.values[0];
                        float y = event.values[1];
                        float z = event.values[2];

                        lastAcceleration = currentAcceleration;

                        currentAcceleration = x * x + y * y + z * z;

                        acceleration = currentAcceleration *
                                (currentAcceleration - lastAcceleration);

                        if (acceleration > ACCELERATION_THRESHOLD)
                            confirmErase();
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            };

    private void confirmErase() {
        EraseImageDialogFragment fragment = new EraseImageDialogFragment();
        fragment.show(getFragmentManager(), "erase dialog");
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.doodle_fragment_menu, menu);
    }

    // handle choice from options menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // switch based on the MenuItem id
        switch (item.getItemId()) {
            case R.id.color:
                ColorDialogFragment colorDialog = new ColorDialogFragment();
                colorDialog.show(getFragmentManager(), "color dialog");
                return true; // consume the menu event
            case R.id.line_width:
                LineWidthDialogFragment widthDialog =
                        new LineWidthDialogFragment();
                widthDialog.show(getFragmentManager(), "line width dialog");
                return true; // consume the menu event
            case R.id.delete_drawing:
                confirmErase(); // confirm before erasing image
                return true; // consume the menu event
            case R.id.save:
                saveImage(); // check permission and save current image
                return true; // consume the menu event
            case R.id.print:
                doodleView.printImage(); // print the current images
                return true; // consume the menu event
        }

        return super.onOptionsItemSelected(item);
    }

    // requests for the permission needed for saving the image if
    // necessary or saves the image if the app already has permission
    private void saveImage() {
        // checks if the app does not have permission needed
        // to save the image
        if (getContext().checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {

            // shows an explanation for why permission is needed
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder =
                        new AlertDialog.Builder(getActivity());

                // set Alert Dialog's message
                builder.setMessage(R.string.permission_explanation);

                // add an OK button to the dialog
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // request permission
                                requestPermissions(new String[]{
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        SAVE_IMAGE_PERMISSION_REQUEST_CODE);
                            }
                        }
                );

                // display the dialog
                builder.create().show();
            } else {
                // request permission
                requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        SAVE_IMAGE_PERMISSION_REQUEST_CODE);
            }
        } else { // if app already has permission to write to external storage
            doodleView.saveImage(); // save the image
        }
    }

    // called by the system when the user either grants or denies the
    // permission for saving an image
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        // switch chooses appropriate action based on which feature
        // requested permission
        switch (requestCode) {
            case SAVE_IMAGE_PERMISSION_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    doodleView.saveImage(); // save the image
                return;
        }
    }

    // returns the DoodleView
    public DoodleView getDoodleView() {
        return doodleView;
    }

    // indicates whether a dialog is displayed
    public void setDialogOnScreen(boolean visible) {
        dialogOnScreen = visible;
    }
}