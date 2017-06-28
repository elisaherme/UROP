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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.content.ContentValues.TAG;

public class MyService extends Service implements SensorEventListener {


    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

    @Override
    public void onCreate() {


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
    Messenger replyMessanger;
    final static int MESSAGE = 1;

    @Override
    public void onSensorChanged(SensorEvent event) {

        String file_name = "hello_file";
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

                    sendDataToActivity(duration, max_acc);
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



    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            if (msg.what == MESSAGE) {
                Bundle bundle = msg.getData();
                //toast(bundle.getString("rec"));//message received
                replyMessanger = msg.replyTo; //init reply messenger
            }
        }
    }

    private void sendDataToActivity(int duration, float max_acc) {

        if (replyMessanger != null)
            try {
                Message message = new Message();
                message.obj = "Duration is " + duration + "ms and the max acceleration felt is " + max_acc + "m/s^2";
                replyMessanger.send(message);//replying / sending msg to activity
            } catch (RemoteException e) {
                e.printStackTrace();
            }
    }
    Messenger messenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

}