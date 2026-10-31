/* Spresenter logic process. No fetch, DOM, Node modules or native network sockets. */
(function () {
  "use strict";
  const KEY = "s-retorno-central-v1";
  let devices = [], pc = null, catalog = [], outputs = [], catalogError = "", catalogAt = 0;
  let ready = false, serial = 0, scanGeneration = 0, scanning = false, found = [], progress = 0;
  let lastPoll = 0, polling = false, mutation = Promise.resolve();
  const states = new Map(), busy = new Set();
  const localHost = host => {
    const p = host.split(".").map(Number);
    return p.length === 4 && p.every((n,i) => Number.isInteger(n) && n >= 0 && n < 256 && String(n) === host.split(".")[i])
      && (p[0] === 10 || p[0] === 192 && p[1] === 168 || p[0] === 172 && p[1] >= 16 && p[1] <= 31)
      && p[3] !== 0 && p[3] !== 255;
  };
  function address(raw) {
    const match = /^(?:http:\/\/)?([0-9.]+)(?::([0-9]{1,5}))?\/?$/.exec(String(raw || "").trim());
    if (!match || !localHost(match[1]) || Number(match[2] || 48762) < 1 || Number(match[2] || 48762) > 65535)
      throw new Error("Informe o endereço local mostrado no app, por exemplo 192.168.1.20:48762.");
    return "http://" + match[1] + ":" + Number(match[2] || 48762);
  }
  const nextId = () => "central-" + Date.now().toString(36) + "-" + (++serial).toString(36);
  const trimName = name => String(name || "Retorno").replace(/[\u0000-\u001f\u007f]/g, " ").trim().slice(0,48) || "Retorno";
  async function save() { await spresenter.storage.set(KEY, {devices, pc}); }
  const fault = (kind,message) => Object.assign(new Error(message),{kind});
  async function http(url, method, body, token, timeout) {
    if (!spresenter.net || typeof spresenter.net.httpRequest !== "function")
      throw fault("unsupported", "Esta versão do Spresenter não oferece a conexão de rede do plugin. Atualize o Spresenter.");
    let res;
    try {
      res = await spresenter.net.httpRequest({url, method: method || "GET", timeoutMs: timeout || 3500,
        headers: Object.assign({Accept:"application/json"}, body ? {"Content-Type":"application/json"} : {},
          token ? {Authorization:"Bearer " + token} : {}), body: body ? JSON.stringify(body) : undefined});
    } catch (e) {
      if (/permission|scope|net:connect/i.test(String(e && e.message || "")))
        throw fault("authorization", "Permita o acesso à rede (net:connect) nas configurações deste plugin.");
      throw fault("unreachable","Sem resposta neste endereço. Confira o IP, a porta e a conexão de rede.");
    }
    if (!res || !res.ok) {
      if (res && (res.status === 401 || res.status === 403)) throw fault("authorization","Acesso recusado. Confira o código ou faça um novo pareamento.");
      if (res && res.status === 409) throw new Error("A ação não corresponde ao modo atual do aparelho. Atualize o estado.");
      if (!res || !res.status || res.status >= 500) throw fault("unreachable","Aparelho desligado ou fora da rede.");
      throw fault("protocol","O pedido não foi confirmado. Atualize o estado antes de tentar novamente.");
    }
    if (typeof res.body !== "string" || res.body.length > 1048576) throw fault("protocol","Resposta inválida.");
    try { return JSON.parse(res.body); } catch (_) { throw fault("protocol","Resposta inválida."); }
  }
  function publish() {
    spresenter.ui.postMessage({type:"state", ready, devices:devices.map(d => {
      const s = states.get(d.id) || {};
      return {id:d.id,address:d.address,name:d.name,status:s.status || null,lastSeen:s.lastSeen || 0,
        failures:s.failures || 0,error:s.error || "",history:s.history || [],sources:s.sources || [],busy:busy.has(d.id),
        lastAction:s.lastAction || "",durable:d.durable !== false};
    }), catalog, outputs, catalogError, pcConfigured:!!pc, scanning, found, progress});
  }
  function note(text, error) { spresenter.ui.postMessage({type:"notice",text,error:!!error}); }
  function operation(m, phase) {
    if (typeof m.requestId === "string" && /^[A-Za-z0-9-]{1,80}$/.test(m.requestId))
      spresenter.ui.postMessage({type:"operation",requestId:m.requestId,phase});
  }
  function getDevice(id) {
    const d = devices.find(x => x.id === id); if (!d) throw new Error("Aparelho não encontrado."); return d;
  }
  async function parallel(items, workers, fn) {
    let index = 0;
    await Promise.all(Array.from({length:Math.min(workers,items.length)}, async() => {
      while (index < items.length) { const item = items[index++]; await fn(item); }
    }));
  }
  async function readDevice(d, withSources) {
    if (busy.has(d.id)) return;
    busy.add(d.id);
    const s = states.get(d.id) || {history:[],failures:0}; states.set(d.id,s);
    try {
      const start = Date.now();
      const status = await http(d.address + "/v1/status", "GET", null, d.token);
      if (status.id !== d.id || status.schema !== 1 || status.ok !== true) throw new Error("A identidade do aparelho mudou. Faça um novo pareamento.");
      const now = Date.now();
      const resumed = !s.lastSeen || now-s.lastSeen >= 20000 || (s.status && typeof status.uptime_ms==="number"
        && typeof s.status.uptime_ms==="number" && status.uptime_ms<s.status.uptime_ms);
      if (resumed) s.history=[];
      const ping = Math.max(0,now-start), previous=s.history[s.history.length-1];
      s.history.push({at:Date.now(),ping,jitter:previous?Math.abs(previous.ping-ping):null,metrics:status.metrics,
        fps:status.playback && status.playback.capture_fps});
      if (s.history.length>60) s.history.shift();
      s.status=status;s.lastSeen=Date.now();s.failures=0;s.error="";
      if (withSources || !s.sources) {
        const result = await http(d.address+"/v1/sources","GET",null,d.token);
        s.sources=Array.isArray(result.sources)?result.sources.slice(0,96):[];
      }
      try { await syncCatalog(d,s); } catch(e) { s.lastAction=e.message; }
    } catch(e) {
      s.failures=(s.failures||0)+1;
      // A powered-off receiver and an unreachable receiver cannot be distinguished here.
      // Missing replies are availability state, never persisted failure reports.
      s.error=e.kind==="unreachable"?"":e.message;
    }
    finally { busy.delete(d.id); }
  }
  function parseEditorLink(link) {
    const m=/^(https?:\/\/([0-9.]+)(?::([0-9]{1,5}))?)\/warp\/?\?token=(spk_[A-Za-z0-9_-]{8,512})$/.exec(String(link||"").trim());
    if (!m || !localHost(m[2]) || m[3] && (Number(m[3])<1 || Number(m[3])>65535))
      throw new Error("Cole o link local completo de Copiar link com token do Editor remoto do Spresenter.");
    return {origin:m[1],token:m[4]};
  }
  async function refreshCatalog(force) {
    if (!pc || !force && Date.now()-catalogAt<60000) return;
    catalogAt=Date.now();
    try {
      const result = await http(pc.origin+"/api/v1/advanced","GET",null,pc.token,5000);
      if (!result.config || !Array.isArray(result.config.outputs)) throw new Error("O Spresenter não forneceu o catálogo HTML esperado.");
      const items=result.config.outputs.filter(o=>o && o.type==="network" && o.enabled!==false
        && typeof o.slug==="string" && /^[A-Za-z0-9_-]{1,120}$/.test(o.slug)).slice(0,48).map(o=>({mode:"html",
          name:trimName(o.name||o.slug),url:pc.origin+"/output/"+o.slug}));
      catalog=[];
      for (const item of items) { if (JSON.stringify(catalog.concat(item)).length*3>6500) break; catalog.push(item); }
      catalogError="";
    } catch(e) { catalogError=e.message; }
  }
  async function syncCatalog(d,s) {
    if (!pc || catalogError) return;
    const value=JSON.stringify(catalog);
    if (s.catalogValue===value) return;
    const result=await http(d.address+"/v1/commands","POST",{id:nextId(),action:"catalog",origin:pc.origin,sources:catalog},d.token);
    if (result.ok!==true || result.applied!==true) throw new Error("O catálogo não foi confirmado pelo app.");
    s.catalogValue=value;
  }
  async function pollAll(force) {
    if (!ready || polling || !force && Date.now()-lastPoll<4500) return;
    polling=true;lastPoll=Date.now();
    try {
      await refreshCatalog(false);
      await parallel(devices.slice(),3,d=>readDevice(d,!!force));
      if (force) try { outputs=(await spresenter.outputs.list()).map(o=>({index:o.index,name:o.name})); } catch(_){outputs=[];}
    } finally { polling=false;publish(); }
  }
  async function scan(prefix) {
    if (!/^\d{1,3}\.\d{1,3}\.\d{1,3}$/.test(prefix) || !localHost(prefix+".1"))
      throw new Error("Informe a faixa local, por exemplo 192.168.1.");
    const generation=++scanGeneration;scanning=true;found=[];progress=0;publish();
    try {
      await parallel(Array.from({length:254},(_,i)=>prefix+"."+(i+1)),6,async host=> {
        if (generation!==scanGeneration) return;
        try {
          const result=await http("http://"+host+":48762/v1/hello","GET",null,null,650);
          if (generation===scanGeneration && result.protocol==="s-retorno-control" && result.schema===1)
            found.push({address:"http://"+host+":48762"});
        } catch(_) { }
        if (generation===scanGeneration) { progress++;if (progress%12===0) publish(); }
      });
    } finally { if (generation===scanGeneration){scanning=false;publish();} }
  }
  async function command(m) {
    const d=getDevice(m.device), s=states.get(d.id)||{history:[]};
    if (busy.has(d.id)) throw new Error("Aguarde a operação atual neste aparelho.");
    const allowed=["set_source","reload_html","reconnect_ndi","clear_html_cache","refresh_sources","identify","rename"];
    if (!allowed.includes(m.action)) throw new Error("Ação inválida.");
    const request={id:String(m.commandId||""),action:m.action};
    if (!/^[A-Za-z0-9-]{12,80}$/.test(request.id)) throw new Error("Identificador inválido.");
    if (m.action==="set_source") {
      if (m.mode==="html" && typeof m.url==="string" && /^https?:\/\//.test(m.url) && m.url.length<=2048)
        Object.assign(request,{mode:"html",url:m.url});
      else if (m.mode==="ndi" && typeof m.name==="string" && m.name.length>0 && m.name.length<=256)
        Object.assign(request,{mode:"ndi",name:m.name});
      else throw new Error("Fonte inválida.");
    }
    if (m.action==="rename") request.name=trimName(m.name);
    busy.add(d.id);publish();
    try {
      const res=await http(d.address+"/v1/commands","POST",request,d.token);
      if (res.ok!==true || res.applied!==true || res.command_id!==request.id) throw new Error("O comando não foi confirmado.");
      if (m.action==="rename") { d.name=request.name;await save(); }
      s.lastAction="Comando confirmado pelo app";states.set(d.id,s);note(s.lastAction);
    } catch(e) { s.lastAction="Resultado sem confirmação. Confira o estado antes de repetir.";states.set(d.id,s);throw e; }
    finally { busy.delete(d.id);publish(); }
    await readDevice(d,true);publish();
  }
  spresenter.ui.onmessage=function(raw) {
    const m=raw && typeof raw==="object" ? raw : {};
    if (m.type==="cancel-scan") {scanGeneration++;scanning=false;publish();return;}
    if (m.type==="tick") {pollAll(false).catch(()=>{});return;}
    if (m.type==="init") {publish();pollAll(true).catch(()=>{});return;}
    operation(m,"started");
    if (m.type==="scan") {
      scan(String(m.prefix||"").trim()).catch(e=>note(e.message,true)).finally(()=>operation(m,"finished"));return;
    }
    mutation=mutation.then(async()=> {
      if (!ready) throw new Error("Aguarde o carregamento da central.");
      if (m.type==="pair") {
        if (devices.length>=32) throw new Error("Limite de 32 aparelhos nesta central.");
        const base=address(m.address);
        if (!/^\d{6}$/.test(String(m.code))) throw new Error("Informe os seis números mostrados no app.");
        const result=await http(base+"/v1/pair","POST",{code:String(m.code),name:trimName(m.name)},null);
        if (result.ok!==true || !/^[a-f0-9]{64}$/.test(result.token) || !/^[a-zA-Z0-9-]{16,64}$/.test(result.id)) throw new Error("Pareamento inválido.");
        devices=devices.filter(d=>d.id!==result.id && d.address!==base);
        const d={id:result.id,address:base,token:result.token,name:trimName(result.name),durable:result.durable};
        devices.push(d);await save();note("Retorno pareado. Você já pode escolher a fonte.");
        await readDevice(d,true);
      } else if (m.type==="pc") {
        const candidate=parseEditorLink(m.link), old=pc;pc=candidate;
        await refreshCatalog(true);
        if(catalogError){pc=old;throw new Error(catalogError);}
        await save();note("Catálogo do Spresenter conectado.");
        await pollAll(true);
      } else if (m.type==="forget-pc") {pc=null;catalog=[];catalogError="";await save();}
      else if (m.type==="remove") {getDevice(m.device);devices=devices.filter(d=>d.id!==m.device);states.delete(m.device);await save();}
      else if (m.type==="relocate") {
        const d=getDevice(m.device),base=address(m.address);
        const status=await http(base+"/v1/status","GET",null,d.token);
        if(status.id!==d.id)throw new Error("O endereço não pertence a este vínculo.");
        d.address=base;await save();await readDevice(d,true);
      } else if (m.type==="command") await command(m);
      else if (m.type==="refresh") {await refreshCatalog(true);await pollAll(true);}
      publish();
    }).catch(e=>{note(e && e.message || "Não foi possível concluir a ação.",true);publish();})
      .finally(()=>operation(m,"finished"));
  };
  (async()=> {
    try {
      const saved=await spresenter.storage.get(KEY);
      if(saved && Array.isArray(saved.devices)) devices=saved.devices.slice(0,32).filter(d=> {
        try{return d && address(d.address)===d.address && /^[a-f0-9]{64}$/.test(d.token) && /^[a-zA-Z0-9-]{16,64}$/.test(d.id);}catch(_){return false;}
      });
      if(saved && saved.pc && typeof saved.pc.origin==="string" && typeof saved.pc.token==="string") {
        try {pc=parseEditorLink(saved.pc.origin+"/warp/?token="+saved.pc.token);}catch(_){pc=null;}
      }
    } catch (_) { note("Não foi possível ler os vínculos salvos do plugin. Reabra a central para tentar novamente.",true); }
    finally {ready=true;publish();pollAll(true).catch(()=>{});}
  })();
  if(typeof setInterval==="function")setInterval(()=>pollAll(false).catch(()=>{}),5000);
})();
