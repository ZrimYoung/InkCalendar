import catalog from '../generated/catalog.mjs';

export const MIN_DATE = catalog.limits.dateMin;
export const MAX_DATE = catalog.limits.dateMax;
const DAY = 86_400_000;
export function dateOf(key) { return new Date(key+'T12:00:00Z'); }
export function validDate(key) {
  if (typeof key !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(key) || key < MIN_DATE || key > MAX_DATE) return false;
  const date = dateOf(key);
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === key;
}
export function requireDate(key) { if (!validDate(key)) throw new Error('date_invalid'); return key; }
export function localToday(now = new Date()) {
  return `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`;
}
export function addDays(key, count) { const date = dateOf(key); date.setUTCDate(date.getUTCDate()+count); return date.toISOString().slice(0,10); }
export function addMonths(key, count) {
  const date = dateOf(key), last = new Date(Date.UTC(date.getUTCFullYear(),date.getUTCMonth()+count+1,0,12)).getUTCDate();
  return new Date(Date.UTC(date.getUTCFullYear(),date.getUTCMonth()+count,Math.min(date.getUTCDate(),last),12)).toISOString().slice(0,10);
}
export function monthOf(key) { return key.slice(0,7)+'-01'; }
export function weekStart(key) { return addDays(key,-((dateOf(key).getUTCDay()+6)%7)); }
export function monthGrid(key) { const start=weekStart(monthOf(key)); return Array.from({length:42},(_,i)=>addDays(start,i)); }
export function distance(from, to) { return Math.round((dateOf(to)-dateOf(from))/DAY); }
export function clampDate(key) { return key < MIN_DATE ? MIN_DATE : key > MAX_DATE ? MAX_DATE : key; }
export function nextMidnight(now = new Date()) {
  const next = new Date(now); next.setDate(next.getDate()+1); next.setHours(0,0,0,0);
  return Math.max(1000,next.getTime()-now.getTime()+250);
}
