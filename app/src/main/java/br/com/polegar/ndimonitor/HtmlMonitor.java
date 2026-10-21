package br.com.polegar.ndimonitor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private TextView failureView;
    private boolean enabled;
    private boolean paused = true;
    private boolean failed;
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

    void reload() {
        retries = 0;
        handler.removeCallbacks(retry);
        if (enabled && !paused && !address.isEmpty()) loadCurrentAddress();
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
            webView = new WebView(activity) {
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
            settings.setSupportZoom(false);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            webView.setWebViewClient(new WebViewClient() {
                @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    return !HtmlSourceUrl.isWebAddress(request.getUrl().toString());
                }

                @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                    if (!isCurrent(view) || !HtmlSourceUrl.isWebAddress(url)) return;
                    failed = false;
                    listener.onStatus(R.string.html_loading);
                }

                @Override public void onPageFinished(WebView view, String url) {
                    if (!isCurrent(view) || failed || !HtmlSourceUrl.isWebAddress(url)) return;
                    retries = 0;
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

                @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                    if (Build.VERSION.SDK_INT >= 26 && view == webView) {
                        disposeRenderer();
                        if (enabled && !paused) fail(true);
                    }
                    return true;
                }
            });
            webView.setWebChromeClient(new WebChromeClient() {
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
            container.addView(webView, 0, new FrameLayout.LayoutParams(-1, -1));
            return true;
        } catch (RuntimeException unavailable) {
            disposeRenderer();
            failed = true;
            showFailure(R.string.html_renderer_unavailable);
            listener.onStatus(R.string.html_renderer_unavailable);
            return false;
        }
    }

    private boolean isCurrent(WebView view) {
        return enabled && !paused && view == webView;
    }

    private void loadCurrentAddress() {
        handler.removeCallbacks(retry);
        if (!ensureRenderer()) return;
        hideCustomView();
        failed = false;
        hideFailure();
        listener.onStatus(R.string.html_loading);
        handler.removeCallbacks(loadTimeout);
        handler.postDelayed(loadTimeout, 15000);
        webView.loadUrl(address);
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
        if (webView != null) {
            WebView old = webView;
            webView = null;
            container.removeView(old);
            old.stopLoading();
            old.setWebChromeClient(null);
            old.setWebViewClient(new WebViewClient());
            old.destroy();
        }
    }
}
