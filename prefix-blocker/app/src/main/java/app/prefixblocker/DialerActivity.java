package app.prefixblocker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class DialerActivity extends Activity {
    private EditText number;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        root.setPadding(padding, padding, padding, padding);
        setContentView(root);

        TextView title = new TextView(this);
        title.setText(R.string.phone);
        title.setTextSize(28);
        root.addView(title);
        number = new EditText(this);
        number.setId(android.R.id.edit);
        number.setHint("Phone number");
        number.setContentDescription("Phone number to call");
        number.setInputType(InputType.TYPE_CLASS_PHONE);
        number.setSingleLine(true);
        root.addView(number);
        if (savedInstanceState == null) {
            readNumber(getIntent());
        }

        Button call = new Button(this);
        call.setText(R.string.call);
        call.setOnClickListener(view -> requestCall());
        root.addView(call);
        Button prefixes = new Button(this);
        prefixes.setText(R.string.manage_prefixes);
        prefixes.setOnClickListener(view -> startActivity(new Intent(this, MainActivity.class)));
        root.addView(prefixes);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readNumber(intent);
    }

    private void readNumber(Intent intent) {
        Uri data = intent.getData();
        if (data != null && "tel".equals(data.getScheme())) {
            number.setText(data.getSchemeSpecificPart());
        }
    }

    private void requestCall() {
        if (number.getText().toString().trim().isEmpty()) {
            number.setError("Enter a phone number.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 1);
            return;
        }
        placeCall();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 1 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            placeCall();
        }
    }

    private void placeCall() {
        TelecomManager telecom = getSystemService(TelecomManager.class);
        if (telecom == null) {
            number.setError("Phone service is unavailable.");
            return;
        }
        try {
            telecom.placeCall(Uri.fromParts("tel", number.getText().toString().trim(), null),
                    new Bundle());
        } catch (SecurityException | IllegalArgumentException exception) {
            number.setError("Unable to place the call. Check phone permissions and SIM settings.");
        }
    }
}
