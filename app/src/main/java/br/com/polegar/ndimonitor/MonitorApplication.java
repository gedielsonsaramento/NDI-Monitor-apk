package br.com.polegar.ndimonitor;

import android.app.Application;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

public final class MonitorApplication extends Application {
    private SupportRuntime support;

    static SupportRuntime support(Context context) {
        Context app = context.getApplicationContext();
        if (app instanceof MonitorApplication && ((MonitorApplication) app).support != null)
            return ((MonitorApplication) app).support;
        return new SupportRuntime(app, BuildConfig.SUPPORT_ENDPOINT, BuildConfig.VERSION_NAME, false);
    }

    @Override public void onCreate() {
        super.onCreate();
        StartupJournal journal = StartupJournal.get(this);
        support = new SupportRuntime(this, BuildConfig.SUPPORT_ENDPOINT, BuildConfig.VERSION_NAME,
                journal.needsRecovery());
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            private int started;
            @Override public void onActivityStarted(Activity activity) {
                if (started++ == 0) support.onForeground();
            }
            @Override public void onActivityStopped(Activity activity) {
                started = Math.max(0, started - 1);
                if (started == 0) support.onBackground();
            }
            @Override public void onActivityCreated(Activity activity, Bundle state) { }
            @Override public void onActivityResumed(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try { journal.recordCrash(thread, error); }
            catch (Throwable unavailable) { /* Always retain Android's crash handling. */ }
            try { support.recordJavaCrash(error); }
            catch (Throwable unavailable) { /* Support must never replace Android's crash handling. */ }
            if (previous != null) previous.uncaughtException(thread, error);
            else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });
    }
}
