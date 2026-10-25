package br.com.polegar.ndimonitor;

import android.app.Application;

public final class MonitorApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        StartupJournal journal = StartupJournal.get(this);
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try { journal.recordCrash(thread, error); }
            catch (Throwable unavailable) { /* Always retain Android's crash handling. */ }
            if (previous != null) previous.uncaughtException(thread, error);
            else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });
    }
}
