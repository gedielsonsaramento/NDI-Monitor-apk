package br.com.polegar.ndimonitor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Runs the production Java HTTP implementation with synthetic credentials, without Android/native code. */
public final class SetupBrowserHarness {
    public static void main(String[] args) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        try (SpresenterPairingServer server = new SpresenterPairingServer("127.0.0.1", new SpresenterPairingServer.Listener() {
            @Override public void onConnection(SpresenterConnection connection) {
                boolean correct = "http://192.0.2.53:5050".equals(connection.origin)
                        && "spk_synthetic_browser_fixture_123456".equals(connection.token);
                System.out.println("{\"received\":" + correct + "}");
                System.out.flush();
                done.countDown();
            }
            @Override public void onEnded() { done.countDown(); }
        })) {
            System.out.println("{\"address\":\"" + server.address() + "\",\"code\":\"" + server.code() + "\"}");
            System.out.flush();
            server.start();
            done.await(60, TimeUnit.SECONDS);
        }
    }
}
