package br.com.polegar.ndimonitor;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=23, manifest=Config.NONE)
public class LanControlServerTest {
    private LanControlServer server;
    private String token="";
    private final AtomicInteger commands=new AtomicInteger();
    @Before public void start() throws Exception {
        server=new LanControlServer("127.0.0.1",0,new LanControlServer.Controller(){
            public String token(){return token;}
            public JSONObject pair(String key,String name)throws Exception{token=key;return new JSONObject().put("id","synthetic-device-123456").put("name",name);}
            public JSONObject status()throws Exception{return new JSONObject().put("name","Private name");}
            public JSONObject sources()throws Exception{return new JSONObject().put("sources",new org.json.JSONArray());}
            public JSONObject command(JSONObject request)throws Exception{commands.incrementAndGet();return new JSONObject().put("applied",true);}
        });server.start();
    }
    @After public void close(){server.close();}
    private String request(String method,String path,String body,String extra)throws Exception{
        URI uri=URI.create(server.address());
        try(Socket socket=new Socket(uri.getHost(),uri.getPort())){
            socket.setSoTimeout(4000);
            String h=method+" "+path+" HTTP/1.1\r\nHost: "+uri.getAuthority()+"\r\n"+extra;
            if(body!=null)h+="Content-Type: application/json\r\nContent-Length: "+body.getBytes(StandardCharsets.UTF_8).length+"\r\n";
            socket.getOutputStream().write((h+"\r\n"+(body==null?"":body)).getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;
            while((n=socket.getInputStream().read(buf))!=-1)out.write(buf,0,n);
            return out.toString("UTF-8");
        }
    }
    private String auth(){return "Authorization: Bearer "+token+"\r\n";}
    private String pair(String code)throws Exception{return request("POST","/v1/pair",new JSONObject().put("code",code).put("name","Músicos").toString(),"");}
    private void paired()throws Exception{assertTrue(pair(server.openPairing()).startsWith("HTTP/1.1 200"));}
    @Test public void discoveryExposesNoCodeTokenNameOrMetrics()throws Exception{
        String code=server.openPairing();String r=request("GET","/v1/hello",null,"");
        assertTrue(r.startsWith("HTTP/1.1 200"));assertTrue(r.contains("s-retorno-control"));assertFalse(r.contains(code));assertFalse(r.contains("Private name"));
        String denied=request("GET","/v1/status",null,"");assertTrue(denied.startsWith("HTTP/1.1 401"));assertFalse(denied.contains("Private name"));
    }
    @Test public void pairingRequiresVisibleWindowAndCodeIsSingleUse()throws Exception{
        assertTrue(pair("000000").startsWith("HTTP/1.1 403"));String code=server.openPairing();
        assertTrue(pair(code).startsWith("HTTP/1.1 200"));assertTrue(token.matches("[a-f0-9]{64}"));
        assertTrue(pair(code).startsWith("HTTP/1.1 403"));assertTrue(request("GET","/v1/status",null,auth()).contains("Private name"));
    }
    @Test public void closingOrExpiringPairingRejectsTheOldCode()throws Exception{
        String code=server.openPairing();server.closePairing();assertTrue(pair(code).startsWith("HTTP/1.1 403"));
        code=server.openPairing(1);Thread.sleep(5);assertTrue(pair(code).startsWith("HTTP/1.1 403"));
    }
    @Test public void fiveWrongAttemptsInvalidateSession()throws Exception{
        String code=server.openPairing();for(int i=0;i<5;i++)assertTrue(pair(code.equals("000000")?"111111":"000000").startsWith("HTTP/1.1 403"));
        assertTrue(pair(code).startsWith("HTTP/1.1 403"));assertEquals("",token);
    }
    @Test public void newPairingRevokesOldControllerCredential()throws Exception{
        paired();String old=token;paired();assertNotEquals(old,token);
        assertTrue(request("GET","/v1/status",null,"Authorization: Bearer "+old+"\r\n").startsWith("HTTP/1.1 401"));
    }
    @Test public void rejectsBrowserOriginsDuplicateHeadersAndWrongHost()throws Exception{
        paired();assertTrue(request("GET","/v1/status",null,auth()+"Origin: null\r\n").startsWith("HTTP/1.1 403"));
        assertTrue(request("GET","/v1/status",null,auth()+"Origin: http://attacker.example\r\n").startsWith("HTTP/1.1 403"));
        assertTrue(request("GET","/v1/status",null,auth()+"Host: attacker.example\r\n").startsWith("HTTP/1.1 400"));
        assertTrue(request("GET","/v1/status",null,auth()+"Authorization: other\r\n").startsWith("HTTP/1.1 400"));
    }
    @Test public void duplicateCommandExecutesOnlyOnceAndChangedPayloadConflicts()throws Exception{
        paired();String b=new JSONObject().put("id","synthetic-command-001").put("action","reload_html").toString();
        assertTrue(request("POST","/v1/commands",b,auth()).startsWith("HTTP/1.1 200"));
        assertTrue(request("POST","/v1/commands",b,auth()).startsWith("HTTP/1.1 200"));assertEquals(1,commands.get());
        assertTrue(request("POST","/v1/commands",b.replace("reload_html","clear_html_cache"),auth()).startsWith("HTTP/1.1 409"));assertEquals(1,commands.get());
    }
    @Test public void malformedAndOversizedPayloadsNeverReachActions()throws Exception{
        paired();assertTrue(request("POST","/v1/commands","{oops}",auth()).startsWith("HTTP/1.1 400"));
        assertTrue(request("POST","/v1/commands","{}",auth()).startsWith("HTTP/1.1 400"));
        assertTrue(request("POST","/v1/commands"," ".repeat(9000),auth()).startsWith("HTTP/1.1 413"));
        assertTrue(request("POST","/v1/commands","{}",auth()+"Transfer-Encoding: chunked\r\n").startsWith("HTTP/1.1 403"));assertEquals(0,commands.get());
    }
    @Test public void revocationImmediatelyBlocksReadsAndCommands()throws Exception{
        paired();String previous=auth();token="";
        assertTrue(request("GET","/v1/status",null,previous).startsWith("HTTP/1.1 401"));
        assertTrue(request("POST","/v1/commands","{}",previous).startsWith("HTTP/1.1 401"));
    }
}
