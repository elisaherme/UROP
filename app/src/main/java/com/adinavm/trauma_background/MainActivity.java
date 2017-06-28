package com.adinavm.trauma_background;

import android.Manifest;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    //  private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0;
    //  private static final int REQUEST_CODE_PERMISSION = 2;
    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;

    public double latitude;
    public double longitude;

    GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gps = new GPSTracker(MainActivity.this);

        // check if GPS enabled
        if (gps.canGetLocation()) {

            latitude = gps.getLatitude();
            longitude = gps.getLongitude();

            Toast.makeText(getApplicationContext(), "Your Location is - \nLat: "
                    + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }
    }


    public void onClickStartService(View view) {
        Intent startingIntent = new Intent(this, MyService.class);
        startService(startingIntent);
    }

    public void onClickStopService(View view) {
        Intent stoppingIntent = new Intent(this, MyService.class);
        stopService(stoppingIntent);
    }

    Messenger msgService;
    boolean isBound;

    ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;
            msgService = new Messenger(service);
        }
    };

    public void sendMessage(View view) {
        if (isBound) {
            try {
                Message message = Message.obtain(null, MyService.MESSAGE, 1, 1);
                message.replyTo = replyMessenger;

                Bundle bundle = new Bundle();
                bundle.putString("rec", "Hi, you hear me");
                message.setData(bundle);

                msgService.send(message); //sending message to service

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    //setting reply messenger and handler
    Messenger replyMessenger = new Messenger(new HandlerReplyMsg());
}

class HandlerReplyMsg extends Handler {
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        String recdMessage = msg.obj.toString(); //msg received from service
        //toast(recdMessage);

        //message received from service with max acceleration and duration, hopefully will send this info in text
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage("+447400534303", null, recdMessage, null, null);
        //then need to send a text with gps location
    }

}