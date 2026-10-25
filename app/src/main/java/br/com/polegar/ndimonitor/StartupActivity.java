package br.com.polegar.ndimonitor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Native-free entry point that can display a report even if MainActivity fails. */
public final class StartupActivity extends Activity {
    static final String EXTRA_HTML_ONLY = "monitor_html_only";
    private StartupJournal journal;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        journal = StartupJournal.get(this);
        if (!journal.needsRecovery()) {
            openMonitor(false);
            return;
        }
        setTitle(R.string.recovery_title);
        int padding = Math.round(16 * getResources().getDisplayMetrics().density);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, padding, padding, padding);
        TextView explanation = new TextView(this);
        explanation.setText(R.string.recovery_explanation);
        content.addView(explanation);
        addButton(content, R.string.recovery_html, () -> openMonitor(true));
        addButton(content, R.string.recovery_retry, () -> openMonitor(false));
        addButton(content, R.string.android_settings, () -> {
            try { startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)); }
            catch (android.content.ActivityNotFoundException unavailable) {
                android.widget.Toast.makeText(this, R.string.settings_unavailable,
                        android.widget.Toast.LENGTH_LONG).show();
            }
        });
        TextView details = new TextView(this);
        details.setText(journal.previousReport());
        details.setTextIsSelectable(true);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(details);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(content);
    }

    private void addButton(LinearLayout parent, int label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
        button.setOnClickListener(view -> action.run());
        parent.addView(button, new LinearLayout.LayoutParams(-1, -2));
    }

    private void openMonitor(boolean htmlOnly) {
        journal.acknowledge();
        journal.beginStartup();
        // Avoid resolving MainActivity while verifying the recovery entry point.
        Intent launch = new Intent().setClassName(this, "br.com.polegar.ndimonitor.MainActivity");
        if (htmlOnly) launch.putExtra(EXTRA_HTML_ONLY, true);
        launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(launch);
        finish();
    }
}
