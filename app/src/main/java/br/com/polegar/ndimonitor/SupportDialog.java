package br.com.polegar.ndimonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;

/** User-initiated report and an explicit, optional automatic-sharing control. */
final class SupportDialog {
    private final Activity owner;
    private final SupportRuntime runtime;
    private AlertDialog dialog;
    private int generation;
    private SupportMetrics.Sample sample;

    SupportDialog(Activity owner, SupportRuntime runtime) {
        this.owner = owner;
        this.runtime = runtime;
    }

    void show() {
        if (dialog != null && dialog.isShowing()) return;
        final int token = ++generation;
        sample = null;
        int padding = dp(16);
        LinearLayout content = new LinearLayout(owner);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, padding, padding, padding);
        text(content, R.string.support_intro);
        text(content, R.string.support_category_label);
        Spinner category = new Spinner(owner);
        category.setId(R.id.support_category);
        category.setMinimumHeight(dp(48));
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(owner,
                R.array.support_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        category.setAdapter(adapter);
        content.addView(category, new LinearLayout.LayoutParams(-1, -2));

        TextView metrics = text(content, R.string.support_measuring);
        metrics.setId(R.id.support_metrics);
        metrics.setTextIsSelectable(true);
        Button send = button(content, R.string.support_send, R.id.support_send_button);
        Button copy = button(content, R.string.support_copy, R.id.support_copy_button);
        send.setEnabled(false);
        copy.setEnabled(false);
        TextView status = text(content, runtime.configured()
                ? R.string.support_server_ready : R.string.support_server_pending);
        status.setId(R.id.support_status);
        CheckBox automatic = new CheckBox(owner);
        automatic.setId(R.id.support_auto_checkbox);
        automatic.setText(R.string.support_automatic);
        automatic.setMinHeight(dp(48));
        automatic.setChecked(runtime.automaticEnabled());
        automatic.setEnabled(runtime.configured());
        content.addView(automatic, new LinearLayout.LayoutParams(-1, -2));
        LocalControlStore local = MonitorApplication.local(owner);
        if (local.enabled() && !local.token().isEmpty()) {
            Button notifyLocal = new Button(owner);
            notifyLocal.setText(R.string.support_notify_local);
            notifyLocal.setAllCaps(false);
            content.addView(notifyLocal, new LinearLayout.LayoutParams(-1, -2));
            notifyLocal.setOnClickListener(view -> {
                int selected = category.getSelectedItemPosition();
                local.event("manual_" + SupportPayload.CATEGORIES[Math.max(0, Math.min(selected, SupportPayload.CATEGORIES.length - 1))]);
                Toast.makeText(owner, R.string.support_local_notified, Toast.LENGTH_SHORT).show();
            });
        }
        text(content, R.string.support_data_scope);
        automatic.setOnCheckedChangeListener((view, checked) -> runtime.setAutomatic(checked));

        ScrollView scroll = new ScrollView(owner);
        scroll.addView(content);
        dialog = new AlertDialog.Builder(owner).setTitle(R.string.support_report)
                .setView(scroll).setPositiveButton(R.string.close, null).create();
        dialog.setOnDismissListener(ignored -> {
            ++generation;
            dialog = null;
        });
        dialog.show();
        runtime.snapshot(measured -> {
            if (!active(token)) return;
            sample = measured;
            metrics.setText(describeMetrics(measured));
            copy.setEnabled(true);
            send.setEnabled(runtime.configured());
        });

        copy.setOnClickListener(view -> {
            if (sample == null) return;
            ClipboardManager clipboard = (ClipboardManager) owner.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) return;
            String report = owner.getString(R.string.support_report_heading, runtime.version())
                    + "\n" + owner.getString(R.string.support_category_label) + " " + category.getSelectedItem()
                    + "\n" + describeMetrics(sample);
            clipboard.setPrimaryClip(ClipData.newPlainText("S-Retorno", report));
            Toast.makeText(owner, R.string.support_copied, Toast.LENGTH_SHORT).show();
        });
        send.setOnClickListener(view -> {
            if (sample == null || !runtime.configured()) return;
            try {
                int selected = category.getSelectedItemPosition();
                JSONObject event = runtime.manualReport(sample,
                        SupportPayload.CATEGORIES[Math.max(0, Math.min(selected, SupportPayload.CATEGORIES.length - 1))]);
                send.setEnabled(false);
                status.setText(R.string.support_sending);
                runtime.sendManual(event, accepted -> {
                    if (!active(token)) return;
                    send.setEnabled(true);
                    status.setText(accepted ? R.string.support_sent : R.string.support_send_failed);
                });
            } catch (JSONException unavailable) {
                status.setText(R.string.support_send_failed);
            }
        });
    }

    private boolean active(int token) {
        return generation == token && dialog != null && dialog.isShowing()
                && !owner.isFinishing() && !owner.isDestroyed();
    }

    private String describeMetrics(SupportMetrics.Sample value) {
        String unavailable = owner.getString(R.string.support_metric_unavailable);
        String memory = value.pssKiB == null ? unavailable : String.format(Locale.getDefault(), "%.1f MB", value.pssKiB / 1024d);
        String cpu = value.cpuPercent == null ? unavailable : String.format(Locale.getDefault(), "%.2f%%", value.cpuPercent);
        String rx = rate(value.rxBytes, value.windowMs, unavailable);
        String tx = rate(value.txBytes, value.windowMs, unavailable);
        return owner.getString(R.string.support_metrics_text, memory, cpu, rx, tx);
    }

    private static String rate(Long bytes, Long windowMs, String unavailable) {
        return bytes == null || windowMs == null || windowMs <= 0 ? unavailable
                : String.format(Locale.getDefault(), "%.2f Mbit/s", bytes * .008d / windowMs);
    }

    private TextView text(LinearLayout parent, int label) {
        TextView text = new TextView(owner);
        text.setText(label);
        text.setTextSize(16);
        text.setPadding(0, dp(8), 0, dp(8));
        parent.addView(text, new LinearLayout.LayoutParams(-1, -2));
        return text;
    }

    private Button button(LinearLayout parent, int label, int id) {
        Button button = new Button(owner);
        button.setId(id);
        button.setText(label);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        parent.addView(button, new LinearLayout.LayoutParams(-1, -2));
        return button;
    }

    private int dp(int value) { return Math.round(value * owner.getResources().getDisplayMetrics().density); }

    void close() {
        ++generation;
        if (dialog != null) dialog.dismiss();
        dialog = null;
    }
}
