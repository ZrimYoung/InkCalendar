import { mountCalendar } from './ui/calendar-app.mjs';
import { prepareFrame } from './ui/frame.mjs';
import { memoryRepository } from './data/repository.mjs';
import { defaultSettings } from './domain/model.mjs';

export const DEMO_TODAY='2026-11-07';
export function demoSnapshot(){return {schema:1,applied:defaultSettings('android'),milestones:[],events:[
  {id:1,date:'2026-11-02',time:'',title:'整理本周计划',completed:false},
  {id:2,date:'2026-11-03',time:'09:30',title:'项目讨论',completed:false},
  {id:3,date:'2026-11-03',time:'15:00',title:'整理资料',completed:true},
  {id:4,date:'2026-11-05',time:'10:00',title:'核对账单',completed:false},
  {id:5,date:'2026-11-06',time:'09:30',title:'阅读 · Book notes',completed:true},
  {id:6,date:'2026-11-06',time:'14:00',title:'整理书桌',completed:false},
  {id:7,date:'2026-11-07',time:'',title:'给绿植浇水',completed:false},
  {id:8,date:'2026-11-07',time:'10:30',title:'散步',completed:false},
  {id:9,date:'2026-11-07',time:'16:00',title:'准备周末晚餐',completed:false},
  {id:10,date:'2026-11-08',time:'20:00',title:'下周安排',completed:false},
]};}
function localDemoBridge(){
  const key='ink-calendar-prototype-v2';let widgetState=null;
  try{widgetState=JSON.parse(localStorage.getItem(key));}catch{}
  return {get widgetState(){return widgetState;},async setWidgetState(value){widgetState=value;try{localStorage.setItem(key,JSON.stringify(value));}catch{}}};
}
export async function startDemo(root){
  prepareFrame(root,{demo:true});
  const repo=memoryRepository(demoSnapshot()),initial=await repo.load();
  return mountCalendar(root,{demo:true,initial,repository:repo,today:()=>DEMO_TODAY,demoBridge:window.openai||localDemoBridge()});
}
