package app.phone.sms;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MainActivity extends Activity {
    private TextView status;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(20), dp(24), dp(12));
        TextView title = text("SMS Sync", 28);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);
        root.addView(text("Shares your SMS with the sms app on your computer "
                + "over the local network.", 15));
        status = text("", 15);
        status.setPadding(0, dp(16), 0, 0);
        status.setTextIsSelectable(true);
        root.addView(status);
        setContentView(root);
        requestPermissions(permissions(), 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] names, int[] results) {
        refresh();
    }

    private void refresh() {
        if (checkSelfPermission(Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            status.setText("Allow SMS access to start syncing.");
            return;
        }
        SyncService.start(this);
        status.setText("Running on " + addresses() + " port " + HttpServer.PORT
                + "\n\nPairing token:\n" + Tokens.get(this));
    }

    private static String[] permissions() {
        List<String> names = new ArrayList<>();
        Collections.addAll(names, Manifest.permission.READ_SMS,
                Manifest.permission.READ_CONTACTS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            names.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return names.toArray(new String[0]);
    }

    private static String addresses() {
        List<String> found = new ArrayList<>();
        try {
            for (NetworkInterface network : Collections.list(
                    NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress address : Collections.list(network.getInetAddresses())) {
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        found.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
            return "unknown address";
        }
        return found.isEmpty() ? "no network" : String.join(", ", found);
    }

    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
