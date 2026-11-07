package app.phone.sms;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import java.io.IOException;

public final class SyncService extends Service {
    private static final String CHANNEL = "sync";

    private HttpServer server;
    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;

    static void start(Context context) {
        context.startForegroundService(new Intent(context, SyncService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        acquireLocks();
        server = new HttpServer(new SmsRepository(this), Tokens.get(this));
        try {
            server.start();
        } catch (IOException exception) {
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        server.stop();
        wifiLock.release();
        wakeLock.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void acquireLocks() {
        WifiManager wifi = getSystemService(WifiManager.class);
        wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "sms:wifi");
        wifiLock.acquire();
        PowerManager power = getSystemService(PowerManager.class);
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sms:server");
        wakeLock.acquire();
    }

    private Notification notification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Sync service", NotificationManager.IMPORTANCE_MIN));
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_sms)
                .setContentTitle("SMS Sync")
                .setContentText("Sharing messages on port " + HttpServer.PORT)
                .setContentIntent(open)
                .setOngoing(true)
                .build();
    }
}
