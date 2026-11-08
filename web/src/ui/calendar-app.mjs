import catalog from '../generated/catalog.mjs';
import {clone as copy,translate,plural,resolveTheme,normalizeSettings,validateEvent,validateMilestone,nextId,compareMilestones} from '../domain/model.mjs';
import {dateOf as day,addDays,weekStart as monday,localToday,validDate,distance,nextMidnight} from '../domain/dates.mjs';
import {lunarDate,annotation,dateDetails,milestoneNext,milestoneYears} from '../domain/almanac.mjs';
import {renderIcons} from './icons.mjs';
import {drawComponentPreview,drawDateSample} from './component-previews.mjs';
import {calendarFitScale} from '../domain/layout.mjs';
import {colorsFor} from './colors.mjs';

export function mountCalendar(root,{demo=false,initial,repository,today=localToday,presentation=null,onPresentation=()=>{},effects=null,demoBridge=globalThis.window?.openai}={}) {
const app=root.querySelector('#ink-calendar-app');
let revision=initial.revision,currentToday=today(),busy=false,stale=false,disposed=false,midnightTimer;
const abort=new AbortController();
const production=!demo;
const fontIds=catalog.fonts.map(f=>f.id),fontNames=catalog.fonts.map(f=>f.name),fontFamilies=catalog.fonts.map(f=>f.family);
const styles=catalog.themes.map(t=>t.id),styleNames=catalog.themes.map(t=>translate(t.resource,'zh')),styleEnglish=catalog.themes.map(t=>translate(t.resource,'en'));
const keys=catalog.components.map(c=>c.key),recipes=catalog.themes.map(t=>t.recipe),choices=catalog.components.map(c=>c.options.map(o=>o.id));
const defaults=copy(initial.data.applied);
let state={page:'calendar',direction:'landscape',applied:copy(defaults),draft:copy(defaults),appearance:null,components:null,preview:null,previewFrom:'settings',settingsPending:false,settingKeys:[],selected:currentToday,events:copy(initial.data.events),milestones:copy(initial.data.milestones),editing:false,message:demo?'交互原型，使用示例事项':''};
const esc=s=>String(s).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const resolve=resolveTheme;
const validTheme=t=>t&&styles.includes(t.style)&&fontIds.includes(t.font)&&t.overrides&&Object.entries(t.overrides).every(([k,v])=>keys.includes(k)&&choices[keys.indexOf(k)].includes(v));
function restore(snapshot){const p=snapshot?.privateContent;if(p?.schema===1&&['settings','appearance','components','calendar','preview','milestones','milestone-editor','future'].includes(p.state?.page)&&validTheme(p.state.applied?.theme)&&validTheme(p.state.draft?.theme)&&(!p.state.appearance||validTheme(p.state.appearance))&&(!p.state.components||validTheme(p.state.components))&&(!p.state.preview||validTheme(p.state.preview.theme))&&Array.isArray(p.state.events)&&p.state.events.length<100){state=copy(p.state);state.milestones=Array.isArray(state.milestones)?state.milestones:[];for(const o of [state.applied,state.draft,state.preview])if(o&&!['classic','balanced'].includes(o.layout))o.layout='classic';normalizeDisplayState();}}
if(demo){restore(demoBridge?.widgetState);state.events=state.events.map(validateEvent);}
else if(presentation?.schema===1&&presentation.view&&validDate(presentation.view.selected)){
  for(const key of ['page','selected','draft','appearance','components','preview','previewFrom','editing','editingId','milestonesFrom','editorFrom','milestoneDraft','futureFrom','jumping','settingsPending','settingKeys'])if(presentation.view[key]!==undefined)state[key]=copy(presentation.view[key]);
  if(!['calendar','settings','appearance','components','preview','milestones','milestone-editor','future'].includes(state.page))state.page='calendar';
  if(!validTheme(state.draft?.theme))state.draft=copy(state.applied);
  if(state.page==='appearance'&&!validTheme(state.appearance))state.page='settings';
  if(state.page==='components'&&!validTheme(state.components))state.page='settings';
  if(state.page==='preview'&&!state.preview)state.page='calendar';
  if(state.page==='milestone-editor'&&!state.milestoneDraft)state.page='milestones';
  if(state.selected===presentation.today)state.selected=currentToday;
  const hasDraft=state.editing||state.jumping||['settings','appearance','components','preview','milestone-editor'].includes(state.page);
  stale=Boolean(hasDraft&&presentation.revision!==revision);
  if(!hasDraft)state.draft=copy(state.applied);
}
function normalizeDisplayState(){
 for(const key of ['applied','draft','preview'])if(state[key])state[key]=normalizeSettings(state[key],demo?'android':'web');
 if(!Array.isArray(state.settingKeys))state.settingKeys=[];
 if(!state.settingsPending){state.settingKeys=[];state.draft=copy(state.applied);if(state.appearance)state.appearance=copy(state.applied.theme);if(state.components)state.components=copy(state.applied.theme);}
}
normalizeDisplayState();
function options(){return state.page==='preview'?state.preview:state.applied;}
function lang(){return options().language;}
function L(zh,en){return lang()==='en'?en:zh;}
function tr(key,...values){return translate(key,lang(),...values);}
function activeTheme(){return state.page==='components'?state.components:state.page==='appearance'?state.appearance:(state.page==='calendar'||state.page==='preview')?options().theme:state.draft.theme;}
function themeName(t){const i=styles.indexOf(t.style);return L(styleNames[i],styleEnglish[i])+(Object.keys(t.overrides).length?L(' · 已微调',' · Custom'):'');}
function remember(){
  if(demo&&demoBridge?.setWidgetState)demoBridge.setWidgetState({modelContent:{concept:'ink-calendar-demo',page:state.page,orientation:state.direction,applied:state.applied,draftTheme:activeTheme(),selected:state.selected},privateContent:{schema:1,state}}).catch(()=>{});
  if(production)onPresentation(collectPresentation());
}
function formContext(){return state.page+'|'+(state.editingId||0)+'|'+(state.milestoneDraft?.id||0);}
function captureForm(){
  const form=app.querySelector('form');if(!form)return null;
  return {id:form.id,context:app.dataset.formContext,values:[...form.elements].filter(e=>e.name).map(e=>({name:e.name,value:e.value,checked:e.checked,type:e.type}))};
}
function restoreForm(saved){
  if(!saved||saved.context!==formContext())return;
  const form=app.querySelector('#'+saved.id);if(!form)return;
  for(const item of saved.values){const input=form.elements.namedItem(item.name);if(!input)continue;if(item.type==='checkbox')input.checked=item.checked;else input.value=item.value;}
}
function collectPresentation(){
  const view={};for(const key of ['page','selected','draft','appearance','components','preview','previewFrom','editing','editingId','milestonesFrom','editorFrom','milestoneDraft','futureFrom','jumping','settingsPending','settingKeys'])if(state[key]!==undefined)view[key]=copy(state[key]);
  return {schema:1,revision,today:currentToday,view,form:captureForm()};
}
function durable(){return {schema:1,applied:copy(state.applied),events:copy(state.events),milestones:copy(state.milestones)};}
function showError(error){
  const node=root.querySelector('#ink-error');if(!node)return;
  const key=error?.message||error;
  const text=key==='storage_conflict'?L('另一窗口已修改日历。当前编辑已保留；重新载入会放弃本页未保存的修改。','Another window changed the calendar. Your edits are kept; reloading will discard unsaved changes on this page.'):
    ['title_required','title_too_long','time_invalid','date_invalid','pin_limit'].includes(key)?translate(key,lang()):
    state.settingsPending?L('设置自动保存失败，当前选择已保留。请重试。','Settings could not be saved. Your selection is kept; retry.'):L('保存未完成，当前编辑已保留。请重试。','Changes were not saved. Your edits are kept; try again.');
  node.hidden=false;node.querySelector('[data-error-text]').textContent=text;
  node.querySelector('[data-action=reload-data]').hidden=key!=='storage_conflict';
  node.querySelector('[data-action=retry-settings]').hidden=!state.settingsPending||key==='storage_conflict';
}
function clearError(){const node=root.querySelector('#ink-error');if(node)node.hidden=true;}
function setBusy(value){busy=value;app.inert=value;app.setAttribute('aria-busy',String(value));if(value)root.querySelector('#ink-status').textContent=L('正在保存…','Saving…');}
async function commitData(change,message,after=()=>{}){
  if(busy)return false;if(stale){showError(new Error('storage_conflict'));return false;}
  setBusy(true);clearError();
  try{
    const next=durable();change(next);const saved=await repository.save(next,revision);
    revision=saved.revision;state.applied=copy(saved.data.applied);state.events=copy(saved.data.events);state.milestones=copy(saved.data.milestones);after();
    setBusy(false);update(message);if(state.settingsPending)showError(new Error('settings_save_failed'));return true;
  }catch(error){setBusy(false);if(error.message==='storage_conflict')stale=true;showError(error);return false;}
}
async function reloadData(keepSettingsPage=false){
  if(busy)return;const page=state.page;setBusy(true);
  try{const value=await repository.load();revision=value.revision;state.applied=copy(value.data.applied);state.draft=copy(value.data.applied);state.events=copy(value.data.events);state.milestones=copy(value.data.milestones);state.settingsPending=false;state.settingKeys=[];state.appearance=state.appearance?copy(state.applied.theme):null;state.components=state.components?copy(state.applied.theme):null;state.page=keepSettingsPage?page:'calendar';state.editing=false;state.jumping=false;state.formMemo=null;stale=false;clearError();setBusy(false);effects?.apply?.(state.applied,{silent:true});update(L('已重新载入。','Reloaded.'));}
  catch(error){setBusy(false);showError(error);}
}
function handleExternalChange(newRevision){
 if(newRevision>=0&&newRevision<=revision)return;
 if(!busy&&!state.settingsPending&&!state.editing&&!state.jumping&&['calendar','settings','appearance','components'].includes(state.page))void reloadData(state.page!=='calendar');
 else{stale=true;showError(new Error('storage_conflict'));}
}
async function autoSaveSettings(focusSelector=null,prepareFull=false,changedKey=null){
 if(busy)return false;
 if(state.page==='components')state.draft.theme=copy(state.components);else if(state.page==='appearance')state.draft.theme=copy(state.appearance);
 const key=changedKey||(['appearance','components'].includes(state.page)?'theme':null);
 if(key&&!state.settingKeys.includes(key))state.settingKeys.push(key);
 const selected=copy(state.applied);for(const name of state.settingKeys)selected[name]=copy(state.draft[name]);
 const next=normalizeSettings(selected,demo?'android':'web'),scrollY=window.scrollY;
 if(prepareFull)effects?.prepare?.(next);
 state.settingsPending=true;remember();
 const ok=await commitData(data=>{data.applied=next;},L('设置已自动保存。','Settings saved automatically.'),()=>{
   state.settingsPending=false;state.settingKeys=[];state.draft=copy(state.applied);
   if(state.appearance)state.appearance=copy(state.applied.theme);if(state.components)state.components=copy(state.applied.theme);
   if(demo&&state.applied.orientation!=='auto')state.direction=state.applied.orientation;
   effects?.apply?.(state.applied);
 });
 if(!ok){render();showError(new Error(stale?'storage_conflict':'settings_save_failed'));remember();}
 if(focusSelector)app.querySelector(focusSelector)?.focus({preventScroll:true});
 window.scrollTo(0,scrollY);return ok;
}
function effectiveDirection(){return state.applied.orientation==='auto'?(window.matchMedia('(orientation: landscape)').matches?'landscape':'portrait'):state.applied.orientation;}
function icon(name){return `<i data-lucide="${name}" aria-hidden="true"></i>`;}
function ib(name,label,action,extra=''){return `<button type="button" class="icon-btn cursor-interaction" aria-label="${esc(label)}" data-tooltip="${esc(label)}" data-action="${action}" ${extra}>${icon(name)}</button>`;}
function head(title){return `<div class="head"><h2>${title}</h2>${ib('x',tr('cancel'),'cancel')}${ib('check',tr('save'),'save')}</div><div class="thick-rule"></div>`;}
function settingsHead(title){return `<div class="head"><h2>${title}</h2>${ib('x',tr('close'),'close-settings')}</div><div class="thick-rule"></div>`;}
function group(title,ico,body){return `<section class="setting-group"><h3 class="group-title">${icon(ico)}${title}</h3>${body}</section>`;}
function toggle(key,title,ico,note=''){return `<label class="setting-row cursor-interaction"><span class="row-icon">${icon(ico)}</span><span class="row-copy"><strong>${title}</strong>${note?`<span class="secondary">${note}</span>`:''}</span><input class="switch" type="checkbox" role="switch" aria-label="${esc(title)}" data-setting="${key}" ${state.draft[key]?'checked':''}></label>`;}
function segments(key,values,labels,icons,object=state.draft){const resource=catalog.components.find(c=>c.key===key)?.resource||({view:'calendar_view',layout:'landscape_layout',orientation:'screen_orientation',language:'language',colorMode:'color_mode'})[key];return `<div class="segments" role="group" aria-label="${esc(tr(resource||key))}">${values.map((v,i)=>`<button type="button" class="cursor-interaction" data-segment="${key}" data-value="${v}" aria-pressed="${object[key]===v}">${icons?.[i]?icon(icons[i]):''}<span>${labels[i]}</span></button>`).join('')}</div>`;}
function settingsUI(){
 const d=state.draft;
 const colorControl=`<div class="control-block"><div class="control-label"><span>${icon('sun')}${tr('color_mode')}</span></div>${segments('colorMode',catalog.colorModes.map(m=>m.id),catalog.colorModes.map(m=>tr(m.resource)),['sun','moon'])}</div>`;
 const appearance=`<button class="setting-row as-button cursor-interaction" data-action="appearance" type="button"><span class="row-icon">${icon('palette')}</span><span class="row-copy"><strong>${tr('appearance')}</strong><span class="secondary">${themeName(d.theme)} · ${fontNames[fontIds.indexOf(d.theme.font)]}</span></span>${icon('chevron-right')}</button><div class="control-block"><div class="control-label"><span>${icon('type')}${tr('calendar_text_size')}</span><output id="scale-output">${d.scale}%</output></div><div class="range-row"><button type="button" class="scale-step cursor-interaction" data-scale-step="-${catalog.limits.scaleStep}" aria-label="${tr('decrease_text')}" ${d.scale<=catalog.limits.scaleMin?'disabled':''}>−</button><input type="range" min="${catalog.limits.scaleMin}" max="${catalog.limits.scaleMax}" step="${catalog.limits.scaleStep}" value="${d.scale}" data-scale style="--range-progress:${(d.scale-catalog.limits.scaleMin)/(catalog.limits.scaleMax-catalog.limits.scaleMin)*100}%" aria-label="${tr('calendar_text_size')}"><button type="button" class="scale-step cursor-interaction" data-scale-step="${catalog.limits.scaleStep}" aria-label="${tr('increase_text')}" ${d.scale>=catalog.limits.scaleMax?'disabled':''}>+</button></div><div class="size-sample" id="scale-sample" style="font-size:${18*d.scale/100}px">墨日历 · Ink Calendar</div></div>${colorControl}`;
 const content=`<div class="control-block"><div class="control-label"><span>${icon('calendar-days')}${tr('calendar_view')}</span></div>${segments('view',['month','week'],[tr('month_view'),tr('week_view')],['calendar-days','columns-3'])}</div><div class="toggle-tiles">${[['lunar','moon',tr('lunar_short')],['terms','sprout',tr('terms_short')],['holidays','flag',tr('holidays_short')]].map(([key,ico,label])=>`<button type="button" class="toggle-tile cursor-interaction" role="switch" aria-checked="${d[key]}" data-toggle="${key}">${icon(ico)}<span>${label}</span></button>`).join('')}</div>${toggle('hideDone',L('隐藏已完成','Hide completed'),'list-filter')}${toggle('desk',tr('display_mode'),'monitor',L('仅显示日期与近期未完成事项','Date and upcoming unfinished items only'))}`;
 const screen=`<div class="control-block"><div class="control-label"><span>${icon('rotate-cw')}${production?L('页面方向','Page orientation'):tr('screen_orientation')}</span></div>${segments('orientation',['auto','landscape','portrait'],[production?L('自动（跟随窗口）','Auto (follow window)'):tr('orientation_auto'),tr('orientation_landscape'),tr('orientation_portrait')])}</div><div class="control-block"><div class="control-label"><span>${icon('columns-2')}${tr('landscape_layout')}</span></div>${segments('layout',['classic','balanced'],[tr('layout_classic'),tr('layout_balanced')])}</div>${toggle('full',tr('full_screen'),'maximize')}${toggle('awake',tr('keep_awake'),'sun')}`;
 const language=`<div class="control-block"><div class="control-label"><span>${icon('languages')}${L('界面语言','Language')}</span></div>${segments('language',['zh','en'],['简体中文','English'])}</div><p class="secondary" style="padding-top:12px">${L('设置即时生效，数据保存在当前浏览器。','Changes apply automatically. Data stays in this browser.')}</p>`;
 return settingsHead(tr('settings'))+`<div class="settings-layout"><div class="settings-column">${group(tr('settings_appearance'),'sliders-horizontal',appearance)}${group(tr('settings_display'),'monitor',screen)}</div><div class="settings-column">${group(tr('settings_calendar'),'calendar-check',content)}${group(L('语言','Language'),'languages',language)}</div></div>`;
}
function styleCard(id){const t=copy(state.appearance);if(t.style!==id){t.style=id;t.overrides={};}return `<button type="button" class="choice cursor-interaction" data-style="${id}" aria-pressed="${state.appearance.style===id}"><canvas class="mini" width="376" height="224" data-mini='${JSON.stringify(t)}' aria-hidden="true"></canvas><span class="choice-caption"><span class="radio-mark" aria-hidden="true"></span>${L(styleNames[styles.indexOf(id)],styleEnglish[styles.indexOf(id)])}</span></button>`;}
function appearanceUI(){const t=state.appearance;return settingsHead(tr('appearance'))+`<h3>${tr('theme_style')}</h3><div class="choice-grid" data-grid>${styles.map(styleCard).join('')}</div><h3>${tr('theme_font')}</h3><p class="secondary">${L('中文自动搭配宋体或黑体。','Chinese is paired automatically.')}</p><div class="choice-grid" data-grid>${fontIds.map((id,i)=>`<button type="button" class="choice font-choice cursor-interaction" style="--choice-font:${fontFamilies[i]}" data-font="${id}" aria-label="${fontNames[i]}" aria-pressed="${t.font===id}"><span class="radio-mark" aria-hidden="true"></span><span class="font-aa" aria-hidden="true">Aa</span><span class="font-name">${fontNames[i]}</span></button>`).join('')}</div><button type="button" class="full-button cursor-interaction" data-action="components">${icon('sliders-horizontal')}${tr('theme_components')}</button>`;}
function componentsUI(){const resolved=resolve(state.components);return settingsHead(tr('theme_components'))+`<div class="component-list">${catalog.components.map(component=>`<section class="control-block"><h3 class="control-label" id="component-label-${component.key}">${tr(component.resource)}</h3><div class="choice-grid component-grid" data-grid role="group" aria-labelledby="component-label-${component.key}">${component.options.map(option=>`<button type="button" class="choice component-choice" data-segment="${component.key}" data-value="${option.id}" aria-pressed="${resolved[component.key]===option.id}"><canvas class="component-mini" width="296" height="192" data-component-mini="${component.key}" data-component-value="${option.id}" aria-hidden="true"></canvas><span class="choice-caption"><span class="radio-mark" aria-hidden="true"></span>${tr(option.resource)}</span></button>`).join('')}</div></section>`).join('')}</div><button type="button" class="full-button cursor-interaction" data-action="reset-components" style="margin-top:20px">${icon('rotate-ccw')}${tr('theme_reset')}</button>`;}
function dayName(s){return (lang()==='en'?['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday']:['星期日','星期一','星期二','星期三','星期四','星期五','星期六'])[day(s).getUTCDay()];}
function shortDay(s){return (lang()==='en'?['Sun','Mon','Tue','Wed','Thu','Fri','Sat']:['周日','周一','周二','周三','周四','周五','周六'])[day(s).getUTCDay()];}
function monthLabel(s){const d=day(s);return lang()==='en'?new Intl.DateTimeFormat('en-US',{month:'long',year:'numeric',timeZone:'UTC'}).format(d):`${d.getUTCFullYear()} 年 ${d.getUTCMonth()+1} 月`;}
function dateLabel(s){const d=day(s);return lang()==='en'?new Intl.DateTimeFormat('en-US',{month:'short',day:'numeric',timeZone:'UTC'}).format(d):`${d.getUTCMonth()+1}月${d.getUTCDate()}日`;}
function eventsOn(s,o){return state.events.filter(e=>e.date===s&&(!o.hideDone||!e.completed)).sort((a,b)=>a.time.localeCompare(b.time)||a.id-b.id);}
function overview(o){const start=monday(state.selected);return `<section class="week-overview" aria-label="${L('一周日历','Weekly calendar')}" data-calendar="${resolve(o.theme).calendar}">${Array.from({length:7},(_,i)=>{const s=addDays(start,i),items=eventsOn(s,o),first=items[0];return `<button type="button" class="day-row cursor-interaction" data-day="${s}" aria-pressed="${s===state.selected}" aria-label="${esc(s+' '+(first?.title||tr('no_events')))}"><span class="day-date"><span class="date-badge">${day(s).getUTCDate()}</span><span class="event-dot" style="visibility:${items.length?'visible':'hidden'}"></span></span><span class="day-summary"><span class="day-title">${first?esc(first.title):tr('no_events')}</span><span class="day-meta">${shortDay(s)}${s===currentToday?L(' · 今天',' · Today'):''}${annotation(s,o)?' · '+annotation(s,o):''}</span></span><span class="day-count">${plural('event_count',items.length,lang())}</span></button>`;}).join('')}</section>`;}
function editorForm(){const e=state.events.find(e=>e.id===state.editingId);return `<form class="inline-editor" id="week-task-form"><label>${tr('date')}<input name="date" type="date" required min="1900-01-01" max="2100-12-31" value="${e?.date||state.selected}"></label><label>${tr('event_title')}<input name="title" maxlength="${catalog.limits.titleLength}" required value="${esc(e?.title||'')}" placeholder="${L('写下事项','Add an event')}" aria-label="${L('事项名称','Event title')}"></label><label>${L('时间，可留空','Time, optional')}<input name="time" type="time" value="${e?.time||''}" aria-label="${L('事项时间','Event time')}"></label>${state.confirmDelete?`<div role="alert">${L('删除这个事项？','Delete this event?')}<button type="button" data-action="confirm-delete-event">${tr('delete')}</button></div>`:''}<div class="edit-actions">${e?`<button type="button" class="cursor-interaction" data-action="delete-event">${tr('delete')}</button>`:''}<button type="button" class="cursor-interaction" data-action="cancel-edit">${tr('cancel')}</button><button type="button" data-action="save-event" class="save-task cursor-interaction">${tr('save')}</button></div></form>`;}
function milestoneSummary(m){const next=milestoneNext(m,currentToday);if(!next)return tr('outside_range');const days=distance(currentToday,next),years=milestoneYears(m,next);let remaining=days===0?tr('today'):plural(days>0?'days_until':'days_since',Math.abs(days),lang());if(m.kind==='anniversary'&&years>0)remaining=tr('anniversary_years',years,remaining);if(m.kind==='anniversary'&&m.date<=currentToday)remaining+='<br>'+plural('days_since',distance(m.date,currentToday),lang());return remaining;}
function milestoneRow(m,compact=false){return `<button type="button" class="milestone-row cursor-interaction" data-edit-milestone="${m.id}"><strong>${esc(m.title)}</strong><span>${milestoneSummary(m)}</span>${compact?'':`<small>${L('起始日期：','Anchor date: ')}${m.date}${m.lunar&&lang()!=='en'?' · '+lunarDate(m.date).full:''}</small>`}</button>`;}
function homeMilestones(){return `<section class="home-milestones"><div class="pane-heading"><strong>${tr('milestones')}</strong>${ib('bookmark',tr('milestones'),'milestones')}</div>${state.milestones.filter(m=>m.pinned).sort(compareMilestones).map(m=>milestoneRow(m,true)).join('')}</section>`;}
function milestonesUI(){return `<div class="head"><h2>${tr('milestones')}</h2><button type="button" class="text-button cursor-interaction" data-action="close-milestones">${tr('close')}</button></div><div class="thick-rule"></div>${state.milestones.length?[...state.milestones].sort(compareMilestones).map(m=>milestoneRow(m)).join(''):`<p style="padding:16px 0">${tr('empty_milestones')}</p>`}<button type="button" class="full-button cursor-interaction" data-action="new-milestone">${tr('add_milestone')}</button>`;}
function milestoneEditorUI(){const m=state.milestoneDraft;return `${head(m.id?tr('edit_milestone'):tr('add_milestone'))}<form class="inline-editor" id="milestone-form"><label>${tr('milestone_name')}<input name="title" value="${esc(m.title)}" required maxlength="${catalog.limits.titleLength}"></label><label>${tr('milestone_type')}<select name="kind"><option value="anniversary" ${m.kind==='anniversary'?'selected':''}>${tr('anniversary')}</option><option value="countdown" ${m.kind==='countdown'?'selected':''}>${tr('countdown')}</option></select></label><label>${tr('date')}<input type="date" name="date" value="${m.date}" min="1900-01-01" max="2100-12-31" required></label><label class="form-option"><input name="lunar" type="checkbox" ${m.lunar?'checked':''} ${m.kind==='countdown'?'disabled':''}>${tr('lunar_anniversary')}</label><label class="form-option"><input name="pinned" type="checkbox" ${m.pinned?'checked':''}>${tr('pin_home')}</label><div role="alert" id="milestone-error">${esc(state.formError||'')}</div>${m.id?`<button type="button" class="text-button cursor-interaction" data-action="delete-milestone">${tr('delete')}</button>`:''}${state.confirmMilestoneDelete?`<div role="alert">${tr('delete_milestone')}<button type="button" data-action="confirm-delete-milestone">${tr('delete')}</button></div>`:''}</form>`;}
function editMilestone(m){state.editorFrom=state.page;state.milestoneDraft=copy(m||{id:0,title:'',date:state.selected,kind:'anniversary',lunar:false,pinned:state.milestones.filter(x=>x.pinned).length<catalog.limits.pinnedMilestones});state.formError='';state.confirmMilestoneDelete=false;state.page='milestone-editor';update(L("编辑纪念日。","Editing milestone."));}
function saveMilestone(){
  const form=app.querySelector('#milestone-form');if(!form.reportValidity())return;
  const data=new FormData(form);let milestone;
  try{milestone=validateMilestone({id:state.milestoneDraft.id||nextId(state.milestones),title:String(data.get('title')),date:String(data.get('date')),kind:String(data.get('kind')),lunar:data.has('lunar'),pinned:data.has('pinned')});}catch(error){showError(error);return;}
  void commitData(next=>{
    if(milestone.pinned&&next.milestones.filter(m=>m.pinned&&m.id!==milestone.id).length>=catalog.limits.pinnedMilestones)throw new Error('pin_limit');
    const index=next.milestones.findIndex(m=>m.id===milestone.id);if(index<0)next.milestones.push(milestone);else next.milestones[index]=milestone;
  },L('已保存纪念日。','Milestone saved.'),()=>{state.page=state.editorFrom||'milestones';});
}
function homeIdentity(o){const t=resolve(o.theme),num=String(day(currentToday).getUTCDate()).padStart(2,'0');return `<section class="home-identity" data-date="${t.date}" data-today="${currentToday}" aria-label="${L('今日日期','Today')} ${currentToday}"><div class="home-hero"><div class="home-number ${t.date==='tiles'?'home-tiles':''}">${t.date==='tiles'?num.split('').map(n=>`<span>${n}</span>`).join(''):num}</div><div class="home-words"><div class="home-year">${monthLabel(currentToday)}</div><div class="home-day tracked">${dayName(currentToday)}</div></div></div>${o.desk?`<div class="calendar-details">${dateDetails(currentToday,o)}</div>`:''}</section>`;}
function taskMarkup(e,upcoming){const meta=(upcoming?(e.date===currentToday?tr('today'):dateLabel(e.date))+'  ':'')+(e.time||tr('all_day'));return `<div class="task ${e.completed?'done':''}"><label class="task-check cursor-interaction"><input type="checkbox" data-event-check="${e.id}" aria-label="${esc(L('完成：','Complete: ')+e.title)}" ${e.completed?'checked':''}></label><button type="button" class="task-body cursor-interaction" data-edit-event="${e.id}" aria-label="${esc(L('编辑：','Edit: ')+e.title)}"><span class="task-time">${meta}${e.completed?L(' · 已完成',' · Completed'):''}</span><span class="task-title">${esc(e.title)}</span></button></div>`;}
function homeAgenda(o){const t=resolve(o.theme),upcoming=o.desk||state.selected===currentToday||state.future,start=o.desk?currentToday:state.selected,end=upcoming?addDays(start,state.future?29:6):start;let items=state.events.filter(e=>e.date>=start&&e.date<=end&&(!(o.hideDone||o.desk)||!e.completed)).sort((a,b)=>a.date.localeCompare(b.date)||a.time.localeCompare(b.time)||a.id-b.id);if(o.desk)items=items.slice(0,3);return `<section class="home-agenda" data-agenda="${t.agenda}"><div class="pane-heading"><strong class="tracked">${state.future?L('未来事项 · 30 天','Upcoming · 30 days'):upcoming?tr('upcoming'):dateLabel(state.selected)}</strong><span class="agenda-tools">${ib('list',tr('future_events'),'future')}${ib('plus',tr('add_event'),'add-event')}</span></div>${state.editing?editorForm():''}${items.length?items.map(e=>taskMarkup(e,upcoming)).join(''):`<p style="padding:8px 0">${upcoming?tr('empty_upcoming'):tr('empty_day')}</p>`}</section>`;}
function futureUI(o){const days=o.futureDays||catalog.defaults.futureDays,items=state.events.filter(e=>e.date>=currentToday&&e.date<=addDays(currentToday,days-1)&&(!o.hideDone||!e.completed)).sort((a,b)=>a.date.localeCompare(b.date)||a.time.localeCompare(b.time)||a.id-b.id);return `<div class="head"><h2>${tr('future_events')}</h2>${ib('plus',tr('add_event'),'add-event')}</div><div class="thick-rule"></div><div class="segments" role="group" aria-label="${L('日期范围','Date range')}">${[7,30].map(n=>`<button type="button" data-future-days="${n}" aria-pressed="${days===n}">${n}${L(' 天',' days')}</button>`).join('')}</div><label class="setting-row"><span class="row-copy">${tr('hide_completed')}</span><input class="switch" role="switch" type="checkbox" data-future-hide ${o.hideDone?'checked':''}></label><section data-agenda="${resolve(o.theme).agenda}">${state.editing?editorForm():''}${items.length?items.map(e=>taskMarkup(e,true)).join(''):`<p style="padding:18px 0">${tr('empty_range')}</p>`}</section><button type="button" class="full-button cursor-interaction" data-action="close-future">${tr('back')}</button>`;}
function monthGrid(o){const selected=day(state.selected),first=new Date(Date.UTC(selected.getUTCFullYear(),selected.getUTCMonth(),1,12)),start=monday(first.toISOString().slice(0,10));return `<div class="month-grid">${(lang()==='en'?['M','T','W','T','F','S','S']:['一','二','三','四','五','六','日']).map(v=>`<span class="weekday">${v}</span>`).join('')}</div>${Array.from({length:6},(_,r)=>`<div class="month-weekrow">${Array.from({length:7},(_,col)=>{const s=addDays(start,r*7+col),d=day(s),marked=state.events.some(e=>e.date===s);return `<button type="button" class="month-cell cursor-interaction ${d.getUTCMonth()!==selected.getUTCMonth()?'adjacent':''} ${s===currentToday?'is-today':''}" data-day="${s}" aria-pressed="${s===state.selected}" aria-label="${s}"><span class="month-number">${d.getUTCDate()}</span><span class="month-dot ${marked?'':'empty'}"></span><span class="month-annotation">${annotation(s,o)||'　'}</span></button>`;}).join('')}</div>`).join('')}`;}
function calendarUI(o,preview=false){
 const t=resolve(o.theme),start=monday(state.selected),end=addDays(start,6),period=o.view==='week'?dateLabel(start)+' — '+dateLabel(end>'2100-12-31'?'2100-12-31':end):monthLabel(state.selected);
 const rule=catalog.themes.find(t=>t.id===o.theme.style).ruleWidth,tracking=t.tracking==='spaced'?'.09em':t.tracking==='tight'?'-.035em':'0';
 const banner=preview?`<div class="draft-banner"><button type="button" class="back-button cursor-interaction" data-action="back-preview">${icon('arrow-left')}${L('返回设置','Back to settings')}</button><span>${L('日历预览','Calendar preview')}</span></div>`:'';
 const jump=state.jumping?`<form class="inline-editor" id="jump-form"><label>${tr('jump_date')}<input name="date" type="date" value="${state.selected}" min="1900-01-01" max="2100-12-31" required></label><div class="edit-actions"><button type="button" data-action="cancel-jump">${tr('cancel')}</button><button type="button" data-action="save-jump">${tr('save')}</button></div></form>`:'';
 return `${banner}<div class="calendar-home" data-date="${t.date}" style="--home-rule:${rule}px;--home-tracking:${tracking}"><div class="head"><strong class="brand tracked">${tr('app_name')}</strong>${ib('calendar-check',tr('go_today'),'today')}${ib('settings',tr('settings'),preview?'back-preview':'open-settings')}</div><div class="thick-rule"></div><div class="home-layout" data-layout="${o.layout||'classic'}" data-flow="${t.flow}" data-desk="${o.desk}"><div class="home-side"><div class="home-date-column">${homeIdentity(o)}${homeMilestones()}</div>${homeAgenda(o)}</div><section class="calendar-panel" data-view="${o.view}" data-calendar="${t.calendar}"><div class="calendar-surface"><div class="home-month-nav">${ib('chevron-left',L(o.view==='week'?'上一周':'上一月',o.view==='week'?'Previous week':'Previous month'),'previous')}<button type="button" class="home-month-title cursor-interaction" data-action="jump" aria-label="${tr('jump_date')}">${period}</button>${ib('chevron-right',L(o.view==='week'?'下一周':'下一月',o.view==='week'?'Next week':'Next month'),'next')}</div>${jump}${o.view==='week'?overview(o):monthGrid(o)}<div class="calendar-details">${dateDetails(state.selected,o)||'　'}</div></div></section></div></div>`;
}
function drawMini(canvas){const t=JSON.parse(canvas.dataset.mini),r=resolve(t),c=canvas.getContext('2d'),land=state.direction==='landscape',wide=land&&app.clientWidth>=catalog.limits.wideLayoutMin,w=land?240:188,o=state.draft,palette=colorsFor(o.colorMode);canvas.width=w*2;canvas.height=296;canvas.style.aspectRatio=w+'/148';c.setTransform(2,0,0,2,0,0);c.fillStyle=palette.paper;c.fillRect(0,0,w,148);c.fillStyle=c.strokeStyle=palette.ink;c.lineWidth=1;
 const txt=(s,x,y,size,bold=false)=>{c.font=`${bold?900:400} ${size}px InkOriginal,serif`;c.fillText(s,x,y);};const line=(x,y,width)=>{c.beginPath();c.moveTo(x,y);c.lineTo(x+width,y);c.stroke();};
 const hero=(x,y,width,column=false)=>drawDateSample(c,r,catalog.fonts[0],x,y,width,column?70:50,column,palette);
 const agenda=(x,y,width,height)=>{line(x,y-3,width);for(let i=0;i<2;i++){const yy=y+i*height/2;if(r.agenda==='cards'){c.setLineDash([3,3]);line(x,yy+height/2-2,width);c.setLineDash([]);}line(x+3,yy+4,Math.min(20,width*.3));line(r.agenda==='timeline'?x+width*.35:x+3,yy+9,width*.6);}};
 const cal=(x,y,width,height)=>{txt(o.view==='week'?'Nov 2 - 8':'November 2026',x+width*.2,y+7,7,true);y+=14;height-=14;const count=o.view==='week'?7:6,step=height/count;if(o.view==='week'){for(let i=0;i<7;i++){const yy=y+i*step;txt(String(i+2),x+3,yy+step*.7,Math.min(8,step*.65));line(x+20,yy+step*.5,width-23);if(r.calendar==='ruled')line(x,yy+step,width);}}else{for(let row=0;row<6;row++){for(let col=0;col<7;col++){const n=row*7+col-5,sel=n===7,xx=x+width*(col+.5)/7,yy=y+row*step;if(sel&&r.calendar!=='plain'){c.beginPath();if(r.calendar==='open')c.arc(xx,yy+step*.5,Math.min(5,step*.45),0,Math.PI*2);else c.rect(xx-width/14,yy,width/7,step);c.fill();c.fillStyle=palette.paper;}txt(String(n<1?31+n:n>30?n-30:n),xx-2,yy+step*.75,Math.min(7,step*.7));c.fillStyle=palette.ink;if(sel&&r.calendar==='plain')line(xx-4,yy+step-1,8);}if(r.calendar==='ruled')line(x,y+(row+1)*step,width);}}};
 txt(r.caps==='on'?'INK CALENDAR':'Ink Calendar',10,12,8,true);line(10,19,w-20);
 if(wide&&o.layout==='balanced'){const first=r.flow==='agenda-first',x=first?10:132;hero(x,27,98,true);agenda(x,108,98,28);cal(first?132:10,31,98,105);}else if(wide){hero(10,27,76,true);if(r.flow==='agenda-first'){agenda(102,34,128,42);cal(10,100,220,40);}else{agenda(10,108,76,28);cal(98,31,132,105);}}else{hero(10,27,w-20);if(r.flow==='agenda-first'){agenda(10,84,w-20,23);cal(10,110,w-20,38);}else{cal(10,80,w-20,38);agenda(10,123,w-20,23);}}
}
let gridObserver;
function drawPreviews(){
 app.querySelectorAll('[data-mini]').forEach(drawMini);
 app.querySelectorAll('[data-component-mini]').forEach(canvas=>{
   const key=canvas.dataset.componentMini,theme=copy(state.components);theme.overrides[key]=canvas.dataset.componentValue;
   drawComponentPreview(canvas,theme,key,{landscape:state.direction==='landscape',wide:state.direction==='landscape'&&app.clientWidth>=catalog.limits.wideLayoutMin,weekly:state.draft.view==='week',balanced:state.draft.layout==='balanced',colorMode:state.draft.colorMode});
 });
}
function fitCalendar(){
 const panel=app.querySelector('.calendar-panel'),surface=panel?.querySelector('.calendar-surface');if(!surface)return;
 surface.style.width='';surface.style.transform='';panel.style.height='';
 let scale=1;
 if(state.direction==='landscape'&&options().view==='month'&&!options().desk&&!state.jumping){
   const viewport=window.visualViewport?.height||window.innerHeight,top=panel.getBoundingClientRect().top+window.scrollY;
   scale=calendarFitScale(surface.offsetHeight,viewport-top-catalog.limits.calendarFitBottomGap);
 }
 // Preserve the original column width while reducing type size and row height uniformly.
 if(scale<1)surface.style.width=(100/scale)+'%';
 const factor=String(Math.round(scale*10000)/10000),height=scale<1?Math.ceil(surface.offsetHeight*scale)+'px':'',transform=scale<1?`scale(${factor})`:'';
 if(panel.style.height!==height)panel.style.height=height;
 panel.style.overflow=scale<1?'hidden':'';
 if(surface.style.transform!==transform)surface.style.transform=transform;
 panel.dataset.calendarScale=factor;
}
function layout(){
 const calendar=state.page==='calendar'||state.page==='preview',wide=state.direction==='landscape'&&app.clientWidth>=catalog.limits.wideLayoutMin&&!(calendar&&options().desk);app.dataset.wide=String(wide);
 for(const grid of app.querySelectorAll('[data-grid]')){
   const width=grid.clientWidth,gap=catalog.limits.choiceGap,max=catalog.limits.choiceMax,min=catalog.limits.choiceMin;
   const possible=Math.max(1,Math.floor((width+gap)/(min+gap))),wanted=Math.max(1,Math.ceil((width+gap)/(max+gap))),columns=Math.min(possible,wanted),cell=Math.min(max,(width-gap*(columns-1))/columns);
   const value=`repeat(${columns}, minmax(0, ${Math.max(0,cell)}px))`;if(grid.style.gridTemplateColumns!==value)grid.style.gridTemplateColumns=value;
 }
 for(const number of app.querySelectorAll('.home-number:not(.home-tiles)')){
   const type=number.closest('[data-date]').dataset.date;let size=(options().desk?120:type==='masthead'?132:type==='center'?112:84)*options().scale/100;number.style.fontSize=size+'px';
   while(number.scrollWidth>number.clientWidth&&size>20){size-=1;number.style.fontSize=size+'px';}
 }
 fitCalendar();
}
let restoredForm=presentation?.form||null;
function render(){
 const savedForm=captureForm()||restoredForm;restoredForm=null;
 if(production)state.direction=effectiveDirection();
 const o=options(),t=o.theme,r=resolve(t),fontIndex=fontIds.indexOf(t.font);app.dataset.direction=state.direction;app.style.setProperty('--latin',fontFamilies[fontIndex]);app.style.setProperty('--reading',t.font==='original'?'InkReading':fontFamilies[fontIndex]);app.style.setProperty('--cjk',catalog.fonts[fontIndex].sans?'InkCJKSans':'InkCJKSerif');app.style.setProperty('--scale',o.scale/100);app.style.setProperty('--rule',catalog.themes.find(x=>x.id===t.style).ruleWidth+'px');app.style.setProperty('--tracking',r.tracking==='spaced'?'.07em':r.tracking==='tight'?'-.03em':'0');
 const palette=colorsFor(o.colorMode);root.dataset.colorMode=palette.id;
 root.style.setProperty('--paper',palette.paper);root.style.setProperty('--ink',palette.ink);
 document.documentElement.dataset.inkColorMode=palette.id;
 document.querySelector('meta[name="theme-color"]')?.setAttribute('content',palette.paper);
 app.innerHTML=state.page==='settings'?settingsUI():state.page==='appearance'?appearanceUI():state.page==='components'?componentsUI():state.page==='milestones'?milestonesUI():state.page==='milestone-editor'?milestoneEditorUI():state.page==='future'?futureUI(o):calendarUI(o,state.page==='preview');
 app.dataset.buttons=r.buttons;app.dataset.caps=r.caps;app.dataset.language=o.language;
 root.lang=o.language==='en'?'en':'zh-CN';if(production)document.documentElement.lang=root.lang;
 app.dataset.formContext=formContext();restoreForm(savedForm);
 app.querySelectorAll('button').forEach(button=>button.classList.add('cursor-interaction'));
 if(state.page==='calendar'||state.page==='preview'){
   app.dataset.calendarView=o.view;
   app.querySelectorAll('[data-day]').forEach(button=>button.disabled=!validDate(button.dataset.day));
   const start=o.view==='week'?monday(state.selected):state.selected.slice(0,7)+'-01';
   app.querySelector('[data-action=previous]').disabled=start<='1900-01-01';
   app.querySelector('[data-action=next]').disabled=start>=(o.view==='week'?monday('2100-12-31'):'2100-12-01');
 }else delete app.dataset.calendarView;
 if(production&&(state.editing||state.jumping)){
   const active=app.querySelector(state.editing?'#week-task-form':'#jump-form');
   if(active)app.querySelectorAll('button,input,select').forEach(control=>{if(!active.contains(control))control.disabled=true;});
 }
 if(production&&state.page==='settings'&&effects){
   for(const key of ['full','awake'])if(!effects.capabilities[key]){const input=app.querySelector(`[data-setting="${key}"]`);if(input){input.disabled=true;input.checked=false;input.closest('label').querySelector('.row-copy').insertAdjacentHTML('beforeend',`<span class="unsupported-note">${L('浏览器不支持','Unavailable in this browser')}</span>`);}}
 }
 root.querySelectorAll('[data-lab-page]').forEach(b=>b.setAttribute('aria-pressed',String((state.page==='calendar'||state.page==='preview'?'calendar':'settings')===b.dataset.labPage&&(!b.dataset.labView||b.dataset.labView===o.view))));root.querySelectorAll('button[data-direction]').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.direction===state.direction)));root.querySelector('#ink-status').textContent=state.message;
 layout();gridObserver?.disconnect();gridObserver=new ResizeObserver(layout);gridObserver.observe(app);app.querySelectorAll('[data-grid],.calendar-surface').forEach(g=>gridObserver.observe(g));drawPreviews();renderIcons(app);
 document.fonts.ready.then(()=>{if(!disposed){layout();drawPreviews();}});
}
function update(message){state.message=message;render();remember();}
function saveEvent(){
  const form=app.querySelector('#week-task-form');if(!form||!form.reportValidity())return;
  const data=new FormData(form),existing=state.events.find(e=>e.id===state.editingId);let event;
  try{event=validateEvent({id:existing?.id||nextId(state.events),date:String(data.get('date')||state.selected),title:String(data.get('title')||''),time:String(data.get('time')||''),completed:existing?.completed||false});}catch(error){showError(error);return;}
  void commitData(next=>{const index=next.events.findIndex(e=>e.id===event.id);if(index<0)next.events.push(event);else next.events[index]=event;},L('已保存事项。','Event saved.'),()=>{state.selected=event.date;state.editing=false;state.editingId=null;state.confirmDelete=false;});
}
function preview(view=null){const o=copy(state.applied);if(typeof view==='string')o.view=view;state.preview=o;state.previewFrom=state.page;state.page='preview';state.editing=false;update(L("日历预览。","Calendar preview."));}
function setScale(value){const limit=catalog.limits;state.draft.scale=Math.max(limit.scaleMin,Math.min(limit.scaleMax,Number(value)));app.querySelector('[data-scale]').value=state.draft.scale;app.querySelector('[data-scale]').style.setProperty('--range-progress',(state.draft.scale-limit.scaleMin)/(limit.scaleMax-limit.scaleMin)*100+'%');app.querySelector('#scale-output').textContent=state.draft.scale+'%';app.querySelector('#scale-sample').style.fontSize=18*state.draft.scale/100+'px';app.querySelector(`[data-scale-step="-${limit.scaleStep}"]`).disabled=state.draft.scale<=limit.scaleMin;app.querySelector(`[data-scale-step="${limit.scaleStep}"]`).disabled=state.draft.scale>=limit.scaleMax;}
function navigatePeriod(step){
 const o=options();
 if(o.view==='week')state.selected=addDays(state.selected,step*7);
 else{const d=day(state.selected),target=new Date(Date.UTC(d.getUTCFullYear(),d.getUTCMonth()+step,1,12)).toISOString().slice(0,10);state.selected=target.slice(0,7)===currentToday.slice(0,7)?currentToday:target;}
 state.selected=state.selected<catalog.limits.dateMin?catalog.limits.dateMin:state.selected>catalog.limits.dateMax?catalog.limits.dateMax:state.selected;
 state.editing=false;state.future=false;update(L('已切换日期范围。','Date range changed.'));
}
let gesture=null,suppressNextClick=false;
root.addEventListener('pointerdown',event=>{
 if(!event.isPrimary){gesture=null;return;}
 if(event.button!==0||busy||state.editing||state.jumping||!['calendar','preview'].includes(state.page)||!event.target.closest('.calendar-panel')||event.target.closest('form'))return;
 gesture={id:event.pointerId,x:event.clientX,y:event.clientY};
},{signal:abort.signal});
root.addEventListener('pointerup',event=>{
 const start=gesture;gesture=null;if(!start||start.id!==event.pointerId||busy)return;
 const dx=event.clientX-start.x,dy=event.clientY-start.y;
 if(Math.abs(dx)>=48&&Math.abs(dx)>Math.abs(dy)*1.5){suppressNextClick=true;setTimeout(()=>{suppressNextClick=false;},0);navigatePeriod(dx<0?1:-1);}
},{signal:abort.signal});
root.addEventListener('pointercancel',()=>{gesture=null;},{signal:abort.signal});
root.addEventListener('click',event=>{if(suppressNextClick){suppressNextClick=false;event.preventDefault();event.stopImmediatePropagation();}},{capture:true,signal:abort.signal});
root.addEventListener('click',event=>{
 const b=event.target.closest('button');if(!b||!root.contains(b)||busy)return;
 if(b.dataset.direction){state.direction=b.dataset.direction;update(state.direction==='landscape'?'横屏：宽度足够时采用总览与详情双栏。':'竖屏：总览与当日详情上下排列。');return;}
 if(b.dataset.labPage){if(b.dataset.labPage==='calendar'){const view=b.dataset.labView||'week';if(state.page!=='calendar'&&state.page!=='preview')preview(view);else{options().view=view;state.editing=false;update('仅切换日历区域，保留当前布局。');}}else{if(state.page==='preview')state.page=state.previewFrom;else if(state.page==='calendar'){if(!state.settingsPending)state.draft=copy(state.applied);state.page='settings';}update(L('设置即时生效并自动保存。','Changes apply and save automatically.'));}return;}
 if(b.dataset.scaleStep){setScale(state.draft.scale+Number(b.dataset.scaleStep));void autoSaveSettings(`[data-scale-step="${b.dataset.scaleStep}"]`,false,'scale');return;}
 if(b.dataset.editEvent){state.editingId=Number(b.dataset.editEvent);state.editing=true;state.confirmDelete=false;update(L("编辑事项。","Editing event."));app.querySelector('input[name=title]')?.focus();return;}
 if(b.dataset.editMilestone){editMilestone(state.milestones.find(m=>m.id===Number(b.dataset.editMilestone)));return;}
 if(b.dataset.futureDays){const days=Number(b.dataset.futureDays);void commitData(next=>{next.applied.futureDays=days;},L('已切换未来事项范围。','Upcoming date range changed.'));return;}
 if(b.dataset.style){state.appearance.style=b.dataset.style;state.appearance.overrides={};void autoSaveSettings(`[data-style="${b.dataset.style}"]`);return;}
 if(b.dataset.font){state.appearance.font=b.dataset.font;void autoSaveSettings(`[data-font="${b.dataset.font}"]`);return;}
 if(b.dataset.toggle){state.draft[b.dataset.toggle]=!state.draft[b.dataset.toggle];void autoSaveSettings(`[data-toggle="${b.dataset.toggle}"]`,false,b.dataset.toggle);return;}
 if(b.dataset.segment){const key=b.dataset.segment;if(state.page==='components')state.components.overrides[key]=b.dataset.value;else state.draft[key]=b.dataset.value;void autoSaveSettings(`[data-segment="${key}"][data-value="${b.dataset.value}"]`,false,state.page==='components'?'theme':key);return;}
 if(b.dataset.mainView){options().view=b.dataset.mainView;state.editing=false;update(b.dataset.mainView==='week'?'周总览与当日详情。':'月历与当日详情。');return;}
 if(b.dataset.day){state.selected=b.dataset.day;state.editing=false;state.future=false;update(tr('selected')+' '+state.selected);return;}
 const action=b.dataset.action;
 if(action==='reload-data'){void reloadData();return;}
 if(action==='retry-settings'){void autoSaveSettings(null,state.draft.full!==state.applied.full);return;}
 if(action==='clear-error'){clearError();return;}
 if(action==='preview')preview();
 else if(action==='back-preview'){state.page=state.previewFrom;state.editing=false;update(L("返回设置。","Returned to settings."));}
 else if(action==='open-settings'){if(!state.settingsPending)state.draft=copy(state.applied);state.page='settings';state.editing=false;update(L("设置即时生效并自动保存。","Changes apply and save automatically."));}
 else if(action==='appearance'){state.appearance=copy(state.draft.theme);state.page='appearance';update(L("风格与字体独立选择。","Style and font can be selected independently."));}
 else if(action==='components'){state.components=copy(state.appearance);state.page='components';update(L("组件微调将覆盖当前预设。","Component changes override the preset."));}
 else if(action==='reset-components'){state.components.overrides={};void autoSaveSettings('[data-action=reset-components]');}
 else if(action==='close-settings'){
   if(state.page==='components')state.page='appearance';else if(state.page==='appearance')state.page='settings';else state.page='calendar';
   update(L('已关闭设置。','Settings closed.'));
 }else if(action==='save'&&state.page==='milestone-editor')saveMilestone();
 else if(action==='cancel'&&state.page==='milestone-editor'){
   state.page=state.editorFrom||'milestones';update(L('已取消当前修改。','Changes cancelled.'));
 }else if(action==='today'){state.selected=currentToday;state.editing=false;state.future=false;update(L('已回到今天。','Back to today.'));}
 else if(action==='previous'||action==='next')navigatePeriod(action==='previous'?-1:1);
 else if(action==='future'){state.futureFrom=state.page;state.page='future';state.editing=false;update(L("未来事项。","Upcoming events."));}
 else if(action==='close-future'){state.page=state.futureFrom||'calendar';state.editing=false;update(L("返回日历。","Returned to the calendar."));}
 else if(action==='add-event'){state.editing=!state.editing;state.editingId=null;state.confirmDelete=false;update(L("为所选日期添加事项。","Add an event for the selected date."));app.querySelector('input[name=title]')?.focus();}
 else if(action==='cancel-edit'){state.editing=false;update(L("已取消编辑。","Editing cancelled."));}
 else if(action==='save-event')saveEvent();
 else if(action==='delete-event'){state.confirmDelete=true;update(L("确认删除事项。","Confirm event deletion."));}
 else if(action==='confirm-delete-event'){const id=state.editingId;void commitData(next=>{next.events=next.events.filter(e=>e.id!==id);},L('已删除事项。','Event deleted.'),()=>{state.editing=false;state.editingId=null;state.confirmDelete=false;});}
 else if(action==='jump'){state.jumping=true;update(L("选择跳转日期。","Choose a date to jump to."));}
 else if(action==='cancel-jump'){state.jumping=false;update(L("已取消日期跳转。","Date jump cancelled."));}
 else if(action==='save-jump'){const form=app.querySelector('#jump-form');if(form.reportValidity()){state.selected=String(new FormData(form).get('date'));state.jumping=false;state.future=false;state.editing=false;update(L("已跳转日期。","Date changed."));}}
 else if(action==='milestones'){state.milestonesFrom=state.page;state.page='milestones';update(L("纪念日与倒计时。","Milestones."));}
 else if(action==='close-milestones'){state.page=state.milestonesFrom||'calendar';update(L("已关闭纪念日列表。","Milestones closed."));}
 else if(action==='new-milestone')editMilestone();
 else if(action==='delete-milestone'){state.confirmMilestoneDelete=true;update(L("确认删除纪念日。","Confirm milestone deletion."));}
 else if(action==='confirm-delete-milestone'){const id=state.milestoneDraft.id;void commitData(next=>{next.milestones=next.milestones.filter(m=>m.id!==id);},L('已删除纪念日。','Milestone deleted.'),()=>{state.page=state.editorFrom||'milestones';});}
},{signal:abort.signal});
root.addEventListener('change',event=>{
 const el=event.target;if(busy)return;
 if(el.dataset.setting){state.draft[el.dataset.setting]=el.checked;void autoSaveSettings(`[data-setting="${el.dataset.setting}"]`,el.dataset.setting==='full',el.dataset.setting);}
 else if(el.hasAttribute('data-scale'))void autoSaveSettings('[data-scale]',false,'scale');
 else if(el.dataset.eventCheck){const id=Number(el.dataset.eventCheck),item=state.events.find(e=>e.id===id),checked=el.checked;if(item)void commitData(next=>{next.events.find(e=>e.id===id).completed=checked;},L('事项完成状态已更新。','Event status updated.')).then(ok=>{if(!ok)el.checked=item.completed;});}
 else if(el.hasAttribute('data-future-hide')){const checked=el.checked,before=state.applied.hideDone;void commitData(next=>{next.applied.hideDone=checked;},L('已更新隐藏完成事项。','Completed event visibility updated.')).then(ok=>{if(!ok)el.checked=before;});}
 else if(el.matches('#milestone-form select[name=kind]')){state.milestoneDraft.kind=el.value;app.querySelector('#milestone-form input[name=lunar]').disabled=el.value==='countdown';remember();}
 else if(el.closest('#milestone-form')&&el.type==='checkbox'){state.milestoneDraft[el.name]=el.checked;remember();}
},{signal:abort.signal});
root.addEventListener('input',event=>{const el=event.target;if(el.hasAttribute('data-scale'))setScale(el.value);else if(el.closest('#milestone-form')&&['title','date'].includes(el.name))state.milestoneDraft[el.name]=el.value;remember();},{signal:abort.signal});
root.addEventListener('submit',event=>{if(event.target.id==='week-task-form'){event.preventDefault();saveEvent();}else if(event.target.id==='milestone-form'){event.preventDefault();saveMilestone();}else if(event.target.id==='jump-form'){event.preventDefault();app.querySelector('[data-action=save-jump]').click();}},{signal:abort.signal});
root.addEventListener('keydown',event=>{if(busy)return;if(event.key==='Enter'&&event.target.matches('#week-task-form input[name=title]')){event.preventDefault();saveEvent();}else if(event.key==='Escape'){const b=app.querySelector('[data-action=close-settings]')||app.querySelector('[data-action=cancel-edit]')||app.querySelector('[data-action=cancel-jump]')||app.querySelector('[data-action=cancel]')||app.querySelector('[data-action=back-preview]')||app.querySelector('[data-action=close-milestones]')||app.querySelector('[data-action=close-future]');if(b){event.preventDefault();b.click();}}},{signal:abort.signal});
if(demo)window.addEventListener('openai:set_globals',event=>{const snapshot=event.detail?.globals?.widgetState;if(snapshot&&JSON.stringify(snapshot.privateContent?.state)!==JSON.stringify(state)){restore(snapshot);state.events=state.events.map(validateEvent);render();}},{signal:abort.signal});
function refreshDate(){
  if(disposed||demo)return;const next=today();
  if(next!==currentToday){const follows=state.selected===currentToday;currentToday=next;if(follows)state.selected=next;render();remember();}
  clearTimeout(midnightTimer);midnightTimer=setTimeout(refreshDate,nextMidnight());
}
function onResize(){if(production&&state.direction!==effectiveDirection()){render();remember();}else{layout();drawPreviews();}}
window.addEventListener('resize',onResize,{signal:abort.signal});
window.visualViewport?.addEventListener('resize',onResize,{signal:abort.signal});
document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible')refreshDate();},{signal:abort.signal});
window.addEventListener('focus',refreshDate,{signal:abort.signal});
window.addEventListener('beforeunload',event=>{if(busy||state.settingsPending){event.preventDefault();event.returnValue='';}else remember();},{signal:abort.signal});
setBusy(false);render();if(stale)showError(new Error('storage_conflict'));else if(state.settingsPending)showError(new Error('settings_save_failed'));
document.fonts.ready.then(()=>{if(!disposed){layout();drawPreviews();}});
if(production)refreshDate();
return {reloadData,handleExternalChange,dispose(){disposed=true;abort.abort();clearTimeout(midnightTimer);gridObserver?.disconnect();repository.close();}};
}
