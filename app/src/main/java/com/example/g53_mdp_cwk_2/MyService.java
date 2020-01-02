package com.example.g53_mdp_cwk_2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.os.Message;
import android.os.Messenger;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

public class MyService extends Service {
    private Messenger messenger;
    public static final int PLAY = 0;
    public static final int PAUSE = 1;
    public static final int STOP = 3;
    public static final int START = 4;
    public static final int REGISTER = 5;
    public static final int UNREGISTER = 6;
    public static final int DURATION = 7;
    private final String CHANNEL_ID = "100";
    int NOTIFICATION_ID = 001;
    ArrayList<Messenger> clientMessengers = new ArrayList<>();
    private Player player;
    public MP3Player mp3;

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d("g53mdp", "Create that service");
        // Creates a new player, MP3Player and messenger
        player = new Player();
        messenger = new Messenger(new MessageHandler());
        mp3  = new MP3Player();
        this.createNotification();

    }

    @Override
    public IBinder onBind(Intent intent) {
        // on bind returns the messenger binder
        return messenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d("g53mdp", "service onStartCommand called");
        // Starts the service
        return Service.START_STICKY;
    }

    public void createNotification(){
        // Definition of notification manager
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "channel name";
            String description = "channel description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }
        // Create a pending intent to return us back to main activity when notification is clicked
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Set notification text and data
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Super Music Player")
                .setContentText("Play all your limited music needs")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    // Message handler for service
    private class MessageHandler extends Handler {
        @Override
        public void handleMessage (Message msg) {
            switch (msg.what) {
                case REGISTER:
                    // Register message at start of activity connection
                    clientMessengers.add(msg.replyTo);
                    break;
                case UNREGISTER:
                    // Unregister message at end of activity connection
                    clientMessengers.remove(msg.replyTo);
                    break;
                case PLAY:
                    // Calls play
                    play();
                    break;
                case PAUSE:
                    // Calls pause
                    pause();
                    break;
                case STOP:
                    // Calls stop
                    stop();
                    break;
                case START:
                    // Calls start using the mp3 path
                    String path = (String) msg.obj;
                    start(path);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    protected class Player extends Thread implements Runnable {
        private boolean playing = true;
        private int duration = 0;

        private Player(){
            this.start();
        }

        public void run(){
            while(this.playing){
                try {Thread.sleep(1000);} catch(Exception e) {return;}
                if(mp3.getState() == MP3Player.MP3PlayerState.PLAYING) {
                    // As long as mp3 is playing adds to time counter
                    duration ++;
                    Log.d("g53mdp", "Duration " + duration);
                }

                // Sends duration information back to activity
                for(int i=clientMessengers.size()-1; i>=0; i--) {
                    try {
                        clientMessengers.get(i).send(Message.obtain(null, DURATION, duration, 0));
                    }
                    catch(RemoteException e){
                        clientMessengers.remove(i);
                    }
                }
            }
        }
    }

        public void start(String path){
            // Loads the supplied mp3 path into the MP3Player
            if(mp3.getState() != MP3Player.MP3PlayerState.STOPPED){
                mp3.stop();
            }
            Log.d("g53mdp", "loading song " + path);
            player.duration = 0;
            mp3.load(path);
            try {
                clientMessengers.get(clientMessengers.size()-1).send(Message.obtain(null, MainActivity.MAX, mp3.getDuration(), 0));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void play(){
            player.playing = true;
            Log.d("g53mdp", "play pressed");
            mp3.play();
            // Calls mp3.play()
        }

        public void pause(){
            Log.d("g53mdp", "pause pressed");
            mp3.pause();
            // Calls mp3.pause()
        }

        public void stop() {
            Log.d("g53mdp", "stop pressed");
            mp3.stop();
            player.duration = 0;
            this.stopSelf();
            // Calls mp3.stop and this.stopSelf();
        }

    @Override
    public void onDestroy() {
        Log.d("g53mdp", "service onDestroy");
        player.playing = false;
        player = null;
        super.onDestroy();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d("g53mdp", "service onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("g53mdp", "service onUnbind");
        return super.onUnbind(intent);
    }
}
