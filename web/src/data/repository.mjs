import { clone, emptySnapshot, validateSnapshot } from '../domain/model.mjs';

export class ConflictError extends Error { constructor(){super('storage_conflict');this.name='ConflictError';} }
function storedRevision(record){
  if(!record)return 0;
  if(!Number.isSafeInteger(record.revision)||record.revision<1)throw new Error('storage_invalid');
  return record.revision;
}

export async function openRepository({name='ink-calendar-web',onChange=()=>{},indexedDB=globalThis.indexedDB}={}) {
  if(!indexedDB)throw new Error('storage_unavailable');
  const db=await new Promise((resolve,reject)=>{
    let settled=false;const request=indexedDB.open(name,1);
    request.onupgradeneeded=()=>{if(!request.result.objectStoreNames.contains('snapshots'))request.result.createObjectStore('snapshots',{keyPath:'key'});};
    request.onerror=()=>{settled=true;reject(request.error);};
    request.onblocked=()=>{settled=true;reject(new Error('storage_blocked'));};
    request.onsuccess=()=>{if(settled)request.result.close();else{settled=true;resolve(request.result);}};
  });
  let channel=null;
  try{if(typeof BroadcastChannel==='function')channel=new BroadcastChannel('ink-calendar:'+name);}catch{}
  if(channel)channel.onmessage=event=>onChange(event.data?.revision);
  db.onversionchange=()=>{db.close();onChange(-1);};
  return {
    async load(){
      return new Promise((resolve,reject)=>{
        const tx=db.transaction('snapshots','readonly');let record;
        tx.objectStore('snapshots').get('calendar').onsuccess=event=>{record=event.target.result;};
        tx.onerror=()=>reject(tx.error);tx.onabort=()=>reject(tx.error||new Error('storage_aborted'));
        tx.oncomplete=()=>{try{resolve(record?{revision:storedRevision(record),data:validateSnapshot(record.data)}:{revision:0,data:emptySnapshot()});}catch(error){reject(error);}};
      });
    },
    async save(input,expectedRevision){
      const data=validateSnapshot(input);
      return new Promise((resolve,reject)=>{
        const tx=db.transaction('snapshots','readwrite'),store=tx.objectStore('snapshots');let error,revision;
        store.get('calendar').onsuccess=event=>{
          try{
            const current=storedRevision(event.target.result);
            if(current!==expectedRevision)throw new ConflictError();
            revision=current+1;if(!Number.isSafeInteger(revision))throw new Error('storage_invalid');
            store.put({key:'calendar',revision,data});
          }catch(cause){error=cause;tx.abort();}
        };
        tx.onerror=()=>reject(error||tx.error);tx.onabort=()=>reject(error||tx.error||new Error('storage_aborted'));
        tx.oncomplete=()=>{try{channel?.postMessage({revision});}catch{}resolve({revision,data:clone(data)});};
      });
    },
    close(){channel?.close();db.close();},
  };
}

export function memoryRepository(initial=emptySnapshot()) {
  let revision=0,data=validateSnapshot(initial);
  return {
    async load(){return {revision,data:clone(data)};},
    async save(next,expected){if(expected!==revision)throw new ConflictError();data=validateSnapshot(next);return {revision:++revision,data:clone(data)};},
    close(){},
  };
}
