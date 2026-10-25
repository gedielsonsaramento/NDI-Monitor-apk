package br.com.polegar.ndimonitor;

import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25, application = Application.class)
public class MonitorApplicationTest {
    @Test public void crashRecorderWritesReportAndStillDelegatesToAndroidHandler() {
        Thread.UncaughtExceptionHandler original = Thread.getDefaultUncaughtExceptionHandler();
        java.util.concurrent.atomic.AtomicReference<Throwable> forwarded = new java.util.concurrent.atomic.AtomicReference<>();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> forwarded.set(error));
        try {
            MonitorApplication application = new MonitorApplication();
            org.robolectric.util.ReflectionHelpers.callInstanceMethod(application, "attachBaseContext",
                    org.robolectric.util.ReflectionHelpers.ClassParameter.from(android.content.Context.class,
                            RuntimeEnvironment.getApplication()));
            application.onCreate();
            Throwable error = new IllegalStateException("Teste do registrador de abertura");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), error);
            assertSame(error, forwarded.get());
            assertTrue(StartupJournal.get(RuntimeEnvironment.getApplication()).previousReport()
                    .contains("Teste do registrador de abertura"));
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(original);
        }
    }
}
