package app.phone.sms;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context.checkSelfPermission(Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            SyncService.start(context);
        }
    }
}
