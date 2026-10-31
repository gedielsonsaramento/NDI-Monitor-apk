package br.com.polegar.ndimonitor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextThemeWrapper;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ConsoleMessage;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Owns the web renderer; no native bridge is exposed to the loaded page. */
final class HtmlMonitor {
    interface Listener {
        void onStatus(int message);
        void onInteraction();
    }

    private final Activity activity;
    private final FrameLayout container;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WebView webView;
    private HtmlStageViewport viewport;
    private boolean stageLayout = true;
    private boolean compatibilityMode;
    private String lastConsoleError = "";
    private HtmlLegacyCompat legacyCompat;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private TextView failureView;
    private boolean enabled;
    private boolean paused = true;
    private boolean failed;
    private boolean loading;
    private String address = "";
    private int retries;
    private final Runnable loadTimeout = () -> {
        if (enabled && !paused && webView != null) {
            webView.stopLoading();
            fail(true);
        }
    };
    private final Runnable retry = () -> {
        if (enabled && !paused && !address.isEmpty()) loadCurrentAddress();
    };

    HtmlMonitor(Activity activity, FrameLayout container, Listener listener) {
        this.activity = activity;
        this.container = container;
        this.listener = listener;
    }

    void show(String address) {
        enabled = true;
        container.setVisibility(View.VISIBLE);
        this.address = address == null ? "" : address;
        retries = 0;
        if (!paused && !this.address.isEmpty()) loadCurrentAddress();
    }

    String state() {
        if (!enabled || paused || address.isEmpty()) return "inactive";
        if (webView == null) return "unavailable";
        return failed ? "waiting_source" : loading ? "loading" : "loaded";
    }

    void clearResourceCache() {
        if (webView != null) webView.clearCache(true);
        reload();
    }

    void reload() {
        retries = 0;
        handler.removeCallbacks(retry);
        if (enabled && !paused && !address.isEmpty()) loadCurrentAddress();
    }

    void setStageLayout(boolean enabled) {
        setDisplayLayout(enabled, compatibilityMode);
    }

    void setDisplayLayout(boolean stage, boolean compatibility) {
        if (stageLayout == stage && compatibilityMode == compatibility) return;
        stageLayout = stage;
        compatibilityMode = compatibility;
        retries = 0;
        handler.removeCallbacks(loadTimeout);
        handler.removeCallbacks(retry);
        disposeRenderer();
        if (this.enabled && !paused && !address.isEmpty()) loadCurrentAddress();
    }

    void resume() {
        paused = false;
        if (enabled && !address.isEmpty()) loadCurrentAddress();
    }

    void pause() {
        paused = true;
        handler.removeCallbacks(retry);
        handler.removeCallbacks(loadTimeout);
        // Destroying the renderer also stops HTML audio when the app leaves the foreground.
        disposeRenderer();
    }

    void hide() {
        enabled = false;
        handler.removeCallbacks(retry);
        handler.removeCallbacks(loadTimeout);
        disposeRenderer();
        container.setVisibility(View.GONE);
    }

    void destroy() {
        hide();
        handler.removeCallbacksAndMessages(null);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private boolean ensureRenderer() {
        if (webView != null) return true;
        try {
            Context rendererContext = activity;
            if (stageLayout && !compatibilityMode) {
                // Scope a 1x density to this renderer, keeping the virtual canvas at 1080p
                // even on high-density phones. App controls keep the user's normal scale.
                ContextThemeWrapper desktop = new ContextThemeWrapper(activity, activity.getTheme());
                Configuration configuration = new Configuration();
                configuration.densityDpi = 160;
                configuration.fontScale = 1f;
                desktop.applyOverrideConfiguration(configuration);
                rendererContext = desktop;
            }
            webView = new WebView(rendererContext) {
                @Override public boolean dispatchTouchEvent(MotionEvent event) {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) listener.onInteraction();
                    return super.dispatchTouchEvent(event);
                }
            };
            webView.setBackgroundColor(Color.BLACK);
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setMediaPlaybackRequiresUserGesture(false);
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            settings.setTextZoom(100);
            settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
            settings.setSupportZoom(false);
            settings.setUseWideViewPort(!stageLayout);
            settings.setLoadWithOverviewMode(!stageLayout);
            if (stageLayout) {
                String agent = settings.getUserAgentString();
                if (agent != null) settings.setUserAgentString(agent
                        .replaceFirst("\\([^)]*\\)", "(X11; Linux x86_64)")
                        .replace(" Version/4.0", "").replace(" Mobile", ""));
                webView.setHorizontalScrollBarEnabled(false);
                webView.setVerticalScrollBarEnabled(false);
                webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            }
            webView.setWebChromeClient(new WebChromeClient() {
                @Override public boolean onConsoleMessage(ConsoleMessage message) {
                    if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                        String text = message.message();
                        String source = message.sourceId();
                        lastConsoleError = (text == null ? "" : text.substring(0, Math.min(240, text.length())))
                                + (source == null || source.isEmpty() ? "" : "\nArquivo: "
                                + source.substring(0, Math.min(512, source.length())) + ":" + message.lineNumber());
                    }
                    return true;
                }
                @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                    if (customView != null) {
                        callback.onCustomViewHidden();
                        return;
                    }
                    customView = view;
                    customViewCallback = callback;
                    container.addView(view, new FrameLayout.LayoutParams(-1, -1));
                }

                @Override public void onHideCustomView() { hideCustomView(); }
            });
            viewport = new HtmlStageViewport(activity, webView, stageLayout);
            container.addView(viewport, 0, new FrameLayout.LayoutParams(-1, -1));
            return true;
        } catch (RuntimeException unavailable) {
            disposeRenderer();
            failed = true;
            showFailure(R.string.html_renderer_unavailable);
            listener.onStatus(R.string.html_renderer_unavailable);
            return false;
        }
    }

    private class MonitorWebViewClient extends WebViewClient {
        private final HtmlLegacyCompat compatibility;
        MonitorWebViewClient(HtmlLegacyCompat compatibility) { this.compatibility = compatibility; }

        @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (request.isForMainFrame()) return null;
            return compatibility.intercept(request.getMethod(), request.getUrl().toString(), request.getRequestHeaders());
        }

        // API 23 calls the String overload; API 24+ calls the request overload.
        @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return !HtmlSourceUrl.isWebAddress(url);
        }
        @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return !HtmlSourceUrl.isWebAddress(request.getUrl().toString());
        }

        @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
            if (!isCurrent(view) || !HtmlSourceUrl.isWebAddress(url)) return;
            failed = false;
            loading = true;
            listener.onStatus(R.string.html_loading);
        }

        @Override public void onPageFinished(WebView view, String url) {
            if (!isCurrent(view) || failed || !HtmlSourceUrl.isWebAddress(url)) return;
            retries = 0;
            loading = false;
            handler.removeCallbacks(loadTimeout);
            hideFailure();
            listener.onStatus(R.string.html_open);
        }

        @Override public void onReceivedError(WebView view, WebResourceRequest request,
                                               WebResourceError error) {
            if (isCurrent(view) && request.isForMainFrame()) fail(true);
        }

        @Override public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                                   WebResourceResponse response) {
            if (isCurrent(view) && request.isForMainFrame()) {
                fail(response.getStatusCode() >= 500);
            }
        }

        @Override public void onReceivedSslError(WebView view, SslErrorHandler sslHandler,
                                                  SslError error) {
            sslHandler.cancel();
            if (isCurrent(view)) fail(false);
        }

    }

    @android.annotation.TargetApi(26)
    @android.annotation.SuppressLint("UseRequiresApi") // Only constructed by the checked API 26+ branch.
    private final class Api26WebViewClient extends MonitorWebViewClient {
        Api26WebViewClient(HtmlLegacyCompat compatibility) { super(compatibility); }
        @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            if (view == webView) {
                disposeRenderer();
                if (enabled && !paused) fail(true);
            }
            return true;
        }
    }

    private boolean isCurrent(WebView view) {
        return enabled && !paused && view == webView;
    }

    private void loadCurrentAddress() {
        handler.removeCallbacks(retry);
        if (!ensureRenderer()) return;
        if (legacyCompat != null) legacyCompat.close();
        legacyCompat = new HtmlLegacyCompat(activity.getAssets(), webView.getSettings().getUserAgentString(), address);
        webView.setWebViewClient(Build.VERSION.SDK_INT >= 26
                ? new Api26WebViewClient(legacyCompat) : new MonitorWebViewClient(legacyCompat));
        hideCustomView();
        failed = false;
        loading = true;
        lastConsoleError = "";
        hideFailure();
        listener.onStatus(R.string.html_loading);
        handler.removeCallbacks(loadTimeout);
        handler.postDelayed(loadTimeout, 15000);
        webView.loadUrl(address);
    }

    interface DiagnosticsCallback { void onResult(String description); }

    void describePage(DiagnosticsCallback callback) {
        WebView current = webView;
        if (current == null) {
            callback.onResult("HTML: nenhuma página ativa");
            return;
        }
        String description = "WebView: " + current.getSettings().getUserAgentString()
                + "\nÁrea do motor: " + current.getWidth() + " × " + current.getHeight()
                + " px; densidade " + current.getResources().getDisplayMetrics().density
                + "\nExibição: " + (stageLayout ? compatibilityMode ? "16:9 compatibilidade" : "16:9" : "página original")
                + (legacyCompat == null ? "" : "\n" + legacyCompat.describe())
                + (lastConsoleError.isEmpty() ? "" : "\nErro JavaScript: " + lastConsoleError);
        java.util.concurrent.atomic.AtomicBoolean finished = new java.util.concurrent.atomic.AtomicBoolean();
        Runnable timeout = () -> {
            if (finished.compareAndSet(false, true)) callback.onResult(description + "\nPágina: sem resposta ao diagnóstico");
        };
        // Manual diagnostics only: older renderers may respond slowly during startup.
        handler.postDelayed(timeout, 5000);
        try {
            current.evaluateJavascript(HtmlPageDiagnostics.SCRIPT, value -> {
                if (!finished.compareAndSet(false, true)) return;
                handler.removeCallbacks(timeout);
                callback.onResult(description + "\n" + (current == webView
                        ? HtmlPageDiagnostics.format(value) : "Página alterada durante a leitura"));
            });
        } catch (RuntimeException unavailable) {
            if (finished.compareAndSet(false, true)) {
                handler.removeCallbacks(timeout);
                callback.onResult(description + "\nPágina: diagnóstico indisponível");
            }
        }
    }

    private void fail(boolean retryable) {
        failed = true;
        handler.removeCallbacks(loadTimeout);
        handler.removeCallbacks(retry);
        showFailure(retryable ? R.string.html_retrying : R.string.html_load_failed);
        listener.onStatus(retryable ? R.string.html_retrying : R.string.html_load_failed);
        if (retryable && enabled && !paused) {
            long delay = Math.min(30000L, 3000L << Math.min(retries++, 4));
            handler.postDelayed(retry, delay);
        }
    }

    private void showFailure(int message) {
        if (failureView == null) {
            failureView = new TextView(activity);
            failureView.setTextColor(Color.WHITE);
            failureView.setBackgroundColor(Color.BLACK);
            failureView.setGravity(Gravity.CENTER);
            int padding = Math.round(24 * activity.getResources().getDisplayMetrics().density);
            failureView.setPadding(padding, padding, padding, padding);
            failureView.setOnClickListener(view -> listener.onInteraction());
            container.addView(failureView, new FrameLayout.LayoutParams(-1, -1));
        }
        failureView.setText(message);
        failureView.setVisibility(View.VISIBLE);
        failureView.bringToFront();
    }

    private void hideFailure() {
        if (failureView != null) failureView.setVisibility(View.GONE);
    }

    private void hideCustomView() {
        if (customView != null) {
            container.removeView(customView);
            customView = null;
        }
        if (customViewCallback != null) {
            WebChromeClient.CustomViewCallback callback = customViewCallback;
            customViewCallback = null;
            callback.onCustomViewHidden();
        }
    }

    private void disposeRenderer() {
        hideCustomView();
        if (legacyCompat != null) { legacyCompat.close(); legacyCompat = null; }
        if (webView != null) {
            WebView old = webView;
            webView = null;
            if (viewport != null) {
                container.removeView(viewport);
                viewport.removeView(old);
                viewport = null;
            }
            old.stopLoading();
            old.setWebChromeClient(null);
            old.setWebViewClient(new WebViewClient());
            old.destroy();
        }
    }
}
