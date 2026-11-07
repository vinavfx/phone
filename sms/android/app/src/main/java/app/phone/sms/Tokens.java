package app.phone.sms;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.SecureRandom;

final class Tokens {
    private Tokens() {
    }

    static synchronized String get(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("sync", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);
        if (token == null) {
            byte[] bytes = new byte[24];
            new SecureRandom().nextBytes(bytes);
            StringBuilder hex = new StringBuilder();
            for (byte value : bytes) {
                hex.append(String.format("%02x", value));
            }
            token = hex.toString();
            preferences.edit().putString("token", token).apply();
        }
        return token;
    }
}
