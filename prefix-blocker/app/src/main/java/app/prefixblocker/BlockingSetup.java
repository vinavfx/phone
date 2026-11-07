package app.prefixblocker;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telecom.TelecomManager;

final class BlockingSetup {
    private BlockingSetup() {}

    static boolean isAvailable(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roles = context.getSystemService(RoleManager.class);
            return roles != null && roles.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING);
        }
        return context.getSystemService(TelecomManager.class) != null;
    }

    static boolean isEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roles = context.getSystemService(RoleManager.class);
            return roles != null && roles.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
        }
        TelecomManager telecom = context.getSystemService(TelecomManager.class);
        return telecom != null && context.getPackageName().equals(telecom.getDefaultDialerPackage());
    }

    static Intent activationIntent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return context.getSystemService(RoleManager.class)
                    .createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
        }
        return new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                        context.getPackageName());
    }
}
