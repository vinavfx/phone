package app.prefixblocker;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

final class PrefixStore {
    private final SharedPreferences preferences;

    PrefixStore(Context context) {
        preferences = context.getSharedPreferences("blocked_prefixes", Context.MODE_PRIVATE);
    }

    Set<String> getPrefixes() {
        return new TreeSet<>(preferences.getStringSet("prefixes", Collections.emptySet()));
    }

    void add(String prefix) {
        Set<String> prefixes = getPrefixes();
        prefixes.add(prefix);
        preferences.edit().putStringSet("prefixes", prefixes).apply();
    }

    void remove(String prefix) {
        Set<String> prefixes = getPrefixes();
        prefixes.remove(prefix);
        preferences.edit().putStringSet("prefixes", prefixes).apply();
    }
}
