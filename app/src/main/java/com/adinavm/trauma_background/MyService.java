package com.adinavm.trauma_background;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.test.mock.MockPackageManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.content.ContentValues.TAG;

public class MyService extends Service implements SensorEventListener {

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

    private static final int REQUEST_CODE_PERMISSION = 2;
    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;

    public double latitude;
    public double longitude;

    GPSTracker gps;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            if (ActivityCompat.checkSelfPermission(this, mPermission)
                    != MockPackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{mPermission},
                        REQUEST_CODE_PERMISSION);

                // If any permission above not allowed by user, this condition will
                // execute every time, else your else part will work
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        gps = new GPSTracker(MyService.this);

        // check if GPS enabled
        if(gps.canGetLocation()){

            latitude = gps.getLatitude();
            longitude = gps.getLongitude();

            Toast.makeText(getApplicationContext(), "Your Location is - \nLat: "
                    + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
        }else{
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }
    }

    @Override
    public void onCreate(){

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startiD){
        Toast.makeText(this, "MyService started ", Toast.LENGTH_LONG).show();

        /*If you return START_STICKY the service gets recreated whenever the
        resources are available. If you return START_NOT_STICKY you have to
        re-activate the service sending a new intent so I have chosen to
        return START_STICKY here so constantly running in the background*/

        return START_STICKY;
    }

    private long lastUpdate = 0;
    private int time = 0;
    private int duration = 0;
    public float max_acc = 0;
    public String max_accl;
    private float last_x, last_y, last_z, last_mag_acceleration;
    // sets the threshold of how sensitive you want the app to be to movement
    private static final int SHAKE_THRESHOLD = 600;

    @Override
    public void onSensorChanged(SensorEvent event) {

        String file_name = "hello_file";
        SmsManager sms = SmsManager.getDefault();
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            Log.d(TAG, "Inside onSensorChanged");
            // to take in the three co-ordinates of the position of the phone
            // x = horizontal movement of the phone
            // y = vertical movement of the phone
            // z = forward/backwards movement of the phone
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float acceleration = (float) Math.sqrt((x*x)+(y*y)+(z*z));


            // constantly moving so to ensure it's not reading all the time set it to only
            // take in another reading if 100ms have gone by
            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {

                    // take in readings every 10ms if acc is above threshold
                    if ((curTime - lastUpdate) > 10) {
                        time++;
                        Log.d(TAG, "Recorded speed above threshold");
                        last_x = x;
                        last_y = y;
                        last_z = z;
                        last_mag_acceleration = acceleration;

                        //Assigns the new acceleration as maximum acceleration if the new one is higher
                        max_acc = Math.max (last_mag_acceleration, max_acc);

                        String x_acc = String.valueOf(last_x);
                        String y_acc = String.valueOf(last_y);
                        String z_acc = String.valueOf(last_z);

                        try {
                            FileOutputStream fileOutputStream = openFileOutput(file_name, MODE_PRIVATE);
                            fileOutputStream.write(x_acc.getBytes());
                            fileOutputStream.write(y_acc.getBytes());
                            fileOutputStream.write(z_acc.getBytes());
                            fileOutputStream.close();
                            Toast.makeText(getApplicationContext(), "Data Saved", Toast.LENGTH_LONG).show();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        lastUpdate = curTime;
                    }
                    duration = time*10;
                    max_accl = String.valueOf(max_acc);
                    sendSMSMessage();
                }
            }
        }
    }

    protected void sendSMSMessage() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.SEND_SMS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage("phoneNo", null, "I had an accident. The maximum acceleration was " + max_accl + ". My last known location is - \nLat: "
                            + latitude + "\nLong: " + longitude, null, null);
                    Toast.makeText(getApplicationContext(), "SMS sent.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "SMS faild, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onDestroy(){
        Toast.makeText(this, "MyService destroyed ", Toast.LENGTH_LONG).show();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        // throw new UnsupportedOperationException("Not yet implemented");#
        return null;
    }
}
