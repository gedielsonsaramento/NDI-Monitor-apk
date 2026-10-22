package br.com.polegar.ndimonitor;

import android.app.Activity;
import android.net.Uri;
import android.os.Looper;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.FrameLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class HtmlMonitorTest {
    private static final String URL = "http://192.168.2.53:5050/output/saida-html-1";

    private static WebResourceRequest request(String url, boolean mainFrame) {
        return new WebResourceRequest() {
            public Uri getUrl() { return Uri.parse(url); }
            public boolean isForMainFrame() { return mainFrame; }
            public boolean isRedirect() { return false; }
            public boolean hasGesture() { return false; }
            public String getMethod() { return "GET"; }
            public Map<String, String> getRequestHeaders() { return java.util.Collections.emptyMap(); }
        };
    }

    private HtmlMonitor create(Activity activity, FrameLayout frame, List<Integer> messages) {
        return new HtmlMonitor(activity, frame, new HtmlMonitor.Listener() {
            public void onStatus(int message) { messages.add(message); }
            public void onInteraction() { }
        });
    }

    @Test
    @Config(qualifiers = "land-xxhdpi")
    public void desktopCanvasIs1080pEvenOnDensePhoneWithLargeSystemFont() {
        org.robolectric.RuntimeEnvironment.setFontScale(2f);
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            FrameLayout frame = new FrameLayout(activity);
            HtmlMonitor monitor = create(activity, frame, new ArrayList<>());
            monitor.resume();
            monitor.show(URL);
            frame.measure(android.view.View.MeasureSpec.makeMeasureSpec(1536, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(709, android.view.View.MeasureSpec.EXACTLY));
            frame.layout(0, 0, 1536, 709);
            HtmlStageViewport viewport = (HtmlStageViewport) frame.getChildAt(0);
            WebView web = (WebView) viewport.getChildAt(0);
            assertTrue(viewport.usesStageLayout());
            // Robolectric replaces Chromium's measurement. Canvas geometry is covered
            // with a real drawing View in HtmlStageViewportTest.
            assertEquals(1f, web.getResources().getDisplayMetrics().density, 0);
            assertEquals(100, web.getSettings().getTextZoom());
            assertFalse(web.getSettings().getUseWideViewPort());
            assertTrue(activity.getResources().getDisplayMetrics().density > 1f);
            assertEquals(2f, activity.getResources().getConfiguration().fontScale, 0);
            monitor.destroy();
        }
    }

    @Test public void changingDisplayLayoutKeepsAddressAndSurvivesPause() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            FrameLayout frame = new FrameLayout(activity);
            HtmlMonitor monitor = create(activity, frame, new ArrayList<>());
            monitor.resume();
            monitor.show(URL);
            WebView original = (WebView) ((HtmlStageViewport) frame.getChildAt(0)).getChildAt(0);
            monitor.setStageLayout(false);
            HtmlStageViewport viewport = (HtmlStageViewport) frame.getChildAt(0);
            WebView changed = (WebView) viewport.getChildAt(0);
            assertNotSame(original, changed);
            assertNull(original.getParent());
            assertFalse(viewport.usesStageLayout());
            assertTrue(changed.getSettings().getUseWideViewPort());
            assertEquals(URL, Shadows.shadowOf(changed).getLastLoadedUrl());
            monitor.pause();
            monitor.resume();
            assertFalse(((HtmlStageViewport) frame.getChildAt(0)).usesStageLayout());
            monitor.setStageLayout(true);
            assertTrue(((HtmlStageViewport) frame.getChildAt(0)).usesStageLayout());
            monitor.destroy();
        }
    }

    @Test public void opensSavedUrlWithMediaAndSafeWebSettings() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            FrameLayout frame = new FrameLayout(activity);
            HtmlMonitor monitor = create(activity, frame, new ArrayList<>());
            monitor.show(URL);
            assertEquals(0, frame.getChildCount());
            monitor.resume();
            WebView web = (WebView) ((HtmlStageViewport) frame.getChildAt(0)).getChildAt(0);
            assertEquals(URL, Shadows.shadowOf(web).getLastLoadedUrl());
            assertTrue(web.getSettings().getJavaScriptEnabled());
            assertFalse(web.getSettings().getMediaPlaybackRequiresUserGesture());
            assertFalse(web.getSettings().getAllowFileAccess());
            assertFalse(web.getSettings().getAllowContentAccess());
            assertTrue(web.getWebViewClient().shouldOverrideUrlLoading(web,
                    request("intent://external/app", true)));
            assertFalse(web.getWebViewClient().shouldOverrideUrlLoading(web, request(URL, true)));
            monitor.destroy();
        }
    }

    @Test public void leavingHtmlDisposesRendererAndResumeRestoresAddress() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            FrameLayout frame = new FrameLayout(activity);
            HtmlMonitor monitor = create(activity, frame, new ArrayList<>());
            monitor.resume();
            monitor.show(URL);
            WebView first = (WebView) ((HtmlStageViewport) frame.getChildAt(0)).getChildAt(0);
            monitor.pause();
            assertEquals(0, frame.getChildCount());
            monitor.resume();
            WebView second = (WebView) ((HtmlStageViewport) frame.getChildAt(0)).getChildAt(0);
            assertNotSame(first, second);
            assertEquals(URL, Shadows.shadowOf(second).getLastLoadedUrl());
            monitor.hide();
            assertEquals(0, frame.getChildCount());
            monitor.resume();
            assertEquals(0, frame.getChildCount());
            monitor.destroy();
        }
    }

    @Test public void aFailedImageDoesNotMarkWholeReturnAsDisconnected() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            FrameLayout frame = new FrameLayout(activity);
            List<Integer> messages = new ArrayList<>();
            HtmlMonitor monitor = create(activity, frame, messages);
            monitor.resume();
            monitor.show(URL);
            WebView web = (WebView) ((HtmlStageViewport) frame.getChildAt(0)).getChildAt(0);
            web.getWebViewClient().onReceivedError(web, request(URL + "/image.png", false), null);
            assertFalse(messages.contains(R.string.html_retrying));
            web.getWebViewClient().onReceivedError(web, request(URL, true), null);
            assertEquals(Integer.valueOf(R.string.html_retrying), messages.get(messages.size() - 1));
            monitor.destroy();
        }
    }

    @Test public void stalledLoadRetriesAndSwitchingModeCancelsPendingWork() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            FrameLayout frame = new FrameLayout(activity);
            List<Integer> messages = new ArrayList<>();
            HtmlMonitor monitor = create(activity, frame, messages);
            monitor.resume();
            monitor.show(URL);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(15));
            assertEquals(Integer.valueOf(R.string.html_retrying), messages.get(messages.size() - 1));
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3));
            assertEquals(Integer.valueOf(R.string.html_loading), messages.get(messages.size() - 1));
            monitor.hide();
            int count = messages.size();
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(40));
            assertEquals(count, messages.size());
            monitor.destroy();
        }
    }
}
