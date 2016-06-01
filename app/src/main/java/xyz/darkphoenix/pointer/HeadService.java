package xyz.darkphoenix.pointer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.IBinder;

import java.io.DataOutputStream;
import java.io.IOException;

public class HeadService extends Service {

    private final static int FOREGROUND_ID = 999;
    private BroadcastReceiver receiver = new Receiver();
    private HeadLayer headLayer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        headLayer = new HeadLayer(this);

        setupReceiver();
        startForeground(FOREGROUND_ID, createNotification());

        return START_STICKY;
    }

    private Notification createNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        return new Notification.Builder(this)
                .setContentTitle("Pointer")
                .setContentText("Tap to configure")
                .setContentIntent(pendingIntent)
                .build();
    }

    private void setupReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.VOLUME_KEY_EVENT);
        filter.addAction(Constants.CLICK_AND_FINISH);
        filter.addAction(Constants.LONG_CLICK_AND_FINISH);

        registerReceiver(receiver, filter);
    }

    private void clickAndFinish(boolean longPress) {

        Point point = headLayer.getPoint();
        String cmd;

        if (headLayer.getResolution().y != headLayer.getHeight())
            point.y += getStatusBarHeight();

        if (longPress)
            cmd = "input swipe " + point.x + " " + point.y + " " + point.x + " " + point.y + " " + 1000;
        else
            cmd = "input tap " + point.x + " " + point.y;

        try {
            runAsRoot(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }

        stopSelf();
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public void runAsRoot(String cmd) throws IOException {
        Process p = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        os.writeBytes(cmd + "\n");
        os.writeBytes("exit\n");
        os.flush();
    }

    @Override
    public void onDestroy() {
        headLayer.destroy();
        headLayer = null;

        unregisterReceiver(receiver);

        stopForeground(true);
    }

    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Constants.VOLUME_KEY_EVENT)) {

                boolean volumeUpPressed = intent.getBooleanExtra(Constants.VOLUME_UP_PRESSED, false);
                boolean volumeDownPressed = intent.getBooleanExtra(Constants.VOLUME_DOWN_PRESSED, false);
                headLayer.update(volumeUpPressed, volumeDownPressed);

            } else if (intent.getAction().equals(Constants.CLICK_AND_FINISH)) {
                clickAndFinish(false);
            } else if (intent.getAction().equals(Constants.LONG_CLICK_AND_FINISH)) {
                clickAndFinish(true);
            }
        }
    }
}
