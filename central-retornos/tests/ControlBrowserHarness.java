package br.com.polegar.ndimonitor;
import org.json.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class ControlBrowserHarness {
    public static void main(String[] args) throws Exception {
        final String[] token={""}, mode={"html"}, selected={"Músicos"};
        AtomicInteger actions=new AtomicInteger();
        LanControlServer server=new LanControlServer("127.0.0.1",0,new LanControlServer.Controller(){
            public String token(){return token[0];}
            public JSONObject pair(String key,String name)throws Exception{token[0]=key;return new JSONObject().put("id","browser-device-123456").put("name",name).put("durable",true);}
            public JSONObject status()throws Exception{return new JSONObject().put("id","browser-device-123456").put("schema",1).put("version","1.5.2")
                .put("mode",mode[0]).put("source",selected[0]).put("uptime_ms",600000).put("durable",true)
                .put("playback",new JSONObject().put("state",mode[0].equals("html")?"loaded":"receiving").put("capture_fps",30).put("dropped_frames",0).put("queue_frames",0).put("source_fps",30).put("width",1920).put("height",1080))
                .put("metrics",new JSONObject().put("memory_pss_kib",124000).put("cpu_percent",12.3).put("rx_bytes",4200000).put("tx_bytes",1000).put("sample_window_ms",5000))
                .put("events",new JSONArray().put(new JSONObject().put("kind","manual_audio").put("at",System.currentTimeMillis())))
                .put("actions",actions.get());}
            public JSONObject sources()throws Exception{return new JSONObject().put("sources",new JSONArray().put(new JSONObject().put("mode","ndi").put("name","Spresenter (Retorno)")));}
            public JSONObject command(JSONObject cmd)throws Exception{
                actions.incrementAndGet();if(cmd.optString("action").equals("set_source")){mode[0]=cmd.getString("mode");selected[0]=cmd.optString("name","HTML");}
                return new JSONObject().put("applied",true);
            }
        });server.start();System.out.println(new JSONObject().put("address",server.address()).put("code",server.openPairing()));System.out.flush();
        while(System.in.read()!=-1){}server.close();
    }
}
