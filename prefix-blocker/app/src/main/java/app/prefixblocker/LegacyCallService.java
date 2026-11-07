package app.prefixblocker;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.DisconnectCause;
import android.telecom.InCallService;
import java.util.ArrayList;
import java.util.List;

public final class LegacyCallService extends InCallService {
    static LegacyCallService instance;
    Runnable observer;
    private PowerManager.WakeLock proximity;
    private final Call.Callback callback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            refresh();
        }

        @Override
        public void onDetailsChanged(Call call, Call.Details details) {
            refresh();
        }

        @Override
        public void onConferenceableCallsChanged(Call call, List<Call> calls) {
            refresh();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        NotificationChannel channel = new NotificationChannel(
                "phone_calls", "Phone calls", NotificationManager.IMPORTANCE_HIGH);
        channel.setSound(null, null);
        channel.enableVibration(false);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        PowerManager power = getSystemService(PowerManager.class);
        if (power.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            proximity = power.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "PrefixBlocker:Proximity");
        }
    }

    @Override
    public void onCallAdded(Call call) {
        call.registerCallback(callback);
        refresh();
        showCallScreen(false);
    }

    @Override
    public void onCallRemoved(Call call) {
        call.unregisterCallback(callback);
        if (call.getDetails().getDisconnectCause() != null
                && call.getDetails().getDisconnectCause().getCode() == DisconnectCause.MISSED) {
            showMissedCall(call);
        }
        refresh();
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState state) {
        refresh();
    }

    @Override
    public void onBringToForeground(boolean showDialpad) {
        showCallScreen(showDialpad);
    }

    private void showCallScreen(boolean showDialpad) {
        startActivity(new Intent(this, CallActivity.class)
                .putExtra("show_dialpad", showDialpad)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
    }

    List<Call> liveCalls() {
        List<Call> calls = new ArrayList<>();
        for (Call call : getCalls()) {
            if (call.getState() != Call.STATE_DISCONNECTED) {
                calls.add(call);
            }
        }
        return calls;
    }

    private void refresh() {
        List<Call> calls = liveCalls();
        NotificationManager notifications = getSystemService(NotificationManager.class);
        boolean ringing = false;
        boolean active = false;
        for (Call call : calls) {
            ringing |= call.getState() == Call.STATE_RINGING;
            active |= call.getState() == Call.STATE_ACTIVE;
        }
        CallAudioState audio = getCallAudioState();
        boolean nearEar = active && audio != null && audio.getRoute() == CallAudioState.ROUTE_EARPIECE;
        if (proximity != null) {
            if (nearEar && !proximity.isHeld()) {
                proximity.acquire(2 * 60 * 60 * 1000L);
            } else if (!nearEar && proximity.isHeld()) {
                proximity.release();
            }
        }
        if (calls.isEmpty()) {
            notifications.cancel(1);
        } else {
            PendingIntent screen = PendingIntent.getActivity(this, 1,
                    new Intent(this, CallActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder notification = new Notification.Builder(this, "phone_calls")
                    .setSmallIcon(R.drawable.ic_shield)
                    .setContentTitle(ringing ? "Incoming call" : "Call in progress")
                    .setContentText("Tap to open call controls")
                    .setCategory(Notification.CATEGORY_CALL)
                    .setVisibility(Notification.VISIBILITY_PRIVATE)
                    .setContentIntent(screen)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true);
            if (ringing) {
                notification.setFullScreenIntent(screen, true);
            }
            notifyCall(1, notification.build());
        }
        if (observer != null) {
            observer.run();
        }
    }

    private void showMissedCall(Call call) {
        Intent intent = new Intent(this, DialerActivity.class).setAction(Intent.ACTION_DIAL)
                .setData(call.getDetails().getHandle());
        PendingIntent screen = PendingIntent.getActivity(this, 2, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        notifyCall(2, new Notification.Builder(this, "phone_calls")
                        .setSmallIcon(R.drawable.ic_shield)
                        .setContentTitle("Missed call")
                        .setContentIntent(screen)
                        .setAutoCancel(true)
                        .build());
    }

    @SuppressLint("NotificationPermission")
    private void notifyCall(int id, Notification notification) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            getSystemService(NotificationManager.class).notify(id, notification);
        }
    }

    @Override
    public void onDestroy() {
        instance = null;
        getSystemService(NotificationManager.class).cancel(1);
        if (proximity != null && proximity.isHeld()) {
            proximity.release();
        }
        if (observer != null) {
            observer.run();
        }
        super.onDestroy();
    }
}
