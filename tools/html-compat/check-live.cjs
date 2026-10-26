'use strict';

// Local integration fixture, not an S-Present server or a physical Chrome 52 test.
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const acorn = require('acorn');

async function main() {
  const [directory, originalScript] = process.argv.slice(2);
  if (!directory || !originalScript) throw new Error('Usage: node check-live.cjs ADAPTED_DIRECTORY ORIGINAL_SCRIPT');
  const {JSDOM, ResourceLoader, VirtualConsole} = require('jsdom');
  const original = fs.readFileSync(originalScript, 'utf8');
  assert.throws(() => acorn.parse(original, {ecmaVersion: 2016}), SyntaxError);
  acorn.parse(fs.readFileSync(path.join(directory, 'spresenter-chrome52.js'), 'utf8'), {ecmaVersion: 2016});
  let lyric = 'Primeira letra do teste';
  const sockets = new Set(), clients = new Set(), errors = [], requests = [];
  const theme = {title: 'Local compatibility fixture', css: {background: '#000000'}, elements: [
    {id: 'main-letra', type: 'TEXT', text: '{cifra}', assetTypeVisibility: ['music'],
      css: {position: 'absolute', top: '10%', left: '5%', width: '90%', height: '50%',
        color: '#ffffff', fontSize: '60px', display: 'flex', alignItems: 'center'}},
    {id: 'visitor', type: 'TEXT', text: 'Visitante inicial',
      css: {position: 'absolute', top: '80%', left: '5%', width: '90%', height: '10%', color: '#ffffff', fontSize: '40px'}}
  ]};
  const layer = () => [{asset: {guid: 'local-music', type: 'music', title: 'Teste local'},
    props: {text: lyric, nextVerse: '', secao: 'Teste', currentIndex: 1, totalCount: 2}}];
  const server = http.createServer((req, res) => {
    const url = new URL(req.url, 'http://localhost'); requests.push(url.pathname);
    res.setHeader('Cache-Control', 'no-store');
    const send = data => {res.setHeader('Content-Type', 'application/json'); res.end(JSON.stringify(data));};
    if (url.pathname === '/slide/') {
      res.setHeader('Content-Type', 'text/html; charset=utf-8');
      return res.end('<!doctype html><html><head><meta charset="utf-8"><link rel="stylesheet" href="/main.css"><script defer src="/main.js"></script></head><body><div id="root"></div></body></html>');
    }
    if (url.pathname === '/main.js') {res.setHeader('Content-Type', 'application/javascript');return res.end(fs.readFileSync(path.join(directory,'spresenter-chrome52.js')));}
    if (url.pathname === '/main.css') {res.setHeader('Content-Type', 'text/css');return res.end(fs.readFileSync(path.join(directory,'spresenter-chrome52.css')));}
    if (url.pathname === '/stage/theme') return send({themeRef: 'local-theme'});
    if (url.pathname === '/assets/local-theme/manifest.json') return send(theme);
    if (url.pathname === '/live/0') return send(layer());
    if (url.pathname === '/live/0/state') return send([{show: true}]);
    if (url.pathname === '/live/0/master') return send({opacity: 1});
    if (url.pathname === '/live/0/elements') return send({});
    if (url.pathname === '/stage/announcement') return send({announcement: null});
    if (url.pathname === '/timer') return send({running: false, remainingMs: 0});
    if (url.pathname === '/media/state') return send({playing: false, loop: false, time: 0});
    if (url.pathname === '/media/layers') return send([]);
    if (url.pathname === '/live/stage/testpattern') return send({on: false});
    res.statusCode = 404; res.end();
  });
  server.on('connection', socket => {sockets.add(socket);socket.on('close',()=>sockets.delete(socket));});
  server.on('upgrade', (req, socket) => {
    const accept = crypto.createHash('sha1').update(req.headers['sec-websocket-key']+'258EAFA5-E914-47DA-95CA-C5AB0DC85B11').digest('base64');
    socket.write('HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: '+accept+'\r\n\r\n');
    clients.add(socket);socket.on('close',()=>clients.delete(socket));socket.on('error',()=>{});
  });
  const emit = (key,data) => {
    const payload=Buffer.from(JSON.stringify({key,data}));
    const head=payload.length<126?Buffer.from([0x81,payload.length]):Buffer.from([0x81,126,payload.length>>>8,payload.length&255]);
    for(const socket of clients)socket.write(Buffer.concat([head,payload]));
  };
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const base='http://127.0.0.1:'+server.address().port;
  let dom;
  try {
    const console = new VirtualConsole();
    console.on('jsdomError',error=>errors.push(error.message));
    console.on('error',error=>errors.push(error?.message || String(error)));
    class LocalResources extends ResourceLoader {
      fetch(url,options) {return url.startsWith(base)?super.fetch(url,options):null;}
    }
    const fetchPolyfill=fs.readFileSync(require.resolve('whatwg-fetch/dist/fetch.umd.js'),'utf8');
    dom=await JSDOM.fromURL(base+'/slide/?output=stage&outputId=fixture&stageThemeGuid=local-theme&id=test',{
      resources:new LocalResources(),runScripts:'dangerously',pretendToBeVisual:true,virtualConsole:console,
      beforeParse(window) {
        window.Object.entries=undefined;window.Object.values=undefined;
        window.String.prototype.padStart=undefined;window.String.prototype.trimStart=undefined;window.String.prototype.trimEnd=undefined;
        window.Promise.prototype.finally=undefined;
        window.ResizeObserver=undefined;window.AbortController=undefined;window.AbortSignal=undefined;window.URLSearchParams=undefined;
        window.eval(fetchPolyfill);
      }
    });
    const document=dom.window.document;
    const wait=async predicate=>{
      const until=Date.now()+15000;
      while(!predicate()) {
        if(errors.length)throw new Error(JSON.stringify({errors,requests}));
        if(Date.now()>until)throw new Error(JSON.stringify({timeout:true,errors,requests,body:document.body.textContent.slice(0,1000)}));
        await new Promise(resolve=>setTimeout(resolve,25));
      }
    };
    await wait(()=>document.getElementById('main-letra')?.textContent.includes('Primeira letra'));
    const root=document.getElementById('theme');
    assert.equal(root.style.width,'100%');assert.equal(root.style.height,'100%');
    const edges={};
    for(const edge of ['top','right','bottom','left']) {
      edges[edge]=root.parentElement.style[edge];assert.equal(edges[edge],'0px');
    }
    lyric='Segunda letra via WebSocket';emit('refresh.live.0',{});
    await wait(()=>document.getElementById('main-letra')?.textContent.includes('Segunda letra'));
    emit('element.update.0',{layer:-1,elementId:'visitor',patch:{text:'Visitante atualizado'}});
    await wait(()=>document.getElementById('visitor')?.textContent.includes('Visitante atualizado'));
    const apis=dom.window.eval('('+(()=>({entries:typeof Object.entries,values:typeof Object.values,
      observer:typeof ResizeObserver,abort:typeof AbortController,params:typeof URLSearchParams,
      padStart:typeof ''.padStart,finally:typeof Promise.prototype.finally})).toString()+')()');
    for(const value of Object.values(apis))assert.equal(value,'function');
    assert.deepEqual(errors,[]);
    global.console.log(JSON.stringify({syntax:'original rejected / adapted accepted as ES2016',
      edges,websocketLyricUpdate:true,websocketElementUpdate:true,apis,errors,requests,
      limitation:'JSDOM with selected APIs removed; no pixel layout, media playback or physical Chrome 52 test.'},null,2));
  } finally {
    if(dom)dom.window.close();
    for(const socket of sockets)socket.destroy();
    await new Promise(resolve=>server.close(resolve));
  }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
