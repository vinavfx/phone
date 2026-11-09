package app.prefixblocker;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import java.util.Locale;

public final class PrefixScreeningService extends CallScreeningService {
    @Override
    public void onScreenCall(Call.Details details) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && details.getCallDirection() != Call.Details.DIRECTION_INCOMING) {
            return;
        }
        Uri handle = details.getHandle();
        boolean blocked = handle != null && "tel".equals(handle.getScheme())
                && PrefixRules.blocks(handle.getSchemeSpecificPart(), region(this),
                        new PrefixStore(this).getPrefixes());
        respondToCall(details, new CallResponse.Builder()
                .setDisallowCall(blocked)
                .setRejectCall(blocked)
                .setSkipNotification(blocked)
                .setSkipCallLog(blocked)
                .build());
        if (blocked) {
            BlockedCallCleaner.cleanSoon(this);
        }
    }

    static String region(Context context) {
        TelephonyManager telephony = context.getSystemService(TelephonyManager.class);
        String country = telephony == null ? "" : telephony.getNetworkCountryIso();
        if (TextUtils.isEmpty(country) && telephony != null) {
            country = telephony.getSimCountryIso();
        }
        return (TextUtils.isEmpty(country) ? Locale.getDefault().getCountry() : country)
                .toUpperCase(Locale.ROOT);
    }
}
