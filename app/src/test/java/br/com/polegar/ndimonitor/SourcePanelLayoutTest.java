package br.com.polegar.ndimonitor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.android.controller.ActivityController;
import java.util.Arrays;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "sw320dp-w640dp-h320dp-land-mdpi")
public class SourcePanelLayoutTest {
    private View layout(Context context, int width, int height) {
        View root = LayoutInflater.from(context).inflate(R.layout.activity_main, null);
        ListView list = root.findViewById(R.id.source_list);
        list.setAdapter(new ArrayAdapter<>(context, R.layout.source_item,
                Arrays.asList("Computador (Retorno do palco)", "Camera", "Outra fonte")));
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, width, height);
        return root;
    }

    private void preview(View root, String name) throws IOException {
        File directory = new File("build/reports/source-panel-previews");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        }
        bitmap.recycle();
    }

    @Test public void shortPhoneLeavesRoomForAtLeastThreeSelectableSources() throws IOException {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            activity.setTheme(R.style.AppTheme);
            View root = layout(activity, 640, 320);
            preview(root, "phone");
            android.widget.LinearLayout panel = root.findViewById(R.id.source_panel);
            String dimensions = "panel=" + panel.getWidth() + "x" + panel.getHeight()
                    + ", list=" + root.findViewById(R.id.source_list).getHeight()
                    + ", density=" + activity.getResources().getDisplayMetrics().density
                    + ", fontScale=" + activity.getResources().getConfiguration().fontScale;
            for (int i = 0; i < panel.getChildCount(); ++i) {
                dimensions += ", child" + i + "=" + panel.getChildAt(i).getHeight();
            }
            assertTrue(dimensions, root.findViewById(R.id.source_list).getHeight() >= 3 * 48);
            assertNull(root.findViewById(R.id.home_settings_button));
            assertNull(root.findViewById(R.id.boot_checkbox));
            for (int id : new int[]{R.id.scan_button, R.id.monitor_settings_button, R.id.close_panel_button}) {
                View button = root.findViewById(id);
                assertTrue(button.getWidth() >= 48);
                assertTrue(button.getHeight() >= 48);
                assertTrue(button.getWidth() - button.getPaddingLeft() - button.getPaddingRight() <= 24);
            }
            assertEquals(View.GONE, root.findViewById(R.id.menu_button).getVisibility());
        }
    }

    @Test public void largePhoneFontStillLeavesASelectableSource() throws IOException {
        RuntimeEnvironment.setFontScale(2f);
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            activity.setTheme(R.style.AppTheme);
            View root = layout(activity, 640, 320);
            preview(root, "phone-large-font");
            for (int id : new int[]{R.id.scan_button, R.id.monitor_settings_button, R.id.close_panel_button}) {
                View button = root.findViewById(id);
                android.view.ViewGroup header = (android.view.ViewGroup) button.getParent();
                assertTrue(button.getLeft() >= 0 && button.getRight() <= header.getWidth());
                assertEquals(View.VISIBLE, button.getVisibility());
            }
            assertTrue(root.findViewById(R.id.source_list).getHeight() >= 48);
        }
    }

    @Test
    @Config(qualifiers = "sw720dp-w1280dp-h720dp-land-mdpi")
    public void largeScreenKeepsLargerControlsAndVisibleList() throws IOException {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            activity.setTheme(R.style.AppTheme);
            View root = layout(activity, 1280, 720);
            preview(root, "large-screen");
            assertEquals(56, root.findViewById(R.id.scan_button).getHeight());
            assertTrue(root.findViewById(R.id.source_list).getHeight() > 400);
        }
    }
}
