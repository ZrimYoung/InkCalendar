import catalog from '../generated/catalog.mjs';
import nativeStrings from '../generated/native-strings.mjs';
import { requireDate } from './dates.mjs';

export const clone=value=>structuredClone(value);
export const trim=value=>typeof value==='string'?value.replace(/^[\u0000-\u0020]+|[\u0000-\u0020]+$/g,''):'';
export function translate(key,language='zh',...values) {
  let text=nativeStrings[language]?.strings[key]??nativeStrings.en.strings[key]??key;
  return text.replace(/%(\d+)\$[ds]/g,(_,n)=>String(values[Number(n)-1]??'')).replaceAll('%%','%');
}
export function plural(key,count,language='zh') {
  const values=nativeStrings[language]?.plurals[key]??nativeStrings.en.plurals[key];
  if(!values)return '';
  return (language==='en'&&count===1?values.one||values.other:values.other).replace(/%\d+\$d/g,String(count));
}
export function normalizeTheme(value={}) {
  const style=catalog.themes.some(t=>t.id===value.style)?value.style:catalog.defaults.theme.style;
  const font=catalog.fonts.some(f=>f.id===value.font)?value.font:catalog.defaults.theme.font;
  const overrides={};
  for(const c of catalog.components)if(c.options.some(o=>o.id===value.overrides?.[c.key]))overrides[c.key]=value.overrides[c.key];
  return {style,font,overrides};
}
export function resolveTheme(value) {
  const theme=normalizeTheme(value),recipe=catalog.themes.find(t=>t.id===theme.style).recipe;
  return Object.fromEntries(catalog.components.map((c,i)=>[c.key,theme.overrides[c.key]||recipe[i]]));
}
export function defaultSettings(platform='web') { return clone({...catalog.defaults,...(platform==='web'?catalog.webDefaults:{})}); }
export function normalizeSettings(value={},platform='web') {
  const result=defaultSettings(platform),limit=catalog.limits;
  for(const key of ['awake','full','lunar','terms','holidays','hideDone','desk'])if(typeof value[key]==='boolean')result[key]=value[key];
  for(const [key,options] of Object.entries({view:['month','week'],layout:['classic','balanced'],orientation:['auto','landscape','portrait'],language:['zh','en']}))if(options.includes(value[key]))result[key]=value[key];
  if(Number.isInteger(value.scale))result.scale=Math.max(limit.scaleMin,Math.min(limit.scaleMax,value.scale));
  if([7,30].includes(value.futureDays))result.futureDays=value.futureDays;
  result.theme=normalizeTheme(value.theme);return result;
}
export function validateEvent(input) {
  requireDate(input.date);const title=trim(input.title),time=input.time==null?'':trim(input.time);
  if(!title)throw new Error('title_required');if(title.length>catalog.limits.titleLength)throw new Error('title_too_long');
  if(time&&!/^([01][0-9]|2[0-3]):[0-5][0-9]$/.test(time))throw new Error('time_invalid');
  return {id:Number(input.id)||0,date:input.date,title,time,completed:Boolean(input.completed??input.done)};
}
export function validateMilestone(input) {
  const event=validateEvent({...input,time:''});
  const kind=input.kind===0?'anniversary':input.kind===1?'countdown':input.kind;
  if(!['anniversary','countdown'].includes(kind))throw new Error('kind_invalid');
  return {id:event.id,date:event.date,title:event.title,kind,lunar:kind==='anniversary'&&Boolean(input.lunar),pinned:Boolean(input.pinned)};
}
export function validateSnapshot(input) {
  if(!input||input.schema!==1||!Array.isArray(input.events)||!Array.isArray(input.milestones))throw new Error('storage_invalid');
  const events=input.events.map(validateEvent),milestones=input.milestones.map(validateMilestone);
  for(const rows of [events,milestones]){
    const ids=new Set();for(const row of rows){if(!Number.isSafeInteger(row.id)||row.id<1||ids.has(row.id))throw new Error('storage_invalid');ids.add(row.id);}
  }
  if(milestones.filter(m=>m.pinned).length>catalog.limits.pinnedMilestones)throw new Error('pin_limit');
  return {schema:1,applied:normalizeSettings(input.applied),events,milestones};
}
export function emptySnapshot() { return {schema:1,applied:defaultSettings(),events:[],milestones:[]}; }
export function compareEvents(a,b) { return a.date.localeCompare(b.date)||a.time.localeCompare(b.time)||a.id-b.id; }
export function compareMilestones(a,b) { return Number(b.pinned)-Number(a.pinned)||a.date.localeCompare(b.date)||a.id-b.id; }
export function eventsBetween(events,start,end,hideCompleted=false) { return events.filter(e=>e.date>=start&&e.date<=end&&(!hideCompleted||!e.completed)).sort(compareEvents); }
export function nextId(rows) { return rows.reduce((max,row)=>Math.max(max,row.id),0)+1; }
