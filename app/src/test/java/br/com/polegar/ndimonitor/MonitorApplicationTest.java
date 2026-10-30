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
    @Test public void authorizedCrashQueueStillDelegatesAndDoesNotSendFromTheCrashHandler() throws Exception {
        Thread.UncaughtExceptionHandler original = Thread.getDefaultUncaughtExceptionHandler();
        java.util.concurrent.atomic.AtomicReference<Throwable> forwarded = new java.util.concurrent.atomic.AtomicReference<>();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> forwarded.set(error));
        SupportRuntime runtime = null;
        try {
            MonitorApplication application = new MonitorApplication();
            org.robolectric.util.ReflectionHelpers.callInstanceMethod(application, "attachBaseContext",
                    org.robolectric.util.ReflectionHelpers.ClassParameter.from(android.content.Context.class,
                            RuntimeEnvironment.getApplication()));
            application.onCreate();
            SupportRuntime initial = org.robolectric.util.ReflectionHelpers.getField(application, "support");
            initial.closeForTests();
            SupportStore store = new SupportStore(RuntimeEnvironment.getApplication(), SupportTestSupport.ENDPOINT);
            store.setEnabled(true, 1);
            SupportTestSupport.Factory factory = new SupportTestSupport.Factory();
            SupportTestSupport.Scheduler clock = new SupportTestSupport.Scheduler();
            runtime = new SupportRuntime(store, "1.4.12", SupportTestSupport.probe(clock),
                    new SupportTransport(factory), clock, () -> null, false);
            org.robolectric.util.ReflectionHelpers.setField(application, "support", runtime);
            Throwable error = new IllegalStateException("http://private.test/?token=spk_PRIVATE");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), error);
            assertSame(error, forwarded.get());
            assertEquals(1, store.pending().size());
            assertEquals("java_crash", store.pending().get(0).getString("kind"));
            assertFalse(store.pending().get(0).toString().contains("spk_PRIVATE"));
            assertTrue(factory.connections.isEmpty());
        } finally {
            if (runtime != null) runtime.closeForTests();
            Thread.setDefaultUncaughtExceptionHandler(original);
        }
    }

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
