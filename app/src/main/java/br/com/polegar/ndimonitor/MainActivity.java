package br.com.polegar.ndimonitor;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.provider.Settings;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    public static final String PREFERENCES = "ndi_monitor_preferences";
    public static final String KEY_LAST_SOURCE = "last_source";
    public static final String KEY_START_ON_BOOT = "start_on_boot";
    public static final String KEY_DISPLAY_MODE = "display_mode";
    public static final String KEY_HTML_URL = "html_url";

    // Shared queues preserve connect/cleanup ordering across Activity recreation.
    private static final ExecutorService CAPTURE_WORKER = Executors.newSingleThreadExecutor();
    private static final ExecutorService DISCOVERY_WORKER = Executors.newSingleThreadExecutor();
    private final SourceCatalog sourceCatalog = new SourceCatalog();
    private SharedPreferences preferences;
    private SurfaceView videoSurface;
    private FrameLayout videoContainer;
    private LinearLayout sourcePanel;
    private ListView sourceList;
    private TextView statusText;
    private TextView sourceStatusText;
    private View scanButton;
    private ArrayAdapter<String> sourceAdapter;
    private List<String> displayedSources = Collections.emptyList();
    private AlertDialog settingsDialog;
    private AlertDialog noticesDialog;
    private AlertDialog modeDialog;
    private AlertDialog htmlAddressDialog;
    private HtmlMonitor htmlMonitor;
    private LanHtmlDiscovery htmlDiscovery;
    private final HtmlSourceCatalog htmlSourceCatalog = new HtmlSourceCatalog();
    private List<HtmlSource> displayedHtmlSources = Collections.emptyList();
    private Runnable nextHtmlScan;
    private int htmlScanGeneration;
    private int htmlScanProgress;
    private int htmlScanFound;
    private boolean htmlScanLimited;
    private EditText htmlUrlEditor;
    private volatile boolean htmlMode;
    private boolean modeInitialized;
    private boolean ndiAvailable;
    private boolean discoveryRequested;
    private boolean ndiDiscoveryActive;
    private volatile String currentNdiSource = "";
    private volatile int playbackGeneration;
    private boolean ndiPlaybackRequested;
    private volatile boolean activityVisible;
    private volatile boolean destroyed;
    private volatile int discoveryGeneration;
    private long discoveryStartedAt;
    private Runnable nextDiscoveryPoll;
    private int videoWidth;
    private int videoHeight;
    private WifiManager.MulticastLock multicastLock;
    private AudioTrack audioTrack;
    private int audioSampleRate;
    private int audioChannels;
    private View menuButton;
    private int safeInsetLeft;
    private int safeInsetRight;
    private OnBackInvokedCallback backCallback;
    private final Handler controlsHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideControls = () -> {
        if (sourcePanel.getVisibility() != View.VISIBLE) {
            menuButton.setVisibility(View.GONE);
            statusText.setVisibility(View.GONE);
            enterImmersiveMode();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        enterImmersiveMode();
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        videoContainer = findViewById(R.id.video_container);
        videoSurface = findViewById(R.id.video_surface);
        sourcePanel = findViewById(R.id.source_panel);
        sourceList = findViewById(R.id.source_list);
        statusText = findViewById(R.id.status_text);
        sourceStatusText = findViewById(R.id.source_status_text);
        scanButton = findViewById(R.id.scan_button);
        htmlDiscovery = new LanHtmlDiscovery(this);
        htmlSourceCatalog.remember(preferences.getString(KEY_HTML_URL, ""), SystemClock.elapsedRealtime());
        htmlMonitor = new HtmlMonitor(this, findViewById(R.id.html_container), new HtmlMonitor.Listener() {
            @Override public void onStatus(int message) {
                if (!destroyed && htmlMode) {
                    statusText.setText(message);
                }
            }

            @Override public void onInteraction() { showControls(); }
        });
        sourceAdapter = new ArrayAdapter<>(this, R.layout.source_item, new ArrayList<>());
        sourceList.setAdapter(sourceAdapter);
        sourceList.setOnItemClickListener((parent, view, position, id) -> {
            if (htmlMode) {
                if (position < displayedHtmlSources.size()) selectHtmlSource(displayedHtmlSources.get(position).url);
            } else {
                String source = sourceAdapter.getItem(position);
                if (source != null) connectToSource(source);
            }
        });

        View root = findViewById(R.id.root);
        root.addOnLayoutChangeListener((view, left, top, right, bottom,
                                       oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                resizeSourcePanel(right - left);
                fitVideoSurface(videoWidth, videoHeight);
            }
        });
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            applyControlInsets(insets);
            resizeSourcePanel(view.getWidth());
            return insets;
        });

        videoSurface.getHolder().addCallback(this);
        menuButton = findViewById(R.id.menu_button);
        videoSurface.setOnClickListener(v -> showControls());
        videoContainer.setOnClickListener(v -> showControls());
        menuButton.setOnClickListener(v -> toggleSourcePanel());
        findViewById(R.id.close_panel_button).setOnClickListener(v -> closeSourcePanel());
        scanButton.setOnClickListener(v -> {
            discoveryRequested = true;
            scanSources();
        });
        findViewById(R.id.source_mode_button).setOnClickListener(v -> showModeChooser());
        findViewById(R.id.manual_html_button).setOnClickListener(v -> showHtmlAddress());
        findViewById(R.id.reload_html_button).setOnClickListener(v -> htmlMonitor.reload());
        findViewById(R.id.monitor_settings_button).setOnClickListener(v -> showMonitorSettings());
        menuButton.setVisibility(View.GONE);
        statusText.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= 33) {
            backCallback = this::toggleSourcePanel;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        }
        root.requestApplyInsets();

        WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            multicastLock = wifiManager.createMulticastLock("NDIMonitorDiscovery");
            multicastLock.setReferenceCounted(false);
        }

        NativeBridge.setCallbackTarget(this);
        ndiAvailable = NativeBridge.isNdiAvailable();
        boolean savedHtml = "html".equals(preferences.getString(KEY_DISPLAY_MODE, "ndi"));
        boolean validHtml = HtmlSourceUrl.isWebAddress(preferences.getString(KEY_HTML_URL, ""));
        String savedNdi = preferences.getString(KEY_LAST_SOURCE, "");
        boolean validNdi = savedNdi != null && !savedNdi.trim().isEmpty();
        // Older versions saved a browsed mode before a source was selected.
        if (savedHtml && !validHtml && validNdi) savedHtml = false;
        else if (!savedHtml && !validNdi && validHtml) savedHtml = true;
        setSourceMode(savedHtml, true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        activityVisible = true;
        updateMulticastLock();
        htmlMonitor.resume();
        if (!htmlMode && ndiAvailable && !currentNdiSource.isEmpty()) startNdiPlayback();
        scanSources();
    }

    @Override
    protected void onStop() {
        activityVisible = false;
        htmlMonitor.pause();
        stopNdiPlayback();
        releaseAudio();
        stopDiscoveryPolling();
        if (multicastLock != null && multicastLock.isHeld()) multicastLock.release();
        super.onStop();
    }

    private void updateMulticastLock() {
        if (multicastLock == null) return;
        if (ndiAvailable && activityVisible && !htmlMode &&
                (ndiDiscoveryActive || !currentNdiSource.isEmpty())) {
            if (!multicastLock.isHeld()) multicastLock.acquire();
        } else if (multicastLock.isHeld()) {
            multicastLock.release();
        }
    }

    private void showModeChooser() {
        if (modeDialog != null && modeDialog.isShowing()) return;
        stopDiscoveryPolling();
        modeDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.choose_mode)
                .setSingleChoiceItems(R.array.monitor_modes, htmlMode ? 1 : 0, (dialog, which) -> {
                    setSourceMode(which == 1, false);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        modeDialog.setOnDismissListener(dialog -> {
            modeDialog = null;
            if (!destroyed) scanSources();
        });
        modeDialog.show();
    }

    private void setSourceMode(boolean html, boolean restoring) {
        if (modeInitialized && htmlMode == html) return;
        modeInitialized = true;
        htmlMode = html;
        discoveryRequested = false;
        ((TextView) findViewById(R.id.source_mode_button)).setText(html ? R.string.mode_html : R.string.mode_ndi);
        sourceList.setVisibility(View.VISIBLE);
        findViewById(R.id.html_actions).setVisibility(html ? View.VISIBLE : View.GONE);
        sourceStatusText.setMaxLines(html ? 1 : 2);
        scanButton.setContentDescription(getString(html ? R.string.search_html : R.string.refresh_sources));
        scanButton.setEnabled(html || ndiAvailable);
        stopDiscoveryPolling();
        updateMulticastLock();
        if (html) {
            stopNdiPlayback();
            releaseAudio();
            currentNdiSource = "";
            videoSurface.setVisibility(View.GONE);
            showHtmlSources();
            String address = preferences.getString(KEY_HTML_URL, "");
            if (!HtmlSourceUrl.isWebAddress(address)) address = "";
            if (!address.isEmpty()) {
                if (restoring) {
                    htmlMonitor.show(address);
                    closeSourcePanel();
                } else selectHtmlSource(address);
            } else {
                htmlMonitor.show("");
                sourcePanel.setVisibility(View.VISIBLE);
                discoveryRequested = true;
                sourceStatusText.setText(R.string.searching_html);
                scanSources();
            }
        } else {
            displayedHtmlSources = Collections.emptyList();
            sourceAdapter.clear();
            displayedSources = Collections.emptyList();
            htmlMonitor.hide();
            videoSurface.setVisibility(View.VISIBLE);
            String lastSource = preferences.getString(KEY_LAST_SOURCE, "");
            currentNdiSource = lastSource == null || lastSource.trim().isEmpty() ? "" : lastSource;
            if (!ndiAvailable) {
                sourceStatusText.setText(R.string.ndi_sdk_missing);
                sourcePanel.setVisibility(View.VISIBLE);
                return;
            }
            if (!currentNdiSource.isEmpty()) {
                sourceCatalog.update(new String[]{currentNdiSource}, SystemClock.elapsedRealtime());
                showSources(Collections.singletonList(currentNdiSource));
                sourceStatusText.setText(R.string.discovery_idle);
                if (restoring) {
                    statusText.setText(getString(R.string.connecting_to, currentNdiSource));
                    closeSourcePanel();
                } else connectToSource(currentNdiSource);
            } else {
                sourcePanel.setVisibility(View.VISIBLE);
                discoveryRequested = true;
                sourceStatusText.setText(R.string.searching_sources);
                scanSources();
            }
        }
        int nextFocus = R.id.source_list;
        for (int id : new int[]{R.id.source_mode_button, R.id.scan_button,
                R.id.monitor_settings_button, R.id.close_panel_button}) {
            findViewById(id).setNextFocusDownId(nextFocus);
        }
    }

    private void openHtmlSource() {
        if (!htmlMode) return;
        String address;
        try {
            address = HtmlSourceUrl.normalize(htmlUrlEditor.getText().toString());
        } catch (IllegalArgumentException invalid) {
            htmlUrlEditor.setError(getString(R.string.html_invalid_address));
            htmlUrlEditor.requestFocus();
            return;
        }
        htmlUrlEditor.setError(null);
        htmlUrlEditor.setText(address);
        InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(htmlUrlEditor.getWindowToken(), 0);
        selectHtmlSource(address);
        if (htmlAddressDialog != null) htmlAddressDialog.dismiss();
    }

    private void selectHtmlSource(String address) {
        if (!htmlMode || !HtmlSourceUrl.isWebAddress(address)) return;
        preferences.edit().putString(KEY_DISPLAY_MODE, "html").putString(KEY_HTML_URL, address).apply();
        htmlSourceCatalog.remember(address, SystemClock.elapsedRealtime());
        htmlMonitor.show(address);
        closeSourcePanel();
        showControls();
    }

    private void showHtmlAddress() {
        if (!htmlMode || (htmlAddressDialog != null && htmlAddressDialog.isShowing())) return;
        stopDiscoveryPolling();
        View form = getLayoutInflater().inflate(R.layout.dialog_html_address, null);
        htmlUrlEditor = form.findViewById(R.id.html_url);
        htmlUrlEditor.setText(preferences.getString(KEY_HTML_URL, ""));
        form.findViewById(R.id.open_html_button).setOnClickListener(v -> openHtmlSource());
        htmlUrlEditor.setOnEditorActionListener((view, action, event) -> {
            if (action != EditorInfo.IME_ACTION_GO) return false;
            openHtmlSource();
            return true;
        });
        htmlAddressDialog = new AlertDialog.Builder(this).setTitle(R.string.html_address)
                .setView(form).setNegativeButton(android.R.string.cancel, null).create();
        htmlAddressDialog.setOnDismissListener(dialog -> {
            htmlAddressDialog = null;
            if (!destroyed) scanSources();
        });
        htmlAddressDialog.show();
    }

    private void scanHtmlSources() {
        stopDiscoveryPolling();
        int generation = htmlScanGeneration;
        htmlScanProgress = 0;
        htmlScanFound = 0;
        showHtmlSources();
        sourceStatusText.setText(R.string.searching_html);
        htmlDiscovery.start(preferences.getString(KEY_HTML_URL, ""), new LanHtmlDiscovery.Listener() {
            private boolean current() {
                return htmlMode && !destroyed && activityVisible && generation == htmlScanGeneration;
            }
            @Override public void onStarted(int total, boolean limited) {
                if (!current()) return;
                htmlScanLimited = limited;
                sourceStatusText.setText(getString(R.string.html_scan_progress, 0, total));
            }
            @Override public void onSource(HtmlSource source) {
                if (!current()) return;
                ++htmlScanFound;
                htmlSourceCatalog.found(source, SystemClock.elapsedRealtime());
                showHtmlSources();
            }
            @Override public void onProgress(int done, int total) {
                if (!current()) return;
                htmlScanProgress = Math.max(htmlScanProgress, done);
                sourceStatusText.setText(getString(R.string.html_scan_progress, htmlScanProgress, total));
            }
            @Override public void onFinished() {
                if (!current()) return;
                htmlSourceCatalog.finishScan(SystemClock.elapsedRealtime());
                showHtmlSources();
                sourceStatusText.setText(htmlScanFound == 0 ? R.string.html_none_found :
                        htmlScanLimited ? R.string.html_found_limited : R.string.html_found);
                scheduleNextHtmlScan(generation);
            }
            @Override public void onUnavailable() {
                if (!current()) return;
                sourceStatusText.setText(R.string.html_no_lan);
                scheduleNextHtmlScan(generation);
            }
        });
    }

    private void scheduleNextHtmlScan(int generation) {
        nextHtmlScan = () -> {
            if (generation == htmlScanGeneration) scanSources();
        };
        controlsHandler.postDelayed(nextHtmlScan, 30_000);
    }

    private void showHtmlSources() {
        List<HtmlSource> next = htmlSourceCatalog.snapshot();
        List<String> labels = new ArrayList<>();
        for (HtmlSource source : next) labels.add(source.displayName());
        if (displayedSources.equals(labels)) { displayedHtmlSources = next; return; }
        int first = sourceList.getFirstVisiblePosition();
        View child = sourceList.getChildAt(0);
        int top = child == null ? 0 : child.getTop();
        int selection = sourceList.getSelectedItemPosition();
        String selectedUrl = selection >= 0 && selection < displayedHtmlSources.size()
                ? displayedHtmlSources.get(selection).url : null;
        sourceAdapter.setNotifyOnChange(false);
        sourceAdapter.clear();
        sourceAdapter.addAll(labels);
        sourceAdapter.notifyDataSetChanged();
        displayedSources = labels;
        displayedHtmlSources = next;
        int nextSelection = -1;
        for (int i = 0; i < next.size(); ++i) {
            if (next.get(i).url.equals(selectedUrl)) { nextSelection = i; break; }
        }
        if (nextSelection >= 0) sourceList.setSelection(nextSelection);
        else if (!labels.isEmpty()) sourceList.setSelectionFromTop(Math.min(first, labels.size() - 1), top);
    }

    private void resizeSourcePanel(int availableWidth) {
        availableWidth -= safeInsetLeft + safeInsetRight;
        if (availableWidth <= 0) return;
        int maximumWidth = getResources().getDimensionPixelSize(R.dimen.source_panel_width);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) sourcePanel.getLayoutParams();
        int width = Math.min(availableWidth, maximumWidth);
        if (params.width != width) {
            params.width = width;
            sourcePanel.setLayoutParams(params);
        }
    }

    private void applyControlInsets(WindowInsets insets) {
        int left, top, right, bottom;
        if (Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets safe = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            left = safe.left;
            top = safe.top;
            right = safe.right;
            bottom = safe.bottom;
        } else {
            left = insets.getSystemWindowInsetLeft();
            top = insets.getSystemWindowInsetTop();
            right = insets.getSystemWindowInsetRight();
            bottom = insets.getSystemWindowInsetBottom();
            if (Build.VERSION.SDK_INT >= 28 && insets.getDisplayCutout() != null) {
                left = Math.max(left, insets.getDisplayCutout().getSafeInsetLeft());
                top = Math.max(top, insets.getDisplayCutout().getSafeInsetTop());
                right = Math.max(right, insets.getDisplayCutout().getSafeInsetRight());
                bottom = Math.max(bottom, insets.getDisplayCutout().getSafeInsetBottom());
            }
        }
        safeInsetLeft = left;
        safeInsetRight = right;
        FrameLayout.LayoutParams panel = (FrameLayout.LayoutParams) sourcePanel.getLayoutParams();
        panel.setMargins(left, top, right, bottom);
        sourcePanel.setLayoutParams(panel);
        int margin = getResources().getDimensionPixelSize(R.dimen.overlay_margin);
        FrameLayout.LayoutParams menu = (FrameLayout.LayoutParams) menuButton.getLayoutParams();
        menu.setMargins(margin + left, margin + top, margin + right, margin + bottom);
        menuButton.setLayoutParams(menu);
        FrameLayout.LayoutParams status = (FrameLayout.LayoutParams) statusText.getLayoutParams();
        status.setMargins(margin + left, margin + top, margin + right, margin + bottom);
        statusText.setLayoutParams(status);
    }

    private void showMonitorSettings() {
        if (settingsDialog != null && settingsDialog.isShowing()) return;
        stopDiscoveryPolling();
        View content = getLayoutInflater().inflate(R.layout.dialog_monitor_settings, null);
        CheckBox bootCheckBox = content.findViewById(R.id.boot_checkbox);
        bootCheckBox.setChecked(preferences.getBoolean(KEY_START_ON_BOOT, true));
        bootCheckBox.setOnCheckedChangeListener((button, checked) ->
                preferences.edit().putBoolean(KEY_START_ON_BOOT, checked).apply());
        settingsDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.monitor_settings)
                .setView(content)
                .setPositiveButton(R.string.close, null)
                .create();
        content.findViewById(R.id.home_settings_button).setOnClickListener(v -> {
            settingsDialog.dismiss();
            openAndroidSettings(Settings.ACTION_HOME_SETTINGS);
        });
        content.findViewById(R.id.android_settings_button).setOnClickListener(v -> {
            settingsDialog.dismiss();
            openAndroidSettings(Settings.ACTION_SETTINGS);
        });
        content.findViewById(R.id.third_party_notices_button).setOnClickListener(v ->
                showThirdPartyNotices());
        settingsDialog.setOnDismissListener(dialog -> {
            settingsDialog = null;
            if (!destroyed) scanSources();
        });
        settingsDialog.show();
    }

    private void showThirdPartyNotices() {
        if (noticesDialog != null && noticesDialog.isShowing()) return;
        String notices;
        try (InputStream input = getAssets().open("libndi_licenses.txt");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            notices = new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException unavailable) {
            notices = getString(R.string.third_party_notices_unavailable);
        }
        noticesDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.third_party_notices)
                .setMessage(notices)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        noticesDialog.setOnDismissListener(dialog -> noticesDialog = null);
        noticesDialog.show();
    }

    private void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void openAndroidSettings(String action) {
        try {
            startActivity(new Intent(action));
        } catch (ActivityNotFoundException unavailable) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (ActivityNotFoundException ignored) {
                android.widget.Toast.makeText(this,
                        "Configurações indisponíveis neste aparelho", android.widget.Toast.LENGTH_LONG).show();
            }
        }
    }

    private void toggleSourcePanel() {
        if (sourcePanel.getVisibility() == View.VISIBLE) {
            closeSourcePanel();
        } else {
            controlsHandler.removeCallbacks(hideControls);
            sourcePanel.setVisibility(View.VISIBLE);
            menuButton.setVisibility(View.GONE);
            statusText.setVisibility(View.GONE);
            if (!hasSavedSource()) discoveryRequested = true;
            if (!discoveryRequested && (htmlMode || ndiAvailable)) {
                sourceStatusText.setText(R.string.discovery_idle);
            }
            scanSources();
            if (!displayedSources.isEmpty()) sourceList.requestFocus();
            else if (htmlMode) findViewById(R.id.manual_html_button).requestFocus();
        }
    }

    private void closeSourcePanel() {
        discoveryRequested = false;
        stopDiscoveryPolling();
        sourcePanel.setVisibility(View.GONE);
        hideControls.run();
    }

    private void showControls() {
        if (sourcePanel.getVisibility() == View.VISIBLE) return;
        menuButton.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        controlsHandler.removeCallbacks(hideControls);
        controlsHandler.postDelayed(hideControls, 4000);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_DOWN && sourcePanel.getVisibility() != View.VISIBLE &&
                (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER ||
                 keyCode == KeyEvent.KEYCODE_MENU)) {
            if (event.getRepeatCount() > 0) return true;
            if (menuButton.getVisibility() == View.VISIBLE) {
                toggleSourcePanel();
            } else {
                showControls();
                menuButton.requestFocus();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    // Legacy fallback. API 33+ registers OnBackInvokedDispatcher in onCreate.
    @SuppressLint("GestureBackNavigation")
    public void onBackPressed() {
        toggleSourcePanel();
    }

    private void scanSources() {
        if (!discoveryRequested || (!htmlMode && !ndiAvailable) || !activityVisible || destroyed ||
                sourcePanel.getVisibility() != View.VISIBLE ||
                (modeDialog != null && modeDialog.isShowing()) ||
                (htmlAddressDialog != null && htmlAddressDialog.isShowing()) ||
                (settingsDialog != null && settingsDialog.isShowing())) return;
        if (htmlMode) { scanHtmlSources(); return; }
        stopDiscoveryPolling();
        discoveryStartedAt = SystemClock.elapsedRealtime();
        ndiDiscoveryActive = true;
        updateMulticastLock();
        sourceCatalog.beginRefresh(discoveryStartedAt);
        if (displayedSources.isEmpty()) sourceStatusText.setText(R.string.searching_sources);
        // Show sources already known to the live finder before waiting for updates.
        pollDiscovery(discoveryGeneration, 0);
    }

    private void stopDiscoveryPolling() {
        ++discoveryGeneration;
        ++htmlScanGeneration;
        if (htmlDiscovery != null) htmlDiscovery.stop();
        if (nextHtmlScan != null) {
            controlsHandler.removeCallbacks(nextHtmlScan);
            nextHtmlScan = null;
        }
        if (nextDiscoveryPoll != null) {
            controlsHandler.removeCallbacks(nextDiscoveryPoll);
            nextDiscoveryPoll = null;
        }
        if (ndiDiscoveryActive) {
            ndiDiscoveryActive = false;
            DISCOVERY_WORKER.execute(NativeBridge::releaseDiscovery);
        }
        updateMulticastLock();
    }

    private void pollDiscovery(int generation, int timeoutMs) {
        if (destroyed || !activityVisible || !discoveryRequested || !ndiDiscoveryActive ||
                htmlMode || generation != discoveryGeneration) return;
        DISCOVERY_WORKER.execute(() -> {
            if (destroyed || generation != discoveryGeneration) return;
            String[] result = NativeBridge.findSources(timeoutMs);
            controlsHandler.post(() -> {
                if (destroyed || !activityVisible || generation != discoveryGeneration ||
                        sourcePanel.getVisibility() != View.VISIBLE) return;
                showSources(sourceCatalog.update(result, SystemClock.elapsedRealtime()));
                nextDiscoveryPoll = () -> pollDiscovery(generation, 500);
                controlsHandler.postDelayed(nextDiscoveryPoll, 50);
            });
        });
    }

    private void showSources(List<String> sources) {
        if (!displayedSources.equals(sources)) {
            String selected = (String) sourceList.getSelectedItem();
            int first = sourceList.getFirstVisiblePosition();
            View firstChild = sourceList.getChildAt(0);
            int top = firstChild == null ? 0 : firstChild.getTop();
            sourceAdapter.setNotifyOnChange(false);
            sourceAdapter.clear();
            sourceAdapter.addAll(sources);
            sourceAdapter.notifyDataSetChanged();
            displayedSources = new ArrayList<>(sources);
            int selection = selected == null ? -1 : sourceAdapter.getPosition(selected);
            if (selection >= 0) {
                sourceList.setSelection(selection);
            } else if (!sources.isEmpty()) {
                sourceList.setSelectionFromTop(Math.min(first, sources.size() - 1), top);
            }
        }
        String message = sources.isEmpty()
                ? getString(SystemClock.elapsedRealtime() - discoveryStartedAt < 5000
                        ? R.string.searching_sources : R.string.no_sources)
                : getResources().getQuantityString(R.plurals.sources_found, sources.size(), sources.size());
        if (!TextUtils.equals(sourceStatusText.getText(), message)) sourceStatusText.setText(message);
    }

    private void connectToSource(String sourceName) {
        if (htmlMode || sourceName == null || sourceName.trim().isEmpty()) return;
        preferences.edit().putString(KEY_DISPLAY_MODE, "ndi").putString(KEY_LAST_SOURCE, sourceName).apply();
        currentNdiSource = sourceName;
        statusText.setText(getString(R.string.connecting_to, sourceName));
        closeSourcePanel();
        showControls();
        if (activityVisible) startNdiPlayback();
    }

    private boolean hasSavedSource() {
        return htmlMode ? HtmlSourceUrl.isWebAddress(preferences.getString(KEY_HTML_URL, ""))
                : !currentNdiSource.isEmpty();
    }

    private void startNdiPlayback() {
        String source = currentNdiSource;
        int generation = ++playbackGeneration;
        ndiPlaybackRequested = true;
        CAPTURE_WORKER.execute(() -> {
            if (!destroyed && activityVisible && !htmlMode && generation == playbackGeneration) {
                NativeBridge.connect(source);
            }
        });
    }

    private void stopNdiPlayback() {
        ++playbackGeneration;
        if (ndiPlaybackRequested) {
            ndiPlaybackRequested = false;
            CAPTURE_WORKER.execute(NativeBridge::disconnect);
        }
    }

    @SuppressWarnings("unused")
    public void onNativeStatus(String message) {
        runOnUiThread(() -> {
            if (!destroyed && activityVisible && !htmlMode) statusText.setText(message);
        });
    }

    @SuppressWarnings("unused")
    public void onNativeVideoFormat(int width, int height, double fps) {
        runOnUiThread(() -> {
            if (destroyed || !activityVisible || htmlMode) return;
            videoWidth = width;
            videoHeight = height;
            fitVideoSurface(width, height);
            statusText.setText(String.format(Locale.getDefault(), "%d x %d  •  %.2f fps", width, height, fps));
        });
    }

    private void fitVideoSurface(int width, int height) {
        if (width <= 0 || height <= 0 || videoContainer.getWidth() == 0) return;
        int availableWidth = videoContainer.getWidth();
        int availableHeight = videoContainer.getHeight();
        float videoRatio = (float) width / height;
        float containerRatio = (float) availableWidth / availableHeight;
        int targetWidth;
        int targetHeight;
        if (videoRatio > containerRatio) {
            targetWidth = availableWidth;
            targetHeight = Math.round(availableWidth / videoRatio);
        } else {
            targetHeight = availableHeight;
            targetWidth = Math.round(availableHeight * videoRatio);
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(targetWidth, targetHeight);
        params.gravity = android.view.Gravity.CENTER;
        videoSurface.setLayoutParams(params);
    }

    @SuppressWarnings("unused")
    public synchronized void onNativeAudio(short[] pcm, int sampleRate, int channels) {
        if (destroyed || htmlMode || !activityVisible || pcm == null || pcm.length == 0) return;
        if (audioTrack == null || audioSampleRate != sampleRate || audioChannels != channels) {
            configureAudio(sampleRate, channels);
        }
        if (audioTrack != null) {
            audioTrack.write(pcm, 0, pcm.length, AudioTrack.WRITE_NON_BLOCKING);
        }
    }

    private synchronized void configureAudio(int sampleRate, int channels) {
        releaseAudio();
        int channelMask = channels <= 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        int minimum = AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT);
        if (minimum <= 0) return;
        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelMask)
                        .build())
                .setBufferSizeInBytes(minimum * 3)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
        audioTrack.play();
        audioSampleRate = sampleRate;
        audioChannels = channels;
    }

    private synchronized void releaseAudio() {
        if (audioTrack != null) {
            try {
                audioTrack.pause();
                audioTrack.flush();
                audioTrack.release();
            } catch (IllegalStateException ignored) {
            }
            audioTrack = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        NativeBridge.setSurface(holder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        NativeBridge.setSurface(holder.getSurface());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        NativeBridge.setSurface(null);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersiveMode();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        stopNdiPlayback();
        htmlMonitor.destroy();
        stopDiscoveryPolling();
        controlsHandler.removeCallbacksAndMessages(null);
        if (settingsDialog != null) settingsDialog.dismiss();
        if (noticesDialog != null) noticesDialog.dismiss();
        if (modeDialog != null) modeDialog.dismiss();
        if (htmlAddressDialog != null) htmlAddressDialog.dismiss();
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        }
        NativeBridge.setCallbackTarget(null);
        releaseAudio();
        if (multicastLock != null && multicastLock.isHeld()) multicastLock.release();
        super.onDestroy();
    }
}
