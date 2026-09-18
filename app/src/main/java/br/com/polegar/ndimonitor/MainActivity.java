package br.com.polegar.ndimonitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.provider.Settings;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    public static final String PREFERENCES = "ndi_monitor_preferences";
    public static final String KEY_LAST_SOURCE = "last_source";
    public static final String KEY_START_ON_BOOT = "start_on_boot";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private SharedPreferences preferences;
    private SurfaceView videoSurface;
    private FrameLayout videoContainer;
    private LinearLayout sourcePanel;
    private ListView sourceList;
    private TextView statusText;
    private Button scanButton;
    private CheckBox bootCheckBox;
    private WifiManager.MulticastLock multicastLock;
    private AudioTrack audioTrack;
    private int audioSampleRate;
    private int audioChannels;
    private View menuButton;
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
        scanButton = findViewById(R.id.scan_button);
        bootCheckBox = findViewById(R.id.boot_checkbox);

        videoSurface.getHolder().addCallback(this);
        menuButton = findViewById(R.id.menu_button);
        videoSurface.setOnClickListener(v -> showControls());
        videoContainer.setOnClickListener(v -> showControls());
        menuButton.setOnClickListener(v -> toggleSourcePanel());
        findViewById(R.id.close_panel_button).setOnClickListener(v -> closeSourcePanel());
        scanButton.setOnClickListener(v -> scanSources());
        findViewById(R.id.home_settings_button).setOnClickListener(v -> openAndroidSettings(Settings.ACTION_HOME_SETTINGS));
        findViewById(R.id.android_settings_button).setOnClickListener(v -> openAndroidSettings(Settings.ACTION_SETTINGS));

        boolean startOnBoot = preferences.getBoolean(KEY_START_ON_BOOT, true);
        bootCheckBox.setChecked(startOnBoot);
        bootCheckBox.setOnCheckedChangeListener((button, checked) ->
                preferences.edit().putBoolean(KEY_START_ON_BOOT, checked).apply());

        WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            multicastLock = wifiManager.createMulticastLock("NDIMonitorDiscovery");
            multicastLock.setReferenceCounted(false);
            multicastLock.acquire();
        }

        NativeBridge.setCallbackTarget(this);
        if (!NativeBridge.isNdiAvailable()) {
            statusText.setText(R.string.ndi_sdk_missing);
            sourcePanel.setVisibility(View.VISIBLE);
            scanButton.setEnabled(false);
        } else {
            String lastSource = preferences.getString(KEY_LAST_SOURCE, "");
            if (lastSource != null && !lastSource.isEmpty()) {
                connectToSource(lastSource);
            }
            scanSources();
        }
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
            menuButton.setVisibility(View.VISIBLE);
            statusText.setVisibility(View.VISIBLE);
        }
    }

    private void closeSourcePanel() {
        sourcePanel.setVisibility(View.GONE);
        hideControls.run();
    }

    private void showControls() {
        menuButton.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        controlsHandler.removeCallbacks(hideControls);
        controlsHandler.postDelayed(hideControls, 4000);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (sourcePanel.getVisibility() != View.VISIBLE &&
                (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER ||
                 keyCode == KeyEvent.KEYCODE_MENU)) {
            if (menuButton.getVisibility() == View.VISIBLE) {
                toggleSourcePanel();
            } else {
                showControls();
                menuButton.requestFocus();
            }
            return true;
        }
        if (sourcePanel.getVisibility() == View.VISIBLE && keyCode == KeyEvent.KEYCODE_BACK) {
            closeSourcePanel();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            toggleSourcePanel();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void scanSources() {
        scanButton.setEnabled(false);
        scanButton.setText(R.string.scanning);
        statusText.setText(R.string.searching_sources);
        worker.execute(() -> {
            String[] result = NativeBridge.findSources(3500);
            List<String> sources = result == null ? Collections.emptyList() : Arrays.asList(result);
            runOnUiThread(() -> showSources(sources));
        });
    }

    private void showSources(List<String> sources) {
        scanButton.setEnabled(true);
        scanButton.setText(R.string.scan_again);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.source_item, sources);
        sourceList.setAdapter(adapter);
        sourceList.setOnItemClickListener((parent, view, position, id) ->
                connectToSource(sources.get(position)));
        if (sources.isEmpty()) {
            statusText.setText(R.string.no_sources);
        } else {
            statusText.setText(getResources().getQuantityString(
                    R.plurals.sources_found, sources.size(), sources.size()));
        }
    }

    private void connectToSource(String sourceName) {
        preferences.edit().putString(KEY_LAST_SOURCE, sourceName).apply();
        statusText.setText(getString(R.string.connecting_to, sourceName));
        closeSourcePanel();
        worker.execute(() -> NativeBridge.connect(sourceName));
    }

    @SuppressWarnings("unused")
    public void onNativeStatus(String message) {
        runOnUiThread(() -> statusText.setText(message));
    }

    @SuppressWarnings("unused")
    public void onNativeVideoFormat(int width, int height, double fps) {
        runOnUiThread(() -> {
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
    public void onNativeAudio(short[] pcm, int sampleRate, int channels) {
        if (pcm == null || pcm.length == 0) return;
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
        controlsHandler.removeCallbacksAndMessages(null);
        NativeBridge.disconnect();
        NativeBridge.setCallbackTarget(null);
        worker.shutdownNow();
        releaseAudio();
        if (multicastLock != null && multicastLock.isHeld()) multicastLock.release();
        super.onDestroy();
    }
}
