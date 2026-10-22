package br.com.polegar.ndimonitor;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;
import java.io.File;
import java.io.FileOutputStream;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "land-mdpi")
public class HtmlStageViewportTest {
    private void layout(View view, int width, int height) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, width, height);
    }

    @Test public void fullStageStaysVisibleOnWidePhoneAndOtherAspectRatios() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            View content = new View(controller.get());
            HtmlStageViewport viewport = new HtmlStageViewport(controller.get(), content, true);
            for (int[] size : new int[][]{{1536, 709}, {1920, 1080}, {1280, 720}, {640, 320}, {320, 640}}) {
                layout(viewport, size[0], size[1]);
                assertEquals(1920, content.getWidth());
                assertEquals(1080, content.getHeight());
                assertEquals(content.getScaleX(), content.getScaleY(), .0001f);
                float left = content.getTranslationX(), top = content.getTranslationY();
                float right = left + content.getWidth() * content.getScaleX();
                float bottom = top + content.getHeight() * content.getScaleY();
                assertTrue(left >= -.01f && top >= -.01f);
                assertTrue(right <= size[0] + .01f && bottom <= size[1] + .01f);
                assertEquals(left, size[0] - right, .02f);
                assertEquals(top, size[1] - bottom, .02f);
                assertTrue(Math.abs(left) < .02f || Math.abs(top) < .02f);
            }
        }
    }

    @Test public void originalPageUsesTheDeviceViewportWithoutTransform() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            View content = new View(controller.get());
            HtmlStageViewport viewport = new HtmlStageViewport(controller.get(), content, false);
            layout(viewport, 1536, 709);
            assertEquals(1536, content.getWidth());
            assertEquals(709, content.getHeight());
            assertEquals(1f, content.getScaleX(), 0);
            assertEquals(0f, content.getTranslationX(), 0);
            assertEquals(0f, content.getTranslationY(), 0);
        }
    }

    @Test public void increasedSystemFontDoesNotChangeTheDesktopCanvas() {
        RuntimeEnvironment.setFontScale(2f);
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            View content = new View(controller.get());
            HtmlStageViewport viewport = new HtmlStageViewport(controller.get(), content, true);
            layout(viewport, 1536, 709);
            assertEquals(1920, content.getWidth());
            assertEquals(1080, content.getHeight());
        }
    }

    @Test public void scaledCanvasDrawsAllFourEdgesInsideWidePhonePreview() throws Exception {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            View content = new View(controller.get()) {
                @Override protected void onDraw(Canvas canvas) {
                    canvas.drawColor(Color.rgb(15, 20, 30));
                    Paint paint = new Paint();
                    paint.setColor(Color.CYAN);
                    canvas.drawRect(0, 0, getWidth(), 24, paint);
                    canvas.drawRect(0, getHeight() - 24, getWidth(), getHeight(), paint);
                    canvas.drawRect(0, 0, 24, getHeight(), paint);
                    canvas.drawRect(getWidth() - 24, 0, getWidth(), getHeight(), paint);
                    paint.setColor(Color.WHITE);
                    paint.setTextSize(55);
                    canvas.drawText("MONITOR 1920 x 1080", 90, 100, paint);
                    canvas.drawText("CONTEUDO INTEIRO", 90, getHeight() - 70, paint);
                }
            };
            HtmlStageViewport viewport = new HtmlStageViewport(controller.get(), content, true);
            layout(viewport, 1536, 709);
            Bitmap bitmap = Bitmap.createBitmap(1536, 709, Bitmap.Config.ARGB_8888);
            viewport.draw(new Canvas(bitmap));
            int left = Math.round(content.getTranslationX()), top = Math.round(content.getTranslationY());
            int right = Math.round(left + content.getWidth() * content.getScaleX());
            int bottom = Math.round(top + content.getHeight() * content.getScaleY());
            assertEquals(Color.CYAN, bitmap.getPixel(left + 3, 709 / 2));
            assertEquals(Color.CYAN, bitmap.getPixel(right - 4, 709 / 2));
            assertEquals(Color.CYAN, bitmap.getPixel(1536 / 2, top + 3));
            assertEquals(Color.CYAN, bitmap.getPixel(1536 / 2, bottom - 4));
            File directory = new File("build/reports/html-stage-previews");
            assertTrue(directory.isDirectory() || directory.mkdirs());
            try (FileOutputStream output = new FileOutputStream(new File(directory, "wide-phone-16x9.png"))) {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
            }
            bitmap.recycle();
        }
    }
}
