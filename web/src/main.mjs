import './ui/calendar.css';
import './ui/document.css';
import { mountCalendar } from './ui/calendar-app.mjs';
import { prepareFrame } from './ui/frame.mjs';
import { openRepository } from './data/repository.mjs';
import { browserEffects } from './platform/browser-effects.mjs';
import { startDemo } from './demo.mjs';

const root=document.getElementById('ink-calendar');
const DEMO=new URLSearchParams(location.search).has('demo');
const DEV=typeof __DEV__!=='undefined'&&__DEV__;
const presentationKey='ink-calendar-web-view-v1';
let app,effects;
if(DEMO)app=await startDemo(root);
else{
  prepareFrame(root);
  let presentation=null;
  try{presentation=JSON.parse(sessionStorage.getItem(presentationKey));}catch{}
  try{
    const repo=await openRepository({onChange:revision=>app?.handleExternalChange(revision)});
    const initial=await repo.load();
    effects=browserEffects(message=>{document.getElementById('ink-platform-status').textContent=message;});
    app=mountCalendar(root,{initial,repository:repo,presentation,effects,onPresentation:value=>{try{sessionStorage.setItem(presentationKey,JSON.stringify(value));}catch{}}});
    effects.apply(initial.data.applied,{silent:true});
  }catch{
    const node=root.querySelector('#ink-calendar-app');node.replaceChildren();
    const heading=document.createElement('h1');heading.textContent='墨日历 · Ink Calendar';
    const text=document.createElement('p');text.textContent='无法打开本地日历，原有数据已保留。请检查浏览器存储权限后重试。 / Local storage is unavailable. Existing data has been kept.';
    const retry=document.createElement('button');retry.type='button';retry.textContent='重试 / Retry';retry.addEventListener('click',()=>location.reload());node.append(heading,text,retry);
  }
}
if(!DEV&&!DEMO&&'serviceWorker' in navigator&&window.isSecureContext){
  navigator.serviceWorker.register('./sw.js').catch(()=>{document.getElementById('ink-platform-status').textContent='离线缓存暂不可用 / Offline cache unavailable';});
}
if(DEV){const events=new EventSource('/esbuild');events.addEventListener('change',()=>location.reload());}
window.addEventListener('pagehide',event=>{if(!event.persisted){app?.dispose();effects?.dispose();}});
