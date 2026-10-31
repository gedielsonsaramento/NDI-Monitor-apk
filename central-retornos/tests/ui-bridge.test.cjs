const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../src/ui/ui.js'),'utf8');
function ui(){
  const elements=new Map(),messages=[],timers=new Map();let counter=0,receive;
  const get=id=>{
    if(!elements.has(id))elements.set(id,{value:'',textContent:'',innerHTML:'',hidden:false,disabled:false,open:false,
      setAttribute(){},removeAttribute(){},addEventListener(){},showModal(){this.open=true;},close(){this.open=false;}});
    return elements.get(id);
  };
  const parent={postMessage:m=>messages.push(m.data)};
  vm.runInNewContext(source,{document:{getElementById:get,addEventListener(){},hidden:false},
    window:{parent,addEventListener:(name,handler)=>{if(name==='message')receive=handler;}},
    crypto:{randomUUID:()=>`test-request-${++counter}`},setInterval(){},
    setTimeout:fn=>{const id=++counter;timers.set(id,fn);return id;},clearTimeout:id=>timers.delete(id),
    Map,Set,Date,Math,Number,String,Array,Object});
  return {get,messages,timers,receive:(data,source=null)=>receive({source,data:{__spresenter:true,data}}),
    submit:id=>get(id).onsubmit({preventDefault(){}})};
}
test('native relay with null source updates the UI according to the official SDK envelope',()=>{
  const h=ui();h.receive({type:'state',devices:[],catalog:[{name:'Músicos'}],outputs:[],pcConfigured:true,found:[]});
  assert.equal(h.get('catalog-state').textContent,'1 saídas HTML');
  h.receive({type:'notice',text:'Acesso recusado. Confira o código.',error:true});
  assert.equal(h.get('pair-notice').hidden,false);assert.match(h.get('pair-notice').textContent,/código/);
});
test('pairing shows an immediate attempt and accepts start/finish acknowledgement',()=>{
  const h=ui();h.get('pair-address').value='192.168.2.20:48762';h.get('pair-code').value='123456';
  h.submit('pair-form');const request=h.messages.at(-1);
  assert.equal(request.type,'pair');assert.equal(request.code,'123456');
  assert.match(h.get('pair-notice').textContent,/Tentando vincular/);assert.equal(h.get('pair-submit').disabled,true);
  h.receive({type:'operation',requestId:request.requestId,phase:'started'});
  h.receive({type:'notice',text:'Retorno pareado.',error:false});
  h.receive({type:'operation',requestId:request.requestId,phase:'finished'});
  assert.equal(h.get('pair-submit').disabled,false);assert.match(h.get('pair-notice').textContent,/pareado/);
});
test('catalog and discovery actions show progress and an absent logic process never stays silent',()=>{
  const h=ui();h.get('pc-link').value='http://192.168.2.5:5050/warp/?token=spk_test_only';h.submit('pc-form');
  assert.equal(h.messages.at(-1).type,'pc');assert.equal(h.get('pc-link').value,'');
  assert.match(h.get('notice').textContent,/Consultando/);
  h.get('scan-prefix').value='192.168.2';h.get('scan').onclick();
  assert.equal(h.messages.at(-1).prefix,'192.168.2');assert.match(h.get('pair-notice').textContent,/Iniciando busca/);
  for(const fn of [...h.timers.values()])fn();
  assert.match(h.get('pair-notice').textContent,/plugin não respondeu/);assert.equal(h.get('scan').disabled,false);
});
