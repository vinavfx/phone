package app.prefixblocker;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.SystemClock;
import android.provider.CallLog;
import java.util.Set;

final class BlockedCallCleaner {
    private static final long RETRY_MILLIS = 2000;
    private static final int ATTEMPTS = 30;

    private BlockedCallCleaner() {}

    static boolean isAllowed(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.WRITE_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED;
    }

    static void cleanSoon(Context context) {
        Context app = context.getApplicationContext();
        if (!isAllowed(app)) {
            return;
        }
        new Thread(() -> {
            for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
                SystemClock.sleep(RETRY_MILLIS);
                if (clean(app) > 0) {
                    return;
                }
            }
        }).start();
    }

    static void cleanNow(Context context) {
        Context app = context.getApplicationContext();
        if (isAllowed(app)) {
            new Thread(() -> clean(app)).start();
        }
    }

    private static int clean(Context context) {
        String region = PrefixScreeningService.region(context);
        Set<String> prefixes = new PrefixStore(context).getPrefixes();
        ContentResolver resolver = context.getContentResolver();
        int deleted = 0;
        try (Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls._ID, CallLog.Calls.NUMBER},
                CallLog.Calls.TYPE + " = ?",
                new String[]{String.valueOf(CallLog.Calls.BLOCKED_TYPE)}, null)) {
            while (cursor != null && cursor.moveToNext()) {
                if (PrefixRules.blocks(cursor.getString(1), region, prefixes)) {
                    deleted += resolver.delete(CallLog.Calls.CONTENT_URI,
                            CallLog.Calls._ID + " = ?",
                            new String[]{String.valueOf(cursor.getLong(0))});
                }
            }
        } catch (SecurityException ignored) {
        }
        return deleted;
    }
}
