package app.prefixblocker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

public final class CallActivity extends Activity {
    private final Runnable observer = this::render;
    private LegacyCallService service;
    private boolean showDialpad;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        showDialpad = savedInstanceState != null
                ? savedInstanceState.getBoolean("show_dialpad")
                : getIntent().getBooleanExtra("show_dialpad", false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        service = LegacyCallService.instance;
        if (service != null) {
            service.observer = observer;
        }
        render();
    }

    @Override
    protected void onStop() {
        if (service != null && service.observer == observer) {
            service.observer = null;
        }
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showDialpad |= intent.getBooleanExtra("show_dialpad", false);
        render();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("show_dialpad", showDialpad);
        super.onSaveInstanceState(state);
    }

    private void render() {
        if (service == null || service != LegacyCallService.instance
                || service.liveCalls().isEmpty()) {
            finish();
            return;
        }
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = column();
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        root.setPadding(padding, padding, padding, padding);
        scroll.addView(root);
        setContentView(scroll);
        for (Call call : service.liveCalls()) {
            renderCall(root, call);
        }
        renderAudio(root);
        if (service.canAddCall()) {
            addButton(root, "Add call", () -> startActivity(new Intent(this, DialerActivity.class)));
        }
    }

    private void renderCall(LinearLayout root, Call call) {
        Call.Details details = call.getDetails();
        String number = details.getHandle() == null ? "Unknown number"
                : details.getHandle().getSchemeSpecificPart();
        TextView heading = new TextView(this);
        heading.setText(number);
        heading.setTextSize(26);
        root.addView(heading);
        TextView state = new TextView(this);
        state.setText(stateLabel(call.getState()));
        state.setTextSize(18);
        root.addView(state);
        if (call.getState() == Call.STATE_RINGING) {
            addButton(root, "Answer with audio", () -> call.answer(VideoProfile.STATE_AUDIO_ONLY));
            addButton(root, "Reject", () -> call.reject(false, null));
        } else {
            if (call.getState() == Call.STATE_SELECT_PHONE_ACCOUNT) {
                renderAccounts(root, call);
            }
            if (details.can(Call.Details.CAPABILITY_HOLD)) {
                boolean held = call.getState() == Call.STATE_HOLDING;
                addButton(root, held ? "Resume" : "Hold", () -> {
                    if (held) {
                        call.unhold();
                    } else {
                        call.hold();
                    }
                });
            }
            if (details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE)) {
                addButton(root, "Merge calls", call::mergeConference);
            } else if (!call.getConferenceableCalls().isEmpty()) {
                addButton(root, "Merge calls", () -> {
                    List<Call> candidates = call.getConferenceableCalls();
                    if (!candidates.isEmpty()) {
                        call.conference(candidates.get(0));
                    }
                });
            }
            addButton(root, "End call", call::disconnect);
            if (call.getState() == Call.STATE_ACTIVE) {
                addButton(root, showDialpad ? "Hide keypad" : "Show keypad", () -> {
                    showDialpad = !showDialpad;
                    render();
                });
                if (showDialpad) {
                    renderKeypad(root, call);
                }
            }
        }
    }

    private void renderAccounts(LinearLayout root, Call call) {
        Bundle extras = call.getDetails().getIntentExtras();
        List<PhoneAccountHandle> accounts = extras == null ? null
                : extras.getParcelableArrayList(Call.AVAILABLE_PHONE_ACCOUNTS);
        if (accounts == null) {
            return;
        }
        TelecomManager telecom = getSystemService(TelecomManager.class);
        for (PhoneAccountHandle account : accounts) {
            PhoneAccount info = telecom.getPhoneAccount(account);
            String label = info == null ? "Select SIM" : info.getLabel().toString();
            addButton(root, label, () -> call.phoneAccountSelected(account, false));
        }
    }

    private void renderAudio(LinearLayout root) {
        CallAudioState audio = service.getCallAudioState();
        if (audio == null) {
            return;
        }
        addButton(root, audio.isMuted() ? "Unmute" : "Mute",
                () -> service.setMuted(!audio.isMuted()));
        LinearLayout routes = new LinearLayout(this);
        int[] values = {CallAudioState.ROUTE_EARPIECE, CallAudioState.ROUTE_SPEAKER,
                CallAudioState.ROUTE_BLUETOOTH, CallAudioState.ROUTE_WIRED_HEADSET};
        String[] labels = {"Earpiece", "Speaker", "Bluetooth", "Headset"};
        for (int index = 0; index < values.length; index++) {
            int route = values[index];
            if ((audio.getSupportedRouteMask() & route) != 0) {
                Button button = addButton(routes, labels[index], () -> service.setAudioRoute(route));
                button.setEnabled(audio.getRoute() != route);
                button.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            }
        }
        root.addView(routes);
    }

    private void renderKeypad(LinearLayout root, Call call) {
        for (String digits : new String[]{"123", "456", "789", "*0#"}) {
            LinearLayout row = new LinearLayout(this);
            for (char digit : digits.toCharArray()) {
                Button key = addButton(row, String.valueOf(digit), () -> {
                    call.playDtmfTone(digit);
                    new Handler(Looper.getMainLooper()).postDelayed(call::stopDtmfTone, 180);
                });
                key.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            }
            root.addView(row);
        }
    }

    private static String stateLabel(int state) {
        switch (state) {
            case Call.STATE_RINGING:
                return "Incoming call";
            case Call.STATE_ACTIVE:
                return "Connected";
            case Call.STATE_HOLDING:
                return "On hold";
            case Call.STATE_SELECT_PHONE_ACCOUNT:
                return "Select a SIM";
            case Call.STATE_CONNECTING:
            case Call.STATE_DIALING:
                return "Calling";
            case Call.STATE_DISCONNECTING:
                return "Ending call";
            default:
                return "Connecting";
        }
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private Button addButton(LinearLayout layout, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(view -> action.run());
        layout.addView(button);
        return button;
    }
}
