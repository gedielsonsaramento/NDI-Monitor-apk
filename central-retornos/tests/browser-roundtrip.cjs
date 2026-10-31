const {chromium}=require('playwright');
const {spawn}=require('node:child_process');
const http=require('node:http'),fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const base=path.resolve(__dirname,'../..');
const work=process.env.MONITOR_BROWSER_WORK;
if(!work||!process.env.JAVA_HOME||!process.env.MONITOR_JSON_JAR||!process.env.CHROME_BIN)throw new Error('Set MONITOR_BROWSER_WORK, JAVA_HOME, MONITOR_JSON_JAR and CHROME_BIN.');
fs.mkdirSync(work,{recursive:true});
const config={jdk:process.env.JAVA_HOME};
const classes=base+'/app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes';
const cp=classes+':'+process.env.MONITOR_JSON_JAR+':'+work;
const built=require('node:child_process').spawnSync(config.jdk+'/bin/javac',['-cp',classes+':'+process.env.MONITOR_JSON_JAR,'-d',work,path.join(__dirname,'ControlBrowserHarness.java')],{encoding:'utf8'});
if(built.status)throw new Error(built.stderr);
const java=spawn(config.jdk+'/bin/java',['-cp',cp,'br.com.polegar.ndimonitor.ControlBrowserHarness'],{stdio:['pipe','pipe','pipe']});
let offline=false,commands=[],staticServer;
async function main(){
 const native=await new Promise((resolve,reject)=>{let out='';java.stdout.on('data',b=>{out+=b;if(out.includes('\n'))resolve(JSON.parse(out.split('\n')[0]));});java.once('exit',code=>reject(new Error('Java harness exited '+code)));});
 const server=staticServer=http.createServer((req,res)=>{
  if(req.url==='/'){
   res.setHeader('Content-Type','text/html');res.end(`<!doctype html><html><style>body{margin:0}iframe{border:0;width:100vw;height:100vh;display:block}</style><iframe id="panel" src="/ui/index.html"></iframe><script>
   const state={};window.spresenter={storage:{get:async k=>state[k],set:async(k,v)=>state[k]=v},outputs:{list:async()=>[{index:0,name:'Principal'},{index:1,name:'Retorno Músicos'}]},net:{httpRequest:r=>window.hostHttp(r)},ui:{postMessage:m=>document.getElementById('panel').contentWindow.dispatchEvent(new MessageEvent('message',{data:{__spresenter:true,data:m}}))}};
   window.addEventListener('message',e=>{if(e.source===document.getElementById('panel').contentWindow&&e.data&&e.data.__spresenter&&spresenter.ui.onmessage)spresenter.ui.onmessage(e.data.data);});
   </script><script src="/code.js"></script></html>`);return;
  }
  const dest=req.url==='/code.js'?base+'/central-retornos/src/code.js':base+'/central-retornos/src'+req.url;
  if(!/^\/ui\/(index.html|ui.js|style.css)$/.test(req.url)&&req.url!=='/code.js'){res.statusCode=404;res.end();return;}
  res.setHeader('Content-Type',dest.endsWith('.js')?'text/javascript':dest.endsWith('.css')?'text/css':'text/html');res.end(fs.readFileSync(dest));
 });await new Promise(r=>server.listen(0,'127.0.0.1',r));
 const browser=await chromium.launch({executablePath:process.env.CHROME_BIN,headless:true,args:['--no-sandbox','--no-zygote','--single-process','--disable-gpu']});
 try{
  const page=await browser.newPage({viewport:{width:1120,height:940}});let errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.exposeFunction('hostHttp',async r=>{
   if(r.url.endsWith('/api/v1/advanced'))return {ok:true,status:200,body:JSON.stringify({config:{outputs:[{type:'network',slug:'musicos',name:'Músicos'},{type:'network',slug:'retorno',name:'Pastor'}]}})};
   if(offline)return {ok:false,status:503,body:'{}'};
   if(r.url.endsWith('/v1/commands'))commands.push(JSON.parse(r.body));
   if(r.url.endsWith('/v1/hello')&&new URL(r.url).hostname!=='192.168.1.20')return {ok:false,status:404,body:'{}'};
   const response=await fetch(native.address+new URL(r.url).pathname,{method:r.method,headers:r.headers,body:r.body});
   return {ok:response.ok,status:response.status,body:await response.text()};
  });
  await page.goto('http://127.0.0.1:'+server.address().port);const f=page.frameLocator('#panel');
  await f.locator('#add').click();await f.locator('#pair-name').fill('Músicos');await f.locator('#pair-address').fill('192.168.1.20:48762');await f.locator('#pair-code').fill(native.code==='000000'?'000001':'000000');await f.locator('#pair-submit').click();
  await f.locator('#pair-notice').filter({hasText:'Acesso recusado'}).waitFor();
  await f.locator('#scan-prefix').fill('192.168.1');await f.locator('#scan').click();
  await f.locator('#scan-result').filter({hasText:'Busca concluída'}).waitFor();
  assert.equal(await f.locator('[data-found]').count(),1);await f.locator('[data-found]').click();
  await f.locator('#pair-code').fill(native.code);await f.locator('#pair-submit').click();
  await f.locator('.device .badge').filter({hasText:'App responde'}).waitFor();await f.locator('[data-close="pair-dialog"]').click();
  await f.locator('#catalog-details summary').click();await f.locator('#pc-link').fill('http://192.168.1.5:5050/warp/?token=spk_browser_synthetic_key');await f.locator('#pc-form button').click();
  await f.locator('#catalog-state').filter({hasText:'2 saídas HTML'}).waitFor();
  await f.locator('[data-do="details"]').click();await f.getByText('Relato: áudio',{exact:false}).waitFor();
  const before=commands.length;await f.locator('[data-do="clear_html_cache"]').click();await f.locator('[data-close="action-dialog"]').click();assert.equal(commands.length,before);
  await f.locator('[data-do="clear_html_cache"]').click();await f.locator('#action-submit').click();await f.getByText('Comando confirmado pelo app',{exact:false}).first().waitFor();assert.equal(commands.filter(x=>x.action==='clear_html_cache').length,1);
  await f.locator('[data-close="detail-dialog"]').click();await f.locator('[data-do="source"]').click();await f.locator('#source-choice').selectOption({label:'NDI · Spresenter (Retorno)'});await f.locator('#action-submit').click();await f.locator('.source b').filter({hasText:'ndi'}).waitFor();
  await f.locator('#catalog-details summary').click();await page.screenshot({path:path.join(work,'central-desktop.png'),fullPage:true});
  await page.setViewportSize({width:420,height:900});await page.screenshot({path:path.join(work,'central-mobile.png'),fullPage:true});
  const overflow=await f.locator('body').evaluate(el=>el.scrollWidth>window.innerWidth+1);assert(!overflow,'Horizontal overflow');
  offline=true;for(let i=0;i<3;i++){await f.locator('#refresh').click();await page.waitForTimeout(250);}
  await f.locator('.badge').filter({hasText:'Sem comunicação'}).waitFor();assert.equal(await f.locator('#connected').innerText(),'0');assert.equal(await f.locator('#attention').innerText(),'0');
  offline=false;await f.locator('#refresh').click();await f.locator('.badge').filter({hasText:'App responde'}).waitFor();
  assert.deepEqual(errors,[]);assert(!JSON.stringify(commands).includes('spk_browser_synthetic_key'));
  console.log(JSON.stringify({passed:true,checks:['null-source SDK relay','visible invalid-code response','254-address scan with one discovered app','real Java LAN server pairing','catalog without API credential distribution','manual error display','cache cancellation and confirmed execution','NDI selection','responsive layout','offline and reconnection'],consoleErrors:errors.length,commands:commands.length}));
 }finally{await browser.close();server.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;}).finally(()=>{java.stdin.end();java.kill();if(staticServer)staticServer.close();});
