(function(){
  "use strict";
  const $=id=>document.getElementById(id), send=data=>window.parent.postMessage({__spresenter:true,data},"*");
  const esc=value=>String(value==null?"":value).replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
  let state={devices:[],catalog:[],outputs:[]},selected=null,action=null,noticeTimer;
  const pending=new Map();
  const uid=()=>typeof crypto.randomUUID==="function"?crypto.randomUUID():"command-"+Date.now().toString(36)+"-"+Math.random().toString(36).slice(2);
  const num=(v,d=1)=>typeof v==="number"&&Number.isFinite(v)?v.toFixed(d):"—";
  const rate=(bytes,ms)=>typeof bytes==="number"&&ms>0?bytes*.008/ms:null;
  const online=d=>d.lastSeen&&Date.now()-d.lastSeen<20000&&d.failures<3;
  const video=d=>d.status&&d.status.playback||{};
  const attention=d=>!!d.error||(online(d)&&["error","unavailable"].includes(video(d).state));
  const playbackText=p=>({loaded:"HTML carregado",loading:"HTML carregando",error:"Falha no HTML",inactive:"Reprodução inativa",unavailable:"Motor indisponível",receiving:"NDI recebendo vídeo",waiting:"NDI aguardando vídeo",waiting_source:"Aguardando fonte HTML"}[p.state]||"Estado ainda não recebido");
  const ago=time=>time?Math.max(0,Math.floor((Date.now()-time)/1000))+" s atrás":"Ainda sem resposta";
  function notice(text,error){
    for(const id of ['notice','pair-notice']){const n=$(id);n.textContent=text;n.className=error?'error':'';n.hidden=false;}
    clearTimeout(noticeTimer);noticeTimer=setTimeout(()=>$("notice").hidden=true,9000);
  }
  function request(data,button,text){
    const requestId=uid();
    if(button){button.disabled=true;button.setAttribute('aria-busy','true');}
    notice(text,false);
    const finish=()=>{if(button){button.disabled=false;button.removeAttribute('aria-busy');}pending.delete(requestId);};
    const timeout=setTimeout(()=>{
      finish();notice('O plugin não respondeu. Reabra a Central de Retornos e confira se o plugin está habilitado no Spresenter.',true);
    },8000);
    pending.set(requestId,{finish,timeout});send({...data,requestId});
  }
  function render(){
    $("connected").textContent=state.devices.filter(online).length;$("total").textContent=state.devices.length;$("attention").textContent=state.devices.filter(attention).length;
    $("devices").innerHTML=state.devices.length?state.devices.map(d=>{
      const live=online(d),s=d.status||{},m=live&&s.metrics||{},p=video(d),h=d.history||[],last=h[h.length-1]||{};
      return '<article class="device"><div class="device-head"><div><h3>'+esc(d.name)+'</h3><small>'+esc(d.address.replace('http://',''))+'</small></div><span class="badge '+(!live?'off':attention(d)?'warn':'')+'">'+(!live?'Sem comunicação':'App responde')+'</span></div><div class="source"><b>'+esc(s.mode||'fonte')+'</b>'+esc(s.source||'Escolha uma fonte')+'</div><div class="metrics"><div><strong>'+num(live?last.ping:null,0)+' ms</strong><span>Resposta do app</span></div><div><strong>'+num(rate(m.rx_bytes,m.sample_window_ms))+'</strong><span>Recepção · Mbps</span></div><div><strong>'+num(m.memory_pss_kib==null?null:m.memory_pss_kib/1024)+' MB</strong><span>Memória do processo</span></div></div><div class="freshness">'+(live?esc(playbackText(p)):'Último estado sem atualização')+' · '+esc(ago(d.lastSeen))+'</div>'+(!d.durable?'<p class="warning">Vínculo válido somente nesta sessão do app.</p>':'')+'<div class="card-actions"><button data-device="'+esc(d.id)+'" data-do="source" '+(!live||d.busy?'disabled':'')+'>Escolher fonte</button><button class="quiet" data-device="'+esc(d.id)+'" data-do="details">Detalhes e manutenção</button></div></article>';
    }).join(''):'<div class="empty">Vincule o primeiro S-Retorno para acompanhar a conexão e escolher sua fonte.</div>';
    $("catalog-state").textContent=state.pcConfigured?state.catalogError?'Verificar conexão':state.catalog.length+' saídas HTML':'Configurar catálogo HTML';
    $("catalog-list").innerHTML=(state.catalogError?'<p class="warning">'+esc(state.catalogError)+'</p>':'')+'<div class="chips">'+state.catalog.map(s=>'<span class="chip">'+esc(s.name)+'</span>').join('')+'</div>'+(state.outputs.length?'<p class="muted">Saídas lógicas do Spresenter: '+state.outputs.map(o=>esc(o.name)).join(', ')+'</p>':'');
    $("forget-pc").hidden=!state.pcConfigured;$("scan").disabled=!!state.scanning;$("cancel-scan").hidden=!state.scanning;
    $("scan-result").innerHTML=(state.scanning?'Consultando '+state.progress+' de 254 endereços…':state.progress?'Busca concluída. '+((state.found||[]).length?'':'Nenhum app respondeu nesta faixa. Ative a Central de Retornos na box e confira a faixa de IP. '):'')+(state.found||[]).map(d=>'<button type="button" class="quiet" data-found="'+esc(d.address)+'">'+esc(d.address.replace('http://',''))+'</button>').join('');
    if($("detail-dialog").open&&selected)renderDetail();
  }
  function spark(values,color){const valid=values.filter(x=>typeof x==='number'&&Number.isFinite(x));if(!valid.length)return '<p class="muted">Aguardando amostras</p>';const max=Math.max(...valid,1);let paths=[],points=[];values.forEach((v,i)=>{if(typeof v!=='number'||!Number.isFinite(v)){if(points.length)paths.push(points.join(' '));points=[];return;}points.push((i*100/Math.max(1,values.length-1)).toFixed(2)+','+(74-v/max*65).toFixed(2));});if(points.length)paths.push(points.join(' '));return '<svg class="spark" viewBox="0 0 100 80" preserveAspectRatio="none" aria-hidden="true">'+paths.map(p=>'<polyline fill="none" stroke="'+color+'" stroke-width="1.4" points="'+p+'"/>').join('')+'</svg>';}
  function renderDetail(){
    const d=state.devices.find(x=>x.id===selected);if(!d){$("detail-dialog").close();return;}
    const s=d.status||{},m=s.metrics||{},p=video(d),h=d.history||[],last=h[h.length-1]||{},disabled=!online(d)||d.busy?'disabled':'';
    const kinds={paired:'Pareamento realizado',java_crash:'Falha Java confirmada',native_crash:'Falha nativa confirmada',anr:'App sem resposta informado pelo Android',html_renderer_failure:'Falha no motor HTML',ndi_initialization_failure:'Falha ao iniciar o receptor NDI',manual_video:'Relato: vídeo',manual_audio:'Relato: áudio',manual_connection:'Relato: conexão',manual_startup:'Relato: abertura',manual_other:'Relato: outro problema'};
    $("detail").innerHTML='<h2>'+esc(d.name)+'</h2><p class="muted">'+esc(d.address)+' · S-Retorno '+esc(s.version||'—')+'</p><p>'+esc(online(d)?playbackText(p):'Sem comunicação com o app')+'</p><div class="metrics"><div><strong>'+num(last.ping,0)+' ms</strong><span>Última resposta</span></div><div><strong>'+num(last.jitter,0)+' ms</strong><span>Variação entre respostas</span></div><div><strong>'+num(m.cpu_percent)+'%</strong><span>CPU do processo</span></div></div><p class="chart-label">Memória do processo · últimas '+h.length+' amostras</p>'+spark(h.map(x=>x.metrics&&x.metrics.memory_pss_kib!=null?x.metrics.memory_pss_kib/1024:null),'#65bcff')+'<p class="chart-label">Recepção de rede · Mbps</p>'+spark(h.map(x=>x.metrics?rate(x.metrics.rx_bytes,x.metrics.sample_window_ms):null),'#6bddb1')+'<p class="chart-label">Resposta do app · ms</p>'+spark(h.map(x=>x.ping),'#f8cb7c')+'<table><tbody><tr><th>Tráfego enviado</th><td>'+num(rate(m.tx_bytes,m.sample_window_ms),3)+' Mbps</td></tr><tr><th>Tempo do app aberto</th><td>'+num(s.uptime_ms==null?null:s.uptime_ms/60000,0)+' min</td></tr>'+(s.mode==='ndi'?'<tr><th>Captura NDI medida</th><td>'+num(p.capture_fps)+' fps</td></tr><tr><th>Formato da fonte</th><td>'+esc(p.width||'—')+' × '+esc(p.height||'—')+' · '+num(p.source_fps)+' fps</td></tr><tr><th>Quadros descartados / fila</th><td>'+esc(p.dropped_frames==null?'—':p.dropped_frames)+' / '+esc(p.queue_frames==null?'—':p.queue_frames)+'</td></tr>':'')+'</tbody></table><p class="freshness">Última resposta: '+esc(ago(d.lastSeen))+'. Métricas acima correspondem à última amostra recebida.</p>'+(d.error?'<p class="warning">'+esc(d.error)+'</p>':!online(d)?'<p class="muted">Aparelho desligado ou fora da rede. Esta ausência não conta como falha.</p>':'')+'<h3>Manutenção</h3><div class="card-actions"><button data-do="identify" data-device="'+esc(d.id)+'" '+disabled+' class="quiet">Identificar tela</button><button data-do="refresh_sources" data-device="'+esc(d.id)+'" '+disabled+' class="quiet">Buscar fontes</button><button data-do="'+(s.mode==='ndi'?'reconnect_ndi':'reload_html')+'" data-device="'+esc(d.id)+'" '+disabled+' class="quiet">'+(s.mode==='ndi'?'Reconectar NDI':'Recarregar HTML')+'</button>'+(s.mode==='html'?'<button data-do="clear_html_cache" data-device="'+esc(d.id)+'" '+disabled+' class="quiet">Limpar cache HTML</button>':'')+'<button data-do="rename" data-device="'+esc(d.id)+'" '+disabled+' class="quiet">Renomear</button><button data-do="relocate" data-device="'+esc(d.id)+'" class="quiet">Atualizar endereço</button><button data-do="remove" data-device="'+esc(d.id)+'" class="quiet">Remover vínculo do PC</button></div><p class="freshness">'+esc(d.lastAction||'')+'</p><h3>Ocorrências locais</h3><ul class="event-list">'+(s.events||[]).filter(e=>!['session_interrupted','html_error','ndi_signal_error'].includes(e.kind)).slice().reverse().map(e=>'<li>'+esc(new Date(e.at).toLocaleString('pt-BR'))+' · '+esc(kinds[e.kind]||(String(e.kind).startsWith('maintenance_')?'Manutenção solicitada':e.kind))+'</li>').join('')+'</ul>';
  }
  function openAction(d,kind){
    selected=d.id;action=kind;let title='',body='';
    if(kind==='source'){
      const sources=state.catalog.concat((d.sources||[]).filter(x=>!state.catalog.some(y=>y.mode===x.mode&&y.url===x.url&&y.name===x.name)));
      title='Escolher fonte para '+d.name;body='<label>Fontes disponíveis<select id="source-choice"><option value="">Selecione uma fonte</option>'+sources.map((s,i)=>'<option value="'+i+'">'+esc(s.mode.toUpperCase()+' · '+s.name)+'</option>').join('')+'</select></label><p class="muted">Ou informe uma fonte:</p><label>Modo<select id="manual-mode"><option value="html">HTML</option><option value="ndi">NDI</option></select></label><label>Endereço HTML completo ou nome exato da fonte NDI<input id="manual-source" maxlength="2048"></label>';action={kind,sources};
    }else if(kind==='rename'){title='Renomear retorno';body='<label>Nome<input id="new-value" maxlength="48" value="'+esc(d.name)+'" required></label>';}
    else if(kind==='relocate'){title='Atualizar endereço';body='<label>Novo endereço local<input id="new-value" value="'+esc(d.address)+'" required></label><p>O vínculo só será atualizado se o app confirmar a mesma identidade.</p>';}
    else if(kind==='remove'){title='Remover vínculo do PC';body='<p>Este retorno sairá da central neste computador. Para revogar também a credencial no app, use <b>Desativar e desvincular</b> na TV.</p>';}
    else {title=kind==='clear_html_cache'?'Limpar cache HTML':kind==='reload_html'?'Recarregar HTML':'Reconectar NDI';body='<p>A reprodução em <b>'+esc(d.name)+'</b> será interrompida brevemente. '+(kind==='clear_html_cache'?'Fontes salvas e pareamento serão preservados.':'')+'</p>';}
    $("action-title").textContent=title;$("action-body").innerHTML=body;$("action-dialog").showModal();
  }
  document.addEventListener('click',e=>{
    const close=e.target.closest('[data-close]');if(close){$(close.dataset.close).close();return;}
    const found=e.target.closest('[data-found]');if(found){$("pair-address").value=found.dataset.found;$("pair-code").focus();return;}
    const b=e.target.closest('[data-do]');if(!b)return;const d=state.devices.find(x=>x.id===b.dataset.device);if(!d)return;
    if(b.dataset.do==='details'){selected=d.id;renderDetail();$("detail-dialog").showModal();}
    else if(['identify','refresh_sources'].includes(b.dataset.do))send({type:'command',device:d.id,action:b.dataset.do,commandId:uid()});
    else openAction(d,b.dataset.do);
  });
  $("add").onclick=()=>$("pair-dialog").showModal();$("refresh").onclick=()=>request({type:'refresh'},$("refresh"),'Atualizando os retornos…');
  $("pair-form").onsubmit=e=>{e.preventDefault();request({type:'pair',name:$("pair-name").value,address:$("pair-address").value,code:$("pair-code").value},$("pair-submit"),'Tentando vincular o retorno em '+$("pair-address").value+'…');};
  $("pc-form").onsubmit=e=>{e.preventDefault();const link=$("pc-link").value;$("pc-link").value='';request({type:'pc',link},$("pc-submit"),'Consultando as saídas HTML do Spresenter…');};
  $("forget-pc").onclick=()=>send({type:'forget-pc'});$("scan").onclick=()=>request({type:'scan',prefix:$("scan-prefix").value},$("scan"),'Iniciando busca de apps na rede…');$("cancel-scan").onclick=()=>send({type:'cancel-scan'});
  $("pair-dialog").addEventListener('close',()=>send({type:'cancel-scan'}));
  $("action-form").onsubmit=e=>{
    e.preventDefault();const d=state.devices.find(x=>x.id===selected);if(!d)return;
    let msg={type:'command',device:d.id,action:typeof action==='string'?action:action.kind,commandId:uid()};
    if(typeof action==='object'&&action.kind==='source'){
      const i=$("source-choice").value,s=i!==''?action.sources[Number(i)]:{mode:$("manual-mode").value,name:$("manual-source").value,url:$("manual-source").value};
      if(!s||!s.name){notice('Selecione ou informe uma fonte.',true);return;}Object.assign(msg,{action:'set_source',mode:s.mode,name:s.name,url:s.url});
    }else if(action==='rename')msg.name=$("new-value").value;
    else if(action==='relocate')msg={type:'relocate',device:d.id,address:$("new-value").value};
    else if(action==='remove')msg={type:'remove',device:d.id};
    send(msg);$("action-dialog").close();
  };
  window.addEventListener('message',e=>{
    // Follow the official SDK envelope. Native host relays may have a null source.
    if(!e.data||e.data.__spresenter!==true)return;
    const m=e.data.data;if(!m)return;
    if(m.type==='state'&&Array.isArray(m.devices)&&Array.isArray(m.catalog)&&Array.isArray(m.outputs)){state=m;render();}
    else if(m.type==='notice')notice(m.text,m.error);
    else if(m.type==='operation'){
      const p=pending.get(m.requestId);if(!p)return;clearTimeout(p.timeout);
      if(m.phase==='finished')p.finish();
      else p.timeout=setTimeout(()=>{p.finish();notice('A operação ainda não foi confirmada. Atualize o estado antes de repetir.',true);},90000);
    }
  });
  send({type:'init'});setInterval(()=>{if(!document.hidden){send({type:'tick'});render();}},5000);
})();
