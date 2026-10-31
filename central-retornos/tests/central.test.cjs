const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const source=fs.readFileSync(require('node:path').join(__dirname,'../src/code.js'),'utf8');
const token='a'.repeat(64),id='synthetic-device-123456';
const wait=async()=>{for(let i=0;i<18;i++)await new Promise(setImmediate);};
async function harness(saved,handler){
  const messages=[],requests=[],writes=[];let stored=saved;
  const spresenter={ui:{postMessage:m=>messages.push(structuredClone(m))},storage:{
    get:async()=>stored,set:async(k,v)=>{stored=structuredClone(v);writes.push(stored);}},outputs:{list:async()=>[{index:0,name:'Principal'}]},
    net:{httpRequest:async r=>{requests.push(r);return handler?handler(r):response(r);}}};
  function response(r){
    if(r.url.endsWith('/v1/pair'))return ok({ok:true,id,token,name:'Músicos',durable:true});
    if(r.url.endsWith('/v1/status'))return ok({ok:true,schema:1,id,name:'Músicos',version:'1.5.0',mode:'html',source:'Músicos',playback:{state:'loaded'},metrics:{sample_window_ms:5000,rx_bytes:10000,memory_pss_kib:120000,cpu_percent:3},events:[]});
    if(r.url.endsWith('/v1/sources'))return ok({ok:true,sources:[]});
    if(r.url.endsWith('/v1/commands'))return ok({ok:true,applied:true,command_id:JSON.parse(r.body).id});
    return {ok:false,status:404,body:'{}'};
  }
  vm.runInNewContext(source,{spresenter,Date,Map,Set,Promise,JSON,Number,String,Math,Array,Object,Error,setInterval:()=>0});await wait();
  return {messages,requests,writes,spresenter,response,send:async m=>{spresenter.ui.onmessage(m);await wait();},state:()=>messages.filter(m=>m.type==='state').at(-1),stored:()=>stored};
}
function ok(body){return {ok:true,status:200,body:JSON.stringify(body)};}
const saved=()=>({devices:[{id,address:'http://192.168.1.20:48762',token,name:'Músicos'}],pc:null});
test('pairing stores credentials privately and never returns them to the UI',async()=>{
  const h=await harness();await h.send({type:'pair',address:'192.168.1.20',name:'Músicos',code:'091827'});
  assert.equal(h.state().devices.length,1);assert.equal(h.stored().devices[0].token,token);
  assert(!JSON.stringify(h.messages).includes(token));assert(!JSON.stringify(h.messages).includes('091827'));
  assert.equal(h.state().devices[0].status.playback.state,'loaded');
});
test('a public address or malformed pairing code never causes a network request',async()=>{
  const h=await harness();await h.send({type:'pair',address:'8.8.8.8',name:'Box',code:'123456'});
  assert.equal(h.requests.length,0);await h.send({type:'pair',address:'192.168.1.20',code:'123',name:'Box'});assert.equal(h.requests.length,0);
});
test('confirmed commands require a matching command receipt',async()=>{
  const h=await harness(saved());await h.send({type:'command',device:id,action:'reload_html',commandId:'synthetic-command-001'});
  assert(h.messages.some(m=>m.type==='notice'&&!m.error&&m.text.includes('confirmado')));
  assert.equal(h.requests.filter(r=>r.url.endsWith('/v1/commands')).length,1);
});
test('a missing acknowledgement is reported as unknown and never retried automatically',async()=>{
  let h;h=await harness(saved(),async r=>r.url.endsWith('/v1/commands')?{ok:false,status:503,body:'{}'}:h?h.response(r):r.url.endsWith('/v1/status')?ok({ok:true,schema:1,id,playback:{state:'loaded'}}):ok({sources:[]}));
  await h.send({type:'command',device:id,action:'clear_html_cache',commandId:'synthetic-command-002'});
  assert.equal(h.requests.filter(r=>r.url.endsWith('/v1/commands')).length,1);
  assert(h.state().devices[0].lastAction.includes('sem confirmação'));assert(h.messages.some(m=>m.type==='notice'&&m.error));
});
test('identity mismatch never updates the last successful response',async()=>{
  const h=await harness(saved(),async()=>ok({ok:true,schema:1,id:'different-device-123456'}));
  assert.equal(h.state().devices[0].lastSeen,0);assert.equal(h.state().devices[0].status,null);
  assert(h.state().devices[0].error.includes('identidade'));
});
test('central HTML catalog is read once on the PC and distributed without its API token',async()=>{
  let h;h=await harness(saved(),async r=>{
    if(r.url.endsWith('/advanced'))return ok({config:{outputs:[{type:'network',slug:'musicos',name:'Músicos'},{type:'network',slug:'retorno',enabled:false},{type:'ndi',slug:'ndi'}]}});
    return h?h.response(r):r.url.endsWith('/status')?ok({ok:true,schema:1,id,playback:{state:'loaded'}}):ok({sources:[]});
  });
  await h.send({type:'pc',link:'http://192.168.1.5:5050/warp/?token=spk_synthetic_api_token'});
  assert.equal(h.state().catalog.length,1);assert.equal(h.state().catalog[0].name,'Músicos');
  const push=h.requests.find(r=>r.url.includes('/commands')&&JSON.parse(r.body).action==='catalog');assert(push);
  assert(!push.body.includes('spk_'));assert(!JSON.stringify(h.messages).includes('spk_'));
  const api=h.requests.find(r=>r.url.endsWith('/advanced'));assert.equal(api.headers.Authorization,'Bearer spk_synthetic_api_token');
});
test('unknown actions and oversized NDI names are rejected before transmission',async()=>{
  const h=await harness(saved());const before=h.requests.length;
  await h.send({type:'command',device:id,action:'reboot',commandId:'synthetic-command-003'});
  await h.send({type:'command',device:id,action:'set_source',mode:'ndi',name:'x'.repeat(300),commandId:'synthetic-command-004'});
  assert.equal(h.requests.length,before);
});
test('scanning uses bounded concurrency, only hello, and no credentials',async()=>{
  let concurrent=0,max=0;
  const h=await harness(null,async r=>{concurrent++;max=Math.max(max,concurrent);await new Promise(setImmediate);concurrent--;return r.url.includes('192.168.1.20:')?ok({protocol:'s-retorno-control',schema:1}):{ok:false,status:404,body:'{}'};});
  await h.send({type:'scan',prefix:'192.168.1'});for(let i=0;i<10&&h.state().scanning;i++)await wait();
  assert.equal(h.requests.length,254);assert(max<=6);assert(h.requests.every(r=>r.url.endsWith('/v1/hello')&&!r.headers.Authorization));assert.equal(h.state().found.length,1);
});
test('saved invalid addresses and corrupt credentials are not contacted',async()=>{
  const h=await harness({devices:[{id,address:'http://8.8.8.8:48762',token},{id,address:'http://192.168.1.20:48762',token:'broken'}]});
  assert.equal(h.state().devices.length,0);assert.equal(h.requests.length,0);
});
test('daily power-off is neutral and reopening reuses identity with a fresh performance graph',async()=>{
  let active=true,uptime=900000,host;
  const h=await harness(saved(),async r=>{
    if(!active)return {ok:false,status:0,body:''};
    if(r.url.endsWith('/status'))return ok({ok:true,schema:1,id,uptime_ms:uptime,mode:'ndi',playback:{state:'waiting'},metrics:null,events:[]});
    return host?host.response(r):ok({sources:[]});
  });host=h;
  for(let day=0;day<12;day++){
    active=false;
    for(let i=0;i<3;i++)await h.send({type:'refresh'});
    assert.equal(h.state().devices[0].error,'');
    assert.equal(h.state().devices[0].status.events.length,0);
    active=true;uptime=1000;
    await h.send({type:'refresh'});
    const d=h.state().devices[0];assert.equal(d.id,id);assert.equal(d.failures,0);assert.equal(d.error,'');
    assert.equal(d.history.length,1);assert.equal(d.history[0].jitter,null);
    uptime=900000;await h.send({type:'refresh'});
  }
  assert.equal(h.requests.filter(r=>r.url.endsWith('/pair')).length,0);
  assert(!h.messages.some(m=>m.type==='notice'&&m.error));
});
test('authorization failures remain visible rather than being mistaken for a powered-off receiver',async()=>{
  const h=await harness(saved(),async()=>({ok:false,status:401,body:'{}'}));
  assert(h.state().devices[0].error.includes('Acesso recusado'));
});
