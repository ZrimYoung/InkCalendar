import catalog from '../generated/catalog.mjs';

export function calendarFitScale(natural,available){
  return natural<=0?1:Math.min(1,Math.max(catalog.limits.calendarFitMin/100,available/natural));
}
