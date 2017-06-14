package com.adinavm.trauma_background;

import android.app.Service;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.hardware.SensorEventListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.content.Context;
import android.view.View;
import android.widget.TextView;




public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickStartService(View view){
        Intent startingIntent = new Intent(this, MyService.class);
        startService(startingIntent);
    }

    public void onClickStopService(View view){
        Intent stoppingIntent = new Intent(this, MyService.class);
        stopService(stoppingIntent);
    }

}