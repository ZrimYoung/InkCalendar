import records from '../generated/almanac.mjs';
import { dateOf, validDate, distance, addDays, MAX_DATE } from './dates.mjs';

const TERMS='小寒 大寒 立春 雨水 惊蛰 春分 清明 谷雨 立夏 小满 芒种 夏至 小暑 大暑 立秋 处暑 白露 秋分 寒露 霜降 立冬 小雪 大雪 冬至'.split(' ');
const MONTHS='正 二 三 四 五 六 七 八 九 十 冬 腊'.split(' ');
export function lunarDate(key) {
  if (!validDate(key)) return null;
  const offset=distance('1900-01-01',key); let low=0,high=records.months.length-1;
  while(low<=high){const mid=(low+high)>>1;if(records.months[mid][0]<=offset)low=mid+1;else high=mid-1;}
  if(high<0)return null;
  const [start,year,month,length]=records.months[high],day=offset-start+1;
  const monthName=(month<0?'闰':'')+MONTHS[Math.abs(month)-1]+'月';
  const dayName=day===10?'初十':day===20?'二十':day===30?'三十':['初','十','廿'][Math.floor((day-1)/10)]+'一二三四五六七八九'[(day-1)%10];
  return {year,month,day,length,short:day===1?monthName:dayName,full:monthName+dayName};
}
export function term(key) { return records.terms[key]===undefined?'':TERMS[records.terms[key]]; }
export function holidayStatus(key,language='zh') {
  if(records.work.includes(key))return language==='en'?'Work':'班';
  return records.rest.some(([a,b])=>key>=a&&key<=b)?(language==='en'?'Off':'休'):'';
}
export function festival(key,language='zh') {
  const md=key.slice(5),lunar=lunarDate(key); let names;
  if(md==='01-01')names=['元旦','New Year'];else if(md==='05-01')names=['劳动节','Labour Day'];else if(md==='10-01')names=['国庆节','National Day'];
  else if(term(key)==='清明')names=['清明节','Qingming'];
  else if(lunar&&lunar.month>0){
    names=({'1-1':['春节','Spring Festival'],'1-15':['元宵节','Lantern Festival'],'5-5':['端午节','Dragon Boat Festival'],'8-15':['中秋节','Mid-Autumn Festival'],'9-9':['重阳节','Double Ninth']})[lunar.month+'-'+lunar.day];
    if(!names&&lunar.month===12&&lunar.day===lunar.length)names=['除夕','Spring Festival Eve'];
  }
  return names?.[language==='en'?1:0]||'';
}
export function annotation(key,options) {
  if(!validDate(key))return '';
  const language=options.language;
  if(options.holidays&&holidayStatus(key,language))return holidayStatus(key,language);
  if(language!=='en'&&options.terms&&term(key))return term(key);
  if(options.holidays&&festival(key,language)){
    const name=festival(key,language);
    return language==='en'?({'New Year':'New Yr','Labour Day':'May Day','National Day':'Natl','Spring Festival':'Spring','Lantern Festival':'Lant.','Dragon Boat Festival':'Boat','Mid-Autumn Festival':'Moon','Double Ninth':'Ninth','Spring Festival Eve':'Eve'}[name]||name):name;
  }
  return language!=='en'&&options.lunar?(lunarDate(key)?.short||''):'';
}
export function dateDetails(key,options) {
  const en=options.language==='en';
  return [!en&&options.lunar?lunarDate(key)?.full:'',!en&&options.terms?term(key):'',options.holidays?festival(key,options.language):'',options.holidays?holidayStatus(key,options.language):'',options.holidays&&!key.startsWith('2026-')?(en?'Holiday schedule unavailable for this year':'本年调休数据未收录'):''].filter(Boolean).join(' · ');
}
export function nextAnniversary(anchor,today) {
  const original=lunarDate(anchor),current=lunarDate(today);if(!original||!current)return null;
  for(let year=current.year;year<=2100;year++){
    let chosen=records.months.find(r=>r[1]===year&&r[2]===original.month);
    if(!chosen&&original.month<0)chosen=records.months.find(r=>r[1]===year&&r[2]===-original.month);
    if(chosen){const candidate=addDays('1900-01-01',chosen[0]+Math.min(original.day,chosen[3])-1);if(candidate>=today&&candidate<=MAX_DATE)return candidate;}
  }
  return null;
}
export function milestoneNext(milestone,today) {
  if(milestone.kind==='countdown'||milestone.date>=today)return milestone.date;
  if(milestone.lunar)return nextAnniversary(milestone.date,today);
  const anchor=dateOf(milestone.date);
  for(let y=dateOf(today).getUTCFullYear();y<=2100;y++){
    const last=new Date(Date.UTC(y,anchor.getUTCMonth()+1,0,12)).getUTCDate();
    const candidate=new Date(Date.UTC(y,anchor.getUTCMonth(),Math.min(anchor.getUTCDate(),last),12)).toISOString().slice(0,10);
    if(candidate>=today)return candidate;
  }
  return null;
}
export function milestoneYears(milestone,occurrence) { return milestone.lunar?lunarDate(occurrence).year-lunarDate(milestone.date).year:dateOf(occurrence).getUTCFullYear()-dateOf(milestone.date).getUTCFullYear(); }
