package br.com.polegar.ndimonitor;

import android.app.Application;
import android.content.Context;
import android.net.*;
import android.os.Looper;
import android.view.View;
import android.widget.ListView;
import java.net.InetAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={23,25}, application=Application.class, shadows={LanNetworkMonitorTest.Connections.class,
        MainActivityModeTest.NoNativeReceiver.class, MainActivityModeTest.FakeHtmlDiscovery.class})
public class LanNetworkMonitorTest {
    @Implements(ConnectivityManager.class)
    public static class Connections extends SpresenterSetupNetworkTest.Connections {
        static ConnectivityManager.NetworkCallback callback;
        static int removed;
        @Implementation protected void registerNetworkCallback(NetworkRequest request, ConnectivityManager.NetworkCallback value) {
            callback=value;
        }
        @Implementation protected void unregisterNetworkCallback(ConnectivityManager.NetworkCallback value) {
            removed++; if(callback==value)callback=null;
        }
    }
    private Context context;
    @Before public void reset() {
        context=RuntimeEnvironment.getApplication();
        Connections.capabilities.clear();Connections.links.clear();Connections.active=null;
        Connections.callback=null;Connections.removed=0;
        MainActivityModeTest.FakeHtmlDiscovery.starts=0;MainActivityModeTest.FakeHtmlDiscovery.publicStarts=0;
        context.getSharedPreferences(MainActivity.PREFERENCES,Context.MODE_PRIVATE).edit().clear().commit();
        ReflectionHelpers.setStaticField(NdiReceiver.class,"callbackTarget",null);
    }
    private void idle() { Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1)); }
    private Network network(int id,int transport,String host) throws Exception {
        Network network=ReflectionHelpers.callConstructor(Network.class,ClassParameter.from(int.class,id));
        NetworkCapabilities caps=new NetworkCapabilities();
        ReflectionHelpers.callInstanceMethod(caps,"addTransportType",ClassParameter.from(int.class,transport));
        Connections.capabilities.put(network,caps);setAddress(network,host);return network;
    }
    private void setAddress(Network network,String host) throws Exception {
        LinkProperties properties=new LinkProperties();
        LinkAddress address=ReflectionHelpers.callConstructor(LinkAddress.class,
                ClassParameter.from(InetAddress.class,InetAddress.getByName(host)),ClassParameter.from(int.class,24));
        ReflectionHelpers.callInstanceMethod(properties,"addLinkAddress",ClassParameter.from(LinkAddress.class,address));
        Connections.links.put(network,properties);
    }
    @Test public void coalescesCallbacksAndDetectsNewAddressWithoutRepeatingUnchangedLan() throws Exception {
        Network wifi=network(2,NetworkCapabilities.TRANSPORT_WIFI,"192.168.1.20");
        AtomicInteger scans=new AtomicInteger();LanNetworkMonitor monitor=new LanNetworkMonitor(context,scans::incrementAndGet);
        try {
            monitor.start();Connections.callback.onAvailable(wifi);
            Connections.callback.onLinkPropertiesChanged(wifi,Connections.links.get(wifi));idle();
            assertEquals(1,scans.get());
            Connections.callback.onAvailable(wifi);idle();assertEquals(1,scans.get());
            setAddress(wifi,"10.20.80.25");Connections.callback.onLinkPropertiesChanged(wifi,Connections.links.get(wifi));
            idle();assertEquals(2,scans.get());
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(20));assertEquals(2,scans.get());
        } finally {monitor.stop();}
    }
    @Test public void ignoresMobileAndVpnAndUnregistersLateNotifications() throws Exception {
        network(1,NetworkCapabilities.TRANSPORT_CELLULAR,"10.1.2.3");
        Network vpn=network(2,NetworkCapabilities.TRANSPORT_WIFI,"192.168.1.20");
        ReflectionHelpers.callInstanceMethod(Connections.capabilities.get(vpn),"addTransportType",
                ClassParameter.from(int.class,NetworkCapabilities.TRANSPORT_VPN));
        AtomicInteger scans=new AtomicInteger();LanNetworkMonitor monitor=new LanNetworkMonitor(context,scans::incrementAndGet);
        monitor.start();idle();assertEquals(0,scans.get());
        ConnectivityManager.NetworkCallback previous=Connections.callback;
        network(3,NetworkCapabilities.TRANSPORT_ETHERNET,"192.168.2.20");
        previous.onAvailable(vpn);Shadows.shadowOf(Looper.getMainLooper()).idle();
        monitor.stop();previous.onAvailable(vpn);idle();assertEquals(0,scans.get());assertEquals(1,Connections.removed);
    }
    @Test public void initialLanAndNetworkSwitchFindNewSourcesWithoutUnlinkingOrChangingPlayback() throws Exception {
        String old="http://192.168.1.5:5050/output/musicos";
        android.content.SharedPreferences prefs=context.getSharedPreferences(MainActivity.PREFERENCES,Context.MODE_PRIVATE);
        prefs.edit().putString(MainActivity.KEY_DISPLAY_MODE,"html").putString(MainActivity.KEY_HTML_URL,old).commit();
        new SpresenterApiSettings(context).save(new SpresenterConnection("http://192.168.1.5:5050","spk_example_saved_network"));
        Network wifi=network(2,NetworkCapabilities.TRANSPORT_WIFI,"192.168.1.20");
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            idle();assertEquals(1,MainActivityModeTest.FakeHtmlDiscovery.publicStarts);
            assertEquals(View.GONE,controller.get().findViewById(R.id.source_panel).getVisibility());
            setAddress(wifi,"10.20.80.20");Connections.callback.onLinkPropertiesChanged(wifi,Connections.links.get(wifi));idle();
            assertEquals(2,MainActivityModeTest.FakeHtmlDiscovery.publicStarts);
            MainActivityModeTest.FakeHtmlDiscovery.listener.onSource(new HtmlSource("http://10.20.80.5:5050/output/retorno","Novo PC"));
            MainActivityModeTest.FakeHtmlDiscovery.listener.onFinished();
            ListView list=controller.get().findViewById(R.id.source_list);
            assertEquals(2,list.getAdapter().getCount());
            assertEquals(old,prefs.getString(MainActivity.KEY_HTML_URL,""));
            assertEquals(1,new SpresenterApiSettings(context).origins().size());
            assertEquals(0,MainActivityModeTest.FakeHtmlDiscovery.starts);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(40));
            assertEquals(2,MainActivityModeTest.FakeHtmlDiscovery.publicStarts);
        }
    }
}
