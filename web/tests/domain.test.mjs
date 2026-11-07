import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import catalog from '../src/generated/catalog.mjs';
import {validDate,weekStart,monthGrid,distance,addMonths,localToday,nextMidnight} from '../src/domain/dates.mjs';
import {lunarDate,milestoneNext,milestoneYears,annotation,dateDetails} from '../src/domain/almanac.mjs';
import {calendarFitScale} from '../src/domain/layout.mjs';
import {validateEvent,validateMilestone,validateSnapshot,emptySnapshot,normalizeSettings,normalizeTheme,resolveTheme,eventsBetween,compareMilestones,translate,plural} from '../src/domain/model.mjs';

const fixtures=JSON.parse(await readFile(new URL('../../shared/parity-cases.json',import.meta.url),'utf8'));
for(const c of fixtures.dates)test('shared calendar: '+c.date,()=>{
  assert.equal(validDate(c.date),true);assert.equal(weekStart(c.date),c.weekStart);
  const days=monthGrid(c.date);assert.equal(days.length,42);assert.equal(days[0],c.gridFirst);assert.equal(days[41],c.gridLast);
});
for(const date of fixtures.invalidDates)test('shared invalid date: '+date,()=>assert.equal(validDate(date),false));
for(const c of fixtures.calendarFits)test(`shared landscape fit: ${c.natural}/${c.available}`,()=>assert.equal(calendarFitScale(c.natural,c.available),c.scale));
fixtures.events.forEach((c,i)=>test('shared event validation: '+i,()=>{
  const input={id:1,...c.input};if(c.repeat)input.title=c.repeat.text.repeat(c.repeat.count);
  if(c.error)assert.throws(()=>validateEvent(input),{message:c.error});
  else assert.deepEqual(validateEvent(input),{id:1,date:input.date,title:c.title??input.title,time:c.time,completed:c.completed});
}));
fixtures.milestones.forEach((c,i)=>test('shared milestone recurrence: '+i,()=>{
  const milestone=validateMilestone({id:1,title:'Test',pinned:false,...c}),next=milestoneNext(milestone,c.today);
  assert.equal(next,c.next);if(c.years!==undefined)assert.equal(milestoneYears(milestone,next),c.years);
  if(c.distance!==undefined)assert.equal(distance(c.today,next),c.distance);
  if(c.kind==='countdown')assert.equal(milestone.lunar,false);
}));
for(const c of fixtures.lunarDates)test('shared lunar date: '+c.date,()=>{
  const lunar=lunarDate(c.date);assert.equal(lunar.month,c.month);assert.equal(lunar.day,c.day);assert.equal(lunar.short,c.short);
});
test('calendar dates remain stable across DST and month boundaries',()=>{
  const zone=process.env.TZ;process.env.TZ='America/New_York';
  try{
    assert.equal(distance('2026-03-07','2026-03-09'),2);assert.equal(addMonths('2026-01-31',1),'2026-02-28');
    const before=new Date(2026,2,8,0,0,0);assert.equal(nextMidnight(before),23*60*60*1000+250);
    assert.equal(localToday(new Date(2026,10,7,23,59)),'2026-11-07');
  }finally{if(zone===undefined)delete process.env.TZ;else process.env.TZ=zone;}
});
test('theme presets, component overrides and fonts remain independent',()=>{
  for(const preset of catalog.themes){
    const theme={style:preset.id,font:'space',overrides:{}};
    assert.deepEqual(Object.values(resolveTheme(theme)),preset.recipe);
    theme.overrides.date='reverse';assert.equal(resolveTheme(theme).date,'reverse');assert.equal(theme.font,'space');
  }
  assert.deepEqual(normalizeTheme({style:'bad',font:'bad',overrides:{date:'bad',unknown:'x'}}),catalog.defaults.theme);
  assert.equal(normalizeSettings({scale:118}).scale,118);assert.equal(normalizeSettings({scale:-1}).scale,90);
  assert.equal(normalizeSettings({scale:200}).scale,130);assert.equal(normalizeSettings().full,false);
  assert.equal(normalizeSettings({},'android').full,true);
  assert.equal(normalizeSettings({futureDays:30}).futureDays,30);assert.equal(normalizeSettings({futureDays:1}).futureDays,7);
});
test('snapshot validation rejects duplicate IDs and too many pinned milestones',()=>{
  const data=emptySnapshot(),event=validateEvent({id:1,date:'2026-11-07',title:'A',time:''});
  assert.throws(()=>validateSnapshot({...data,events:[event,event]}),{message:'storage_invalid'});
  assert.throws(()=>validateSnapshot({...data,events:[{...event,id:0}]}),{message:'storage_invalid'});
  const milestone={date:event.date,title:'A',kind:'anniversary',lunar:false,pinned:true};
  assert.throws(()=>validateSnapshot({...data,milestones:[1,2,3].map(id=>({...milestone,id}))}),{message:'pin_limit'});
  assert.deepEqual(data,emptySnapshot());
});
test('events are ordered by day, time and identity, with completion filtering',()=>{
  const rows=[{id:4,date:'2026-11-08',time:'',completed:false},{id:3,date:'2026-11-07',time:'09:00',completed:false},{id:2,date:'2026-11-07',time:'',completed:true},{id:1,date:'2026-11-07',time:'09:00',completed:false}];
  assert.deepEqual(eventsBetween(rows,'2026-11-07','2026-11-08').map(e=>e.id),[2,1,3,4]);
  assert.deepEqual(eventsBetween(rows,'2026-11-07','2026-11-07',true).map(e=>e.id),[1,3]);
});
test('native translations and packaged almanac are used in both languages',()=>{
  assert.equal(translate('anniversary_years','zh',6,'今天'),'第 6 周年 · 今天');
  assert.equal(plural('event_count',1,'en'),'1 event');assert.equal(plural('event_count',2,'en'),'2 events');
  const options={lunar:true,terms:true,holidays:true,language:'zh'};
  assert.equal(annotation('2026-02-17',options),'休');assert.match(dateDetails('2026-02-17',options),/春节/);
  assert.match(dateDetails('2027-01-01',options),/本年调休数据未收录/);
  assert.equal(annotation('2101-01-01',options),'');
});
test('milestones follow native pinned, anchor date and identity ordering',()=>{
  const rows=[{id:3,date:'2020-01-01',pinned:false},{id:2,date:'2026-11-07',pinned:true},{id:1,date:'2026-11-07',pinned:true}];
  assert.deepEqual(rows.sort(compareMilestones).map(m=>m.id),[1,2,3]);
});
