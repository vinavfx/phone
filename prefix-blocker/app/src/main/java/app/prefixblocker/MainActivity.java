package app.prefixblocker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Set;

public final class MainActivity extends Activity {
    private PrefixStore store;
    private TextView status;
    private Button activate;
    private Button contacts;
    private EditText input;
    private LinearLayout list;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new PrefixStore(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(20), dp(24), dp(12));
        setContentView(root);

        TextView title = text("Prefix Blocker", 28);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);
        root.addView(text("Block incoming calls by number prefix. No sound. No notification.", 15));

        status = text("", 15);
        status.setPadding(0, dp(16), 0, dp(4));
        root.addView(status);
        activate = button(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                ? "Set as default phone app" : "Enable call blocking");
        activate.setOnClickListener(view -> startActivityForResult(
                BlockingSetup.activationIntent(this), 1));
        root.addView(activate);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            root.addView(text("Android 9 requires this app to be your default phone app. "
                    + "It will also show controls for calls that are not blocked.", 14));
            Button dial = button("Open dial pad");
            dial.setOnClickListener(view -> startActivity(new Intent(this, DialerActivity.class)));
            root.addView(dial);
        }
        contacts = button("Include saved contacts");
        contacts.setOnClickListener(view -> requestPermissions(
                new String[]{Manifest.permission.READ_CONTACTS}, 2));
        root.addView(contacts);

        TextView help = text("Add a national prefix, such as 600 or 80. "
                + "Use + and the country code for an international prefix.", 14);
        help.setPadding(0, dp(12), 0, dp(8));
        root.addView(help);

        LinearLayout entry = new LinearLayout(this);
        entry.setGravity(Gravity.CENTER_VERTICAL);
        input = new EditText(this);
        input.setHint("Prefix (e.g. 600)");
        input.setContentDescription("Number prefix to block");
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setSingleLine(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setOnEditorActionListener((view, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) {
                addPrefix();
                return true;
            }
            return false;
        });
        entry.addView(input, new LinearLayout.LayoutParams(0, dp(56), 1));
        Button add = button("Add");
        add.setOnClickListener(view -> addPrefix());
        entry.addView(add);
        root.addView(entry);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        renderPrefixes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        refreshStatus();
    }

    private void refreshStatus() {
        boolean available = BlockingSetup.isAvailable(this);
        boolean enabled = available && BlockingSetup.isEnabled(this);
        boolean includesContacts = checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
        status.setText(!available ? "Call blocking is not supported on this device."
                : !enabled ? "Inactive: enable call blocking below."
                : includesContacts ? "Active: all matching numbers are blocked."
                : "Active: saved contacts are not screened. Grant contact access to include them.");
        activate.setVisibility(enabled ? View.GONE : View.VISIBLE);
        activate.setEnabled(available);
        contacts.setVisibility(includesContacts ? View.GONE : View.VISIBLE);
    }

    private void addPrefix() {
        String prefix = PrefixRules.normalizePrefix(input.getText().toString());
        if (prefix.isEmpty()) {
            input.setError("Enter 1 to 15 digits, optionally starting with +.");
            return;
        }
        if (store.getPrefixes().contains(prefix)) {
            input.setError("This prefix is already blocked.");
            return;
        }
        store.add(prefix);
        input.setText("");
        renderPrefixes();
    }

    private void renderPrefixes() {
        list.removeAllViews();
        Set<String> prefixes = store.getPrefixes();
        if (prefixes.isEmpty()) {
            TextView empty = text("No blocked prefixes. Add one above to get started.", 16);
            empty.setPadding(0, dp(24), 0, 0);
            list.addView(empty);
            return;
        }
        for (String prefix : prefixes) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = text(prefix + "…", 22);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            Button remove = button("Delete");
            remove.setContentDescription("Delete prefix " + prefix);
            remove.setOnClickListener(view -> {
                store.remove(prefix);
                renderPrefixes();
            });
            row.addView(remove);
            list.addView(row);
        }
    }

    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        return view;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
