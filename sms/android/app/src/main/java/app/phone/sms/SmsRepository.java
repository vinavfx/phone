package app.phone.sms;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class SmsRepository {
    private static final Uri SMS = Uri.parse("content://sms");
    private static final String[] COLUMNS = {
        "_id", "thread_id", "address", "body", "date", "type", "read"
    };

    private final Context context;
    private final Map<String, String> names = new HashMap<>();

    SmsRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    JSONArray messages(long since, int limit) throws JSONException {
        JSONArray result = new JSONArray();
        String[] args = {Long.toString(since)};
        try (Cursor cursor = context.getContentResolver()
                .query(SMS, COLUMNS, "date >= ?", args, "date DESC")) {
            while (cursor != null && cursor.moveToNext() && result.length() < limit) {
                result.put(message(cursor));
            }
        }
        return result;
    }

    private JSONObject message(Cursor cursor) throws JSONException {
        String address = cursor.getString(2);
        return new JSONObject()
                .put("id", cursor.getLong(0))
                .put("thread", cursor.getLong(1))
                .put("address", address == null ? "" : address)
                .put("name", name(address))
                .put("body", cursor.getString(3) == null ? "" : cursor.getString(3))
                .put("date", cursor.getLong(4))
                .put("sent", cursor.getInt(5) != 1)
                .put("read", cursor.getInt(6) == 1);
    }

    private synchronized String name(String address) {
        if (address == null || address.isEmpty() || !canReadContacts()) {
            return "";
        }
        String cached = names.get(address);
        if (cached != null) {
            return cached;
        }
        String found = "";
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address));
        String[] columns = {PhoneLookup.DISPLAY_NAME};
        try (Cursor cursor = context.getContentResolver().query(uri, columns, null, null, null)) {
            if (cursor != null && cursor.moveToFirst() && cursor.getString(0) != null) {
                found = cursor.getString(0);
            }
        } catch (IllegalArgumentException ignored) {
            found = "";
        }
        names.put(address, found);
        return found;
    }

    private boolean canReadContacts() {
        return context.checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }
}
