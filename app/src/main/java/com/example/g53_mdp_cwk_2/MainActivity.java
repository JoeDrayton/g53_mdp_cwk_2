package com.example.g53_mdp_cwk_2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.io.File;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.Intent;
import android.os.Bundle;
import android.widget.SeekBar;


public class MainActivity extends AppCompatActivity {
    private Messenger messenger;
    private Messenger replyMessenger;
    public static final int MAX = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Start and bind to the service
        this.startService(new Intent(this, MyService.class));
        this.bindService(new Intent(this, MyService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        // Set the reply messenger to a new messenger connected to my handler class
        replyMessenger = new Messenger(new MyHandler());
        setContentView(R.layout.activity_main);

        // Get the listview from the activity layout, populate the list from /Music/
        final ListView lv = findViewById(R.id.listView);
        File musicDir = new File(
                Environment.getExternalStorageDirectory().getPath()+ "/Music/");
        File list[] = musicDir.listFiles();
        lv.setAdapter(new ArrayAdapter<File>(this, android.R.layout.simple_list_item_1, list));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // On item click get file path and send a message to the service to begin playback
                File selectedFromList = (File) lv.getItemAtPosition(position);
                Log.d("g53mdp", selectedFromList.getAbsolutePath());
                Message message = Message.obtain(null, MyService.START, 0, 0);
                message.obj = selectedFromList.getAbsolutePath();
                try {
                    messenger.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg){
            switch(msg.what) {
                case MyService.DURATION:
                    // When a DURATION message is sent use it to set the seekbar progress
                    final int duration = msg.arg1;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SeekBar sb = findViewById(R.id.progress);
                            sb.setProgress(duration);
                        }
                    });
                    break;
                case MainActivity.MAX:
                    // When a MAX message is sent use it to set the seekbar MAX
                    final int max = (msg.arg1/1000);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SeekBar sb = findViewById(R.id.progress);
                            sb.setMax(max);
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    }
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // When the service is connected, connect the messenger to the service
            Log.d("g53mdp", "service connected");
            messenger = new Messenger(service);

            try{
                // Sends a register message to the service
                Message msg = Message.obtain(null, MyService.REGISTER);
                msg.replyTo = replyMessenger;
                messenger.send(msg);
            } catch (RemoteException e){

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // nullify the messenger
            Log.d("g53mdp","service disconnected");
            messenger = null;
        }
    };

    public void onPlay(View v){
        // Send a message to the service informing it to play
        Message message = Message.obtain(null, MyService.PLAY, 0, 0);
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onPause(View v){
        // Send a message to the service informing it to pause
        Message message = Message.obtain(null, MyService.PAUSE, 0, 0);
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onStop(View v){
        // Send a message to the service informing it to stop playback
        Message message = Message.obtain(null, MyService.STOP, 0, 0);
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        // if the connection is active, sends a unregister message to the service
        Log.d("g53mdp", "activity destroyed");
        if(serviceConnection!= null){

            try {
                Message msg = Message.obtain(null, MyService.UNREGISTER);
                msg.replyTo = replyMessenger;
                messenger.send(msg);
            } catch (RemoteException e) {

            }
            unbindService(serviceConnection);
            serviceConnection = null;
        }
    }
}
