package br.com.polegar.ndimonitor;

import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.json.*;
import java.lang.reflect.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=23)
public class LocalControlTest {
    private Context context;
    @Before public void clear(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences(LocalControlStore.PREFS,0).edit().clear().commit();}
    @Test public void managementIsOffByDefaultAndRevocationDeletesIdentityAndEvents()throws Exception{
        LocalControlStore store=new LocalControlStore(context);assertFalse(store.enabled());store.event("java_crash");assertEquals(0,store.events().length());
        store.enable();store.pair("a".repeat(64),"Músicos");assertFalse(store.id().isEmpty());store.event("manual_audio");assertTrue(store.events().length()>0);
        store.revoke();assertFalse(store.enabled());assertEquals("",store.token());assertEquals("",store.id());assertEquals(0,store.events().length());
    }
    @Test public void credentialIsEncryptedAtRestAndSurvivesRecreationWithSameKey()throws Exception{
        KeyGenerator gen=KeyGenerator.getInstance("AES");gen.init(128);SecretKey key=gen.generateKey();SpresenterTokenCipher cipher=new SpresenterTokenCipher(()->key);
        LocalControlStore first=new LocalControlStore(context,cipher);first.enable();first.pair("b".repeat(64),"Pastor");
        assertFalse(context.getSharedPreferences(LocalControlStore.PREFS,0).getString("credential","").contains("b".repeat(64)));
        LocalControlStore second=new LocalControlStore(context,cipher);assertEquals(first.id(),second.id());assertEquals(first.token(),second.token());assertTrue(second.durable());
    }
    @Test public void dailyPowerCyclesDoNotCreateFailureReportsAndKeepThePairing()throws Exception{
        KeyGenerator gen=KeyGenerator.getInstance("AES");gen.init(128);SecretKey key=gen.generateKey();
        SpresenterTokenCipher cipher=new SpresenterTokenCipher(()->key);
        LocalControlStore store=new LocalControlStore(context,cipher);store.enable();store.pair("c".repeat(64),"Músicos e Pastor");
        String id=store.id();int baseline=store.events().length();
        for(int day=0;day<40;day++){
            store.foreground(); // Simulate losing power without onStop/background.
            store=new LocalControlStore(context,cipher);store.foreground();
            assertEquals(id,store.id());assertEquals("c".repeat(64),store.token());
            assertEquals("Músicos e Pastor",store.name());assertEquals(baseline,store.events().length());
        }
        store.event("java_crash");assertEquals(baseline+1,store.events().length());
    }
    @Test public void sourceAndConnectionAbsenceAreNotStoredAsAutomaticFailures()throws Exception{
        LocalControlStore store=new LocalControlStore(context);store.enable();
        for(String kind:java.util.List.of("session_interrupted","html_error","ndi_signal_error"))store.event(kind);
        assertEquals(0,store.events().length());
        store.event("html_renderer_failure");assertEquals("html_renderer_failure",store.events().getJSONObject(0).getString("kind"));
    }
    @Test public void legacyAvailabilityEntriesAreExcludedFromTheReport()throws Exception{
        context.getSharedPreferences(LocalControlStore.PREFS,0).edit().putString("events",new JSONArray()
                .put(new JSONObject().put("kind","session_interrupted").put("at",1))
                .put(new JSONObject().put("kind","ndi_signal_error").put("at",2))
                .put(new JSONObject().put("kind","native_crash").put("at",3)).toString()).commit();
        LocalControlStore store=new LocalControlStore(context);assertEquals(1,store.events().length());
        assertEquals("native_crash",store.events().getJSONObject(0).getString("kind"));
    }
    @Test public void localNamesKeepLettersAndNormalizeWhitespace(){
        assertEquals("Músicos Pastor",LocalControlStore.cleanName("  Músicos\t Pastor\n"));
    }
    @Test public void eventHistoryIsBoundedAndDoesNotStoreArbitraryMessages()throws Exception{
        LocalControlStore store=new LocalControlStore(context);store.enable();
        store.event("http://secret/?token=private");assertEquals(0,store.events().length());
        for(int i=0;i<80;i++)store.event(i%2==0?"java_crash":"manual_audio");assertEquals(24,store.events().length());
        for(int i=0;i<24;i++)assertEquals(2,store.events().getJSONObject(i).length());
    }
    @Test public void confirmedExitReasonsExcludeOldFailuresAndNormalUserStops()throws Exception{
        LocalControlStore store=new LocalControlStore(context);store.enable();store.foreground();
        store.reviewPreviousExit(new SupportExitHistory.Exit(1,"native_crash",false));
        assertEquals(0,store.events().length());
        store.reviewPreviousExit(new SupportExitHistory.Exit(System.currentTimeMillis()+1000,"",true));
        store.foreground();assertEquals(0,store.events().length());
        store.reviewPreviousExit(new SupportExitHistory.Exit(System.currentTimeMillis()+2000,"native_crash",false));
        assertEquals("native_crash",store.events().getJSONObject(0).getString("kind"));
        store.foreground();assertEquals(1,store.events().length());
    }
    @Test public void measuredFpsHandlesCounterResetAndStaleFrames()throws Exception{
        ReceiverStats stats=new ReceiverStats();assertTrue(stats.json(1000).isNull("capture_fps"));
        stats.accept(30,0,0,1000);stats.accept(60,2,1,2000);assertEquals(30,stats.json(2000).getDouble("capture_fps"),.01);
        assertEquals("waiting",stats.json(6000).getString("state"));stats.accept(1,0,0,7000);assertTrue(stats.json(7000).isNull("capture_fps"));
        stats.reset();assertTrue(stats.json(8000).isNull("captured_frames"));
    }
    private JSONObject invoke(MainActivity activity,String method,JSONObject command)throws Exception{
        Method m=MainActivity.class.getDeclaredMethod(method,JSONObject.class);m.setAccessible(true);
        try{return (JSONObject)m.invoke(activity,command);}catch(InvocationTargetException e){throw (Exception)e.getCause();}
    }
    @Test public void centralCatalogSurvivesAppRecreationAndRejectsForeignSource()throws Exception{
        try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            JSONObject cat=new JSONObject().put("action","catalog").put("origin","http://192.168.1.5:5050")
                    .put("sources",new JSONArray().put(new JSONObject().put("name","Músicos").put("url","http://192.168.1.5:5050/output/musicos")));
            assertTrue(invoke(c.get(),"localCommand",cat).getBoolean("applied"));
            String saved=c.get().getSharedPreferences(MainActivity.PREFERENCES,0).getString("local_html_catalog","");assertTrue(saved.contains("musicos"));
            cat.getJSONArray("sources").getJSONObject(0).put("url","http://192.168.1.50:5050/output/private");
            assertThrows(LanControlServer.Failure.class,()->invoke(c.get(),"localCommand",cat));
            assertEquals(saved,c.get().getSharedPreferences(MainActivity.PREFERENCES,0).getString("local_html_catalog",""));
            c.recreate();Method m=MainActivity.class.getDeclaredMethod("localSources");m.setAccessible(true);
            assertTrue(m.invoke(c.get()).toString().contains("musicos"));
        }
    }
    @Test public void cacheMaintenancePreservesSavedSourceAndRejectsWrongModeCommand()throws Exception{
        context.getSharedPreferences(MainActivity.PREFERENCES,0).edit().putString(MainActivity.KEY_HTML_URL,"http://192.168.1.5:5050/output/musicos").putString(MainActivity.KEY_DISPLAY_MODE,"html").commit();
        try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            assertTrue(invoke(c.get(),"localCommand",new JSONObject().put("action","clear_html_cache")).getBoolean("applied"));
            assertEquals("http://192.168.1.5:5050/output/musicos",context.getSharedPreferences(MainActivity.PREFERENCES,0).getString(MainActivity.KEY_HTML_URL,""));
            assertThrows(LanControlServer.Failure.class,()->invoke(c.get(),"localCommand",new JSONObject().put("action","reconnect_ndi")));
        }
    }
    @Test public void pinnedCatalogIsRetainedDuringLanOutageAndCanBeRemoved(){
        HtmlSourceCatalog catalog=new HtmlSourceCatalog();String origin="http://192.168.1.5:5050";
        catalog.replacePinnedCatalog(origin,java.util.List.of(new HtmlSource(origin+"/output/musicos","Músicos",origin)),0);
        catalog.finishScan(1000000);assertEquals(1,catalog.snapshot().size());
        catalog.replacePinnedCatalog(origin,java.util.List.of(),1000001);assertTrue(catalog.snapshot().isEmpty());
    }
}
